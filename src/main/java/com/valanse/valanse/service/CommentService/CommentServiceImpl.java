package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.valanse.valanse.dto.Comment.MyCommentResponseDto;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;

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

            comment.setIsDeleted(true);
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

        if ("asc".equalsIgnoreCase(sort)) {
            comments = commentRepository.findByMemberIdAndIsDeletedFalseOrderByCreatedAtAsc(memberId);
        } else {
            comments = commentRepository.findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(memberId);
        }

        return comments.stream()
                .map(MyCommentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}