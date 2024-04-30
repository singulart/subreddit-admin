package net.lex.reddit.subs.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.lex.reddit.subs.domain.RedditSubsCategorized;
import net.lex.reddit.subs.repository.RedditSubsCategorizedRepository;
import net.lex.reddit.subs.repository.search.RedditSubsCategorizedSearchRepository;
import net.lex.reddit.subs.web.rest.errors.BadRequestAlertException;
import net.lex.reddit.subs.web.rest.errors.ElasticsearchExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link net.lex.reddit.subs.domain.RedditSubsCategorized}.
 */
@RestController
@RequestMapping("/api/reddit-subs-categorizeds")
@Transactional
public class RedditSubsCategorizedResource {

    private final Logger log = LoggerFactory.getLogger(RedditSubsCategorizedResource.class);

    private static final String ENTITY_NAME = "redditSubsCategorized";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RedditSubsCategorizedRepository redditSubsCategorizedRepository;

    private final RedditSubsCategorizedSearchRepository redditSubsCategorizedSearchRepository;

    public RedditSubsCategorizedResource(
        RedditSubsCategorizedRepository redditSubsCategorizedRepository,
        RedditSubsCategorizedSearchRepository redditSubsCategorizedSearchRepository
    ) {
        this.redditSubsCategorizedRepository = redditSubsCategorizedRepository;
        this.redditSubsCategorizedSearchRepository = redditSubsCategorizedSearchRepository;
    }

    /**
     * {@code POST  /reddit-subs-categorizeds} : Create a new redditSubsCategorized.
     *
     * @param redditSubsCategorized the redditSubsCategorized to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new redditSubsCategorized, or with status {@code 400 (Bad Request)} if the redditSubsCategorized has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RedditSubsCategorized> createRedditSubsCategorized(
        @Valid @RequestBody RedditSubsCategorized redditSubsCategorized
    ) throws URISyntaxException {
        log.debug("REST request to save RedditSubsCategorized : {}", redditSubsCategorized);
        if (redditSubsCategorized.getId() != null) {
            throw new BadRequestAlertException("A new redditSubsCategorized cannot already have an ID", ENTITY_NAME, "idexists");
        }
        redditSubsCategorized = redditSubsCategorizedRepository.save(redditSubsCategorized);
        redditSubsCategorizedSearchRepository.index(redditSubsCategorized);
        return ResponseEntity.created(new URI("/api/reddit-subs-categorizeds/" + redditSubsCategorized.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, redditSubsCategorized.getId().toString()))
            .body(redditSubsCategorized);
    }

    /**
     * {@code PUT  /reddit-subs-categorizeds/:id} : Updates an existing redditSubsCategorized.
     *
     * @param id the id of the redditSubsCategorized to save.
     * @param redditSubsCategorized the redditSubsCategorized to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated redditSubsCategorized,
     * or with status {@code 400 (Bad Request)} if the redditSubsCategorized is not valid,
     * or with status {@code 500 (Internal Server Error)} if the redditSubsCategorized couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RedditSubsCategorized> updateRedditSubsCategorized(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RedditSubsCategorized redditSubsCategorized
    ) throws URISyntaxException {
        log.debug("REST request to update RedditSubsCategorized : {}, {}", id, redditSubsCategorized);
        if (redditSubsCategorized.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, redditSubsCategorized.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!redditSubsCategorizedRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        redditSubsCategorized = redditSubsCategorizedRepository.save(redditSubsCategorized);
        redditSubsCategorizedSearchRepository.index(redditSubsCategorized);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, redditSubsCategorized.getId().toString()))
            .body(redditSubsCategorized);
    }

    /**
     * {@code PATCH  /reddit-subs-categorizeds/:id} : Partial updates given fields of an existing redditSubsCategorized, field will ignore if it is null
     *
     * @param id the id of the redditSubsCategorized to save.
     * @param redditSubsCategorized the redditSubsCategorized to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated redditSubsCategorized,
     * or with status {@code 400 (Bad Request)} if the redditSubsCategorized is not valid,
     * or with status {@code 404 (Not Found)} if the redditSubsCategorized is not found,
     * or with status {@code 500 (Internal Server Error)} if the redditSubsCategorized couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RedditSubsCategorized> partialUpdateRedditSubsCategorized(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RedditSubsCategorized redditSubsCategorized
    ) throws URISyntaxException {
        log.debug("REST request to partial update RedditSubsCategorized partially : {}, {}", id, redditSubsCategorized);
        if (redditSubsCategorized.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, redditSubsCategorized.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!redditSubsCategorizedRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RedditSubsCategorized> result = redditSubsCategorizedRepository
            .findById(redditSubsCategorized.getId())
            .map(existingRedditSubsCategorized -> {
                if (redditSubsCategorized.getSub() != null) {
                    existingRedditSubsCategorized.setSub(redditSubsCategorized.getSub());
                }
                if (redditSubsCategorized.getCat() != null) {
                    existingRedditSubsCategorized.setCat(redditSubsCategorized.getCat());
                }
                if (redditSubsCategorized.getSubcat() != null) {
                    existingRedditSubsCategorized.setSubcat(redditSubsCategorized.getSubcat());
                }
                if (redditSubsCategorized.getNiche() != null) {
                    existingRedditSubsCategorized.setNiche(redditSubsCategorized.getNiche());
                }

                return existingRedditSubsCategorized;
            })
            .map(redditSubsCategorizedRepository::save)
            .map(savedRedditSubsCategorized -> {
                redditSubsCategorizedSearchRepository.index(savedRedditSubsCategorized);
                return savedRedditSubsCategorized;
            });

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, redditSubsCategorized.getId().toString())
        );
    }

    /**
     * {@code GET  /reddit-subs-categorizeds} : get all the redditSubsCategorizeds.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of redditSubsCategorizeds in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RedditSubsCategorized>> getAllRedditSubsCategorizeds(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get a page of RedditSubsCategorizeds");
        Page<RedditSubsCategorized> page = redditSubsCategorizedRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /reddit-subs-categorizeds/:id} : get the "id" redditSubsCategorized.
     *
     * @param id the id of the redditSubsCategorized to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the redditSubsCategorized, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RedditSubsCategorized> getRedditSubsCategorized(@PathVariable("id") Long id) {
        log.debug("REST request to get RedditSubsCategorized : {}", id);
        Optional<RedditSubsCategorized> redditSubsCategorized = redditSubsCategorizedRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(redditSubsCategorized);
    }

    /**
     * {@code DELETE  /reddit-subs-categorizeds/:id} : delete the "id" redditSubsCategorized.
     *
     * @param id the id of the redditSubsCategorized to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRedditSubsCategorized(@PathVariable("id") Long id) {
        log.debug("REST request to delete RedditSubsCategorized : {}", id);
        redditSubsCategorizedRepository.deleteById(id);
        redditSubsCategorizedSearchRepository.deleteFromIndexById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /reddit-subs-categorizeds/_search?query=:query} : search for the redditSubsCategorized corresponding
     * to the query.
     *
     * @param query the query of the redditSubsCategorized search.
     * @param pageable the pagination information.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public ResponseEntity<List<RedditSubsCategorized>> searchRedditSubsCategorizeds(
        @RequestParam("query") String query,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to search for a page of RedditSubsCategorizeds for query {}", query);
        try {
            Page<RedditSubsCategorized> page = redditSubsCategorizedSearchRepository.search(query, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
            return ResponseEntity.ok().headers(headers).body(page.getContent());
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
