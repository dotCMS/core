package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.List;

/**
 * {@inheritDoc}
 */
public class ContentletDestroyDelegateImpl implements ContentletDestroyDelegate {

    @WrapInTransaction
    @Override
    public void destroy(final List<Contentlet> contentlets, final User user) {
        try {
            APILocator.getContentletAPI().destroy(contentlets, user, false);
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error destroying contents", e);
        }
    }

}
