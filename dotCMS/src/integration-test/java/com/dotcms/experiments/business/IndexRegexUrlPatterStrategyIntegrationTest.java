package com.dotcms.experiments.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import org.junit.BeforeClass;
import org.junit.Test;

public class IndexRegexUrlPatterStrategyIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} and
     * {@link RootIndexRegexUrlPatterStrategy#getRegexPattern(HTMLPageAsset)}
     * When: The page is the root index page
     * Should: the {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} return true
     * and the {@link RootIndexRegexUrlPatterStrategy#getRegexPattern(HTMLPageAsset)} return the correct regex pattern
     * also the regex pattern should match the page url
     *
     * @throws DotDataException
     */
    @Test
    public void realRootIndexPage() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .pageURL("index")
                .nextPersisted();

        final RootIndexRegexUrlPatterStrategy rootIndexRegexUrlPatterStrategy = new RootIndexRegexUrlPatterStrategy();
        final boolean match = rootIndexRegexUrlPatterStrategy.isMatch(htmlPageAsset);
        assertTrue(match);

        assertEquals("(http|https):\\/\\/.*:[0-9]*(\\/index|\\/)?(\\?.*)?",
                rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset));

        final String regexPattern = rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset);
        assertTrue(("http://localhost:8080" + htmlPageAsset.getURI()).matches(regexPattern));
    }

    /**
     * Method to test: {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} and
     * {@link RootIndexRegexUrlPatterStrategy#getRegexPattern(HTMLPageAsset)}
     * When: The page is not an index page
     * Should: the {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} return false
     * and the regex pattern should not match the page url
     *
     * @throws DotDataException
     */
    @Test
    public void bbb() throws DotDataException {
            final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(folder, template)
                .nextPersisted();

        final RootIndexRegexUrlPatterStrategy rootIndexRegexUrlPatterStrategy = new RootIndexRegexUrlPatterStrategy();
        final boolean match = rootIndexRegexUrlPatterStrategy.isMatch(htmlPageAsset);
        assertFalse(match);

        final String regexPattern = rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset);
        assertFalse(("http://localhost:8080" + htmlPageAsset.getURI()).matches(regexPattern));
    }

    /**
     * Method to test: {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} and
     * {@link RootIndexRegexUrlPatterStrategy#getRegexPattern(HTMLPageAsset)}
     * When: The page is an index page but not the root index page
     * Should: the {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} return false
     * and the regex pattern should not match the page url
     *
     * @throws DotDataException
     */
    @Test
    public void ccc() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(folder, template)
                .pageURL("index")
                .nextPersisted();

        final RootIndexRegexUrlPatterStrategy rootIndexRegexUrlPatterStrategy = new RootIndexRegexUrlPatterStrategy();
        final boolean match = rootIndexRegexUrlPatterStrategy.isMatch(htmlPageAsset);
        assertFalse(match);

        final String regexPattern = rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset);
        assertFalse(("http://localhost:8080" + htmlPageAsset.getURI()).matches(regexPattern));
    }
}
