package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.domain.enums.Age;
import com.valanse.valanse.domain.enums.Gender;

// 프롬프트 작성에 필요한 봇 페르소나 정보. 호출자가 Member/MemberProfile로부터 조립한다.
public record BotPersonaContext(
        String nickname,
        Gender gender,
        Age age,
        String mbti,
        String interestHint
) {
}
