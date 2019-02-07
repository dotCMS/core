package com.dotmarketing.cms.urlmap.filters;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.jetbrains.annotations.NotNull;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.TagUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

public class URLMapAPI {

    private List<PatternCache> _patternsCache = new ArrayList<>();

    protected List<PatternCache> getPatternsCache() {
        return _patternsCache;
    }

    public URLMapAPI() {
        this.loadURLMapPatterns();

    }

    /**
     * Builds the list of URL maps and sorts them by the number of slashes in the URL (highest to
     * lowest). This method is called only when a new URL map is added, and is marked as
     * <code>synchronized</code> to avoid data inconsistency.
     *
     * @return A <code>String</code> containing a Regex, which contains all the URL maps in the system.
     * @throws DotDataException An error occurred when retrieving information from the database.
     */
    private synchronized String buildCacheObjects() throws DotDataException {
        List<SimpleStructureURLMap> urlMaps = APILocator.getContentTypeAPI(APILocator.systemUser()).findStructureURLMapPatterns();
        StringBuilder masterRegEx = new StringBuilder();
        boolean first = true;
        List<PatternCache> patterns = new ArrayList<>();

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
            patterns.add(pc);
            if (!first) {
                masterRegEx.append("|");
            }
            masterRegEx.append(regEx);
            first = false;
        }

        Collections.sort(patterns, new Comparator<URLMapAPI.PatternCache>() {
            public int compare(URLMapAPI.PatternCache patternCache1, URLMapAPI.PatternCache patternCache2) {
                String regex1 = patternCache1.getRegEx();
                String regex2 = patternCache2.getRegEx();

                if (!regex1.endsWith(StringPool.FORWARD_SLASH)) {
                    regex1 += StringPool.FORWARD_SLASH;
                }

                if (!regex2.endsWith(StringPool.FORWARD_SLASH)) {
                    regex2 += StringPool.FORWARD_SLASH;
                }

                final int regExLength1 = getSlashCount(regex1);
                final int regExLength2 = getSlashCount(regex2);

                if (regExLength1 < regExLength2) {
                    return 1;
                } else if (regExLength1 > regExLength2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        this._patternsCache = ImmutableList.copyOf(patterns);
        CacheLocator.getContentTypeCache().addURLMasterPattern(masterRegEx.toString());
        return masterRegEx.toString();
    }

    protected class PatternCache {

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

    @NotNull
    protected String loadURLMapPatterns() {

        String mastRegEx = null;
        try {
            mastRegEx = CacheLocator.getContentTypeCache().getURLMasterPattern();
        } catch (DotCacheException e2) {
            Logger.error(URLMapFilter.class, e2.getMessage(), e2);
        }

        if (mastRegEx == null || _patternsCache.isEmpty()) {
            synchronized (ContentTypeCache.MASTER_STRUCTURE) {
                try {
                    mastRegEx = buildCacheObjects();
                } catch (DotDataException e) {
                    Logger.error(URLMapFilter.class, e.getMessage(), e);
                    throw new DotRuntimeException("Unable to build URLMap patterns", e);
                }
            }
        }
        return mastRegEx;
    }


    public Optional<URLMapInfo> getUrlMapInfo(final String previewPage, final long languageId, final String uri, final Host host,
            final User user, final String url, final PageMode pageMode) {

        StringBuilder query;
        final URLMapAPI urlMapAPI = new URLMapAPI();
        if (RegEX.contains(url, loadURLMapPatterns())) {

            final boolean adminMode =pageMode.isAdmin;
            final boolean editMode = pageMode == PageMode.EDIT_MODE;
            ContentType type = null;
            List<ContentletSearch> contentletSearches = null;

            for (final URLMapAPI.PatternCache patternCache : urlMapAPI.getPatternsCache()) {

                final List<RegExMatch> matches = RegEX.findForUrlMap(url, patternCache.getRegEx());
                if (matches != null && matches.size() > 0) {

                    query = new StringBuilder();
                    final List<RegExMatch> groups = matches.get(0).getGroups();
                    final List<String> fieldMatches = patternCache.getFieldMatches();
                    type = CacheLocator.getContentTypeCache().byVarOrInode(patternCache.getStructureInode());
                    final List<Field> fields = type.fields();

                    query.append("+structureName:").append(type.variable()).append(" +deleted:false ");

                    if ((editMode || adminMode) && UtilMethods.isSet(previewPage)) {
                        query.append("+working:true ");
                    } else {
                        query.append("+live:true ");
                    }

                    // Set Host Stuff
                    final Field hostField = this.findHostField(fields);
                    final boolean hasHostField = null != hostField;
                    final boolean hostIsRequired = null != hostField ? hostField.required() : false;

                    query.append(this.setHostQuery(host, hasHostField));
                    this.buildFields(query, type, groups, fieldMatches);

                    try {

                        final long sessionLang = languageId;
                        final long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
                        boolean checkIndex = false;



                        contentletSearches = this.contentletAPI.searchIndex(query.toString(), 2, 0,
                                hostIsRequired ? "conhost, modDate" : "modDate", APILocator.systemUser(), true);

                        int idx = 0;
                        if (checkIndex && contentletSearches.size() == 2) {
                            // prefer session setting
                            final Contentlet second =
                                    this.contentletAPI.find(contentletSearches.get(1).getInode(), APILocator.systemUser(), true);
                            if (second.getLanguageId() == sessionLang) {
                                idx = 1;
                            }
                        }

                        final ContentletSearch contentletSearch = contentletSearches.get(idx);
                        final Contentlet contentlet =
                                this.contentletAPI.find(contentletSearch.getInode(), APILocator.systemUser(), true);

                        if (optSession != null) {
                            optSession.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(contentlet.getLanguageId()));
                        }

                        // Verify and handle the case for unauthorized access of this contentlet
                        if (this.cmsUrlUtil.isUnauthorizedAndHandleError(contentlet, uri, user, request, response)) {
                            return false;
                        }

                        request.setAttribute(WebKeys.WIKI_CONTENTLET, contentletSearch.getIdentifier());
                        request.setAttribute(WebKeys.WIKI_CONTENTLET_INODE, contentletSearch.getInode());
                        request.setAttribute(WebKeys.WIKI_CONTENTLET_URL, url);
                        request.setAttribute(WebKeys.CLICKSTREAM_IDENTIFIER_OVERRIDE, contentletSearch.getIdentifier());

                        final String[] urlTokens = url.split("/");
                        for (int i = 0; i < urlTokens.length; i++) {
                            if (UtilMethods.isSet(urlTokens[i])) {
                                request.setAttribute("URL_ARG" + i, urlTokens[i]);
                            }
                        }

                        // Check if we want to accrue the tags of URL maps
                        if (Config.getBooleanProperty("ACCRUE_TAGS_IN_URLMAPS", true)) {

                            // Search for the tags asocciated to this contentlet inode
                            final List<Tag> contentletFoundTags = this.tagAPI.getTagsByInode(contentletSearch.getInode());
                            if (contentletFoundTags != null) {
                                // Accrue the found tags
                                TagUtil.accrueTags(request, contentletFoundTags);
                            }
                        }

                        break;
                    } catch (DotDataException e) {
                        Logger.warn(this, "DotDataException", e);
                    } catch (DotSecurityException e) {
                        Logger.warn(this, "DotSecurityException", e);
                    } catch (IndexOutOfBoundsException iob) {
                        Logger.warn(this, "No urlmap contentlent found uri:" + url + " query:" + query.toString());
                    } catch (Exception e) {
                        Logger.warn(this, "No index?" + e.getMessage());
                    }
                }
            }

            if (structure != null && UtilMethods.isSet(structure.getDetailPage())) {

                try {

                    final Identifier identifier = this.identifierAPI.find(structure.getDetailPage());
                    if (identifier == null || !UtilMethods.isSet(identifier.getInode())) {
                        throw new DotRuntimeException("No valid detail page for structure '" + structure.getName()
                                + "'. Looking for detail page id=" + structure.getDetailPage());
                    }

                    if (UtilMethods.isSet(contentletSearches) || !urlFallthrough) {

                        request.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE, identifier.getURI());
                    }
                } catch (Exception e) {
                    Logger.error(URLMapFilter.class, e.getMessage(), e);
                }
            }
        }

        return true;
    }

    private Field findHostField(final List<Field> fields) {

        for (final Field field : fields) {
            if (field instanceof HostFolderField) {
                return field;
            }
        }

        return null;
    }

    private String setHostQuery(final Host host, final boolean hasHostField) {
        StringWriter query = new StringWriter();
        if (hasHostField && host != null) {
            try {
                query.append("+(conhost:").append(host.getIdentifier()).append(" ").append("conhost:")
                        .append(APILocator.systemHost().getIdentifier()).append(") ");
            } catch (Exception e) {
                Logger.error(URLMapFilter.class, e.getMessage() + " : Unable to build host in query : ", e);
            }
        }
        return query.toString();
    }

    private void buildFields(final StringBuilder query, final ContentType type, final List<RegExMatch> groups,
            final List<String> fieldMatches) {

        int counter = 0;
        for (final RegExMatch regExMatch : groups) {

            String value = regExMatch.getMatch();
            if (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            query.append("+").append(type.variable()).append(".").append(fieldMatches.get(counter)).append(":")
                    .append(QueryParser.escape(value)).append(" ");
            counter++;
        }
    }
}
