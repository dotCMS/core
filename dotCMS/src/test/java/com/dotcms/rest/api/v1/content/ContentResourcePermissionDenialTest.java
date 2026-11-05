package com.dotcms.rest.api.v1.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.liferay.portal.model.User;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.MockedStatic;

/** Content read denials must not return the protected content's metadata. */
public class ContentResourcePermissionDenialTest {

    /** Keeps the 403 exception type without leaking its original message or cause. */
    @Test
    public void test_contentReads_hideProtectedMetadataOnPermissionDenial() throws Exception {
        final WebResource webResource = mock(WebResource.class);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final ShortyIdAPI shortyAPI = mock(ShortyIdAPI.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final User user = mock(User.class);
        final InitDataObject initData = new InitDataObject();
        initData.setUser(user);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        when(shortyAPI.getShorty("known-inode")).thenReturn(Optional.of(
                new ShortyId("known", "known-inode", ShortType.INODE, ShortType.CONTENTLET)));
        when(contentletAPI.find(eq("known-inode"), eq(user), anyBoolean())).thenThrow(
                new DotSecurityException("READ denied: Confidential Salary, type Payroll, language 2, inode hidden-inode"));

        try (MockedStatic<APILocator> locator = mockStatic(APILocator.class)) {
            locator.when(APILocator::getShortyAPI).thenReturn(shortyAPI);
            final ContentResource resource = new ContentResource(webResource, contentletAPI,
                    null, null, null, null);
            final ThrowingRunnable[] reads = {
                    () -> resource.getContent(request, response, "known-inode", "1", "DEFAULT", -1),
                    () -> resource.getContentletReferences(request, response, "known-inode", "1"),
                    () -> resource.refreshContent(request, response, "known-inode", "1")
            };
            for (final ThrowingRunnable read : reads) {
                final DotSecurityException error = assertThrows(DotSecurityException.class, read);
                assertEquals("Content read forbidden", error.getMessage());
                assertNull("Dev-mode error responses must not expose the original cause", error.getCause());
            }
        }
    }
}
