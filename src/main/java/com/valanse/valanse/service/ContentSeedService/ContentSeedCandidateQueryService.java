package com.valanse.valanse.service.ContentSeedService;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
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

    // 특정 봇이 투표+댓글을 남길 상호작용 후보 게시글 목록.
    // 우선순위: 실제 사용자 글(탈퇴 회원 포함) -> 이번 실행 봇 글 -> 기존 봇 글, 그 안에서는 최신순.
    public List<CandidatePost> findInteractionCandidates(Long botMemberId, Set<Long> thisRunBotMemberIds) {
        List<Long> candidateVoteIds = findCandidateVoteIds(botMemberId);
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

        return candidates;
    }

    private List<Long> findCandidateVoteIds(Long botMemberId) {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(properties.getTargetVoteLookbackDays());

        List<Long> votedVoteIds = queryFactory
                .select(memberVoteOption.vote.id)
                .from(memberVoteOption)
                .where(memberVoteOption.member.id.eq(botMemberId))
                .fetch();

        List<Long> commentedVoteIds = queryFactory
                .select(comment.commentGroup.vote.id)
                .from(comment)
                .where(comment.member.id.eq(botMemberId), comment.deletedAt.isNull())
                .fetch();

        BooleanBuilder where = new BooleanBuilder();
        where.and(vote.createdAt.goe(since));
        where.and(vote.member.isNull().or(vote.member.id.ne(botMemberId)));
        if (!votedVoteIds.isEmpty()) {
            where.and(vote.id.notIn(votedVoteIds));
        }
        if (!commentedVoteIds.isEmpty()) {
            where.and(vote.id.notIn(commentedVoteIds));
        }

        return queryFactory
                .select(vote.id)
                .from(vote)
                .where(where)
                .fetch();
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
