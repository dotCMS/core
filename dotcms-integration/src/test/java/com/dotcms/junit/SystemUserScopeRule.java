package com.dotcms.junit;

import com.dotcms.auth.SystemUserScope;
import org.junit.rules.ExternalResource;

/**
 * JUnit 4 rule that runs every test of a class inside a {@link SystemUserScope}, for tests whose requests
 * stand in for server-side work acting as the built-in system user.
 */
public class SystemUserScopeRule extends ExternalResource {

    private SystemUserScope scope;

    @Override
    protected void before() {
        scope = SystemUserScope.open();
    }

    @Override
    protected void after() {
        if (scope != null) {
            scope.close();
            scope = null;
        }
    }
}
