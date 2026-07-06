package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.enums.ReportReason;
import com.valanse.valanse.domain.enums.ReportType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void reportType과_reason은_필수다() {
        ReportRequest request = new ReportRequest();

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("reportType", "reason");
    }

    @Test
    void content는_1000자까지_허용한다() {
        ReportRequest request = new ReportRequest();
        request.setReportType(ReportType.VOTE);
        request.setReason(ReportReason.SPAM);
        request.setContent("a".repeat(1001));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("content");
    }
}
