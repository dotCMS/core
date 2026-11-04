package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the related content that {@link PageViewSerializer} adds to the page map
 * (for the page and for its URL-mapped content), with real content loaded through the APIs.
 *
 * @author hassandotcms
 */
public class PageViewSerializerRelatedContentTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: createObjectMapUrlContent
     * Given Scenario: the request asks for related content with the {@code depth} request
     * attribute ({@code 0}) and the URL-mapped content, loaded through the Content API, has two
     * related children
     * ExpectedResult: the url content map lists both children, and the contentlet that was
     * passed in has no value for the relationship field
     */
    @Test
    public void createObjectMapUrlContent_withDepth_leavesGivenContentletUnchanged() throws Exception {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request();
        request.setAttribute(WebKeys.HTMLPAGE_DEPTH, "0");
        assertUrlContentListsChildrenAndStaysUnchanged(request);
    }

    /**
     * Method to test: createObjectMapUrlContent
     * Given Scenario: the request asks for related content with the {@code depth} query parameter
     * ({@code 0}), with no request attribute, and the URL-mapped content, loaded through the
     * Content API, has two related children
     * ExpectedResult: the url content map lists both children, and the contentlet that was
     * passed in has no value for the relationship field
     */
    @Test
    public void createObjectMapUrlContent_withDepthParameter_leavesGivenContentletUnchanged() throws Exception {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/?depth=0").request()).request();
        assertUrlContentListsChildrenAndStaysUnchanged(request);
    }

    /**
     * Creates URL-mapped content related to two children, serializes it with the given request as
     * the current request, and checks that the url content map lists both children while the
     * contentlet that was passed in has no value for the relationship field.
     *
     * @param request The request that asks for related content; the system user is set on it.
     */
    private static void assertUrlContentListsChildrenAndStaysUnchanged(final HttpServletRequest request)
            throws Exception {

        final ContentType childContentType = new ContentTypeDataGen().velocityVarName(
                "child" + System.currentTimeMillis()).nextPersisted();
        final ContentType parentContentType = new ContentTypeDataGen().velocityVarName(
                "parent" + System.currentTimeMillis()).nextPersisted();
        APILocator.getContentTypeFieldAPI().save(FieldBuilder.builder(RelationshipField.class)
                .name("children")
                .contentTypeId(parentContentType.id())
                .values(String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(childContentType.variable()).build(), APILocator.systemUser());

        final Contentlet child1 = new ContentletDataGen(childContentType.id()).nextPersistedAndPublish();
        final Contentlet child2 = new ContentletDataGen(childContentType.id()).nextPersistedAndPublish();
        final Contentlet parent = new ContentletDataGen(parentContentType.id())
                .setProperty("children", List.of(child1, child2)).nextPersistedAndPublish();

        final HttpServletRequest oldRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();
        try {
            request.setAttribute(com.liferay.portal.util.WebKeys.USER, APILocator.systemUser());
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(new MockHttpResponse().response());

            final Contentlet urlContent = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(parent.getIdentifier());
            final Map<String, Object> pageViewMap = new HashMap<>();
            new PageViewSerializer().createObjectMapUrlContent(urlContent, pageViewMap);

            final Map<?, ?> urlContentMap = Map.class.cast(pageViewMap.get("urlContentMap"));
            assertNotNull("the url content map must list the related content",
                    urlContentMap.get("children"));
            assertEquals(Set.of(child1.getIdentifier(), child2.getIdentifier()),
                    new HashSet<>((Collection<?>) urlContentMap.get("children")));
            assertFalse(urlContent.getMap().containsKey("children"));
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(oldResponse);
            ContentletDataGen.remove(parent);
            ContentletDataGen.remove(child1);
            ContentletDataGen.remove(child2);
            ContentTypeDataGen.remove(parentContentType);
            ContentTypeDataGen.remove(childContentType);
        }
    }

    /**
     * Method to test: getObjectMap
     * Given Scenario: the request asks for related content ({@code depth=0}) and the page, of a
     * page type with a relationship field and loaded through the HTML Page API, has two related
     * children
     * ExpectedResult: the page map lists both children, and the page object that was passed in
     * has no value for the relationship field
     */
    @Test
    public void getObjectMap_withDepth_leavesGivenPageUnchanged() throws Exception {

        final Host site = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().site(site).nextPersisted();
        final ContentType childContentType = new ContentTypeDataGen().velocityVarName(
                "child" + System.currentTimeMillis()).nextPersisted();
        final ContentType pageContentType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.HTMLPAGE)
                .velocityVarName("page" + System.currentTimeMillis()).nextPersisted();
        APILocator.getContentTypeFieldAPI().save(FieldBuilder.builder(RelationshipField.class)
                .name("children")
                .contentTypeId(pageContentType.id())
                .values(String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(childContentType.variable()).build(), APILocator.systemUser());

        final Contentlet child1 = new ContentletDataGen(childContentType.id()).nextPersistedAndPublish();
        final Contentlet child2 = new ContentletDataGen(childContentType.id()).nextPersistedAndPublish();
        final Contentlet savedPage = new HTMLPageDataGen(site, template)
                .structure(pageContentType.id())
                .setProperty("children", List.of(child1, child2))
                .nextPersisted();

        final HttpServletRequest oldRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse oldResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();
        try {
            final HttpServletRequest request = new MockAttributeRequest(
                    new MockHttpRequestIntegrationTest("localhost", "/").request()).request();
            request.setAttribute(com.liferay.portal.util.WebKeys.USER, APILocator.systemUser());
            request.setAttribute(WebKeys.HTMLPAGE_DEPTH, "0");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(new MockHttpResponse().response());

            final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(
                    APILocator.getContentletAPI().find(savedPage.getInode(), APILocator.systemUser(), false));
            final PageView pageView = new PageView.Builder()
                    .site(site)
                    .template(template)
                    .containers(Collections.emptyList())
                    .page(page)
                    .viewAs(new ViewAsPageStatus.Builder().build())
                    .build();

            final Map<String, Object> pageViewMap = new PageViewSerializer().getObjectMap(pageView);

            final Map<?, ?> pageMap = Map.class.cast(pageViewMap.get("page"));
            assertNotNull("the page map must list the related content", pageMap.get("children"));
            assertEquals(Set.of(child1.getIdentifier(), child2.getIdentifier()),
                    new HashSet<>((Collection<?>) pageMap.get("children")));
            assertFalse(page.getMap().containsKey("children"));
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(oldRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(oldResponse);
            ContentletDataGen.remove(savedPage);
            ContentletDataGen.remove(child1);
            ContentletDataGen.remove(child2);
            ContentTypeDataGen.remove(pageContentType);
            ContentTypeDataGen.remove(childContentType);
            TemplateDataGen.remove(template);
        }
    }
}
