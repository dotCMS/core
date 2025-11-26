package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.util.FiltersUtil;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.visitor.domain.Visitor;
import com.dotcms.visitor.filter.characteristics.BaseCharacter;
import com.dotcms.visitor.filter.characteristics.Character;
import com.dotcms.visitor.filter.characteristics.CharacterWebAPI;
import com.dotcms.visitor.filter.characteristics.GDPRCharacter;
import com.dotcms.visitor.filter.servlet.VisitorFilter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import graphql.AssertException;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Simple utility class that helps with the execution of Content Analytics-related tests. It
 * provides methods for mocking specific objects, creating test data, compare expected results with
 * the returned results, and so on.
 *
 * @author Jose Castro
 * @since Oct 9th, 2024
 */
public class Util {

    public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:131" +
            ".0) Gecko/20100101 Firefox/131.0";

    /**
     * Creates a mock {@link HttpServletRequest} object with the given parameters.
     *
     * @param response  The {@link HttpServletResponse} object.
     * @param url       The URL of the request.
     * @param requestId The request ID.
     * @param cmsUser   The {@link User} object.
     *
     * @return The mock {@link HttpServletRequest} object.
     *
     * @throws UnknownHostException
     */
    public static HttpServletRequest mockHttpRequestObj(final HttpServletResponse response,
                                                        final String url, final String requestId,
                                                        final User cmsUser) throws UnknownHostException {
        return mockHttpRequestObj(response, url, requestId, cmsUser, null, null);
    }

    /**
     * Creates a mock {@link HttpServletRequest} object with the given parameters.
     *
     * @param response      The {@link HttpServletResponse} object.
     * @param url           The URL of the request.
     * @param requestId     The request ID.
     * @param cmsUser       The {@link User} object.
     * @param requestAttrs  A Map with the request attributes.
     * @param requestParams A Map with the request parameters.
     *
     * @return The mock {@link HttpServletRequest} object.
     *
     * @throws UnknownHostException
     */
    public static HttpServletRequest mockHttpRequestObj(final HttpServletResponse response,
                                                        final String url, final String requestId,
                                                        final User cmsUser, final Map<String,
            Object> requestAttrs,
                                                        final Map<String, Object> requestParams) throws UnknownHostException {
        return mockHttpRequestObj(response, url, requestId, cmsUser, requestAttrs, requestParams,
                null);
    }

    /**
     * Creates a mock {@link HttpServletRequest} object with the given parameters.
     *
     * @param response        The {@link HttpServletResponse} object.
     * @param url             The URL of the request.
     * @param requestId       The request ID.
     * @param cmsUser         The {@link User} object.
     * @param requestAttrs    A Map with the request attributes.
     * @param requestParams   A Map with the request parameters.
     * @param characterParams A Map with parameters for the {@link Character} object.
     *
     * @return The mock {@link HttpServletRequest} object.
     *
     * @throws UnknownHostException
     */
    public static HttpServletRequest mockHttpRequestObj(final HttpServletResponse response,
                                                        final String url, final String requestId,
                                                        final User cmsUser, final Map<String,
            Object> requestAttrs,
                                                        final Map<String, Object> requestParams,
                                                        final Map<String, Object> characterParams) throws UnknownHostException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        when(request.getSession()).thenReturn(session);
        when(session.getId()).thenReturn("DAA3339CD687D9ABD4101CF9EDDD42DB");
        when(session.isNew()).thenReturn(true);
        when(request.getRequestURI()).thenReturn(url);
        when(request.getAttribute(VisitorFilter.DOTPAGE_PROCESSING_TIME)).thenReturn(1000L);
        when(request.getAttribute("requestId")).thenReturn(requestId);
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
        when(request.getHeader("user-agent")).thenReturn(USER_AGENT);
        when(request.getHeader("host")).thenReturn("localhost:8080");
        if (UtilMethods.isSet(requestAttrs)) {
            for (final Map.Entry<String, Object> entry : requestAttrs.entrySet()) {
                when(request.getAttribute(entry.getKey())).thenReturn(entry.getValue());
            }
        }

        if (UtilMethods.isSet(requestParams)) {
            for (final Map.Entry<String, Object> entry : requestParams.entrySet()) {
                when(request.getParameter(entry.getKey())).thenReturn((String) entry.getValue());
            }
        }

        final Visitor visitor = new Visitor();
        visitor.setIpAddress(HttpRequestDataUtil.getIpAddress(request));
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(visitor);
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(cmsUser);

        final GDPRCharacter gdprCharacter = new GDPRCharacter(new BaseCharacter(request, response));
        if (UtilMethods.isSet(characterParams)) {
            for (final Map.Entry<String, Object> entry : characterParams.entrySet()) {
                gdprCharacter.getMap().put(entry.getKey(), (Serializable) entry.getValue());
            }
        }
        when(request.getAttribute(CharacterWebAPI.DOT_CHARACTER)).thenReturn(gdprCharacter);

        when(request.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.LIVE.name());

        return request;
    }

    /**
     * Retrieves the {@link CollectorPayloadBean} object with specific test parameters from the
     * given collector.
     *
     * @param request        The {@link HttpServletRequest} object.
     * @param collector      The {@link Collector} that will be called.
     * @param requestMatcher The {@link RequestMatcher} object.
     * @param contextMap     A Map with the context parameters.
     *
     * @return The {@link CollectorPayloadBean} object.
     */
    public static CollectorPayloadBean getCollectorPayloadBean(final HttpServletRequest request,
                                                               final Collector collector,
                                                               final RequestMatcher requestMatcher,
                                                               final Map<String, Object> contextMap) {
        final Character character = (Character) request.getAttribute(CharacterWebAPI.DOT_CHARACTER);
        final CollectorContextMap syncCollectorContextMap =
                null == contextMap
                        ? new RequestCharacterCollectorContextMap(request, character,
                        requestMatcher)
                        : new CharacterCollectorContextMap(character, requestMatcher, contextMap);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBean();
        return collector.collect(syncCollectorContextMap, collectorPayloadBean);
    }

    /**
     * Retrieves the {@link CollectorPayloadBean} object with specific test parameters from the
     * given collector. This one is specific For Vanity URL Collectors.
     *
     * @param request    The {@link HttpServletRequest} object.
     * @param collector  The {@link Collector} that will be called.
     * @param contextMap A Map with the context parameters.
     *
     * @return The {@link CollectorPayloadBean} object.
     */
    public static CollectorPayloadBean getRequestCharacterCollectorPayloadBean(final HttpServletRequest request,
                                                                               final Collector collector,
                                                                               final Map<String,
                                                                                       Object> contextMap) {
        final Character character = (Character) request.getAttribute(CharacterWebAPI.DOT_CHARACTER);
        if (UtilMethods.isSet(contextMap)) {
            for (final Map.Entry<String, Object> entry : contextMap.entrySet()) {
                character.getMap().put(entry.getKey(), (Serializable) entry.getValue());
            }
        }
        final RequestMatcher requestMatcher = new VanitiesRequestMatcher();
        final CollectorContextMap syncCollectorContextMap =
                new RequestCharacterCollectorContextMap(request, character, requestMatcher);
        final CollectorPayloadBean collectorPayloadBean = new ConcurrentCollectorPayloadBean();
        return collector.collect(syncCollectorContextMap, collectorPayloadBean);
    }

    /**
     * Creates a {@link ContentType} object that can be used for URL Mapped content.
     *
     * @param contentTypeName      The name of the content type.
     * @param site                 The {@link Host} object where the Content Type will live.
     * @param detailPageIdentifier The identifier of the detail page that will displayed the mapped
     *                             content.
     * @param urlMapPattern        The URL map pattern for the URL Map.
     *
     * @return The {@link ContentType} object.
     */
    @WrapInTransaction
    public static ContentType getUrlMapLikeContentType(final String contentTypeName,
                                                       final Host site,
                                                       final String detailPageIdentifier,
                                                       final String urlMapPattern) {
        ContentType newsType = Try.of(() ->
                APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentTypeName)).getOrNull();
        if (newsType == null) {
            final List<Field> fields = new ArrayList<>();
            fields.add(new FieldDataGen()
                    .name("Site or Folder")
                    .velocityVarName("hostfolder")
                    .required(Boolean.TRUE)
                    .type(HostFolderField.class)
                    .next()
            );
            fields.add(new FieldDataGen()
                    .name("urlTitle")
                    .velocityVarName("urlTitle")
                    .searchable(false)
                    .indexed(true)
                    .listed(true)
                    .next()
            );

            try {
                final ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(APILocator.getWorkflowAPI().findSystemWorkflowScheme().getId())
                        .fields(fields);
                if (null != site) {
                    contentTypeDataGen.host(site);
                }
                if (null != detailPageIdentifier) {
                    contentTypeDataGen.detailPage(detailPageIdentifier);
                }
                if (null != urlMapPattern) {
                    contentTypeDataGen.urlMapPattern(urlMapPattern);
                }
                newsType = contentTypeDataGen.nextPersisted();
            } catch (final Exception e) {
                throw new DotRuntimeException(e);
            }
        }
        return newsType;
    }

    /**
     * Validates the expected data that should be returned by a given Collector with the actual data
     * that was returned.
     *
     * @param expectedDataMap The expected data.
     * @param collectedData   The actual data.
     */
    public static void validateExpectedEntries(final Map<String, Object> expectedDataMap,
                                               final CollectorPayloadBean collectedData) {
        final Map<String, Serializable> collectedDataMap = collectedData.toMap();
        validateExpectedEntries(expectedDataMap, collectedDataMap);
    }

    /**
     * Validates the expected data that should be returned by a given Collector with the actual data
     * that was returned.
     *
     * @param expectedDataMap The expected data.
     * @param collectedData   The actual data.
     */
    public static void validateExpectedEntries(final Map<String, Object> expectedDataMap,
                                               final Map<String, ?> collectedData) {
        assertTrue("Number of returned expected properties doesn't match", collectedData.size() >= expectedDataMap.size());
        for (final String key : expectedDataMap.keySet()) {
            if (collectedData.containsKey(key)) {
                final Object expectedValue = expectedDataMap.get(key);
                final Object collectedValue = collectedData.get(key);
                if (expectedValue instanceof Map) {
                    final Map<String, Object> expectedMap = (Map<String, Object>) expectedValue;
                    final Map<String, Object> collectedMap = (Map<String, Object>) collectedValue;
                    assertTrue("Number of returned expected properties in 'object' entry " +
                                    "doesn't match",
                            collectedMap.size() >= expectedMap.size());
                    for (final String mapKey : expectedMap.keySet()) {
                        assertEquals("Collected value in 'object' entry must be equal to expected" +
                                " value for key: "
                                + mapKey, expectedMap.get(mapKey).toString(), collectedMap.get(mapKey).toString());
                    }
                } else {
                    assertEquals("Collected value must be equal to expected value for key: " + key,
                            expectedValue, collectedValue);
                }
            } else {
                throw new AssertException("Expected key in the Collected value: " + key);
            }
        }
    }

    /**
     * Creates a test HTML Page with the given name.
     *
     * @param site     The {@link Host} object where the HTML Page will live.
     * @param pageName The name of the HTML Page.
     *
     * @return The {@link HTMLPageAsset} object.
     */
    public static HTMLPageAsset createTestHTMLPage(final Host site, final String pageName) {
        final HTMLPageAsset testPage = new HTMLPageDataGen(site,
                APILocator.getTemplateAPI().systemTemplate())
                .pageURL(pageName)
                .title(pageName)
                .nextPersisted();
        ContentletDataGen.publish(testPage);
        return testPage;
    }

    /**
     * Creates a test HTML Page with the given name and folder name.
     *
     * @param site       The {@link Host} object where the HTML Page will live.
     * @param pageName   The name of the HTML Page.
     * @param folderName The name of the folder where the HTML Page will live.
     *
     * @return The {@link HTMLPageAsset} object.
     */
    public static HTMLPageAsset createTestHTMLPage(final Host site, final String pageName,
                                                   final String folderName) {
        final Folder parentFolder =
                new FolderDataGen().name(folderName).title(folderName).site(site)
                        .nextPersisted();
        final HTMLPageAsset testPage = new HTMLPageDataGen(parentFolder,
                APILocator.getTemplateAPI().systemTemplate())
                .pageURL(pageName)
                .title(pageName)
                .nextPersisted();
        ContentletDataGen.publish(testPage);
        return testPage;
    }

    /**
     * Creates a test File Asset with the given parameters.
     *
     * @param fileName   The name of the file.
     * @param suffix     The file's extension.
     * @param content    The file's content, if necessary.
     * @param folderName The name of the folder where the file will live.
     * @param site       The {@link Host} object where the file will live.
     *
     * @return The {@link FileAsset} object.
     *
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    public static FileAsset createTestFileAsset(final String fileName, final String suffix,
                                                final String content, final String folderName,
                                                final Host site) throws DotDataException,
            IOException, DotSecurityException {
        final Folder parentFolder =
                new FolderDataGen().name(folderName).title(folderName).site(site).nextPersisted();
        final Contentlet testFileAsContent = FileAssetDataGen.createFileAssetDataGen(parentFolder,
                fileName, suffix, content).nextPersisted();
        ContentletDataGen.publish(testFileAsContent);
        return APILocator.getFileAssetAPI().fromContentlet(testFileAsContent);
    }

    /**
     * Creates a test Vanity URL contentlet.
     *
     * @param site      The {@link Host} object where the Vanity URL will live.
     * @param title     The title of the Vanity URL.
     * @param uri       The URI of the HTML Page that the Vanity will point to.
     * @param forwardTo The URL to forward to.
     *
     * @return The {@link Contentlet} object representing the Vanity URL.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static Optional<CachedVanityUrl> createAndResolveVanityURL(final Host site,
                                                                      final String title,
                                                                      final String uri,
                                                                      final String forwardTo) throws DotDataException, DotSecurityException {
        final FiltersUtil filtersUtil = FiltersUtil.getInstance();
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        final Contentlet vanity = filtersUtil.createVanityUrl(title, site, uri, forwardTo,
                200, 1, defaultLanguage.getId());
        filtersUtil.publishVanityUrl(vanity);

        return APILocator.getVanityUrlAPI().resolveVanityUrl(uri, site, defaultLanguage);
    }

}
