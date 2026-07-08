package com.valanse.valanse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; //  추가

@EnableJpaAuditing
@SpringBootApplication
/**
 * 애플리케이션 기능을 구성하는 Java 코드입니다.
 */
public class ValanseApplication {

	/**
	 * ValanseApplication의 main 기능을 수행하는 메서드입니다.
	 */
	public static void main(String[] args) {
		SpringApplication.run(ValanseApplication.class, args);
	}
}
