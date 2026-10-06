package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.io.File;
import java.util.Optional;

/**
 * Default implementation
 * @author jsanca
 */
public class DotAssetAPIImpl implements DotAssetAPI {

    private final BaseTypeMimeTypeMatcher mimeTypeMatcher = new BaseTypeMimeTypeMatcher();

    @Override
    public Optional<ContentType> tryMatch(final File file,final  Host currentHost,final  User user) throws DotDataException, DotSecurityException {

        return this.tryMatch(APILocator.getFileAssetAPI().getMimeType(file), currentHost, user);
    }

    @Override
    public Optional<ContentType> tryMatch(final String mimeType, final Host currentHost, final User user) throws DotSecurityException, DotDataException {

        return this.mimeTypeMatcher.match(mimeType, currentHost, user,
                BaseContentType.DOTASSET, DotAssetContentType.ASSET_FIELD_VAR);
    }
}
