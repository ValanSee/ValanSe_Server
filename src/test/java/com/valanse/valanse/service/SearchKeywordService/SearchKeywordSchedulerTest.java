package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.common.config.SearchKeywordProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SearchKeywordSchedulerTest {

    @Mock private SearchKeywordCollector collector;

    @Test
    void disabled_run_does_not_touch_collector() {
        SearchKeywordProperties properties = new SearchKeywordProperties();
        properties.setEnabled(false);

        new SearchKeywordScheduler(collector, properties).run();

        verifyNoInteractions(collector);
    }

    @Test
    void enabled_run_collects_once() {
        SearchKeywordProperties properties = new SearchKeywordProperties();
        properties.setEnabled(true);

        new SearchKeywordScheduler(collector, properties).run();

        verify(collector, times(1)).collect();
    }
}
