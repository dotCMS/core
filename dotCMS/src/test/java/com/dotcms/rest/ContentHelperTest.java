package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotcms.util.ContentSearchLimit;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Unit test for {@link ContentHelper}
 */
public class ContentHelperTest extends UnitTestBase {

    private String previousMaximum;

    @Before
    public void clearConfiguredMaximum() {
        previousMaximum = Config.getStringProperty(ContentSearchLimit.MAX_LIMIT_PROPERTY, null);
        Config.setProperty(ContentSearchLimit.MAX_LIMIT_PROPERTY, null);
    }

    @After
    public void restoreConfiguredMaximum() {
        Config.setProperty(ContentSearchLimit.MAX_LIMIT_PROPERTY, previousMaximum);
    }

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
    public void testPullContentCapsALimitAboveTheMaximum() throws Exception {
        // Given a query and a limit of 20000, with the default maximum of 10000
        // When the content is pulled
        // Then ContentUtils.pull receives a limit of 10000
        assertEquals(10000, limitPulledFor(20000));
    }

    @Test
    public void testPullContentKeepsALimitWithinTheMaximum() throws Exception {
        // Given a query and a limit of 20
        // When the content is pulled
        // Then ContentUtils.pull receives a limit of 20
        assertEquals(20, limitPulledFor(20));
    }

    /**
     * Pulls content for a query with the given limit and returns the limit the content was
     * pulled with.
     */
    private int limitPulledFor(final int requestedLimit) throws Exception {
        final ContentHelper contentHelper = new ContentHelper(mock(IdentifierAPI.class),
                MapToContentletPopulator.INSTANCE);
        final User user = mock(User.class);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        // ContentUtils looks up the content API when the class loads, so the locator is mocked first
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class);
                MockedStatic<ContentUtils> contentUtils = mockStatic(ContentUtils.class)) {
            apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
            contentUtils.when(() -> ContentUtils.pull(anyString(), anyInt(), anyInt(), any(),
                    any(User.class), any(), anyBoolean())).thenReturn(new PaginatedArrayList<>());

            contentHelper.pullContent(mock(HttpServletRequest.class),
                    mock(HttpServletResponse.class), "+contentType:News", user, PageMode.LIVE, 0,
                    requestedLimit, "modDate desc", null, null, user, -1, 1L, false);

            final ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
            contentUtils.verify(() -> ContentUtils.pull(anyString(), anyInt(), limit.capture(),
                    any(), any(User.class), any(), anyBoolean()));
            return limit.getValue();
        }
    }

}
