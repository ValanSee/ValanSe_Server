package com.valanse.valanse.service.ContentSeedService;

// 생성 항목(게시글 1개 또는 상호작용 1개) 단위 실패 기록.
// detail은 로그/Discord 알림에 노출해도 안전하도록 마스킹·길이 제한이 끝난 값이어야 한다.
public record ContentSeedItemFailure(String detail, String reason) {
}
