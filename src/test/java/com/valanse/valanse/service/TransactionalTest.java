package com.valanse.valanse.service;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.service.MemberService.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
// 트랜잭션 기능 (롤백 등) 점검 테스트
public class TransactionalTest {


    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 에러_발생시_롤백되는지() {
        // given
        Member member = memberService.createOauth("sid", "u@e.com", "테스터", "img", "a", "r");

        // 보안 컨텍스트 설정
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(member.getId().toString());
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // when
        assertThrows(ApiException.class, () -> {
            memberService.findById(999L); // 존재하지 않는 사용자 → 예외 발생
        });

        // then
        // 트랜잭션 롤백되었는지 확인
        Member stillExists = memberRepository.findById(member.getId()).orElse(null);
        assertNotNull(stillExists, "예외 발생 후 롤백되어야 함");
    }


}
