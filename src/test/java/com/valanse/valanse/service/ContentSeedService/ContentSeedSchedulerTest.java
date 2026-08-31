package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.common.config.ContentSeedProperties;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ContentSeedSchedulerTest {

    @Test
    void enabled이면_트리거를_SCHEDULED로_실행한다() {
        ContentSeedRunner runner = mock(ContentSeedRunner.class);
        ContentSeedProperties properties = new ContentSeedProperties();
        properties.setEnabled(true);
        ContentSeedScheduler scheduler = new ContentSeedScheduler(runner, properties);

        scheduler.run();

        verify(runner).runAndNotify("SCHEDULED");
    }

    @Test
    void disabled이면_아무것도_실행하지_않는다() {
        ContentSeedRunner runner = mock(ContentSeedRunner.class);
        ContentSeedProperties properties = new ContentSeedProperties();
        properties.setEnabled(false);
        ContentSeedScheduler scheduler = new ContentSeedScheduler(runner, properties);

        scheduler.run();

        verifyNoInteractions(runner);
    }
}
