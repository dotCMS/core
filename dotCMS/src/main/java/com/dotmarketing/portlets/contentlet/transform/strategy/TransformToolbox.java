package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.rest.ContentHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.google.common.annotations.VisibleForTesting;


/**
 * Just a class for common share code to reside
 * And pass all the required services across layers within one single unit.
 */
public class TransformToolbox {

    final IdentifierAPI identifierAPI;
    final HostAPI hostAPI;
    final LanguageAPI languageAPI;
    final FileAssetAPI fileAssetAPI;
    final VersionableAPI versionableAPI;
    final UserAPI userAPI;
    final ContentletAPI contentletAPI;
    final HTMLPageAssetAPI htmlPageAssetAPI;
    final CategoryAPI categoryAPI;
    final ContentHelper contentHelper;

    /**
     * main constructor
     * @param identifierAPI
     * @param hostAPI
     * @param languageAPI
     * @param fileAssetAPI
     * @param versionableAPI
     * @param userAPI
     * @param contentletAPI
     * @param htmlPageAssetAPI
     * @param categoryAPI
     * @param contentHelper
     */
    @VisibleForTesting
    public TransformToolbox(final IdentifierAPI identifierAPI,
            final HostAPI hostAPI,
            final LanguageAPI languageAPI,
            final FileAssetAPI fileAssetAPI, final VersionableAPI versionableAPI,
            final UserAPI userAPI,  final ContentletAPI contentletAPI,
            final HTMLPageAssetAPI htmlPageAssetAPI,
            final CategoryAPI categoryAPI,
            final ContentHelper contentHelper) {
        this.identifierAPI = identifierAPI;
        this.hostAPI = hostAPI;
        this.languageAPI = languageAPI;
        this.fileAssetAPI = fileAssetAPI;
        this.versionableAPI = versionableAPI;
        this.userAPI = userAPI;
        this.contentletAPI = contentletAPI;
        this.htmlPageAssetAPI = htmlPageAssetAPI;
        this.categoryAPI = categoryAPI;
        this.contentHelper = contentHelper;
    }

    /**
     * Default constructor
     */
    TransformToolbox() {
        this(APILocator.getIdentifierAPI(), APILocator.getHostAPI(), APILocator.getLanguageAPI(),
            APILocator.getFileAssetAPI(), APILocator.getVersionableAPI(), APILocator.getUserAPI(),
            APILocator.getContentletAPI(), APILocator.getHTMLPageAssetAPI(), APILocator.getCategoryAPI(),
            ContentHelper.getInstance());
    }

    /**
     * Copy content util method
     * @param contentlet
     * @return
     */
    public static Contentlet copyContentlet(final Contentlet contentlet) {
        final Contentlet newContentlet = new Contentlet();
        if (null != contentlet && null != contentlet.getMap()) {
            newContentlet.getMap().putAll(contentlet.getMap());
        }
        return newContentlet;
    }

}
