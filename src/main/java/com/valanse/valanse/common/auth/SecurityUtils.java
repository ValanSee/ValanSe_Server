package com.valanse.valanse.common.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 현재 SecurityContext의 인증 정보를 읽는 유틸 코드입니다.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /**
     * 현재 요청의 로그인 회원 id를 반환합니다. 비로그인이거나 회원 id 형식이 아닌 주체면 null을 반환합니다.
     */
    public static Long currentMemberIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return null;
        }
        try {
            return Long.parseLong(name);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
