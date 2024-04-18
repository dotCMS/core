package com.dotcms.rest.api.v2.languages;


import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LanguageVariableDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class LanguageVariablesHelperTest {

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    void simplePaginationTest() throws DotDataException {

        LanguageDataGen languageDataGen = new LanguageDataGen();
        final List<Language> languages = List.of(
                languageDataGen.nextPersisted(),
                languageDataGen.nextPersisted(),
                languageDataGen.nextPersisted()
        );

        List<Contentlet> contentlets = new ArrayList<>();
        LanguageVariableDataGen languageVariableDataGen = new LanguageVariableDataGen();
        //Contentlets are created and published
        for (Language language : languages) {
            contentlets.add(languageVariableDataGen.languageId(language.getId()).key("key1").value("value1").nextPersistedAndPublish());
            contentlets.add(languageVariableDataGen.languageId(language.getId()).key("key2").value("value2").nextPersistedAndPublish());
            contentlets.add(languageVariableDataGen.languageId(language.getId()).key("key3").value("value3").nextPersistedAndPublish());
        }

        final LanguageVariablesHelper helper = new LanguageVariablesHelper();
        final LanguageVariablePageView view = helper.view(
                new PaginationContext(null, 0, 10, null, null), null);

        view.variables().forEach((key, value) -> {

            value.forEach((lang, langVar) -> {
                System.out.println("Lang: " + lang + " Value: " + langVar.value());
            });
        });

    }

}
