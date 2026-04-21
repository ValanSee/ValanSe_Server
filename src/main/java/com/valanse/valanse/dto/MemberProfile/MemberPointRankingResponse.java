package com.valanse.valanse.dto.MemberProfile;

public record MemberPointRankingResponse(
        String nickname,
        long point,
        int rank
) {
}
