package com.valanse.valanse.service.PointService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.common.message.PointErrorMessage;
import com.valanse.valanse.common.message.ProfileErrorMessage;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.PointHistory;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.dto.PointHistory.PointHistoryResponse;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // 날짜 포맷터
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void givePoint(Long memberId, PointType type) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        MemberProfile profile = memberProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ApiException(ProfileErrorMessage.PROFILE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

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
            case TITLE_PURCHASE -> throw new ApiException(PointErrorMessage.INVALID_REWARD_TYPE.message(), HttpStatus.BAD_REQUEST);
        };

        // 포인트가 0보다 클 때만 포인트 지급 및 히스토리 저장
        if (amount > 0) {
            profile.addPoint(amount);
            memberProfileRepository.save(profile);

            PointHistory history = PointHistory.builder()
                    .member(member)
                    .amount(amount)
                    .remainingPoint(profile.getPoint())
                    .type(type)
                    .build();
            pointHistoryRepository.save(history);
        }
    }

    @Override
    public void recordPointUsage(Long memberId, long amount, PointType type) {
        if (amount <= 0) {
            throw new ApiException(PointErrorMessage.USAGE_POINT_INVALID.message(), HttpStatus.BAD_REQUEST);
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        MemberProfile profile = memberProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ApiException(ProfileErrorMessage.PROFILE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        PointHistory history = PointHistory.builder()
                .member(member)
                .amount(-amount)
                .remainingPoint(profile.getPoint())
                .type(type)
                .build();
        pointHistoryRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public PointHistoryResponse getPointHistory(Long memberId) {
        // 회원 존재 여부 확인
        memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        // 포인트 히스토리 조회 (최신순으로 정렬)
        List<PointHistory> histories = pointHistoryRepository.findByMemberId(memberId);
        Map<Long, Long> fallbackRemainingPoints = calculateRemainingPoints(histories);

        // DTO로 변환
        List<PointHistoryResponse.PointHistoryItem> historyItems = histories.stream()
                .sorted((h1, h2) -> {
                    // null 값 처리: null은 가장 오래된 것으로 간주
                    if (h1.getCreatedAt() == null && h2.getCreatedAt() == null) return 0;
                    if (h1.getCreatedAt() == null) return 1;
                    if (h2.getCreatedAt() == null) return -1;
                    return h2.getCreatedAt().compareTo(h1.getCreatedAt()); // 최신순 정렬
                })
                .map(history -> new PointHistoryResponse.PointHistoryItem(
                        history.getId(),
                        history.getAmount(),
                        history.getRemainingPoint() != null
                                ? history.getRemainingPoint()
                                : fallbackRemainingPoints.get(history.getId()),
                        history.getType(),
                        getPointTypeDescription(history.getType()),
                        formatCreatedAt(history.getCreatedAt())
                ))
                .toList();

        return new PointHistoryResponse(historyItems);
    }

    private Map<Long, Long> calculateRemainingPoints(List<PointHistory> histories) {
        Map<Long, Long> remainingPoints = new HashMap<>();
        long balance = 0L;

        List<PointHistory> oldestFirstHistories = histories.stream()
                .sorted((h1, h2) -> {
                    if (h1.getCreatedAt() == null && h2.getCreatedAt() == null) {
                        return compareId(h1, h2);
                    }
                    if (h1.getCreatedAt() == null) return -1;
                    if (h2.getCreatedAt() == null) return 1;

                    int createdAtCompare = h1.getCreatedAt().compareTo(h2.getCreatedAt());
                    return createdAtCompare != 0 ? createdAtCompare : compareId(h1, h2);
                })
                .toList();

        for (PointHistory history : oldestFirstHistories) {
            balance += history.getAmount();
            remainingPoints.put(history.getId(), balance);
        }

        return remainingPoints;
    }

    private int compareId(PointHistory h1, PointHistory h2) {
        if (h1.getId() == null && h2.getId() == null) return 0;
        if (h1.getId() == null) return -1;
        if (h2.getId() == null) return 1;
        return h1.getId().compareTo(h2.getId());
    }

    private String getPointTypeDescription(PointType type) {
        return switch (type) {
            case SIGN_UP -> "회원가입";
            case POST_CREATE -> "게시글 작성";
            case COMMENT_CREATE -> "댓글 작성";
            case POST_VOTED -> "투표 참여";
            case HOT_ISSUE -> "핫이슈";
            case TITLE_PURCHASE -> "칭호 구매";
        };
    }

    private String formatCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            return null;
        }
        return createdAt.format(DATE_TIME_FORMATTER);
    }
}
