package com.valanse.valanse.dto.MemberProfile;

import java.util.List;

public record MemberPointRankingResponse (
        List<Info> profiles
) {
    public record Info(
            String nickname,
            long point
    ){}
}
