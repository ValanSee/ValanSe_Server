package com.valanse.valanse.service.MemberBlockService;

import com.valanse.valanse.dto.MemberBlock.MemberBlockListResponse;

/**
 * 회원 차단 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface MemberBlockService {

    // 차단 (이미 차단돼 있어도 성공으로 처리)
    void block(Long blockerId, Long blockedId);

    // 차단 해제 (차단돼 있지 않아도 성공으로 처리)
    void unblock(Long blockerId, Long blockedId);

    MemberBlockListResponse getBlocks(Long blockerId);
}
