package com.valanse.valanse.domain;

import com.valanse.valanse.domain.enums.SearchKeywordSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "search_keyword",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_search_keyword_source_normalized",
                columnNames = {"source", "normalized_keyword"}
        ),
        indexes = @Index(name = "idx_search_keyword_last_used_at", columnList = "last_used_at")
)
@Getter
@NoArgsConstructor
public class SearchKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private SearchKeywordSource source;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(name = "normalized_keyword", nullable = false, length = 100)
    private String normalizedKeyword;

    @Column(name = "monthly_search_volume")
    private Integer monthlySearchVolume;

    @Column
    private Integer impressions;

    @Column
    private Integer clicks;

    @Column(name = "avg_position", precision = 6, scale = 2)
    private BigDecimal avgPosition;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SearchKeyword create(SearchKeywordSource source, String keyword, String normalized,
                                       LocalDateTime now) {
        SearchKeyword entity = new SearchKeyword();
        entity.source = source;
        entity.keyword = keyword;
        entity.normalizedKeyword = normalized;
        entity.firstSeenAt = now;
        entity.lastSeenAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void refreshMetrics(Integer monthlySearchVolume, Integer impressions, Integer clicks,
                               BigDecimal avgPosition, LocalDateTime now) {
        this.monthlySearchVolume = monthlySearchVolume;
        this.impressions = impressions;
        this.clicks = clicks;
        this.avgPosition = avgPosition;
        this.lastSeenAt = now;
        this.updatedAt = now;
    }

    public void markUsed(LocalDateTime now) {
        this.lastUsedAt = now;
        this.updatedAt = now;
    }
}
