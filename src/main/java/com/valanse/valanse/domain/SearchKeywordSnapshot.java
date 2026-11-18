package com.valanse.valanse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "search_keyword_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_search_keyword_snapshot_keyword_date",
                columnNames = {"search_keyword_id", "collected_date"}
        )
)
@Getter
@NoArgsConstructor
public class SearchKeywordSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "search_keyword_id", nullable = false)
    private SearchKeyword searchKeyword;

    @Column(name = "collected_date", nullable = false)
    private LocalDate collectedDate;

    @Column(name = "monthly_search_volume")
    private Integer monthlySearchVolume;

    @Column
    private Integer impressions;

    @Column
    private Integer clicks;

    @Column(name = "avg_position", precision = 6, scale = 2)
    private BigDecimal avgPosition;

    public static SearchKeywordSnapshot of(SearchKeyword searchKeyword, LocalDate collectedDate,
                                           Integer monthlySearchVolume, Integer impressions, Integer clicks,
                                           BigDecimal avgPosition) {
        SearchKeywordSnapshot snapshot = new SearchKeywordSnapshot();
        snapshot.searchKeyword = searchKeyword;
        snapshot.collectedDate = collectedDate;
        snapshot.overwrite(monthlySearchVolume, impressions, clicks, avgPosition);
        return snapshot;
    }

    public void overwrite(Integer monthlySearchVolume, Integer impressions, Integer clicks,
                          BigDecimal avgPosition) {
        this.monthlySearchVolume = monthlySearchVolume;
        this.impressions = impressions;
        this.clicks = clicks;
        this.avgPosition = avgPosition;
    }
}
