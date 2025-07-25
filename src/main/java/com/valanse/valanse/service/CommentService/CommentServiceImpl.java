package com.valanse.valanse.service.CommentService;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.valanse.valanse.domain.*;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.dto.Comment.*;
import com.valanse.valanse.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;


import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final MemberRepository memberRepository;
    private final VoteRepository voteRepository;
    private final CommentGroupRepository commentGroupRepository;
    private final CommentRepository commentRepository;
    private final MemberProfileRepository memberProfileRepository;

    @Override
    @Transactional
    public void deleteMyComment(Member member, Long commentId) {
        commentRepository.findById(commentId).ifPresentOrElse(comment -> {
            Long writerId = comment.getMember().getId();
            Long loginId = member.getId();

            System.out.println("[삭제 시도] 댓글 ID: " + commentId);
            System.out.println("작성자 ID: " + writerId + ", 요청자 ID: " + loginId);

            if (!writerId.equals(loginId)) {
                System.out.println("삭제 권한 없음: 요청자 ≠ 작성자");
                throw new IllegalArgumentException("삭제 권한 없음");
            }

            comment.setDeletedAt(LocalDateTime.now());
            commentRepository.save(comment);

            System.out.println("댓글 ID " + commentId + " → isDeleted=true 저장 완료");

        }, () -> {
            System.out.println("삭제 실패: 해당 댓글 ID " + commentId + " 없음");
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyCommentResponseDto> getMyComments(Member member, String sort) {
        Long memberId = member.getId();
        List<Comment> comments;

        if ("oldest".equalsIgnoreCase(sort)) {
            comments = commentRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtAsc(memberId);
        } else if ("latest".equalsIgnoreCase(sort)) {
            comments = commentRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId);
        } else {
            throw new IllegalArgumentException("sort 파라미터는 'latest' 또는 'oldest'만 허용됩니다.");
        }

        return comments.stream()
                .map(MyCommentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long createComment(Long voteId, Long userId, CommentPostRequest request) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("Vote not found"));
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // 1. CommentGroup 찾기 (없으면 생성)
        CommentGroup commentGroup = commentGroupRepository.findByVoteId(voteId)
                .orElseGet(() -> {
                    CommentGroup newGroup = CommentGroup.builder()
                            .vote(vote)
                            .totalCommentCount(0)
                            .build();
                    return commentGroupRepository.save(newGroup);
                });

        // 2. 부모 댓글이 있을 경우 replyCount 증가
        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 id에 해당하는 부모 댓글이 존재하지 않습니다."));
            parent.updateReplyCount(parent.getReplyCount() + 1); // replyCount 증가
        }

        // 3. 댓글 저장
        Comment comment = Comment.builder()
                .content(request.getContent())
                .member(member)
                .commentGroup(commentGroup)
                .parent(parent)
                .likeCount(0)
                .replyCount(0)
                .deletedAt(null)
                .build();
        commentGroup.setTotalCommentCount(commentGroup.getTotalCommentCount() + 1); // 총 댓글 수 증가
        commentGroupRepository.save(commentGroup);

        return commentRepository.save(comment).getId();
    }

    @Override
    public PagedCommentResponse getCommentsByVoteId(Long voteId, String sort, Pageable pageable) {
        Slice<CommentResponseDto> slice = commentRepository.findCommentsByVoteIdSlice(voteId, sort, pageable);
        return PagedCommentResponse.builder() // 인덱스 , 쿼리
                .comments(slice.getContent())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .hasNext(slice.hasNext())
                .build();
    }

    @Override
    public BestCommentResponseDto getBestCommentByVoteId(Long voteId) {
        // commentGroup이 없으면 아예 댓글도 없다고 판단하고 빈 응답 반환
        CommentGroup group = commentGroupRepository.findByVoteId(voteId).orElse(null);
        if (group == null) {
            return BestCommentResponseDto.builder()
                    .totalCommentCount(0)
                    .content(null)
                    .build();
        }

        return commentRepository.findMostLikedCommentByVoteId(voteId)
                .map(comment -> BestCommentResponseDto.builder()
                        .totalCommentCount(group.getTotalCommentCount())
                        .content(comment.getContent())
                        .build())
                .orElse( // 댓글이 없을 경우에도 빈 응답 반환
                        BestCommentResponseDto.builder()
                                .totalCommentCount(group.getTotalCommentCount())
                                .content(null)
                                .build()
                );
    }


    @Override
    public List<CommentReplyResponseDto> getReplies(Long voteId, Long parentCommentId) {
        // 유효한 투표인지 확인
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("투표가 존재하지 않습니다."));

        // 대댓글 조회
        List<Comment> replies = commentRepository.findAllByParentId(parentCommentId);

        return replies.stream()
                .map(reply -> {
                    MemberProfile profile = memberProfileRepository.findByMemberId(reply.getMember().getId())
                            .orElseThrow(() -> new IllegalArgumentException("회원 프로필이 존재하지 않습니다."));

                    VoteLabel label = reply.getMember().getMemberVoteOptions().stream()
                            .filter(opt -> opt.getVoteOption().getVote().getId().equals(voteId))
                            .map(opt -> opt.getVoteOption().getLabel())
                            .findFirst()
                            .orElse(null);


                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime createdAt = reply.getCreatedAt();
                    long totalHours = ChronoUnit.HOURS.between(createdAt, now);
                    long daysAgo = totalHours / 24;
                    long hoursAgo = totalHours % 24;

                    return CommentReplyResponseDto.builder()
                            .id(reply.getId())
                            .nickname(profile.getNickname())
                            .createdAt(reply.getCreatedAt())
                            .content(reply.getContent())
                            .likeCount(reply.getLikeCount())
                            .replyCount(reply.getReplyCount())
                            .deletedAt(reply.getDeletedAt())
                            .label(label)
                            .daysAgo(daysAgo)
                            .hoursAgo(hoursAgo)
                            .build();
                })
                .collect(Collectors.toList());
    }
}