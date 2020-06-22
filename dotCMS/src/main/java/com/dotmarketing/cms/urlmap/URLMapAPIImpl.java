package com.dotmarketing.cms.urlmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

/**
 * {@link URLMapAPI} implementation
 *
 * {@inheritDoc}
 */
public class URLMapAPIImpl implements URLMapAPI {

    private volatile Collection<ContentTypeURLPattern> patternsCache;
    private final UserWebAPI wuserAPI = WebAPILocator.getUserWebAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
    private final ContentTypeAPI typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
    public boolean isUrlPattern(final UrlMapContext urlMapContext)
            throws DotDataException, DotSecurityException {
        return matchingUrlPattern(urlMapContext.getUri()) && getContentlet(urlMapContext) != null;
    }

    public Optional<URLMapInfo> processURLMap(final UrlMapContext context)
            throws DotSecurityException, DotDataException {

        final Contentlet contentlet = getContentlet(context);
        if (contentlet == null) {
            return Optional.empty();
        }

        final ContentType contentType = contentlet.getContentType();
        final Optional<Identifier> optDetailIdentifier = this.getDetailtPageUri(contentType, context.getHost());

        if(!optDetailIdentifier.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new URLMapInfo(contentlet, optDetailIdentifier.get(), context.getUri()));
    }

    /**
     * Return the {@link Contentlet} the match the {@link UrlMapContext#getUri()} value, if not
     * exists any {@link com.dotcms.contenttype.model.type.UrlMapable} matching with the URI then a
     * {@link DotRuntimeException} is thrown
     *
     * @param urlMapContext
     * @return
     */
    private Contentlet getContentlet(final UrlMapContext urlMapContext) throws DotSecurityException {

        Contentlet matchingContentlet = null;

        try {
            // We could have multiple matches as multiple content types could have the same
            // URLMap pattern and we need to evaluate all until we find content match.
            final List<Matches> matchesFound = this.findMatch(urlMapContext.getUri());
            if (!matchesFound.isEmpty()) {

                for (final Matches matches : matchesFound) {
                    final ContentType contentType = typeAPI.find(matches.getPatternChange().getStructureInode());

                    matchingContentlet = this
                            .getContentlet(matches, contentType, urlMapContext);
                    if (null != matchingContentlet) {
                        break;
                    }
                }

            }
        } catch (DotDataException e) {
            Logger.error(this.getClass(),
                    String.format("Error processing URL [%s]", urlMapContext.getUri()), e);
            return null;
        }

        return matchingContentlet;
    }

    private Optional<Identifier> getDetailtPageUri(final ContentType contentType, Host currentHost) {
        if (contentType == null || UtilMethods.isEmpty(contentType.detailPage())) {
            return Optional.empty();
        }
        
        try {
            final Identifier identifier = this.identifierAPI.find(contentType.detailPage());
            if (identifier == null || !UtilMethods.isSet(identifier.getId())) {
                Logger.info(this.getClass(),
                        "No valid detail page for structure '" + contentType.name()
                                + "'. Looking for detail page id=" + contentType
                                .detailPage());
                return Optional.empty();
            }
            
            //if the detail page is on this host, send it!
            if(identifier.getHostId().equals(currentHost.getIdentifier())) {
                return Optional.of(identifier);
            }
            
            // look for it on the current host
            final Identifier myHostIdentifier = this.identifierAPI.find(currentHost, identifier.getPath());
            if (myHostIdentifier == null || !UtilMethods.isSet(myHostIdentifier.getId())) {
                Logger.info(this.getClass(),
                        "No valid detail page for structure '" + contentType.name()
                                + "'. Looking for a detail page=" + identifier.getPath() + " on host " + currentHost.getHostname());
                return Optional.empty();
            }

            return Optional.of(myHostIdentifier);
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), e);
            return Optional.empty();
        }
    } 
    

    /**
     * Return all the matches related to a given URI, multiple content types could use the URLMap
     * pattern and on those cases we need to evaluate all the matches.
     *
     * @param uri URI to evaluate for matches
     * @return List of found matches
     * @throws DotDataException
     */
    private List<Matches> findMatch(final String uri) throws DotDataException {

        // We want to avoid unnecessary lookups for vanity urls when browsing in the backend
        final boolean filtered = CMSUrlUtil.BACKEND_FILTERED_COLLECTION.stream()
                .anyMatch(uri::startsWith);
        if (filtered) {
            return Collections.emptyList();
        }

        if (this.shouldLoadPatterns()) {
            this.loadPatterns();
        }

        final List<Matches> foundMatches = new ArrayList<>();

        final String url =
                !uri.endsWith(StringPool.FORWARD_SLASH) ? uri + StringPool.FORWARD_SLASH : uri;

        for (final ContentTypeURLPattern contentTypeURLPattern : this.patternsCache) {

            final List<RegExMatch> matches = RegEX
                    .findForUrlMap(url, contentTypeURLPattern.getRegEx());
            if (matches != null && !matches.isEmpty()) {

                /*
                We need to make sure we have an exact match, we could have regex too generic, like
                a regex in the root: "/{urlTitle}" resulting in a regex like "/(.+)/" which basically
                will match any url.
                 */
                for (final RegExMatch regExMatch : matches) {
                    if (regExMatch.getMatch().equals(url)) {
                        foundMatches.add(new Matches(contentTypeURLPattern, matches));
                    }
                }

            }
        }

        return foundMatches;
    }

    private boolean matchingUrlPattern(final String uri) throws DotDataException {
        final List<Matches> foundMatches = findMatch(uri);
        return !foundMatches.isEmpty();
    }


    private String buildFields(final ContentType structure,
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
            query.append('+').append(structure.variable()).append('.')
                    .append(fieldMatches.get(counter)).append("_dotRaw").append(':')
                    .append(ESUtils.escapeExcludingSlashIncludingSpace(value)).append(' ');
            counter++;
        }

        return query.toString();
    }

    private Contentlet getContentlet(
            final Matches matches,
            final ContentType contentType,
            final UrlMapContext context)
             throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;

        final String query = this.buildContentQuery(matches, contentType, context);
        final List<Contentlet> contentletSearches =
                ContentUtils.pull(query, 0, 2, "score",this.wuserAPI.getSystemUser(), true);

        if (!contentletSearches.isEmpty()) {
            int idx = 0;
            if (contentletSearches.size() == 2) {
                // prefer session setting
                final Contentlet second = contentletSearches.get(1);
                if (second.getLanguageId() == context.getLanguageId()) {
                    idx = 1;
                }
            }

            contentlet = contentletSearches.get(idx);
            checkContentPermission(context, contentlet);
        }

        return contentlet;
    }

    private void checkContentPermission(final UrlMapContext context, final Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        final boolean havePermission = this.permissionAPI.doesUserHavePermission(
                contentlet, PermissionLevel.READ.getType(), context.getUser(), context.getMode().respectAnonPerms);

        if (!havePermission) {
            throw new DotSecurityException(String.format("User does not have permission in content: %s", contentlet.getName()));
        }
    }

    private String buildContentQuery(
            final Matches matches,
            final ContentType structure,
            final UrlMapContext context) {

        final StringBuilder query = new StringBuilder();

        query.append("+contentType:")
            .append(structure.variable())
            .append(" +deleted:false ")
            .append(" +(conhost:")
                .append(context.getHost().getIdentifier())
                .append(" OR conhost:")
                .append(Host.SYSTEM_HOST)
            .append(")");
        

        if (context.getMode().showLive) {
            query.append(" +live:true ");
        } else {
            query.append(" +working:true ");
        }



        query.append(" ");
        query.append(this.buildFields(structure, matches));
        
        // score the current language higher
        query.append(" languageId:").append(context.getLanguageId());

        return query.toString();
    }

    private boolean shouldLoadPatterns() {
        String mastRegEx = null;

        try {
            mastRegEx = CacheLocator.getContentTypeCache().getURLMasterPattern();
        } catch (DotCacheException e2) {
            Logger.error(URLMapAPIImpl.class, e2.getMessage(), e2);
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

        final List<SimpleStructureURLMap> urlMaps = typeAPI.findStructureURLMapPatterns();

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
