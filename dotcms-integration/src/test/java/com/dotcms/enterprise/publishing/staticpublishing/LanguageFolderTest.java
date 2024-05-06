package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UUIDGenerator;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class LanguageFolderTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] dataProvider() {
        final LanguageFolder languageFolderDefaultLang = new LanguageFolder(new File(
                UUIDGenerator.shorty() + "test.png"),new Language(1));
        final LanguageFolder languageFolderSpanishLang = new LanguageFolder(new File(
                UUIDGenerator.shorty() + "test.png"),new Language(2));
        final LanguageFolder languageFolderItalianLang = new LanguageFolder(new File(
                UUIDGenerator.shorty() + "test.png"),new Language(3));
        return new Object[] {
                new TestCase(languageFolderDefaultLang, languageFolderDefaultLang,0),
                new TestCase(languageFolderDefaultLang, languageFolderSpanishLang,1),
                new TestCase(languageFolderSpanishLang, languageFolderDefaultLang,-1),
                new TestCase(languageFolderSpanishLang, languageFolderItalianLang,
                        (int) (languageFolderSpanishLang.getLanguage().getId()-languageFolderItalianLang.getLanguage().getId())),
                new TestCase(languageFolderItalianLang, languageFolderSpanishLang,
                        (int) (languageFolderItalianLang.getLanguage().getId()-languageFolderSpanishLang.getLanguage().getId())),
        };
    }

    private static class TestCase {
        LanguageFolder languageFolder1;
        LanguageFolder languageFolder2;
        int expectedResult;

        public TestCase(final LanguageFolder languageFolder1, final LanguageFolder languageFolder2, final int expectedResult) {
            this.languageFolder1 = languageFolder1;
            this.languageFolder2 = languageFolder2;
            this.expectedResult = expectedResult;
        }
    }

    @Test
    @UseDataProvider("dataProvider")
    public void testCompareTo(final TestCase testCase) {
        final LanguageFolder languageFolder1 = testCase.languageFolder1;
        final LanguageFolder languageFolder2  = testCase.languageFolder2;
        Assert.assertEquals(testCase.expectedResult,languageFolder1.compareTo(languageFolder2));
    }

}