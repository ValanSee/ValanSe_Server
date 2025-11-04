package com.valanse.valanse.service.ReportService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Report;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.ReportRepository;
import com.valanse.valanse.repository.VoteRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private CommentRepository commentRepository;

    @Test
    void 댓글_신고() {
        //given
        Member member = Member.builder().id(1L).build();
        Member writer = Member.builder().id(2L).build();
        Comment comment = Comment.builder().member(writer).build();

        //stub
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        when(reportRepository.existsByMemberAndReportTypeAndTargetId(any(), any(), any())).thenReturn(false);

        //when
        reportService.report(member, 1L, ReportType.COMMENT);

        //then
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report report = captor.getValue();
        assertEquals(member, report.getMember());
    }

    @Test
    void 게임_신고() {
        //given
        Member member = Member.builder().id(1L).build();
        Member writer = Member.builder().id(2L).build();
        Vote vote = Vote.builder().member(writer).build();

        //stub
        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(reportRepository.existsByMemberAndReportTypeAndTargetId(any(), any(), any())).thenReturn(false);

        //when
        reportService.report(member, 1L, ReportType.VOTE);

        //then
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report report = captor.getValue();
        assertEquals(member, report.getMember());
    }

    @Test
    void 본인_댓글_신고_불가() {
        //given
        Member writer = Member.builder().id(2L).build();
        Comment comment = Comment.builder().member(writer).build();

        //stub
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));

        //when
        ApiException apiException = assertThrows(ApiException.class, () -> reportService.report(writer, 1L, ReportType.COMMENT));

        //then
        assertThat(apiException.getMessage()).isEqualTo("자신의 댓글은 신고할 수 없습니다.");
    }

    @Test
    void 본인_게임_신고_불가() {
        //given
        Member writer = Member.builder().id(2L).build();
        Vote vote = Vote.builder().member(writer).build();

        //stub
        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));

        //when
        ApiException apiException = assertThrows(ApiException.class, () -> reportService.report(writer, 1L, ReportType.VOTE));

        //then
        assertThat(apiException.getMessage()).isEqualTo("자신의 투표는 신고할 수 없습니다.");
    }

    @Test
    void 중복_신고_불가() {
        //given
        Member member = Member.builder().id(1L).build();
        Member writer = Member.builder().id(2L).build();
        Comment comment = Comment.builder().member(writer).build();

        //stub
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        when(reportRepository.existsByMemberAndReportTypeAndTargetId(any(), any(), any())).thenReturn(true);

        //when
        ApiException apiException = assertThrows(ApiException.class, () -> reportService.report(member, 1L, ReportType.COMMENT));

        //then
        assertThat(apiException.getMessage()).isEqualTo("이미 신고한 대상입니다.");
    }


}