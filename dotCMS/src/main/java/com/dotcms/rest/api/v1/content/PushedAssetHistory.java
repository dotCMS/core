package com.dotcms.rest.api.v1.content;

import java.util.Date;

/**
 * Simple view model representing a single entry in the pushed asset history for an asset.
 * <p>
 * Instances are created from a database row map returned by the persistence layer and expose a
 * subset of attributes useful to the REST API: the environment name, the push date, the bundle id
 * associated with the push, and the full name of the user who triggered the push (if available).
 * </p>
 *
 * @author Freddy Rodriguez
 * @since 25.09.2025
 */
public class PushedAssetHistory {

    private final String pushedBy;
    private final String environment;
    private final Date pushDate;
    private final String bundleId;

    public PushedAssetHistory(final String pushedBy, final String environment,
                              final Date pushDate, final String bundleId) {
        this.pushedBy = pushedBy;
        this.environment = environment;
        this.pushDate = pushDate;
        this.bundleId = bundleId;
    }

    /**
     * Returns the name of the user who triggered the push.
     *
     * @return the username.
     */
    public String getPushedBy() {
        return pushedBy;
    }

    /**
     * Returns the ID of the Push Publishing environment that the Contentlet was pushed to.
     *
     * @return the ID of the environment.
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Returns the date when the Contentlet was pushed.
     *
     * @return The push date.
     */
    public Date getPushDate() {
        return pushDate;
    }

    /**
     * Returns the ID of the bundle that contained the Contentlet that was pushed.
     *
     * @return the ID of the bundle.
     */
    public String getBundleId() {
        return bundleId;
    }

}
