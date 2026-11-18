package com.valanse.valanse.controller;

import com.valanse.valanse.domain.enums.SearchKeywordSource;
import com.valanse.valanse.service.SearchKeywordService.SearchKeywordCollectResult;
import com.valanse.valanse.service.SearchKeywordService.SearchKeywordCollector;
import com.valanse.valanse.service.SearchKeywordService.SearchKeywordScheduler;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchKeywordAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private LockProvider lockProvider;
    @MockBean private SearchKeywordCollector collector;

    @Test
    @WithMockUser(roles = "ADMIN")
    void 락을_획득하면_200과_결과를_반환하고_락을_해제한다() throws Exception {
        SimpleLock simpleLock = mock(SimpleLock.class);
        ArgumentCaptor<LockConfiguration> captor = ArgumentCaptor.forClass(LockConfiguration.class);
        when(lockProvider.lock(captor.capture())).thenReturn(Optional.of(simpleLock));
        when(collector.collect()).thenReturn(new SearchKeywordCollectResult(
                Map.of(SearchKeywordSource.GOOGLE_SEARCH_CONSOLE, 2),
                Map.of(SearchKeywordSource.NAVER_SEARCH_AD, "boom")));

        mockMvc.perform(post("/admin/search-keywords/collect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCounts.GOOGLE_SEARCH_CONSOLE").value(2))
                .andExpect(jsonPath("$.failures.NAVER_SEARCH_AD").value("boom"));

        assertThat(captor.getValue().getName()).isEqualTo(SearchKeywordScheduler.LOCK_NAME);
        verify(simpleLock).unlock();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 락_획득에_실패하면_409를_반환하고_수집하지_않는다() throws Exception {
        when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/admin/search-keywords/collect"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("검색 키워드 수집이 이미 실행 중입니다."));

        verify(collector, never()).collect();
    }

    @Test
    @WithMockUser(roles = "USER")
    void 일반_사용자는_403이다() throws Exception {
        mockMvc.perform(post("/admin/search-keywords/collect"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lockProvider);
        verifyNoInteractions(collector);
    }
}
