package com.dotcms.util;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.contenttype.transform.contenttype.ImplClassContentTypeTransformer;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.db.listeners.CommitAPI;
import com.dotmarketing.db.listeners.CommitAPI.CommitListenerStatus;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jonathan Gamba 8/21/17
 */
public class FiltersUtil {

    private static final String VANITY_URL_CONTENT_TYPE_NAME = "Vanity URL";
    private static final String VANITY_URL_CONTENT_TYPE_VARNAME = "Vanityurl";

    private User user = APILocator.systemUser();
    private ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

    private static FiltersUtil filtersUtil;

    public static FiltersUtil getInstance() {
        if (filtersUtil == null) {

            synchronized (FiltersUtil.class) {
                filtersUtil = new FiltersUtil();
            }

        }
        return filtersUtil;
    }

    private FiltersUtil() {
    }

    /**
     * Creates a new Vanity URL contentlet
     */
    public Contentlet createVanityUrl(String title, String site, String uri,
            String forwardTo, int action, int order, long languageId)
            throws DotDataException, DotSecurityException {

        //VanityURL content type
        ContentType contentType = getVanityURLContentType();

        //Create the new Contentlet
        Contentlet contentlet = new Contentlet();
        contentlet.setStructureInode(contentType.inode());
        contentlet.setHost(site);
        contentlet.setLanguageId(languageId);

        contentlet.setStringProperty(VanityUrlContentType.TITLE_FIELD_VAR, title);
        contentlet.setStringProperty(VanityUrlContentType.SITE_FIELD_VAR, site);
        contentlet.setStringProperty(VanityUrlContentType.URI_FIELD_VAR, uri);
        contentlet.setStringProperty(VanityUrlContentType.FORWARD_TO_FIELD_VAR, forwardTo);
        contentlet.setLongProperty(VanityUrlContentType.ACTION_FIELD_VAR, action);
        contentlet.setLongProperty(VanityUrlContentType.ORDER_FIELD_VAR, order);

        //Get The permissions of the Content Type
        List<Permission> contentTypePermissions = permissionAPI.getPermissions(contentType);

        //Validate if the contenlet is OK
        contentletAPI.validateContentlet(contentlet, new ArrayList());

        //Save the contentlet
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentlet = contentletAPI.checkin(contentlet, contentTypePermissions, user, true);
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);

        return contentlet;
    }

    /**
     * Publish the Vanity URL contentlet
     */
    public void publishVanityUrl(Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        //Publish Vanity Url
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        CommitAPI.getInstance().forceStatus(CommitListenerStatus.ALLOW_SYNC);
        contentletAPI.publish(contentlet, user, false);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            //Do nothing...
        }
    }

    /**
     * Unpublish the Vanity URL contentlet
     */
    public void unpublishVanityURL(Contentlet vanityURL)
            throws DotSecurityException, DotDataException {

        vanityURL.setIndexPolicy(IndexPolicy.FORCE);
        vanityURL.setIndexPolicyDependencies(IndexPolicy.FORCE);
        vanityURL.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        CommitAPI.getInstance().forceStatus(CommitListenerStatus.ALLOW_SYNC);
        contentletAPI.unpublish(vanityURL, user, false);
    }

    /**
     * Get the Vanity Url Content Type. If the content Type doesn't exist then it will be created
     *
     * @return a Vanity Url Content Type
     */
    private ContentType getVanityURLContentType() throws DotDataException, DotSecurityException {

        String query = " velocity_var_name = '" + VANITY_URL_CONTENT_TYPE_VARNAME + "'";
        List<ContentType> contentTypes = contentTypeAPI.search(query);

        ContentType contentType;
        if (contentTypes.size() == 0) {
            contentType = createVanityUrl();
        } else {
            contentType = contentTypes.get(0);
        }

        return contentType;
    }

    /**
     * Create a VanityUrl content type
     *
     * @return A new vanity Url content Type
     */
    private ContentType createVanityUrl() throws DotDataException, DotSecurityException {
        BaseContentType base = BaseContentType
                .getBaseContentType(BaseContentType.VANITY_URL.getType());

        final ContentType type = new ContentType() {
            @Override
            public String name() {
                return VANITY_URL_CONTENT_TYPE_NAME;
            }

            @Override
            public String id() {
                return null;
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public String variable() {
                return VANITY_URL_CONTENT_TYPE_VARNAME;
            }

            @Override
            public BaseContentType baseType() {
                return base;
            }
        };
        CommitAPI.getInstance().forceStatus(CommitListenerStatus.ALLOW_SYNC);
        return contentTypeAPI.save(new ImplClassContentTypeTransformer(type).from());
    }

}
