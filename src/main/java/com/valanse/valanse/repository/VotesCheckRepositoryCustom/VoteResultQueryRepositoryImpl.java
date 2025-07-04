package com.valanse.valanse.repository.VotesCheckRepositoryCustom;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto.OptionResultDto;
import com.valanse.valanse.domain.mapping.QMemberVoteOption;
import com.valanse.valanse.domain.QMember;
import com.valanse.valanse.domain.QMemberProfile;
import com.valanse.valanse.domain.QVoteOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class VoteResultQueryRepositoryImpl implements VoteResultQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QMemberVoteOption mvo = QMemberVoteOption.memberVoteOption;
    private final QMember member = QMember.member;
    private final QMemberProfile profile = QMemberProfile.memberProfile;
    private final QVoteOption option = QVoteOption.voteOption;

    @Override
    public VoteGenderResultResponseDto findVoteResultByGender(Long voteId, String gender) {

        // 1. 전체 성별 유저 투표 수
        Long totalCount = queryFactory
                .select(mvo.count())
                .from(mvo)
                .join(mvo.member, member)
                .join(member.profile, profile)
                .where(
                        mvo.voteOption.vote.id.eq(voteId),
                        profile.gender.stringValue().eq(gender)
                )
                .fetchOne();

        if (totalCount == null || totalCount == 0) {
            return VoteGenderResultResponseDto.builder()
                    .voteId(voteId)
                    .gender(gender)
                    .totalCount(0)
                    .options(List.of())
                    .build();
        }

        // 2. 각 선택지별 성별 유저 투표 수
        List<Tuple> rawResults = queryFactory
                .select(
                        option.label,
                        option.content,
                        mvo.count()
                )
                .from(mvo)
                .join(mvo.voteOption, option)
                .join(mvo.member, member)
                .join(member.profile, profile)
                .where(
                        mvo.voteOption.vote.id.eq(voteId),
                        profile.gender.stringValue().eq(gender)
                )
                .groupBy(option.label, option.content)
                .fetch();

        List<OptionResultDto> optionStats = rawResults.stream()
                .map(tuple -> OptionResultDto.builder()
                        .label(tuple.get(option.label).name())
                        .content(tuple.get(option.content))
                        .voteCount(tuple.get(mvo.count()).intValue())
                        .ratio((float) (tuple.get(mvo.count()) * 100.0 / totalCount))
                        .build()
                )
                .collect(Collectors.toList());

        return VoteGenderResultResponseDto.builder()
                .voteId(voteId)
                .gender(gender)
                .totalCount(totalCount.intValue())
                .options(optionStats)
                .build();
    }
}
