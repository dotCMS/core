package com.dotcms.util.pagination;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;

import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.Map;
import static com.dotcms.util.CollectionsUtils.map;

public class ContentTypesPaginatorTest {

    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.systemUser();
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

        assertTrue(result.stream().anyMatch(contentType ->
                contentType.get("baseType").toString().equals(BaseContentType.PERSONA.name())
                        || contentType.get("name").toString().toLowerCase()
                        .contains(BaseContentType.PERSONA.name().toLowerCase())));
    }

    @Test
    public void test_getItems_WhenFilterStartsWithBaseType_ReturnsAllChildrenContentTypes() {
        final Map<String, Object> extraParams = map(ContentTypesPaginator.TYPE_PARAMETER_NAME, list("File"));
        final ContentTypesPaginator paginator = new ContentTypesPaginator();
        final PaginatedArrayList<Map<String, Object>> result = paginator
                .getItems(user, null, -1, 0, "name", OrderDirection.ASC, extraParams);

        assertTrue(UtilMethods.isSet(result));

        assertTrue(result.stream().anyMatch(contentType -> contentType.get("baseType").toString()
                .equals(BaseContentType.FILEASSET.name())));
    }

    @Test
    public void test_getItems_WhenMultiBaseTypeFilter_ReturnsAllChildrenContentTypes() {
        final Map<String, Object> extraParams = map(ContentTypesPaginator.TYPE_PARAMETER_NAME, list(BaseContentType.FILEASSET.toString(),
                BaseContentType.PERSONA.toString()));
        final ContentTypesPaginator paginator = new ContentTypesPaginator();
        final PaginatedArrayList<Map<String, Object>> result = paginator
                .getItems(user, null, -1, 0, "name", OrderDirection.ASC, extraParams);

        assertTrue(UtilMethods.isSet(result));

        assertTrue(result.stream().anyMatch(contentType -> contentType.get("baseType").toString()
                .equals(BaseContentType.FILEASSET.name())));

        assertTrue(result.stream().anyMatch(contentType -> contentType.get("baseType").toString()
                .equals(BaseContentType.PERSONA.name())));
    }

    @Test
    public void test_getItems_WhenFilterContainsContentTypeName_ReturnsTheContentTypeThatMatches()
            throws DotSecurityException, DotDataException {

        final ContentTypeAPI contentTypeApi   = APILocator.getContentTypeAPI(user);
        final ContentTypesPaginator paginator = new ContentTypesPaginator();

        final long i = System.currentTimeMillis();
        final String contentTypeName = BaseContentType.FORM.name() + "Testing" + i;

        //Create a new content type
        ContentTypeBuilder builder = ContentTypeBuilder.builder(BaseContentType.FORM.immutableClass())
                .description("description" + i)
                .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                .name(contentTypeName).owner("owner")
                .variable("velocityVarNameTesting" + i);

        ContentType type = builder.build();
        try {
            type = contentTypeApi.save(type);

            final PaginatedArrayList<Map<String, Object>> result = paginator
                    .getItems(user, contentTypeName, -1, 0);

            assertTrue(UtilMethods.isSet(result));
            assertTrue(result.size() == 1);
            assertTrue(result.get(0).get("name").toString().equals(contentTypeName));
        }finally{
            contentTypeApi.delete(type);
        }
    }

}
