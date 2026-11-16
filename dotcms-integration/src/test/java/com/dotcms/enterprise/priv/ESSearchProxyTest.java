package com.dotcms.enterprise.priv;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ESSearchProxyTest extends IntegrationTestBase {

    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        long defLangId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        user = APILocator.getUserAPI().getSystemUser();

        final Host defaultHost = APILocator.getHostAPI()
                .findDefaultHost(APILocator.systemUser(), false);
        final User systemUser = APILocator.systemUser();
        final Folder aboutUs =
         APILocator.getFolderAPI()
                .createFolders("/about-us" + System.currentTimeMillis() + "/",
                        defaultHost, systemUser, false);

        for(int i=0; i<=10; i++ ){
            final Contentlet page = TestDataUtils.getPageContent(true, defLangId, aboutUs);
            ContentletDataGen.publish(page);
        }

    }



    @Test
    public void test_esSearch_WithLicense_Success() throws Exception {
        final String query = "{\"query\":{\"query_string\":{\"query\":\"+basetype:5 +parentpath:*\\\\\\/abou*\"}}}";
        final List<ESSearchResults> resultsList = getEsSearchResults(query);
        Assert.assertFalse(resultsList.isEmpty());
    }

    /**
     * MethodToTest {@link ESSearchAPIImpl#esSearch(String, boolean, User, boolean)}
     * Given Scenario: ContentType with a Date Field can be query via ES
     * ExpectedResult: Should return list of contentlets
     * @throws Exception
     */
    @Test
    public void test_esSearch_WithDates_Success() throws Exception{
        final Host newHost = new SiteDataGen().nextPersisted();
        final ContentType blogContentType = TestDataUtils.getBlogLikeContentType(newHost);
        for(int i=0;i<10;i++) {
            TestDataUtils.getBlogContent(true, 1,blogContentType.id(),newHost);
        }
        final String query = "{\"query\":{\"query_string\":{\"query\":\"+contenttype:" + blogContentType.variable()
                + " +conhost:"+ newHost.getIdentifier()
                +" +" + blogContentType.variable() + ".sysPublishDate:[2021-01-01t00:00:00 TO 2121-12-01t00:00:00]\""
                +"}}}";
        final List<ESSearchResults> resultsList = getEsSearchResults(query, false);
        Assert.assertFalse(resultsList.isEmpty());
    }

    /**
     * MethodToTest {@link ESSearchAPIImpl#applyPermissionsFilter(JSONObject, User, boolean)}
     * Given Scenario: A non-admin user runs a raw query, once with a plain body and once with a
     *                 body that carries an extra top-level key.
     * ExpectedResult: The permission filter is injected in both cases — what the caller puts in
     *                 the body takes no part in the decision. The assertion is made on the body
     *                 that would be sent to Elasticsearch, because a body with an unknown
     *                 top-level key never runs: SearchSourceBuilder.parseXContent rejects it, so
     *                 observing hits cannot tell a filtered query from a rejected one.
     */
    @Test
    public void test_applyPermissionsFilter_nonAdminUser_alwaysInjectsPermissions() throws Exception {

        final User limitedUser = new UserDataGen().nextPersisted();

        for (final String body : new String[] {
                "{\"query\":{\"match_all\":{}}}",
                "{\"permissions:\":\"x\",\"query\":{\"match_all\":{}}}"}) {

            final JSONObject filtered = ESSearchAPIImpl.applyPermissionsFilter(
                    new JSONObject(body), limitedUser, false);
            final String query = filtered.getJSONObject("query").toString();

            Assert.assertTrue("The query sent for " + body + " must carry the permission filter: "
                    + query, query.contains("+owner:" + limitedUser.getUserId()));
        }
    }

    /**
     * MethodToTest {@link ESSearchAPIImpl#applyPermissionsFilter(JSONObject, User, boolean)}
     * Given Scenario: An administrator runs the same raw query.
     * ExpectedResult: The query reaches Elasticsearch as the caller wrote it — administrators are
     *                 not filtered.
     */
    @Test
    public void test_applyPermissionsFilter_adminUser_queryIsUntouched() throws Exception {

        final JSONObject filtered = ESSearchAPIImpl.applyPermissionsFilter(
                new JSONObject("{\"query\":{\"match_all\":{}}}"), TestUserUtils.getAdminUser(), false);

        Assert.assertEquals("{\"match_all\":{}}", filtered.getJSONObject("query").toString());
    }

    private List<ESSearchResults> getEsSearchResults(final String query)
            throws DotSecurityException, DotDataException {
        return (List<ESSearchResults>) getEsSearchResults(query,true);
    }

    private List<ESSearchResults> getEsSearchResults(final String query, final boolean live)
            throws DotSecurityException, DotDataException {
        final ESSearchProxy esSearchProxy = new ESSearchProxy();
        return (List<ESSearchResults>) esSearchProxy.esSearch(query,live, user,false);
    }
}
