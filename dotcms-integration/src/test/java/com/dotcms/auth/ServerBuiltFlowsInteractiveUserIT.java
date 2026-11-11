package com.dotcms.auth;

import static com.dotcms.rendering.velocity.directive.ParseContainer.getDotParserContainerUUID;
import static com.dotmarketing.util.Constants.USER_AGENT_DOTCMS_SITESEARCH;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Server-built flows that act as the built-in account keep working. Each test runs one flow with the
 * built-in account as the acting user and checks what a viewtool inside that flow resolves.
 */
public class ServerBuiltFlowsInteractiveUserIT {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private static String render(final String containerCode) throws Exception {
        final Host site = new SiteDataGen().nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final ContentType blogType = TestDataUtils.getBlogLikeContentType(site);
        final Container container = new ContainerDataGen().withContentType(blogType, containerCode)
                .nextPersisted();
        ContainerDataGen.publish(container);
        final String uuid = UUIDGenerator.generateUuid();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid).nextPersisted();
        TemplateDataGen.publish(template);
        final HTMLPageAsset page = new HTMLPageDataGen(site, template)
                .pageURL("interactive-user-page").title("interactive-user-page").nextPersisted();
        HTMLPageDataGen.publish(page);
        final Contentlet content = new ContentletDataGen(blogType.id())
                .languageId(language.getId()).setProperty("body", "myBody").nextPersisted();
        ContentletDataGen.publish(content);
        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(page.getIdentifier(),
                container.getIdentifier(), content.getIdentifier(), getDotParserContainerUUID(uuid), 0));
        return APILocator.getHTMLPageAssetAPI().getHTML(page.getURI(), site, true, content.getIdentifier(),
                APILocator.systemUser(), content.getLanguageId(), USER_AGENT_DOTCMS_SITESEARCH);
    }

    /** Harness check: a plain container renders, so an empty result below points at the viewtool. */
    @Test
    public void page_render_harness_renders_plain_container_code() throws Exception {
        final String html = render("${body}[[plain]]");
        assertNotNull(html);
        assertTrue("plain render failed: " + html, html.contains("[[plain]]"));
    }

    /**
     * Page render: a viewtool in the page resolves the user from the request the render builds, so it must
     * see the built-in account the render was asked to run as.
     */
    @Test
    public void page_render_resolves_the_built_in_account_in_viewtools() throws Exception {
        final String html = render("${body}[[user:$cmsuser.getLoggedInUser($request).getUserId()]]");
        assertNotNull("render returned nothing", html);
        assertTrue("viewtool did not resolve the built-in account: " + html,
                html.contains("[[user:" + UserAPI.SYSTEM_USER_ID + "]]"));
    }
}
