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
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
     * Test a servlet with an authenticated user
     * First, the servlet is tested with an unauthenticated user and the status code is verified to be 401
     * Then, the servlet is tested with an authenticated user and the request is verified to be forwarded
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

            // Send request to the servlet with a non-authenticated user
            request.setHeader("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(
                            userEmailAndInvalidPassword.getBytes()));
            servlet.service(request, response);

            // Verify that the status code is 401
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());

            // Send request to the servlet with an authenticated user
            request.setHeader("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(
                            userEmailAndPassword.getBytes()));
            servlet.service(request, response);

            // Verify that the request was forwarded
            verify(dispatcher).forward(any(), any());

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
}
