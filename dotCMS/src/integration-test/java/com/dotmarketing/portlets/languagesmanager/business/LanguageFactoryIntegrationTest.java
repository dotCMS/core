package com.dotmarketing.portlets.languagesmanager.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Config;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LanguageFactoryIntegrationTest extends IntegrationTestBase {

    private static LanguageFactory languageFactory;

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();
        when(Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY))
                .thenReturn(new MultiMessageResources(MultiMessageResourcesFactory.createFactory(),""));

        languageFactory = FactoryLocator.getLanguageFactory();
    }

    @AfterClass
    public static void cleanup() throws Exception {
    }

    @Test
    public void test_get_languages(){
         final List<Language> languages = languageFactory.getLanguages();
         assertTrue(languages.size() >= 2);
    }

    @Test
    public void test_get_default_language(){
        final Language language = languageFactory.getDefaultLanguage();
        assertEquals(language.getLanguageCode(), "en");
        assertEquals(language.getLanguage(), "English");
    }

    @Test
    public void test_insert_language_then_find_then_remove() {

        final Language danish = new Language(0, "da", "DK ", "Danish", "Denmark");
        languageFactory.saveLanguage(danish);
        Language found = languageFactory
                .getLanguage(danish.getLanguageCode(), danish.getCountryCode());
        assertNotNull(found);
    }

    @Test
    public void test_insert_language_then_test_has_language_then_remove() {

        final Language danish = new Language(0, "da", "DK ", "Danish", "Denmark");
        languageFactory.saveLanguage(danish);
        Language found = languageFactory
                .getLanguage(danish.getLanguageCode(), danish.getCountryCode());
        assertNotNull(found);

        assertTrue(languageFactory.hasLanguage(found.getId()));
        assertTrue(languageFactory.hasLanguage(found.getId() + ""));
        assertTrue(languageFactory.hasLanguage(found.getLanguageCode(), found.getCountryCode()));
    }

    @Test
    public void test_insert_language_then_update() throws Exception {

        final String countryName = "Deutschland";

        final Language german = new Language(0, "de", "DE", "German", "Germany");
        languageFactory.saveLanguage(german);
        Language found = languageFactory
                .getLanguage(german.getLanguageCode(), german.getCountryCode());

        found.setCountry(countryName);

        languageFactory.saveLanguage(found);

        found = languageFactory.getLanguage(found.getId());

        assertEquals(countryName, found.getCountry());
    }

    @Test
    public void test_insert_expect_id_greater_than_0_then_delete_then_expect_null() throws Exception {

        long currentMillis = System.currentTimeMillis();

        Language found = null;
        try {
            final Language german = new Language(0, "de", "DE" + currentMillis, "German",
                    "Germany");
            languageFactory.saveLanguage(german);
            assertNotEquals("An id different from 0 should have been returned",0, german.getId());
            found = languageFactory.getLanguage(german.getLanguageCode(), german.getCountryCode());
        } finally {
            if (null != found) {
                languageFactory.deleteLanguage(found);
                assertNull(languageFactory.getLanguage(found.getLanguageCode(), found.getCountryCode()));
            }
        }
    }


    @Test
    public void test_insert_duplicate_then_find_by_code() throws Exception {

        long currentMillis = System.currentTimeMillis();

        Language german1 = new Language(0, "de", "DE" + currentMillis, "German",
                "Germany");
        languageFactory.saveLanguage(german1);

        Language german2 = new Language(0, "de", "DE" + currentMillis, "German",
                "Germany");
        languageFactory.saveLanguage(german2);

        final Language lang = languageFactory
                .getLanguage("de", "DE" );
        assertTrue(lang.getId() == german1.getId() || lang.getId() == german2.getId());
    }

    @Test
    public void test_insert_force_id_expect_new_id_to_match() throws Exception {
        final long newId = System.currentTimeMillis();

        Language russian = new Language(newId, "ru", "RUS", "Russian", "Russia");
        languageFactory.saveLanguage(russian);

        assertTrue(russian.getId() > 0);

        assertEquals("We expected the new record to have a value of " + Math.abs(russian.toString().hashCode()), Math.abs(russian.toString().hashCode()),
                russian.getId());

        assertTrue(languageFactory.hasLanguage(russian.getId()));
    }

    @Test
    public void test_insert_language_then_insert_language_keys_then_find_keys() throws Exception {

        final Language german = new Language(0, "de", "DE", "German", "Germany");
        languageFactory.saveLanguage(german);
        Language found = languageFactory
                .getLanguage(german.getLanguageCode(), german.getCountryCode());
        final Map<String, String> generalKeys = ImmutableMap.of("a", "a", "b", "b", "c", "c");
        //final Map<String, String> specificKeys = ImmutableMap.of("a", "a1", "b", "b1", "c", "c1");
        final Map<String, String> emptyKeys = new HashMap<>();
        final Set<String> emptySet = new HashSet<>();
        languageFactory.saveLanguageKeys(found, generalKeys, emptyKeys, emptySet);
        final List<LanguageKey> keys1 = languageFactory.getLanguageKeys(found.getLanguageCode());
        assertFalse("We expected 3 keys", keys1.isEmpty());

        final Map<String, String> mappedGeneral = keys1.stream().collect(
                Collectors.toMap(LanguageKey::getKey, LanguageKey::getValue));

        Assert.assertEquals(generalKeys, mappedGeneral);
    }

}
