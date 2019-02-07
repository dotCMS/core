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
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This filter handles all requests regarding URL Maps. These URL maps on
 * content structures are used to create friendly URLs for SEO.
 *
 * @author root
 * @version 1.2
 * @since 03-22-2012
 */
public class URLMapFilter implements Filter {

    public static final String PREVIEW_PAGE = "previewPage";

    private ContentletAPI      contentletAPI;
    private UserWebAPI         wuserAPI;
    private HostWebAPI         whostAPI;
    private LanguageWebAPI     languageWebAPI;
    private LanguageAPI        languageAPI;
    private TagAPI             tagAPI;
    private IdentifierAPI      identifierAPI;
    private boolean            urlFallthrough;
    private final CMSUrlUtil   cmsUrlUtil = CMSUrlUtil.getInstance();


    public void destroy() {

    }

    /**
     * Runs the filter validations on the current request.
     */
    public void doFilter(final ServletRequest  req,
                         final ServletResponse res,
                         final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest request   = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        final HttpSession optSession       = request.getSession(false);
        final String previewPage           = request.getParameter(PREVIEW_PAGE);
        final long languageId              = this.languageWebAPI.getLanguage(request).getId();
        String uri                         = this.cmsUrlUtil.getURIFromRequest(request);
        final Host host                    = getHost(request, uri);
        final User user                    = getUser(request);
        final URLMapAPI urlMapAPI          = new URLMapAPI();
        // http://jira.dotmarketing.net/browse/DOTCMS-6079
        if (uri.endsWith(StringPool.FORWARD_SLASH)) {
            uri = uri.substring(0, uri.length() - 1);
        }

        final String mastRegEx   = urlMapAPI.loadURLMapPatterns();
        final boolean trailSlash = uri.endsWith(StringPool.FORWARD_SLASH);
        final boolean isDotPage  = this.cmsUrlUtil.isPageAsset(uri, host, languageId);
        final String url         = !trailSlash && !isDotPage? uri + StringPool.FORWARD_SLASH : uri;

        // if not URLMAP Pattern or if it is webdav, continue
        if (!UtilMethods.isSet(mastRegEx) || this.cmsUrlUtil.isVanityUrlFiltered(uri)) {
            chain.doFilter(req, res);
            return;
        }

        try {
            if (this.processURLMap(request, response, optSession,
                    previewPage, languageId, uri, host, user, mastRegEx, url)) {
                chain.doFilter(req, res);
            }
        } catch (Exception e) {

            Logger.error(URLMapFilter.class, e.getMessage(), e);
            if (ExceptionUtil.causedBy(e, MalformedPatternException.class)) {
                chain.doFilter(req, res);
            }
        }
    }

    private boolean processURLMap(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  final HttpSession optSession,
                                  final String previewPage,
                                  final long languageId,
                                  final String uri,
                                  final Host host,
                                  final User user,
                                  final String mastRegEx,
                                  final String url) {

        StringBuilder query;
        final URLMapAPI urlMapAPI          = new URLMapAPI();
        if (RegEX.contains(url, mastRegEx)) {

            final boolean adminMode     = PageMode.get(request).isAdmin;
            final boolean editMode      = PageMode.get(request) == PageMode.EDIT_MODE;
            Structure         structure = null;
            List<ContentletSearch> contentletSearches = null;

            for (final URLMapAPI.PatternCache patternCache : urlMapAPI.getPatternsCache()) {

                final List<RegExMatch> matches = RegEX.findForUrlMap(url, patternCache.getRegEx());
                if (matches != null && matches.size() > 0) {

                    query                           = new StringBuilder();
                    final List<RegExMatch> groups   = matches.get(0).getGroups();
                    final List<String> fieldMatches = patternCache.getFieldMatches();
                    structure                       = CacheLocator.getContentTypeCache()
                                                            .getStructureByInode(patternCache.getStructureInode());
                    final List<Field> fields        = FieldsCache
                                                            .getFieldsByStructureInode(structure.getInode());

                    query.append("+structureName:").append(structure.getVelocityVarName())
                            .append(" +deleted:false ");

                    if ((editMode || adminMode) && UtilMethods.isSet(previewPage)) {
                        query.append("+working:true ");
                    } else {
                        query.append("+live:true ");
                    }

                    // Set Host Stuff
                    final Field   hostField       = this.findHostField (fields);
                    final boolean hasHostField    = null != hostField;
                    final boolean hostIsRequired  = null != hostField? hostField.isRequired():false;

                    this.setHostQuery(host, query, hasHostField);
                    this.buildFields (query, structure, groups, fieldMatches);

                    try {

                        final long sessionLang = this.languageWebAPI.getLanguage(request).getId();
                        final long defaultLang = this.languageAPI.getDefaultLanguage().getId();
                        boolean checkIndex = false;

                        if (request.getParameter("language_id") == null && Config
                                .getBooleanProperty(WebKeys.DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false)) {
                            // consider default language. respecting language_id in parameters
                            query.append(" +(languageId:").append(defaultLang)
                                    .append(" languageId:").append(sessionLang).append(") ");
                            checkIndex = true;
                        } else if (request.getParameter("language_id") != null) {
                            query.append(" +languageId:").append(languageId).append(" ");
                        } else {
                            // respect session language
                            query.append(" +languageId:").append(sessionLang).append(" ");
                        }

                        contentletSearches = this.contentletAPI.searchIndex(query.toString(), 2, 0,
                                hostIsRequired? "conhost, modDate" : "modDate", this.wuserAPI.getSystemUser(), true);

                        int idx = 0;
                        if (checkIndex && contentletSearches.size() == 2) {
                            // prefer session setting
                            final Contentlet second = this.contentletAPI.find(contentletSearches.get(1).getInode(),
                                    this.wuserAPI.getSystemUser(), true);
                            if (second.getLanguageId() == sessionLang) {
                                idx = 1;
                            }
                        }

                        final ContentletSearch contentletSearch = contentletSearches.get(idx);
                        final Contentlet contentlet = this.contentletAPI
                                .find(contentletSearch.getInode(), this.wuserAPI.getSystemUser(), true);

                        if (optSession != null) {
                            optSession.setAttribute(WebKeys.HTMLPAGE_LANGUAGE,
                                    String.valueOf(contentlet.getLanguageId()));
                        }

                        //Verify and handle the case for unauthorized access of this contentlet
                        if (this.cmsUrlUtil.isUnauthorizedAndHandleError
                                (contentlet, uri, user, request, response)) {
                            return false;
                        }

                        request.setAttribute(WebKeys.WIKI_CONTENTLET, contentletSearch.getIdentifier());
                        request.setAttribute(WebKeys.WIKI_CONTENTLET_INODE, contentletSearch.getInode());
                        request.setAttribute(WebKeys.WIKI_CONTENTLET_URL, url);
                        request.setAttribute(WebKeys.CLICKSTREAM_IDENTIFIER_OVERRIDE,
                                contentletSearch.getIdentifier());

                        final String[] urlTokens = url.split("/");
                        for (int i = 0; i < urlTokens.length; i++) {
                            if (UtilMethods.isSet(urlTokens[i])) {
                                request.setAttribute("URL_ARG" + i, urlTokens[i]);
                            }
                        }

                        //Check if we want to accrue the tags of URL maps
                        if (Config.getBooleanProperty("ACCRUE_TAGS_IN_URLMAPS", true)) {

                            //Search for the tags asocciated to this contentlet inode
                            final List<Tag> contentletFoundTags =
                                    this.tagAPI.getTagsByInode(contentletSearch.getInode());
                            if (contentletFoundTags != null) {
                                //Accrue the found tags
                                TagUtil.accrueTags(request, contentletFoundTags);
                            }
                        }

                        break;
                    } catch (DotDataException e) {
                        Logger.warn(this, "DotDataException", e);
                    } catch (DotSecurityException e) {
                        Logger.warn(this, "DotSecurityException", e);
                    } catch (IndexOutOfBoundsException iob) {
                        Logger.warn(this,
                                "No urlmap contentlent found uri:" + url + " query:" + query
                                        .toString());
                    } catch (Exception e) {
                        Logger.warn(this, "No index?" + e.getMessage());
                    }
                }
            }

            if (structure != null && UtilMethods.isSet(structure.getDetailPage())) {

                try {

                    final Identifier identifier = this.identifierAPI.find(structure.getDetailPage());
                    if (identifier == null || !UtilMethods.isSet(identifier.getInode())) {
                        throw new DotRuntimeException(
                                "No valid detail page for structure '" + structure.getName()
                                        + "'. Looking for detail page id=" + structure
                                        .getDetailPage());
                    }

                    if (UtilMethods.isSet (contentletSearches) || !urlFallthrough) {

                        request.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE, identifier.getURI());
                    }
                } catch (Exception e) {
                    Logger.error(URLMapFilter.class, e.getMessage(), e);
                }
            }
        }

        return true;
    }

    private void buildFields(final StringBuilder query,
                             final Structure structure,
                             final List<RegExMatch> groups,
                             final List<String> fieldMatches) {

        int counter = 0;
        for (final RegExMatch regExMatch : groups) {

            String value = regExMatch.getMatch();
            if (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            query.append("+").append(structure.getVelocityVarName()).append(".")
                    .append(fieldMatches.get(counter)).append(":")
                    .append(QueryParser.escape(value)).append(" ");
            counter++;
        }
    }

    private void setHostQuery(final Host host,
                              final StringBuilder query,
                              final boolean hasHostField) {

        if (hasHostField && host != null) {

            try {
                query.append("+(conhost:").append(host.getIdentifier()).append(" ")
                        .append("conhost:").append(this.whostAPI
                        .findSystemHost(this.wuserAPI.getSystemUser(), true)
                        .getIdentifier()).append(") ");
            } catch (Exception e) {
                Logger.error(URLMapFilter.class, e.getMessage()
                        + " : Unable to build host in query : ", e);
            }
        }
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
        this.contentletAPI = APILocator.getContentletAPI();
        this.wuserAPI       = WebAPILocator.getUserWebAPI();
        this.whostAPI       = WebAPILocator.getHostWebAPI();
        this.languageWebAPI = WebAPILocator.getLanguageWebAPI();
        this.languageAPI    = APILocator.getLanguageAPI();
        this.tagAPI         = APILocator.getTagAPI();
        this.identifierAPI  = APILocator.getIdentifierAPI();
        // persistant on disk cache makes this necessary
        CacheLocator.getContentTypeCache().clearURLMasterPattern();
        this.urlFallthrough = Config.getBooleanProperty("URLMAP_FALLTHROUGH", true);
    }


}