package net.lex.reddit.subs.web.rest;

import io.swagger.v3.oas.annotations.Hidden;
import net.lex.reddit.subs.service.ElasticsearchIndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/index")
public class ElasticsearchReindexResource {

    private final ElasticsearchIndexService elasticsearchIndexService;

    public ElasticsearchReindexResource(ElasticsearchIndexService elasticsearchIndexService) {
        this.elasticsearchIndexService = elasticsearchIndexService;
    }

    @GetMapping("")
    @Hidden
    public ResponseEntity<Void> reindex() {
        this.elasticsearchIndexService.reindexAll();
        return ResponseEntity.accepted().build();
    }
}
