package com.valanse.valanse.service;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    public void deleteMyComment(Member member, Long commentId) {
        // 삭제 로직 구현
        commentRepository.findById(commentId).ifPresent(comment -> {
            if (!comment.getMember().getId().equals(member.getId())) {
                throw new IllegalArgumentException("삭제 권한 없음");
            }
            comment.setIsDeleted(true);
            commentRepository.save(comment);
        });
    }
}