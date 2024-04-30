package net.lex.reddit.subs.web.rest;

import static net.lex.reddit.subs.domain.RedditSubsCategorizedAsserts.*;
import static net.lex.reddit.subs.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.lex.reddit.subs.IntegrationTest;
import net.lex.reddit.subs.domain.RedditSubsCategorized;
import net.lex.reddit.subs.repository.RedditSubsCategorizedRepository;
import net.lex.reddit.subs.repository.search.RedditSubsCategorizedSearchRepository;
import org.assertj.core.util.IterableUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link RedditSubsCategorizedResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class RedditSubsCategorizedResourceIT {

    private static final String DEFAULT_SUB = "AAAAAAAAAA";
    private static final String UPDATED_SUB = "BBBBBBBBBB";

    private static final String DEFAULT_CAT = "AAAAAAAAAA";
    private static final String UPDATED_CAT = "BBBBBBBBBB";

    private static final String DEFAULT_SUBCAT = "AAAAAAAAAA";
    private static final String UPDATED_SUBCAT = "BBBBBBBBBB";

    private static final String DEFAULT_NICHE = "AAAAAAAAAA";
    private static final String UPDATED_NICHE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/reddit-subs-categorizeds";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/reddit-subs-categorizeds/_search";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RedditSubsCategorizedRepository redditSubsCategorizedRepository;

    @Autowired
    private RedditSubsCategorizedSearchRepository redditSubsCategorizedSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRedditSubsCategorizedMockMvc;

    private RedditSubsCategorized redditSubsCategorized;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RedditSubsCategorized createEntity(EntityManager em) {
        RedditSubsCategorized redditSubsCategorized = new RedditSubsCategorized()
            .sub(DEFAULT_SUB)
            .cat(DEFAULT_CAT)
            .subcat(DEFAULT_SUBCAT)
            .niche(DEFAULT_NICHE);
        return redditSubsCategorized;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RedditSubsCategorized createUpdatedEntity(EntityManager em) {
        RedditSubsCategorized redditSubsCategorized = new RedditSubsCategorized()
            .sub(UPDATED_SUB)
            .cat(UPDATED_CAT)
            .subcat(UPDATED_SUBCAT)
            .niche(UPDATED_NICHE);
        return redditSubsCategorized;
    }

    @AfterEach
    public void cleanupElasticSearchRepository() {
        redditSubsCategorizedSearchRepository.deleteAll();
        assertThat(redditSubsCategorizedSearchRepository.count()).isEqualTo(0);
    }

    @BeforeEach
    public void initTest() {
        redditSubsCategorized = createEntity(em);
    }

    @Test
    @Transactional
    void createRedditSubsCategorized() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        // Create the RedditSubsCategorized
        var returnedRedditSubsCategorized = om.readValue(
            restRedditSubsCategorizedMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redditSubsCategorized)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RedditSubsCategorized.class
        );

        // Validate the RedditSubsCategorized in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertRedditSubsCategorizedUpdatableFieldsEquals(
            returnedRedditSubsCategorized,
            getPersistedRedditSubsCategorized(returnedRedditSubsCategorized)
        );

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });
    }

    @Test
    @Transactional
    void createRedditSubsCategorizedWithExistingId() throws Exception {
        // Create the RedditSubsCategorized with an existing ID
        redditSubsCategorized.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restRedditSubsCategorizedMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redditSubsCategorized)))
            .andExpect(status().isBadRequest());

        // Validate the RedditSubsCategorized in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkSubIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        // set the field null
        redditSubsCategorized.setSub(null);

        // Create the RedditSubsCategorized, which fails.

        restRedditSubsCategorizedMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redditSubsCategorized)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkCatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        // set the field null
        redditSubsCategorized.setCat(null);

        // Create the RedditSubsCategorized, which fails.

        restRedditSubsCategorizedMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redditSubsCategorized)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkSubcatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        // set the field null
        redditSubsCategorized.setSubcat(null);

        // Create the RedditSubsCategorized, which fails.

        restRedditSubsCategorizedMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redditSubsCategorized)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkNicheIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        // set the field null
        redditSubsCategorized.setNiche(null);

        // Create the RedditSubsCategorized, which fails.

        restRedditSubsCategorizedMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redditSubsCategorized)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllRedditSubsCategorizeds() throws Exception {
        // Initialize the database
        redditSubsCategorizedRepository.saveAndFlush(redditSubsCategorized);

        // Get all the redditSubsCategorizedList
        restRedditSubsCategorizedMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(redditSubsCategorized.getId().intValue())))
            .andExpect(jsonPath("$.[*].sub").value(hasItem(DEFAULT_SUB)))
            .andExpect(jsonPath("$.[*].cat").value(hasItem(DEFAULT_CAT)))
            .andExpect(jsonPath("$.[*].subcat").value(hasItem(DEFAULT_SUBCAT)))
            .andExpect(jsonPath("$.[*].niche").value(hasItem(DEFAULT_NICHE)));
    }

    @Test
    @Transactional
    void getRedditSubsCategorized() throws Exception {
        // Initialize the database
        redditSubsCategorizedRepository.saveAndFlush(redditSubsCategorized);

        // Get the redditSubsCategorized
        restRedditSubsCategorizedMockMvc
            .perform(get(ENTITY_API_URL_ID, redditSubsCategorized.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(redditSubsCategorized.getId().intValue()))
            .andExpect(jsonPath("$.sub").value(DEFAULT_SUB))
            .andExpect(jsonPath("$.cat").value(DEFAULT_CAT))
            .andExpect(jsonPath("$.subcat").value(DEFAULT_SUBCAT))
            .andExpect(jsonPath("$.niche").value(DEFAULT_NICHE));
    }

    @Test
    @Transactional
    void getNonExistingRedditSubsCategorized() throws Exception {
        // Get the redditSubsCategorized
        restRedditSubsCategorizedMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRedditSubsCategorized() throws Exception {
        // Initialize the database
        redditSubsCategorizedRepository.saveAndFlush(redditSubsCategorized);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        redditSubsCategorizedSearchRepository.save(redditSubsCategorized);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());

        // Update the redditSubsCategorized
        RedditSubsCategorized updatedRedditSubsCategorized = redditSubsCategorizedRepository
            .findById(redditSubsCategorized.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedRedditSubsCategorized are not directly saved in db
        em.detach(updatedRedditSubsCategorized);
        updatedRedditSubsCategorized.sub(UPDATED_SUB).cat(UPDATED_CAT).subcat(UPDATED_SUBCAT).niche(UPDATED_NICHE);

        restRedditSubsCategorizedMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedRedditSubsCategorized.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedRedditSubsCategorized))
            )
            .andExpect(status().isOk());

        // Validate the RedditSubsCategorized in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRedditSubsCategorizedToMatchAllProperties(updatedRedditSubsCategorized);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<RedditSubsCategorized> redditSubsCategorizedSearchList = Streamable.of(
                    redditSubsCategorizedSearchRepository.findAll()
                ).toList();
                RedditSubsCategorized testRedditSubsCategorizedSearch = redditSubsCategorizedSearchList.get(searchDatabaseSizeAfter - 1);

                assertRedditSubsCategorizedAllPropertiesEquals(testRedditSubsCategorizedSearch, updatedRedditSubsCategorized);
            });
    }

    @Test
    @Transactional
    void putNonExistingRedditSubsCategorized() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        redditSubsCategorized.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRedditSubsCategorizedMockMvc
            .perform(
                put(ENTITY_API_URL_ID, redditSubsCategorized.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(redditSubsCategorized))
            )
            .andExpect(status().isBadRequest());

        // Validate the RedditSubsCategorized in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchRedditSubsCategorized() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        redditSubsCategorized.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRedditSubsCategorizedMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(redditSubsCategorized))
            )
            .andExpect(status().isBadRequest());

        // Validate the RedditSubsCategorized in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRedditSubsCategorized() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        redditSubsCategorized.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRedditSubsCategorizedMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redditSubsCategorized)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RedditSubsCategorized in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateRedditSubsCategorizedWithPatch() throws Exception {
        // Initialize the database
        redditSubsCategorizedRepository.saveAndFlush(redditSubsCategorized);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the redditSubsCategorized using partial update
        RedditSubsCategorized partialUpdatedRedditSubsCategorized = new RedditSubsCategorized();
        partialUpdatedRedditSubsCategorized.setId(redditSubsCategorized.getId());

        partialUpdatedRedditSubsCategorized.cat(UPDATED_CAT).subcat(UPDATED_SUBCAT);

        restRedditSubsCategorizedMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRedditSubsCategorized.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRedditSubsCategorized))
            )
            .andExpect(status().isOk());

        // Validate the RedditSubsCategorized in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRedditSubsCategorizedUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRedditSubsCategorized, redditSubsCategorized),
            getPersistedRedditSubsCategorized(redditSubsCategorized)
        );
    }

    @Test
    @Transactional
    void fullUpdateRedditSubsCategorizedWithPatch() throws Exception {
        // Initialize the database
        redditSubsCategorizedRepository.saveAndFlush(redditSubsCategorized);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the redditSubsCategorized using partial update
        RedditSubsCategorized partialUpdatedRedditSubsCategorized = new RedditSubsCategorized();
        partialUpdatedRedditSubsCategorized.setId(redditSubsCategorized.getId());

        partialUpdatedRedditSubsCategorized.sub(UPDATED_SUB).cat(UPDATED_CAT).subcat(UPDATED_SUBCAT).niche(UPDATED_NICHE);

        restRedditSubsCategorizedMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRedditSubsCategorized.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRedditSubsCategorized))
            )
            .andExpect(status().isOk());

        // Validate the RedditSubsCategorized in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRedditSubsCategorizedUpdatableFieldsEquals(
            partialUpdatedRedditSubsCategorized,
            getPersistedRedditSubsCategorized(partialUpdatedRedditSubsCategorized)
        );
    }

    @Test
    @Transactional
    void patchNonExistingRedditSubsCategorized() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        redditSubsCategorized.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRedditSubsCategorizedMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, redditSubsCategorized.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(redditSubsCategorized))
            )
            .andExpect(status().isBadRequest());

        // Validate the RedditSubsCategorized in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRedditSubsCategorized() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        redditSubsCategorized.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRedditSubsCategorizedMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(redditSubsCategorized))
            )
            .andExpect(status().isBadRequest());

        // Validate the RedditSubsCategorized in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRedditSubsCategorized() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        redditSubsCategorized.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRedditSubsCategorizedMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(redditSubsCategorized)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RedditSubsCategorized in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteRedditSubsCategorized() throws Exception {
        // Initialize the database
        redditSubsCategorizedRepository.saveAndFlush(redditSubsCategorized);
        redditSubsCategorizedRepository.save(redditSubsCategorized);
        redditSubsCategorizedSearchRepository.save(redditSubsCategorized);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the redditSubsCategorized
        restRedditSubsCategorizedMockMvc
            .perform(delete(ENTITY_API_URL_ID, redditSubsCategorized.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redditSubsCategorizedSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchRedditSubsCategorized() throws Exception {
        // Initialize the database
        redditSubsCategorized = redditSubsCategorizedRepository.saveAndFlush(redditSubsCategorized);
        redditSubsCategorizedSearchRepository.save(redditSubsCategorized);

        // Search the redditSubsCategorized
        restRedditSubsCategorizedMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + redditSubsCategorized.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(redditSubsCategorized.getId().intValue())))
            .andExpect(jsonPath("$.[*].sub").value(hasItem(DEFAULT_SUB)))
            .andExpect(jsonPath("$.[*].cat").value(hasItem(DEFAULT_CAT)))
            .andExpect(jsonPath("$.[*].subcat").value(hasItem(DEFAULT_SUBCAT)))
            .andExpect(jsonPath("$.[*].niche").value(hasItem(DEFAULT_NICHE)));
    }

    protected long getRepositoryCount() {
        return redditSubsCategorizedRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected RedditSubsCategorized getPersistedRedditSubsCategorized(RedditSubsCategorized redditSubsCategorized) {
        return redditSubsCategorizedRepository.findById(redditSubsCategorized.getId()).orElseThrow();
    }

    protected void assertPersistedRedditSubsCategorizedToMatchAllProperties(RedditSubsCategorized expectedRedditSubsCategorized) {
        assertRedditSubsCategorizedAllPropertiesEquals(
            expectedRedditSubsCategorized,
            getPersistedRedditSubsCategorized(expectedRedditSubsCategorized)
        );
    }

    protected void assertPersistedRedditSubsCategorizedToMatchUpdatableProperties(RedditSubsCategorized expectedRedditSubsCategorized) {
        assertRedditSubsCategorizedAllUpdatablePropertiesEquals(
            expectedRedditSubsCategorized,
            getPersistedRedditSubsCategorized(expectedRedditSubsCategorized)
        );
    }
}
