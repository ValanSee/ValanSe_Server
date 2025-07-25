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

import java.util.ArrayList;
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

        // 1. 전체 투표 옵션(label + content) 가져오기
        List<Tuple> allOptions = queryFactory
                .select(option.label, option.content)
                .from(option)
                .where(option.vote.id.eq(voteId))
                .fetch();

        // 2. 해당 성별의 전체 투표 수
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

        if (totalCount == null) totalCount = 0L;

        // 3. 성별별 투표 결과 집계
        List<Tuple> rawResults = queryFactory
                .select(option.label, option.content, mvo.count())
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

        // 4. 결과를 Map<String(label|content), count> 형태로 변환
        Map<String, Long> resultMap = rawResults.stream()
                .collect(Collectors.toMap(
                        t -> t.get(option.label).name() + "|" + t.get(option.content),
                        t -> t.get(mvo.count())
                ));

        // 5. 전체 옵션을 기준으로 누락 없이 DTO 생성
        Long finalTotalCount = totalCount;
        List<OptionResultDto> optionStats = allOptions.stream()
                .map(t -> {
                    String label = t.get(option.label).name();
                    String content = t.get(option.content);
                    String key = label + "|" + content;
                    int count = resultMap.getOrDefault(key, 0L).intValue();
                    float ratio = (finalTotalCount == 0) ? 0f : (count * 100f / finalTotalCount);

                    return OptionResultDto.builder()
                            .label(label)
                            .content(content)
                            .voteCount(count)
                            .ratio(ratio)
                            .build();
                })
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
        // 1. 전체 라벨 + content 조회
        List<Tuple> allOptions = queryFactory
                .select(option.label, option.content)
                .from(option)
                .where(option.vote.id.eq(voteId))
                .fetch();

        // 2. 실제 응답값 집계
        List<Tuple> rawResults = queryFactory
                .select(option.label, profile.age.stringValue(), mvo.count(), option.content)
                .from(mvo)
                .join(mvo.voteOption, option)
                .join(mvo.member, member)
                .join(member.profile, profile)
                .where(mvo.voteOption.vote.id.eq(voteId))
                .groupBy(option.label, profile.age, option.content)
                .fetch();

        // 3. rawResults를 Map<String: label|age, count>로 변환
        Map<String, Integer> resultMap = rawResults.stream()
                .collect(Collectors.toMap(
                        t -> t.get(option.label).name() + "|" + convertAgeToLabel(t.get(profile.age.stringValue())),
                        t -> Math.toIntExact(t.get(mvo.count()))
                ));

        // 4. age enum → label 변환을 위한 전체 목록 선언
        List<String> ageLabels = List.of("10대", "20대", "30대", "40대 이상");

        // 5. 전체 옵션 × 전체 나이대 조합으로 결과 구성
        Map<String, VoteAgeResultResponseDto.AgeGroupStats> ageRatios = new HashMap<>();

        for (Tuple optionTuple : allOptions) {
            String label = optionTuple.get(option.label).name();
            String content = optionTuple.get(option.content);

            VoteAgeResultResponseDto.AgeGroupStats stats = VoteAgeResultResponseDto.AgeGroupStats.builder()
                    .totalCount(0)
                    .ageGroups(new HashMap<>())
                    .build();

            for (String ageLabel : ageLabels) {
                String key = label + "|" + ageLabel;
                int count = resultMap.getOrDefault(key, 0);
                stats.setTotalCount(stats.getTotalCount() + count);

                stats.getAgeGroups().put(ageLabel, VoteAgeResultResponseDto.AgeRatioDto.builder()
                        .content(content)
                        .voteCount(count)
                        .ratio(0) // 일단 0, 아래에서 계산
                        .build());
            }

            ageRatios.put(label, stats);
        }

        // 6. 비율 계산
        for (VoteAgeResultResponseDto.AgeGroupStats stats : ageRatios.values()) {
            int total = stats.getTotalCount();
            for (VoteAgeResultResponseDto.AgeRatioDto dto : stats.getAgeGroups().values()) {
                dto.setRatio(total == 0 ? 0f : dto.getVoteCount() * 100f / total);
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

        // 2. 전체 MBTI 유형 목록 설정
        List<String> mbtiValues = mbtiType.equals("ie") ? List.of("I", "E") : List.of("T", "F");

        // 3. 전체 선택지 content 조회
        List<String> allContents = queryFactory
                .select(option.content)
                .from(option)
                .where(option.vote.id.eq(voteId))
                .fetch();

        // 4. 실제 집계 결과 가져오기
        List<Tuple> rawResults = queryFactory
                .select(mbtiField.stringValue(), option.content, mvo.count())
                .from(mvo)
                .join(mvo.member, member)
                .join(member.profile, profile)
                .join(mvo.voteOption, option)
                .where(mvo.voteOption.vote.id.eq(voteId))
                .groupBy(mbtiField, option.content)
                .fetch();

        // 5. rawResults → Map<String(mbti|content), count>
        Map<String, Long> resultMap = rawResults.stream()
                .collect(Collectors.toMap(
                        t -> t.get(mbtiField.stringValue()) + "|" + t.get(option.content),
                        t -> t.get(mvo.count())
                ));

        // 6. 최종 결과 구성
        Map<String, List<VoteMbtiResultResponseDto.OptionRatio>> resultMapDto = new HashMap<>();
        int totalCount = 0;

        for (String mbti : mbtiValues) {
            long totalPerMbti = 0;
            List<VoteMbtiResultResponseDto.OptionRatio> optionRatios = new ArrayList<>();

            for (String content : allContents) {
                String key = mbti + "|" + content;
                int count = resultMap.getOrDefault(key, 0L).intValue();
                totalPerMbti += count;

                optionRatios.add(VoteMbtiResultResponseDto.OptionRatio.builder()
                        .content(content)
                        .vote_count(count)
                        .ratio(0f) // 비율은 아래에서 계산
                        .build());
            }

            // 비율 계산
            for (VoteMbtiResultResponseDto.OptionRatio dto : optionRatios) {
                int count = dto.getVote_count();
                float ratio = totalPerMbti == 0 ? 0f : Math.round((count * 1000f / totalPerMbti)) / 10.0f;
                dto.setRatio(ratio);
            }

            totalCount += totalPerMbti;
            resultMapDto.put(mbti, optionRatios);
        }

        return VoteMbtiResultResponseDto.builder()
                .vote_id(voteId)
                .mbti_type(mbtiType)
                .total_count(totalCount)
                .mbti_ratios(resultMapDto)
                .build();
    }




}
