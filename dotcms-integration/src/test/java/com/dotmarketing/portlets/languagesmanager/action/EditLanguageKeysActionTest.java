package com.dotmarketing.portlets.languagesmanager.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletSessionImpl;
import com.liferay.util.servlet.SessionMessages;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the {@code cmd=save} branch of {@link EditLanguageKeysAction}, driven
 * through {@link EditLanguageKeysAction#processAction} with real roles and layouts: the posted
 * language keys are only written for a user with the Languages or the Locales portlet.
 */
public class EditLanguageKeysActionTest {

    private static final String LANGUAGES_PORTLET = "languages";
    private static final String LOCALES_PORTLET = "locales";
    private static final String ERROR_MESSAGE = "error";
    private static final String SUCCESS_MESSAGE = "message";
    private static final String VALUE = "Saved from the language keys page";
    private static final String REFERER = "/c/portal/layout?p_l_id=languages";

    private static LanguageAPI languageAPI;
    private static Language language;
    private static Layout languagesLayout;
    private static User userWithoutPortlet;
    private static User languagesPortletUser;

    private final String key = "integration.language.action.key." + System.nanoTime();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        languageAPI = APILocator.getLanguageAPI();
        language = languageAPI.getDefaultLanguage();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();

        userWithoutPortlet = backendUser("language-keys-no-portlet");
        languagesPortletUser = backendUser("language-keys-portlet");
        languagesLayout = new LayoutDataGen()
                .name("language-keys-" + System.nanoTime())
                .portletIds(LANGUAGES_PORTLET)
                .nextPersisted();
        roleAPI.addLayoutToRole(languagesLayout, roleAPI.getUserRole(languagesPortletUser));

        assertFalse(layoutAPI.doesUserHaveAccessToPortlet(LANGUAGES_PORTLET, userWithoutPortlet));
        assertFalse(layoutAPI.doesUserHaveAccessToPortlet(LOCALES_PORTLET, userWithoutPortlet));
        assertTrue(layoutAPI.doesUserHaveAccessToPortlet(LANGUAGES_PORTLET, languagesPortletUser));
    }

    /**
     * Deletes the test users, handing their content to the admin user, and the test layout
     * through a freshly loaded instance (the persisted one is detached from the current session).
     */
    @AfterClass
    public static void cleanUp() throws Exception {
        final User replacement = TestUserUtils.getAdminUser();
        for (final User user : new User[]{userWithoutPortlet, languagesPortletUser}) {
            if (null != user) {
                APILocator.getUserAPI().delete(user, replacement, APILocator.systemUser(), false);
            }
        }
        if (null != languagesLayout) {
            final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
            layoutAPI.removeLayout(layoutAPI.findLayout(languagesLayout.getId()));
        }
    }

    /**
     * Deletes the test key. {@link LanguageAPI#saveLanguageKeys} adds the keys already stored for
     * the language to the maps it is given, so they must be mutable.
     */
    @After
    public void removeKey() throws Exception {
        languageAPI.saveLanguageKeys(language, new HashMap<>(), new HashMap<>(),
                Collections.singleton(key));
        languageAPI.clearCache();
    }

    /**
     * Method to test: {@link EditLanguageKeysAction#processAction} with {@code cmd=save}
     * Given: a back-end user with neither the Languages nor the Locales portlet posts a new key,
     * with a referer to return to
     * Expected: the action's error message, no success message, no redirect to the referer, and
     * the key is not written
     */
    @Test
    public void save_requiresLanguagesOrLocalesPortlet() throws Exception {
        final ActionRequestImpl request = saveRequest(userWithoutPortlet);
        when(request.getParameter("referer")).thenReturn(REFERER);
        final ActionResponse response = mock(ActionResponse.class);

        new EditLanguageKeysAction().processAction(null, null, null, request, response);

        assertFalse("the key must not be written", storedValue().isPresent());
        assertTrue(SessionMessages.contains(request, ERROR_MESSAGE));
        assertFalse(SessionMessages.contains(request, SUCCESS_MESSAGE));
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * Method to test: {@link EditLanguageKeysAction#processAction} with {@code cmd=save}
     * Given: a back-end user with the Languages portlet posts a new key
     * Expected: the key is written with the posted value and the success message is shown
     */
    @Test
    public void save_allowsLanguagesPortlet() throws Exception {
        final ActionRequestImpl request = saveRequest(languagesPortletUser);

        new EditLanguageKeysAction().processAction(null, null, null, request, mock(ActionResponse.class));

        assertEquals(Optional.of(VALUE), storedValue());
        assertTrue(SessionMessages.contains(request, SUCCESS_MESSAGE));
        assertFalse(SessionMessages.contains(request, ERROR_MESSAGE));
    }

    private static User backendUser(final String label) throws DotDataException {
        return new UserDataGen()
                .emailAddress(label + System.nanoTime() + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
    }

    /**
     * A {@code cmd=save} request of the given user that posts one country-specific key for the
     * default language, in the parameter format of the language keys page. Request and session
     * attributes are kept in maps so the action can read back what it stores, and the message
     * resources the action reloads after saving are a mock.
     */
    private ActionRequestImpl saveRequest(final User user) {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getAttribute(WebKeys.USER)).thenReturn(user);
        when(httpRequest.getRemoteUser()).thenReturn(user.getUserId());

        final Map<String, Object> sessionAttributes = new HashMap<>();
        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(anyString()))
                .thenAnswer(invocation -> sessionAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> sessionAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(httpSession).setAttribute(anyString(), any());
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpRequest.getSession(false)).thenReturn(httpSession);
        final PortletSessionImpl portletSession = mock(PortletSessionImpl.class);
        when(portletSession.getSession()).thenReturn(httpSession);

        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(request.getPortletSession()).thenReturn(portletSession);
        when(request.getRemoteUser()).thenReturn(user.getUserId());

        final Map<String, Object> requestAttributes = new HashMap<>();
        requestAttributes.put(Globals.MESSAGES_KEY, mock(MultiMessageResources.class));
        when(request.getAttribute(anyString()))
                .thenAnswer(invocation -> requestAttributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> requestAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());

        final String languageCode = language.getLanguageCode();
        when(request.getParameter("cmd")).thenReturn("save");
        when(request.getParameter("keys")).thenReturn("1");
        when(request.getParameter(languageCode + "-0-key")).thenReturn(key);
        when(request.getParameter(languageCode + "-general-0-value")).thenReturn("");
        when(request.getParameter(languageCode + "-" + language.getCountryCode() + "-0-value"))
                .thenReturn(VALUE);
        return request;
    }

    private Optional<String> storedValue() {
        languageAPI.clearCache();
        return languageAPI.getLanguageKeys(language.getLanguageCode(), language.getCountryCode())
                .stream()
                .filter(languageKey -> key.equals(languageKey.getKey()))
                .map(LanguageKey::getValue)
                .findFirst();
    }
}
