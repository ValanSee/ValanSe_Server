package com.valanse.valanse.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;
import com.valanse.valanse.common.alert.ServerErrorEvent;

class GlobalExceptionHandlerTest {

    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
    }

    @Test
    @DisplayName("prod profile에서는 예상치 못한 예외의 내부 상세를 응답하지 않는다")
    void handleUnexpectedException_ProdProfile_HidesInternalDetails() throws Exception {
        MockMvc mockMvc = mockMvc("prod");

        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(header().exists("X-Trace-Id"))
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
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.message").value("boom"))
                .andExpect(jsonPath("$.type").value("IllegalStateException"));
    }

    @Test
    @DisplayName("예상하지 못한 예외가 발생하면 서버 오류 이벤트를 발행한다")
    void handleUnexpectedException_PublishesServerErrorEvent() throws Exception {
        MockMvc mockMvc = mockMvc("prod");

        String responseTraceId = mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andReturn()
                .getResponse()
                .getHeader("X-Trace-Id");

        ArgumentCaptor<ServerErrorEvent> captor = ArgumentCaptor.forClass(ServerErrorEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        ServerErrorEvent event = captor.getValue();
        assertThat(event.status()).isEqualTo(500);
        assertThat(event.environment()).isEqualTo("prod");
        assertThat(event.httpMethod()).isEqualTo("GET");
        assertThat(event.requestUri()).isEqualTo("/boom");
        assertThat(event.exceptionType()).isEqualTo("IllegalStateException");
        assertThat(event.traceId()).isEqualTo(responseTraceId);
    }

    @Test
    @DisplayName("제어된 ApiException도 5xx이면 서버 오류 이벤트를 발행한다")
    void handleApiException_ServerError_PublishesServerErrorEvent() throws Exception {
        MockMvc mockMvc = mockMvc("prod");

        mockMvc.perform(get("/controlled-server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(header().exists("X-Trace-Id"));

        ArgumentCaptor<ServerErrorEvent> captor = ArgumentCaptor.forClass(ServerErrorEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().exceptionType()).isEqualTo("ApiException");
        assertThat(captor.getValue().status()).isEqualTo(500);
    }

    @Test
    @DisplayName("4xx ApiException은 서버 오류 이벤트를 발행하지 않는다")
    void handleApiException_ClientError_DoesNotPublishServerErrorEvent() throws Exception {
        MockMvc mockMvc = mockMvc("prod");

        mockMvc.perform(get("/controlled-client-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").doesNotExist())
                .andExpect(header().doesNotExist("X-Trace-Id"));

        verify(eventPublisher, never()).publishEvent(any());
    }

    private MockMvc mockMvc(String activeProfiles) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(eventPublisher);
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

        @GetMapping("/controlled-server-error")
        void controlledServerError() {
            throw new ApiException("controlled boom", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @GetMapping("/controlled-client-error")
        void controlledClientError() {
            throw new ApiException("bad request", HttpStatus.BAD_REQUEST);
        }
    }
}
