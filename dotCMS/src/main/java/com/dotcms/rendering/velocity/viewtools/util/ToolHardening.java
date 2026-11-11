package com.dotcms.rendering.velocity.viewtools.util;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;

/**
 * Tells whether the Velocity tool hardening is switched on. On this LTS it is off unless an
 * operator sets {@link FeatureFlagName#FEATURE_FLAG_VELOCITY_TOOL_HARDENING}, so the tools and the
 * widget rendering behave as they did before the hardening was added.
 */
public final class ToolHardening {

    private ToolHardening() {
    }

    /**
     * Reads the master flag every time, so a change in the System Table applies without a restart.
     *
     * @return {@code true} when the hardening is on
     */
    public static boolean isEnabled() {
        return Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_VELOCITY_TOOL_HARDENING, false);
    }
}
