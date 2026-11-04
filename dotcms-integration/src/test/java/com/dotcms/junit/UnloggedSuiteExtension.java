package com.dotcms.junit;

import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.UnloggedTestDatabase;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Prepares once per Jupiter engine (each JUnit Platform suite has its own engine root). */
public class UnloggedSuiteExtension implements BeforeAllCallback {

    /**
     * Stores both success and failure at the suite root so later classes never retry preparation.
     *
     * @param context the class about to run
     * @throws Exception if initialization or conversion failed
     */
    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        final Exception failure = context.getRoot().getStore(
                ExtensionContext.Namespace.create(UnloggedSuiteExtension.class))
                .getOrComputeIfAbsent("preparation", key -> {
                    try {
                        IntegrationTestInitService.getInstance().init();
                        UnloggedTestDatabase.prepare(context.getConfigurationParameter("dotcms.test.suite")
                                .orElse(context.getRoot().getUniqueId()));
                        return new Preparation(null);
                    } catch (Exception e) {
                        return new Preparation(e);
                    }
                }, Preparation.class).failure();
        if (failure != null) {
            throw failure;
        }
    }

    /** Remembers failed preparation as well as successful preparation. */
    private record Preparation(Exception failure) {
    }
}
