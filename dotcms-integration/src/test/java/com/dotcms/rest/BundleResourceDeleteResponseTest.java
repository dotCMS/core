package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the synchronous response of {@link BundleResource#deleteBundlesByIdentifiers}, whose
 * actual delete runs in a background thread.
 */
public class BundleResourceDeleteResponseTest {

    private static final List<String> createdBundleIds = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @AfterClass
    public static void cleanup() {
        for (final String bundleId : createdBundleIds) {
            try {
                APILocator.getBundleAPI().deleteBundleAndDependencies(bundleId, APILocator.systemUser());
            } catch (Exception e) {
                // already removed by the delete under test
            }
        }
        createdBundleIds.clear();
    }

    /**
     * Method to Test: {@link BundleResource#deleteBundlesByIdentifiers(HttpServletRequest, HttpServletResponse, DeleteBundlesByIdentifierForm)}
     * Given: several existing bundles and one identifier that never existed
     * When:  they are deleted in one request
     * Should: report the existing bundles as being removed and only the unknown identifier as
     *         not existing, however fast the background delete finishes. Many bundles are used so
     *         that a response built after the delete started would reliably list some as missing.
     */
    @Test
    public void test_deleteBundlesByIdentifiers_responseReflectsStateBeforeDelete() throws Exception {
        final List<String> existingIds = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            existingIds.add(insertBundle());
        }
        final String unknownId = UUIDGenerator.generateUuid();
        final List<String> identifiers = new ArrayList<>(existingIds);
        identifiers.add(unknownId);

        final User admin = APILocator.systemUser();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(admin);

        final Response response = new BundleResource().deleteBundlesByIdentifiers(
                request, mock(HttpServletResponse.class),
                new DeleteBundlesByIdentifierForm.Builder().identifiers(identifiers).build());

        assertEquals(200, response.getStatus());
        final String message = ((ResponseEntityView<?>) response.getEntity()).getEntity().toString();
        assertTrue(message, message.startsWith("Removing the follow bundles in a separated process, "
                + "the result of the operation will be notified: " + String.join(", ", existingIds)));
        assertTrue(message, message.endsWith(
                "The following bundles were not deleted because they don't exist: " + unknownId));
    }

    private static String insertBundle() throws DotDataException {
        final Bundle bundle = new Bundle();
        bundle.setId(UUIDGenerator.generateUuid());
        bundle.setName("deleteResponseTestBundle" + System.currentTimeMillis());
        bundle.setForcePush(false);
        bundle.setOwner(APILocator.systemUser().getUserId());
        bundle.setPublishDate(new Date());
        APILocator.getBundleAPI().saveBundle(bundle);
        createdBundleIds.add(bundle.getId());
        return bundle.getId();
    }
}
