package com.valanse.valanse.domain;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V17__add_daily_vote.sql 의 컬럼명과 JPA 매핑의 물리 컬럼명이 일치하는지 검증한다 (Docker 불필요).
 * H2 는 ddl-auto: create 라 스키마 불일치를 못 잡으므로 매핑 메타데이터를 직접 비교한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class DailyVoteColumnMappingTest {

    @Autowired
    private EntityManagerFactory emf;

    @Test
    void daily_vote_컬럼명이_V17과_일치한다() {
        assertThat(columns(DailyVote.class)).containsExactlyInAnyOrder(
                "vote_date", "vote_id", "source", "question_snapshot", "option_a_snapshot", "option_b_snapshot",
                "option_a_id", "option_b_id", "winner", "settled_a_count", "settled_b_count", "settled_at",
                "created_at", "updated_at");
    }

    @Test
    void daily_vote_participation_컬럼명이_V17과_일치한다() {
        assertThat(columns(DailyVoteParticipation.class)).containsExactlyInAnyOrder(
                "id", "vote_date", "vote_id", "member_id", "first_option_id", "current_option_id",
                "participation_point", "bonus_point", "created_at");
    }

    private Set<String> columns(Class<?> entity) {
        MappingMetamodel mm = emf.unwrap(SessionFactoryImplementor.class).getMappingMetamodel();
        AbstractEntityPersister p = (AbstractEntityPersister) mm.getEntityDescriptor(entity);
        Set<String> cols = new LinkedHashSet<>();
        for (String prop : p.getPropertyNames()) {
            cols.addAll(Arrays.asList(p.getPropertyColumnNames(prop)));
        }
        cols.addAll(Arrays.asList(p.getIdentifierColumnNames()));
        return cols;
    }
}
