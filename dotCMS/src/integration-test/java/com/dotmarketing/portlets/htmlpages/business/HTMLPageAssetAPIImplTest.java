package com.dotmarketing.portlets.htmlpages.business;

import static com.dotcms.rendering.velocity.directive.ParseContainer.getDotParserContainerUUID;
import static com.dotmarketing.util.Constants.USER_AGENT_DOTCMS_SITESEARCH;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.UUIDGenerator;
import java.util.Collections;
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
     * a new content in a second language (not default) belonging to a htmlpage
     *
     * Expected result:
     * getHTML method should respond with the proper rendered content on the belonging htmlpage
     */

    @Test
    public void test_getHTML_GivenSpanishOnlyContent_shouldReturnHTML()
            throws DotSecurityException, DotDataException, WebAssetException {
        final Host site = new SiteDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();

        final ContentType blogType = TestDataUtils.getBlogLikeContentType(site);

        // create detail page
        final Container container = new ContainerDataGen()
                .withContentType(blogType, "$body")
                .nextPersisted();

        ContainerDataGen.publish(container);

        final String uuid = UUIDGenerator.generateUuid();

        final Template template = new TemplateDataGen().withContainer(container.getIdentifier(), uuid)
                .nextPersisted();

        TemplateDataGen.publish(template);

        final HTMLPageAsset htmlPage = new HTMLPageDataGen(site, template)
                .pageURL("blog-detail")
                .title("blog-detail")
                .nextPersisted();

        HTMLPageDataGen.publish(htmlPage);

        // create URL-Mapped content
        final Contentlet urlMappedContent = new ContentletDataGen(blogType.id())
                .languageId(language.getId())
                .setProperty("body", "myBody")
                .nextPersisted();

        ContentletDataGen.publish(urlMappedContent);

        final MultiTree multiTree = new MultiTree(htmlPage.getIdentifier(),
                container.getIdentifier(),
                urlMappedContent.getIdentifier(),
                getDotParserContainerUUID(uuid), 0);

        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        String html = APILocator.getHTMLPageAssetAPI().getHTML(htmlPage.getURI(), site, true,
                urlMappedContent.getIdentifier(), APILocator.systemUser(),
                urlMappedContent.getLanguageId(),  USER_AGENT_DOTCMS_SITESEARCH);


        Assert.assertEquals("<div>myBody</div>", html);
    }
}
