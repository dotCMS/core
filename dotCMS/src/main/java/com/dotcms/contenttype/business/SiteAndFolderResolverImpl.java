package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.component.ImmutableResolvedSiteAndFolder;
import com.dotcms.contenttype.model.component.ResolvedSiteAndFolder;
import com.dotcms.contenttype.model.component.SiteAndFolder;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.List;
import java.util.Optional;

/**
 * Centralized class to resolve Site and Folder for Content-Type Construction
 */
public class SiteAndFolderResolverImpl implements SiteAndFolderResolver {

    private final User user;

    private final boolean skipResolveSite;

    private final boolean fallbackToDefaultSite;

    final FolderAPI folderAPI = APILocator.getFolderAPI();
    final HostAPI hostAPI = APILocator.getHostAPI();

    /**
     * Main constructor
     * @param user Required for folder permission validation
     * @param skipResolveSite Set to true when loading starter
     * @param fallbackToDefaultSite Set to true if we want to switch to System-Host
     */
    SiteAndFolderResolverImpl(final User user, final boolean skipResolveSite, final boolean fallbackToDefaultSite) {
        this.user = user;
        this.skipResolveSite = skipResolveSite;
        this.fallbackToDefaultSite = fallbackToDefaultSite;
    }

    @Override
    public ContentType resolveSiteAndFolder(final ContentType contentType)
            throws DotDataException, DotSecurityException {

        final List<Field> fields = contentType.fields();
        if(contentType.fixed()){
            //CT marked as fixed are meant to live under SYSTEM_HOST
            final ContentType build = ContentTypeBuilder.builder(contentType)
                    .host(Host.SYSTEM_HOST)
                    .siteName(Host.SYSTEM_HOST_NAME)
                    .folder(Folder.SYSTEM_FOLDER)
                    .folderPath(Folder.SYSTEM_FOLDER_PATH)
                    .build();
            build.constructWithFields(fields);
            return build;
        }


        //when lazy calculations are blocked we can get null values when nothing has been set on those fields instead of the lazy calculation
        //Immutables work in such a way that if a getter

        final SiteAndFolder params = contentType.siteAndFolder();
        final String resolvedSite = resolveSite(params);
        final ResolvedSiteAndFolder resolvedSiteAndFolder = resolveFolder(params, resolvedSite);

        final ContentType build = ContentTypeBuilder.builder(contentType)
                .host(resolvedSiteAndFolder.resolvedSite())
                .folder(resolvedSiteAndFolder.resolvedFolder())
                .build();
        build.constructWithFields(fields);
        return build;
    }

    /**
     * takes an incoming CT object with siteId or siteName.
     * The last one takes precedence over siteId
     * meaning that if both are provided siteName will be used to resole
     * if a site-name is passed and the resolution fails we fall back to default site
     * if nothing is passed the site is resolved to SYSTEM_HOST
     * @param params
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    String resolveSite(final SiteAndFolder params)
            throws DotDataException, DotSecurityException {

        //Typically, this should be set only when loading starter.
        //Therefore, we trust that the incoming values are set correctly, and they can be trusted
        //Additionally we can not count on APIs being available at Booting time
        if(skipResolveSite){
            return UtilMethods.isSet(params.host()) ? params.host() : Host.SYSTEM_HOST;
        }

        // Sets the host:
        // We can set host and folder in two different ways we can use the host/siteName property pair
        // Or we can set the folder using the property pair folder/folderPath
        // The properties siteName/folderPath take precedence

        // First thing's first.
        // Here we need to work on the siteName property which takes precede over host
        // SiteName takes the human-readable and more approachable site name while host is expected to have an id
        if (UtilMethods.isSet(params.siteName()) && !Host.SYSTEM_HOST_NAME.equals(params.siteName())) {
            return resolveOrFallback(params.siteName());
        }

        if (UtilMethods.isSet(params.host()) && !Host.SYSTEM_HOST.equals(params.host())) {
            return resolveOrFallback(params.host());
        }

        //In case System-Host has been explicitly set we must respect that
        if(Host.SYSTEM_HOST.equals(params.host()) || Host.SYSTEM_HOST_NAME.equals(params.siteName())){
            return Host.SYSTEM_HOST;
        }

        return fallbackHost();
    }


    /**
     * takes an incoming CT object with valid hostId and from there the folder id is resolved
     * @param params
     * @param resolvedSiteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    ResolvedSiteAndFolder resolveFolder(final SiteAndFolder params, final String resolvedSiteId)
            throws DotDataException, DotSecurityException {

        //Typically, this should be set only when loading starter.
        //Therefore, we trust that the incoming values are set correctly, and they can be trusted
        //Additionally we can not count on APIs being available at Booting time
        if(skipResolveSite){
            final String folder = UtilMethods.isSet(params.folder()) ? params.folder() : Folder.SYSTEM_FOLDER;
            return ImmutableResolvedSiteAndFolder.builder().resolvedFolder(folder).resolvedSite(resolvedSiteId).build();
        }

        //At this point we must relay that a valid hostId has been already set

        // Now Check if the folder has been set
        // First try with the folderPath
        if (UtilMethods.isSet(params.folderPath()) && !Folder.SYSTEM_FOLDER_PATH.equals(params.folderPath())) {
            final Optional<ResolvedSiteAndFolder> resolvedSiteAndFolder = tryAsFolderPath(params, resolvedSiteId);
            if(resolvedSiteAndFolder.isPresent()){
               return resolvedSiteAndFolder.get();
            }
        }

        //If we haven't established what our folder is yet. try folder as an id
        if (UtilMethods.isSet(params.folder()) && !Folder.SYSTEM_FOLDER.equals(params.folder())) {
            Optional<ResolvedSiteAndFolder> resolvedSiteAndFolder = tryAsFolderId(params.folder());
            if(resolvedSiteAndFolder.isPresent()){
                return resolvedSiteAndFolder.get();
            }
        }

        //Still no folder eh?
        return ImmutableResolvedSiteAndFolder.builder().resolvedFolder(Folder.SYSTEM_FOLDER).resolvedSite(resolvedSiteId).build();
    }

    /**
     * Attempt to resolve the incoming params as a folder path
     * @param params
     * @param resolvedSiteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<ResolvedSiteAndFolder> tryAsFolderPath(final SiteAndFolder params, final String resolvedSiteId) throws DotDataException, DotSecurityException {
        final String folderPath = params.folderPath();
        if(UtilMethods.isSet(folderPath)) {
            // There are two possibilities

            Optional<Folder> fromPath = fromPath(folderPath);

            if (fromPath.isPresent()) {
                final Folder folder = fromPath.get();
                return Optional.of(ImmutableResolvedSiteAndFolder.builder()
                        .resolvedFolder(folder.getInode())
                        .resolvedSite(folder.getHostId()).build());
            }

            fromPath = fromPath(folderPath, resolvedSiteId);

            if (fromPath.isPresent()) {
                final Folder folder = fromPath.get();
                return Optional.of(ImmutableResolvedSiteAndFolder.builder()
                        .resolvedFolder(folder.getInode())
                        .resolvedSite(folder.getHostId()).build());
            }
        }
        return Optional.empty();
    }

    /**
     * Attempt to resolve the incoming param as folderId
     * @param folderId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<ResolvedSiteAndFolder> tryAsFolderId(String folderId) throws DotDataException, DotSecurityException {
        final Folder folder = folderAPI.find(folderId, user, true);
        if(null != folder){
            // if the folder is provided we have no choice but to use the folder's host id
            // Otherwise will get a `Cannot assign host/folder to structure, folder does not belong to given host`
            return Optional.of(ImmutableResolvedSiteAndFolder.builder().resolvedFolder(folder.getInode()).resolvedSite(folder.getHostId()).build());
        }
        return Optional.empty();
    }

    /**
     * Handle a folder path of the form site-name + :/ + folder-path
     * default:/application/containers
     * @param path
     * @return
     */
    Optional<Folder> fromPath(final String path) throws DotDataException, DotSecurityException {
        final String[] parts = path.split(StringPool.COLON);
        if(parts.length == 2){
            final String siteNamePart = parts[0];
            final String folderPathPart = parts[1];
            final Host site = hostAPI.resolveHostName(siteNamePart, APILocator.systemUser(), true);
            final Folder folder = folderAPI.findFolderByPath(folderPathPart, site,
                    user, true);
            if (null != folder && UtilMethods.isSet(folder.getIdentifier())) {
                return Optional.of(folder);
            }
        }
        return Optional.empty();
    }

    /**
     * Handle a folder path given a siteId
     * @param path
     * @param resolvedSiteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<Folder> fromPath(final String path, final String resolvedSiteId) throws DotDataException, DotSecurityException {
        final Host site = hostAPI.resolveHostName(resolvedSiteId, APILocator.systemUser(), true);
        final Folder folder = folderAPI.findFolderByPath(path, site,
                user, true);
        if (null != folder && UtilMethods.isSet(folder.getIdentifier())) {
            return Optional.of(folder);
        }
        return Optional.empty();
    }

    /**
     * Main function takes a siteId or hostName and attempts to resolve the site
     * IF it does exist all good returns the site id
     * If it doesn't then return the fallback
     * @param siteIdOrName
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    String resolveOrFallback(final String siteIdOrName) throws DotDataException, DotSecurityException {
        final Optional<Host> resolvedSite = UUIDUtil.isUUID(siteIdOrName) ?
              Optional.ofNullable(hostAPI.find(siteIdOrName, APILocator.systemUser(), true)) :
              Optional.ofNullable(hostAPI.findByName(siteIdOrName, APILocator.systemUser(), true));
        if (resolvedSite.isPresent()) {
            return resolvedSite.get().getIdentifier();
        }
        return fallbackHost();
    }

    String fallbackHost() {
        // if we fail to validate the incoming site's id we fall back to the default site
        if (fallbackToDefaultSite) {  // We can disallow this behavior via properties
            return Try.of(()->hostAPI.findDefaultHost(user, true).getIdentifier()).getOrElse(Host.SYSTEM_HOST);
        }
        // Then we fall back to SYSTEM-HOST
        return Host.SYSTEM_HOST;
    }

}
