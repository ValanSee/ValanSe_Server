package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.AuthErrorMessage;
import com.valanse.valanse.common.message.CommentErrorMessage;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.common.message.ProfileErrorMessage;
import com.valanse.valanse.common.message.VoteErrorMessage;
import com.valanse.valanse.domain.*;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.dto.Comment.*;
import com.valanse.valanse.repository.*;
import com.valanse.valanse.service.PointService.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final MemberRepository memberRepository;
    private final VoteRepository voteRepository;
    private final CommentGroupRepository commentGroupRepository;
    private final CommentRepository commentRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberProfileTitleRepository memberProfileTitleRepository;
    private final PointService pointService;

    @Override
    public void deleteMyComment(Member member, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(CommentErrorMessage.COMMENT_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        Long writerId = comment.getMember().getId();
        Long loginId = member.getId();

        if (!writerId.equals(loginId) && member.getRole() != Role.ADMIN) {
            throw new ApiException(AuthErrorMessage.DELETE_PERMISSION_DENIED.message(), HttpStatus.FORBIDDEN);
        }

        // Soft delete 처리
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);

        // ✅ 추가: 카운트 감소 로직
        if (comment.getParent() == null) {
            // 부모 댓글인 경우: totalCommentCount 감소
            CommentGroup commentGroup = comment.getCommentGroup();
            commentGroup.setTotalCommentCount(commentGroup.getTotalCommentCount() - 1);
            commentGroupRepository.save(commentGroup);
        } else {
            // 대댓글인 경우: 부모 댓글의 replyCount 감소
            Comment parent = comment.getParent();
            parent.updateReplyCount(parent.getReplyCount() - 1);
            commentRepository.save(parent);
        }
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
            throw new ApiException(CommentErrorMessage.WRONG_SORT_PARAMETER.message(), HttpStatus.BAD_REQUEST);
        }

        return comments.stream()
                .map(MyCommentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Long createComment(Long voteId, Long userId, CommentPostRequest request) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorMessage.VOTE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

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
                    .orElseThrow(() -> new ApiException(CommentErrorMessage.PARENT_COMMENT_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
            parent.updateReplyCount(parent.getReplyCount() + 1); // replyCount 증가
            commentRepository.save(parent);
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

        // ✅ 수정: 부모 댓글일 때만 totalCommentCount 증가
        if (request.getParentId() == null) {
            commentGroup.setTotalCommentCount(commentGroup.getTotalCommentCount() + 1);
            commentGroupRepository.save(commentGroup);
        }

        Long savedCommentId = commentRepository.save(comment).getId();

        // 댓글 작성 포인트 지급 (부모 댓글일 때만)
        if (request.getParentId() == null) {
            pointService.givePoint(userId, PointType.COMMENT_CREATE);
        }

        return savedCommentId;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedCommentResponse getCommentsByVoteId(Long voteId, String sort, Pageable pageable, Long loginId, Boolean isAdmin) {

        Slice<CommentResponseDto> slice = commentRepository.findCommentsByVoteIdSlice(voteId, sort, pageable, loginId, isAdmin);
        return PagedCommentResponse.builder() // 인덱스 , 쿼리
                .comments(slice.getContent())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .hasNext(slice.hasNext())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BestCommentResponseDto getBestCommentByVoteId(Long voteId) {
        // commentGroup이 없으면 아예 댓글도 없다고 판단하고 빈 응답 반환
        CommentGroup group = commentGroupRepository.findByVoteId(voteId).orElse(null);
        if (group == null) {
            return BestCommentResponseDto.builder()
                    .totalCommentCount(0)
                    .content(null)
                    .build();
        }
        Long actualCommentCount = commentRepository.countActiveCommentsByVoteId(voteId);

        return commentRepository.findMostLikedCommentByVoteId(voteId)
                .map(comment -> BestCommentResponseDto.builder()
                        .totalCommentCount(actualCommentCount.intValue())
                        .content(comment.getContent())
                        .build())
                .orElse( // 댓글이 없을 경우에도 빈 응답 반환
                        BestCommentResponseDto.builder()
                                .totalCommentCount(actualCommentCount.intValue())
                                .content(null)
                                .build()
                );
    }


    @Override
    @Transactional(readOnly = true)
    public List<CommentReplyResponseDto> getReplies(Member loginUser, Long voteId, Long parentCommentId) {
        // 유효한 투표인지 확인
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorMessage.VOTE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

        // 대댓글 조회
        List<Comment> replies = commentRepository.findAllByParentId(parentCommentId);

        return replies.stream()
                .map(reply -> {
                    MemberProfile profile = memberProfileRepository.findByMemberId(reply.getMember().getId())
                            .orElseThrow(() -> new ApiException(ProfileErrorMessage.PROFILE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));

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

                    boolean isAdmin = loginUser != null && loginUser.getRole() == Role.ADMIN;
                    boolean canDelete = false;
                    if (loginUser != null && vote.getMember() != null) {
                        canDelete = isAdmin || vote.getMember().getId().equals(loginUser.getId());
                    }

                    return CommentReplyResponseDto.builder()
                            .id(reply.getId())
                            .nickname(profile.getNickname())
                            .title(getEquippedTitleName(reply.getMember().getId()))
                            .createdAt(reply.getCreatedAt())
                            .content(reply.getContent())
                            .likeCount(reply.getLikeCount())
                            .replyCount(reply.getReplyCount())
                            .deletedAt(reply.getDeletedAt())
                            .label(label)
                            .daysAgo(daysAgo)
                            .hoursAgo(hoursAgo)
                            .canDelete(canDelete)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getEquippedTitleName(Long memberId) {
        return memberProfileTitleRepository.findByMemberProfileMemberIdAndEquippedTrue(memberId)
                .map(MemberProfileTitle::getTitle)
                .map(Title::getName)
                .orElse(null);
    }
}
