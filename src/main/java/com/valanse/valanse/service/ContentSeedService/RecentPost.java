package com.valanse.valanse.service.ContentSeedService;

// 게시글 생성 프롬프트에 참고 자료로 제공하는 최근 활성 게시글 정보.
// 제목만으로는 Claude가 "최근 생성 내역 기준으로 카테고리를 고르게 배분"할 수
// 없으므로 카테고리를 함께 전달한다.
public record RecentPost(String title, GeneratableVoteCategory category) {
}
