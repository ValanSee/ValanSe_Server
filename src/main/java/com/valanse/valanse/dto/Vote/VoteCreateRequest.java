package com.valanse.valanse.dto.Vote;// src/main/java/com/valanse/valanse/dto/vote/VoteCreateRequest.java


import com.valanse.valanse.domain.enums.VoteCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class VoteCreateRequest {
    private String title;
    private List<String> options; // 최대 4개 옵션을 받을 수 있도록 List<String>으로 정의
    private VoteCategory category; // ERD의 Vote 테이블 category와 매핑
}