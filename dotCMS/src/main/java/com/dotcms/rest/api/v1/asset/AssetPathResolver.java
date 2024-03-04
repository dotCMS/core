package com.dotcms.rest.api.v1.asset;

import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.FORWARD_SLASH;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.rest.validation.Preconditions;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * This class is responsible for resolving a path to an asset. The path can be a folder or an asset.
 * Inorder to determine what is getting requested a little bit of parsing is required.
 * So this class takes the uri and a user then tries to determine the host and folder path
 * if we're unable to determine a folder then we'll try to determine an asset.
 */
public class AssetPathResolver {

    FolderAPI folderAPI;
    HostAPI hostAPI;

    AssetPathResolver() {
        this(APILocator.getFolderAPI(), APILocator.getHostAPI());
    }

    AssetPathResolver(final FolderAPI folderAPI, final HostAPI hostAPI) {
        this.folderAPI = folderAPI;
        this.hostAPI = hostAPI;
    }

    /**
     * Main entry point for resolving a path to an asset. This method will attempt to resolve the path to a folder or
     * @param url
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public ResolvedAssetAndPath resolve(final String url, final User user)
            throws DotDataException, DotSecurityException{
       return resolve(url, user, false);
    }

    /**
     * Main entry point for resolving a path to an asset. This method will attempt to resolve the path to a folder or
     * @param url
     * @param user
     * @param createMissingFolders
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public ResolvedAssetAndPath resolve(final String url, final User user, final boolean createMissingFolders)
            throws DotDataException, DotSecurityException {

        try {
            Logger.debug(this, String.format("Resolving url: [%s] " , url));
            final URI uri = new URI(url);
            final Optional<Host> siteByName = resolveHosBytName(uri.getHost(), user);
            if(siteByName.isEmpty()){
                throw new NotFoundException(String.format("Unable to determine a valid host from uri: [%s].", url));
            }

            final Host host = siteByName.get();
            final String path = BLANK.equals(uri.getRawPath()) ? FORWARD_SLASH : uri.getRawPath();
            if (null == path) {
                throw new IllegalArgumentException(String.format("Unable to determine path: [%s].", url));
            }
            final var decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);

            //This line determines if the path is a folder
            final Optional<Folder> folder = resolveExistingFolder(decodedPath, host, user);
            if(folder.isEmpty()){
                //if we've got this far we need to verify if the path is an asset. The folder will be expected to be the parent folder
                Optional<FolderAndAsset> folderAndAsset = resolveAssetAndFolder(decodedPath, host,
                        user, createMissingFolders);
                if(folderAndAsset.isEmpty()){
                    throw new NotFoundInDbException(String.format("Unable to determine a valid folder or asset from uri: [%s].", url));
                }
                final FolderAndAsset folderAsset = folderAndAsset.get();
                return ResolvedAssetAndPath.builder()
                        .resolvedHost(host)
                        .host(host.getHostname())
                        .resolvedFolder(folderAsset.folder())
                        .path(decodedPath)
                        .asset(folderAsset.asset())
                        .build();
            }

            //if we succeed to determine a valid folder from the path then we resolve the last bit as an asset name
            final String resource = uri.getRawQuery() != null ?
                    uri.getRawPath() + "?" + uri.getRawQuery() : uri.getRawPath();
            final Optional<String> asset = asset(folder.get(), resource);

            final ResolvedAssetAndPath.Builder builder = ResolvedAssetAndPath.builder();
            builder.resolvedHost(host)
                    .host(host.getHostname())
                    .resolvedFolder(folder.get())
                    .path(decodedPath);

            asset.ifPresent(builder::asset);
            return builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Error Parsing uri:" + url, e);
        }
    }

    /**
     * First thing we need to do is resolve the site portion from the uri.
     * If we fail then no need to continue.
     * @param hostName
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<Host> resolveHosBytName(final String hostName, final User user) throws DotDataException, DotSecurityException {
            Preconditions.checkNotEmpty(hostName,IllegalArgumentException.class, String.format("can not resolve a valid hostName [%s]", hostName));
            final Host siteByName = hostAPI.findByName(hostName, user, false);
            if (null != siteByName && UtilMethods.isSet(siteByName.getIdentifier())) {
                return Optional.of(siteByName);
            }
            return Optional.empty();
    }

    /**
     *
     * @param rawPath
     * @param host
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<Folder> resolveExistingFolder(final String rawPath, final Host host, final User user) throws DotDataException, DotSecurityException {
        Preconditions.checkNotEmpty(rawPath,IllegalArgumentException.class, String.format("Failed determining path from [%s].", rawPath));
        String path = BLANK.equals(rawPath) ? FORWARD_SLASH : rawPath;
        path = !path.endsWith(FORWARD_SLASH) ? path + FORWARD_SLASH : path;
        final Folder folderByPath = folderAPI.findFolderByPath(path, host, user, false);
       if (null != folderByPath && UtilMethods.isSet(folderByPath.getInode())) {
            return Optional.of(folderByPath);
        }

        return Optional.empty();
    }

    /**
     * here we test a specific case we try to resolve anything that matches a pattern like forward slash followed by a string
     *
     * @param decodedRawPath the decoded raw path
     * @param host
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<FolderAndAsset> resolveAssetAndFolder(final String decodedRawPath, final Host host,
            final User user, final boolean createMissingFolder)
            throws DotDataException, DotSecurityException {

        final String startsWithForwardSlash = "^\\/[a-zA-Z0-9\\.\\-]+$";
        // if our path starts with / followed by a string  then we're looking at file asset in the root folder
        if (decodedRawPath.matches(startsWithForwardSlash)) {
            final String[] split = decodedRawPath.split(FORWARD_SLASH, 2);
            return  Optional.of(
                    FolderAndAsset.builder().folder(folderAPI.findSystemFolder()).asset(split[1]).build()
           );
        }
        //if we're not looking at the root folder then we need to extract the parent folder and the asset name
        final int index = decodedRawPath.lastIndexOf(FORWARD_SLASH);
        if(index == -1){
            return Optional.empty();
        }

        final String parentFolderPath = Try.of(() -> decodedRawPath.substring(0, index))
                .getOrNull();
        final String assetName = Try.of(() -> decodedRawPath.substring(index + 1)).getOrNull();
        final String folderPath = parentFolderPath + FORWARD_SLASH;
        final Folder parentFolder = folderAPI.findFolderByPath(folderPath, host, user, false);

        if (null != parentFolder && UtilMethods.isSet(parentFolder.getInode())) {
            return Optional.of(
                    FolderAndAsset.builder().folder(parentFolder).asset(assetName).build()
            );
        }

       if(createMissingFolder){
           final Folder folder = folderAPI.createFolders(folderPath, host, user, false);
           return Optional.of(
                   FolderAndAsset.builder().folder(folder).asset(assetName).build()
           );
       }

        return Optional.empty();
    }


    /**
     * Extract the relevant file asset name if any otherwise return null
     * if the extracted name is equal to the folder name we assume there's no asset name
     * @param resolvedFolder
     * @param rawResource
     * @return
     */
    Optional<String> asset(final Folder resolvedFolder, final String rawResource) {

        final var decodedRawResource = URLDecoder.decode(rawResource, StandardCharsets.UTF_8);

        String resource = decodedRawResource.toLowerCase();
        final int index = resource.lastIndexOf(FORWARD_SLASH);
        if (index == 0) {
            return Optional.empty();
        } else {
                //subtract the folder name from the resource, so we get the asset name
                // if for example we have a URL like:
                // `//demo.dotcms.com/images/blogs` or `//demo.dotcms.com/images/blogs/`
                //  and the folder name is `/images/blogs/`  we should return an empty string
                //  if the url contains a resource like
                // `//demo.dotcms.com/images/blogs/xyz.jpg` and a path like `/images/blogs/xyz.jpg
                // we should remove the folder name from the path so the result is `xyz.jpg`
                String folderPath = resolvedFolder.getPath().toLowerCase();
                if(folderPath.endsWith(FORWARD_SLASH)){
                    folderPath = folderPath.replaceAll(".$","");
                }
                resource = resource.replaceFirst(folderPath, BLANK);
                resource = resource.replace(FORWARD_SLASH, BLANK);
        }
        return UtilMethods.isNotSet(resource) ? Optional.empty() :  Optional.of(resource);
    }

    /**
     * Factory method for creating a new instance of AssetPathResolver
     * @return a new instance of AssetPathResolver
     */
    public static AssetPathResolver newInstance() {
        return new AssetPathResolver();
    }

}
