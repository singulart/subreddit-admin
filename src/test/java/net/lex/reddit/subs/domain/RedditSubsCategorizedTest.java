package net.lex.reddit.subs.domain;

import static net.lex.reddit.subs.domain.RedditSubsCategorizedTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import net.lex.reddit.subs.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RedditSubsCategorizedTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RedditSubsCategorized.class);
        RedditSubsCategorized redditSubsCategorized1 = getRedditSubsCategorizedSample1();
        RedditSubsCategorized redditSubsCategorized2 = new RedditSubsCategorized();
        assertThat(redditSubsCategorized1).isNotEqualTo(redditSubsCategorized2);

        redditSubsCategorized2.setId(redditSubsCategorized1.getId());
        assertThat(redditSubsCategorized1).isEqualTo(redditSubsCategorized2);

        redditSubsCategorized2 = getRedditSubsCategorizedSample2();
        assertThat(redditSubsCategorized1).isNotEqualTo(redditSubsCategorized2);
    }
}
