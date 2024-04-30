package net.lex.reddit.subs.repository;

import net.lex.reddit.subs.domain.RedditSubsCategorized;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RedditSubsCategorized entity.
 */
@SuppressWarnings("unused")
@Repository
public interface RedditSubsCategorizedRepository extends JpaRepository<RedditSubsCategorized, Long> {}
