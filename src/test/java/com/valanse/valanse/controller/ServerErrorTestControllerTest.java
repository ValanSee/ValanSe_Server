package com.valanse.valanse.controller;

import com.valanse.valanse.common.alert.ServerErrorEvent;
import com.valanse.valanse.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServerErrorTestControllerTest {

    @Test
    @DisplayName("dev-server 프로필에서는 서버 오류 테스트 컨트롤러를 등록한다")
    void devServerProfile_RegistersController() {
        try (AnnotationConfigApplicationContext context = context("dev-server")) {
            assertThat(context.getBeansOfType(ServerErrorTestController.class)).hasSize(1);
        }
    }

    @Test
    @DisplayName("prod 프로필에서는 서버 오류 테스트 컨트롤러를 등록하지 않는다")
    void prodProfile_DoesNotRegisterController() {
        try (AnnotationConfigApplicationContext context = context("prod")) {
            assertThat(context.getBeansOfType(ServerErrorTestController.class)).isEmpty();
        }
    }

    @Test
    @DisplayName("테스트 API 호출 시 traceId가 포함된 500 응답과 서버 오류 이벤트를 생성한다")
    void serverError_ReturnsTraceIdAndPublishesEvent() throws Exception {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(eventPublisher);
        ReflectionTestUtils.setField(exceptionHandler, "activeProfiles", "dev-server");

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ServerErrorTestController())
                .setControllerAdvice(exceptionHandler)
                .build();

        mockMvc.perform(get("/admin/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(header().exists("X-Trace-Id"));

        verify(eventPublisher).publishEvent(any(ServerErrorEvent.class));
    }

    private AnnotationConfigApplicationContext context(String activeProfile) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(activeProfile);
        context.register(ServerErrorTestController.class);
        context.refresh();
        return context;
    }
}
