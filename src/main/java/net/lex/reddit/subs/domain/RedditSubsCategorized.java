package net.lex.reddit.subs.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A RedditSubsCategorized.
 */
@Entity
@Table(name = "reddit_subs_categorized")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@org.springframework.data.elasticsearch.annotations.Document(indexName = "redditsubscategorized")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RedditSubsCategorized implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 64)
    @Column(name = "sub", length = 64, nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String sub;

    @NotNull
    @Size(max = 64)
    @Column(name = "cat", length = 64, nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String cat;

    @NotNull
    @Size(max = 64)
    @Column(name = "subcat", length = 64, nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String subcat;

    @NotNull
    @Size(max = 64)
    @Column(name = "niche", length = 64, nullable = false)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Text)
    private String niche;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public RedditSubsCategorized id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSub() {
        return this.sub;
    }

    public RedditSubsCategorized sub(String sub) {
        this.setSub(sub);
        return this;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getCat() {
        return this.cat;
    }

    public RedditSubsCategorized cat(String cat) {
        this.setCat(cat);
        return this;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }

    public String getSubcat() {
        return this.subcat;
    }

    public RedditSubsCategorized subcat(String subcat) {
        this.setSubcat(subcat);
        return this;
    }

    public void setSubcat(String subcat) {
        this.subcat = subcat;
    }

    public String getNiche() {
        return this.niche;
    }

    public RedditSubsCategorized niche(String niche) {
        this.setNiche(niche);
        return this;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedditSubsCategorized)) {
            return false;
        }
        return getId() != null && getId().equals(((RedditSubsCategorized) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RedditSubsCategorized{" +
            "id=" + getId() +
            ", sub='" + getSub() + "'" +
            ", cat='" + getCat() + "'" +
            ", subcat='" + getSubcat() + "'" +
            ", niche='" + getNiche() + "'" +
            "}";
    }
}
