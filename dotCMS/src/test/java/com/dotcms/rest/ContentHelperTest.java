package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import org.junit.Test;

/**
 * Unit test for {@link ContentHelper}
 */
public class ContentHelperTest extends UnitTestBase {

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

    @Test
    public void testHydrateContentLetWithUrl() throws DotDataException {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final String identifier = "1234";
        Identifier identifierObject = new Identifier();
        String urlExpected = "home_page";
        identifierObject.setAssetName(urlExpected);
        identifierObject.setId(identifier);
        Contentlet contentlet = new Contentlet();

        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);
        contentlet.getMap().put(HTMLPageAssetAPI.URL_FIELD, urlExpected);


        when(identifierAPI.find(identifier)).thenReturn(identifierObject);

        final Contentlet newContentlet = contentHelper.hydrateContentLet(contentlet);

        assertNotNull(newContentlet);
        assertNotSame(newContentlet, contentlet); //This method now returns a new instance. A copy of the original contentlet
        assertFalse(newContentlet.getMap().containsKey(Contentlet.NULL_PROPERTIES));
        assertEquals(newContentlet.getMap().get(ContentletForm.IDENTIFIER_KEY), identifier);
        assertEquals(newContentlet.getMap().get(HTMLPageAssetAPI.URL_FIELD), urlExpected);
    }

    @Test
    public void testHydrateContentLetWithoutUrlAndAssetNameDoesNotExist() throws DotDataException {
        final String  anyUrl = "anyUrl";
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE){
            @Override
            public String getUrl(Contentlet contentlet) {
                return anyUrl;
            }
        };
        final String identifier = "1234";

        Contentlet contentlet = new Contentlet();

        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);

        when(identifierAPI.find(identifier)).thenReturn(null);

        final Contentlet newContentlet = contentHelper.hydrateContentLet(contentlet);

        assertNotNull(newContentlet);
        assertNotSame(newContentlet, contentlet); //This method now returns a new instance. A copy of the original contentlet
        assertFalse(newContentlet.getMap().containsKey(Contentlet.NULL_PROPERTIES));
        assertEquals(identifier, newContentlet.getMap().get(ContentletForm.IDENTIFIER_KEY));
        assertEquals(anyUrl, newContentlet.getMap().get(HTMLPageAssetAPI.URL_FIELD));
    }

    @Test
    public void testHydrateContentLetWithoutUrl() throws DotDataException {
        String urlExpected = "home_page";
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE){
            @Override
            public String getUrl(Contentlet contentlet) {
                return urlExpected;
            }
        };
        final String identifier = "1234";
        Identifier identifierObject = new Identifier();
        identifierObject.setAssetName(urlExpected);
        identifierObject.setId(identifier);
        Contentlet contentlet = new Contentlet();

        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);
        when(identifierAPI.find(identifier)).thenReturn(identifierObject);

        final Contentlet newContentlet = contentHelper.hydrateContentLet(contentlet);

        assertNotNull(newContentlet);
        assertNotSame(newContentlet, contentlet);
        assertFalse(contentlet.getMap().containsKey(HTMLPageAssetAPI.URL_FIELD));
        assertTrue(newContentlet.getMap().containsKey(HTMLPageAssetAPI.URL_FIELD));
        assertEquals(urlExpected, newContentlet.getMap().get(HTMLPageAssetAPI.URL_FIELD));
        assertFalse(newContentlet.getMap().containsKey(Contentlet.NULL_PROPERTIES));
    }
}
