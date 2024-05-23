package com.dotcms.languagevariable.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LanguageVariableDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
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

    /**
     * Methods to test {@link LanguageVariableFactoryImpl#countVariablesByKey(ContentType)} and {@link LanguageVariableFactoryImpl#countVariablesByKey(ContentType, long).
     * Given scenario: This is a simple excercise to count the number of language variables with a specific key.
     * We test the count for all languages and for each language.
     * Expected Result: The factory should return the correct count of language variables.
     */
    @Test
    public void simpleLanguageCountTest() throws DotDataException {

        final LanguageVariableFactory factory = new LanguageVariableFactoryImpl();
        final int allVarsCountBefore = factory.countVariablesByKey(
                LanguageVariableDataGen.langVarContentType.get());
        Assert.assertTrue(allVarsCountBefore >= 0);

        final Language language1 = new LanguageDataGen().nextPersisted();
        new LanguageVariableDataGen().languageId(language1.getId())
                .key("key1" + System.currentTimeMillis()).value("value1").nextPersistedAndPublish();

        final Language language2 = new LanguageDataGen().nextPersisted();
        new LanguageVariableDataGen().languageId(language2.getId())
                .key("key1" + System.currentTimeMillis()).value("value1").nextPersistedAndPublish();
        new LanguageVariableDataGen().languageId(language2.getId())
                .key("key2" + System.currentTimeMillis()).value("value2").nextPersistedAndPublish();

        final Language language3 = new LanguageDataGen().nextPersisted();
        new LanguageVariableDataGen().languageId(language3.getId())
                .key("key1" + System.currentTimeMillis()).value("value1").nextPersistedAndPublish();
        new LanguageVariableDataGen().languageId(language3.getId())
                .key("key2" + System.currentTimeMillis()).value("value2").nextPersistedAndPublish();
        new LanguageVariableDataGen().languageId(language3.getId())
                .key("key3" + System.currentTimeMillis()).value("value3").nextPersistedAndPublish();

        final int allVarsCountAfter = factory.countVariablesByKey(
                LanguageVariableDataGen.langVarContentType.get());
        Assert.assertEquals(allVarsCountBefore + 6, allVarsCountAfter);

        final int lang1Count = factory.countVariablesByKey(
                LanguageVariableDataGen.langVarContentType.get(), language1.getId());
        Assert.assertEquals(1, lang1Count);

        final int lang2Count = factory.countVariablesByKey(
                LanguageVariableDataGen.langVarContentType.get(), language2.getId());
        Assert.assertEquals(2, lang2Count);

        final int lang3Count = factory.countVariablesByKey(
                LanguageVariableDataGen.langVarContentType.get(), language3.getId());
        Assert.assertEquals(3, lang3Count);

    }

}
