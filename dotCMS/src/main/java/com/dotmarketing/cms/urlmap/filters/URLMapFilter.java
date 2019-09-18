/*
 * WebSessionFilter
 *
 * A filter that recognizes return users who have
 * chosen to have their login information remembered.
 * Creates a valid WebSession object and
 * passes it a contact to use to fill its information
 *
 */
package com.dotmarketing.cms.urlmap.filters;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cms.urlmap.URLMapAPIImpl;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContextBuilder;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.*;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.oro.text.regex.MalformedPatternException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

/**
 * This filter handles all requests regarding URL Maps. These URL maps on
 * content structures are used to create friendly URLs for SEO.
 *
 * @author root
 * @version 1.2
 * @since 03-22-2012
 */
public class URLMapFilter implements Filter {

    private UserWebAPI wuserAPI;
    private HostWebAPI whostAPI;
    private LanguageWebAPI languageWebAPI;
    private URLMapAPIImpl urlMapAPI;

    public void destroy() {

    }

    /**
     * Runs the filter validations on the current request.
     */
    public void doFilter(final ServletRequest  req,
                         final ServletResponse res,
                         final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest request   = (HttpServletRequest) req;

        if (!APILocator.getLoginServiceAPI().isLoggedIn(request)) {
            try {

                final long languageId = this.languageWebAPI.getLanguage(request).getId();
                String uri = request.getRequestURI();
                final Host host = getHost(request, uri);
                final User user = getUser(request);

                final Optional<URLMapInfo> urlMapInfoOptional = this.urlMapAPI.processURLMap(
                        UrlMapContextBuilder.builder()
                                .setHost(host)
                                .setLanguageId(languageId)
                                .setMode(PageMode.LIVE)
                                .setUri(uri)
                                .setUser(user)
                                .build()
                );

                if (urlMapInfoOptional.isPresent()) {
                    final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
                    request.setAttribute(WebKeys.WIKI_CONTENTLET, urlMapInfo.getContentlet().getIdentifier());
                    request.setAttribute(WebKeys.WIKI_CONTENTLET_INODE, urlMapInfo.getContentlet().getInode());
                    request.setAttribute(WebKeys.WIKI_CONTENTLET_URL, uri);
                    request.setAttribute(WebKeys.CLICKSTREAM_IDENTIFIER_OVERRIDE, urlMapInfo.getContentlet().getIdentifier());
                    request.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE, urlMapInfo.getIdentifier().getURI());
                }

            } catch (Exception e) {

                Logger.error(URLMapFilter.class, e.getMessage(), e);
                if (ExceptionUtil.causedBy(e, MalformedPatternException.class)) {
                    chain.doFilter(req, res);
                }
            }
        }

        chain.doFilter(req, res);
    }


    @Nullable
    private User getUser(final HttpServletRequest request) {
        User user = null;
        try {
            user = wuserAPI.getLoggedInUser(request);
        } catch (Exception e1) {
            Logger.error(URLMapFilter.class, e1.getMessage(), e1);
        }
        return user;
    }

    private Host getHost(final HttpServletRequest request, final String uri) throws ServletException {

        Host host;
        try {
            host = this.whostAPI.getCurrentHost(request);
        } catch (Exception e) {
            Logger.warn(this, "Unable to retrieve current request host for URI " + uri);
            throw new ServletException(e.getMessage(), e);
        }
        return host;
    }

    public void init(FilterConfig config) throws ServletException {

        Config.setMyApp(config.getServletContext());
        this.wuserAPI       = WebAPILocator.getUserWebAPI();
        this.whostAPI       = WebAPILocator.getHostWebAPI();
        this.languageWebAPI = WebAPILocator.getLanguageWebAPI();
        this.urlMapAPI = APILocator.getURLMapAPI();
        // persistant on disk cache makes this necessary
        CacheLocator.getContentTypeCache().clearURLMasterPattern();
    }
}