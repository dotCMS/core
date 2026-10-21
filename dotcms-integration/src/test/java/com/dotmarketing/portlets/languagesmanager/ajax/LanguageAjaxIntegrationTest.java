package com.dotmarketing.portlets.languagesmanager.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.ajax.DwrWebContextRule;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Integration tests for {@link LanguageAjax} with real roles and layouts: only a user with the
 * Languages or the Locales portlet can save or read language keys.
 */
public class LanguageAjaxIntegrationTest {

    private static final String LANGUAGES_PORTLET = "languages";
    private static final String LOCALES_PORTLET = "locales";
    private static final String VALUE = "Saved from the language keys editor";

    @ClassRule
    public static final DwrWebContextRule dwrContext = new DwrWebContextRule();

    private static LanguageAPI languageAPI;
    private static Language language;
    private static Layout languagesLayout;
    private static User userWithoutPortlet;
    private static User languagesPortletUser;

    private final String key = "integration.language.key." + System.nanoTime();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        languageAPI = APILocator.getLanguageAPI();
        language = languageAPI.getDefaultLanguage();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();

        userWithoutPortlet = backendUser("language-no-portlet");
        languagesPortletUser = backendUser("language-portlet");
        languagesLayout = new LayoutDataGen()
                .name("languages-" + System.nanoTime())
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
     * Method to test: {@link LanguageAjax#saveKeys(String, String, List, List, List)}
     * Given: a back-end user with neither the Languages nor the Locales portlet
     * Expected: DotSecurityException, and the key is not written
     */
    @Test
    public void saveKeys_requiresLanguagesOrLocalesPortlet() {
        dwrContext.loginAs(userWithoutPortlet);

        assertThrows(DotSecurityException.class, () -> new LanguageAjax().saveKeys(
                language.getLanguageCode(), language.getCountryCode(), keysToAdd(),
                Collections.emptyList(), Collections.emptyList()));

        assertFalse(storedValue().isPresent());
    }

    /**
     * Method to test: {@link LanguageAjax#saveKeys(String, String, List, List, List)}
     * Given: a back-end user with the Languages portlet
     * Expected: the key is written with the posted value
     */
    @Test
    public void saveKeys_allowsLanguagesPortlet() throws Exception {
        dwrContext.loginAs(languagesPortletUser);

        new LanguageAjax().saveKeys(language.getLanguageCode(), language.getCountryCode(), keysToAdd(),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(Optional.of(VALUE), storedValue());
    }

    /**
     * Method to test: {@link LanguageAjax#getPaginatedLanguageKeys(String, String, int, String)},
     * the call the language keys page makes
     * Given: a back-end user with neither the Languages nor the Locales portlet
     * Expected: DotSecurityException
     */
    @Test
    public void getPaginatedLanguageKeys_requiresLanguagesOrLocalesPortlet() {
        dwrContext.loginAs(userWithoutPortlet);

        assertThrows(DotSecurityException.class, () -> new LanguageAjax().getPaginatedLanguageKeys(
                language.getLanguageCode(), language.getCountryCode(), 1, ""));
    }

    /**
     * Method to test: {@link LanguageAjax#getPaginatedLanguageKeys(String, String, int, String)},
     * the call the language keys page makes
     * Given: a back-end user with the Languages portlet
     * Expected: the counters row, with at least one key for the default language
     */
    @Test
    public void getPaginatedLanguageKeys_allowsLanguagesPortlet() throws Exception {
        dwrContext.loginAs(languagesPortletUser);

        final List<Map<String, Object>> page = new LanguageAjax().getPaginatedLanguageKeys(
                language.getLanguageCode(), language.getCountryCode(), 1, "");

        assertTrue("the default language must have keys, got " + page.get(0),
                ((Number) page.get(0).get("total")).longValue() > 0);
    }

    private static User backendUser(final String label) throws DotDataException {
        return new UserDataGen()
                .emailAddress(label + System.nanoTime() + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
    }

    /** One country-specific key, in the {@code key<sep>general<sep>specific} format the editor posts. */
    private List<String> keysToAdd() {
        final String separator = WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR;
        return Collections.singletonList(key + separator + separator + VALUE);
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
