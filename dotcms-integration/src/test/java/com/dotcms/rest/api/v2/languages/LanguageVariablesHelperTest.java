package com.dotcms.rest.api.v2.languages;


import static com.dotcms.languagevariable.business.LanguageVariableAPITest.destroyAll;

import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LanguageVariableDataGen;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.languagevariable.business.LanguageVariableAPITest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for LanguageVariablesHelper
 */
public class LanguageVariablesHelperTest {

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
        LanguageVariableAPITest.prepare();
    }

    /**
     * Given scenario: There are 3 languages and 3 language variables for each language
     * Expected result: The keys should be ordered as key1, key2, key3
     * @throws DotDataException if an error occurs
     * @throws DotSecurityException if an error occurs
     * @throws JsonProcessingException if an error occurs
     */
    @Test
    public void paginationTestVerifyKeyOrder()
            throws Exception {

        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();

        destroyAll();
        Assert.assertEquals(0, languageVariableAPI.countVariablesByKey());

        final List<Language> languages = List.of(
                new LanguageDataGen().nextPersisted(),
                new LanguageDataGen().nextPersisted(),
                new LanguageDataGen().nextPersisted()
        );

        LanguageVariableDataGen languageVariableDataGen = new LanguageVariableDataGen();
        //Contentlets are created and published
        for (Language language : languages) {
            languageVariableDataGen.languageId(language.getId()).key("key1").value("value1")
                    .nextPersistedAndPublish();
            languageVariableDataGen.languageId(language.getId()).key("key2").value("value2")
                    .nextPersistedAndPublish();
            languageVariableDataGen.languageId(language.getId()).key("key3").value("value3")
                    .nextPersistedAndPublish();
        }
        Assert.assertEquals("Count should match the number of unique keys ", 3,
                languageVariableAPI.countVariablesByKey());
        final List<String> languageTags = languages.stream().map(Language::getIsoCode)
                .collect(Collectors.toList());

        final LanguageVariablesHelper helper = new LanguageVariablesHelper();
        final LanguageVariablePageView view = helper.view(
                new PaginationContext(null, 0, 10, null, null), false);

        Assert.assertEquals("Total languages does not match ",  3, view.total());
        final LinkedHashMap<String, Map<String, LanguageVariableView>> variables = view.variables();
        //Validate order of the keys

        final AtomicInteger count = new AtomicInteger(1);
        variables.forEach((key, langVarMap) -> {
            //Validate the order of the keys we have to have key1, key2, key3
            Assert.assertEquals("key" + count.getAndIncrement(), key);
            //At least we should have a value for each language
            Assert.assertEquals("The inner map has to have at least 3", 3, langVarMap.size());
            //Each one of those keys should have a value for each language
            languageTags.forEach(languageTag -> {
                Assert.assertTrue(langVarMap.containsKey(languageTag));
            });
        });

    }

}