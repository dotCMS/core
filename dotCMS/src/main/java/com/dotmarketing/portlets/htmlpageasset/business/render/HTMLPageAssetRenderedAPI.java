package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Provides utility methods to render a {@link com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset}
 */
public interface HTMLPageAssetRenderedAPI {

    public HTMLPageAssetRendered getPageRendered(HttpServletRequest request, HttpServletResponse response, User user,
                                               String pageUri, PageMode pageMode)
            throws DotDataException, DotSecurityException, IOException;

    public HTMLPageAssetRendered getPageRendered(final HttpServletRequest request, final HttpServletResponse response,
                                                 final User user, final HTMLPageAsset page, PageMode pageMode)
            throws DotDataException, DotSecurityException, IOException ;

    public PageView getPageMetadata(final HttpServletRequest request, final HttpServletResponse
            response, final User user, final String uri, boolean live) throws DotSecurityException,
            DotDataException;

    /**
     * Returns the rendered version of an HTML Page, i.e., the HTML code that will be rendered in
     * the browser.
     *
     * @param request  The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param user     The {@link User} performing this action.
     * @param uri      The path to the HTML Page whose information will be retrieved.
     * @return The {@link PageView} object containing the metadata of the different objects that
     * make up an HTML Page.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     */
    public PageView getPageMetadataRendered(final HttpServletRequest request, final HttpServletResponse response,
                                            final User user, final String uri, boolean live) throws DotSecurityException,
            DotDataException;
}
