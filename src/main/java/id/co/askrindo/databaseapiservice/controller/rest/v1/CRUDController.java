package id.co.askrindo.databaseapiservice.controller.rest.v1;

import id.co.askrindo.databaseapiservice.model.dto.GETDto;
import id.co.askrindo.databaseapiservice.repository.DynamicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/rest/v1/crud")
@RequiredArgsConstructor
public class CRUDController {

    private final DynamicRepository dynamicRepository;
    private final Environment environment;

    @DeleteMapping("/{table}")
    public ResponseEntity<?> deleteTableData(
            @PathVariable String table,
            @RequestParam Map<String, String> filter,
            @RequestHeader Map<String, String> headers
    ) {
        String key = headers.get("x-api-key");
        if(key == null || !key.equals(environment.getProperty("api.key"))) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        return ResponseEntity.ok(dynamicRepository.deleteTableData(table, filter));
    }

    @PutMapping("/{table}")
    public ResponseEntity<?> updateTableData(
            @PathVariable String table,
            @RequestParam Map<String, String> filter,
            @RequestBody Map<String, Object> body,
            @RequestHeader Map<String, String> headers
    ) {
        String key = headers.get("x-api-key");
        if(key == null || !key.equals(environment.getProperty("api.key"))) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        return ResponseEntity.ok(dynamicRepository.updateTableData(table, body, filter));
    }

    @PostMapping("/{table}")
    public ResponseEntity<?> insertTableData(
            @PathVariable String table,
            @RequestBody Map<String, Object> body,
            @RequestHeader Map<String, String> headers
    ) {
        String key = headers.get("x-api-key");
        if(key == null || !key.equals(environment.getProperty("api.key"))) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        return ResponseEntity.ok(dynamicRepository.insertTableData(table, body));
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
            @RequestParam(defaultValue = "true", required = false) boolean ascending,
            @RequestHeader Map<String, String> headers
    ) {
        String key = headers.get("x-api-key");
        if(key == null || !key.equals(environment.getProperty("api.key"))) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
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

        return ResponseEntity.ok(dynamicRepository.getTableData(getDto));
    }
}
