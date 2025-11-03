package com.valanse.valanse.service.ReportService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Report;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.ReportRepository;
import com.valanse.valanse.repository.ReportRepositoryCustom.ReportRepositoryCustom;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService{

    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final ReportRepositoryCustom reportRepositoryCustom;

    @Override
    public void report(Member member, Long targetId, ReportType reportType){

        // ReportType 에 따라서 구분.
        if (reportType == ReportType.VOTE) {
            Vote vote = voteRepository.findById(targetId)
                    .orElseThrow(() -> new ApiException("투표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
            if (vote.getMember().getId().equals(member.getId())) {
                throw new ApiException("자신의 투표는 신고할 수 없습니다.", HttpStatus.BAD_REQUEST);
            }
        }

        if (reportType == ReportType.COMMENT) {
            Comment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new ApiException("댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
            if (comment.getMember().getId().equals(member.getId())) {
                throw new ApiException("자신의 댓글은 신고할 수 없습니다.", HttpStatus.BAD_REQUEST);
            }
        }

        if (reportRepository.existsByMemberAndReportTypeAndTargetId(member, reportType, targetId)) {
            throw new ApiException("이미 신고한 대상입니다.", HttpStatus.BAD_REQUEST);
        }

        Report report = Report.builder()
                .member(member)
                .reportType(reportType)
                .targetId(targetId)
                .build();

        reportRepository.save(report);
    }

    @Override
    public List<ReportedTargetResponse> getReportedTargets(Member member, ReportType type, String sort) {
        if (member.getRole() != Role.ADMIN) {
            throw new ApiException("관리자만 접근 가능합니다.", HttpStatus.FORBIDDEN);
        }
        return reportRepositoryCustom.findReportedTargets(type, sort);
    }

    @Override
    public long countReports(Member member,ReportType type, Long targetId) {
        if (member.getRole() != Role.ADMIN) {
            throw new ApiException("관리자만 접근 가능합니다.", HttpStatus.FORBIDDEN);
        }
        return reportRepositoryCustom.countReports(type, targetId);
    }


}
