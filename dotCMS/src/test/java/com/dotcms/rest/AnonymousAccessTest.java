package com.dotcms.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotmarketing.util.Config;
import org.junit.Test;

/**
 * Covers the config-backed accessors on {@link AnonymousAccess}.
 */
public class AnonymousAccessTest extends UnitTestBase {

    /**
     * When SCRIPTING_APIS_ALLOW_ANONYMOUS is unset, anonymous callers are kept out of the
     * scripting endpoints; setting it to true lets them in.
     */
    @Test
    public void scriptingEndpointsAllowAnonymous_defaultsToFalse_andHonoursTrue() {
        final String original = Config.getStringProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, null);
        try {
            Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, null);
            assertFalse(AnonymousAccess.scriptingEndpointsAllowAnonymous());

            Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, "true");
            assertTrue(AnonymousAccess.scriptingEndpointsAllowAnonymous());
        } finally {
            Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, original);
        }
    }

}
