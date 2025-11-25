package com.dotcms.util.pagination;

import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.startup.runonce.Task04210CreateDefaultLanguageVariable;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.liferay.util.StringPool.COMMA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This Integration Test verifies that the {@link ContentTypesPaginator} class performs as expected.
 *
 * @author Will Ezell
 * @since Jun 14th, 2018
 */
@RunWith(DataProviderRunner.class)
public class ContentTypesPaginatorTest {

    private static User user;
    private static ContentTypeAPI contentTypeApi;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.systemUser();
        contentTypeApi = APILocator.getContentTypeAPI(user);
    }

    public static class TestCase {
        boolean caseSensitive;

        public TestCase(final boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }
    }

    @DataProvider
    public static Object[] testCases(){
        return new TestCase[]{
                new TestCase(true),
                new TestCase(false)
        };
    }

    @Test
    public void test_getItems_WhenNoFilter_ReturnsAllContentTypes() {
        final ContentTypesPaginator paginator = new ContentTypesPaginator();
        final PaginatedArrayList<Map<String, Object>> result = paginator
                .getItems(user, null, -1, 0);

        assertTrue(UtilMethods.isSet(result));
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link ContentTypesPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}</li>
     *     <li><b>Given Scenario: </b>Retrieve all Content Types of Base File Asset Type, ordered
     *     by name.</li>
     *     <li><b>Expected Result: </b>There must be at least one match for the File Asset
     *     Content Type.</li>
     * </ul>
     */
    @Test
    public void test_getItems_WhenFilterEqualsToBaseType_ReturnsAllChildrenContentTypes() {
        final Map<String, Object> extraParams = Map.of(ContentTypesPaginator.TYPE_PARAMETER_NAME,
                BaseContentType.FILEASSET.toString());
        final ContentTypesPaginator paginator = new ContentTypesPaginator();
        final PaginatedArrayList<Map<String, Object>> result = paginator
                .getItems(user, null, -1, 0, "name", OrderDirection.ASC, extraParams);

        assertTrue(UtilMethods.isSet(result));

        assertTrue(result.stream().anyMatch(contentType -> contentType.get("baseType").toString()
                .equals(BaseContentType.FILEASSET.name())));
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link ContentTypesPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}</li>
     *     <li><b>Given Scenario: </b>Retrieve all Content Types of Base Persona Type, ordered
     *     by name.</li>
     *     <li><b>Expected Result: </b>All the results must be of type Persona.</li>
     * </ul>
     */
    @Test
    public void test_getItems_WhenFilterEqualsToBaseType_ReturnsAllRelatedContentTypes() {
        final Map<String, Object> extraParams = Map.of(ContentTypesPaginator.TYPE_PARAMETER_NAME, BaseContentType.PERSONA.toString());
        final ContentTypesPaginator paginator = new ContentTypesPaginator();
        final PaginatedArrayList<Map<String, Object>> result = paginator
                .getItems(user, null, -1, 0, "name", OrderDirection.ASC, extraParams);

        assertTrue(UtilMethods.isSet(result));

        assertTrue(result.stream().allMatch(contentType ->
                contentType.get("baseType").toString().equals(BaseContentType.PERSONA.name())
                        || contentType.get("name").toString().toLowerCase()
                        .contains(BaseContentType.PERSONA.name().toLowerCase())));
    }

    @UseDataProvider("testCases")
    @Test
    public void test_getItems_WhenFilterContainsContentTypeName_ReturnsTheContentTypeThatMatches(final TestCase testCase)
            throws DotSecurityException, DotDataException {

        ContentType type = null;
        final ContentTypesPaginator paginator = new ContentTypesPaginator();

        try {
            type = createContentType();

            final String contentTypeName = type.name();

            final PaginatedArrayList<Map<String, Object>> result = paginator
                    .getItems(user, testCase.caseSensitive?contentTypeName:contentTypeName.toLowerCase(), -1, 0);

            assertTrue(UtilMethods.isSet(result));
            assertEquals(1, result.size());
            assertEquals(1, result.getTotalResults());
            assertEquals(contentTypeName, result.get(0).get("name"));
        }finally{
            contentTypeApi.delete(type);
        }
    }

    /**
     * Method to Test: {@link ContentTypesPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     * When: When the current indices are desactive or deleted
     * Should: Should return NA in the entries property
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void whenTheIndicesAreDeactivateShouldReturnNAInEntries() throws DotDataException, IOException {
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final String live = APILocator.getESIndexAPI().removeClusterIdFromName(indiciesInfo.getLive());
        final String working = APILocator.getESIndexAPI().removeClusterIdFromName(indiciesInfo.getWorking());
        try {
            if (live != null) {
                APILocator.getContentletIndexAPI().deactivateIndex(live);
            }

            APILocator.getContentletIndexAPI().deactivateIndex(working);

            final ContentTypesPaginator paginator = new ContentTypesPaginator();

            final PaginatedArrayList<Map<String, Object>> items = paginator.getItems(user, "", -1, 0);

            for (final Map<String, Object> item : items) {
                assertEquals("N/A", item.get("nEntries"));
            }

        }finally {
            if (live != null) {
                APILocator.getContentletIndexAPI().activateIndex(live);
            }

            APILocator.getContentletIndexAPI().activateIndex(working);
        }
    }

    private ContentType createContentType() throws DotSecurityException, DotDataException {
        final long i = System.currentTimeMillis();
        final String contentTypeName = BaseContentType.FORM.name() + "Testing" + i;

        //Create a new content type
        final ContentTypeBuilder builder = ContentTypeBuilder.builder(BaseContentType.FORM.immutableClass())
                .description("description" + i)
                .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                .name(contentTypeName).owner("owner")
                .variable("velocityVarNameTesting" + i);

        return contentTypeApi.save(builder.build());
    }

    /**
     *
     * @param role
     * @param site
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void addPermission(final Role role, final Host site) throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final Permission permission = new Permission();
        permission.setInode(site.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(PermissionAPI.PERMISSION_READ);
        APILocator.getPermissionAPI().save(CollectionsUtils.list(permission), site, systemUser, false);
    }

    /**
     * Method to test: {@link ContentTypesPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     *
     * Given Scenario: Get the complete list of Content Types without any pagination, ordered by their names.
     *
     * Expected Result: A list with 5 Content Type objects ordered by name.
     */
    @Test
    public void getAllAllowedContentTypesOrderedByName() {
        // Initialization
        final String DEFAULT_ORDER_BY = "UPPER(name)";
        final String NO_FILTER = StringPool.BLANK;
        final String contentTypeVars = "webPageContent,Vanityurl,DotAsset,htmlpageasset";
        final List<String> typeVarNames = Arrays.asList(contentTypeVars.split(COMMA));
        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put(ContentTypesPaginator.TYPES_PARAMETER_NAME, typeVarNames);
        final ContentTypesPaginator paginator = new ContentTypesPaginator();

        // Test data generation
        final PaginatedArrayList<Map<String, Object>> contentTypes =
                paginator.getItems(user, NO_FILTER, -1, -1, DEFAULT_ORDER_BY, OrderDirection.ASC, extraParams);

        // Assertions
        assertEquals("There must be 4 Content Types returned by the paginator", 4, contentTypes.size());
        assertEquals("The 'Content (Generic)' type must come first", "Content (Generic)",
                contentTypes.get(0).get("name").toString());
    }

    /**
     * Method to test: {@link ContentTypesPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     *
     * Given Scenario: Get the filtered list of Content Types with the letters "ent" without any pagination, ordered by
     * their names.
     *
     * Expected Result: A list with 2 Content Type objects ordered by name.
     */
    @Test
    public void getFilteredAllowedContentTypesOrderedByName() throws DotDataException {
        try {
            APILocator.getContentTypeAPI(APILocator.systemUser()).find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
        }catch (Exception e){
            //Create LanguageVariable Content Type in case doesn't exist
            Task04210CreateDefaultLanguageVariable upgradeTaskCreateLanguageVariable = new Task04210CreateDefaultLanguageVariable();
            upgradeTaskCreateLanguageVariable.executeUpgrade();
        }
        // Initialization
        final String DEFAULT_ORDER_BY = "UPPER(name)";
        final String FILTER = "va";
        final String contentTypeVars = "webPageContent,Vanityurl,DotAsset,htmlpageasset,Languagevariable,vanityurl";
        final List<String> typeVarNames = Arrays.asList(contentTypeVars.split(COMMA));
        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put(ContentTypesPaginator.TYPES_PARAMETER_NAME, typeVarNames);
        final ContentTypesPaginator paginator = new ContentTypesPaginator();

        // Test data generation
        final PaginatedArrayList<Map<String, Object>> contentTypes =
                paginator.getItems(user, FILTER, -1, -1, DEFAULT_ORDER_BY, OrderDirection.ASC, extraParams);

        // Assertions
        assertEquals("There must be 2 Content Types returned by the paginator", 2, contentTypes.size());
        assertEquals("The 'Language Variable' type must come first", "Language Variable",
                contentTypes.get(0).get("name").toString());
    }

    /**
     * Method to test: {@link ContentTypesPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     *
     * Given Scenario: Get the filtered paginated list of Content Types with the letters "set",  ordered by their names.
     *
     * Expected Result: A list with 1 Content Type object must be returned, as we're excluding one result.
     */
    @Test
    public void getFilteredPaginatedAllowedContentTypesOrderedByName() {
        // Initialization
        final String DEFAULT_ORDER_BY = "UPPER(name)";
        final String FILTER = "set";
        final String contentTypeVars = "webPageContent,calendarEvent,Vanityurl,DotAsset,htmlpageasset";
        final List<String> typeVarNames = Arrays.asList(contentTypeVars.split(COMMA));
        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put(ContentTypesPaginator.TYPES_PARAMETER_NAME, typeVarNames);
        final ContentTypesPaginator paginator = new ContentTypesPaginator();

        // Test data generation
        // Results must contain "DotAsset" and "htmlpageasset"
        final int offset = 0;
        final int limit = 4;
        final PaginatedArrayList<Map<String, Object>> contentTypes =
                paginator.getItems(user, FILTER, limit, offset, DEFAULT_ORDER_BY, OrderDirection.ASC, extraParams);

        // Assertions
        assertEquals("There must be two Content Types returned by the paginator", 2, contentTypes.size());
        assertEquals("The 'dotAsset' type must come first", "dotAsset", contentTypes.get(0).get("name").toString());
    }

    /**
     * Method to test: {@link ContentTypesPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     *
     * Given Scenario: Get a paginated list of Content Types, ordered by their names.
     *
     * Expected Result: Get the second page of results, which must have only 2 Content Types.
     */
    @Test
    public void getPaginatedAllowedContentTypesOrderedByName() {
        // Initialization
        final String DEFAULT_ORDER_BY = "UPPER(name)";
        final String NO_FILTER = StringPool.BLANK;
        final String contentTypeVars = "webPageContent,calendarEvent,Vanityurl,DotAsset,htmlpageasset";
        final List<String> typeVarNames = Arrays.asList(contentTypeVars.split(COMMA));
        final Map<String, Object> extraParams =
                Map.of(ContentTypesPaginator.TYPES_PARAMETER_NAME, typeVarNames);
        final ContentTypesPaginator paginator = new ContentTypesPaginator();
        // Offset and Limit are automatically adjusted by the PaginationUtil class. So, set them up as the class would
        // Get the result page #2, so set this to 3
        final int offset = 3;
        // And set a total of 3 items per page
        final int limit = 3;

        // Test data generation
        final PaginatedArrayList<Map<String, Object>> contentTypes =
                paginator.getItems(user, NO_FILTER, limit, offset, DEFAULT_ORDER_BY, OrderDirection.ASC, extraParams);

        // Assertions
        assertEquals("There must be 2 Content Types returned by the paginator", 2, contentTypes.size());
        assertEquals("The 'dotAsset' type must come first", "dotAsset", contentTypes.get(0).get("name").toString());
    }

}
