package com.dotcms.rest;

import com.dotcms.UnitTestBase;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ContentHelper}
 */
public class ContentHelperTest extends UnitTestBase {

    @Test
    public void testGetNullUrl() {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI);
        final String identifier = null;

        final String url = contentHelper.getUrl(identifier);

        assertNull(url);
    }

    @Test
    public void testGetNotFoundUrl() throws DotDataException {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI);
        final String identifier = null;

        when(identifierAPI.find(identifier)).thenReturn(null);

        final String url = contentHelper.getUrl(identifier);

        assertNull(url);
    }

    @Test
    public void testGetFoundUrl() throws DotDataException {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI);
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
        final ContentHelper contentHelper = new ContentHelper(identifierAPI);
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
        assertTrue(newContentlet == contentlet);
    }

    @Test
    public void testHydrateContentLetWithoutUrlAndAssetNameDoesNotExist() throws DotDataException {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI);
        final String identifier = "1234";
        Contentlet contentlet = new Contentlet();

        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);


        when(identifierAPI.find(identifier)).thenReturn(null);

        final Contentlet newContentlet = contentHelper.hydrateContentLet(contentlet);

        assertNotNull(newContentlet);
        assertTrue(newContentlet == contentlet);
    }

    @Test
    public void testHydrateContentLetWithoutUrl() throws DotDataException {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI);
        final String identifier = "1234";
        Identifier identifierObject = new Identifier();
        String urlExpected = "home_page";
        identifierObject.setAssetName(urlExpected);
        identifierObject.setId(identifier);
        Contentlet contentlet = new Contentlet();

        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);


        when(identifierAPI.find(identifier)).thenReturn(identifierObject);

        final Contentlet newContentlet = contentHelper.hydrateContentLet(contentlet);

        assertNotNull(newContentlet);
        assertFalse(newContentlet == contentlet);
        assertFalse(contentlet.getMap().containsKey(HTMLPageAssetAPI.URL_FIELD));
        assertTrue(newContentlet.getMap().containsKey(HTMLPageAssetAPI.URL_FIELD));
        assertEquals(urlExpected, newContentlet.getMap().get(HTMLPageAssetAPI.URL_FIELD));
    }
}
