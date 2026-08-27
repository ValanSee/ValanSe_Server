package com.valanse.valanse.service.ContentSeedService;

import java.util.List;

// 투표+댓글 생성 대상 게시글. 호출자는 회원 ID·닉네임·프로필 등 개인 식별 정보를
// 절대 담지 않고, recentTopComments는 이메일·전화번호 마스킹까지 마친 텍스트만 전달한다.
public record CandidatePost(
        Long id,
        String title,
        String body,
        String optionA,
        String optionB,
        List<String> recentTopComments
) {
}
