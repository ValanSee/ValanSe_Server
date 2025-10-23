package com.valanse.valanse.service.MemberService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
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

// Repository(db) 에 의존적인 메서드는 테스트 하지 않는다.
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @InjectMocks
    private MemberServiceImpl memberService;

    @Mock
    private MemberRepository memberRepository;

    @BeforeEach
    void setupSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("1"); // userId=1 가정
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
        void 멤버삭제_test() {
        // given
        Member member = Member.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("테스트")
                .name("test")
                .role(Role.USER)
                .build();

        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Member deletedMember = memberService.deleteMemberById();

        // then
        assertNotNull(deletedMember.getDeletedAt(), "삭제 시간이 설정되어야 합니다.");
        verify(memberRepository).save(member);
    }

    @Test
    void 멤버삭제실패_test() {
        // given
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        // when & then
        ApiException exception = assertThrows(ApiException.class,
                () -> memberService.deleteMemberById());
        assertEquals("사용자를 찾을 수 없습니다", exception.getMessage());
    }
}