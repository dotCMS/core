package com.dotcms.contenttype.transform.contenttype;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

/**
 * Integration tests for {@link DetailPageTransformerImpl}.
 *
 * <p>Specifically verifies that {@link DetailPageTransformerImpl#uriToId()} accepts pages of any
 * content type whose base type is {@link BaseContentType#HTMLPAGE}, not just the default
 * {@code htmlpageasset} type.
 */
public class DetailPageTransformerImplTest extends IntegrationTestBase {

    private static User user;
    private static Host host;
    private static Template template;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        user = APILocator.systemUser();
        host = new SiteDataGen().nextPersisted();
        template = new TemplateDataGen().host(host).nextPersisted();
    }

    /**
     * Method to test: {@link DetailPageTransformerImpl#uriToId()}
     * Given: A ContentType whose detailPage is the identifier of a standard {@code htmlpageasset} page
     * When:  {@code uriToId()} is called
     * Should: Return the page identifier (regression guard — must keep working after the fix)
     */
    @Test
    public void uriToId_withStandardHtmlpageasset_returnsIdentifier() throws Exception {
        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();
        final ContentType contentType = new ContentTypeDataGen()
                .detailPage(page.getIdentifier())
                .urlMapPattern("/test-standard-{urlTitle}")
                .next();

        final Optional<String> result = new DetailPageTransformerImpl(contentType, user).uriToId();

        Assert.assertTrue("Expected a non-empty result for standard htmlpageasset", result.isPresent());
        Assert.assertEquals(page.getIdentifier(), result.get());
    }

    /**
     * Method to test: {@link DetailPageTransformerImpl#uriToId()}
     * Given: A ContentType whose detailPage is the identifier of a page using a custom HTMLPAGE content type
     * When:  {@code uriToId()} is called
     * Should: Return the page identifier — custom HTMLPAGE types must be accepted as valid detail pages
     *
     * <p>This is the bug-fix scenario: before the fix, {@code validateIdentifier()} compared
     * {@code assetSubType} against the hardcoded string {@code "htmlpageasset"}, causing any page
     * with a custom content type (e.g. {@code landingPage}) to be rejected with
     * {@link IllegalArgumentException}.
     */
    @Test
    public void uriToId_withCustomPageContentType_returnsIdentifier() throws Exception {
        final long time = System.currentTimeMillis();
        final ContentType customPageType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.HTMLPAGE)
                .host(host)
                .name("CustomPageType_" + time)
                .velocityVarName("customPageType" + time)
                .nextPersisted();

        Contentlet customPage = null;
        try {
            customPage = new ContentletDataGen(customPageType)
                    .host(host)
                    .setProperty(HTMLPageAssetAPI.URL_FIELD, "custom-page-" + time)
                    .setProperty(HTMLPageAssetAPI.TITLE_FIELD, "Custom Page " + time)
                    .setProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, template.getIdentifier())
                    .setProperty(HTMLPageAssetAPI.FRIENDLY_NAME_FIELD, "Custom Page")
                    .setProperty(HTMLPageAssetAPI.CACHE_TTL_FIELD, "0")
                    .nextPersisted();

            final ContentType contentType = new ContentTypeDataGen()
                    .detailPage(customPage.getIdentifier())
                    .urlMapPattern("/test-custom-{urlTitle}")
                    .next();

            final Optional<String> result = new DetailPageTransformerImpl(contentType, user).uriToId();

            Assert.assertTrue("Expected a non-empty result for custom HTMLPAGE content type", result.isPresent());
            Assert.assertEquals(customPage.getIdentifier(), result.get());
        } finally {
            if (customPage != null) {
                APILocator.getContentletAPI().destroy(customPage, user, false);
            }
            ContentTypeDataGen.remove(customPageType);
        }
    }

    /**
     * Method to test: {@link DetailPageTransformerImpl#uriToId()}
     * Given: A ContentType whose detailPage identifier points to a non-page contentlet
     * When:  {@code uriToId()} is called
     * Should: Throw {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void uriToId_withNonPageIdentifier_throwsIllegalArgumentException() throws Exception {
        final ContentType simpleType = new ContentTypeDataGen().nextPersisted();
        Contentlet contentlet = null;
        try {
            contentlet = new ContentletDataGen(simpleType).host(host).nextPersisted();

            final ContentType contentType = new ContentTypeDataGen()
                    .detailPage(contentlet.getIdentifier())
                    .urlMapPattern("/test-nonpage-{urlTitle}")
                    .next();

            new DetailPageTransformerImpl(contentType, user).uriToId();
        } finally {
            if (contentlet != null) {
                APILocator.getContentletAPI().destroy(contentlet, user, false);
            }
            ContentTypeDataGen.remove(simpleType);
        }
    }
}
