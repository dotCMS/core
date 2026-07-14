package com.dotmarketing.servlets;

import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpStatusResponse;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import org.apache.commons.lang.RandomStringUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import static com.dotmarketing.business.Role.DOTCMS_BACK_END_USER;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ServletTestUtils {

    /**
     * Add read permissions to a role for a given contentlet
     * Also removes all permissions for the CMS Anonymous role
     * @param contentlet contentlet to add permissions to
     * @param role role to add permissions for
     * @throws DotDataException if there is an data error adding permissions
     * @throws DotSecurityException if there is a permissions error adding permissions
     */
    public static void addPermissions(
            final Contentlet contentlet, final Role role)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();
        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();

        final Permission anonPermission = new Permission();
        anonPermission.setInode(contentlet.getPermissionId());
        anonPermission.setRoleId(anonymousRole.getId());
        anonPermission.setPermission(0);

        final Permission permission = new Permission();
        permission.setInode(contentlet.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(PermissionAPI.PERMISSION_READ);

        APILocator.getPermissionAPI().save(
                List.of(anonPermission, permission),
                contentlet, systemUser, false);

    }

    /**
     * Add anonymous READ permission to a contentlet, making it publicly readable.
     * @param contentlet contentlet to make publicly readable
     * @throws DotDataException if there is a data error adding permissions
     * @throws DotSecurityException if there is a permissions error adding permissions
     */
    public static void addAnonymousReadPermission(final Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();
        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();

        final Permission anonPermission = new Permission();
        anonPermission.setInode(contentlet.getPermissionId());
        anonPermission.setRoleId(anonymousRole.getId());
        anonPermission.setPermission(PermissionAPI.PERMISSION_READ);

        APILocator.getPermissionAPI().save(
                List.of(anonPermission), contentlet, systemUser, false);
    }

    /**
     * Test that a servlet serving an asset does not reject the request when an unrelated/invalid
     * BASIC {@code Authorization} header is present — instead it falls through to anonymous and
     * delegates permission enforcement to the downstream {@code /contentAsset} servlet.
     * <p>
     * Both an invalid (non-dotCMS-user) credential and a valid dotCMS credential must result in the
     * request being forwarded; neither is rejected with a {@code 401} at this servlet. This is the
     * behavior fixed in dotCMS/core#35536: before the fix, an invalid/foreign credential aborted
     * with {@code 401} before any permission check.
     *
     * @param servlet servlet to test
     * @param urlGenerator function to generate a URL for the servlet
     * @throws DotDataException if there is a data error
     * @throws DotSecurityException if there is a security error
     * @throws ServletException if there is a servlet error
     * @throws IOException if there is an I/O error
     */
    public static void testServletWithAuthenticatedUser(
            final HttpServlet servlet, final Function<String, String> urlGenerator)
            throws DotDataException, DotSecurityException, ServletException, IOException {
        Host testHost = null;
        Role testRole = null;
        User testUser = null;
        try {
            // Create a test host, role, and user
            final String userPassword = RandomStringUtils.randomAlphabetic(10);
            final String userEmail = RandomStringUtils.randomAlphabetic(5) + "@dotcms.com";
            final String userEmailAndPassword = userEmail + ":" + userPassword;
            final String userEmailAndInvalidPassword = userEmail + ":"
                    + RandomStringUtils.randomAlphabetic(5) + userPassword;

            testHost = new SiteDataGen().nextPersisted();
            testRole = new RoleDataGen().nextPersisted();
            testUser = new UserDataGen().roles(testRole,
                            APILocator.getRoleAPI().loadRoleByKey(DOTCMS_BACK_END_USER))
                    .emailAddress(userEmail).password(userPassword).nextPersisted();

            // Create a dot asset and publish it
            // Add permissions to the asset for the test role
            final Folder folder = new FolderDataGen().site(testHost).nextPersisted();
            final Contentlet dotAssset = TestDataUtils.getDotAssetLikeContentlet(folder);
            addPermissions(dotAssset, testRole);
            ContentletDataGen.publish(dotAssset);

            // Generate URL for the asset
            final String assetId = dotAssset.getIdentifier();
            final String assetShortyURL = urlGenerator.apply(assetId);

            // Mock the request and response
            final MockHeaderRequest request = spy(new MockHeaderRequest(
                    new MockHttpRequestIntegrationTest(
                            "localhost", assetShortyURL).request()));

            final RequestDispatcher dispatcher = mock(RequestDispatcher.class);
            doReturn(dispatcher).when(request).getRequestDispatcher(anyString());

            final HttpServletResponse response = new MockHttpStatusResponse(
                    mock(HttpServletResponse.class));

            // Send request with an invalid (non-dotCMS-user) credential. Per #35536 the servlet must
            // NOT reject with 401 here; it falls through to anonymous and forwards, delegating
            // permission enforcement to the downstream /contentAsset (BinaryExporterServlet).
            request.setHeader("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(
                            userEmailAndInvalidPassword.getBytes()));
            servlet.service(request, response);

            // Verify the auth step did not abort the request with a 401
            assertNotEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());

            // Send request to the servlet with an authenticated user
            request.setHeader("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(
                            userEmailAndPassword.getBytes()));
            servlet.service(request, response);

            // Verify that the request was forwarded for both the invalid and the valid credential
            verify(dispatcher, times(2)).forward(any(), any());

        } finally {
            // Clean up
            if (testHost != null) {
                final User systemUser = APILocator.systemUser();
                testHost.setIndexPolicy(IndexPolicy.WAIT_FOR);
                APILocator.getHostAPI().unpublish(testHost, systemUser, false);
                APILocator.getHostAPI().archive(testHost, systemUser, false);
                APILocator.getHostAPI().delete(testHost, systemUser, false);
            }
            if (testRole != null) {
                RoleDataGen.remove(testRole);
            }
            if (testUser != null) {
                UserDataGen.remove(testUser);
            }
        }
    }

    /**
     * Regression test for dotCMS/core#35536: an anonymously-readable (public) asset must still be
     * served when the request carries an unrelated/foreign BASIC {@code Authorization} header whose
     * credentials are not a valid dotCMS user (e.g. a credential replayed by an upstream Basic-Auth
     * gating layer on a sub-resource request per RFC 7617). The servlet must fall through to
     * anonymous and forward the request rather than rejecting it with a {@code 401}.
     *
     * @param servlet servlet to test
     * @param urlGenerator function to generate a URL for the servlet
     * @throws DotDataException if there is a data error
     * @throws DotSecurityException if there is a security error
     * @throws ServletException if there is a servlet error
     * @throws IOException if there is an I/O error
     */
    public static void testPublicAssetServedWithForeignBasicAuth(
            final HttpServlet servlet, final Function<String, String> urlGenerator)
            throws DotDataException, DotSecurityException, ServletException, IOException {
        Host testHost = null;
        try {
            testHost = new SiteDataGen().nextPersisted();

            // Create a publicly-readable (anonymous READ) dot asset and publish it
            final Folder folder = new FolderDataGen().site(testHost).nextPersisted();
            final Contentlet dotAsset = TestDataUtils.getDotAssetLikeContentlet(folder);
            addAnonymousReadPermission(dotAsset);
            ContentletDataGen.publish(dotAsset);

            final String assetUrl = urlGenerator.apply(dotAsset.getIdentifier());

            final MockHeaderRequest request = spy(new MockHeaderRequest(
                    new MockHttpRequestIntegrationTest("localhost", assetUrl).request()));

            final RequestDispatcher dispatcher = mock(RequestDispatcher.class);
            doReturn(dispatcher).when(request).getRequestDispatcher(anyString());

            final HttpServletResponse response = new MockHttpStatusResponse(
                    mock(HttpServletResponse.class));

            // Foreign / non-dotCMS-user BASIC credentials, as an upstream gateway would replay
            final String foreignCredentials = "gateway-user:" + RandomStringUtils.randomAlphabetic(8);
            request.setHeader("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(foreignCredentials.getBytes()));

            servlet.service(request, response);

            // The public asset must be served (forwarded), not rejected with a 401
            assertNotEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
            verify(dispatcher).forward(any(), any());

        } finally {
            if (testHost != null) {
                final User systemUser = APILocator.systemUser();
                testHost.setIndexPolicy(IndexPolicy.WAIT_FOR);
                APILocator.getHostAPI().unpublish(testHost, systemUser, false);
                APILocator.getHostAPI().archive(testHost, systemUser, false);
                APILocator.getHostAPI().delete(testHost, systemUser, false);
            }
        }
    }
}
