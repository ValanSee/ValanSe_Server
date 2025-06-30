package com.valanse.valanse.repository;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.repository.CommentRepositoryCustom.CommentRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {
}