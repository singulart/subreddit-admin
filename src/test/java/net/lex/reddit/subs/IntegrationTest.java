package net.lex.reddit.subs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.lex.reddit.subs.config.AsyncSyncConfiguration;
import net.lex.reddit.subs.config.EmbeddedElasticsearch;
import net.lex.reddit.subs.config.EmbeddedSQL;
import net.lex.reddit.subs.config.JacksonConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = { SubredditsAdminApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class })
@EmbeddedElasticsearch
@EmbeddedSQL
public @interface IntegrationTest {
}
