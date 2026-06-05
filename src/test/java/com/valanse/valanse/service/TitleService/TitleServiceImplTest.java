package com.valanse.valanse.service.TitleService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.MemberProfileTitle;
import com.valanse.valanse.domain.Title;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;
import com.valanse.valanse.dto.Title.TitleCreateRequest;
import com.valanse.valanse.dto.Title.TitleUpdateRequest;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberProfileTitleRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.TitleRepository;
import com.valanse.valanse.service.PointService.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TitleServiceImplTest {

    @InjectMocks
    private TitleServiceImpl titleService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @Mock
    private MemberProfileTitleRepository memberProfileTitleRepository;

    @Mock
    private TitleRepository titleRepository;

    @Mock
    private PointService pointService;

    private Member member;
    private MemberProfile profile;

    @BeforeEach
    void setup() {
        member = Member.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("테스터")
                .name("test")
                .build();
        profile = MemberProfile.builder()
                .id(1L)
                .member(member)
                .nickname("테스트닉네임")
                .build();
    }

    @Test
    @DisplayName("getTitleList()는 기본, 보유, 미보유 칭호를 분리해서 반환한다")
    void getTitleList_기본_보유_미보유_분리() {
        Title defaultTitle = title(1L, "밸런스 새싹", TitleAcquisitionType.DEFAULT, 0L, null);
        Title ownedTitle = title(2L, "싸움 구경꾼", TitleAcquisitionType.ACHIEVEMENT, 0L, "투표 10회 참여");
        Title pointTitle = title(3L, "선택의 신", TitleAcquisitionType.POINT_PURCHASE, 300L, null);
        Title seasonTitle = title(4L, "2026 봄 논쟁왕", TitleAcquisitionType.SEASON, 0L, "시즌한정");
        MemberProfileTitle ownedProfileTitle = MemberProfileTitle.builder()
                .memberProfile(profile)
                .title(ownedTitle)
                .build();
        ownedProfileTitle.equip();

        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(titleRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc())
                .thenReturn(List.of(defaultTitle, ownedTitle, pointTitle, seasonTitle));
        when(memberProfileTitleRepository.findAllByMemberProfileMemberId(1L))
                .thenReturn(List.of(ownedProfileTitle));
        when(memberProfileTitleRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = titleService.getTitleList(1L);

        assertThat(response.defaultTitles()).hasSize(1);
        assertThat(response.defaultTitles().get(0).title()).isEqualTo("밸런스 새싹");
        assertThat(response.defaultTitles().get(0).owned()).isTrue();
        assertThat(response.defaultTitles().get(0).locked()).isFalse();

        assertThat(response.ownedTitles()).hasSize(1);
        assertThat(response.ownedTitles().get(0).title()).isEqualTo("싸움 구경꾼");
        assertThat(response.ownedTitles().get(0).equipped()).isTrue();

        assertThat(response.lockedTitles()).hasSize(2);
        assertThat(response.lockedTitles().get(0).title()).isEqualTo("선택의 신");
        assertThat(response.lockedTitles().get(0).lockReason()).isEqualTo("300P 필요");
        assertThat(response.lockedTitles().get(1).title()).isEqualTo("2026 봄 논쟁왕");
        assertThat(response.lockedTitles().get(1).lockReason()).isEqualTo("시즌한정");
    }

    @Test
    @DisplayName("getTitleList()는 누락된 기본 칭호를 자동 지급한다")
    void getTitleList_기본칭호_자동지급() {
        Title defaultTitle = title(1L, "밸런스 새싹", TitleAcquisitionType.DEFAULT, 0L, null);

        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(titleRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(defaultTitle));
        when(memberProfileTitleRepository.findAllByMemberProfileMemberId(1L)).thenReturn(List.of());
        when(memberProfileTitleRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = titleService.getTitleList(1L);

        assertThat(response.defaultTitles()).hasSize(1);
        assertThat(response.defaultTitles().get(0).owned()).isTrue();
        verify(memberProfileTitleRepository).saveAll(org.mockito.ArgumentMatchers.argThat(savedTitles ->
                containsOnlyTitle(savedTitles, defaultTitle.getId())
        ));
    }

    @Test
    @DisplayName("equipTitle()은 기존 장착 칭호를 해제하고 선택한 칭호를 장착한다")
    void equipTitle_기존칭호해제_선택칭호장착() {
        MemberProfileTitle equippedTitle = equippedProfileTitle(1L, "기존 칭호");
        MemberProfileTitle targetTitle = profileTitle(2L, "새 칭호");

        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(memberProfileTitleRepository.findByMemberProfileMemberIdAndTitleId(1L, 2L))
                .thenReturn(Optional.of(targetTitle));
        when(memberProfileTitleRepository.findAllByMemberProfileMemberIdAndEquippedTrue(1L))
                .thenReturn(List.of(equippedTitle));

        var response = titleService.equipTitle(1L, 2L);

        assertThat(equippedTitle.isEquipped()).isFalse();
        assertThat(targetTitle.isEquipped()).isTrue();
        assertThat(response.titleId()).isEqualTo(2L);
        assertThat(response.title()).isEqualTo("새 칭호");
        assertThat(response.equipped()).isTrue();
    }

    @Test
    @DisplayName("equipTitle()은 보유하지 않은 칭호를 장착할 수 없다")
    void equipTitle_보유하지않은칭호_예외() {
        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(memberProfileTitleRepository.findByMemberProfileMemberIdAndTitleId(1L, 99L))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.equipTitle(1L, 99L)
        );

        assertThat(exception.getMessage()).isEqualTo("보유하지 않은 칭호입니다.");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(memberProfileTitleRepository, never()).findAllByMemberProfileMemberIdAndEquippedTrue(1L);
    }

    @Test
    @DisplayName("equipTitle()은 삭제된 칭호를 장착할 수 없다")
    void equipTitle_삭제된칭호_예외() {
        Title inactiveTitle = Title.builder()
                .id(99L)
                .code("DELETED_TITLE")
                .name("삭제된 칭호")
                .tier(TitleTier.BASIC)
                .acquisitionType(TitleAcquisitionType.ACHIEVEMENT)
                .active(false)
                .build();
        MemberProfileTitle inactiveProfileTitle = MemberProfileTitle.builder()
                .memberProfile(profile)
                .title(inactiveTitle)
                .build();

        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(memberProfileTitleRepository.findByMemberProfileMemberIdAndTitleId(1L, 99L))
                .thenReturn(Optional.of(inactiveProfileTitle));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.equipTitle(1L, 99L)
        );

        assertThat(exception.getMessage()).isEqualTo("장착할 수 없는 칭호입니다.");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(memberProfileTitleRepository, never()).findAllByMemberProfileMemberIdAndEquippedTrue(1L);
    }

    @Test
    @DisplayName("equipTitle()은 프로필이 없으면 예외를 던진다")
    void equipTitle_프로필없음_예외() {
        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.equipTitle(1L, 1L)
        );

        assertThat(exception.getMessage()).isEqualTo("프로필이 존재하지 않습니다.");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberProfileTitleRepository, never()).findByMemberProfileMemberIdAndTitleId(1L, 1L);
    }

    @Test
    @DisplayName("purchaseTitle()은 포인트 구매형 칭호를 구매하고 포인트를 차감한다")
    void purchaseTitle_구매성공_포인트차감() {
        profile.addPoint(500L);
        Title pointTitle = title(3L, "선택의 신", TitleAcquisitionType.POINT_PURCHASE, 300L, null);

        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(titleRepository.findById(3L)).thenReturn(Optional.of(pointTitle));
        when(memberProfileTitleRepository.findByMemberProfileMemberIdAndTitleId(1L, 3L))
                .thenReturn(Optional.empty());

        var response = titleService.purchaseTitle(1L, 3L);

        assertThat(profile.getPoint()).isEqualTo(200L);
        assertThat(response.titleId()).isEqualTo(3L);
        assertThat(response.title()).isEqualTo("선택의 신");
        assertThat(response.owned()).isTrue();
        assertThat(response.remainingPoint()).isEqualTo(200L);
        verify(pointService).recordPointUsage(1L, 300L, PointType.TITLE_PURCHASE);
        verify(memberProfileTitleRepository).save(org.mockito.ArgumentMatchers.argThat(savedTitle ->
                savedTitle.getMemberProfile() == profile && savedTitle.getTitle().getId().equals(3L)
        ));
    }

    @Test
    @DisplayName("purchaseTitle()은 포인트가 부족하면 필요 포인트와 함께 예외를 던진다")
    void purchaseTitle_포인트부족_예외() {
        profile.addPoint(100L);
        Title pointTitle = title(3L, "선택의 신", TitleAcquisitionType.POINT_PURCHASE, 300L, null);

        when(memberProfileRepository.findByMemberId(1L)).thenReturn(Optional.of(profile));
        when(titleRepository.findById(3L)).thenReturn(Optional.of(pointTitle));
        when(memberProfileTitleRepository.findByMemberProfileMemberIdAndTitleId(1L, 3L))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.purchaseTitle(1L, 3L)
        );

        assertThat(exception.getMessage()).isEqualTo("포인트가 부족합니다. (필요포인트 300P 필요)");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(profile.getPoint()).isEqualTo(100L);
        verify(pointService, never()).recordPointUsage(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        verify(memberProfileTitleRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("createTitle()은 관리자가 새로운 칭호를 생성한다")
    void createTitle_관리자_칭호생성() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();
        TitleCreateRequest request = new TitleCreateRequest(
                "  CHOICE_MASTER  ",
                "  선택의 달인  ",
                "투표 참여 고수",
                300L,
                TitleTier.RARE,
                TitleAcquisitionType.POINT_PURCHASE,
                "300P 필요",
                true,
                10
        );
        Title savedTitle = Title.builder()
                .id(10L)
                .code("CHOICE_MASTER")
                .name("선택의 달인")
                .description("투표 참여 고수")
                .price(300L)
                .tier(TitleTier.RARE)
                .acquisitionType(TitleAcquisitionType.POINT_PURCHASE)
                .requirementText("300P 필요")
                .active(true)
                .displayOrder(10)
                .build();

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(admin));
        when(titleRepository.existsByCode("CHOICE_MASTER")).thenReturn(false);
        when(titleRepository.save(any(Title.class))).thenReturn(savedTitle);

        var response = titleService.createTitle(1L, request);

        assertThat(response.titleId()).isEqualTo(10L);
        assertThat(response.code()).isEqualTo("CHOICE_MASTER");
        assertThat(response.title()).isEqualTo("선택의 달인");
        assertThat(response.price()).isEqualTo(300L);
        assertThat(response.acquisitionType()).isEqualTo(TitleAcquisitionType.POINT_PURCHASE);
        verify(titleRepository).save(org.mockito.ArgumentMatchers.argThat(title ->
                title.getCode().equals("CHOICE_MASTER")
                        && title.getName().equals("선택의 달인")
                        && title.getPrice() == 300L
                        && title.getTier() == TitleTier.RARE
                        && title.getAcquisitionType() == TitleAcquisitionType.POINT_PURCHASE
                        && title.getDisplayOrder() == 10
        ));
    }

    @Test
    @DisplayName("getTitleListForAdmin()은 관리자에게 칭호 마스터 데이터 목록을 반환한다")
    void getTitleListForAdmin_관리자_칭호목록조회() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();
        Title firstTitle = title(1L, "밸런스 새싹", TitleAcquisitionType.DEFAULT, 0L, null);
        Title secondTitle = title(2L, "선택의 달인", TitleAcquisitionType.POINT_PURCHASE, 300L, "300P 필요");

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(admin));
        when(titleRepository.findAllByOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(firstTitle, secondTitle));

        var response = titleService.getTitleListForAdmin(1L);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).titleId()).isEqualTo(1L);
        assertThat(response.get(0).titleName()).isEqualTo("밸런스 새싹");
        assertThat(response.get(1).titleId()).isEqualTo(2L);
        assertThat(response.get(1).price()).isEqualTo(300L);
        assertThat(response.get(1).requirementText()).isEqualTo("300P 필요");
    }

    @Test
    @DisplayName("createTitle()은 관리자가 아니면 예외를 던진다")
    void createTitle_관리자아님_예외() {
        Member user = Member.builder().id(1L).role(Role.USER).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.createTitle(1L, validCreateRequest())
        );

        assertThat(exception.getMessage()).isEqualTo("관리자만 접근 가능합니다.");
        verify(titleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTitle()은 중복된 칭호 코드를 생성할 수 없다")
    void createTitle_중복코드_예외() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(admin));
        when(titleRepository.existsByCode("CHOICE_MASTER")).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.createTitle(1L, validCreateRequest())
        );

        assertThat(exception.getMessage()).isEqualTo("이미 존재하는 칭호 코드입니다.");
        verify(titleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTitle()은 필수값이 없으면 예외를 던진다")
    void createTitle_필수값없음_예외() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();
        TitleCreateRequest request = new TitleCreateRequest(
                "CHOICE_MASTER",
                " ",
                null,
                null,
                TitleTier.BASIC,
                TitleAcquisitionType.ACHIEVEMENT,
                null,
                null,
                null
        );

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(admin));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.createTitle(1L, request)
        );

        assertThat(exception.getMessage()).isEqualTo("칭호 이름을 입력해주세요.");
        verify(titleRepository, never()).existsByCode("CHOICE_MASTER");
        verify(titleRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTitle()은 관리자가 칭호를 수정한다")
    void updateTitle_관리자_칭호수정() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();
        Title targetTitle = title(2L, "기존 칭호", TitleAcquisitionType.ACHIEVEMENT, 0L, null);
        TitleUpdateRequest request = new TitleUpdateRequest(
                "  UPDATED_TITLE  ",
                "  수정된 칭호  ",
                "수정된 설명",
                500L,
                TitleTier.RARE,
                TitleAcquisitionType.POINT_PURCHASE,
                "500P 필요",
                false,
                20
        );

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(admin));
        when(titleRepository.findById(2L)).thenReturn(Optional.of(targetTitle));
        when(titleRepository.existsByCodeAndIdNot("UPDATED_TITLE", 2L)).thenReturn(false);

        var response = titleService.updateTitle(1L, 2L, request);

        assertThat(response.titleId()).isEqualTo(2L);
        assertThat(response.code()).isEqualTo("UPDATED_TITLE");
        assertThat(response.title()).isEqualTo("수정된 칭호");
        assertThat(response.description()).isEqualTo("수정된 설명");
        assertThat(response.price()).isEqualTo(500L);
        assertThat(response.tier()).isEqualTo(TitleTier.RARE);
        assertThat(response.acquisitionType()).isEqualTo(TitleAcquisitionType.POINT_PURCHASE);
        assertThat(response.requirementText()).isEqualTo("500P 필요");
        assertThat(response.active()).isFalse();
        assertThat(response.displayOrder()).isEqualTo(20);
        assertThat(targetTitle.getCode()).isEqualTo("UPDATED_TITLE");
        assertThat(targetTitle.getName()).isEqualTo("수정된 칭호");
    }

    @Test
    @DisplayName("updateTitle()은 중복된 칭호 코드로 수정할 수 없다")
    void updateTitle_중복코드_예외() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();
        Title targetTitle = title(2L, "기존 칭호", TitleAcquisitionType.ACHIEVEMENT, 0L, null);

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(admin));
        when(titleRepository.findById(2L)).thenReturn(Optional.of(targetTitle));
        when(titleRepository.existsByCodeAndIdNot("CHOICE_MASTER", 2L)).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.updateTitle(1L, 2L, validUpdateRequest())
        );

        assertThat(exception.getMessage()).isEqualTo("이미 존재하는 칭호 코드입니다.");
        assertThat(targetTitle.getCode()).isEqualTo("TITLE_2");
    }

    @Test
    @DisplayName("updateTitle()은 관리자가 아니면 예외를 던진다")
    void updateTitle_관리자아님_예외() {
        Member user = Member.builder().id(1L).role(Role.USER).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.updateTitle(1L, 2L, validUpdateRequest())
        );

        assertThat(exception.getMessage()).isEqualTo("관리자만 접근 가능합니다.");
        verify(titleRepository, never()).findById(2L);
    }

    @Test
    @DisplayName("deleteTitle()은 관리자가 칭호를 삭제하고 장착 회원을 기본 칭호로 변경한다")
    void deleteTitle_관리자_장착회원_기본칭호변경() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();
        Title targetTitle = title(2L, "시즌 칭호", TitleAcquisitionType.SEASON, 0L, null);
        Title defaultTitle = title(1L, "밸런스 새싹", TitleAcquisitionType.DEFAULT, 0L, null);
        MemberProfileTitle equippedTitle = MemberProfileTitle.builder()
                .memberProfile(profile)
                .title(targetTitle)
                .build();
        equippedTitle.equip();
        MemberProfileTitle fallbackProfileTitle = MemberProfileTitle.builder()
                .memberProfile(profile)
                .title(defaultTitle)
                .build();

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(admin));
        when(titleRepository.findById(2L)).thenReturn(Optional.of(targetTitle));
        when(titleRepository.findFirstByActiveTrueAndAcquisitionTypeOrderByDisplayOrderAscIdAsc(TitleAcquisitionType.DEFAULT))
                .thenReturn(Optional.of(defaultTitle));
        when(memberProfileTitleRepository.findAllByTitleIdAndEquippedTrue(2L))
                .thenReturn(List.of(equippedTitle));
        when(memberProfileTitleRepository.findByMemberProfileIdAndTitleId(1L, 1L))
                .thenReturn(Optional.of(fallbackProfileTitle));

        var response = titleService.deleteTitle(1L, 2L);

        assertThat(targetTitle.isActive()).isFalse();
        assertThat(equippedTitle.isEquipped()).isFalse();
        assertThat(fallbackProfileTitle.isEquipped()).isTrue();
        assertThat(response.deletedTitleId()).isEqualTo(2L);
        assertThat(response.fallbackTitleId()).isEqualTo(1L);
        assertThat(response.reassignedCount()).isEqualTo(1);
        assertThat(response.active()).isFalse();
    }

    @Test
    @DisplayName("deleteTitle()은 기본 칭호를 삭제할 수 없다")
    void deleteTitle_기본칭호_예외() {
        Member admin = Member.builder().id(1L).role(Role.ADMIN).build();
        Title defaultTitle = title(1L, "밸런스 새싹", TitleAcquisitionType.DEFAULT, 0L, null);

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(admin));
        when(titleRepository.findById(1L)).thenReturn(Optional.of(defaultTitle));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.deleteTitle(1L, 1L)
        );

        assertThat(exception.getMessage()).isEqualTo("기본 칭호는 삭제할 수 없습니다.");
        assertThat(defaultTitle.isActive()).isTrue();
        verify(memberProfileTitleRepository, never()).findAllByTitleIdAndEquippedTrue(1L);
    }

    @Test
    @DisplayName("deleteTitle()은 관리자가 아니면 예외를 던진다")
    void deleteTitle_관리자아님_예외() {
        Member user = Member.builder().id(1L).role(Role.USER).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> titleService.deleteTitle(1L, 2L)
        );

        assertThat(exception.getMessage()).isEqualTo("관리자만 접근 가능합니다.");
        verify(titleRepository, never()).findById(2L);
    }

    private MemberProfileTitle equippedProfileTitle(Long titleId, String titleName) {
        MemberProfileTitle profileTitle = profileTitle(titleId, titleName);
        profileTitle.equip();
        return profileTitle;
    }

    private MemberProfileTitle profileTitle(Long titleId, String titleName) {
        Title title = title(titleId, titleName, TitleAcquisitionType.DEFAULT, 0L, null);

        return MemberProfileTitle.builder()
                .memberProfile(profile)
                .title(title)
                .build();
    }

    private Title title(
            Long titleId,
            String titleName,
            TitleAcquisitionType acquisitionType,
            long price,
            String requirementText
    ) {
        return Title.builder()
                .id(titleId)
                .code("TITLE_" + titleId)
                .name(titleName)
                .tier(TitleTier.BASIC)
                .acquisitionType(acquisitionType)
                .price(price)
                .requirementText(requirementText)
                .build();
    }

    private TitleCreateRequest validCreateRequest() {
        return new TitleCreateRequest(
                "CHOICE_MASTER",
                "선택의 달인",
                "투표 참여 고수",
                300L,
                TitleTier.RARE,
                TitleAcquisitionType.POINT_PURCHASE,
                "300P 필요",
                true,
                10
        );
    }

    private TitleUpdateRequest validUpdateRequest() {
        return new TitleUpdateRequest(
                "CHOICE_MASTER",
                "선택의 달인",
                "투표 참여 고수",
                300L,
                TitleTier.RARE,
                TitleAcquisitionType.POINT_PURCHASE,
                "300P 필요",
                true,
                10
        );
    }

    private boolean containsOnlyTitle(Iterable<MemberProfileTitle> savedTitles, Long titleId) {
        int count = 0;
        boolean matched = false;
        for (MemberProfileTitle savedTitle : savedTitles) {
            count++;
            matched = savedTitle.getTitle().getId().equals(titleId);
        }
        return count == 1 && matched;
    }
}
