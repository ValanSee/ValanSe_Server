package com.valanse.valanse.common.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 오늘의 밸런스 선정·정산 스케줄러 설정(daily-vote.*)을 바인딩하는 프로퍼티 코드입니다.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "daily-vote")
public class DailyVoteProperties {

    private boolean enabled = true;

    @NotBlank
    private String selectCron = "0 30 23 * * *";

    @NotBlank
    private String settleCron = "0 0 0 * * *";

    @NotBlank
    private String safetyCron = "0 5 * * * *";

    // ClockConfig 의 Clock(Asia/Seoul)과 반드시 같은 값이어야 한다
    @NotBlank
    private String zone = "Asia/Seoul";
}
