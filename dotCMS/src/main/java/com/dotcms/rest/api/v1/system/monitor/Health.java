package com.dotcms.rest.api.v1.system.monitor;

/**
 * Represents the health status of a dotCMS subsystem or external integration reported by the
 * system monitor.
 *
 * <p>Used by {@link MonitorHelper} for subsystems that are optionally configured per Site (e.g.
 * Content Analytics), where the distinction between "not set up" and "misconfigured" is meaningful
 * to operators.
 *
 * @author Jose Castro
 * @since Apr 30th, 2026
 */
public enum Health {

    /** The subsystem is fully configured and all connectivity checks passed. */
    OK,

    /**
     * The subsystem is not configured for the current Site. No action is required unless the
     * feature is expected to be active on this Site.
     */
    NOT_CONFIGURED,

    /**
     * The subsystem is partially or incorrectly configured, or a connectivity check failed.
     * Inspect the relevant configuration properties and verify service availability.
     */
    CONFIGURATION_ERROR

}
