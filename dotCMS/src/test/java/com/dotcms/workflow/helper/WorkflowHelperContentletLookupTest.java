package com.dotcms.workflow.helper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link WorkflowHelper#getContentletByIdentifier(String, PageMode, User, String,
 * java.util.function.Supplier)}: the lookup returns the version in the session language, then the
 * default language, then any language, and only ever a version the user can read.
 */
class WorkflowHelperContentletLookupTest {

    private static final String IDENTIFIER = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4";
    private static final long SESSION_LANGUAGE = 2L;
    private static final long DEFAULT_LANGUAGE = 1L;
    private static final String VARIANT = VariantAPI.DEFAULT_VARIANT.name();

    private MockedStatic<APILocator> apiLocator;
    private ContentletAPI contentletAPI;
    private PermissionAPI permissionAPI;
    private WorkflowHelper workflowHelper;
    private User user;
    private Contentlet contentlet;

    @BeforeEach
    void setUp() {
        contentletAPI = mock(ContentletAPI.class);
        permissionAPI = mock(PermissionAPI.class);
        user = mock(User.class);
        contentlet = new Contentlet();
        contentlet.setIdentifier(IDENTIFIER);

        final ShortyIdAPI shortyIdAPI = mock(ShortyIdAPI.class);
        when(shortyIdAPI.getShorty(anyString())).thenReturn(Optional.empty());
        final Language defaultLanguage = mock(Language.class);
        when(defaultLanguage.getId()).thenReturn(DEFAULT_LANGUAGE);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        when(languageAPI.getDefaultLanguage()).thenReturn(defaultLanguage);

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getShortyAPI).thenReturn(shortyIdAPI);
        apiLocator.when(APILocator::getLanguageAPI).thenReturn(languageAPI);

        workflowHelper = new WorkflowHelper(mock(WorkflowAPI.class), mock(RoleAPI.class),
                contentletAPI, permissionAPI, mock(WorkflowImportExportUtil.class));
    }

    @AfterEach
    void tearDown() {
        apiLocator.close();
    }

    @Test
    void returnsSessionLanguageVersionWhenReadable() throws Exception {
        // Given a version in the session language that the user can read
        when(contentletAPI.findContentletByIdentifier(IDENTIFIER, false, SESSION_LANGUAGE, VARIANT,
                user, false)).thenReturn(contentlet);

        // When the contentlet is looked up by identifier
        final Optional<Contentlet> result = lookUp(PageMode.EDIT_MODE);

        // Then that version is returned and the any-language lookup is not used
        assertSame(contentlet, result.orElse(null));
        verify(contentletAPI, never()).findContentletByIdentifierAnyLanguage(anyString());
    }

    @Test
    void fallsBackToAnyLanguageVersionWhenReadable() throws Exception {
        // Given no version in the session or default language, and a version in another
        // language that the user can read
        givenNoPerLanguageVersion();
        when(contentletAPI.findContentletByIdentifierAnyLanguage(IDENTIFIER)).thenReturn(contentlet);
        when(permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user,
                false)).thenReturn(true);

        // When the contentlet is looked up by identifier
        final Optional<Contentlet> result = lookUp(PageMode.EDIT_MODE);

        // Then the version in the other language is returned
        assertSame(contentlet, result.orElse(null));
    }

    @Test
    void returnsEmptyWhenPerLanguageLookupIsRefused() throws Exception {
        // Given the per-language lookups refuse the user, and a version exists in some language
        // that the user cannot read
        when(contentletAPI.findContentletByIdentifier(eq(IDENTIFIER), anyBoolean(), anyLong(),
                anyString(), eq(user), anyBoolean()))
                .thenThrow(new DotSecurityException("not allowed"));
        when(contentletAPI.findContentletByIdentifierAnyLanguage(IDENTIFIER)).thenReturn(contentlet);
        when(permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user,
                false)).thenReturn(false);

        // When the contentlet is looked up by identifier
        final Optional<Contentlet> result = lookUp(PageMode.EDIT_MODE);

        // Then nothing is returned
        assertFalse(result.isPresent());
    }

    @Test
    void returnsEmptyWhenAnyLanguageVersionIsNotReadable() throws Exception {
        // Given no version in the session or default language, and a version in another
        // language that the user cannot read
        givenNoPerLanguageVersion();
        when(contentletAPI.findContentletByIdentifierAnyLanguage(IDENTIFIER)).thenReturn(contentlet);
        when(permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user,
                false)).thenReturn(false);

        // When the contentlet is looked up by identifier
        final Optional<Contentlet> result = lookUp(PageMode.EDIT_MODE);

        // Then nothing is returned
        assertFalse(result.isPresent());
    }

    @Test
    void checksReadWithModeFrontendRolesFlag() throws Exception {
        // Given LIVE mode, which honours front-end roles, and only an any-language version
        givenNoPerLanguageVersion();
        when(contentletAPI.findContentletByIdentifierAnyLanguage(IDENTIFIER)).thenReturn(contentlet);
        when(permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user,
                true)).thenReturn(true);

        // When the contentlet is looked up by identifier
        final Optional<Contentlet> result = lookUp(PageMode.LIVE);

        // Then READ is checked with the mode's front-end roles flag
        assertSame(contentlet, result.orElse(null));
        verify(permissionAPI).doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ,
                user, PageMode.LIVE.respectAnonPerms);
    }

    private void givenNoPerLanguageVersion() throws Exception {
        when(contentletAPI.findContentletByIdentifier(eq(IDENTIFIER), anyBoolean(), anyLong(),
                anyString(), eq(user), anyBoolean())).thenReturn(null);
    }

    private Optional<Contentlet> lookUp(final PageMode mode) throws Exception {
        return workflowHelper.getContentletByIdentifier(IDENTIFIER, mode, user, VARIANT,
                () -> SESSION_LANGUAGE);
    }
}
