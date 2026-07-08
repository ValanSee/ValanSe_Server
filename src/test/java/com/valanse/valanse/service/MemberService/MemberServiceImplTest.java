package com.valanse.valanse.service.MemberService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @InjectMocks
    private MemberServiceImpl memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @BeforeEach
    void setupSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("1");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("회원 탈퇴 시 Member와 MemberProfile 모두 soft delete 처리")
    void 멤버삭제_MemberProfile도_함께_softDelete() {
        // given
        Member member = new Member();
        MemberProfile profile = new MemberProfile();

        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.of(profile));

        // when
        Member deletedMember = memberService.deleteMemberById();

        // then
        assertNotNull(deletedMember.getDeletedAt(), "Member의 deletedAt이 설정되어야 한다");
        assertNotNull(profile.getDeletedAt(), "MemberProfile의 deletedAt도 함께 설정되어야 한다");
        verify(memberRepository, times(1)).save(member);
        verify(memberProfileRepository, times(1)).findByMemberId(1L);
    }

    @Test
    @DisplayName("MemberProfile이 없는 회원도 탈퇴 정상 처리")
    void 멤버삭제_프로필없어도_정상처리() {
        // given
        Member member = new Member();

        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.empty()); // 프로필 없음

        // when
        Member deletedMember = memberService.deleteMemberById();

        // then
        assertNotNull(deletedMember.getDeletedAt(), "Member의 deletedAt이 설정되어야 한다");
        verify(memberRepository, times(1)).save(member);
    }

    @Test
    @DisplayName("존재하지 않는 회원 탈퇴 시 ApiException 발생")
    void 멤버삭제실패_존재하지않는_회원() {
        // given
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        // when & then
        ApiException exception = assertThrows(ApiException.class,
                () -> memberService.deleteMemberById());
        assertEquals(MemberErrorMessage.MEMBER_NOT_FOUND.message(), exception.getMessage());
    }
}
