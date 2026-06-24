package com.valanse.valanse.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("prod profile에서는 예상치 못한 예외의 내부 상세를 응답하지 않는다")
    void handleUnexpectedException_ProdProfile_HidesInternalDetails() throws Exception {
        MockMvc mockMvc = mockMvc("prod");

        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.type").doesNotExist());
    }

    @Test
    @DisplayName("test profile에서는 예상치 못한 예외의 디버깅 상세를 응답한다")
    void handleUnexpectedException_TestProfile_IncludesDebugDetails() throws Exception {
        MockMvc mockMvc = mockMvc("test");

        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("boom"))
                .andExpect(jsonPath("$.type").value("IllegalStateException"));
    }

    private MockMvc mockMvc(String activeProfiles) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "activeProfiles", activeProfiles);

        return MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(handler)
                .build();
    }

    @RestController
    static class FailingController {

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }
    }
}
