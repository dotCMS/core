package com.dotcms.junit;

import com.dotcms.auth.SystemUserScope;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that runs every test of a class inside a {@link SystemUserScope}, for tests whose
 * requests stand in for server-side work acting as the built-in system user.
 */
public class SystemUserScopeExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(SystemUserScopeExtension.class);

    @Override
    public void beforeEach(final ExtensionContext context) {
        context.getStore(NAMESPACE).put("scope", SystemUserScope.open());
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        final SystemUserScope scope = context.getStore(NAMESPACE).remove("scope", SystemUserScope.class);
        if (scope != null) {
            scope.close();
        }
    }
}
