package com.valanse.valanse.service.ContentSeedService;

import java.util.Map;

// REQ-001: 봇의 관심사·말투 힌트를 닉네임이 아니라 안정적인 봇 식별자(social_id)로
// 관리한다. 닉네임을 바꿔도 관심사 매핑이 끊어지지 않도록 하기 위함이다.
// MemberProfile에는 이 정보를 저장할 필드가 없어 여기서 정적으로 관리한다 - 5개
// 봇 social_id가 생성 카테고리(FOOD/LOVE/BUY/SPORT/WORRY) 5개와 각각 대응한다.
final class BotPersonaCatalog {

    record BotPersonaTraits(String interestHint, String toneExample) {
    }

    private static final BotPersonaTraits EMPTY = new BotPersonaTraits("", "");

    private static final Map<String, BotPersonaTraits> TRAITS_BY_SOCIAL_ID = Map.of(
            "content-seed-bot-001", new BotPersonaTraits("음식", "친근하고 편한 반말 말투, 또박또박 설명하는 편"),
            "content-seed-bot-002", new BotPersonaTraits("연애", "공감형, 상대 입장을 먼저 헤아리는 존댓말 말투"),
            "content-seed-bot-003", new BotPersonaTraits("소비·쇼핑", "분석적이고 간결한 존댓말 말투, 근거를 먼저 말함"),
            "content-seed-bot-004", new BotPersonaTraits("스포츠·승부", "활기차고 단정적인 반말 말투, 승부욕이 섞인 표현"),
            "content-seed-bot-005", new BotPersonaTraits("고민·선택", "느긋하고 신중한 반말 말투, 섣불리 단정 짓지 않음")
    );

    private BotPersonaCatalog() {
    }

    static BotPersonaTraits traitsFor(String socialId) {
        if (socialId == null) {
            return EMPTY;
        }
        return TRAITS_BY_SOCIAL_ID.getOrDefault(socialId, EMPTY);
    }
}
