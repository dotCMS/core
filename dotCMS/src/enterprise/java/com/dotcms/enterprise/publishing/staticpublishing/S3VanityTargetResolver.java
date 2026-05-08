package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.urlmap.URLMapAPI;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContextBuilder;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter.IAm;
import com.dotmarketing.filters.CMSFilter.IAmSubType;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;

import java.util.Optional;

/**
 * Resolves Vanity URL forward targets using dotCMS URL semantics.
 */
public class S3VanityTargetResolver {

    private final CMSUrlUtil cmsUrlUtil;
    private final HTMLPageAssetAPI htmlPageAssetAPI;
    private final URLMapAPI urlMapAPI;
    private final ContentletAPI contentletAPI;
    private final FileAssetAPI fileAssetAPI;

    /**
     * Creates a resolver with system dependencies.
     */
    public S3VanityTargetResolver() {
        this(CMSUrlUtil.getInstance(), APILocator.getHTMLPageAssetAPI(), APILocator.getURLMapAPI(),
                APILocator.getContentletAPI(), APILocator.getFileAssetAPI());
    }

    /**
     * Creates a resolver with explicit dependencies for tests.
     *
     * @param cmsUrlUtil CMS URL resolver
     * @param htmlPageAssetAPI HTML page API
     * @param urlMapAPI URL Map API
     * @param contentletAPI Contentlet API
     * @param fileAssetAPI File Asset API
     */
    public S3VanityTargetResolver(final CMSUrlUtil cmsUrlUtil, final HTMLPageAssetAPI htmlPageAssetAPI,
                                  final URLMapAPI urlMapAPI, final ContentletAPI contentletAPI,
                                  final FileAssetAPI fileAssetAPI) {
        this.cmsUrlUtil = cmsUrlUtil;
        this.htmlPageAssetAPI = htmlPageAssetAPI;
        this.urlMapAPI = urlMapAPI;
        this.contentletAPI = contentletAPI;
        this.fileAssetAPI = fileAssetAPI;
    }

    /**
     * Resolves a forward target into a supported static publishing target.
     *
     * @param canonicalPath normalized Vanity URL forward target
     * @param context Vanity URL publishing context
     * @param user user used for dotCMS resolution
     * @return resolved target when dotCMS can render it as static HTML
     * @throws DotDataException when dotCMS data access fails
     * @throws DotSecurityException when permissions prevent target resolution
     */
    public Optional<S3VanityResolvedTarget> resolve(final String canonicalPath,
                                                    final S3VanityAliasPublishContext context,
                                                    final User user)
            throws DotDataException, DotSecurityException {
        final Optional<S3VanityResolvedTarget> page = resolvePage(canonicalPath, context, S3VanityTargetType.PAGE);
        if (page.isPresent()) {
            return page;
        }

        final Tuple2<IAm, IAmSubType> resourceType = cmsUrlUtil.resolveResourceType(IAm.NOTHING_IN_THE_CMS,
                canonicalPath, context.host, context.language.getId());
        if (IAm.FILE.equals(resourceType._1())) {
            return resolveFileAsset(canonicalPath, context, user);
        }
        if (!IAm.PAGE.equals(resourceType._1())) {
            return Optional.empty();
        }
        return resolvePageSubtype(resourceType._2(), canonicalPath, context, user);
    }

    /**
     * Resolves a File Asset target using the same URL classification used by dotCMS.
     *
     * @param canonicalPath normalized forward target path
     * @param context Vanity URL publishing context
     * @param user user used for content lookup
     * @return resolved File Asset when found
     * @throws DotDataException when dotCMS data access fails
     * @throws DotSecurityException when permissions prevent target resolution
     */
    private Optional<S3VanityResolvedTarget> resolveFileAsset(final String canonicalPath,
                                                             final S3VanityAliasPublishContext context,
                                                             final User user)
            throws DotDataException, DotSecurityException {
        final Identifier identifier = APILocator.getIdentifierAPI().find(context.host, canonicalPath);
        if (identifier == null || !identifier.exists()) {
            return Optional.empty();
        }

        final Optional<Contentlet> contentlet = contentletAPI.findContentletByIdentifierOrFallback(identifier.getId(),
                true, context.language.getId(), user, false);
        if (contentlet.isEmpty() || !fileAssetAPI.isFileAsset(contentlet.get())) {
            return Optional.empty();
        }

        final FileAsset fileAsset = fileAssetAPI.fromContentlet(contentlet.get());
        return Optional.of(new S3VanityResolvedTarget(S3VanityTargetType.FILE_ASSET, canonicalPath,
                null, null, fileAsset));
    }

    /**
     * Resolves the specific page subtype reported by dotCMS.
     *
     * @param subType dotCMS page subtype
     * @param canonicalPath normalized forward target path
     * @param context Vanity URL publishing context
     * @param user user used for URL Map resolution
     * @return resolved target when supported
     * @throws DotDataException when dotCMS data access fails
     * @throws DotSecurityException when permissions prevent target resolution
     */
    private Optional<S3VanityResolvedTarget> resolvePageSubtype(final IAmSubType subType,
                                                               final String canonicalPath,
                                                               final S3VanityAliasPublishContext context,
                                                               final User user)
            throws DotDataException, DotSecurityException {
        if (IAmSubType.PAGE_URL_MAP.equals(subType)) {
            return resolveUrlMap(canonicalPath, context, user);
        }
        final S3VanityTargetType targetType = IAmSubType.PAGE_INDEX.equals(subType)
                ? S3VanityTargetType.PAGE_INDEX : S3VanityTargetType.PAGE;
        return resolvePage(canonicalPath, context, targetType);
    }

    /**
     * Resolves a regular page or folder index page.
     *
     * @param canonicalPath normalized forward target path
     * @param context Vanity URL publishing context
     * @param targetType target type to assign
     * @return resolved page target when found
     * @throws DotDataException when dotCMS data access fails
     * @throws DotSecurityException when permissions prevent target resolution
     */
    private Optional<S3VanityResolvedTarget> resolvePage(final String canonicalPath,
                                                        final S3VanityAliasPublishContext context,
                                                        final S3VanityTargetType targetType)
            throws DotDataException, DotSecurityException {
        final IHTMLPage htmlPage = htmlPageAssetAPI.getPageByPath(canonicalPath, context.host,
                context.language.getId(), true);
        return htmlPage == null ? Optional.empty()
                : Optional.of(new S3VanityResolvedTarget(targetType, canonicalPath, htmlPage, null));
    }

    /**
     * Resolves a URL mapped target into its detail page and contentlet.
     *
     * @param canonicalPath normalized forward target path
     * @param context Vanity URL publishing context
     * @param user user used for URL Map resolution
     * @return resolved URL mapped target when found
     * @throws DotDataException when dotCMS data access fails
     * @throws DotSecurityException when permissions prevent target resolution
     */
    private Optional<S3VanityResolvedTarget> resolveUrlMap(final String canonicalPath,
                                                          final S3VanityAliasPublishContext context,
                                                          final User user)
            throws DotDataException, DotSecurityException {
        final Optional<URLMapInfo> urlMapInfo = urlMapAPI.processURLMap(UrlMapContextBuilder.builder()
                .setHost(context.host)
                .setLanguageId(context.language.getId())
                .setMode(PageMode.LIVE)
                .setUri(canonicalPath)
                .setUser(user)
                .build());
        if (urlMapInfo.isEmpty()) {
            return Optional.empty();
        }

        final IHTMLPage detailPage = htmlPageAssetAPI.getPageByPath(urlMapInfo.get().getDetailtPageUri(),
                context.host, context.language.getId(), true);
        return detailPage == null ? Optional.empty()
                : Optional.of(new S3VanityResolvedTarget(S3VanityTargetType.PAGE_URL_MAP, canonicalPath,
                        detailPage, urlMapInfo.get()));
    }
}
