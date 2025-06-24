package com.valanse.valanse.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//QueryDSL은 JPA 쿼리를 더 쉽고 안전하게 작성할 수 있도록 돕는 라이브러리입니다. 이 설정 파일은 QueryDSL의 JPAQueryFactory
//빈을 등록하여 데이터베이스 쿼리를 동적으로 생성할 수 있도록 합니다.
@Configuration
@RequiredArgsConstructor
public class QueryDSLConfig {
    private final EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory(){
        return new JPAQueryFactory(entityManager);
    }
}