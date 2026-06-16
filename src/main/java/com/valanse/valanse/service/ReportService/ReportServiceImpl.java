package com.valanse.valanse.service.ReportService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.AuthErrorMessage;
import com.valanse.valanse.common.message.ReportErrorMessage;
import com.valanse.valanse.common.message.VoteErrorMessage;
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
                .build();

        reportRepository.save(report);
    }

    /**
     * 관리자가 신고 누적 대상 목록을 조회하는 메서드입니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReportedTargetResponse> getReportedTargets(Member member, ReportType type, String sort) {
        if (member.getRole() != Role.ADMIN) {
            throw new ApiException(AuthErrorMessage.ADMIN_ONLY.message(), HttpStatus.FORBIDDEN);
        }
        return reportRepositoryCustom.findReportedTargets(type, sort);
    }


}
