package com.dotmarketing.cms.urlmap;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cms.urlmap.filters.URLMapFilter;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.jetbrains.annotations.NotNull;

/**
 * {@link URLMapAPI} implementation
 *
 * {@inheritDoc}
 */
public class URLMapAPIImpl implements URLMapAPI {

    @VisibleForTesting
    volatile Collection<ContentTypeURLPattern> patternsCache;

    private final UserWebAPI wuserAPI = WebAPILocator.getUserWebAPI();
    private final HostWebAPI whostAPI = WebAPILocator.getHostWebAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();;
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

    /**
     * Return true if any {@link com.dotcms.contenttype.model.type.UrlMapable} matching with
     * {@link UrlMapContext#getUri()}, in otherwise return false
     *
     * @param urlMapContext
     * @return
     * @throws DotDataException
     */
    public boolean isUrlPattern(final UrlMapContext urlMapContext) throws DotDataException {
        return matchingUrlPattern(urlMapContext.getUri()) && getContentlet(urlMapContext) != null;
    }

    public Optional<URLMapInfo> processURLMap(final UrlMapContext context)
            throws DotSecurityException, DotDataException {

        if (this.matchingUrlPattern(context.getUri())) {
            final Matches matches = this.findPatternChange(context.getUri());

            final Structure structure = CacheLocator.getContentTypeCache()
                    .getStructureByInode(matches.getPatternChange().getStructureInode());

            final Field hostField = this.findHostField(structure);

            final Contentlet contentlet = this.getContentlet(matches, structure, hostField, context);

            if (contentlet == null) {
                return Optional.empty();
            }

            final Identifier pageUriIdentifier = this.getDetailtPageUri(structure);

            return Optional.of(new URLMapInfo(contentlet, pageUriIdentifier));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Return the {@link Contentlet} the match the {@link UrlMapContext#getUri()} value,
     * if not exists any {@link com.dotcms.contenttype.model.type.UrlMapable} matching with the URI
     * then a {@link DotRuntimeException} is thrown
     *
     * @param urlMapContext
     * @return
     */
    private Contentlet getContentlet(final UrlMapContext urlMapContext){
        final Matches matches = this.findPatternChange(urlMapContext.getUri());
        final Structure structure = CacheLocator.getContentTypeCache()
                .getStructureByInode(matches.getPatternChange().getStructureInode());

        final Field hostField = this.findHostField(structure);

        try {
            return this.getContentlet(matches, structure, hostField, urlMapContext);
        } catch (DotDataException | DotSecurityException e){
            return null;
        }
    }

    private boolean matchingUrlPattern(final String uri) throws DotDataException {
        if (this.shouldLoadPatterns()) {
            this.loadPatterns();
        }

        return containsRegEx(uri);
    }

    private Identifier getDetailtPageUri(final Structure structure) {
        if (structure != null && UtilMethods.isSet(structure.getDetailPage())) {

            try {

                final Identifier identifier = this.identifierAPI.find(structure.getDetailPage());
                if (identifier == null || !UtilMethods.isSet(identifier.getInode())) {
                    throw new DotRuntimeException(
                            "No valid detail page for structure '" + structure.getName()
                                    + "'. Looking for detail page id=" + structure
                                    .getDetailPage());
                }

                return identifier;
            } catch (Exception e) {
                throw new DotRuntimeException(e);
            }
        } else {
            return null;
        }
    }

    private boolean containsRegEx(final String uri) {
        try {
            final String mastRegEx = CacheLocator.getContentTypeCache().getURLMasterPattern();

            final String url = !uri.endsWith(StringPool.FORWARD_SLASH) ? uri + StringPool.FORWARD_SLASH : uri;
            return mastRegEx != null && RegEX.contains(url, mastRegEx);
        } catch (DotCacheException e) {
            throw new DotRuntimeException(e);
        }
    }

    private Matches findPatternChange(final String uri) {
        final String url = !uri.endsWith(StringPool.FORWARD_SLASH) ? uri + StringPool.FORWARD_SLASH : uri;

        for (final ContentTypeURLPattern contentTypeURLPattern : this.patternsCache) {

            final List<RegExMatch> matches = RegEX.findForUrlMap(url, contentTypeURLPattern.getRegEx());
            if (matches != null && !matches.isEmpty()) {
                return new Matches(contentTypeURLPattern, matches);
            }
        }

        throw new DotRuntimeException("Not pattern match found");
    }

    private Field findHostField(final Structure structure) {
        return FieldsCache.getFieldsByStructureInode(structure.getInode()).stream()
                .filter(field -> field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString()))
                .findFirst()
                .orElse(null);
    }

    private String getHostFilter(final Host host) {
        try {
            final Host systemHost = this.whostAPI.findSystemHost(this.wuserAPI.getSystemUser(), true);
            return String.format("+(conhost: %s conhost: %s)", host.getIdentifier(), systemHost.getIdentifier());
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(URLMapFilter.class, e.getMessage()
                    + " : Unable to build host in query : ", e);
            return "";
        }
    }

    private String buildFields(final Structure structure,
            final Matches matches) {

        final StringBuilder query = new StringBuilder();
        final List<RegExMatch> groups = matches.getMatches().get(0).getGroups();
        final List<String> fieldMatches = matches.getPatternChange().getFieldMatches();

        int counter = 0;
        for (final RegExMatch regExMatch : groups) {

            String value = regExMatch.getMatch();
            if (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            query.append('+').append(structure.getVelocityVarName()).append('.')
                    .append(fieldMatches.get(counter)).append("_dotRaw").append(':')
                    .append(QueryParser.escape(value)).append(' ');
            counter++;
        }

        return query.toString();
    }

    private Contentlet getContentlet(
            final Matches matches,
            final Structure structure,
            final Field hostField,
            final UrlMapContext context)
             throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;

        final String query = this.buildContentQuery(matches, structure, hostField, context);
        final List<ContentletSearch> contentletSearches =
                this.contentletAPI.searchIndex(query, 2, 0,
                        (hostField!=null && hostField.isRequired()) ? "conhost, modDate" : "modDate",
                        this.wuserAPI.getSystemUser(), true);

        if (!contentletSearches.isEmpty()) {
            int idx = 0;
            if (contentletSearches.size() == 2) {
                // prefer session setting
                final Contentlet second = this.contentletAPI
                        .find(contentletSearches.get(1).getInode(),
                                this.wuserAPI.getSystemUser(), true);
                if (second.getLanguageId() == context.getLanguageId()) {
                    idx = 1;
                }
            }

            final ContentletSearch contentletSearch = contentletSearches.get(idx);
            contentlet = this.contentletAPI
                    .find(contentletSearch.getInode(), this.wuserAPI.getSystemUser(), true);

            checkContentPermission(context, contentlet);
        }

        return contentlet;
    }

    private void checkContentPermission(final UrlMapContext context, final Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        final boolean havePermission = this.permissionAPI.doesUserHavePermission(
                contentlet, PermissionLevel.READ.getType(), context.getUser(), context.getMode().respectAnonPerms);

        if (!havePermission) {
            throw new DotSecurityException(String.format("User dont have permission in content: %s", contentlet.getName()));
        }
    }

    private String buildContentQuery(
            final Matches matches,
            final Structure structure,
            final Field hostField,
            final UrlMapContext context) {

        final StringBuilder query = new StringBuilder();

        query.append("+structureName:").append(structure.getVelocityVarName())
             .append(" +deleted:false ");

        if (context.getMode() == PageMode.PREVIEW_MODE || context.getMode() == PageMode.EDIT_MODE) {
            query.append("+working:true ");
        } else {
            query.append("+live:true ");
        }

        if (null != hostField && context.getHost() != null) {
            query.append(this.getHostFilter(context.getHost()));
        }

        query.append(this.buildFields(structure, matches))
             .append(" +languageId:").append(context.getLanguageId());

        return query.toString();
    }

    private boolean shouldLoadPatterns() {
        String mastRegEx = null;

        try {
            mastRegEx = CacheLocator.getContentTypeCache().getURLMasterPattern();
        } catch (DotCacheException e2) {
            Logger.error(URLMapFilter.class, e2.getMessage(), e2);
        }

        return mastRegEx == null || patternsCache == null || patternsCache.isEmpty();
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
    private synchronized void loadPatterns() throws DotDataException {
        patternsCache = new ArrayList<>();

        final List<SimpleStructureURLMap> urlMaps = StructureFactory.findStructureURLMapPatterns();

        if (urlMaps != null && !urlMaps.isEmpty()) {
            final StringBuilder masterRegEx = new StringBuilder("^(");

            final int startLength = masterRegEx.length();

            for (final SimpleStructureURLMap urlMap : urlMaps) {
                final String regEx = StructureUtil.generateRegExForURLMap(urlMap.getURLMapPattern());

                if (!UtilMethods.isSet(regEx) || regEx.trim().length() < 3) {
                    continue;
                }

                patternsCache.add(new ContentTypeURLPattern(
                        regEx, urlMap.getInode(),
                        urlMap.getURLMapPattern(), getFieldMathed(urlMap)
                ));

                if (masterRegEx.length() > startLength) {
                    masterRegEx.append('|');
                }

                masterRegEx.append(regEx);
            }

            masterRegEx.append(")");

            CacheLocator.getContentTypeCache().addURLMasterPattern(masterRegEx.toString());
        }
    }

    @NotNull
    private List<String> getFieldMathed(final SimpleStructureURLMap urlMap) {
        final List<RegExMatch> fieldMathed = RegEX.find(urlMap.getURLMapPattern(), "{([^{}]+)}");
        final List<String> fields = new ArrayList<String>();
        for (final RegExMatch regExMatch : fieldMathed) {
            fields.add(regExMatch.getGroups().get(0).getMatch());
        }
        return fields;
    }

    private class Matches {
        final ContentTypeURLPattern patternChange;
        final List<RegExMatch> matches;

        public Matches(final ContentTypeURLPattern patternChange, final List<RegExMatch> matches) {
            this.patternChange = patternChange;
            this.matches = matches;
        }

        public ContentTypeURLPattern getPatternChange() {
            return patternChange;
        }

        public List<RegExMatch> getMatches() {
            return matches;
        }
    }
}
