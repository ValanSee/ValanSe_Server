package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.repository.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 오늘의 밸런스 포인트 지급 대상 여부를 판단하는 컴포넌트 코드입니다.
 * 봇은 givePoint 가 건너뛰고, 탈퇴했거나 프로필이 없는 회원은 givePoint 가 예외를 던져 호출 트랜잭션을 롤백시키므로 미리 제외합니다.
 */
@Component
@RequiredArgsConstructor
public class DailyVotePointEligibility {

    private final MemberProfileRepository memberProfileRepository;

    public boolean canReceive(Member member) {
        return !member.isBot()
                && member.getDeletedAt() == null
                && memberProfileRepository.existsByMemberId(member.getId());
    }
}
