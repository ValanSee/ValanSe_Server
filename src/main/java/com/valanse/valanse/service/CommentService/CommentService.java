package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Comment.MyCommentResponseDto;
import java.util.List;

public interface CommentService {
    void deleteMyComment(Member member, Long commentId);

    List<MyCommentResponseDto> getMyComments(Member member, String sort);

}