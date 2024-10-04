package id.co.anudbservice.controller;

import id.co.anudbservice.model.dto.GETDto;
import id.co.anudbservice.repository.DynamicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

@RequiredArgsConstructor
public abstract class DatabaseApiController<Repo extends DynamicRepository> {

    private final Repo repo;

    @DeleteMapping("/{table}")
    public ResponseEntity<?> deleteTableData(
            @PathVariable String table,
            @RequestParam Map<String, String> filter
    ) {
        return ResponseEntity.ok(repo.deleteTableData(table, filter));
    }

    @PutMapping("/{table}")
    public ResponseEntity<?> updateTableData(
            @PathVariable String table,
            @RequestParam Map<String, String> filter,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(repo.updateTableData(table, body, filter));
    }

    @PostMapping("/{table}")
    public ResponseEntity<?> insertTableData(
            @PathVariable String table,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(repo.insertTableData(table, body));
    }

    @GetMapping("/{table}")
    public ResponseEntity<?> getTableData(
            @PathVariable String table,
            @RequestParam(defaultValue = "*") String select,
            @RequestParam(required = false) Map<String,String> filter,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(required = false) String orderby,
            @RequestParam(required = false) String join,
            @RequestParam(required = false) String on,
            @RequestParam(defaultValue = "true", required = false) boolean ascending
    ) {
        Stream.of("page", "size", "orderby", "select", "ascending", "join", "on").forEach((param) -> {
            try {
                filter.remove(param);
            } catch (Exception ignored) {}
        });
        GETDto getDto = GETDto.builder()
                .table(table)
                .selects(Arrays.stream(select.split(",")).toList())
                .page(PageRequest.of(page, size))
                .orderBy(orderby)
                .ascending(ascending)
                .filter(filter)
                .join(join)
                .on(on)
                .build();

        return ResponseEntity.ok(repo.getTableData(getDto));
    }
}
