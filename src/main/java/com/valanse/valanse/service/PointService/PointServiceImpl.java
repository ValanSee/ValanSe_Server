package com.valanse.valanse.service.PointService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.PointHistory;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PointServiceImpl implements PointService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 포인트 정책
    private static final long SIGN_UP_POINT = 40L;
    private static final long POST_CREATE_POINT = 5L;
    private static final long COMMENT_CREATE_POINT = 1L;
    private static final long POST_VOTED_POINT = 1L;
    private static final long HOT_ISSUE_POINT = 50L;

    // 댓글 포인트 일일 최대 획득 횟수
    private static final long COMMENT_DAILY_LIMIT = 3L;

    @Override
    public void givePoint(Long memberId, PointType type) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        MemberProfile profile = memberProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다."));

        long amount = switch (type) {
            case SIGN_UP -> SIGN_UP_POINT;
            case POST_CREATE -> POST_CREATE_POINT;
            case COMMENT_CREATE -> {
                // 오늘 자정부터 현재까지 댓글 포인트 획득 횟수 체크
                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                long todayCount = pointHistoryRepository.countByMemberIdAndTypeAndCreatedAtAfter(
                        memberId, PointType.COMMENT_CREATE, todayStart
                );
                if (todayCount >= COMMENT_DAILY_LIMIT) {
                    yield 0L; // 일일 제한 초과 시 0 포인트 지급
                }
                yield COMMENT_CREATE_POINT;
            }
            case POST_VOTED -> POST_VOTED_POINT;
            case HOT_ISSUE -> HOT_ISSUE_POINT;
        };

        // 포인트가 0보다 클 때만 포인트 지급 및 히스토리 저장
        if (amount > 0) {
            profile.addPoint(amount);
            memberProfileRepository.save(profile);

            PointHistory history = PointHistory.builder()
                    .member(member)
                    .amount(amount)
                    .type(type)
                    .build();
            pointHistoryRepository.save(history);
        }
    }
}
