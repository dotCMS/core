package com.dotmarketing.portlets.contentlet.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Integration tests for how {@link EditContentletAction#_retrieveWebAsset} loads the contentlet a
 * user opens in the legacy content editor, including the source contentlet of a new translation.
 */
public class EditContentletActionRetrieveTest extends IntegrationTestBase {

    private static User systemUser;
    private static Language defaultLanguage;
    private static Language secondLanguage;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        secondLanguage = new LanguageDataGen().nextPersisted();
    }

    /**
     * Method to test: {@link EditContentletAction#_retrieveWebAsset}
     * Given: a new translation whose source contentlet the user cannot read
     * Expected: a {@link DotSecurityException} with a generic message, and no contentlet is put
     * in the request
     */
    @Test
    public void test_newTranslation_sourceNotReadable_shouldBeRefused() throws Exception {
        final Contentlet source = onlyAdminCanRead(newContentlet());
        final User user = newUserWithRole(new RoleDataGen().nextPersisted());
        final ActionRequestImpl request = mockRequest(Map.of(
                "sibbling", source.getInode(),
                "lang", String.valueOf(secondLanguage.getId())));

        assertRefused(request, user, source);
    }

    /**
     * Method to test: {@link EditContentletAction#_retrieveWebAsset}
     * Given: the source contentlet is requested in its own language and the user cannot read it
     * Expected: a {@link DotSecurityException} with a generic message, and no contentlet is put
     * in the request
     */
    @Test
    public void test_sameLanguageSource_notReadable_shouldBeRefused() throws Exception {
        final Contentlet source = onlyAdminCanRead(newContentlet());
        final User user = newUserWithRole(new RoleDataGen().nextPersisted());
        final ActionRequestImpl request = mockRequest(Map.of(
                "sibbling", source.getInode(),
                "lang", String.valueOf(source.getLanguageId())));

        assertRefused(request, user, source);
    }

    /**
     * Method to test: {@link EditContentletAction#_retrieveWebAsset}
     * Given: an existing contentlet, opened by inode, that the user cannot read
     * Expected: a {@link DotSecurityException} with a generic message
     */
    @Test
    public void test_existingContentlet_notReadable_shouldBeRefused() throws Exception {
        final Contentlet contentlet = onlyAdminCanRead(newContentlet());
        final User user = newUserWithRole(new RoleDataGen().nextPersisted());
        final ActionRequestImpl request = mockRequest(Map.of("inode", contentlet.getInode()));

        assertRefused(request, user, contentlet);
    }

    /**
     * Method to test: {@link EditContentletAction#_retrieveWebAsset}
     * Given: a new translation whose source contentlet the user can read
     * Expected: the new contentlet is put in the request with the source's identifier and the
     * requested language
     */
    @Test
    public void test_newTranslation_sourceReadable_shouldOpen() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final Contentlet source = newContentlet();
        APILocator.getPermissionAPI().assignPermissions(List.of(
                        new Permission(source.getPermissionId(), APILocator.getRoleAPI().loadCMSAdminRole().getId(),
                                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true),
                        new Permission(source.getPermissionId(), role.getId(), PermissionAPI.PERMISSION_READ, true)),
                source, systemUser, false);
        final User user = newUserWithRole(role);
        assertTrue("The user should be able to read the source contentlet",
                APILocator.getPermissionAPI().doesUserHavePermission(source, PermissionAPI.PERMISSION_READ, user, false));
        final ActionRequestImpl request = mockRequest(Map.of(
                "sibbling", source.getInode(),
                "lang", String.valueOf(secondLanguage.getId())));

        new EditContentletAction()._retrieveWebAsset(request, null, null, null, user);

        final ArgumentCaptor<Object> edited = ArgumentCaptor.forClass(Object.class);
        verify(request).setAttribute(eq(WebKeys.CONTENTLET_EDIT), edited.capture());
        final Contentlet translation = (Contentlet) edited.getValue();
        assertEquals(source.getIdentifier(), translation.getIdentifier());
        assertEquals(secondLanguage.getId(), translation.getLanguageId());
    }

    private void assertRefused(final ActionRequestImpl request, final User user,
            final Contentlet contentlet) throws Exception {
        try {
            new EditContentletAction()._retrieveWebAsset(request, null, null, null, user);
            fail("The contentlet should not be loaded for a user who cannot read it");
        } catch (final DotSecurityException e) {
            assertTrue(e.getMessage().contains(user.getUserId()));
            assertFalse("The message should not describe the contentlet",
                    e.getMessage().contains(contentlet.getTitle()));
            assertFalse("The message should not describe the contentlet",
                    e.getMessage().contains(contentlet.getIdentifier()));
        }
        verify(request, never()).setAttribute(eq(WebKeys.CONTENTLET_EDIT), any());
    }

    private static Contentlet newContentlet() {
        return TestDataUtils.getGenericContentContent(true, defaultLanguage.getId());
    }

    /**
     * Leaves the contentlet readable by the CMS Administrator role only.
     */
    private static Contentlet onlyAdminCanRead(final Contentlet contentlet) throws Exception {
        final Role adminRole = APILocator.getRoleAPI().loadCMSAdminRole();
        APILocator.getPermissionAPI().assignPermissions(
                List.of(new Permission(contentlet.getPermissionId(), adminRole.getId(),
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true)),
                contentlet, systemUser, false);
        return contentlet;
    }

    /**
     * Creates a back-end user holding the given role, so READ depends only on permissions and not
     * on whether the content is live.
     */
    private static User newUserWithRole(final Role role) throws Exception {
        return new UserDataGen().roles(role, APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
    }

    private static ActionRequestImpl mockRequest(final Map<String, String> parameters) {
        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getParameter(anyString())).thenAnswer(
                invocation -> parameters.get(invocation.<String>getArgument(0)));
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getSession()).thenReturn(mock(HttpSession.class));
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        return request;
    }
}
