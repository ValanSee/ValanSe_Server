package com.valanse.valanse;

import com.valanse.valanse.common.config.QueryDSLConfig;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Import(QueryDSLConfig.class)
public class MemberProfileJpaTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    MemberProfileRepository memberProfileRepository;

    @Test
    void memberProfile_PK는_Member_PK_와_같다() {
        Member member = memberRepository.save(new Member());

        MemberProfile profile = MemberProfile.builder()
                .member(member)
                .nickname("테스트")
                .build();

        MemberProfile saved = memberProfileRepository.save(profile);

        Assertions.assertThat(saved.getId()).isEqualTo(member.getId());
    }
}
