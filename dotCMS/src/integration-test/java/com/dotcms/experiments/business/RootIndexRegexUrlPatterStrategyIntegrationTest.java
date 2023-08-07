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

public class RootIndexRegexUrlPatterStrategyIntegrationTest {

    private static String EXPECTED_REGEX = "^(http|https):\\/\\/(localhost|127.0.0.1|\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,})(:\\d{1,5})?(\\/index|\\/)?(\\/?\\?.*)?$";
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)}
     * When: The page is the root index page
     * Should: the {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} return true
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

        assertEquals(EXPECTED_REGEX, rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset));

        final String regexPattern = rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset);

        assertFalse(("http://localhost:8080/any_folder/index").matches(regexPattern));
        assertFalse(("http://localhost/any_folder/index").matches(regexPattern));

        assertTrue(("http://localhost:8080/index").matches(regexPattern));
        assertTrue(("http://localhost/index").matches(regexPattern));

        assertFalse(("http://localhost:8080/index/index").matches(regexPattern));
        assertFalse(("http://localhost/index/index").matches(regexPattern));

        assertTrue(("http://localhost:8080/").matches(regexPattern));
        assertTrue(("http://localhost/").matches(regexPattern));

        assertFalse(("http://localhost:8080/aaaa/bbb").matches(regexPattern));
        assertFalse(("http://localhost:8080/aaaa").matches(regexPattern));

        assertFalse(("http://localhost/aaaa/bbb").matches(regexPattern));
        assertFalse(("http://localhost/aaaa").matches(regexPattern));

        assertFalse(("http://localhost/xedni").matches(regexPattern));

        assertFalse(("http://localhost:8080/indexindex").matches(regexPattern));
        assertFalse(("http://localhost/indexindex").matches(regexPattern));

        assertTrue(("http://localhost:8080").matches(regexPattern));
        assertTrue(("http://localhost").matches(regexPattern));
        assertTrue(("http://localhost").matches(regexPattern));

        assertFalse(("http://localhost:8080/blog/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));
        assertFalse(("http://localhost/blog/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));

        assertTrue(("http://localhost:8080/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));
        assertTrue(("http://localhost/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));
    }

    /**
     * Method to test: {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} and
     * {@link RootIndexRegexUrlPatterStrategy#getRegexPattern(HTMLPageAsset)}
     * When: The page is the root index page and the protocol is https
     * Should: the {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} return true
     * and the {@link RootIndexRegexUrlPatterStrategy#getRegexPattern(HTMLPageAsset)} return the correct regex pattern
     * also the regex pattern should match the page url
     *
     * @throws DotDataException
     */
    @Test
    public void httpsRealRootIndexPage() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .pageURL("index")
                .nextPersisted();

        final RootIndexRegexUrlPatterStrategy rootIndexRegexUrlPatterStrategy = new RootIndexRegexUrlPatterStrategy();
        final boolean match = rootIndexRegexUrlPatterStrategy.isMatch(htmlPageAsset);
        assertTrue(match);

        assertEquals(EXPECTED_REGEX, rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset));

        final String regexPattern = rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset);
        assertTrue(("https://localhost:8080/index").matches(regexPattern));
        assertTrue(("https://localhost/index").matches(regexPattern));

        assertFalse(("https://localhost:8080/index/index").matches(regexPattern));
        assertFalse(("https://localhost/index/index").matches(regexPattern));

        assertTrue(("https://localhost:8080/").matches(regexPattern));
        assertTrue(("https://localhost/").matches(regexPattern));

        assertFalse(("https://localhost:8080/aaaa/bbb").matches(regexPattern));
        assertFalse(("https://localhost:8080/aaaa").matches(regexPattern));

        assertFalse(("https://localhost/aaaa/bbb").matches(regexPattern));
        assertFalse(("https://localhost/aaaa").matches(regexPattern));

        assertFalse(("https://localhost/xedni").matches(regexPattern));

        assertFalse(("https://localhost:8080/indexindex").matches(regexPattern));
        assertFalse(("https://localhost/indexindex").matches(regexPattern));

        assertTrue(("https://localhost:8080").matches(regexPattern));
        assertTrue(("https://localhost").matches(regexPattern));
        assertTrue(("https://localhost").matches(regexPattern));

        assertFalse(("https://localhost:8080/blog/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));
        assertFalse(("https://localhost/blog/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));

        assertTrue(("https://localhost:8080/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));
        assertTrue(("https://localhost/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));
    }


    /**
     * Method to test: {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)}
     * When: The page is the root index page and the domain is not localhost
     * Should: the {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} return true
     *
     * @throws DotDataException
     */
    @Test
    public void realRootIndexPageNotLocalHost() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .pageURL("index")
                .nextPersisted();

        final RootIndexRegexUrlPatterStrategy rootIndexRegexUrlPatterStrategy = new RootIndexRegexUrlPatterStrategy();
        final boolean match = rootIndexRegexUrlPatterStrategy.isMatch(htmlPageAsset);
        assertTrue(match);

        assertEquals(EXPECTED_REGEX, rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset));

        final String regexPattern = rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset);

        assertFalse(("http://demo.dotcms.com:8080/any_folder/index").matches(regexPattern));
        assertFalse(("http://demo.dotcms.com/any_folder/index").matches(regexPattern));

        assertTrue(("http://demo.dotcms.com:8080/index").matches(regexPattern));
        assertTrue(("http://demo.dotcms.com/index").matches(regexPattern));

        assertFalse(("http://demo.dotcms.com:8080/index/index").matches(regexPattern));
        assertFalse(("http://demo.dotcms.com/index/index").matches(regexPattern));

        assertTrue(("http://demo.dotcms.com:8080/").matches(regexPattern));
        assertTrue(("http://demo.dotcms.com/").matches(regexPattern));

        assertFalse(("http://demo.dotcms.com:8080/aaaa/bbb").matches(regexPattern));
        assertFalse(("http://demo.dotcms.com:8080/aaaa").matches(regexPattern));

        assertFalse(("http://demo.dotcms.com/aaaa/bbb").matches(regexPattern));
        assertFalse(("http://demo.dotcms.com/aaaa").matches(regexPattern));

        assertFalse(("http://demo.dotcms.com/xedni").matches(regexPattern));

        assertFalse(("http://demo.dotcms.com:8080/indexindex").matches(regexPattern));
        assertFalse(("http://demo.dotcms.com/indexindex").matches(regexPattern));

        assertTrue(("http://demo.dotcms.com:8080").matches(regexPattern));
        assertTrue(("http://demo.dotcms.com").matches(regexPattern));
        assertTrue(("http://demo.dotcms.com").matches(regexPattern));

        assertFalse(("http://demo.dotcms.com:8080/blog/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));
        assertFalse(("http://demo.dotcms.com/blog/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));

        assertTrue(("http://demo.dotcms.com:8080/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));
        assertTrue(("http://demo.dotcms.com/?variantName=dotexperiment-fcaef4575a-variant-1&redirect=true").matches(regexPattern));
    }

    /**
     * Method to test: {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} and
     * {@link RootIndexRegexUrlPatterStrategy#getRegexPattern(HTMLPageAsset)}
     * When: The page is the root index page and the protocol is https and the domain is not localhost
     * Should: the {@link RootIndexRegexUrlPatterStrategy#isMatch(HTMLPageAsset)} return true
     * and the {@link RootIndexRegexUrlPatterStrategy#getRegexPattern(HTMLPageAsset)} return the correct regex pattern
     * also the regex pattern should match the page url
     *
     * @throws DotDataException
     */
    @Test
    public void httpsRealRootIndexPageNotLocalhost() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .pageURL("index")
                .nextPersisted();

        final RootIndexRegexUrlPatterStrategy rootIndexRegexUrlPatterStrategy = new RootIndexRegexUrlPatterStrategy();
        final boolean match = rootIndexRegexUrlPatterStrategy.isMatch(htmlPageAsset);
        assertTrue(match);

        assertEquals(EXPECTED_REGEX, rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset));

        final String regexPattern = rootIndexRegexUrlPatterStrategy.getRegexPattern(htmlPageAsset);
        assertTrue(("https://demo.dotcms.com:8080/index").matches(regexPattern));
        assertTrue(("https://demo.dotcms.com/index").matches(regexPattern));

        assertFalse(("https://demo.dotcms.com:8080/index/index").matches(regexPattern));
        assertFalse(("https://demo.dotcms.com/index/index").matches(regexPattern));

        assertTrue(("https://demo.dotcms.com:8080/").matches(regexPattern));
        assertTrue(("https://demo.dotcms.com/").matches(regexPattern));

        assertFalse(("https://demo.dotcms.com:8080/aaaa/bbb").matches(regexPattern));
        assertFalse(("https://demo.dotcms.com:8080/aaaa").matches(regexPattern));

        assertFalse(("https://demo.dotcms.com/aaaa/bbb").matches(regexPattern));
        assertFalse(("https://demo.dotcms.com/aaaa").matches(regexPattern));

        assertFalse(("https://demo.dotcms.com/xedni").matches(regexPattern));

        assertFalse(("https://demo.dotcms.com:8080/indexindex").matches(regexPattern));
        assertFalse(("https://demo.dotcms.com/indexindex").matches(regexPattern));

        assertTrue(("https://demo.dotcms.com:8080").matches(regexPattern));
        assertTrue(("https://demo.dotcms.com").matches(regexPattern));
        assertTrue(("https://demo.dotcms.com").matches(regexPattern));
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
    public void notIndexPage() throws DotDataException {
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
    public void indexPageButNotRootIndexPage() throws DotDataException {
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
