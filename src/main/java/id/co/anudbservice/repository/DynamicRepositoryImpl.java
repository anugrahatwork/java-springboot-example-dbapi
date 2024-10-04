package id.co.anudbservice.repository;

import id.co.anudbservice.model.dto.ColumnInfo;
import id.co.anudbservice.model.dto.GETDto;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Setter
public class DynamicRepositoryImpl implements DynamicRepository {

    private DataSource dataSource;

    @Override
    public void dataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Page<Map<String, Object>> getTableData(GETDto getDto) {
        List<ColumnInfo> columnInfos = getAllColumnName(getDto.getTable());

        if(getDto.getSelects().isEmpty() || getDto.getSelects().stream().anyMatch((s) -> s.equals("*"))) {
            getDto.setSelects(columnInfos.stream().map(ColumnInfo::getColumnName).toList());
        }

        String selectedFields = String.join(", ", getDto.getSelects());

        StringBuilder countSql = new StringBuilder(
                String.format("SELECT count(*) FROM %s", getDto.getTable())
        );

        StringBuilder initSql = new StringBuilder(
                String.format("SELECT %s FROM %s", selectedFields, getDto.getTable())
        );

        StringBuilder sql = new StringBuilder();

        int i = 1;
        Map<Integer, Map<String, Object>> clause = new HashMap<>();

        if(!getDto.getFilter().isEmpty()) {
            for (Map.Entry<String, String> entry : getDto.getFilter().entrySet()) {
                List<String> constructFilter = List.of(entry.getValue().split("\\."));
                if(i == 1) {
                    sql.append(" WHERE ")
                            .append(entry.getKey())
                            .append(" = ")
                            .append(" ? ");
                    clause.put(i++, Map.of(
                            "name", entry.getKey(),
                            "value", constructFilter.get(1)
                    ));
                } else {
                    sql.append(Objects.equals(constructFilter.get(0), "eq")
                            ? " AND "
                            : " OR "
                    ).append(entry.getKey())
                            .append(" = ")
                            .append(" ? ");
                    clause.put(i++, Map.of(
                            "name", entry.getKey(),
                            "value", constructFilter.get(1)
                    ));
                }
            }
        }
        if(getDto.getOrderBy() != null) {
            sql.append(" order by ").append(" ? ").append(getDto.isAscending() ? " ASC" : " DESC");
            clause.put(i++,
                    clause.put(i++, Map.of(
                            "name", "orderby",
                            "value", getDto.getOrderBy()
                    )));
        }
        StringBuilder rawSql = countSql.append(sql);
        sql = initSql.append(sql);

        if(getDto.getPage() != null) {
            sql.append(" LIMIT ? OFFSET ?");
            clause.put(i++,
                    clause.put(i++, Map.of(
                            "name", "limit",
                            "value", getDto.getPage().getPageSize()
                    )));
            clause.put(i,
                    clause.put(i, Map.of(
                            "name", "offset",
                            "value", getDto.getPage().getOffset()
                    )));
        }

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());
            PreparedStatement rawPreparedStatement = connection.prepareStatement(rawSql.toString());

            AtomicInteger j = new AtomicInteger(1);
            getDto.getFilter().forEach((key, value) -> {
                try {
                    int index = j.get();
                    String tableName = clause.get(index).get("name").toString();
                    String type = columnInfos.stream().filter((c) -> c.getColumnName().equals(tableName)).findFirst().orElseThrow().getColumnType();
                    Object val = clause.get(index).get("value");
                    setPreparedStatementValue(preparedStatement, index, type, val);
                    setPreparedStatementValue(rawPreparedStatement, index, type, val);
                    j.getAndIncrement();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            if(getDto.getOrderBy() != null) {
                rawPreparedStatement.setString(j.get(), getDto.getOrderBy());
                preparedStatement.setString(j.getAndIncrement(), getDto.getOrderBy());
            }
            if(getDto.getPage() != null) {
                preparedStatement.setInt(j.getAndIncrement(), getDto.getPage().getPageSize());
                preparedStatement.setLong(j.getAndIncrement(), getDto.getPage().getOffset());
            }
            long count = 0;
            try (ResultSet rs = rawPreparedStatement.executeQuery()) {
                while (rs.next()) {
                    count = rs.getLong(1);
                }
            }
            try (ResultSet rs = preparedStatement.executeQuery()) {
                List<Map<String, Object>> result = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> param = new HashMap<>();
                    for (String columnName : getDto.getSelects()) {
                        param.put(columnName, rs.getObject(columnName));
                    }
                    result.add(param);
                }
                return new PageImpl<>(result, getDto.getPage(), count);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<ColumnInfo> getAllColumnName(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                List<ColumnInfo> columnInfos = new ArrayList<>();
                while (rs.next()) {
                    columnInfos.add(ColumnInfo.builder()
                            .columnName(rs.getString("COLUMN_NAME"))
                            .columnType(rs.getString("TYPE_NAME"))
                            .build()
                    );
                }
                return columnInfos;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setPreparedStatementValue(PreparedStatement preparedStatement, int paramIndex, String columnType, Object value) throws SQLException {
        switch (columnType.toLowerCase()) {
            case "varchar":
            case "text":
                preparedStatement.setString(paramIndex, value.toString());
                break;
            case "integer":
            case "int4":
            case "serial":
                preparedStatement.setInt(paramIndex, Integer.parseInt(value.toString()));
                break;
            case "bigint":
            case "int8":
                preparedStatement.setLong(paramIndex, Long.parseLong(value.toString()));
                break;
            case "boolean":
            case "bool":
                preparedStatement.setBoolean(paramIndex, Boolean.parseBoolean(value.toString()));
                break;
            case "uuid":
                preparedStatement.setObject(paramIndex, UUID.fromString(value.toString()));
                break;
            // Add more cases for other data types as needed
            default:
                preparedStatement.setObject(paramIndex, value);
                break;
        }
    }

    @Override
    public Map<String, Object> insertTableData(String table, Map<String, Object> body) {
        List<ColumnInfo> columnInfos = getAllColumnName(table);
        StringBuilder sql = new StringBuilder(
                String.format("INSERT INTO %s (", table)
        );
        StringBuilder values = new StringBuilder(" VALUES (");

        AtomicInteger i = new AtomicInteger();

        body.forEach((key, value) -> {
            if(columnInfos.stream().anyMatch((c) -> c.getColumnName().equals(key))) {
                sql.append(key);
                values.append("?");
            } else {
                throw new RuntimeException("Column " + key + " not found in table " + table);
            }
            i.getAndIncrement();
            if(body.size() > i.get()) {
                sql.append(", ");
                values.append(", ");
            } else {
                sql.append(")");
                values.append(")");
            }
        });
        sql.append(values);
        sql.append(" RETURNING *");
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());
            i.set(1);
            body.forEach((key, value) -> {
                try {
                    ColumnInfo columnInfo = columnInfos.stream().filter((c) -> c.getColumnName().equals(key)).findFirst().orElseThrow();
                    setPreparedStatementValue(preparedStatement, i.getAndIncrement(), columnInfo.getColumnType(), value);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if(rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    for (ColumnInfo columnInfo : columnInfos) {
                        result.put(columnInfo.getColumnName(), rs.getObject(columnInfo.getColumnName()));
                    }
                    return result;
                } else {
                    throw new RuntimeException("Failed to insert data");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> updateTableData(String table, Map<String, Object> body, Map<String, String> filter) {
        List<ColumnInfo> columnInfos = getAllColumnName(table);

        StringBuilder sql = new StringBuilder(
                String.format("UPDATE %s SET ", table)
        );

        Map<Integer, Map<String, Object>> clause = new HashMap<>();

        AtomicInteger i = new AtomicInteger();
        body.forEach((key, value) -> {
            if(columnInfos.stream().anyMatch((c) -> c.getColumnName().equals(key))) {
                sql.append(key).append(" = ?");
            } else {
                throw new RuntimeException("Column " + key + " not found in table " + table);
            }
            clause.put(i.get(), Map.of(
                    "name", key,
                    "value", value
            ));
            i.getAndIncrement();
            if(body.size() > i.get()) {
                sql.append(", ");
            }
        });

        if(filter.isEmpty()) {
            throw new RuntimeException("Filter is required to update data");
        } else {
            sql.append(" WHERE ");
            filter.forEach((key, value) -> {
                if(columnInfos.stream().anyMatch((c) -> c.getColumnName().equals(key))) {
                    sql.append(key).append(" = ?");
                } else {
                    throw new RuntimeException("Column " + key + " not found in table " + table);
                }
                i.getAndIncrement();
                clause.put(i.get(), Map.of(
                        "name", key,
                        "value", value
                ));
                if(filter.size() > i.get()) {
                    sql.append(" AND ");
                }
            });
        }

        sql.append(" RETURNING *");
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());
            i.set(1);
            clause.forEach((key, value) -> {
                try {
                    ColumnInfo columnInfo = columnInfos.stream().filter((c) -> c.getColumnName().equals(value.get("name"))).findFirst().orElseThrow();
                    setPreparedStatementValue(preparedStatement, i.getAndIncrement(), columnInfo.getColumnType(), value.get("value"));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if(rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    for (ColumnInfo columnInfo : columnInfos) {
                        result.put(columnInfo.getColumnName(), rs.getObject(columnInfo.getColumnName()));
                    }
                    return result;
                } else {
                    throw new RuntimeException("Failed to update data");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object deleteTableData(String table, Map<String, String> filter) {
        List<ColumnInfo> columnInfos = getAllColumnName(table);
        StringBuilder sql = new StringBuilder(
                String.format("DELETE FROM %s", table)
        );

        Map<Integer, Map<String, Object>> clause = new HashMap<>();
        AtomicInteger i = new AtomicInteger();
        if(filter.isEmpty()) {
            throw new RuntimeException("Filter is required to delete data");
        } else {
            sql.append(" WHERE ");
            filter.forEach((key, value) -> {
                if(columnInfos.stream().anyMatch((c) -> c.getColumnName().equals(key))) {
                    sql.append(key).append(" = ?");
                } else {
                    throw new RuntimeException("Column " + key + " not found in table " + table);
                }
                i.getAndIncrement();
                clause.put(i.get(), Map.of(
                        "name", key,
                        "value", value
                ));
                if(filter.size() > i.get()) {
                    sql.append(" AND ");
                }
            });
        }
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());
            i.set(1);
            clause.forEach((key, value) -> {
                try {
                    ColumnInfo columnInfo = columnInfos.stream().filter((c) -> c.getColumnName().equals(value.get("name"))).findFirst().orElseThrow();
                    setPreparedStatementValue(preparedStatement, i.getAndIncrement(), columnInfo.getColumnType(), value.get("value"));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            preparedStatement.executeUpdate();
            return Map.of("message", "Data deleted");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
