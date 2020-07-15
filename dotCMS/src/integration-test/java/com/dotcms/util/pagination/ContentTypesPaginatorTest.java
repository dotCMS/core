package com.dotcms.util.pagination;

import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.enterprise.rules.RulesAPIImpl;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.dotcms.util.CollectionsUtils.map;

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

    @Test
    public void test_getItems_WhenFilterEqualsToBaseType_ReturnsAllChildrenContentTypes() {
        final Map<String, Object> extraParams = map(ContentTypesPaginator.TYPE_PARAMETER_NAME,
                list(BaseContentType.FILEASSET.toString()));
        final ContentTypesPaginator paginator = new ContentTypesPaginator();
        final PaginatedArrayList<Map<String, Object>> result = paginator
                .getItems(user, null, -1, 0, "name", OrderDirection.ASC, extraParams);

        assertTrue(UtilMethods.isSet(result));

        assertTrue(result.stream().anyMatch(contentType -> contentType.get("baseType").toString()
                .equals(BaseContentType.FILEASSET.name())));
    }

    @Test
    public void test_getItems_WhenFilterEqualsToBaseType_ReturnsAllRelatedContentTypes() {
        final Map<String, Object> extraParams = map(ContentTypesPaginator.TYPE_PARAMETER_NAME, list(BaseContentType.PERSONA.toString()));
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

        final String live = indiciesInfo.getLive();
        final String working = indiciesInfo.getWorking();
        try {
            if (live != null) {
                APILocator.getContentletIndexAPI().deactivateIndex(live.substring(live.indexOf(".") + 1));
            }

            APILocator.getContentletIndexAPI().deactivateIndex(working.substring(working.indexOf(".") + 1));

            final ContentTypesPaginator paginator = new ContentTypesPaginator();

            final PaginatedArrayList<Map<String, Object>> items = paginator.getItems(user, "", -1, 0);

            for (final Map<String, Object> item : items) {
                assertEquals("N/A", item.get("nEntries"));
            }

        }finally {
            if (live != null) {
                APILocator.getContentletIndexAPI().activateIndex(live.substring(live.indexOf(".") + 1));
            }

            APILocator.getContentletIndexAPI().activateIndex(working.substring(working.indexOf(".") + 1));
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

}
