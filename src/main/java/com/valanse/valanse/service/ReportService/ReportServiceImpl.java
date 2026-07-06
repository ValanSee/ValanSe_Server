package com.valanse.valanse.service.ReportService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.AuthErrorMessage;
import com.valanse.valanse.common.message.ReportErrorMessage;
import com.valanse.valanse.common.message.VoteErrorMessage;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Report;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.ReportReason;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;
import com.valanse.valanse.dto.Report.ReportDetailItemResponse;
import com.valanse.valanse.dto.Report.ReportDetailResponse;
import com.valanse.valanse.dto.Report.ReportedCommentResponse;
import com.valanse.valanse.dto.Report.ReportedVoteResponse;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.ReportRepository;
import com.valanse.valanse.repository.ReportRepositoryCustom.ReportRepositoryCustom;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
/**
 * 신고 대상 검증, 중복 신고 방지, 관리자 신고 목록 조회를 처리하는 서비스 코드입니다.
 */
public class ReportServiceImpl implements ReportService{

    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final ReportRepositoryCustom reportRepositoryCustom;

    /**
     * 투표 또는 댓글 신고를 생성하고 본인/중복 신고를 차단하는 메서드입니다.
     */
    @Override
    public void report(Member member, Long targetId, ReportType reportType){
        report(member, targetId, reportType, ReportReason.ETC, null);
    }

    @Override
    public void report(Member member, Long targetId, ReportType reportType, ReportReason reason, String content){
        validateReportRequest(reportType, reason, content);
        // ReportType 에 따라서 구분.
        if (reportType == ReportType.VOTE) {
            Vote vote = voteRepository.findById(targetId)
                    .orElseThrow(() -> new ApiException(VoteErrorMessage.VOTE_DETAIL_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
            if (vote.getMember().getId().equals(member.getId())) {
                throw new ApiException(ReportErrorMessage.OWN_VOTE_REPORT_NOT_ALLOWED.message(), HttpStatus.BAD_REQUEST);
            }
        }

        if (reportType == ReportType.COMMENT) {
            Comment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new ApiException(ReportErrorMessage.COMMENT_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
            if (comment.getMember().getId().equals(member.getId())) {
                throw new ApiException(ReportErrorMessage.OWN_COMMENT_REPORT_NOT_ALLOWED.message(), HttpStatus.BAD_REQUEST);
            }
        }

        // 신고를 이미 했다면 에러 발생
        if (reportRepository.existsByMemberAndReportTypeAndTargetId(member, reportType, targetId)) {
            throw new ApiException(ReportErrorMessage.ALREADY_REPORTED.message(), HttpStatus.BAD_REQUEST);
        }

        Report report = Report.builder()
                .member(member)
                .reportType(reportType)
                .targetId(targetId)
                .reason(reason)
                .content(normalizeContent(content))
                .build();

        saveReport(report);
    }

    /**
     * 관리자가 신고 누적 대상 목록을 조회하는 메서드입니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReportedTargetResponse> getReportedTargets(Member member, ReportType type, String sort) {
        validateAdmin(member);
        return reportRepositoryCustom.findReportedTargets(type, sort);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getReportDetail(Member member, ReportType type, Long targetId) {
        validateAdmin(member);

        ReportedVoteResponse voteResponse = null;
        ReportedCommentResponse commentResponse = null;

        if (type == ReportType.VOTE) {
            Vote vote = voteRepository.findByIdAndDeletedAtIsNull(targetId)
                    .orElseThrow(() -> new ApiException(
                            ReportErrorMessage.REPORTED_VOTE_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
            voteResponse = new ReportedVoteResponse(vote);
        } else if (type == ReportType.COMMENT) {
            Comment comment = commentRepository.findByIdAndDeletedAtIsNull(targetId)
                    .orElseThrow(() -> new ApiException(
                            ReportErrorMessage.REPORTED_COMMENT_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
            commentResponse = new ReportedCommentResponse(comment);
        } else {
            throw new ApiException(ReportErrorMessage.REPORT_TYPE_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }

        List<ReportDetailItemResponse> reports = reportRepository.findDetailsByTarget(type, targetId).stream()
                .map(ReportDetailItemResponse::new)
                .toList();

        return ReportDetailResponse.builder()
                .targetId(targetId)
                .targetType(type)
                .reportCount(reports.size())
                .vote(voteResponse)
                .comment(commentResponse)
                .reports(reports)
                .build();
    }

    private void validateAdmin(Member member) {
        if (member.getRole() != Role.ADMIN) {
            throw new ApiException(AuthErrorMessage.ADMIN_ONLY.message(), HttpStatus.FORBIDDEN);
        }
    }

    private void validateReportRequest(ReportType reportType, ReportReason reason, String content) {
        if (reportType == null) {
            throw new ApiException(ReportErrorMessage.REPORT_TYPE_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        if (reason == null) {
            throw new ApiException(ReportErrorMessage.REPORT_REASON_REQUIRED.message(), HttpStatus.BAD_REQUEST);
        }
        if (content != null && content.length() > 1000) {
            throw new ApiException(ReportErrorMessage.REPORT_CONTENT_TOO_LONG.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeContent(String content) {
        return content == null || content.isBlank() ? null : content.trim();
    }

    private void saveReport(Report report) {
        try {
            reportRepository.save(report);
            reportRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ReportErrorMessage.ALREADY_REPORTED.message(), HttpStatus.BAD_REQUEST);
        }
    }

}
