package com.dotcms.vanityurl.business;


import static org.hamcrest.MatcherAssert.assertThat;

import com.dotcms.cache.VanityUrlCache;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.contenttype.transform.contenttype.ImplClassContentTypeTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.VanityUrlUtil;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class test the {@link VanityUrlAPI} methods
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 22, 2017
 */
public class VanityUrlAPITest {

    private static ContentletAPI contentletAPI;
    private static VanityUrlAPI vanityUrlAPI;
    private static Host defaultHost;
    private static HostAPI hostAPI;
    private static User user;
    private static LanguageAPI languageAPI;
    private static long defaultLanguageId;
    private static ContentTypeAPI contentTypeAPI;
    private static PermissionAPI permissionAPI;
    private static ContentType contentType;
    private static VanityUrlCache vanityUrlCache;

    private static final String VANITY_URL_CONTENT_TYPE_NAME = "Vanity URL Asset";
    private static final String VANITY_URL_CONTENT_TYPE_VARNAME = "Vanityurlasset";

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        /* Default user */
        user = APILocator.systemUser();

        /* APIs initialization */
        hostAPI = APILocator.getHostAPI();
        contentletAPI = APILocator.getContentletAPI();
        vanityUrlAPI = APILocator.getVanityUrlAPI();
        languageAPI = APILocator.getLanguageAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        permissionAPI = APILocator.getPermissionAPI();

        /* Load Cache */
        vanityUrlCache = CacheLocator.getVanityURLCache();

        /* Default variables */
        defaultHost = hostAPI.findDefaultHost(user, false);
        defaultLanguageId = languageAPI.getDefaultLanguage().getId();
        getContentType();
    }

    /**
     * Testing {@link VanityUrlAPI#getWorkingVanityUrl(String, com.dotmarketing.beans.Host,
     * long, com.liferay.portal.model.User)}
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getWorkingVanityUrl() {

        Contentlet contentlet1 = null;

        try {
            long i = System.currentTimeMillis();
            String title = "VanityURL" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test1_" + i;
            String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action = 200;
            int order = 1;

            contentlet1 = createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            VanityUrl vanity1 = vanityUrlAPI
                    .getWorkingVanityUrl(uri, defaultHost, defaultLanguageId, user);
            assertThat("Vanity URL should exist",
                    vanity1 != null && !vanity1.getInode()
                            .equals(VanityUrlAPI.CACHE_404_VANITY_URL));
            assertThat("Vanity URL title field is incorrect",
                    vanity1.getTitle().equals(title));
            assertThat("Vanity URL site field is incorrect",
                    vanity1.getSite().equals(site));
            assertThat("Vanity URL uri field is incorrect", vanity1.getURI()
                    .equals(uri));
            assertThat("Vanity URL forwardTo field is incorrect", vanity1.getForwardTo()
                    .equals(forwardTo));
            assertThat("Vanity URL action code field is incorrect", vanity1.getAction() == action);

            vanity1 = vanityUrlAPI.getLiveVanityUrl(uri, defaultHost, defaultLanguageId, user);
            assertThat("Vanity URL should not be live", vanity1 == null || vanity1.getInode()
                    .equals(VanityUrlAPI.CACHE_404_VANITY_URL));

        } catch (DotDataException | DotSecurityException e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.delete(contentlet1, user, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }
        }
    }

    /**
     * Testing {@link VanityUrlAPI#getLiveVanityUrl(String, com.dotmarketing.beans.Host,
     * long, com.liferay.portal.model.User)}
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getLiveVanityUrl() {

        Contentlet contentlet1 = null;
        Contentlet contentlet2 = null;
        try {
            long i = System.currentTimeMillis();
            String title = "VanityURL" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test2_1_" + i;
            String forwardTo = "/products/";
            int action = 200;
            int order = 1;

            contentlet1 = createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            publishVanityUrl(contentlet1);

            //Not live
            String title2 = "VanityURL_2_" + i;
            String site2 = defaultHost.getIdentifier();
            String uri2 = "/test2_2_" + i;
            String forwardTo2 = "/products/";
            int action2 = 301;
            int order2 = 1;

            contentlet2 = createVanityUrl(title2, site2, uri2,
                    forwardTo2, action2, order2, defaultLanguageId);

            VanityUrl vanity1 = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(contentlet1));
            assertThat("Vanity URL should not be cached yet",
                    vanity1 == null);

            vanity1 = vanityUrlAPI
                    .getLiveVanityUrl(uri, defaultHost, defaultLanguageId, user);

            assertThat("Vanity URL should be live",
                    vanity1 != null && !vanity1.getInode()
                            .equals(VanityUrlAPI.CACHE_404_VANITY_URL));
            assertThat("Vanity URL title field is incorrect",
                    vanity1.getTitle().equals(title));
            assertThat("Vanity URL site field is incorrect",
                    vanity1.getSite().equals(site));
            assertThat("Vanity URL uri field is incorrect", vanity1.getURI()
                    .equals(uri));
            assertThat("Vanity URL forwardTo field is incorrect", vanity1.getForwardTo()
                    .equals(forwardTo));
            assertThat("Vanity URL action code field is incorrect", vanity1.getAction() == action);

            vanity1 = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(contentlet1));
            assertThat("Vanity URL should be cached",
                    vanity1 != null && !vanity1.getInode()
                    .equals(VanityUrlAPI.CACHE_404_VANITY_URL));

            VanityUrl vanity2 = vanityUrlCache.get(VanityUrlUtil.sanitizeKey(contentlet2));
            assertThat("Vanity URL should not be cached yet",
                    vanity2 == null);

            vanity2 = vanityUrlAPI
                    .getLiveVanityUrl(uri2, defaultHost, defaultLanguageId, user);

            assertThat("Vanity URL should not be live",
                    vanity2 == null || vanity2.getInode()
                            .equals(VanityUrlAPI.CACHE_404_VANITY_URL));

        } catch (DotDataException | DotSecurityException e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.delete(contentlet1, user, false);
                }

                if (contentlet2 != null) {
                    contentletAPI.delete(contentlet2, user, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }
        }
    }

    /**
     * Testing {@link VanityUrlAPI#getVanityUrlFromContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getVanityUrlFromContentlet() {
        Contentlet contentlet1 = null;
        try {
            long i = System.currentTimeMillis();
            String title = "VanityURL" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test3_" + i;
            String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action = 301;
            int order = 1;

            contentlet1 = createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            publishVanityUrl(contentlet1);

            VanityUrl vanity1 = vanityUrlAPI
                    .getVanityUrlFromContentlet(contentlet1);

            assertThat("Vanity URL should exist",
                    vanity1 != null && !vanity1.getInode()
                            .equals(VanityUrlAPI.CACHE_404_VANITY_URL));
            assertThat("Vanity URL title field is incorrect",
                    vanity1.getTitle().equals(title));
            assertThat("Vanity URL site field is incorrect",
                    vanity1.getSite().equals(site));
            assertThat("Vanity URL uri field is incorrect", vanity1.getURI()
                    .equals(uri));
            assertThat("Vanity URL forwardTo field is incorrect", vanity1.getForwardTo()
                    .equals(forwardTo));
            assertThat("Vanity URL action code field is incorrect", vanity1.getAction() == action);

        } catch (DotDataException | DotSecurityException e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.delete(contentlet1, user, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }
        }
    }

    /**
     * Testing {@link VanityUrlAPI#getActiveVanityUrls(com.liferay.portal.model.User)}
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getActiveVanityUrls() {
        Contentlet contentlet1 = null;
        Contentlet contentlet2 = null;
        Contentlet contentlet3 = null;
        Contentlet contentlet4 = null;
        try {

            List<VanityUrl> currentActiveVanityUrls = vanityUrlAPI.getActiveVanityUrls(user);
            int initialActiveVanityUrlsCount = currentActiveVanityUrls.size();

            long i = System.currentTimeMillis();

            //Live
            String title = "VanityURL_1_" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test4_1_" + i;
            String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action = 200;
            int order = 1;

            contentlet1 = createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            publishVanityUrl(contentlet1);

            //Not live
            String title2 = "VanityURL_2_" + i;
            String site2 = defaultHost.getIdentifier();
            String uri2 = "/test4_2_" + i;
            String forwardTo2 = "/products/";
            int action2 = 301;
            int order2 = 1;

            contentlet2 = createVanityUrl(title2, site2, uri2,
                    forwardTo2, action2, order2, defaultLanguageId);

            //Live
            String title3 = "VanityURL_3_" + i;
            String site3 = defaultHost.getIdentifier();
            String uri3 = "/test4_3_" + i;
            String forwardTo3 = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action3 = 500;
            int order3 = 1;

            contentlet3 = createVanityUrl(title3, site3, uri3,
                    forwardTo3, action3, order3, defaultLanguageId);

            publishVanityUrl(contentlet3);

            //not Live
            String title4 = "VanityURL_4_" + i;
            String site4 = defaultHost.getIdentifier();
            String uri4 = "/test4_4_" + i;
            String forwardTo4 = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action4 = 200;
            int order4 = 1;

            contentlet4 = createVanityUrl(title4, site4, uri4,
                    forwardTo4, action4, order4, defaultLanguageId);

            List<VanityUrl> vanityURLs = vanityUrlAPI.getActiveVanityUrls(user);
            assertThat("Incorrect number of active Vanity URLs",
                    vanityURLs.size() > initialActiveVanityUrlsCount);
            assertThat("Vanity URL is not included in the list of active Vanity URLs",
                    vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet1)));
            assertThat("Vanity URL List should not include only working vanityUrls",
                    !vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet2)));
            assertThat("Vanity URL is not included in the list of active Vanity URLs",
                    vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet3)));
            assertThat("Vanity URL List should not include only working vanityUrls",
                    !vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet4)));


        } catch (DotDataException | DotSecurityException e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.delete(contentlet1, user, false);
                }
                if (contentlet2 != null) {
                    contentletAPI.delete(contentlet2, user, false);
                }
                if (contentlet3 != null) {
                    contentletAPI.delete(contentlet3, user, false);
                }
                if (contentlet4 != null) {
                    contentletAPI.delete(contentlet4, user, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }
        }
    }

    /**
     * Testing {@link VanityUrlAPI#getAllVanityUrls(com.liferay.portal.model.User))}
     *
     * @see VanityUrlAPI
     */
    @Test
    public void getAllVanityUrls() {
        Contentlet contentlet1 = null;
        Contentlet contentlet2 = null;
        Contentlet contentlet3 = null;
        Contentlet contentlet4 = null;
        try {

            List<VanityUrl> currentActiveVanityUrls = vanityUrlAPI.getAllVanityUrls(user);
            int initialVanityUrlsCount = currentActiveVanityUrls.size();

            long i = System.currentTimeMillis();

            //Live
            String title = "VanityURL_1_" + i;
            String site = defaultHost.getIdentifier();
            String uri = "/test5_1_" + i;
            String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action = 200;
            int order = 1;

            contentlet1 = createVanityUrl(title, site, uri,
                    forwardTo, action, order, defaultLanguageId);

            publishVanityUrl(contentlet1);

            //Not live
            String title2 = "VanityURL_2_" + i;
            String site2 = defaultHost.getIdentifier();
            String uri2 = "/test5_2_" + i;
            String forwardTo2 = "/products/";
            int action2 = 301;
            int order2 = 1;

            contentlet2 = createVanityUrl(title2, site2, uri2,
                    forwardTo2, action2, order2, defaultLanguageId);

            //Live
            String title3 = "VanityURL_3_" + i;
            String site3 = defaultHost.getIdentifier();
            String uri3 = "/test5_3_" + i;
            String forwardTo3 = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action3 = 500;
            int order3 = 1;

            contentlet3 = createVanityUrl(title3, site3, uri3,
                    forwardTo3, action3, order3, defaultLanguageId);

            publishVanityUrl(contentlet3);

            //not Live
            String title4 = "VanityURL_4_" + i;
            String site4 = defaultHost.getIdentifier();
            String uri4 = "/test5_4_" + i;
            String forwardTo4 = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
            int action4 = 200;
            int order4 = 1;

            contentlet4 = createVanityUrl(title4, site4, uri4,
                    forwardTo4, action4, order4, defaultLanguageId);

            List<VanityUrl> vanityURLs = vanityUrlAPI.getAllVanityUrls(user);
            assertThat("Incorrect number of active Vanity URLs",
                    vanityURLs.size() > initialVanityUrlsCount);
            assertThat("Vanity URL is not included in the list of All Vanity URLs",
                    vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet1)));
            assertThat("Vanity URL is not included in the list of All Vanity URLs",
                    vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet2)));
            assertThat("Vanity URL is not included in the list of All Vanity URLs",
                    vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet3)));
            assertThat("Vanity URL is not included in the list of All Vanity URLs",
                    vanityURLs.contains(vanityUrlAPI.getVanityUrlFromContentlet(contentlet4)));


        } catch (DotDataException | DotSecurityException e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            try {
                if (contentlet1 != null) {
                    contentletAPI.delete(contentlet1, user, false);
                }
                if (contentlet2 != null) {
                    contentletAPI.delete(contentlet2, user, false);
                }
                if (contentlet3 != null) {
                    contentletAPI.delete(contentlet3, user, false);
                }
                if (contentlet4 != null) {
                    contentletAPI.delete(contentlet4, user, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }
        }
    }

    /**
     * Get the Vanity Url Content Type. If the content Type doesn't
     * exist then it will be created
     *
     * @return a Vanity Url Content Type
     */
    private static ContentType getContentType() throws DotDataException, DotSecurityException {
        String query = " velocity_var_name = '" + VANITY_URL_CONTENT_TYPE_VARNAME + "'";
        List<ContentType> contentTypes = contentTypeAPI.search(query);

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
    private static ContentType createVanityUrl() throws DotDataException, DotSecurityException {
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

        return contentTypeAPI.save(new ImplClassContentTypeTransformer(type).from());
    }

    /**
     * Creates a new Vanity URL contentlet
     */
    private Contentlet createVanityUrl(String title, String site, String uri,
            String forwardTo, int action, int order, long languageId)
            throws DotDataException, DotSecurityException {
        //Create the new Contentlet
        Contentlet contentlet = new Contentlet();
        contentlet.setStructureInode(contentType.inode());
        contentlet.setHost(defaultHost.getIdentifier());
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
        contentlet = contentletAPI.checkin(contentlet, contentTypePermissions, user, true);
        contentletAPI.isInodeIndexed(contentlet.getInode());

        return contentlet;
    }

    /**
     * Publish the Vanity URL contentlet
     */
    private void publishVanityUrl(Contentlet contentlet)
            throws DotDataException, DotSecurityException {
        //Publish Vanity Url
        contentletAPI.publish(contentlet, user, false);
        contentletAPI.isInodeIndexed(contentlet.getInode(),1);
    }
}
