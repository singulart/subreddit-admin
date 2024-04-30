package net.lex.reddit.subs.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class RedditSubsCategorizedTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static RedditSubsCategorized getRedditSubsCategorizedSample1() {
        return new RedditSubsCategorized().id(1L).sub("sub1").cat("cat1").subcat("subcat1").niche("niche1");
    }

    public static RedditSubsCategorized getRedditSubsCategorizedSample2() {
        return new RedditSubsCategorized().id(2L).sub("sub2").cat("cat2").subcat("subcat2").niche("niche2");
    }

    public static RedditSubsCategorized getRedditSubsCategorizedRandomSampleGenerator() {
        return new RedditSubsCategorized()
            .id(longCount.incrementAndGet())
            .sub(UUID.randomUUID().toString())
            .cat(UUID.randomUUID().toString())
            .subcat(UUID.randomUUID().toString())
            .niche(UUID.randomUUID().toString());
    }
}
