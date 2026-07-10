package com.valanse.valanse.service.ReportService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Report;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.ReportReason;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.dto.Report.PagedReportedTargetResponse;
import com.valanse.valanse.dto.Report.ReportDetailResponse;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.ReportRepository;
import com.valanse.valanse.repository.VoteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportIntegrationTest {

    @Autowired private ReportService reportService;
    @Autowired private ReportRepository reportRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("복합 unique 제약이 같은 사용자의 동일 대상 중복 신고를 막는다")
    void uniqueConstraintPreventsDuplicateReports() {
        Member reporter = saveMember("unique-reporter", Role.USER);
        Member writer = saveMember("unique-writer", Role.USER);
        Vote vote = saveVote(writer, "unique vote");

        reportRepository.saveAndFlush(newReport(reporter, vote.getId(), ReportReason.SPAM, null));

        assertThatThrownBy(() ->
                reportRepository.saveAndFlush(newReport(reporter, vote.getId(), ReportReason.ETC, "duplicate")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("관리자 신고 목록에서 삭제된 대상은 제외한다")
    void reportedTargetsExcludeDeletedTarget() {
        Member admin = saveMember("list-admin", Role.ADMIN);
        Member reporter = saveMember("list-reporter", Role.USER);
        Member writer = saveMember("list-writer", Role.USER);
        Vote activeVote = saveVote(writer, "active vote");
        Vote deletedVote = saveVote(writer, "deleted vote");

        reportRepository.save(newReport(reporter, activeVote.getId(), ReportReason.SPAM, null));
        reportRepository.save(newReport(reporter, deletedVote.getId(), ReportReason.ETC, null));
        deletedVote.softDelete();
        entityManager.flush();
        entityManager.clear();

        PagedReportedTargetResponse results =
                reportService.getReportedTargets(admin, ReportType.VOTE, "latest", PageRequest.of(0, 10));

        assertThat(results.getReports())
                .extracting(ReportedTargetResponse::getTargetId)
                .containsExactly(activeVote.getId());
        assertThat(results.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("관리자 상세 조회는 신고자와 사유, 내용을 반환한다")
    void reportDetailReturnsIndividualReports() {
        Member admin = saveMember("detail-admin", Role.ADMIN);
        Member reporter = saveMember("detail-reporter", Role.USER);
        Member writer = saveMember("detail-writer", Role.USER);
        Vote vote = saveVote(writer, "detail vote");
        reportRepository.saveAndFlush(newReport(
                reporter,
                vote.getId(),
                ReportReason.COMMERCIAL_OR_PROMOTIONAL,
                "홍보성 내용입니다."));
        entityManager.clear();

        ReportDetailResponse response =
                reportService.getReportDetail(admin, ReportType.VOTE, vote.getId());

        assertThat(response.getTargetId()).isEqualTo(vote.getId());
        assertThat(response.getReportCount()).isEqualTo(1);
        assertThat(response.getReports().get(0).getReporterNickname()).isEqualTo("detail-reporter");
        assertThat(response.getReports().get(0).getReason())
                .isEqualTo(ReportReason.COMMERCIAL_OR_PROMOTIONAL);
        assertThat(response.getReports().get(0).getContent()).isEqualTo("홍보성 내용입니다.");
        assertThat(response.getReports().get(0).getReportedAt()).isNotNull();
    }

    @Test
    @DisplayName("일반 회원은 관리자 신고 상세를 조회할 수 없다")
    void reportDetailRejectsNonAdmin() {
        Member member = saveMember("detail-user", Role.USER);

        assertThatThrownBy(() -> reportService.getReportDetail(member, ReportType.VOTE, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("관리자만 접근 가능합니다.");
    }

    private Member saveMember(String nickname, Role role) {
        return memberRepository.saveAndFlush(Member.builder()
                .socialId(nickname)
                .nickname(nickname)
                .role(role)
                .build());
    }

    private Vote saveVote(Member writer, String title) {
        return voteRepository.saveAndFlush(Vote.builder()
                .member(writer)
                .category(VoteCategory.ETC)
                .title(title)
                .content(title + " content")
                .pinType(PinType.NONE)
                .build());
    }

    private Report newReport(Member reporter, Long targetId, ReportReason reason, String content) {
        return Report.builder()
                .member(reporter)
                .reportType(ReportType.VOTE)
                .targetId(targetId)
                .reason(reason)
                .content(content)
                .build();
    }
}
