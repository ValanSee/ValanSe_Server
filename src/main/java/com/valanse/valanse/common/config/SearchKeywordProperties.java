package com.valanse.valanse.common.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated
@Component
@ConfigurationProperties(prefix = "search-keyword")
public class SearchKeywordProperties {

    private boolean enabled = false;

    @NotBlank
    private String cron = "0 0 3 * * *";

    @NotBlank
    private String zone = "Asia/Seoul";

    @Positive
    private int freshnessDays = 14;

    @Positive
    private int reuseCooldownDays = 56;

    private List<String> brandTerms = new ArrayList<>(List.of("발란세", "valanse", "valsense", "valance"));

    private final Naver naver = new Naver();

    private final Google google = new Google();

    private final Prompt prompt = new Prompt();

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

    public int getFreshnessDays() {
        return freshnessDays;
    }

    public void setFreshnessDays(int freshnessDays) {
        this.freshnessDays = freshnessDays;
    }

    public int getReuseCooldownDays() {
        return reuseCooldownDays;
    }

    public void setReuseCooldownDays(int reuseCooldownDays) {
        this.reuseCooldownDays = reuseCooldownDays;
    }

    public List<String> getBrandTerms() {
        return brandTerms;
    }

    public void setBrandTerms(List<String> brandTerms) {
        this.brandTerms = brandTerms;
    }

    public Naver getNaver() {
        return naver;
    }

    public Google getGoogle() {
        return google;
    }

    public Prompt getPrompt() {
        return prompt;
    }

    public static class Naver {

        private String apiKey = "";

        private String secretKey = "";

        private String customerId = "";

        private List<String> hintKeywords = new ArrayList<>(List.of(
                "밸런스게임", "커플밸런스게임", "연애밸런스게임", "음식밸런스게임", "친구밸런스게임"));

        @Positive
        private int maxKeywords = 200;

        public boolean isConfigured() {
            return hasText(apiKey) && hasText(secretKey) && hasText(customerId);
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public List<String> getHintKeywords() {
            return hintKeywords;
        }

        public void setHintKeywords(List<String> hintKeywords) {
            this.hintKeywords = hintKeywords;
        }

        public int getMaxKeywords() {
            return maxKeywords;
        }

        public void setMaxKeywords(int maxKeywords) {
            this.maxKeywords = maxKeywords;
        }
    }

    public static class Google {

        private String serviceAccountJsonBase64 = "";

        @NotBlank
        private String siteUrl = "https://valanse.kr/";

        @Positive
        private int maxKeywords = 200;

        public boolean isConfigured() {
            return hasText(serviceAccountJsonBase64);
        }

        public String getServiceAccountJsonBase64() {
            return serviceAccountJsonBase64;
        }

        public void setServiceAccountJsonBase64(String serviceAccountJsonBase64) {
            this.serviceAccountJsonBase64 = serviceAccountJsonBase64;
        }

        public String getSiteUrl() {
            return siteUrl;
        }

        public void setSiteUrl(String siteUrl) {
            this.siteUrl = siteUrl;
        }

        public int getMaxKeywords() {
            return maxKeywords;
        }

        public void setMaxKeywords(int maxKeywords) {
            this.maxKeywords = maxKeywords;
        }
    }

    public static class Prompt {

        @PositiveOrZero
        private int naverLimit = 15;

        @PositiveOrZero
        private int googleLimit = 10;

        @Positive
        private int maxPostsPerRun = 1;

        public int getNaverLimit() {
            return naverLimit;
        }

        public void setNaverLimit(int naverLimit) {
            this.naverLimit = naverLimit;
        }

        public int getGoogleLimit() {
            return googleLimit;
        }

        public void setGoogleLimit(int googleLimit) {
            this.googleLimit = googleLimit;
        }

        public int getMaxPostsPerRun() {
            return maxPostsPerRun;
        }

        public void setMaxPostsPerRun(int maxPostsPerRun) {
            this.maxPostsPerRun = maxPostsPerRun;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
