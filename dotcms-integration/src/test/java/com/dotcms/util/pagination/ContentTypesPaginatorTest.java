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
import java.util.Objects;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.COMMA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    /**
     * Method to Test: {@link ContentTypesPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     * When: A simple test to ensure we can a Collection of BaseTypes and get coherent results
     * Result: We validate that the items are within the set of baseTypes we passed
     */
    @Test
    public void test_getItems_WhenMultipleBaseTypesArePassed() {
        final Map<String, Object> extraParams = Map.of(
                ContentTypesPaginator.TYPE_PARAMETER_NAME,
                Set.of(BaseContentType.PERSONA.toString(), BaseContentType.FILEASSET.toString())
        );

        final ContentTypesPaginator paginator = new ContentTypesPaginator();
        final PaginatedArrayList<Map<String, Object>> result = paginator
                .getItems(user, null, -1, 0, "name", OrderDirection.ASC, extraParams);

        assertTrue(UtilMethods.isSet(result));

        // We're passing a couple of base types PERSONA o FILEASSET
        assertTrue(result.stream().allMatch(contentType -> {
            String baseType = contentType.get("baseType").toString();
            return BaseContentType.PERSONA.name().equals(baseType)
                    || BaseContentType.FILEASSET.name().equals(baseType);
        }));

        // validate we get one or the other
        boolean hasPersona = result.stream()
                .anyMatch(contentType -> BaseContentType.PERSONA.name()
                        .equals(contentType.get("baseType").toString()));
        boolean hasFile = result.stream()
                .anyMatch(contentType -> BaseContentType.FILEASSET.name()
                        .equals(contentType.get("baseType").toString()));

        assertTrue("Expected at least one PERSONA in results", hasPersona);
        assertTrue("Expected at least one FILEASSET in results", hasFile);
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

    /**
     * Method to test: {@link BaseContentType#fromNames(List)}
     * Given Scenario: Test various scenarios for converting a list of names to BaseContentType set
     * Expected Result: Correct BaseContentType set based on valid names, ignoring invalid ones
     */
    @Test
    public void test_BaseContentType_fromNames() {
        // Test with null input
        Set<BaseContentType> result = BaseContentType.fromNames(null);
        assertEquals("Null input should return ANY", Set.of(BaseContentType.ANY), result);

        // Test with empty list
        result = BaseContentType.fromNames(List.of());
        assertEquals("Empty list should return ANY", Set.of(BaseContentType.ANY), result);

        // Test with valid single name
        result = BaseContentType.fromNames(List.of("CONTENT"));
        assertEquals("Valid single name should return correct type", Set.of(BaseContentType.CONTENT), result);

        // Test with valid multiple names
        result = BaseContentType.fromNames(List.of("CONTENT", "FORM", "PERSONA"));
        Set<BaseContentType> expected = Set.of(BaseContentType.CONTENT, BaseContentType.FORM, BaseContentType.PERSONA);
        assertEquals("Valid multiple names should return correct types", expected, result);

        // Test with case insensitive names
        result = BaseContentType.fromNames(List.of("content", "FORM", "Persona"));
        expected = Set.of(BaseContentType.CONTENT, BaseContentType.FORM, BaseContentType.PERSONA);
        assertEquals("Case insensitive names should work", expected, result);

        // Test with alternate names
        result = BaseContentType.fromNames(List.of("File", "Page", "Form"));
        expected = Set.of(BaseContentType.FILEASSET, BaseContentType.HTMLPAGE, BaseContentType.FORM);
        assertEquals("Alternate names should work", expected, result);

        // Test with null and blank entries
        result = BaseContentType.fromNames(Arrays.asList("CONTENT", null, "", "  ", "FORM"));
        expected = Set.of(BaseContentType.CONTENT, BaseContentType.FORM);
        assertEquals("Null and blank entries should be ignored", expected, result);

        // Test with names containing whitespace
        result = BaseContentType.fromNames(List.of("  CONTENT  ", " FORM "));
        expected = Set.of(BaseContentType.CONTENT, BaseContentType.FORM);
        assertEquals("Names with whitespace should be trimmed", expected, result);

        // Test with duplicate names
        result = BaseContentType.fromNames(List.of("CONTENT", "CONTENT", "FORM", "CONTENT"));
        expected = Set.of(BaseContentType.CONTENT, BaseContentType.FORM);
        assertEquals("Duplicate names should be deduplicated", expected, result);

        // Test with all BaseContentType values by name
        List<String> allTypeNames = Arrays.stream(BaseContentType.values())
                .filter(type -> type != BaseContentType.ANY)
                .map(BaseContentType::name)
                .collect(Collectors.toList());
        result = BaseContentType.fromNames(allTypeNames);
        Set<BaseContentType> allTypes = Arrays.stream(BaseContentType.values())
                .filter(type -> type != BaseContentType.ANY)
                .collect(Collectors.toSet());
        assertEquals("All valid type names should return all types", allTypes, result);

    }

    /**
     * Method to test: {@link BaseContentType#fromNames(List)}
     * Given Scenario: Test various scenarios for converting a list of names to BaseContentType set
     * Expected Result: Correct BaseContentType set based on valid names, ignoring invalid ones
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_BaseContentType_fromNames_InvalidTypes_Expect_Exception() {
        // Test with invalid names (should throw an IllegalArgumentException)
        BaseContentType.fromNames(List.of("CONTENT", "INVALID_TYPE", "FORM"));
    }


        /**
         * Method to test: {@link ContentTypesPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
         * Given Scenario: Create three Content Types with different Base Types and test filtering using Extra Params
         * with both Variable Names (types parameter) and Base Types (type parameter)
         * Expected Result: Validate that filtering works correctly for both parameters individually and combined
         */
    @Test
    public void test_getItems_ExtraParamsFiltering_VariableNamesAndBaseTypes() throws DotSecurityException, DotDataException {
        ContentType contentType1 = null;
        ContentType contentType2 = null;
        ContentType contentType3 = null;
        
        try {
            final long timestamp = System.currentTimeMillis();
            
            // Create three Content Types with different Base Types
            contentType1 = createContentTypeWithBaseType(BaseContentType.CONTENT, "TestContent" + timestamp);
            contentType2 = createContentTypeWithBaseType(BaseContentType.FORM, "TestForm" + timestamp);
            contentType3 = createContentTypeWithBaseType(BaseContentType.PERSONA, "TestPersona" + timestamp);
            
            final ContentTypesPaginator paginator = new ContentTypesPaginator();

            final List<String> variableNames = List.of(
                    Objects.requireNonNull(contentType1.variable()),
                    Objects.requireNonNull(contentType3.variable())
            );

            //Now Let's filter using only varNames
            final Map<String, Object> extraParams1 = new HashMap<>();
            extraParams1.put(ContentTypesPaginator.TYPES_PARAMETER_NAME, variableNames);
            PaginatedArrayList<Map<String, Object>> result1 = paginator
                    .getItems(user, null, -1, 0, "name", OrderDirection.ASC, extraParams1);
            assertEquals("Result should not be empty when filtering by variable names", 2, result1.size());
            assertTrue("returned ct name must be contained within varNames Collection", result1.stream().anyMatch(contentType -> {
                final String varName = (String)contentType.get("variable");
                return variableNames.contains(varName);
            }));

            //Filter by variable-names and Base-type
            final Map<String, Object> extraParams2 = new HashMap<>();
            extraParams2.put(ContentTypesPaginator.TYPES_PARAMETER_NAME, variableNames);
            extraParams2.put(ContentTypesPaginator.TYPE_PARAMETER_NAME, Set.of(BaseContentType.FILEASSET.name()));
            
            final PaginatedArrayList<Map<String, Object>> result2 = paginator
                .getItems(user, null, -1, 0, "name", OrderDirection.ASC, extraParams2);
            //Even though we are passing a list of variableName, we should get none back because we are filtering by base type
            assertTrue("Result should not be empty when filtering by variable names", result2.isEmpty());

        } finally {
            // Cleanup created Content Types
            if (contentType1 != null) {
                contentTypeApi.delete(contentType1);
            }
            if (contentType2 != null) {
                contentTypeApi.delete(contentType2);
            }
            if (contentType3 != null) {
                contentTypeApi.delete(contentType3);
            }
        }
    }

    /**
     * Helper method to create a Content Type with a specific Base Type
     */
    private ContentType createContentTypeWithBaseType(BaseContentType baseType, String name) 
            throws DotSecurityException, DotDataException {
        final long timestamp = System.currentTimeMillis();
        final String variable = "velocityVar" + name + timestamp;
        
        final ContentTypeBuilder builder = ContentTypeBuilder.builder(baseType.immutableClass())
                .description("Test description for " + name)
                .expireDateVar(null)
                .folder(FolderAPI.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST)
                .name(name)
                .owner("owner")
                .variable(variable);
        
        return contentTypeApi.save(builder.build());
    }

}
