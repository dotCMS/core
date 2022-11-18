package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.component.ImmutableResolvedSiteAndFolder;
import com.dotcms.contenttype.model.component.ImmutableSiteAndFolderParams;
import com.dotcms.contenttype.model.component.ResolvedSiteAndFolder;
import com.dotcms.contenttype.model.component.SiteAndFolderParams;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class SiteAndFolderResolverImpl implements SiteAndFolderResolver {

    private final User user;

    SiteAndFolderResolverImpl(final User user) {
        this.user = user;
    }

    @Override
    public ContentType resolveSiteAndFolder(final ContentType contentType)
            throws DotDataException, DotSecurityException {

        final List<Field> fields = contentType.fields();
        if(contentType.fixed()){
            //CT marked as fixed are mean to live under SYSTEM_HOST
            final ContentType build = ContentTypeBuilder.builder(contentType)
                    .host(Host.SYSTEM_HOST)
                    .folder(Folder.SYSTEM_FOLDER).build();
            build.constructWithFields(fields);
            return build;
        }

        final String id = contentType.id();

        //by setting a null id we block all lazy calculations happening within ContentType
        //when lazy calculations are blocked we can get null values when nothing has been set on those fields instead of the lazy calculation
        //Immutables work in such a way that if a getter
        final ContentType idLess = ContentTypeBuilder.builder(contentType).id(null).build();
        ImmutableSiteAndFolderParams siteAndFolderParams =
                ImmutableSiteAndFolderParams.builder()
                        .host(idLess.host())
                        .folder(idLess.folder())
                        .siteName(idLess.siteName())
                        .folderPath(idLess.folderPath())
                        .build();

        final String resolvedSite = resolveSite(siteAndFolderParams);
        final ResolvedSiteAndFolder resolvedSiteAndFolder = resolveFolder(siteAndFolderParams, resolvedSite);

        final ContentType build = ContentTypeBuilder.builder(contentType)
                .host(resolvedSiteAndFolder.resolvedSite())
                .folder(resolvedSiteAndFolder.resolvedFolder()).id(id).build();
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
    @NotNull
    String resolveSite(final SiteAndFolderParams params)
            throws DotDataException, DotSecurityException {
        // Sets the host:
        // We can set host and folder in two different ways we can use the host/siteName property pair
        // Or we can set the folder using the property pair folder/folderPath
        // The properties siteName/folderPath take precedence

        final HostAPI hostAPI = APILocator.getHostAPI();

        // First thing's first.
        // Here we need to work on the siteName property which takes precede over host
        // SiteName takes the human-readable and more approachable site name while host is expected to have an id
        if (UtilMethods.isSet(params.siteName()) && !Host.SYSTEM_HOST.equals(params.siteName())) {
            Host resolvedSite = hostAPI.resolveHostName(params.siteName(), APILocator.systemUser(),
                    true);
            //if site-name resolution fails it will fall back to the default host.
            //Logger.debug(ContentTypeAPIImpl.class, String.format("Attempt to resolve property [siteName] with value [%s] as [%s] ", contentType.host(), resolvedSite.getHostname()));
            return resolvedSite.getIdentifier();
        }

        if (UtilMethods.isSet(params.host())) {
            // Attempt resolve host
            if (!UUIDUtil.isUUID(params.host()) && !Host.SYSTEM_HOST.equalsIgnoreCase(
                    params.host())) {
                //Resolve prop as site-name
                Host resolvedSite = hostAPI.resolveHostName(params.host(), APILocator.systemUser(),
                        true);
                Logger.debug(ContentTypeAPIImpl.class,
                        String.format(
                                "Attempt to resolve property [host] with value [%s] as a siteName ",
                                params.host()));
                return resolvedSite.getIdentifier();
            } else {
                if (!Host.SYSTEM_HOST.equals(params.host())) {
                    //Resolve property as an id
                    Host resolvedSite = hostAPI.find(params.host(), APILocator.systemUser(), true);
                    if (null != resolvedSite) {
                        return resolvedSite.getIdentifier();
                    } else {
                        final String fallBackSiteIdentifier = Try.of(
                                () -> hostAPI.findDefaultHost(APILocator.systemUser(), false)
                                        .getIdentifier()).getOrElse(Host.SYSTEM_HOST);
                        Logger.warn(ContentTypeAPIImpl.class, String.format(
                                "Unable to resolve the provided value in prop host[%s] using fallback [%s].",
                                params.host(), fallBackSiteIdentifier));
                        return fallBackSiteIdentifier;
                    }
                }
            }
        }

        return Host.SYSTEM_HOST;
    }


    /**
     * takes an incoming CT object with valid hostId and from there the folder id is resolved
     * @param params
     * @param resolvedSiteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    ResolvedSiteAndFolder resolveFolder(final SiteAndFolderParams params, final String resolvedSiteId)
            throws DotDataException, DotSecurityException {

        //At this point we must relay that a valid hostId has been already set

        // Now Check if the folder has been set
        // First try with the folderPath
        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final HostAPI hostAPI = APILocator.getHostAPI();
        if (UtilMethods.isSet(params.folderPath()) && !Folder.SYSTEM_FOLDER_PATH.equals(params.folderPath())) {

            final String folderPath = params.folderPath();
            if(UtilMethods.isSet(folderPath)) {
                //default://application/containers
                final Optional<Folder> fromPath = fromPath(folderPath);
                if (fromPath.isPresent()) {
                    final Folder folder = fromPath.get();
                    return ImmutableResolvedSiteAndFolder.builder()
                            .resolvedFolder(folder.getInode())
                            .resolvedSite(folder.getHostId()).build();
                }
            }

            //trying the incoming folder as a path but in order to do that we must have a valid host
            if (UtilMethods.isSet(resolvedSiteId) && !Host.SYSTEM_HOST.equals(resolvedSiteId)) {
                Logger.info(ContentTypeAPIImpl.class,
                        () -> String.format("Trying folder [%s] as a path on given host [%s]", folderPath, resolvedSiteId));

                //We have a site and we have a path
                Folder folder = folderAPI.findFolderByPath(folderPath, resolvedSiteId, APILocator.systemUser(), false);
                if (null != folder && UtilMethods.isSet(folder.getIdentifier())) {
                    //contentTypeToSave = ContentTypeBuilder.builder(contentTypeToSave).folder(folder.getInode()).host(folder.getHostId()).build();
                    return ImmutableResolvedSiteAndFolder.builder().resolvedFolder(folder.getInode()).resolvedSite(folder.getHostId()).build();
                } else {
                    folder = folderAPI.findSystemFolder();
                    final Host defaultHost = hostAPI.findDefaultHost(APILocator.systemUser(), false);
                    //contentTypeToSave = ContentTypeBuilder.builder(contentTypeToSave).folder(Folder.SYSTEM_FOLDER).host(defaultHost.getIdentifier()).build();
                    return ImmutableResolvedSiteAndFolder.builder().resolvedFolder(Folder.SYSTEM_FOLDER).resolvedSite(defaultHost.getIdentifier()).build();
                }
            }
        }

        //If we haven't established what our folder is yet. try folder as an id
        if (UtilMethods.isSet(params.folder()) && !Folder.SYSTEM_FOLDER.equals(params.folder())) {
            final String folderId = params.folder();
            if (!UUIDUtil.isUUID(folderId) && !Folder.SYSTEM_FOLDER.equals(folderId)) {
                throw new DotDataException(
                        "property [folder] should only be used to set ids, folder names/paths must be set through the [folderPath] property.");
            }
            Folder folder = folderAPI.find(folderId, user, false);
            if(null != folder){
                // if the folder is provided we have no choice but to use the folder's host id
                // Otherwise will get a `Cannot assign host/folder to structure, folder does not belong to given host`
                //contentTypeToSave = ContentTypeBuilder.builder(contentTypeToSave).folder(folder.getInode()).host(folder.getHostId()).build();
                return ImmutableResolvedSiteAndFolder.builder().resolvedFolder(folder.getInode()).resolvedSite(folder.getHostId()).build();
            }
        }

        //Still no folder eh?
        return ImmutableResolvedSiteAndFolder.builder().resolvedFolder(Folder.SYSTEM_FOLDER).resolvedSite(resolvedSiteId).build();
    }

    Optional<Folder> fromPath(final String path) {
        final String[] parts = path.split(StringPool.COLON);
        if(parts.length == 2){
            final String siteNamePart = parts[0];
            final String folderPathPart = parts[1];
            final FolderAPI folderAPI = APILocator.getFolderAPI();
            final HostAPI hostAPI = APILocator.getHostAPI();
            try {
                final Host site = hostAPI.resolveHostName(siteNamePart, APILocator.systemUser(), false);
                final Folder folder = folderAPI.findFolderByPath(folderPathPart, site,
                        APILocator.systemUser(), false);
                if (null != folder && UtilMethods.isSet(folder.getIdentifier())) {
                    return Optional.of(folder);
                }
            }catch (DotDataException |  DotSecurityException e){
                Logger.error(ContentTypeAPIImpl.class,
                        String.format("An error occurred while calculating folderPath from given string [%s]", path), e);
            }
        }
        return Optional.empty();
    }


}
