package net.lex.reddit.subs.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import java.util.List;
import net.lex.reddit.subs.domain.RedditSubsCategorized;
import net.lex.reddit.subs.repository.RedditSubsCategorizedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link RedditSubsCategorized} entity.
 */
public interface RedditSubsCategorizedSearchRepository
    extends ElasticsearchRepository<RedditSubsCategorized, Long>, RedditSubsCategorizedSearchRepositoryInternal {}

interface RedditSubsCategorizedSearchRepositoryInternal {
    Page<RedditSubsCategorized> search(String query, Pageable pageable);

    Page<RedditSubsCategorized> search(Query query);

    @Async
    void index(RedditSubsCategorized entity);

    @Async
    void deleteFromIndexById(Long id);
}

class RedditSubsCategorizedSearchRepositoryInternalImpl implements RedditSubsCategorizedSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final RedditSubsCategorizedRepository repository;

    RedditSubsCategorizedSearchRepositoryInternalImpl(
        ElasticsearchTemplate elasticsearchTemplate,
        RedditSubsCategorizedRepository repository
    ) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<RedditSubsCategorized> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery.setPageable(pageable));
    }

    @Override
    public Page<RedditSubsCategorized> search(Query query) {
        SearchHits<RedditSubsCategorized> searchHits = elasticsearchTemplate.search(query, RedditSubsCategorized.class);
        List<RedditSubsCategorized> hits = searchHits.map(SearchHit::getContent).stream().toList();
        return new PageImpl<>(hits, query.getPageable(), searchHits.getTotalHits());
    }

    @Override
    public void index(RedditSubsCategorized entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), RedditSubsCategorized.class);
    }
}
