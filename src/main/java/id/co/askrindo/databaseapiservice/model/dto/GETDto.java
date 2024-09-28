package id.co.askrindo.databaseapiservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GETDto {
    private String table;
    private List<String> selects;
    private PageRequest page;
    private Sort sort;
    private String orderBy;
    private boolean ascending;
    private String join;
    private String on;
    private Map<String, String> filter;
}
