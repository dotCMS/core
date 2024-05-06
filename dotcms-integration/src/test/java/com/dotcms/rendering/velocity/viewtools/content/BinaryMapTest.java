package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Constants;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static com.dotcms.uuid.shorty.ShortyIdAPIImpl.MINIMUM_SHORTY_ID_LENGTH;
import static com.liferay.util.StringPool.FORWARD_SLASH;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the {@link BinaryMap} available in certain ViewTools is working as expected.
 *
 * @author Daniel Silva
 * @since Feb 15th, 2022
 */
public class BinaryMapTest {

    private static final String IMAGE_FIELD_VAR_NAME = "image";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Test getSize method return expected result
     */
    @Test
    public void test_getSize() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

        assertEquals("154.8 K", binaryMap.getSize());
    }

    /**
     * Test getWidth method return expected result
     */
    @Test
    public void test_getWidth() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

        assertEquals(800, binaryMap.getWidth());
    }

    /**
     * Test getHeight method return expected result
     */
    @Test
    public void test_getHeight() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

        assertEquals(500, binaryMap.getHeight());
    }

    /**
     * Test getFile method return expected result
     */
    @Test
    public void test_getFile() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

        assertTrue(binaryMap.getFile().getName().startsWith("image"));
        assertTrue(binaryMap.getFile().getName().endsWith("jpg"));
    }

    /**
     * Test getThumbnailUri method return expected result
     */
    @Test
    public void test_getThumbnailUri() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

        assertTrue(binaryMap.getThumbnailUri().startsWith("/contentAsset/image/"));
        assertTrue(binaryMap.getThumbnailUri().endsWith("/image/filter/Thumbnail"));
    }

    /**
     * Test getShortyUrlInode method return expected result
     */
    @Test
    public void test_getShortyUrlInode() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

        assertTrue(binaryMap.getShortyUrlInode().startsWith("/dA/"));
        assertTrue(binaryMap.getShortyUrlInode().endsWith(".jpg"));
    }

    /**
     * Test getShorty method return expected result
     */
    @Test
    public void test_getShorty() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

        assertEquals(MINIMUM_SHORTY_ID_LENGTH, binaryMap.getShorty().length());
    }

    /**
     * Test getShortyUrl method return expected result
     */

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BinaryMap#getShortyUrl()} ()}</li>
     *     <li><b>Given Scenario:</b> Verify that the {@link BinaryMap#getShortyUrl()} ()} method
     *     returns the URI in the expected format.</li>
     *     <li><b>Expected Result:</b> The Shorty URL must include the file extension
     *     and the language parameter.</li>
     * </ul>
     */
    @Test
    public void testGetShortyUrl() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), IMAGE_FIELD_VAR_NAME);

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);
        final String rawUri = "/dA/" + binaryMap.getShorty() + FORWARD_SLASH + IMAGE_FIELD_VAR_NAME + FORWARD_SLASH;

        assertTrue("The generated Shorty URL does not match the expected format",
                binaryMap.getShortyUrl().startsWith(rawUri));
        assertTrue("The generated Shorty URL does not contain the file extension 'jpg'",
                binaryMap.getShortyUrl().contains(".jpg"));
        assertTrue("The generated Shorty URL does not contain the language parameter",
                binaryMap.getShortyUrl().endsWith("?language_id=" + language.getId()));
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BinaryMap#getRawUri()}</li>
     *     <li><b>Given Scenario:</b> Verify that the {@link BinaryMap#getRawUri()} method returns the URI in the
     *     expected format.</li>
     *     <li><b>Expected Result:</b> The Raw URI must comply with the specified format.</li>
     * </ul>
     */
    @Test
    public void testGetRawUri() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), IMAGE_FIELD_VAR_NAME);

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);
        final String rawUri = "/dA/" + banner.getIdentifier() + FORWARD_SLASH + IMAGE_FIELD_VAR_NAME + FORWARD_SLASH;
        final String[] uriArray = binaryMap.getRawUri().split(FORWARD_SLASH);

        assertTrue("The generated Raw URI does not match the expected format",
                binaryMap.getRawUri().startsWith(rawUri));
        assertEquals("The Image field Var Name is not 'image'", IMAGE_FIELD_VAR_NAME, uriArray[3]);
        assertTrue("The generated Raw URI does not contain the language parameter",
                binaryMap.getShortyUrl().endsWith("?language_id=" + language.getId()));

    }

    /**
     * Test getName method return expected result
     */
    @Test
    public void test_getName() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

        assertTrue(binaryMap.getName().startsWith("image"));
        assertTrue(binaryMap.getName().endsWith(".jpg"));
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BinaryMap#getResizeUri(Integer, Integer)}</li>
     *     <li><b>Given Scenario:</b> Verify that the {@link BinaryMap#getResizeUri(Integer, Integer)} method returns the URI in the
     *     expected format.</li>
     *     <li><b>Expected Result:</b> The URI must not contain double slash.</li>
     * </ul>
     */
    @Test
    public void test_getResizeUri() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), "image");

        final Context velocityContext = mock(Context.class);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

        String resizeUri = binaryMap.getResizeUri(200,200);

        assertFalse("Contains double slash sending both params", resizeUri.contains("//"));

        resizeUri = binaryMap.getResizeUri(0,200);

        assertFalse("Contains double slash sending only height", resizeUri.contains("//"));

        resizeUri = binaryMap.getResizeUri(200,0);

        assertFalse("Contains double slash sending only width", resizeUri.contains("//"));

        resizeUri = binaryMap.getResizeUri(0,0);

        assertFalse("Contains double slash sending no params", resizeUri.contains("//"));
    }

/**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BinaryMap#getShortyUrl()}</li>
     *     <li><b>Given Scenario:</b> Verify that the {@link BinaryMap#getShortyUrl()} method returns
     *     the URI without language parameter when the page is generated for static PP.</li>
     *     <li><b>Expected Result:</b> The Shorty URL must not contain the language parameter.</li>
     * </ul>
     */
    @Test
    public void test_GetShortyURLWithoutLanguage() throws DotDataException {
        final Language language = new LanguageDataGen().nextPersisted();

        final Contentlet banner =
                TestDataUtils.getBannerLikeContent(true, language.getId(), null, null);

        final Field binaryField = APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(banner.getContentTypeId(), IMAGE_FIELD_VAR_NAME);

        // User-agent attribute is set in the request to signal that the page is generated for static PP
        final ChainedContext velocityContext = mock(ChainedContext.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(velocityContext.getRequest()).thenReturn(request);
        when(request.getAttribute("User-Agent")).thenReturn(Constants.USER_AGENT_DOTCMS_PUSH_PUBLISH);

        final BinaryMap binaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

        final String shortyUrl = binaryMap.getShortyUrl();

        assertFalse("Shorty URL does not contain language parameter",
                shortyUrl.contains("?language_id=" + language.getId()));

        // If the request is not included in the context, it should be stored in a ThreadLocal variable
        HttpServletRequest currentRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        try {
            final BinaryMap fallbackBinaryMap = new BinaryMap(banner,
                new LegacyFieldTransformer(binaryField).asOldField(), velocityContext);

            final String fallbackShortyUrl = fallbackBinaryMap.getShortyUrl();

            assertFalse("Fallback shorty URL does not contain language parameter",
                fallbackShortyUrl.contains("?language_id=" + language.getId()));

        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(currentRequest);
        }

    }
}
