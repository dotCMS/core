package com.dotcms.api;

import com.dotcms.rest.ContentHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;


/**
 * Class to provide different APIs to a class. Defaults to using the {@link APILocator}
 * <p>
 * It's very useful for passing mocked APIs. Have the constructor of your class take this as argument
 * and access to the different APIs from it.
 * <p>
 * Example Code:
 *  {@code final APIProvider toolBox = new Builder().withIdentifierAPI(identifierAPI)
 *                 .withFileAssetAPI(fileAssetAPI).withUserAPI(APILocator.getUserAPI())
 *                 .withContentHelper(contentHelper).build();}

 *
 */
public class APIProvider {

    public final IdentifierAPI identifierAPI;
    public final HostAPI hostAPI;
    public final LanguageAPI languageAPI;
    public final FileAssetAPI fileAssetAPI;
    public final VersionableAPI versionableAPI;
    public final UserAPI userAPI;
    public final ContentletAPI contentletAPI;
    public final HTMLPageAssetAPI htmlPageAssetAPI;
    public final CategoryAPI categoryAPI;
    public final ContentHelper contentHelper;
    public final PermissionAPI permissionAPI;

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

    private APIProvider(final IdentifierAPI identifierAPI,
            final HostAPI hostAPI,
            final LanguageAPI languageAPI,
            final FileAssetAPI fileAssetAPI, final VersionableAPI versionableAPI,
            final UserAPI userAPI,  final ContentletAPI contentletAPI,
            final HTMLPageAssetAPI htmlPageAssetAPI,
            final CategoryAPI categoryAPI,
            final ContentHelper contentHelper,
            final PermissionAPI permissionAPI) {
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
        this.permissionAPI = permissionAPI;
    }

    public static class Builder {

        private IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
        private HostAPI hostAPI = APILocator.getHostAPI();
        private LanguageAPI languageAPI = APILocator.getLanguageAPI();
        private FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
        private VersionableAPI versionableAPI = APILocator.getVersionableAPI();
        private UserAPI userAPI = APILocator.getUserAPI();
        private ContentletAPI contentletAPI = APILocator.getContentletAPI();
        private HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
        private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        private ContentHelper contentHelper = ContentHelper.getInstance();
        private PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        public Builder withIdentifierAPI(IdentifierAPI identifierAPI) {
            this.identifierAPI = identifierAPI;
            return this;
        }

        public Builder withHostAPI(HostAPI hostAPI) {
            this.hostAPI = hostAPI;
            return this;
        }

        public Builder withLanguageAPI(LanguageAPI languageAPI) {
            this.languageAPI = languageAPI;
            return this;
        }

        public Builder withFileAssetAPI(FileAssetAPI fileAssetAPI) {
            this.fileAssetAPI = fileAssetAPI;
            return this;
        }

        public Builder withVersionableAPI(VersionableAPI versionableAPI) {
            this.versionableAPI = versionableAPI;
            return this;
        }

        public Builder withUserAPI(UserAPI userAPI) {
            this.userAPI = userAPI;
            return this;
        }

        public Builder withContentletAPI(ContentletAPI contentletAPI) {
            this.contentletAPI = contentletAPI;
            return this;
        }

        public Builder withHtmlPageAssetAPI(HTMLPageAssetAPI htmlPageAssetAPI) {
            this.htmlPageAssetAPI = htmlPageAssetAPI;
            return this;
        }

        public Builder withCategoryAPI(CategoryAPI categoryAPI) {
            this.categoryAPI = categoryAPI;
            return this;
        }

        public Builder withContentHelper(ContentHelper contentHelper) {
            this.contentHelper = contentHelper;
            return this;
        }

        public Builder withPermissionAPI(PermissionAPI permissionAPI) {
            this.permissionAPI = permissionAPI;
            return this;
        }

        public APIProvider build() {
            return new APIProvider(identifierAPI, hostAPI, languageAPI, fileAssetAPI,
                    versionableAPI, userAPI, contentletAPI, htmlPageAssetAPI, categoryAPI,
                    contentHelper, permissionAPI);
        }
    }
}
