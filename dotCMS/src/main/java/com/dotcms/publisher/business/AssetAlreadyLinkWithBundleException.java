package com.dotcms.publisher.business;

import com.dotmarketing.util.web.WebDotcmsException;
import com.liferay.portal.model.User;

/**
 * Created by freddyrodriguez on 30/3/16.
 */
public class AssetAlreadyLinkWithBundleException extends WebDotcmsException {

    public AssetAlreadyLinkWithBundleException(User user, String identifier) {
        super(user, "bundle.add.error.asset.exists", identifier);
    }
}
