package com.valanse.valanse.service.ContentSeedService;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.valanse.valanse.common.config.ContentSeedProperties;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.QComment;
import com.valanse.valanse.domain.QMember;
import com.valanse.valanse.domain.QVote;
import com.valanse.valanse.domain.QVoteOption;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.domain.mapping.QMemberVoteOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// 봇 콘텐츠 생성(게시글/상호작용)에 참고할 데이터를 DB에서 조회하는 서비스.
// ContentSeedProperties의 recentTitleLimit/targetVoteLookbackDays/commentContextLimit/
// maxCommentsPerVote 설정을 기준으로 삼는다.
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentSeedCandidateQueryService {

    // 상호작용 후보 게시글 수의 상한. applyRecencyLimit=false(관리자 댓글 전용 수동
    // 실행)에서 기간 제한이 없으면 후보 전체가 Claude 프롬프트에 그대로 들어가므로
    // (ClaudeContentGenerator.buildInteractionPrompt), findCandidateVoteIds의 ID 조회
    // SQL 자체에서 적격성(선택지 정확히 2개, 게시글당 봇 댓글 한도)과 참여 이력 제외를
    // 서브쿼리로, 우선순위(실사용자 -> 이번 실행 봇 -> 기존 봇, 그 안에서는 최신순)를
    // ORDER BY로 모두 적용한 뒤 LIMIT한다 - 이렇게 해야 "거대한 IN(...)"·전체 후보
    // 엔티티/댓글 조회·애플리케이션 메모리 전체 정렬을 모두 피할 수 있다. 주간 자동
    // 생성(applyRecencyLimit=true, 7일 제한 유지)에는 LIMIT을 적용하지 않는다.
    static final int MAX_INTERACTION_CANDIDATES = 50;

    private static final QVote vote = QVote.vote;
    private static final QMember member = QMember.member;
    private static final QVoteOption voteOption = QVoteOption.voteOption;
    private static final QComment comment = QComment.comment;
    private static final QMemberVoteOption memberVoteOption = QMemberVoteOption.memberVoteOption;

    private final JPAQueryFactory queryFactory;
    private final ContentSeedProperties properties;
    private final ContentQualityGate qualityGate;
    private final Clock clock;

    // 게시글 생성 프롬프트에 참고 자료로 넘길 최근 활성 게시글(제목+카테고리) 목록.
    public List<RecentPost> findRecentActivePosts() {
        List<Vote> recentVotes = queryFactory
                .selectFrom(vote)
                .orderBy(vote.createdAt.desc(), vote.id.desc())
                .limit(properties.getRecentTitleLimit())
                .fetch();

        List<RecentPost> recentPosts = new ArrayList<>();
        for (Vote v : recentVotes) {
            toGeneratableCategory(v).ifPresent(category -> recentPosts.add(new RecentPost(v.getTitle(), category)));
        }
        return recentPosts;
    }

    // 특정 봇이 투표+댓글을 남길 상호작용 후보 게시글 목록. 주간 자동 생성(기존 호출자)은
    // 항상 이 2-인자 메서드를 사용해 targetVoteLookbackDays 제한을 유지한다.
    // 우선순위: 실제 사용자 글(탈퇴 회원 포함) -> 이번 실행 봇 글 -> 기존 봇 글, 그 안에서는 최신순.
    public List<CandidatePost> findInteractionCandidates(Long botMemberId, Set<Long> thisRunBotMemberIds) {
        return findInteractionCandidates(botMemberId, thisRunBotMemberIds, true);
    }

    // 관리자 댓글 전용 수동 실행(이슈 #328) 전용 진입점. applyRecencyLimit=false면
    // targetVoteLookbackDays 제한 없이 전체 게시글을 후보로 조회한다.
    public List<CandidatePost> findInteractionCandidates(
            Long botMemberId, Set<Long> thisRunBotMemberIds, boolean applyRecencyLimit) {
        List<Long> candidateVoteIds = findCandidateVoteIds(botMemberId, thisRunBotMemberIds, applyRecencyLimit);
        if (candidateVoteIds.isEmpty()) {
            return List.of();
        }

        List<Vote> candidateVotes = queryFactory
                .selectFrom(vote)
                .distinct()
                .leftJoin(vote.member, member).fetchJoin()
                .leftJoin(vote.voteOptions, voteOption).fetchJoin()
                .where(vote.id.in(candidateVoteIds))
                .fetch();

        Map<Long, Long> botCommentCountsByVoteId = new HashMap<>();
        Map<Long, List<String>> recentCommentsByVoteId = new HashMap<>();
        loadTopLevelComments(candidateVoteIds, botCommentCountsByVoteId, recentCommentsByVoteId);

        List<CandidatePost> candidates = new ArrayList<>();
        for (Vote v : candidateVotes) {
            findOptionA(v).flatMap(optionA -> findOptionB(v).map(optionB -> Map.entry(optionA, optionB)))
                    .ifPresent(options -> {
                        long botCommentCount = botCommentCountsByVoteId.getOrDefault(v.getId(), 0L);
                        if (botCommentCount < properties.getMaxCommentsPerVote()) {
                            candidates.add(new CandidatePost(
                                    v.getId(),
                                    v.getTitle(),
                                    v.getContent(),
                                    options.getKey().getContent(),
                                    options.getValue().getContent(),
                                    recentCommentsByVoteId.getOrDefault(v.getId(), List.of())
                            ));
                        }
                    });
        }

        Map<Long, Vote> voteById = new HashMap<>();
        candidateVotes.forEach(v -> voteById.put(v.getId(), v));
        candidates.sort(Comparator
                .comparingInt((CandidatePost c) -> priorityTier(voteById.get(c.id()), thisRunBotMemberIds))
                .thenComparing(c -> voteById.get(c.id()).getCreatedAt(), Comparator.reverseOrder()));

        // candidateVoteIds가 이미 findCandidateVoteIds의 SQL LIMIT으로 상한 이하이므로
        // (applyRecencyLimit=false일 때), 여기서 다시 자를 필요가 없다 - candidates는
        // candidateVoteIds의 부분집합(적격성 재확인으로 줄어들기만 함)이기 때문이다.
        return candidates;
    }

    // 적격성(선택지 정확히 2개, 게시글당 봇 댓글 한도 미달)과 참여 이력 제외(자기 글,
    // 이미 투표/댓글)을 모두 서브쿼리로 DB에서 거르고, 우선순위(실사용자 -> 이번 실행
    // 봇 -> 기존 봇, 그 안에서는 최신순)로 정렬한 뒤 applyRecencyLimit=false일 때만
    // MAX_INTERACTION_CANDIDATES로 LIMIT한다 - "전체 게시글 ID를 담은 거대한 IN(...)",
    // 전체 후보 엔티티/댓글 조회, 애플리케이션 메모리 전체 정렬을 모두 피하기 위함이다.
    private List<Long> findCandidateVoteIds(Long botMemberId, Set<Long> thisRunBotMemberIds, boolean applyRecencyLimit) {
        QVoteOption subVoteOption = new QVoteOption("subVoteOption");
        QComment subVotedComment = new QComment("subVotedComment");
        QComment subBotComment = new QComment("subBotComment");
        QMember subCommentAuthor = new QMember("subCommentAuthor");

        BooleanBuilder where = new BooleanBuilder();
        if (applyRecencyLimit) {
            LocalDateTime since = LocalDateTime.now(clock).minusDays(properties.getTargetVoteLookbackDays());
            where.and(vote.createdAt.goe(since));
        }
        where.and(vote.member.isNull().or(vote.member.id.ne(botMemberId)));

        // 이 봇이 이미 투표한 게시글 제외.
        where.and(JPAExpressions
                .selectOne()
                .from(memberVoteOption)
                .where(memberVoteOption.vote.id.eq(vote.id), memberVoteOption.member.id.eq(botMemberId))
                .notExists());

        // 이 봇이 이미 댓글을 남긴 게시글 제외.
        where.and(JPAExpressions
                .selectOne()
                .from(subVotedComment)
                .where(
                        subVotedComment.commentGroup.vote.id.eq(vote.id),
                        subVotedComment.member.id.eq(botMemberId),
                        subVotedComment.deletedAt.isNull()
                )
                .notExists());

        // 선택지가 정확히 2개(A, B)인 게시글만 - 선택지는 항상 A부터 순서대로 부여되므로
        // 개수 2는 라벨 {A, B}와 동치다(findOptionByLabel의 전제와 동일).
        where.and(JPAExpressions
                .select(subVoteOption.count())
                .from(subVoteOption)
                .where(subVoteOption.vote.id.eq(vote.id))
                .eq(2L));

        // 게시글당 봇 댓글(최상위, 활성) 수가 상한 미달인 게시글만.
        where.and(JPAExpressions
                .select(subBotComment.count())
                .from(subBotComment)
                .innerJoin(subBotComment.member, subCommentAuthor)
                .where(
                        subBotComment.commentGroup.vote.id.eq(vote.id),
                        subBotComment.parent.isNull(),
                        subBotComment.deletedAt.isNull(),
                        subCommentAuthor.isBot.isTrue()
                )
                .lt((long) properties.getMaxCommentsPerVote()));

        var query = queryFactory
                .select(vote.id)
                .from(vote)
                .where(where)
                .orderBy(priorityTierExpression(thisRunBotMemberIds).asc(), vote.createdAt.desc(), vote.id.desc());

        if (!applyRecencyLimit) {
            query = query.limit(MAX_INTERACTION_CANDIDATES);
        }

        return query.fetch();
    }

    // 실제 사용자(탈퇴 포함) 글은 0, 이번 실행 봇 글은 1, 기존 봇 글은 2 - priorityTier(Vote, Set)와 동일한 규칙.
    private NumberExpression<Integer> priorityTierExpression(Set<Long> thisRunBotMemberIds) {
        CaseBuilder.Cases<Integer, NumberExpression<Integer>> cases = new CaseBuilder()
                .when(vote.member.isNull().or(vote.member.isBot.isFalse())).then(0);
        if (!thisRunBotMemberIds.isEmpty()) {
            cases = cases.when(vote.member.id.in(thisRunBotMemberIds)).then(1);
        }
        return cases.otherwise(2);
    }

    // 후보 게시글들의 최상위(부모 없음) 활성 댓글을 한 번에 읽어, 봇 댓글 수와
    // 게시글별 최신 댓글(commentContextLimit개, 마스킹 완료) 맵을 채운다.
    private void loadTopLevelComments(
            List<Long> candidateVoteIds,
            Map<Long, Long> botCommentCountsByVoteId,
            Map<Long, List<String>> recentCommentsByVoteId
    ) {
        List<Tuple> rows = queryFactory
                .select(comment.commentGroup.vote.id, comment.content, member.isBot)
                .from(comment)
                .leftJoin(comment.member, member)
                .where(
                        comment.commentGroup.vote.id.in(candidateVoteIds),
                        comment.parent.isNull(),
                        comment.deletedAt.isNull()
                )
                .orderBy(comment.commentGroup.vote.id.asc(), comment.createdAt.desc(), comment.id.desc())
                .fetch();

        for (Tuple row : rows) {
            Long voteId = row.get(comment.commentGroup.vote.id);
            String content = row.get(comment.content);
            Boolean isBotAuthor = row.get(member.isBot);

            if (Boolean.TRUE.equals(isBotAuthor)) {
                botCommentCountsByVoteId.merge(voteId, 1L, Long::sum);
            }

            List<String> recentComments = recentCommentsByVoteId.computeIfAbsent(voteId, id -> new ArrayList<>());
            if (recentComments.size() < properties.getCommentContextLimit()) {
                recentComments.add(qualityGate.maskPersonalInformation(content));
            }
        }
    }

    // 실제 사용자(탈퇴 포함) 글은 0, 이번 실행 봇 글은 1, 기존 봇 글은 2.
    private int priorityTier(Vote v, Set<Long> thisRunBotMemberIds) {
        Member creator = v.getMember();
        if (creator == null || !creator.isBot()) {
            return 0;
        }
        return thisRunBotMemberIds.contains(creator.getId()) ? 1 : 2;
    }

    private Optional<VoteOption> findOptionA(Vote v) {
        return findOptionByLabel(v, VoteLabel.A);
    }

    private Optional<VoteOption> findOptionB(Vote v) {
        return findOptionByLabel(v, VoteLabel.B);
    }

    // CandidatePost/GeneratedInteraction은 선택지가 항상 2개(A, B)라고 가정하므로,
    // 선택지가 정확히 2개(A, B)인 게시글만 상호작용 후보로 다룬다.
    private Optional<VoteOption> findOptionByLabel(Vote v, VoteLabel label) {
        List<VoteOption> options = v.getVoteOptions();
        if (options.size() != 2) {
            return Optional.empty();
        }
        return options.stream().filter(option -> option.getLabel() == label).findFirst();
    }

    private Optional<GeneratableVoteCategory> toGeneratableCategory(Vote v) {
        if (v.getCategory() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(GeneratableVoteCategory.valueOf(v.getCategory().name()));
        } catch (IllegalArgumentException e) {
            // ALL 등 생성 스키마에 없는 카테고리는 참고 목록에서 제외한다.
            return Optional.empty();
        }
    }
}
