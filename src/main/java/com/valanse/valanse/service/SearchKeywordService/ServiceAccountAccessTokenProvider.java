package com.valanse.valanse.service.SearchKeywordService;

import com.google.auth.oauth2.GoogleCredentials;
import com.valanse.valanse.common.config.SearchKeywordProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * Resolves credentials lazily so that an empty service-account JSON never breaks application startup.
 */
@Component
public class ServiceAccountAccessTokenProvider implements GoogleAccessTokenProvider {

    private static final String SCOPE = "https://www.googleapis.com/auth/webmasters.readonly";

    private final SearchKeywordProperties properties;
    private GoogleCredentials credentials;

    public ServiceAccountAccessTokenProvider(SearchKeywordProperties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized String accessToken() {
        try {
            if (credentials == null) {
                byte[] json = Base64.getDecoder().decode(properties.getGoogle().getServiceAccountJsonBase64().trim());
                credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(json))
                        .createScoped(List.of(SCOPE));
            }
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to obtain Google access token", e);
        }
    }
}
