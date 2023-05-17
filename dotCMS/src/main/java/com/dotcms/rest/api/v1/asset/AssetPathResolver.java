package com.dotcms.rest.api.v1.asset;

import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.DOUBLE_SLASH;
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
import java.util.Optional;

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

    public ResolvedAssetAndPath resolve(final String url, final User user)
            throws DotDataException, DotSecurityException {

        try {
            final URI uri = new URI(url);
            final Optional<Host> siteByName = resolveHosBytName(uri.getHost(), user);
            if(siteByName.isEmpty()){
                throw new NotFoundException(String.format("Unable to determine a valid host from uri: [%s].", url));
            }

            final Host host = siteByName.get();
            final String path = BLANK.equals(uri.getPath()) ? FORWARD_SLASH : uri.getPath();
            if (null == path) {
                throw new IllegalArgumentException(String.format("Unable to determine path: [%s].", url));
            }

            //This line determines if the path is a folder
            final Optional<Folder> folder = resolveFolder(path, host, user);
            if(folder.isEmpty()){
                //if we've got this far we need to verify if the path is an asset. The folder will be expected to be the parent folder
                Optional<FolderAndAsset> folderAndAsset = resolveAssetAndFolder(uri.getPath(), host, user);
                if(folderAndAsset.isEmpty()){
                    throw new NotFoundInDbException(String.format("Unable to determine a valid folder or asset from uri: [%s].", url));
                }
                final FolderAndAsset folderAsset = folderAndAsset.get();
                return ResolvedAssetAndPath.builder()
                        .resolvedHost(host)
                        .host(host.getHostname())
                        .resolvedFolder(folderAsset.folder())
                        .path(path)
                        .asset(folderAsset.asset())
                        .build();
            }

            //if we succeed to determine a valid folder from the path then we resolve the last bit as an asset name
            final String resource = uri.getRawQuery() != null ? uri.getPath() + "?" + uri.getRawQuery() : uri.getPath();
            final Optional<String> asset = asset(folder.get(), resource);

            final ResolvedAssetAndPath.Builder builder = ResolvedAssetAndPath.builder();
            builder.resolvedHost(host)
                    .host(host.getHostname())
                    .resolvedFolder(folder.get())
                    .path(path);

            asset.ifPresent(builder::asset);
            return builder.build();
        } catch (URISyntaxException | DotSecurityException e) {
            throw new IllegalArgumentException("Error Parsing uri:" + url, e);
        }
    }

    Optional<Host> resolveHosBytName(final String hostName, final User user) throws DotDataException, DotSecurityException {
            Preconditions.checkNotEmpty(hostName,IllegalArgumentException.class, String.format("can not resolve a valid hostName [%s]", hostName));
            final Host siteByName = hostAPI.findByName(hostName, user, false);
            if (null != siteByName && UtilMethods.isSet(siteByName.getIdentifier())) {
                return Optional.of(siteByName);
            }
            return Optional.empty();
    }

    Optional<Folder> resolveFolder(final String rawPath, final Host host, final User user) throws DotDataException, DotSecurityException {
        Preconditions.checkNotEmpty(rawPath,IllegalArgumentException.class, String.format("Failed determining path from [%s].", rawPath));
        String path = BLANK.equals(rawPath) ? FORWARD_SLASH : rawPath;
        path = !path.endsWith(FORWARD_SLASH) ? path + FORWARD_SLASH : path;
        final Folder folderByPath = folderAPI.findFolderByPath(path, host, user, false);
       if (null != folderByPath && UtilMethods.isSet(folderByPath.getInode())) {
            return Optional.of(folderByPath);
        }

        return Optional.empty();
    }

    Optional<FolderAndAsset> resolveAssetAndFolder(final String rawPath, final Host host, final User user)
            throws DotDataException, DotSecurityException {
        final String startsWithForwardSlash = "^\\/[a-zA-Z0-9\\.\\-]+$";
        // if our path and a path then we're looking at file asset in the root folder
        if(rawPath.matches(startsWithForwardSlash)){
            final String[] split = rawPath.split(FORWARD_SLASH, 2);
            return  Optional.of(
                    FolderAndAsset.builder().folder(folderAPI.findSystemFolder()).asset(split[1]).build()
           );
        }

        final int index = rawPath.lastIndexOf(FORWARD_SLASH);
        if(index == -1){
            return Optional.empty();
        }

        final String parentFolderPath = Try.of(()-> rawPath.substring(0, index)).getOrNull();
        final String assetName = Try.of(()->rawPath.substring(index + 1)).getOrNull();
        final Folder parentFolder = folderAPI.findFolderByPath(parentFolderPath + FORWARD_SLASH, host, user, false);

        if (null != parentFolder && UtilMethods.isSet(parentFolder.getInode())) {
            return Optional.of(
                    FolderAndAsset.builder().folder(parentFolder).asset(assetName).build()
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
        String resource = rawResource.toLowerCase();
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
