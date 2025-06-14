package com.valanse.valanse.service.AuthService;

import java.util.Map;

public interface AuthService {
    void logout();
    Map<String, String> reissueAccessToken(String requestRefreshToken);
}
