package com.dotcms.rest.api.v2.languages;


import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LanguageVariableDataGen;
import com.dotcms.languagevariable.business.LanguageVariableAPITest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageVariable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class LanguageVariablesHelperTest {

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
        LanguageVariableAPITest.prepare();
    }

    @Test
    public void simplePaginationTest()
            throws DotDataException, DotSecurityException, JsonProcessingException {

        cleanup();

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

        ObjectMapper mapper = new ObjectMapper();
        final String asString = mapper.writeValueAsString(view);

        System.out.println(asString);

    }

    void cleanup() throws DotDataException, DotSecurityException {
        final User user = APILocator.systemUser();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final List<LanguageVariable> allVariables = APILocator.getLanguageVariableAPI().findAllVariables();
        for (LanguageVariable languageVariable : allVariables) {
            final Contentlet cont = (Contentlet) languageVariable;
            contentletAPI.archive(cont, user,false );
            contentletAPI.delete(cont, user,false);
        }
    }

}
