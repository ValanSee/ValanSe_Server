package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.domain.Member;

public interface CommentService {
    void deleteMyComment(Member member, Long commentId);
}