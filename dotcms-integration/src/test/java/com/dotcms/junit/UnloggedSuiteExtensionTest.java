package com.dotcms.junit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.UnloggedTestDatabase;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Checks suite-scoped preparation without modifying the suite's own database. */
public class UnloggedSuiteExtensionTest {

    /** Multiple classes share preparation, while another suite root prepares independently. */
    @Test
    void preparesOncePerSuiteRoot() throws Exception {
        final var init = mock(IntegrationTestInitService.class);
        try (var service = mockStatic(IntegrationTestInitService.class);
             var database = mockStatic(UnloggedTestDatabase.class)) {
            service.when(IntegrationTestInitService::getInstance).thenReturn(init);
            final var extension = new UnloggedSuiteExtension();
            final var first = suiteContext("suite-one");
            extension.beforeAll(first);
            extension.beforeAll(first);
            extension.beforeAll(suiteContext("suite-two"));
            database.verify(() -> UnloggedTestDatabase.prepare("suite-one"), times(1));
            database.verify(() -> UnloggedTestDatabase.prepare("suite-two"), times(1));
        }
    }

    /** A failed conversion blocks every class without retrying the partial setup. */
    @Test
    void remembersFailureForEveryClass() throws Exception {
        final var init = mock(IntegrationTestInitService.class);
        final var failure = new Exception("conversion failed");
        try (var service = mockStatic(IntegrationTestInitService.class);
             var database = mockStatic(UnloggedTestDatabase.class)) {
            service.when(IntegrationTestInitService::getInstance).thenReturn(init);
            database.when(() -> UnloggedTestDatabase.prepare("failing-suite")).thenThrow(failure);
            final var extension = new UnloggedSuiteExtension();
            final var context = suiteContext("failing-suite");
            assertSame(failure, assertThrows(Exception.class, () -> extension.beforeAll(context)));
            assertSame(failure, assertThrows(Exception.class, () -> extension.beforeAll(context)));
            org.mockito.Mockito.verify(init, times(1)).init();
            database.verify(() -> UnloggedTestDatabase.prepare("failing-suite"), times(1));
        }
    }

    /** The default empty SQL path never even opens a connection. */
    @Test
    void disabledPreparationDoesNotAccessDatabase() throws Exception {
        try (var config = mockStatic(Config.class);
             var database = mockStatic(DbConnectionFactory.class)) {
            config.when(() -> Config.getStringProperty("test.unlogged.sql", "")).thenReturn("");
            UnloggedTestDatabase.prepare("disabled-suite");
            database.verifyNoInteractions();
        }
    }

    /** Builds a suite root with a real map behind its mocked extension store. */
    @SuppressWarnings("unchecked")
    private static ExtensionContext suiteContext(final String name) {
        final var context = mock(ExtensionContext.class);
        final var store = mock(ExtensionContext.Store.class);
        final Map<Object, Object> values = new HashMap<>();
        when(context.getRoot()).thenReturn(context);
        when(context.getUniqueId()).thenReturn(name);
        when(context.getStore(any())).thenReturn(store);
        when(store.getOrComputeIfAbsent(anyString(), any(), any())).thenAnswer(invocation -> {
            final Function<Object, Object> creator = invocation.getArgument(1);
            return values.computeIfAbsent(invocation.getArgument(0), creator);
        });
        return context;
    }
}
