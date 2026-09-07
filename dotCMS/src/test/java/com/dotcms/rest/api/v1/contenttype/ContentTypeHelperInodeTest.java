package com.dotcms.rest.api.v1.contenttype;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for {@link ContentTypeHelper#loadContentletIfPresent(String, ContentType, User)}.
 */
public class ContentTypeHelperInodeTest extends UnitTestBase {

    private static final String INODE = "test-inode-123";
    private static final String TYPE_A_ID = "content-type-a";
    private static final String TYPE_B_ID = "content-type-b";

    private ContentType contentTypeA() {
        return ContentTypeBuilder.builder(SimpleContentType.class)
                .id(TYPE_A_ID)
                .name("Type A")
                .variable("typeA")
                .build();
    }

    private ContentType contentTypeB() {
        return ContentTypeBuilder.builder(SimpleContentType.class)
                .id(TYPE_B_ID)
                .name("Type B")
                .variable("typeB")
                .build();
    }

    private Contentlet contentletOfType(final ContentType type) {
        final Contentlet contentlet = new Contentlet();
        contentlet.setInode(INODE);
        contentlet.setContentType(type);
        return contentlet;
    }

    @Test
    public void testLoadContentletIfPresent_whenInodeNotSet_returnsNull() throws DotSecurityException {
        final ContentTypeHelper helper = ContentTypeHelper.getInstance();
        assertNull(helper.loadContentletIfPresent(null, contentTypeA(), mock(User.class)));
        assertNull(helper.loadContentletIfPresent("", contentTypeA(), mock(User.class)));
    }

    @Test
    public void testLoadContentletIfPresent_whenContentletMatchesType_returnsContentlet()
            throws Exception {
        final User user = mock(User.class);
        final ContentType requestedType = contentTypeA();
        final Contentlet contentlet = contentletOfType(requestedType);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final ContentTypeAPI contentTypeAPI = mock(ContentTypeAPI.class);

        when(contentletAPI.find(INODE, user, false)).thenReturn(contentlet);
        when(contentTypeAPI.find(TYPE_A_ID)).thenReturn(requestedType);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
            apiLocator.when(() -> APILocator.getContentTypeAPI(user)).thenReturn(contentTypeAPI);
            apiLocator.when(APILocator::systemUser).thenReturn(user);

            final Contentlet result = ContentTypeHelper.getInstance()
                    .loadContentletIfPresent(INODE, requestedType, user);

            assertSame(contentlet, result);
        }
    }

    @Test
    public void testLoadContentletIfPresent_whenContentletTypeMismatch_returnsNull() throws Exception {
        final User user = mock(User.class);
        final ContentType requestedType = contentTypeA();
        final ContentType actualType = contentTypeB();
        final Contentlet contentlet = contentletOfType(actualType);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final ContentTypeAPI contentTypeAPI = mock(ContentTypeAPI.class);

        when(contentletAPI.find(INODE, user, false)).thenReturn(contentlet);
        when(contentTypeAPI.find(TYPE_B_ID)).thenReturn(actualType);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
            apiLocator.when(() -> APILocator.getContentTypeAPI(user)).thenReturn(contentTypeAPI);
            apiLocator.when(APILocator::systemUser).thenReturn(user);

            final Contentlet result = ContentTypeHelper.getInstance()
                    .loadContentletIfPresent(INODE, requestedType, user);

            assertNull(result);
        }
    }

    @Test
    public void testLoadContentletIfPresent_whenSecurityException_propagates() throws Exception {
        final User user = mock(User.class);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final DotSecurityException securityException = new DotSecurityException("access denied");

        when(contentletAPI.find(INODE, user, false)).thenThrow(securityException);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);

            try {
                ContentTypeHelper.getInstance().loadContentletIfPresent(INODE, contentTypeA(), user);
                fail("Expected DotSecurityException");
            } catch (DotSecurityException e) {
                assertSame(securityException, e);
            }
        }
    }

    @Test
    public void testLoadContentletIfPresent_whenDataException_returnsNull() throws Exception {
        final User user = mock(User.class);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);

        when(contentletAPI.find(INODE, user, false)).thenThrow(new DotDataException("not found"));

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);

            assertNull(ContentTypeHelper.getInstance()
                    .loadContentletIfPresent(INODE, contentTypeA(), user));
        }
    }
}
