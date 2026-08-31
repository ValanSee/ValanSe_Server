package com.valanse.valanse.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@Component
@ConfigurationProperties(prefix = "content-seed")
public class ContentSeedProperties {

    // 시드된 봇 계정 수 (member_profile 5쌍). botsPerRun의 상한.
    public static final int SEEDED_BOT_COUNT = 5;

    private boolean enabled = false;

    @NotBlank
    private String cron = "0 0 4 * * MON";

    @NotBlank
    private String zone = "Asia/Seoul";

    @NotBlank
    private String model = "claude-sonnet-5";

    @Min(1)
    @Max(SEEDED_BOT_COUNT)
    private int botsPerRun = 2;

    @Positive
    private int postsPerBot = 3;

    @Positive
    private int interactionsPerBot = 5;

    @Positive
    private int recentTitleLimit = 30;

    @Positive
    private int targetVoteLookbackDays = 7;

    @PositiveOrZero
    private int commentContextLimit = 5;

    @PositiveOrZero
    private int maxQualityRetries = 1;

    @Positive
    private int maxCommentsPerVote = 2;

    private String frontendBaseUrl = "";

    private final Pricing pricing = new Pricing();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getBotsPerRun() {
        return botsPerRun;
    }

    public void setBotsPerRun(int botsPerRun) {
        this.botsPerRun = botsPerRun;
    }

    public int getPostsPerBot() {
        return postsPerBot;
    }

    public void setPostsPerBot(int postsPerBot) {
        this.postsPerBot = postsPerBot;
    }

    public int getInteractionsPerBot() {
        return interactionsPerBot;
    }

    public void setInteractionsPerBot(int interactionsPerBot) {
        this.interactionsPerBot = interactionsPerBot;
    }

    public int getRecentTitleLimit() {
        return recentTitleLimit;
    }

    public void setRecentTitleLimit(int recentTitleLimit) {
        this.recentTitleLimit = recentTitleLimit;
    }

    public int getTargetVoteLookbackDays() {
        return targetVoteLookbackDays;
    }

    public void setTargetVoteLookbackDays(int targetVoteLookbackDays) {
        this.targetVoteLookbackDays = targetVoteLookbackDays;
    }

    public int getCommentContextLimit() {
        return commentContextLimit;
    }

    public void setCommentContextLimit(int commentContextLimit) {
        this.commentContextLimit = commentContextLimit;
    }

    public int getMaxQualityRetries() {
        return maxQualityRetries;
    }

    public void setMaxQualityRetries(int maxQualityRetries) {
        this.maxQualityRetries = maxQualityRetries;
    }

    public int getMaxCommentsPerVote() {
        return maxCommentsPerVote;
    }

    public void setMaxCommentsPerVote(int maxCommentsPerVote) {
        this.maxCommentsPerVote = maxCommentsPerVote;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public Pricing getPricing() {
        return pricing;
    }

    public static class Pricing {

        // Claude Sonnet 5 표준 단가(2026-08 기준, platform.claude.com/docs/en/about-claude/pricing).
        // 요금이 바뀌면 이 기본값도 함께 갱신해야 한다.
        @Positive
        private BigDecimal inputTokenPricePerMillionUsd = new BigDecimal("2.00");

        @Positive
        private BigDecimal outputTokenPricePerMillionUsd = new BigDecimal("10.00");

        public BigDecimal getInputTokenPricePerMillionUsd() {
            return inputTokenPricePerMillionUsd;
        }

        public void setInputTokenPricePerMillionUsd(BigDecimal inputTokenPricePerMillionUsd) {
            this.inputTokenPricePerMillionUsd = inputTokenPricePerMillionUsd;
        }

        public BigDecimal getOutputTokenPricePerMillionUsd() {
            return outputTokenPricePerMillionUsd;
        }

        public void setOutputTokenPricePerMillionUsd(BigDecimal outputTokenPricePerMillionUsd) {
            this.outputTokenPricePerMillionUsd = outputTokenPricePerMillionUsd;
        }
    }
}
