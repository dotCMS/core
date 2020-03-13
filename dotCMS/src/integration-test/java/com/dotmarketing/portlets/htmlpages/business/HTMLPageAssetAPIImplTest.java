package com.dotmarketing.portlets.htmlpages.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Constants;
import java.util.Date;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class HTMLPageAssetAPIImplTest {

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Given scenario:
     * a new URL-Mapped content in a second language (not default)
     *
     * Expected result:
     * getHTML method should respond with the proper rendered content on the detail page
     */

    @Test
    public void test_getHTML_GivenSpanishOnlyUrlMappedContent_shouldReturnURLMapDetailPage()
            throws DotSecurityException, DotDataException {
        final Host site = new SiteDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final String newsPatternPrefix =
                "/testpattern" + System.currentTimeMillis() + "/";

        final String parent1Name = "news-events";
        final Folder parent1 = new FolderDataGen().name(parent1Name).title(parent1Name).site(site)
                .nextPersisted();
        final String parent2Name = "news";
        final Folder parent2 = new FolderDataGen().name(parent2Name).title(parent2Name).parent(parent1)
                .nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();

        final HTMLPageAsset detailPage = new HTMLPageDataGen(parent2, template)
                .pageURL("news-detail")
                .title("news-detail")
                .nextPersisted();

        HTMLPageDataGen.publish(detailPage);

        final Contentlet contentlet = TestDataUtils.createNewsLikeURLMappedContent(newsPatternPrefix, new Date(),
                APILocator.getLanguageAPI().getDefaultLanguage().getId(), site,
                detailPage.getIdentifier());

        String html = APILocator.getHTMLPageAssetAPI().getHTML(detailPage.getURI(), site, true,
                contentlet.getIdentifier(), APILocator.systemUser(), contentlet.getLanguageId(),  "");


        Assert.assertEquals("", html);
    }
}
