package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    @Transactional
    public void deleteMyComment(Member member, Long commentId) {
        commentRepository.findById(commentId).ifPresent(comment -> {
            if (!comment.getMember().getId().equals(member.getId())) {
                throw new IllegalArgumentException("삭제 권한 없음");
            }
            comment.setIsDeleted(true);
            commentRepository.save(comment);
        });
    }
}