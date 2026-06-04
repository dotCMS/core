package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Coordinates publishing and removal of static vanity aliases.
 */
public class S3VanityAliasService {

    private final VanityUrlAPI vanityUrlAPI;
    private final S3VanityAliasSupport aliasSupport;
    private final S3VanityAliasRepository repository;
    private final HTMLPageAssetAPI htmlPageAssetAPI;
    private final S3VanityTargetResolver targetResolver;

    /**
     * Creates the service with system dependencies.
     */
    public S3VanityAliasService() {
        this(APILocator.getVanityUrlAPI(), new S3VanityAliasSupport(), new S3VanityAliasRepository(),
                APILocator.getHTMLPageAssetAPI(), new S3VanityTargetResolver());
    }

    /**
     * Creates the service with explicit dependencies for tests.
     *
     * @param vanityUrlAPI Vanity URL API
     * @param aliasSupport alias support component
     * @param repository alias mapping repository
     */
    public S3VanityAliasService(final VanityUrlAPI vanityUrlAPI, final S3VanityAliasSupport aliasSupport,
                                final S3VanityAliasRepository repository) {
        this(vanityUrlAPI, aliasSupport, repository, APILocator.getHTMLPageAssetAPI(),
                new S3VanityTargetResolver());
    }

    /**
     * Creates the service with explicit dependencies for tests.
     *
     * @param vanityUrlAPI Vanity URL API
     * @param aliasSupport alias support component
     * @param repository alias mapping repository
     * @param htmlPageAssetAPI HTML page rendering API
     * @param targetResolver dotCMS target resolver
     */
    public S3VanityAliasService(final VanityUrlAPI vanityUrlAPI, final S3VanityAliasSupport aliasSupport,
                                final S3VanityAliasRepository repository,
                                final HTMLPageAssetAPI htmlPageAssetAPI,
                                final S3VanityTargetResolver targetResolver) {
        this.vanityUrlAPI = vanityUrlAPI;
        this.aliasSupport = aliasSupport;
        this.repository = repository;
        this.htmlPageAssetAPI = htmlPageAssetAPI;
        this.targetResolver = targetResolver;
    }

    /**
     * Publishes one Vanity URL clone by rendering its live forward target.
     *
     * @param context Vanity URL publishing context
     * @param vanityContentlet live Vanity URL contentlet
     * @throws DotDataException when persistence or S3 operations fail
     */
    public void publishAliasForVanityUrl(final S3VanityAliasPublishContext context,
                                         final Contentlet vanityContentlet) throws DotDataException {
        if (!aliasSupport.isSupportedVanityUrl(vanityContentlet)) {
            Logger.warn(this, "Skipping unsupported Vanity URL: "
                    + (vanityContentlet != null ? vanityContentlet.getIdentifier() : "null"));
            if (vanityContentlet != null) {
                unpublishAliasesByVanityUrl(new S3VanityAliasCleanupContext(context.endpointId,
                        context.endpointPublisher), vanityContentlet.getLanguageId(),
                        vanityContentlet.getIdentifier());
            }
            return;
        }

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final Optional<String> canonicalPath = aliasSupport.normalizeCanonicalPath(
                aliasSupport.getForwardTo(vanityContentlet));
        final Optional<S3VanityResolvedTarget> target;
        if (canonicalPath.isEmpty()) {
            target = Optional.empty();
        } else {
            target = resolveTarget(context, canonicalPath.get(), systemUser);
        }
        if (target.isEmpty()) {
            unpublishAliasesByVanityUrl(new S3VanityAliasCleanupContext(context.endpointId,
                    context.endpointPublisher), vanityContentlet.getLanguageId(),
                    vanityContentlet.getIdentifier());
            return;
        }

        final Optional<S3VanityAlias> alias = buildAlias(context, vanityContentlet, target.get());
        if (alias.isEmpty()) {
            unpublishAliasesByVanityUrl(new S3VanityAliasCleanupContext(context.endpointId,
                    context.endpointPublisher), vanityContentlet.getLanguageId(),
                    vanityContentlet.getIdentifier());
            return;
        }

        final Optional<File> renderedFile = renderTarget(context, target.get(), systemUser);
        if (renderedFile.isEmpty()) {
            unpublishAliasesByVanityUrl(new S3VanityAliasCleanupContext(context.endpointId,
                    context.endpointPublisher), vanityContentlet.getLanguageId(),
                    vanityContentlet.getIdentifier());
            return;
        }

        try {
            publishMaterializedAlias(context, alias.get(), renderedFile.get());
        } finally {
            cleanupMaterializedFile(target.get(), renderedFile.get());
        }
    }

    /**
     * Builds the operational alias from a supported Vanity URL contentlet.
     *
     * @param context Vanity URL publishing context
     * @param vanityContentlet live Vanity URL contentlet
     * @return alias to materialize when the contentlet is supported
     */
    private Optional<S3VanityAlias> buildAlias(final S3VanityAliasPublishContext context,
                                              final Contentlet vanityContentlet,
                                              final S3VanityResolvedTarget target) {
        final String canonicalPath = target.canonicalPath;
        return aliasSupport.materializeVanityPath(aliasSupport.getUri(vanityContentlet), target.type)
                .map(vanityPath -> new S3VanityAlias(context.endpointId, context.host.getIdentifier(),
                        context.language.getId(), canonicalPath, vanityPath, vanityContentlet.getIdentifier(),
                        context.bucketName, context.bucketRegion, context.bucketPrefix));
    }

    /**
     * Resolves a canonical path with dotCMS URL semantics.
     *
     * @param context Vanity URL publishing context
     * @param canonicalPath normalized forward target path
     * @param systemUser system user used for resolution
     * @return resolved target when the path can be static-published
     * @throws DotDataException when resolution fails unexpectedly
     */
    private Optional<S3VanityResolvedTarget> resolveTarget(final S3VanityAliasPublishContext context,
                                                          final String canonicalPath,
                                                          final User systemUser)
            throws DotDataException {
        try {
            final Optional<S3VanityResolvedTarget> target = targetResolver.resolve(canonicalPath, context, systemUser);
            if (target.isEmpty()) {
                Logger.warn(this, "Skipping Vanity URL because canonical target is not static-publishable: "
                        + canonicalPath);
            }
            return target;
        } catch (final DotSecurityException e) {
            Logger.warn(this, "Skipping Vanity URL because canonical target cannot be resolved: "
                    + canonicalPath + ". " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Renders the resolved target and stores it in a temporary HTML file.
     *
     * @param context Vanity URL publishing context
     * @param target resolved target to render
     * @param systemUser system user used for rendering
     * @return temporary file when rendering produced HTML
     * @throws DotDataException when rendering fails unexpectedly
     */
    private Optional<File> renderTarget(final S3VanityAliasPublishContext context,
                                        final S3VanityResolvedTarget target,
                                        final User systemUser) throws DotDataException {
        if (DotAsset.FILE_ASSET.equals(target.type)) {
            return target.physicalFile();
        }

        try {
            final String html = renderTargetHtml(context, target, systemUser);
            if (!UtilMethods.isSet(html)) {
                Logger.warn(this, "Skipping Vanity URL because canonical target rendered empty: "
                        + target.canonicalPath);
                return Optional.empty();
            }
            return Optional.of(writeHtmlTempFile(html));
        } catch (final DotStateException | DotSecurityException | IOException e) {
            Logger.warn(this, "Skipping Vanity URL because canonical target cannot be rendered: "
                    + target.canonicalPath + ". " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Renders the resolved target using the matching dotCMS rendering API.
     *
     * @param context Vanity URL publishing context
     * @param target resolved target to render
     * @param systemUser system user used for rendering
     * @return rendered HTML
     * @throws DotStateException when render state is invalid
     * @throws DotDataException when data access fails
     * @throws DotSecurityException when permissions prevent rendering
     */
    private String renderTargetHtml(final S3VanityAliasPublishContext context,
                                    final S3VanityResolvedTarget target,
                                    final User systemUser)
            throws DotStateException, DotDataException, DotSecurityException {
        if (!APILocator.getPermissionAPI().doesUserHavePermission(
                target.htmlPage, PermissionAPI.PERMISSION_READ, null, true)) {
            Logger.warn(this, "Skipping Vanity URL because canonical target is not publicly readable: "
                    + target.canonicalPath);
            return null;
        }
        final String contentletInode = target.contentletInode().orElse(null);
        return htmlPageAssetAPI.getHTML(target.htmlPage, true, contentletInode, systemUser,
                context.language.getId(), Constants.USER_AGENT_DOTCMS_PUSH_PUBLISH);
    }

    /**
     * Writes rendered HTML into a temporary file accepted by the S3 publisher.
     *
     * @param html rendered HTML
     * @return temporary HTML file
     * @throws IOException when the temporary file cannot be written
     */
    private File writeHtmlTempFile(final String html) throws IOException {
        final File tempFile = File.createTempFile("s3-vanity-alias-", ".html");
        Files.write(tempFile.toPath(), html.getBytes(StandardCharsets.UTF_8));
        return tempFile;
    }

    /**
     * Publishes the current alias, removes obsolete S3 clones, and realigns persistence.
     *
     * @param context Vanity URL publishing context
     * @param alias alias to publish
     * @param file rendered canonical HTML file
     * @throws DotDataException when persistence or S3 operations fail
     */
    private void publishMaterializedAlias(final S3VanityAliasPublishContext context,
                                          final S3VanityAlias alias,
                                          final File file) throws DotDataException {
        final List<S3VanityAlias> previousAliases =
                repository.findByVanityUrlId(context.endpointId, context.language.getId(), alias.vanityUrlId);
        try {
            repository.replaceMappingsByVanityUrlId(context.endpointId, context.language.getId(), alias.vanityUrlId,
                    Collections.singletonList(alias));
            publishAlias(context, alias, file);
            deleteObsoleteAliases(context, previousAliases, alias);
        } catch (final Exception e) {
            try {
                repository.replaceMappingsByVanityUrlId(context.endpointId, context.language.getId(),
                        alias.vanityUrlId, previousAliases);
            } catch (final Exception compensateEx) {
                Logger.warn(this, "Failed to restore previous alias mappings for " + alias.vanityUrlId
                        + ": " + compensateEx.getMessage());
            }
            throw wrapAsDotDataException(e);
        }
    }

    /**
     * Deletes materialized aliases no longer represented by the current Vanity URL.
     *
     * @param context Vanity URL publishing context
     * @param persistedAliases previously persisted aliases
     * @param currentAlias current alias
     * @throws DotDataException when S3 cleanup fails
     * @throws DotPublishingException when S3 cleanup fails
     */
    private void deleteObsoleteAliases(final S3VanityAliasPublishContext context,
                                       final List<S3VanityAlias> persistedAliases,
                                       final S3VanityAlias currentAlias)
            throws DotDataException, DotPublishingException {
        for (final S3VanityAlias alias : persistedAliases) {
            if (!storageLocation(alias).equals(storageLocation(currentAlias))) {
                deleteAlias(context.endpointPublisher, alias);
            }
        }
    }

    /**
     * Publishes one rendered Vanity URL clone.
     *
     * @param context Vanity URL publishing context
     * @param alias alias to publish
     * @param file rendered canonical HTML file
     * @throws DotPublishingException when S3 publishing fails
     */
    private void publishAlias(final S3VanityAliasPublishContext context,
                              final S3VanityAlias alias,
                              final File file) throws DotPublishingException {
        context.endpointPublisher.pushFileToEndpoint(alias.bucketName, alias.bucketRegion,
                alias.bucketPrefix, alias.vanityPath, file);
    }

    /**
     * Removes a materialized file created for Vanity URL publishing.
     *
     * File Assets are backed by dotCMS-managed binaries and must not be removed
     * after the S3 upload.
     *
     * @param target resolved target
     * @param file materialized file
     */
    private void cleanupMaterializedFile(final S3VanityResolvedTarget target, final File file) {
        if (DotAsset.FILE_ASSET.equals(target.type)) {
            return;
        }

        try {
            Files.deleteIfExists(file.toPath());
        } catch (final IOException e) {
            Logger.warn(this, "Unable to delete temporary Vanity URL file: " + file.getAbsolutePath());
        }
    }

    /**
     * Publishes current vanity aliases and realigns the persisted mapping.
     *
     * @param context publishing context
     * @throws DotDataException when persistence or S3 operations fail
     */
    public void publishAliases(final S3VanityAliasContext context) throws DotDataException {
        final List<S3VanityAlias> currentAliases = loadCurrentAliases(context);
        final List<S3VanityAlias> persistedAliases = repository.findByLookup(context.lookup);
        final Map<String, S3VanityAlias> currentByLocation = indexByStorageLocation(currentAliases);
        final List<S3VanityAlias> aliasesToRefresh = filterExisting(currentAliases,
                indexByStorageLocation(persistedAliases));
        final List<S3VanityAlias> aliasesToDelete = filterMissing(persistedAliases, currentByLocation);
        final List<S3VanityAlias> publishedNow = new ArrayList<>();
        final List<S3VanityAlias> deletedNow = new ArrayList<>();

        try {
            publishAliases(context, aliasesToRefresh, publishedNow::add);
            deleteAliases(context, aliasesToDelete, deletedNow::add);
            repository.replaceMappings(context.lookup, aliasesToRefresh);
        } catch (final Exception e) {
            for (final S3VanityAlias alias : publishedNow) {
                try {
                    deleteAlias(context, alias);
                } catch (final Exception ce) {
                    Logger.error(this, "Unable to compensate published alias " + alias.vanityPath, ce);
                }
            }
            restoreAliases(context, deletedNow);
            throw wrapAsDotDataException(e);
        }
    }

    /**
     * Removes vanity aliases materialized by a deleted or unpublished Vanity
     * URL.
     *
     * @param context minimal S3 cleanup context
     * @param languageId Vanity URL language identifier
     * @param vanityUrlId source Vanity URL identifier
     * @throws DotDataException when persistence or S3 cleanup fails
     */
    public void unpublishAliasesByVanityUrl(final S3VanityAliasCleanupContext context,
                                            final long languageId,
                                            final String vanityUrlId) throws DotDataException {
        if (!UtilMethods.isSet(vanityUrlId)) {
            return;
        }

        final List<S3VanityAlias> persistedAliases = languageId > 0
                ? repository.findByVanityUrlId(context.endpointId, languageId, vanityUrlId)
                : repository.findByVanityUrlId(context.endpointId, vanityUrlId);
        try {
            if (languageId > 0) {
                repository.deleteByVanityUrlId(context.endpointId, languageId, vanityUrlId);
            } else {
                repository.deleteByVanityUrlId(context.endpointId, vanityUrlId);
            }
            for (final S3VanityAlias alias : persistedAliases) {
                removeMaterializedAlias(context, alias);
            }
        } catch (final Exception e) {
            throw wrapAsDotDataException(e);
        }
    }

    /**
     * Removes a materialized Vanity URL key, restoring the live resource when
     * the Vanity URL was obscuring an existing dotCMS resource.
     *
     * @param context cleanup context
     * @param alias persisted alias to remove
     * @throws DotDataException when resolving the live resource fails
     * @throws DotSecurityException when live resource resolution is denied
     * @throws DotPublishingException when S3 publishing or deletion fails
     */
    private void removeMaterializedAlias(final S3VanityAliasCleanupContext context,
                                         final S3VanityAlias alias)
            throws DotDataException, DotSecurityException, DotPublishingException {
        final Optional<S3VanityRestoreResult> restoreResult = restoreLiveResource(context, alias);
        if (restoreResult.isEmpty()) {
            deleteAlias(context.endpointPublisher, alias);
        }
    }

    /**
     * Restores a live dotCMS resource that is currently obscured by a Vanity
     * URL clone.
     *
     * @param context cleanup context
     * @param alias persisted alias to evaluate
     * @return restore result when the vanity path resolves to a live resource
     * @throws DotDataException when resolving or rendering the live resource fails
     * @throws DotSecurityException when live resource resolution is denied
     * @throws DotPublishingException when S3 publishing fails
     */
    private Optional<S3VanityRestoreResult> restoreLiveResource(final S3VanityAliasCleanupContext context,
                                                               final S3VanityAlias alias)
            throws DotDataException, DotSecurityException, DotPublishingException {
        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final Optional<S3VanityAliasPublishContext> restoreContext =
                buildRestoreContext(context, alias, systemUser);
        if (restoreContext.isEmpty()) {
            return Optional.empty();
        }

        final Optional<S3VanityResolvedTarget> target =
                resolveObscuredLiveTarget(restoreContext.get(), alias, systemUser);
        if (target.isEmpty()) {
            return Optional.empty();
        }

        final S3VanityRestoreResult result = materializeRestoreTarget(restoreContext.get(), target.get(), systemUser);
        try {
            publishRestoredResource(restoreContext.get(), alias, result.file);
        } finally {
            cleanupMaterializedFile(result.target, result.file);
        }
        return Optional.of(result);
    }

    /**
     * Builds a publishing context from a persisted alias row.
     *
     * @param context cleanup context
     * @param alias persisted alias
     * @param systemUser system user used for host lookup
     * @return restore context when host and language are still available
     * @throws DotDataException when host lookup fails
     * @throws DotSecurityException when host lookup is denied
     */
    private Optional<S3VanityAliasPublishContext> buildRestoreContext(final S3VanityAliasCleanupContext context,
                                                                     final S3VanityAlias alias,
                                                                     final User systemUser)
            throws DotDataException, DotSecurityException {
        final Host host = APILocator.getHostAPI().find(alias.hostId, systemUser, false);
        final Language language = APILocator.getLanguageAPI().getLanguage(alias.languageId);
        if (host == null || language == null) {
            return Optional.empty();
        }

        return Optional.of(new S3VanityAliasPublishContext(alias.endpointId, alias.bucketName,
                alias.bucketRegion, alias.bucketPrefix, host, language, context.endpointPublisher));
    }

    /**
     * Resolves the vanity path as a possible live resource obscured by the
     * removed Vanity URL.
     *
     * @param context restore publishing context
     * @param alias persisted alias
     * @param systemUser system user used for resolution
     * @return resolved live target when one exists
     * @throws DotDataException when target resolution fails
     * @throws DotSecurityException when target resolution is denied
     */
    private Optional<S3VanityResolvedTarget> resolveObscuredLiveTarget(final S3VanityAliasPublishContext context,
                                                                      final S3VanityAlias alias,
                                                                      final User systemUser)
            throws DotDataException, DotSecurityException {
        return targetResolver.resolve(alias.vanityPath, context, systemUser);
    }

    /**
     * Materializes a resolved live resource for S3 restore.
     *
     * @param context restore publishing context
     * @param target resolved live target
     * @param systemUser system user used for rendering
     * @return restore result with the materialized file
     * @throws DotDataException when rendering fails
     * @throws DotPublishingException when the live resource cannot be materialized
     */
    private S3VanityRestoreResult materializeRestoreTarget(final S3VanityAliasPublishContext context,
                                                          final S3VanityResolvedTarget target,
                                                          final User systemUser)
            throws DotDataException, DotPublishingException {
        final Optional<File> file = renderTarget(context, target, systemUser);
        if (file.isEmpty()) {
            throw new DotPublishingException("Unable to restore live resource for Vanity URL path: "
                    + target.canonicalPath);
        }
        return new S3VanityRestoreResult(target, file.get());
    }

    /**
     * Publishes the restored live resource on the obscured Vanity URL key.
     *
     * @param context restore publishing context
     * @param alias persisted alias being removed
     * @param file live resource file
     * @throws DotPublishingException when S3 publishing fails
     */
    private void publishRestoredResource(final S3VanityAliasPublishContext context,
                                         final S3VanityAlias alias,
                                         final File file) throws DotPublishingException {
        context.endpointPublisher.pushFileToEndpoint(alias.bucketName, alias.bucketRegion,
                alias.bucketPrefix, alias.vanityPath, file);
    }

    /**
     * Removes the vanity aliases materialized for the current mapping.
     *
     * @param context publishing context
     * @throws DotDataException when persistence or S3 cleanup fails
     */
    public void unpublishAliases(final S3VanityAliasContext context) throws DotDataException {
        final List<S3VanityAlias> persistedAliases = repository.findByLookup(context.lookup);
        final List<S3VanityAlias> deletedNow = new ArrayList<>();

        try {
            deleteAliases(context, persistedAliases, deletedNow::add);
            repository.deleteByLookup(context.lookup);
        } catch (final Exception e) {
            restoreAliases(context, deletedNow);
            throw wrapAsDotDataException(e);
        }
    }

    /**
     * Loads and normalizes the current vanity aliases.
     *
     * @param context publishing context
     * @return supported current aliases
     */
    private List<S3VanityAlias> loadCurrentAliases(final S3VanityAliasContext context) {
        final List<CachedVanityUrl> vanityUrls = vanityUrlAPI.findByForward(
                context.host, context.language, context.lookup.canonicalPath, HttpStatus.SC_OK, true);
        return aliasSupport.toAliasMappings(context, vanityUrls).stream()
                .sorted(Comparator.comparing(alias -> alias.vanityPath))
                .collect(Collectors.toList());
    }

    /**
     * Publishes the provided alias list.
     *
     * @param context publishing context
     * @param aliases aliases to publish
     * @param publishedConsumer success consumer
     * @throws DotDataException when S3 publishing fails
     */
    private void publishAliases(final S3VanityAliasContext context, final List<S3VanityAlias> aliases,
                                final Consumer<S3VanityAlias> publishedConsumer)
            throws DotDataException, DotPublishingException {
        for (final S3VanityAlias alias : aliases) {
            publishAlias(context, alias);
            publishedConsumer.accept(alias);
        }
    }

    /**
     * Removes the provided alias list.
     *
     * @param context publishing context
     * @param aliases aliases to remove
     * @param deletedConsumer success consumer
     * @throws DotDataException when S3 cleanup fails
     */
    private void deleteAliases(final S3VanityAliasContext context, final List<S3VanityAlias> aliases,
                               final Consumer<S3VanityAlias> deletedConsumer)
            throws DotDataException, DotPublishingException {
        for (final S3VanityAlias alias : aliases) {
            deleteAlias(context, alias);
            deletedConsumer.accept(alias);
        }
    }

    /**
     * Publishes one vanity alias on S3.
     *
     * @param context publishing context
     * @param alias vanity alias to publish
     * @throws DotDataException when S3 publishing fails
     */
    private void publishAlias(final S3VanityAliasContext context, final S3VanityAlias alias)
            throws DotDataException, DotPublishingException {
        context.endpointPublisher.pushFileToEndpoint(alias.bucketName, alias.bucketRegion,
                alias.bucketPrefix, alias.vanityPath, context.file);
    }

    /**
     * Removes one vanity alias from S3.
     *
     * @param context publishing context
     * @param alias vanity alias to remove
     * @throws DotDataException when S3 cleanup fails
     */
    private void deleteAlias(final S3VanityAliasContext context, final S3VanityAlias alias)
            throws DotDataException, DotPublishingException {
        deleteAlias(context.endpointPublisher, alias);
    }

    /**
     * Removes one vanity alias from the S3 location stored in the mapping.
     *
     * @param endpointPublisher concrete S3 adapter
     * @param alias vanity alias to remove
     * @throws DotDataException when S3 cleanup fails
     */
    private void deleteAlias(final AWSS3EndPointPublisher endpointPublisher, final S3VanityAlias alias)
            throws DotDataException, DotPublishingException {
        endpointPublisher.deleteFilesFromEndpoint(alias.bucketName, alias.bucketPrefix, alias.vanityPath);
    }

    /**
     * Restores aliases already removed when a later step fails.
     *
     * @param context publishing context
     * @param aliases aliases to restore
     */
    private void restoreAliases(final S3VanityAliasContext context, final List<S3VanityAlias> aliases) {
        for (final S3VanityAlias alias : aliases) {
            try {
                publishAlias(context, alias);
            } catch (final Exception e) {
                Logger.error(this, "Unable to restore vanity alias " + alias.vanityPath, e);
            }
        }
    }

    /**
     * Indexes aliases by materialized S3 location.
     *
     * @param aliases aliases to index
     * @return map keyed by S3 location
     */
    private Map<String, S3VanityAlias> indexByStorageLocation(final List<S3VanityAlias> aliases) {
        return aliases.stream().collect(Collectors.toMap(this::storageLocation, alias -> alias, (left, right) -> left));
    }

    /**
     * Filters aliases that are not present in the comparison map.
     *
     * @param aliases source aliases
     * @param comparison comparison map
     * @return aliases missing from the comparison map
     */
    private List<S3VanityAlias> filterMissing(final List<S3VanityAlias> aliases,
                                              final Map<String, S3VanityAlias> comparison) {
        return aliases.stream().filter(alias -> !comparison.containsKey(storageLocation(alias))).collect(Collectors.toList());
    }

    /**
     * Filters aliases that are present in the comparison map.
     *
     * @param aliases source aliases
     * @param comparison comparison map
     * @return aliases present in the comparison map
     */
    private List<S3VanityAlias> filterExisting(final List<S3VanityAlias> aliases,
                                               final Map<String, S3VanityAlias> comparison) {
        return aliases.stream().filter(alias -> comparison.containsKey(storageLocation(alias))).collect(Collectors.toList());
    }

    /**
     * Calculates the logical key for the materialized S3 location.
     *
     * @param alias alias to index
     * @return key composed of bucket, prefix, and vanity path
     */
    private String storageLocation(final S3VanityAlias alias) {
        return String.join("|", nullSafe(alias.bucketName), nullSafe(alias.bucketPrefix), alias.vanityPath);
    }

    /**
     * Normalizes null values used in comparison keys.
     *
     * @param value value to normalize
     * @return empty string when the value is not set
     */
    private String nullSafe(final String value) {
        return value == null ? "" : value;
    }

    /**
     * Converts a generic error into the type expected by the publisher.
     *
     * @param error original error
     * @return domain exception
     */
    private DotDataException wrapAsDotDataException(final Exception error) {
        if (error instanceof DotDataException) {
            return (DotDataException) error;
        }
        return new DotDataException(error);
    }
}
