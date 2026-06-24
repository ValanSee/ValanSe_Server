package com.valanse.valanse.service.ReportService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.ReportErrorMessage;
import com.valanse.valanse.common.message.VoteErrorMessage;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Report;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.ReportReason;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.ReportRepository;
import com.valanse.valanse.repository.VoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @InjectMocks private ReportServiceImpl reportService;

    @Mock private ReportRepository reportRepository;
    @Mock private VoteRepository voteRepository;
    @Mock private CommentRepository commentRepository;

    // ──────────────────────────────────────────────
    // 정상 신고
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("타인의 댓글을 신고하면 Report가 저장된다")
    void 댓글_신고_성공() {
        Member reporter = Member.builder().id(1L).build();
        Member writer = Member.builder().id(2L).build();
        Comment comment = Comment.builder().member(writer).build();

        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        when(reportRepository.existsByMemberAndReportTypeAndTargetId(any(), any(), any())).thenReturn(false);

        reportService.report(reporter, 1L, ReportType.COMMENT);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getMember()).isEqualTo(reporter);
    }

    @Test
    @DisplayName("타인의 투표를 신고하면 Report가 저장된다")
    void 게임_신고_성공() {
        Member reporter = Member.builder().id(1L).build();
        Member writer = Member.builder().id(2L).build();
        Vote vote = Vote.builder().member(writer).build();

        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(reportRepository.existsByMemberAndReportTypeAndTargetId(any(), any(), any())).thenReturn(false);

        reportService.report(reporter, 1L, ReportType.VOTE);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getMember()).isEqualTo(reporter);
    }

    @Test
    @DisplayName("신고 사유와 내용을 함께 저장한다")
    void 신고_사유_내용_저장() {
        Member reporter = Member.builder().id(1L).build();
        Member writer = Member.builder().id(2L).build();
        Vote vote = Vote.builder().member(writer).build();

        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(reportRepository.existsByMemberAndReportTypeAndTargetId(any(), any(), any())).thenReturn(false);

        reportService.report(reporter, 1L, ReportType.VOTE, ReportReason.SPAM, "반복 광고성 게시물입니다.");

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(captor.getValue().getContent()).isEqualTo("반복 광고성 게시물입니다.");
    }

    @Test
    @DisplayName("신고 사유가 없으면 예외가 발생한다")
    void 신고_사유_검증() {
        Member reporter = Member.builder().id(1L).build();

        ApiException ex = assertThrows(ApiException.class,
                () -> reportService.report(reporter, 1L, ReportType.VOTE, null, "내용"));

        assertThat(ex.getMessage()).isEqualTo(ReportErrorMessage.REPORT_REASON_REQUIRED.message());
        verify(reportRepository, never()).save(any());
        verify(voteRepository, never()).findById(any());
        verify(commentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("신고 상세 내용은 비어 있어도 저장할 수 있다")
    void 신고_내용_선택값() {
        Member reporter = Member.builder().id(1L).build();
        Member writer = Member.builder().id(2L).build();
        Vote vote = Vote.builder().member(writer).build();

        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(reportRepository.existsByMemberAndReportTypeAndTargetId(any(), any(), any())).thenReturn(false);

        reportService.report(reporter, 1L, ReportType.VOTE, ReportReason.SPAM, "");

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isNull();
    }

    // ──────────────────────────────────────────────
    // 자기 자신 신고 불가
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("본인 댓글은 신고할 수 없다")
    void 본인_댓글_신고_불가() {
        Member writer = Member.builder().id(2L).build();
        Comment comment = Comment.builder().member(writer).build();

        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));

        ApiException ex = assertThrows(ApiException.class,
                () -> reportService.report(writer, 1L, ReportType.COMMENT));
        assertThat(ex.getMessage()).isEqualTo(ReportErrorMessage.OWN_COMMENT_REPORT_NOT_ALLOWED.message());
    }

    @Test
    @DisplayName("본인 투표는 신고할 수 없다")
    void 본인_게임_신고_불가() {
        Member writer = Member.builder().id(2L).build();
        Vote vote = Vote.builder().member(writer).build();

        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));

        ApiException ex = assertThrows(ApiException.class,
                () -> reportService.report(writer, 1L, ReportType.VOTE));
        assertThat(ex.getMessage()).isEqualTo(ReportErrorMessage.OWN_VOTE_REPORT_NOT_ALLOWED.message());
    }

    // ──────────────────────────────────────────────
    // 중복 신고 방지
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("같은 댓글을 두 번 신고하면 예외가 발생한다")
    void 중복_신고_불가() {
        Member reporter = Member.builder().id(1L).build();
        Member writer = Member.builder().id(2L).build();
        Comment comment = Comment.builder().member(writer).build();

        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        when(reportRepository.existsByMemberAndReportTypeAndTargetId(any(), any(), any())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> reportService.report(reporter, 1L, ReportType.COMMENT));
        assertThat(ex.getMessage()).isEqualTo(ReportErrorMessage.ALREADY_REPORTED.message());
    }

    @Test
    @DisplayName("같은 투표를 두 번 신고하면 예외가 발생한다")
    void 중복_게임_신고_불가() {
        Member reporter = Member.builder().id(1L).build();
        Member writer = Member.builder().id(2L).build();
        Vote vote = Vote.builder().member(writer).build();

        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(reportRepository.existsByMemberAndReportTypeAndTargetId(any(), any(), any())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> reportService.report(reporter, 1L, ReportType.VOTE));
        assertThat(ex.getMessage()).isEqualTo(ReportErrorMessage.ALREADY_REPORTED.message());
    }

    // ──────────────────────────────────────────────
    // 존재하지 않는 대상 신고
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 댓글을 신고하면 예외가 발생한다")
    void 존재하지않는_댓글_신고() {
        Member reporter = Member.builder().id(1L).build();
        when(commentRepository.findById(any())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> reportService.report(reporter, 999L, ReportType.COMMENT));
        assertThat(ex.getMessage()).isEqualTo(ReportErrorMessage.COMMENT_NOT_FOUND.message());
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 투표를 신고하면 예외가 발생한다")
    void 존재하지않는_투표_신고() {
        Member reporter = Member.builder().id(1L).build();
        when(voteRepository.findById(any())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> reportService.report(reporter, 999L, ReportType.VOTE));
        assertThat(ex.getMessage()).isEqualTo(VoteErrorMessage.VOTE_DETAIL_NOT_FOUND.message());
        verify(reportRepository, never()).save(any());
    }
}
