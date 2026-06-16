package com.valanse.valanse.dto.MemberProfile;

import com.valanse.valanse.domain.enums.Age;
import com.valanse.valanse.domain.enums.Gender;
import com.valanse.valanse.domain.enums.MbtiIe;
import com.valanse.valanse.domain.enums.MbtiTf;

/**
 * MemberProfileRequest API 요청 값을 전달하는 DTO 코드입니다.
 */
public record MemberProfileRequest(
        String nickname,
        Gender gender,
        Age age,
        MbtiIe mbtiIe,
        MbtiTf mbtiTf,
        String mbti
) {}