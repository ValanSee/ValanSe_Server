package com.valanse.valanse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.dto.Analytics.PageViewEventRequest;
import com.valanse.valanse.repository.ActivityEventRepository;
import com.valanse.valanse.repository.AnonymousUserLinkRepository;
import com.valanse.valanse.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActivityEventRepository activityEventRepository;

    @Autowired
    private AnonymousUserLinkRepository anonymousUserLinkRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        activityEventRepository.deleteAll();
        anonymousUserLinkRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("페이지 방문 수집 API는 비로그인 사용자도 호출할 수 있다")
    void recordPageView_AllowsAnonymousRequest() throws Exception {
        PageViewEventRequest request = new PageViewEventRequest("anon-controller", "/");

        mockMvc.perform(post("/analytics/events/page-view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").isNumber());

        assertThat(activityEventRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("관리자 MAU 조회 API는 비로그인 사용자가 접근할 수 없다")
    void getMau_AnonymousUser_IsForbidden() throws Exception {
        mockMvc.perform(get("/admin/analytics/mau")
                        .param("yearMonth", "2026-06"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("관리자 MAU 조회 API는 일반 사용자가 접근할 수 없다")
    void getMau_UserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/admin/analytics/mau")
                        .param("yearMonth", "2026-06"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자 MAU 조회 API는 ADMIN 권한으로 접근할 수 있다")
    void getMau_AdminRole_IsOk() throws Exception {
        mockMvc.perform(get("/admin/analytics/mau")
                        .param("yearMonth", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-06"))
                .andExpect(jsonPath("$.totalMau").value(0));
    }
}
