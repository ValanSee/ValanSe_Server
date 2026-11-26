package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.domain.DailyVote;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 오늘의 밸런스 날짜별 정산(승자 결정, 보너스 포인트 지급)의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface DailyVoteSettlementService {

    /**
     * 해당 날짜를 정산합니다. 행이 없거나 이미 정산됐으면 empty 를 반환합니다.
     */
    Optional<DailyVote> settle(LocalDate date);
}
