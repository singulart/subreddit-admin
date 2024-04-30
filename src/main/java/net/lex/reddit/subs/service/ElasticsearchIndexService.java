package net.lex.reddit.subs.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import net.lex.reddit.subs.domain.RedditSubsCategorized;
import net.lex.reddit.subs.repository.RedditSubsCategorizedRepository;
import net.lex.reddit.subs.repository.UserRepository;
import net.lex.reddit.subs.repository.search.RedditSubsCategorizedSearchRepository;
import net.lex.reddit.subs.repository.search.UserSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ElasticsearchIndexService {

    private static final Lock reindexLock = new ReentrantLock();

    private final Logger log = LoggerFactory.getLogger(ElasticsearchIndexService.class);

    private final RedditSubsCategorizedRepository redditSubsCategorizedRepository;

    private final RedditSubsCategorizedSearchRepository redditSubsCategorizedSearchRepository;

    // private final UserRepository userRepository;

    // private final UserSearchRepository userSearchRepository;

    // private final ElasticsearchTemplate elasticsearchTemplate;

    public ElasticsearchIndexService(
        UserRepository userRepository,
        UserSearchRepository userSearchRepository,
        RedditSubsCategorizedRepository redditSubsCategorizedRepository,
        RedditSubsCategorizedSearchRepository redditSubsCategorizedSearchRepository,
        ElasticsearchTemplate elasticsearchTemplate
    ) {
        // this.userRepository = userRepository;
        // this.userSearchRepository = userSearchRepository;
        this.redditSubsCategorizedRepository = redditSubsCategorizedRepository;
        this.redditSubsCategorizedSearchRepository = redditSubsCategorizedSearchRepository;
        // this.elasticsearchTemplate = elasticsearchTemplate;
    }

    @Async
    public void reindexAll() {
        if (reindexLock.tryLock()) {
            try {
                reindexForClass(RedditSubsCategorized.class, redditSubsCategorizedRepository, redditSubsCategorizedSearchRepository);
                // if (!skipUserManagement && (!applicationType || applicationType === 'monolith' || applicationType === 'gateway')) {
                //     reindexForClass(User.class, userRepository, userSearchRepository);
                // }

                log.info("Elasticsearch: Successfully performed reindexing");
            } finally {
                reindexLock.unlock();
            }
        } else {
            log.info("Elasticsearch: concurrent reindexing attempt");
        }
    }

    @SuppressWarnings("unchecked")
    private <T, ID extends Serializable> void reindexForClass(
        Class<T> entityClass,
        JpaRepository<T, ID> jpaRepository,
        ElasticsearchRepository<T, ID> elasticsearchRepository
    ) {
        // elasticsearchTemplate.deleteIndex(entityClass);
        // try {
        //     elasticsearchTemplate.createIndex(entityClass);
        // <%_ if (useResourceException) { _%>
        // } catch (ResourceAlreadyExistsException e) {
        // <%_ } else { _%>
        // } catch (IndexAlreadyExistsException e) {
        // <%_ } _%>
        //     // Do nothing. Index was already concurrently recreated by some other service.
        // }
        // elasticsearchTemplate.putMapping(entityClass);
        if (jpaRepository.count() > 0) {
            // if a JHipster entity field is the owner side of a many-to-many relationship, it should be loaded manually
            List<Method> relationshipGetters = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.getType().equals(Set.class))
                .filter(field -> field.getAnnotation(JsonIgnore.class) == null)
                .map(field -> {
                    try {
                        return new PropertyDescriptor(field.getName(), entityClass).getReadMethod();
                    } catch (IntrospectionException e) {
                        log.error(
                            "Error retrieving getter for class {}, field {}. Field will NOT be indexed",
                            entityClass.getSimpleName(),
                            field.getName(),
                            e
                        );
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            int size = 1000;
            for (int i = 0; i < jpaRepository.count() / size; i++) {
                Pageable page = PageRequest.of(i, size);
                log.info("Indexing page {} of {}, size {}", i, jpaRepository.count() / size, size);
                Page<T> results = jpaRepository.findAll(page);
                results.map(result -> {
                    // if there are any relationships to load, do it now
                    relationshipGetters.forEach(method -> {
                        try {
                            // eagerly load the relationship set
                            ((Set) method.invoke(result)).size();
                        } catch (Exception ex) {
                            log.error(ex.getMessage());
                        }
                    });
                    return result;
                });
                elasticsearchRepository.saveAll(results.getContent());
            }
        }
        log.info("Elasticsearch: Indexed all rows for {}", entityClass.getSimpleName());
    }
}
