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

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.web.HostWebAPI;
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
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.TagUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This filter handles all requests regarding URL Maps. These URL maps on
 * content structures are used to create friendly URLs for SEO.
 *
 * @author root
 * @version 1.2
 * @since 03-22-2012
 */
public class URLMapFilter implements Filter {

    private List<PatternCache> patternsCache = new ArrayList<>();
    private ContentletAPI conAPI;
    private UserWebAPI wuserAPI;
    private HostWebAPI whostAPI;
    private boolean urlFallthrough;
    private CMSUrlUtil cmsUrlUtil = CMSUrlUtil.getInstance();

    public void destroy() {

    }

    /**
     * Runs the filter validations on the current request.
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException,
            ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        HttpSession optSession = request.getSession(false);

        String uri = cmsUrlUtil.getURIFromRequest(request);

        String previewPage = request.getParameter("previewPage");
        long languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();

		/*
         * Getting host object form the session
		 */
        Host host;
        try {
            host = whostAPI.getCurrentHost(request);
        } catch (Exception e) {
            Logger.warn(this, "Unable to retrieve current request host for URI " + uri);
            throw new ServletException(e.getMessage(), e);
        }

        User user = null;
        try {
            user = wuserAPI.getLoggedInUser(request);
        } catch (Exception e1) {
            Logger.error(URLMapFilter.class, e1.getMessage(), e1);
        }

        // http://jira.dotmarketing.net/browse/DOTCMS-6079
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }

        String mastRegEx = null;
        StringBuilder query;
        try {
            mastRegEx = CacheLocator.getContentTypeCache().getURLMasterPattern();
        } catch (DotCacheException e2) {
            Logger.error(URLMapFilter.class, e2.getMessage(), e2);
        }
        if (mastRegEx == null || patternsCache.isEmpty()) {
            synchronized (ContentTypeCache.MASTER_STRUCTURE) {
                try {
                    mastRegEx = buildCacheObjects();
                } catch (DotDataException e) {
                    Logger.error(URLMapFilter.class, e.getMessage(), e);
                    throw new ServletException("Unable to build URLMap patterns", e);
                }
            }
        }
        boolean trailSlash = uri.endsWith("/");
        boolean isDotPage = cmsUrlUtil.isPageAsset(uri, host, languageId);

        String url = (!trailSlash && !isDotPage) ? uri + '/' : uri;
        if (!UtilMethods.isSet(mastRegEx) || uri.startsWith("/webdav")) {
            chain.doFilter(req, res);
            return;
        }
        if (RegEX.contains(url, mastRegEx)) {
            boolean ADMIN_MODE = (optSession != null
                    && optSession.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION)
                    != null);
            boolean EDIT_MODE = ((optSession != null
                    && optSession.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION)
                    != null) && ADMIN_MODE);

            Structure structure = null;

            List<ContentletSearch> cons = null;
            for (PatternCache pc : patternsCache) {
                List<RegExMatch> matches = RegEX.findForUrlMap(url, pc.getRegEx());
                if (matches != null && matches.size() > 0) {
                    query = new StringBuilder();
                    List<RegExMatch> groups = matches.get(0).getGroups();
                    List<String> fieldMatches = pc.getFieldMatches();
                    structure = CacheLocator.getContentTypeCache()
                            .getStructureByInode(pc.getStructureInode());
                    List<Field> fields = FieldsCache
                            .getFieldsByStructureInode(structure.getInode());
                    query.append("+structureName:").append(structure.getVelocityVarName())
                            .append(" +deleted:false ");
                    if ((EDIT_MODE || ADMIN_MODE) && UtilMethods.isSet(previewPage)) {
                        query.append("+working:true ");
                    } else {
                        query.append("+live:true ");
                    }

                    // Set Host Stuff
                    boolean hasHostField = false;
                    Boolean hostIsRequired = false;
                    for (Field field : fields) {
                        if (field.getFieldType()
                                .equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                            hasHostField = true;
                            if (field.isRequired()) {
                                hostIsRequired = true;
                            }
                            break;
                        }
                    }
                    if (hasHostField) {
                        if (host != null) {
                            //if (hostIsRequired) {
                            //query.append("+conhost:" + host.getIdentifier() + " ");
                            //} else {
                            try {
                                query.append("+(conhost:").append(host.getIdentifier()).append(" ")
                                        .append("conhost:").append(whostAPI
                                        .findSystemHost(wuserAPI.getSystemUser(), true)
                                        .getIdentifier()).append(") ");
                            } catch (Exception e) {
                                Logger.error(URLMapFilter.class, e.getMessage()
                                        + " : Unable to build host in query : ", e);
                            }
                            //}
                        }
                    }

                    // build fields
                    int counter = 0;
                    for (RegExMatch regExMatch : groups) {
                        String value = regExMatch.getMatch();
                        if (value.endsWith("/")) {
                            value = value.substring(0, value.length() - 1);
                        }
                        query.append("+").append(structure.getVelocityVarName()).append(".")
                                .append(fieldMatches.get(counter)).append(":")
                                .append(value).append(" ");
                        counter++;
                    }

                    try {
                        long sessionLang = WebAPILocator.getLanguageWebAPI().getLanguage(request)
                                .getId();
                        long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
                        boolean checkIndex = false;

                        if (request.getParameter("language_id") == null && Config
                                .getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false)) {
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

                        cons = conAPI.searchIndex(query.toString(), 2, 0,
                                (hostIsRequired ? "conhost, modDate" : "modDate"), wuserAPI.getSystemUser(), true);
                        int idx = 0;
                        if (checkIndex && cons.size() == 2) {
                            // prefer session setting
                            Contentlet second = conAPI.find(cons.get(1).getInode(), wuserAPI.getSystemUser(), true);
                            if (second.getLanguageId() == sessionLang) {
                                idx = 1;
                            }
                        }
                        ContentletSearch c = cons.get(idx);
                        Contentlet contentlet = conAPI
                                .find(c.getInode(), wuserAPI.getSystemUser(), true);

                        if (optSession != null) {
                            optSession.setAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE,
                                    String.valueOf(contentlet.getLanguageId()));
                        }

                        //Verify and handle the case for unauthorized access of this contentlet
                        Boolean unauthorized = CMSUrlUtil.getInstance()
                                .isUnauthorizedAndHandleError(contentlet, uri, user, request,
                                        response);
                        if (unauthorized) {
                            return;
                        }

                        request.setAttribute(WebKeys.WIKI_CONTENTLET, c.getIdentifier());
                        request.setAttribute(WebKeys.WIKI_CONTENTLET_INODE, c.getInode());
                        request.setAttribute(WebKeys.CLICKSTREAM_IDENTIFIER_OVERRIDE,
                                c.getIdentifier());
                        request.setAttribute(WebKeys.WIKI_CONTENTLET_URL, url);
                        String[] x = url.split("/");
                        for (int i = 0; i < x.length; i++) {
                            if (UtilMethods.isSet(x[i])) {
                                request.setAttribute("URL_ARG" + i, x[i]);
                            }
                        }

                        //Check if we want to accrue the tags of URL maps
                        if (Config.getBooleanProperty("ACCRUE_TAGS_IN_URLMAPS", true)) {

                            //Search for the tags asocciated to this contentlet inode
                            List<Tag> contentletFoundTags = APILocator.getTagAPI()
                                    .getTagsByInode(c.getInode());
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
                    } catch (java.lang.IndexOutOfBoundsException iob) {
                        Logger.warn(this,
                                "No urlmap contentlent found uri:" + url + " query:" + query
                                        .toString());
                    } catch (Exception e) {
                        Logger.warn(this, "No index?" + e.getMessage());
                    }
                }
            }

            if (structure != null && UtilMethods.isSet(structure.getDetailPage())) {
                Identifier ident;
                try {
                    ident = APILocator.getIdentifierAPI().find(structure.getDetailPage());
                    if (ident == null || !UtilMethods.isSet(ident.getInode())) {
                        throw new DotRuntimeException(
                                "No valid detail page for structure '" + structure.getName()
                                        + "'. Looking for detail page id=" + structure
                                        .getDetailPage());
                    }

                    if ((cons != null && cons.size() > 0) || !urlFallthrough) {

                        request.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE, ident.getURI());

                    }

                } catch (Exception e) {
                    Logger.error(URLMapFilter.class, e.getMessage(), e);
                }
            }

        }
        chain.doFilter(req, res);
    }

    public void init(FilterConfig config) throws ServletException {
        Config.setMyApp(config.getServletContext());
        conAPI = APILocator.getContentletAPI();
        wuserAPI = WebAPILocator.getUserWebAPI();
        whostAPI = WebAPILocator.getHostWebAPI();
        // persistant on disk cache makes this necessary
        CacheLocator.getContentTypeCache().clearURLMasterPattern();
        urlFallthrough = Config.getBooleanProperty("URLMAP_FALLTHROUGH", true);
    }

    /**
     * Builds the list of URL maps and sorts them by the number of slashes in
     * the URL (highest to lowest). This method is called only when a new URL
     * map is added, and is marked as <code>synchronized</code> to avoid data
     * inconsistency.
     *
     * @return A <code>String</code> containing a Regex, which contains all the URL maps in the
     * system.
     * @throws DotDataException An error occurred when retrieving information from the database.
     */
    private synchronized String buildCacheObjects() throws DotDataException {
        List<SimpleStructureURLMap> urlMaps = StructureFactory.findStructureURLMapPatterns();
        StringBuilder masterRegEx = new StringBuilder();
        boolean first = true;
        patternsCache.clear();
        for (SimpleStructureURLMap urlMap : urlMaps) {
            PatternCache pc = new PatternCache();
            String regEx = StructureUtil.generateRegExForURLMap(urlMap.getURLMapPattern());
            // if we have an empty string, move on
            if (!UtilMethods.isSet(regEx) || regEx.trim().length() < 3) {
                continue;

            }
            pc.setRegEx(regEx);
            pc.setStructureInode(urlMap.getInode());
            pc.setURLpattern(urlMap.getURLMapPattern());
            List<RegExMatch> fieldMathed = RegEX.find(urlMap.getURLMapPattern(), "{([^{}]+)}");
            List<String> fields = new ArrayList<String>();
            for (RegExMatch regExMatch : fieldMathed) {
                fields.add(regExMatch.getGroups().get(0).getMatch());
            }
            pc.setFieldMatches(fields);
            patternsCache.add(pc);
            if (!first) {
                masterRegEx.append("|");
            }
            masterRegEx.append(regEx);
            first = false;
        }
        Collections.sort(this.patternsCache, new Comparator<PatternCache>() {
            public int compare(PatternCache o1, PatternCache o2) {
                String regex1 = o1.getRegEx();
                String regex2 = o2.getRegEx();
                if (!regex1.endsWith("/")) {
                    regex1 += "/";
                }
                if (!regex2.endsWith("/")) {
                    regex2 += "/";
                }
                int regExLength1 = getSlashCount(regex1);
                int regExLength2 = getSlashCount(regex2);
                if (regExLength1 < regExLength2) {
                    return 1;
                } else if (regExLength1 > regExLength2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        CacheLocator.getContentTypeCache().addURLMasterPattern(masterRegEx.toString());
        return masterRegEx.toString();
    }

    private class PatternCache {

        private String regEx;
        private String structureInode;
        private String URLpattern;
        private List<String> fieldMatches;

        public void setRegEx(String regEx) {
            this.regEx = regEx;
        }

        public String getRegEx() {
            return regEx;
        }

        public void setStructureInode(String structureInode) {
            this.structureInode = structureInode;
        }

        public String getStructureInode() {
            return structureInode;
        }

        public void setURLpattern(String uRLpattern) {
            URLpattern = uRLpattern;
        }

        @SuppressWarnings("unused")
        public String getURLpattern() {
            return URLpattern;
        }

        public void setFieldMatches(List<String> fieldMatches) {
            this.fieldMatches = fieldMatches;
        }

        public List<String> getFieldMatches() {
            return fieldMatches;
        }
    }

    private int getSlashCount(String string) {
        int ret = 0;
        if (UtilMethods.isSet(string)) {
            for (int i = 0; i < string.length(); i++) {
                if (string.charAt(i) == '/') {
                    ret += 1;
                }
            }
        }
        return ret;
    }
}