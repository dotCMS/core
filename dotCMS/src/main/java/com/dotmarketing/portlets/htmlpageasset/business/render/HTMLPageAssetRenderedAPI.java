package com.dotmarketing.portlets.htmlpageasset.business.render;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl.HTMLPageUrl;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.ViewAsPageStatus;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides utility methods to render a {@link com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset}
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
    public PageView getPageMetadata(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final User user,
            final String uri,
            final PageMode mode)
                throws DotSecurityException, DotDataException;

    /**
     * Returns the rendered version of an HTML Page, i.e., the HTML code that will be rendered in
     * the browser.
     *
     * @param context {@link PageContext}
     * @param request The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @return The {@link PageView} object containing the metadata of the different objects that
     * make up an HTML Page.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws IOException
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
     *  TODO add javadoc
     * @param request
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
     List<ContainerRaw> getPageContainers(final HttpServletRequest request,
             final HTMLPageAsset htmlPage, final PageMode mode, final User user)
             throws DotDataException, DotSecurityException;
}
