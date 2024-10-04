package id.co.anudbservice.repository;

import id.co.anudbservice.model.dto.GETDto;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.NoRepositoryBean;

import javax.sql.DataSource;
import java.util.Map;

@NoRepositoryBean
public interface DynamicRepository {

    void dataSource(DataSource dataSource);

    Page<Map<String, Object>> getTableData(GETDto getDto);

    Map<String, Object> insertTableData(String table, Map<String, Object> body);

    Map<String, Object> updateTableData(String table, Map<String, Object> body, Map<String, String> filter);

    Object deleteTableData(String table, Map<String, String> filter);
}
