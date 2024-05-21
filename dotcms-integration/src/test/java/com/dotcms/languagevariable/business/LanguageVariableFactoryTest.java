package com.dotcms.languagevariable.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LanguageVariableDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link LanguageVariableFactoryImpl}.
 */
public class LanguageVariableFactoryTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LanguageVariableAPITest.prepare();
    }

    /**
     * Method to test {@link LanguageVariableFactoryImpl#findVariables(ContentType, long, int, int, String)} method.
     * Given scenario: Look up for language variables content-type then create two instances on is published and the other is working.
     * Expected Result: The factory should return a list of language variables.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void simpleFindVariablesTest() throws DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final LanguageVariableDataGen dataGen = new LanguageVariableDataGen();
        //Create some content
        final Contentlet live = dataGen.languageId(language.getId())
                .key("key")
                .value("value")
                .nextPersistedAndPublish();

        final Contentlet working = dataGen.languageId(language.getId())
                .key("key")
                .value("value")
                .nextPersisted();

        final LanguageVariableFactory languageVariableFactory = new LanguageVariableFactoryImpl();
        final List<LanguageVariable> variables = languageVariableFactory.findVariables(LanguageVariableDataGen.langVarContentType.get(),
                language.getId(), 0, 10, null);
        //Assert the list is not empty and at least two variables are in the list (cause there can be more from other tests
        Assert.assertTrue(variables.size() >= 2 );
        for (LanguageVariable variable : variables) {
            Assert.assertNotNull(variable.key());
            Assert.assertNotNull(variable.value());
            Assert.assertNotNull(variable.identifier());
        }
        //Verify both live and working are in the list
        Assert.assertTrue(variables.stream().anyMatch((variable) -> (variable.identifier().equals(live.getIdentifier()))));
        Assert.assertTrue(variables.stream().anyMatch((variable) -> (variable.identifier().equals(working.getIdentifier()))));
    }

    /**
     * Method to test {@link LanguageVariableFactoryImpl#findVariablesForPagination(ContentType, int, int, String)} method.
     * Given scenario: Look up for language variables content-type then create 20 instances on is published and the other is working.
     * Expected Result: The factory should return a list of language variables paginated.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void simpleFindVariablesForPaginationTest() throws DotDataException, DotSecurityException {
        final Language language = new LanguageDataGen().nextPersisted();
        final LanguageVariableDataGen dataGen = new LanguageVariableDataGen();
        //Create some content\
        for(int i = 0; i < 20; i++) {
            dataGen.languageId(language.getId())
                    .key( "paginated-key" + i)
                    .value( "paginated-value" + i)
                    .nextPersistedAndPublish();

            dataGen.languageId(language.getId())
                    .key("paginated-key" + i)
                    .value("paginated-value" + i)
                    .nextPersisted();
        }

        final LanguageVariableFactory languageVariableFactory = new LanguageVariableFactoryImpl();

        for (int i = 0; i < 20; i += 5) {
            final List<LanguageVariableExt> variables = languageVariableFactory.findVariablesForPagination(LanguageVariableDataGen.langVarContentType.get(), i, 5, null);
            //Assert the list is not empty and at least two variables are in the list (cause there can be more from other tests
            Assert.assertEquals(5, variables.size());
            for (LanguageVariableExt variable : variables) {
                Assert.assertNotNull(variable.key());
                Assert.assertNotNull(variable.value());
                Assert.assertNotNull(variable.identifier());
                Assert.assertTrue(variable.languageId() > 0);
            }
        }
    }

}
