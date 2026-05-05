package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import java.util.Collections;
import java.util.Map;
import org.junit.After;
import org.junit.Test;

import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.URL_FIELD;

/**
 * Unit test for {@link ContentHelper}
 */
public class ContentHelperTest extends UnitTestBase {

    @After
    public void resetFlag() {
        // Restore default (true) after each test that manipulates the flag.
        ContentHelper.setSuppressContentUrlFallback(true);
    }

    // -------------------------------------------------------------------------
    // Existing identifier-based tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetNullUrl() {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final String identifier = null;

        final String url = contentHelper.getUrl(identifier);

        assertNull(url);
    }

    @Test
    public void testGetNotFoundUrl() throws DotDataException {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final String identifier = null;

        when(identifierAPI.find(identifier)).thenReturn(null);

        final String url = contentHelper.getUrl(identifier);

        assertNull(url);
    }

    @Test
    public void testGetFoundUrl() throws DotDataException {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final String identifier = "1234";
        Identifier identifierObject = new Identifier();
        String urlExpected = "home_page";
        identifierObject.setAssetName(urlExpected);
        identifierObject.setId(identifier);

        when(identifierAPI.find(identifier)).thenReturn(identifierObject);

        final String url = contentHelper.getUrl(identifier);

        assertNotNull(url);
        assertEquals(urlExpected, url);
    }

    // -------------------------------------------------------------------------
    // FEATURE_FLAG_SUPPRESS_CONTENT_URL_FALLBACK tests
    // -------------------------------------------------------------------------

    /**
     * Regular contentlet with no URL field and flag ON (default) must return null so that
     * the url key is omitted entirely from the REST response map.
     */
    @Test
    public void testGetUrl_regularContent_noUrlField_flagOn_returnsNull() {

        ContentHelper.setSuppressContentUrlFallback(true);

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final Contentlet contentlet = mockRegularContentlet("abc-123", null);

        assertNull(contentHelper.getUrl(contentlet));
    }

    /**
     * Regular contentlet with no URL field and flag OFF must still return the
     * identifier-based fallback URL (legacy behaviour).
     */
    @Test
    public void testGetUrl_regularContent_noUrlField_flagOff_returnsFallback() throws DotDataException {

        ContentHelper.setSuppressContentUrlFallback(false);

        final String identifierId = "abc-123";
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final Identifier identifier = new Identifier();
        identifier.setId(identifierId);
        identifier.setParentPath("/");
        identifier.setAssetName(identifierId + ".content");
        when(identifierAPI.find(identifierId)).thenReturn(identifier);

        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final Contentlet contentlet = mockRegularContentlet(identifierId, null);

        final String url = contentHelper.getUrl(contentlet);

        assertNotNull(url);
        assertEquals("/" + identifierId + ".content", url);
    }

    /**
     * Regular contentlet that has an explicit URL field value must always return
     * that stored value regardless of the flag state.
     */
    @Test
    public void testGetUrl_regularContent_withUrlField_flagOn_returnsStoredUrl() {

        ContentHelper.setSuppressContentUrlFallback(true);

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final Contentlet contentlet = mockRegularContentlet("abc-123", "/my-article");

        assertEquals("/my-article", contentHelper.getUrl(contentlet));
    }

    /**
     * Web assets (HTMLPage) must always resolve their URL from the identifier
     * even when the flag is ON — they are explicitly excluded from suppression.
     */
    @Test
    public void testGetUrl_htmlPage_flagOn_returnsIdentifierUrl() throws DotDataException {

        ContentHelper.setSuppressContentUrlFallback(true);

        final String identifierId = "page-001";
        final String pageUri = "/about-us";
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final Identifier identifier = new Identifier();
        identifier.setId(identifierId);
        identifier.setParentPath("/");
        identifier.setAssetName("about-us");
        when(identifierAPI.find(identifierId)).thenReturn(identifier);

        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final Contentlet contentlet = mockWebAssetContentlet(identifierId, true, false, false);

        final String url = contentHelper.getUrl(contentlet);

        assertNotNull(url);
        assertEquals(pageUri, url);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a mock regular contentlet (not a page, file asset, or dot asset).
     *
     * @param identifierId the identifier key value
     * @param storedUrl    value returned by getStringProperty(URL_FIELD); null means "no URL field"
     */
    private Contentlet mockRegularContentlet(final String identifierId, final String storedUrl) {
        // hasUrlField checks fieldMap(fn) != null — a non-null map is sufficient.
        // Whether the field "exists" is then gated by getStringProperty(URL_FIELD) != null.
        final ContentType contentType = mock(ContentType.class);
        when(contentType.fieldMap(any())).thenReturn(Collections.emptyMap());

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(contentType);
        when(contentlet.getStringProperty(URL_FIELD)).thenReturn(storedUrl);
        when(contentlet.isFileAsset()).thenReturn(false);
        when(contentlet.isHTMLPage()).thenReturn(false);
        when(contentlet.isDotAsset()).thenReturn(false);
        when(contentlet.getMap()).thenReturn(Map.of(ContentletForm.IDENTIFIER_KEY, identifierId));
        return contentlet;
    }

    /**
     * Builds a mock web-asset contentlet (page, file asset, or dot asset).
     */
    private Contentlet mockWebAssetContentlet(final String identifierId,
            final boolean isHtmlPage, final boolean isFileAsset, final boolean isDotAsset) {

        final ContentType contentType = mock(ContentType.class);
        when(contentType.fieldMap(any())).thenReturn(Collections.emptyMap());

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(contentType);
        when(contentlet.getStringProperty(URL_FIELD)).thenReturn(null);
        when(contentlet.isHTMLPage()).thenReturn(isHtmlPage);
        when(contentlet.isFileAsset()).thenReturn(isFileAsset);
        when(contentlet.isDotAsset()).thenReturn(isDotAsset);
        when(contentlet.getMap()).thenReturn(Map.of(ContentletForm.IDENTIFIER_KEY, identifierId));
        return contentlet;
    }
}
