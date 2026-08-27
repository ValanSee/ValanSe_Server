package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.domain.enums.VoteCategory;

// Claude 구조화 출력 스키마에 사용하는 카테고리. VoteCategory와 달리 ALL이 없어서,
// 설명(prompt) 문구가 아니라 JSON Schema의 enum 값 자체에서 ALL을 제외한다.
public enum GeneratableVoteCategory {
    FOOD, LOVE, BUY, SPORT, WORRY, ETC;

    public VoteCategory toVoteCategory() {
        return VoteCategory.valueOf(name());
    }
}
