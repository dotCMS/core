package com.dotcms.enterprise.priv;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldVariableDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ESSearchProxyTest extends IntegrationTestBase {

    private static User user;

    private static final String CATEGORY_CHECK_PROPERTY = "PERMISSION_SECONDARY_CATEGORY_CHECK";

    private static Role categoryReaderRole;
    private static User categoryReader;
    private static User categoryOutsider;
    private static Category topCategory;
    private static Category readersOnlyCategory;
    private static ContentType categorizedType;
    private static Contentlet readersOnlyContent;
    private static Contentlet uncategorizedContent;

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

        prepareCategoryReadData();
    }

    /**
     * Builds the data the category read tests share: a role allowed to read one category, a
     * back-end user holding that role and one who does not, and a content type whose Category
     * field enables the secondary category check. Two items of that type are created: one tagged
     * with the readers-only category and one with no category. Object permissions on the type let
     * every back-end user read both, so the category is the only thing that tells them apart.
     * Category permissions are set before the items are saved, because they are copied into the
     * index document when an item is indexed.
     */
    private static void prepareCategoryReadData() throws DotDataException, DotSecurityException {
        final String suffix = String.valueOf(System.currentTimeMillis());
        final User systemUser = APILocator.systemUser();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role backEndRole = APILocator.getRoleAPI().loadBackEndUserRole();
        final int readOnly = PermissionAPI.PERMISSION_READ;

        categoryReaderRole = new RoleDataGen()
                .name("categoryReaders" + suffix)
                .key("categoryReaders" + suffix)
                .nextPersisted();
        categoryReader = new UserDataGen()
                .roles(backEndRole, categoryReaderRole)
                .nextPersisted();
        categoryOutsider = new UserDataGen()
                .roles(backEndRole)
                .nextPersisted();

        topCategory = new CategoryDataGen()
                .setCategoryName("readTop" + suffix)
                .setKey("readTop" + suffix)
                .setCategoryVelocityVarName("readTop" + suffix)
                .nextPersisted();
        readersOnlyCategory = new CategoryDataGen()
                .setCategoryName("readersOnly" + suffix)
                .setKey("readersOnly" + suffix)
                .setCategoryVelocityVarName("readersOnly" + suffix)
                .parent(topCategory)
                .nextPersisted();
        permissionAPI.save(new Permission(readersOnlyCategory.getPermissionId(),
                categoryReaderRole.getId(), readOnly), readersOnlyCategory, systemUser, false);

        categorizedType = new ContentTypeDataGen()
                .name("categoryRead" + suffix)
                .velocityVarName("categoryRead" + suffix)
                .host(APILocator.systemHost())
                .fields(List.of(new FieldDataGen().type(ImmutableTextField.class)
                        .name("Title").velocityVarName("title").next()))
                .nextPersisted();
        final Field categoryField = new FieldDataGen()
                .type(ImmutableCategoryField.class)
                .sortOrder(10)
                .contentTypeId(categorizedType.id())
                .name("topics")
                .velocityVarName("topics")
                .values(topCategory.getInode())
                .nextPersisted();
        new FieldVariableDataGen().key("secondaryPermissionCheck").value("true")
                .field(categoryField).nextPersisted();
        permissionAPI.assignPermissions(List.of(
                new Permission(categorizedType.getPermissionId(), backEndRole.getId(), readOnly, true),
                new Permission(Contentlet.class.getCanonicalName(),
                        categorizedType.getPermissionId(), backEndRole.getId(), readOnly, true)),
                categorizedType, systemUser, false);

        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        readersOnlyContent = new ContentletDataGen(categorizedType)
                .host(APILocator.systemHost())
                .languageId(languageId)
                .setProperty("title", "readersOnly" + suffix)
                .setProperty(categoryField.variable(), readersOnlyCategory.getInode())
                .addCategory(readersOnlyCategory)
                .nextPersisted();
        uncategorizedContent = new ContentletDataGen(categorizedType)
                .host(APILocator.systemHost())
                .languageId(languageId)
                .setProperty("title", "uncategorized" + suffix)
                .nextPersisted();
    }

    /**
     * Removes the data created by {@link #prepareCategoryReadData()}, items first, then the type,
     * the categories, the users and finally the role they held.
     */
    @AfterClass
    public static void removeCategoryReadData() {
        if (readersOnlyContent != null) {
            ContentletDataGen.remove(readersOnlyContent);
        }
        if (uncategorizedContent != null) {
            ContentletDataGen.remove(uncategorizedContent);
        }
        if (categorizedType != null) {
            ContentTypeDataGen.remove(categorizedType);
        }
        if (readersOnlyCategory != null) {
            CategoryDataGen.delete(readersOnlyCategory, true);
        }
        if (topCategory != null) {
            CategoryDataGen.delete(topCategory, true);
        }
        deleteTestUser(categoryReader);
        deleteTestUser(categoryOutsider);
        if (categoryReaderRole != null) {
            RoleDataGen.remove(categoryReaderRole);
        }
    }

    /**
     * Deletes a user created by this class, handing anything it owns to the administrator. A
     * failure is logged rather than thrown, so the remaining cleanup still runs.
     *
     * @param testUser the user to delete, or {@code null} when it was never created
     */
    private static void deleteTestUser(final User testUser) {
        if (testUser == null) {
            return;
        }
        try {
            APILocator.getUserAPI().delete(testUser, TestUserUtils.getAdminUser(),
                    APILocator.systemUser(), false);
        } catch (final Exception e) {
            Logger.warn(ESSearchProxyTest.class,
                    "Unable to delete test user " + testUser.getUserId() + ": " + e.getMessage());
        }
    }

    /**
     * esSearch is an ES-only legacy API: ESSearchProxy delegates straight to the ES
     * RestHighLevelClient and returns an org.elasticsearch SearchResponse, so it has no OpenSearch
     * routing path. Under Phase 3 (OS-only) Elasticsearch no longer receives writes, so the path
     * refuses to run rather than answer from the index frozen at cutover — asserted separately by
     * {@link #test_esSearch_phase3_failsLoudlyInsteadOfReturningNothing()}. Re-routing esSearch
     * through the neutral search path is a pending product decision (#35784), so the assertions
     * below are gated to the phases where ES still serves reads (0/1/2).
     */
    private static void skipWhenOpenSearchOnly() {
        assumeFalse("esSearch is ES-only; asserted separately under Phase 3 (OS-only) — see #35784",
                IndexConfigHelper.MigrationPhase.current().isMigrationComplete());
    }

    @Test
    public void test_esSearch_WithLicense_Success() throws Exception {
        skipWhenOpenSearchOnly();
        final String query = "{\"query\":{\"query_string\":{\"query\":\"+basetype:5 +parentpath:*\\\\\\/abou*\"}}}";
        final List<ESSearchResults> resultsList = getEsSearchResults(query);
        Assert.assertFalse(resultsList.isEmpty());
    }

    /**
     * Phase 3 must fail this deprecated path loudly, with the specific exception type that survives
     * Velocity.
     *
     * <p>Past the final phase Elasticsearch receives no more writes. The switchover keeps its active
     * pointers, so without a phase check this path would answer from the index frozen at cutover —
     * results that look valid but miss everything written since — and where no pointer was ever
     * registered it used to carry a null into the ES client and die with a NullPointerException, which
     * Velocity's method-exception handler converts into a null return outside EDIT mode, so a
     * template's {@code #set} assigned nothing and the page rendered its own unresolved source
     * (issue #37635). It now fails on the phase itself, whatever the index store holds.
     * DotStateException is one of the few types that handler rethrows, so asserting the type here is
     * asserting the visible behaviour: an error, not stale results and not Velocity printed on the
     * page.</p>
     */
    @Test
    public void test_esSearch_phase3_failsLoudlyInsteadOfReturningNothing() {
        assumeTrue("asserts the Phase 3 (OS-only) behaviour of the deprecated ES-only path",
                IndexConfigHelper.MigrationPhase.current().isMigrationComplete());
        final String query = "{\"query\":{\"query_string\":{\"query\":\"+basetype:5\"}}}";

        Assert.assertThrows(DotStateException.class,
                () -> APILocator.getContentletAPI().esSearch(query, true, user, true));
    }

    /**
     * MethodToTest {@link ESSearchAPIImpl#esSearch(String, boolean, User, boolean)}
     * Given Scenario: ContentType with a Date Field can be query via ES
     * ExpectedResult: Should return list of contentlets
     * @throws Exception
     */
    @Test
    public void test_esSearch_WithDates_Success() throws Exception{
        skipWhenOpenSearchOnly();
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

    /**
     * MethodToTest {@link ESSearchAPIImpl#applyPermissionsFilter(JSONObject, User, boolean)}
     * Given Scenario: A non-admin back-end user runs a raw query.
     * ExpectedResult: Besides the read-permission clause, the query sent to Elasticsearch carries
     *                 the category read clause built from the user's roles, the same clause the
     *                 Lucene and OpenSearch searches append.
     */
    @Test
    public void test_applyPermissionsFilter_nonAdminUser_includesCategoryClause() throws Exception {

        final JSONObject filtered = ESSearchAPIImpl.applyPermissionsFilter(
                new JSONObject("{\"query\":{\"match_all\":{}}}"), categoryOutsider, false);
        final String query = filtered.getJSONObject("query").toString();

        final String backEndRoleId = APILocator.getRoleAPI().loadBackEndUserRole().getId();
        Assert.assertTrue("The query must carry the category clause for the user's roles: " + query,
                query.contains("categoryperms:" + backEndRoleId));
        Assert.assertTrue("The query must let through content with no restricted category: " + query,
                query.contains("categoryperms:none"));
    }

    /**
     * MethodToTest {@link ESSearchAPIImpl#applyPermissionsFilter(JSONObject, User, boolean)}
     * Given Scenario: A raw query runs with no user, respecting front-end roles (an anonymous
     *                 visitor).
     * ExpectedResult: The query carries the category read clause for the anonymous role.
     */
    @Test
    public void test_applyPermissionsFilter_anonymous_includesCategoryClause() throws Exception {

        final JSONObject filtered = ESSearchAPIImpl.applyPermissionsFilter(
                new JSONObject("{\"query\":{\"match_all\":{}}}"), null, true);
        final String query = filtered.getJSONObject("query").toString();

        Assert.assertTrue("The query must carry the category clause for the anonymous role: " + query,
                query.contains("categoryperms:cms_anon_role"));
        Assert.assertTrue("The query must let through content with no restricted category: " + query,
                query.contains("categoryperms:none"));
    }

    /**
     * MethodToTest {@link ESSearchAPIImpl#applyPermissionsFilter(JSONObject, User, boolean)}
     * Given Scenario: The secondary category check is turned off with
     *                 {@code PERMISSION_SECONDARY_CATEGORY_CHECK=false} and a non-admin user runs a
     *                 raw query.
     * ExpectedResult: The read-permission clause is still added, the category clause is not.
     */
    @Test
    public void test_applyPermissionsFilter_categoryCheckDisabled_noCategoryClause() throws Exception {

        final boolean original = Config.getBooleanProperty(CATEGORY_CHECK_PROPERTY, true);
        try {
            Config.setProperty(CATEGORY_CHECK_PROPERTY, false);

            final JSONObject filtered = ESSearchAPIImpl.applyPermissionsFilter(
                    new JSONObject("{\"query\":{\"match_all\":{}}}"), categoryOutsider, false);
            final String query = filtered.getJSONObject("query").toString();

            Assert.assertTrue("The query must carry the read-permission clause: " + query,
                    query.contains("+owner:" + categoryOutsider.getUserId()));
            Assert.assertFalse("The query must not carry the category clause: " + query,
                    query.contains("categoryperms:"));
        } finally {
            Config.setProperty(CATEGORY_CHECK_PROPERTY, original);
        }
    }

    /**
     * MethodToTest {@link ESSearchAPIImpl#esSearchRaw(String, boolean, User, boolean)}
     * Given Scenario: A content type whose Category field enables the secondary category check
     *                 holds one item tagged with a category only a given role can read, and one
     *                 item with no category. A back-end user without that role searches the type.
     * ExpectedResult: Only the uncategorized item comes back.
     */
    @Test
    public void test_esSearchRaw_userWithoutCategoryRole_doesNotGetCategoryRestrictedContent()
            throws Exception {
        skipWhenOpenSearchOnly();

        final Set<String> identifiers = searchCategorizedType(categoryOutsider);

        Assert.assertTrue("The uncategorized item must be returned: " + identifiers,
                identifiers.contains(uncategorizedContent.getIdentifier()));
        Assert.assertFalse("The item in a category the user cannot read must not be returned: "
                + identifiers, identifiers.contains(readersOnlyContent.getIdentifier()));
    }

    /**
     * MethodToTest {@link ESSearchAPIImpl#esSearchRaw(String, boolean, User, boolean)}
     * Given Scenario: Same data; a back-end user holding the role that can read the category
     *                 searches the type.
     * ExpectedResult: Both items come back.
     */
    @Test
    public void test_esSearchRaw_userWithCategoryRole_getsCategoryRestrictedContent()
            throws Exception {
        skipWhenOpenSearchOnly();

        final Set<String> identifiers = searchCategorizedType(categoryReader);

        Assert.assertTrue("The uncategorized item must be returned: " + identifiers,
                identifiers.contains(uncategorizedContent.getIdentifier()));
        Assert.assertTrue("The item in a category the user can read must be returned: "
                + identifiers, identifiers.contains(readersOnlyContent.getIdentifier()));
    }

    /**
     * Runs a raw Elasticsearch query for every item of the category test type, as the given user,
     * against the working index.
     *
     * @param searcher the user the search runs as
     * @return the identifiers of the items returned
     */
    private static Set<String> searchCategorizedType(final User searcher)
            throws DotDataException, DotSecurityException {
        final String query = "{\"query\":{\"query_string\":{\"query\":\"+contenttype:"
                + categorizedType.variable() + "\"}}}";
        final SearchResponse response = new ESSearchAPIImpl().esSearchRaw(query, false, searcher, false);
        final Set<String> identifiers = new HashSet<>();
        for (final SearchHit hit : response.getHits().getHits()) {
            identifiers.add(String.valueOf(hit.getSourceAsMap().get("identifier")));
        }
        return identifiers;
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
