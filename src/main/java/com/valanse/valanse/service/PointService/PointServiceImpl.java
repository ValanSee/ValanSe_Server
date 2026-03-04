package com.valanse.valanse.service.PointService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.PointHistory;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class PointServiceImpl implements PointService{
    private final PointHistoryRepository pointHistoryRepository;

    //포인트 지급
    @Override
    public void givePoint(Member member, PointType type, Long amount) {

        MemberProfile profile = member.getProfile();

        profile.addPoint(amount);

        pointHistoryRepository.save(
                PointHistory.of(member, type, amount)
        );
    }

    @Override
    public void giveCommentPointIfAvailable(Member member) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long todayCount = pointHistoryRepository.countByMemberAndTypeAndCreatedAtBetween(
                member,
                PointType.COMMENT_CREATE,
                startOfDay,
                now
        );

        if (todayCount >= 5) return;

        givePoint(member, PointType.COMMENT_CREATE, 1L);
    }


}
