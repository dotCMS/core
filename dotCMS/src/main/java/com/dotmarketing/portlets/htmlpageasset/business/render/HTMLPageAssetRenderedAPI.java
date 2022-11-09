package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl.HTMLPageUrl;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.ViewAsPageStatus;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This API provides access to the metadata required byt dotCMS to render an HTML Page. For example, many methods in
 * this API return an instance of the {@link PageView} class, which represents all the different pieces of information
 * that are required for an HTML Page to display correctly.
 *
 * @author Freddy Rodriguez
 * @since Apr 12th, 2018
 */
public interface HTMLPageAssetRenderedAPI {

    /**
     * Returns the rendered version of an HTML Page, i.e., the HTML code that will be rendered in
     * the browser.
     *
     * @param request The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param user The {@link User} performing this action..
     * @param mode
     * @param user     The {@link User} performing this action.
     * @param uri      The path to the HTML Page whose information will be retrieved.
     * @return The {@link PageView} object containing the metadata of the different objects that
     * make up an HTML Page.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws IOException
     *
     * @deprecated deprecated since 5.1, use {@link HTMLPageAssetRenderedAPI#getPageMetadata(PageContext, HttpServletRequest, HttpServletResponse)} instead
     */
    @Deprecated(since = "Feb 22nd, 2019")
    PageView getPageMetadata(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final User user,
            final String uri,
            final PageMode mode)
            throws DotSecurityException, DotDataException;

    /**
     * Returns the Metadata object containing all the different pieces of information that make up an HTML Page. This
     * way, it can potentially be rendered in the browser or handled by any headless service.
     *
     * @param context  The {@link PageContext} object providing the details on how the HTML Page must be exposed.
     * @param request  The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     *
     * @return The {@link PageView} object containing the metadata of the different objects that
     * make up an HTML Page.
     *
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     */
    PageView getPageMetadata(
            final PageContext context,
            final HttpServletRequest request,
            final HttpServletResponse response)
            throws DotSecurityException, DotDataException;

    /**
     * Returns a {@link HTMLPageUrl} given a {@link PageContext}
     *
     * @param context {@link PageContext} object with the request parameters
     * @param request servlet request
     * @return the resulting HTMLPageURL
     * @throws DotSecurityException The user does not have the specified permissions to perform
     * this action.
     * @throws DotDataException
     */

    HTMLPageUrl getHtmlPageAsset(PageContext context,
            HttpServletRequest request) throws DotSecurityException, DotDataException;

    /***
     * Returns the rendered version of an HTML Page, i.e., the HTML code that will be rendered in
     * the browser.
     *
     * @param request The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param user The {@link User} performing this action.
     * @param pageUri The path to the HTML Page whose information will be retrieved.
     * @param pageMode
     * @return The {@link HTMLPageAssetRendered} object containing the metadata of the different objects that
     * make up an HTML Page.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws IOException
     *
     * @deprecated deprecated since 5.1, use {@link HTMLPageAssetRenderedAPI#getPageRendered(PageContext, HttpServletRequest, HttpServletResponse)} instead
     */
    public PageView getPageRendered(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final User user,
            final String pageUri,
            final PageMode pageMode)
            throws DotDataException, DotSecurityException;
    /***
     * Returns the rendered version of an HTML Page, i.e., the HTML code that will be rendered in
     * the browser.
     *
     * @param context The {@link PageContext} object.
     * @return The {@link HTMLPageAssetRendered} object containing the metadata of the different objects that
     * make up an HTML Page.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws IOException
     */
    PageView getPageRendered(
            final PageContext context,
            final HttpServletRequest request,
            final HttpServletResponse response)
            throws DotDataException, DotSecurityException;


    /**
     * Return the Page's html string
     * @param request The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param user user
     * @param uri page's uri
     * @param mode page's mode
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     *
     * @deprecated deprecated since 5.1, use {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)} instead
     */
    public String getPageHtml(final HttpServletRequest request, final HttpServletResponse response, final User user,
            final String uri, final PageMode mode) throws DotSecurityException, DotDataException;

    /**
     * Return the Page's html string
     *
     * @param context {@link PageContext}
     * @param request The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    String getPageHtml(
            final PageContext context,
            final HttpServletRequest request,
            final HttpServletResponse response) throws DotSecurityException, DotDataException;

    /**
     * Return the page's default mode for edit page portlet, if user have the page's lock then return
     * {@link PageMode#EDIT_MODE}, in other case return {@link PageMode#PREVIEW_MODE}
     *
     * @param user
     * @param request
     * @param pageUri
     * @return
     */
    PageMode getDefaultEditPageMode(
            final User user,
            final HttpServletRequest request,
            final String pageUri
    );

    /**
     * Returns the {@link ViewAsPageStatus} for the given {@link HTMLPageAsset} and {@link PageMode}
     *
     * @param pageMode
     * @param htmlpage
     * @return
     */

    ViewAsPageStatus getViewAsStatus(final HttpServletRequest request,
            final PageMode pageMode, final HTMLPageAsset htmlpage, final User user)
            throws DotDataException;

    /**
     * Gets the page rendered live and preview
     * @param pageId {@link String}
     * @param languageId {@link Long}
     * @return PageLivePreviewVersionBean
     */
    PageLivePreviewVersionBean getPageRenderedLivePreviewVersion (final String  pageId,
                                                                  final User user,
                                                                  final long languageId,
                                                                  final HttpServletRequest  request,
                                                                  final HttpServletResponse response)
            throws DotSecurityException, DotDataException;

}
