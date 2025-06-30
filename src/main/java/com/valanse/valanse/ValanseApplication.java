package com.valanse.valanse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; //  추가

@EnableJpaAuditing
@SpringBootApplication
public class ValanseApplication {

	public static void main(String[] args) {
		SpringApplication.run(ValanseApplication.class, args);
	}
}
