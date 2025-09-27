package com.dotcms.rest.api.v1.content;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Simple view model representing a single entry in the pushed asset history for an asset.
 * <p>
 * Instances are created from a database row map returned by the persistence layer and expose
 * a subset of attributes useful to the REST API: the environment name, the push date, the bundle id
 * associated with the push, and the full name of the user who triggered the push (if available).
 * </p>
 *
 * @since 25.09.2025
 */
public class PushedAssetHistory {

     private final String pushedBy;
     private final String environment;
     private final Date pushDate;
     private final String bundleId;

    public PushedAssetHistory(final Map<String, Object> row) {

        this.environment = row.get("environment_name").toString();
        this.pushDate = (Date) row.get("push_date");
        this.bundleId = row.get("bundle_id").toString();
        this.pushedBy =  getUserFullName(row).orElse(StringPool.BLANK);
    }

    private Optional<String> getUserFullName(Map<String, Object> row)  {
        try {

            if (!UtilMethods.isSet(bundleId)) {
                return Optional.empty();
            }

            final User owner = APILocator.getUserAPI().loadUserById(row.get("owner").toString());
            return Optional.of(owner.getFullName());
        } catch (DotDataException | DotSecurityException e) {
            return Optional.empty();
        }
    }

    public String getPushedBy() {
        return pushedBy;
    }

    public String getEnvironment() {
        return environment;
    }

    public Date getPushDate() {
        return pushDate;
    }

    public String getBundleId() {
        return bundleId;
    }
}
