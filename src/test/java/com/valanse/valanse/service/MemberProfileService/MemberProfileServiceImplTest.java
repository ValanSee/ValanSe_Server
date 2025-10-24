package com.valanse.valanse.service.MemberProfileService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.enums.*;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class MemberProfileServiceImplTest {

    @InjectMocks
    private MemberProfileServiceImpl memberProfileService;

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @Mock
    private MemberRepository memberRepository;

    private Member member;
    private MemberProfileRequest baseDto;

    @BeforeEach
    void setupSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("1"); // userId=1 가정
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @BeforeEach
    void setup() {
        member = Member.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("테스트")
                .name("test")
                .role(Role.USER)
                .build();

        baseDto = new MemberProfileRequest("테스트", Gender.MALE, Age.TWENTY, MbtiIe.E, MbtiTf.T, "ENTP");
    }

    @Test
    void 프로필생성_test() {
        // given
        MemberProfile newProfile = MemberProfile.builder()
                .member(member)
                .nickname(baseDto.nickname())
                .gender(baseDto.gender())
                .age(baseDto.age())
                .mbtiIe(baseDto.mbtiIe())
                .mbtiTf(baseDto.mbtiTf())
                .mbti(baseDto.mbti())
                .build();

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.empty());
        when(memberProfileRepository.save(any())).thenReturn(newProfile);

        //when
        memberProfileService.saveOrUpdateProfile(baseDto);

        // then: memberProfile 저장 1회, savedProfile 객체 속성 확인
        ArgumentCaptor<MemberProfile> captor = ArgumentCaptor.forClass(MemberProfile.class);
        verify(memberProfileRepository, times(1)).save(captor.capture());

        MemberProfile savedProfile = captor.getValue();

        assertEquals("테스트", savedProfile.getNickname());
        assertEquals(Gender.MALE, savedProfile.getGender());
        assertEquals(Age.TWENTY, savedProfile.getAge());
        assertEquals(member, savedProfile.getMember());
    }

    @Test
    void 프로필수정_test() {
        // given
        MemberProfile oldProfile = MemberProfile.builder()
                .member(member)
                .nickname(baseDto.nickname())
                .gender(baseDto.gender())
                .age(baseDto.age())
                .mbtiIe(baseDto.mbtiIe())
                .mbtiTf(baseDto.mbtiTf())
                .mbti(baseDto.mbti())
                .build();

        MemberProfileRequest newDto = new MemberProfileRequest("테스트123", Gender.FEMALE, Age.TEN, MbtiIe.I, MbtiTf.F, "INFP");

        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(member));
        when(memberProfileRepository.findByMemberId(1L))
                .thenReturn(Optional.of(oldProfile));

        //when
        memberProfileService.saveOrUpdateProfile(newDto);

        // then: memberProfile 저장 1회, savedProfile 객체 속성 검증
        ArgumentCaptor<MemberProfile> captor = ArgumentCaptor.forClass(MemberProfile.class);
        verify(memberProfileRepository, times(1)).save(captor.capture());

        MemberProfile savedProfile = captor.getValue();

        assertEquals("테스트123", savedProfile.getNickname());
        assertEquals(Gender.FEMALE, savedProfile.getGender());
        assertEquals(Age.TEN, savedProfile.getAge());
        assertEquals(member, savedProfile.getMember());
    }




}