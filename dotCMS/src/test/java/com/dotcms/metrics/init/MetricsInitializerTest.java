package com.dotcms.metrics.init;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.metrics.MetricsService;
import com.dotmarketing.util.Config;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Verifies that optional metrics startup resolves CDI only when enabled and cannot break boot. */
class MetricsInitializerTest {

    private final MetricsInitializer initializer = new MetricsInitializer();
    private final MetricsService service = mock(MetricsService.class);
    private MockedStatic<Config> config;
    private MockedStatic<CDIUtils> cdi;

    /** Uses default-enabled metrics and an initialized mocked service. */
    @BeforeEach
    void setUp() {
        config = mockStatic(Config.class);
        config.when(() -> Config.getBooleanProperty(anyString(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        cdi = mockStatic(CDIUtils.class);
        cdi.when(() -> CDIUtils.getBean(MetricsService.class)).thenReturn(Optional.of(service));
        when(service.isEnabled()).thenReturn(true);
    }

    /** Releases static mocks to leave other startup/configuration tests unchanged. */
    @AfterEach
    void tearDown() {
        cdi.close();
        config.close();
    }

    /** An enabled startup invokes the service to trigger the existing CDI lifecycle. */
    @Test
    void enabledMetrics_resolveAndInitializeService() {
        initializer.init();

        cdi.verify(() -> CDIUtils.getBean(MetricsService.class));
        verify(service).isEnabled();
    }

    /** Disabled instrumentation must not resolve or instantiate the metrics service. */
    @Test
    void disabledMetrics_doNotResolveService() {
        config.when(() -> Config.getBooleanProperty("metrics.enabled", true)).thenReturn(false);

        initializer.init();

        cdi.verifyNoInteractions();
    }

    /** Missing CDI is an instrumentation failure, not an application startup failure. */
    @Test
    void missingService_doesNotPreventStartup() {
        cdi.when(() -> CDIUtils.getBean(MetricsService.class)).thenReturn(Optional.empty());

        assertDoesNotThrow(initializer::init);
    }

    /** Failures creating an optional metrics bean cannot abort the other initializers. */
    @Test
    void initializationException_doesNotPreventStartup() {
        cdi.when(() -> CDIUtils.getBean(MetricsService.class))
                .thenThrow(new IllegalStateException("metrics initialization unavailable"));

        assertDoesNotThrow(initializer::init);
    }
}
