package com.dotmarketing.portlets.languagesmanager.ajax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

/**
 * Unit tests for the access rules of {@link LanguageAjax}: reading or saving language keys
 * requires the Languages or the Locales portlet. The logged-in user comes from a mocked DWR
 * request and every collaborator is mocked.
 */
class LanguageAjaxTest {

    private static final String SEPARATOR = WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR;

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final UserWebAPI userWebAPI = mock(UserWebAPI.class);
    private final LayoutAPI layoutAPI = mock(LayoutAPI.class);
    private final LanguageAPI languageAPI = mock(LanguageAPI.class);
    private final Language english = mock(Language.class);
    private final User user = mock(User.class);

    /** Names of the {@link SecurityLogger} methods called during a test. */
    private final List<String> securityLoggerCalls = new ArrayList<>();

    private MockedStatic<WebContextFactory> webContextFactory;
    private MockedStatic<WebAPILocator> webAPILocator;
    private MockedStatic<PortalUtil> portalUtil;
    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<UtilMethods> utilMethods;
    private MockedStatic<LanguageUtil> languageUtil;
    private MockedStatic<SecurityLogger> securityLogger;

    @BeforeEach
    void setUp() throws Exception {
        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);
        webContextFactory = mockStatic(WebContextFactory.class);
        webContextFactory.when(WebContextFactory::get).thenReturn(webContext);

        when(user.getUserId()).thenReturn("back-end-user");
        when(user.getEmailAddress()).thenReturn("user@test.local");
        when(user.isBackendUser()).thenReturn(true);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
        when(userWebAPI.isLoggedToBackend(request)).thenReturn(true);
        webAPILocator = mockStatic(WebAPILocator.class);
        webAPILocator.when(WebAPILocator::getUserWebAPI).thenReturn(userWebAPI);
        portalUtil = mockStatic(PortalUtil.class);
        portalUtil.when(() -> PortalUtil.getUser(request)).thenReturn(user);

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getLanguageAPI).thenReturn(languageAPI);
        apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
        when(languageAPI.getLanguage("en", "US")).thenReturn(english);

        final Company company = mock(Company.class);
        when(company.getCompanyId()).thenReturn("company");
        when(company.getLocale()).thenReturn(Locale.US);
        utilMethods = mockStatic(UtilMethods.class, CALLS_REAL_METHODS);
        utilMethods.when(UtilMethods::getDefaultCompany).thenReturn(company);
        languageUtil = mockStatic(LanguageUtil.class);
        securityLogger = mockStatic(SecurityLogger.class, invocation -> {
            securityLoggerCalls.add(invocation.getMethod().getName());
            return RETURNS_DEFAULTS.answer(invocation);
        });
    }

    @AfterEach
    void tearDown() {
        securityLogger.close();
        languageUtil.close();
        utilMethods.close();
        apiLocator.close();
        portalUtil.close();
        webAPILocator.close();
        webContextFactory.close();
    }

    /** Grants, or not, each of the two portlets that manage language keys. */
    private void givenPortlets(final boolean languages, final boolean locales) throws Exception {
        when(layoutAPI.doesUserHaveAccessToPortlet("languages", user)).thenReturn(languages);
        when(layoutAPI.doesUserHaveAccessToPortlet("locales", user)).thenReturn(locales);
    }

    /** Saves one general key, as the language keys page sends it. */
    private void saveOneKey() throws Exception {
        new LanguageAjax().saveKeys("en", "US",
                List.of("greeting" + SEPARATOR + "Hello" + SEPARATOR), List.of(), List.of());
    }

    /**
     * Given: a back-end user with neither the Languages nor the Locales portlet
     * When: saveKeys is called
     * Then: a DotSecurityException is thrown and no key is saved
     */
    @Test
    void saveKeys_requiresLanguagesOrLocalesPortlet() throws Exception {
        givenPortlets(false, false);

        assertThrows(DotSecurityException.class, this::saveOneKey);
        verify(languageAPI, never()).saveLanguageKeys(any(), any(), any(), any());
    }

    /**
     * Given: a back-end user with the Languages portlet only
     * When: saveKeys is called
     * Then: the keys are saved
     */
    @Test
    void saveKeys_allowsLanguagesPortlet() throws Exception {
        givenPortlets(true, false);

        saveOneKey();

        verify(languageAPI).saveLanguageKeys(eq(english), eq(Map.of("greeting", "Hello")), any(), any());
    }

    /**
     * Given: a back-end user with the Locales portlet only
     * When: saveKeys is called
     * Then: the keys are saved
     */
    @Test
    void saveKeys_allowsLocalesPortlet() throws Exception {
        givenPortlets(false, true);

        saveOneKey();

        verify(languageAPI).saveLanguageKeys(eq(english), eq(Map.of("greeting", "Hello")), any(), any());
    }

    /**
     * Given: a back-end user with neither the Languages nor the Locales portlet
     * When: saveKeys is called
     * Then: the denial is recorded once in the security log, not once per portlet checked
     */
    @Test
    void saveKeys_denialLoggedOnce() throws Exception {
        givenPortlets(false, false);

        assertThrows(DotSecurityException.class, this::saveOneKey);

        assertEquals(1, securityLoggerCalls.stream().filter(name -> name.startsWith("log")).count(),
                "one security log entry per denial, got " + securityLoggerCalls);
    }

    /** A call to one of the methods that read language keys. */
    @FunctionalInterface
    interface KeyReader {
        List<?> read(LanguageAjax ajax) throws Exception;
    }

    static Stream<Arguments> keyReaders() {
        return Stream.of(
                Arguments.of("getLanguageKeys(code)", (KeyReader) ajax -> ajax.getLanguageKeys("en")),
                Arguments.of("getLanguageKeys(code, country)",
                        (KeyReader) ajax -> ajax.getLanguageKeys("en", "US")),
                Arguments.of("getPaginatedLanguageKeys(code, page)",
                        (KeyReader) ajax -> ajax.getPaginatedLanguageKeys("en", 1)),
                Arguments.of("getPaginatedLanguageKeys(code, country, page, filter)",
                        (KeyReader) ajax -> ajax.getPaginatedLanguageKeys("en", "US", 1, "")));
    }

    /** One stored key, returned by every language key lookup of the language API. */
    private void givenStoredKey() {
        final List<LanguageKey> keys = List.of(new LanguageKey("en", "US", "greeting", "Hello"));
        when(languageAPI.getLanguageKeys("en")).thenReturn(keys);
        when(languageAPI.getLanguageKeys("en", null)).thenReturn(keys);
        when(languageAPI.getLanguageKeys("en", "US")).thenReturn(keys);
    }

    /**
     * Given: a back-end user with neither the Languages nor the Locales portlet
     * When: each method that reads language keys is called
     * Then: a DotSecurityException is thrown, no key is read and the denial is recorded once in
     * the security log
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("keyReaders")
    void keyReads_requireLanguagesOrLocalesPortlet(final String method, final KeyReader reader)
            throws Exception {
        givenPortlets(false, false);
        givenStoredKey();

        assertThrows(DotSecurityException.class, () -> reader.read(new LanguageAjax()),
                method + " must not return keys to a user with neither portlet");

        verify(languageAPI, never()).getLanguageKeys(anyString());
        verify(languageAPI, never()).getLanguageKeys(anyString(), any());
        assertEquals(1, securityLoggerCalls.stream().filter(name -> name.startsWith("log")).count(),
                "one security log entry per denial, got " + securityLoggerCalls);
    }

    /**
     * Given: a back-end user with the Languages portlet only
     * When: getLanguageKeys is called, with and without a country
     * Then: the stored keys are returned
     */
    @Test
    void getLanguageKeys_allowsLanguagesPortlet() throws Exception {
        givenPortlets(true, false);
        givenStoredKey();
        final LanguageAjax languageAjax = new LanguageAjax();

        assertEquals("greeting", languageAjax.getLanguageKeys("en").get(0).getKey());
        assertEquals("greeting", languageAjax.getLanguageKeys("en", "US").get(0).getKey());
    }

    /**
     * Given: a back-end user with the Locales portlet only
     * When: getPaginatedLanguageKeys is called, with and without a country and filter
     * Then: the counters row is followed by the stored key
     */
    @Test
    void getPaginatedLanguageKeys_allowsLocalesPortlet() throws Exception {
        givenPortlets(false, true);
        givenStoredKey();
        final LanguageAjax languageAjax = new LanguageAjax();

        final List<Map<String, Object>> page = languageAjax.getPaginatedLanguageKeys("en", 1);
        assertEquals(1L, page.get(0).get("total"));
        assertEquals("greeting", page.get(1).get("key"));

        final List<Map<String, Object>> filtered =
                languageAjax.getPaginatedLanguageKeys("en", "US", 1, "greet");
        assertEquals(1L, filtered.get(0).get("total"));
        assertEquals("greeting", filtered.get(1).get("key"));
    }
}
