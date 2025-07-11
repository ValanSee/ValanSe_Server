package com.valanse.valanse.repository.VotesCheckRepositoryCustom;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.dto.VotesCheck.VoteAgeResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto.OptionResultDto;
import com.valanse.valanse.domain.mapping.QMemberVoteOption;
import com.valanse.valanse.domain.QMember;
import com.valanse.valanse.domain.QMemberProfile;
import com.valanse.valanse.domain.QVoteOption;
import com.valanse.valanse.dto.VotesCheck.VoteMbtiResultResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        // gender 값 전처리
        if ("F".equalsIgnoreCase(gender)) gender = "FEMALE";
        else if ("M".equalsIgnoreCase(gender)) gender = "MALE";

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

    @Override
    public VoteAgeResultResponseDto findVoteResultByAge(Long voteId) {
        List<Tuple> rawResults = queryFactory
                .select(option.label, profile.age.stringValue(), mvo.count(), option.content)
                .from(mvo)
                .join(mvo.voteOption, option)
                .join(mvo.member, member)
                .join(member.profile, profile)
                .where(mvo.voteOption.vote.id.eq(voteId))
                .groupBy(option.label, profile.age, option.content)
                .fetch();

        // A/B 선택지별로 AgeGroupStats 생성
        Map<String, VoteAgeResultResponseDto.AgeGroupStats> ageRatios = new HashMap<>();

        for (Tuple tuple : rawResults) {
            String label = tuple.get(option.label).name(); // A or B
            String age = convertAgeToLabel(tuple.get(profile.age.stringValue())); // 10대, 20대 등
            String content = tuple.get(option.content); // ex) 김치찌개
            int count = Math.toIntExact(tuple.get(mvo.count())); // 투표 수

            // AgeGroupStats 객체 초기화
            ageRatios.computeIfAbsent(label, k -> VoteAgeResultResponseDto.AgeGroupStats.builder()
                    .totalCount(0)
                    .ageGroups(new HashMap<>())
                    .build());

            VoteAgeResultResponseDto.AgeGroupStats stats = ageRatios.get(label);

            // totalCount 누적
            stats.setTotalCount(stats.getTotalCount() + count);

            // age별 dto 저장 (비율은 일단 0)
            stats.getAgeGroups().put(age, VoteAgeResultResponseDto.AgeRatioDto.builder()
                    .content(content)
                    .voteCount(count)
                    .ratio(0)
                    .build());
        }

        // 비율 계산
        for (VoteAgeResultResponseDto.AgeGroupStats stats : ageRatios.values()) {
            int total = stats.getTotalCount();
            for (VoteAgeResultResponseDto.AgeRatioDto dto : stats.getAgeGroups().values()) {
                dto.setRatio(dto.getVoteCount() * 100f / total);
            }
        }

        return VoteAgeResultResponseDto.builder()
                .voteId(voteId)
                .ageRatios(ageRatios)
                .build();
    }



    private String convertAgeToLabel(String enumAge) {
        return switch (enumAge) {
            case "TEN" -> "10대";
            case "TWENTY" -> "20대";
            case "THIRTY" -> "30대";
            case "OVER_FORTY" -> "40대 이상";
            default -> "기타";
        };
    }

    @Override
    public VoteMbtiResultResponseDto findVoteResultByMbti(Long voteId, String mbtiType) {
        // 1. MBTI 기준 필드 선택
        EnumPath<?> mbtiField = mbtiType.equals("ie") ? profile.mbtiIe : profile.mbtiTf;

        // 2. MBTI별 content 통계 가져오기
        List<Tuple> rawResults = queryFactory
                .select(mbtiField.stringValue(), option.content, mvo.count())
                .from(mvo)
                .join(mvo.member, member)
                .join(member.profile, profile)
                .join(mvo.voteOption, option)
                .where(mvo.voteOption.vote.id.eq(voteId))
                .groupBy(mbtiField, option.content)
                .fetch();

        // 3. MBTI별로 분류하여 내부 총합과 함께 비율 계산
        Map<String, List<Tuple>> grouped = rawResults.stream()
                .collect(Collectors.groupingBy(t -> t.get(mbtiField.stringValue())));

        Map<String, List<VoteMbtiResultResponseDto.OptionRatio>> resultMap = new HashMap<>();
        int totalCount = 0;

        for (Map.Entry<String, List<Tuple>> entry : grouped.entrySet()) {
            String mbtiValue = entry.getKey();
            List<Tuple> tuples = entry.getValue();

            // 해당 MBTI 유형의 총 투표 수
            long totalPerMbti = tuples.stream().mapToLong(t -> t.get(mvo.count())).sum();
            totalCount += totalPerMbti;

            List<VoteMbtiResultResponseDto.OptionRatio> ratios = tuples.stream()
                    .map(t -> {
                        long count = t.get(mvo.count());
                        double ratio = totalPerMbti == 0 ? 0 : (count * 100.0 / totalPerMbti);
                        return VoteMbtiResultResponseDto.OptionRatio.builder()
                                .content(t.get(option.content))
                                .vote_count((int) count)
                                .ratio((float) (Math.round(ratio * 10) / 10.0)) // 소수점 1자리
                                .build();
                    })
                    .collect(Collectors.toList());

            resultMap.put(mbtiValue, ratios);
        }

        return VoteMbtiResultResponseDto.builder()
                .vote_id(voteId)
                .mbti_type(mbtiType)
                .total_count(totalCount)
                .mbti_ratios(resultMap)
                .build();
    }



}
