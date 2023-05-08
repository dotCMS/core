package com.dotcms.rest.api.v1.asset;

import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.DOUBLE_SLASH;
import static com.liferay.util.StringPool.FORWARD_SLASH;
import com.dotcms.rest.exception.NotFoundException;
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
        final ResolvedAssetAndPath.Builder builder = ResolvedAssetAndPath.builder();

        if (FORWARD_SLASH.equals(url) || DOUBLE_SLASH.equals(url)) {
            final Host defaultHost = hostAPI.findDefaultHost(user, false);
            return builder.host(defaultHost.getName()).resolvedHost(defaultHost)
                    .resolvedFolder(folderAPI.findSystemFolder()).path(FORWARD_SLASH).build();
        }
        try {
            final URI uri = new URI(url);
            final String host = uri.getHost();
            if (null == host) {
                //Whenever we can determine a failure caused by an invalid input we should throw an IllegalArgumentException
                throw new IllegalArgumentException(String.format(
                        "Unable to determine host: [%s]. host  must start with a valid protocol or simply // ",
                        url));
            }

            final Host siteByName = hostAPI.findByName(host, user, false);
            if (null == siteByName || UtilMethods.isNotSet(siteByName.getIdentifier())) {
                //If the input is valid, but we can't find a host we should throw a NotFoundException
                throw new NotFoundException(
                        String.format("Unable to determine a valid host from path: [%s].", host));
            }

            final String path = BLANK.equals(uri.getPath()) ? FORWARD_SLASH : uri.getPath();
            if (null == path) {
                throw new IllegalArgumentException(
                        String.format("Unable to determine path: [%s].", url));
            }

            Folder folderByPath = folderAPI.findFolderByPath(path, siteByName, user, false);
            //if we fail to determine a folder we should try to  find a file looking at parent folder
            if (null == folderByPath || UtilMethods.isNotSet(folderByPath.getInode())) {
                Logger.debug(this,
                        String.format("Failed determining path from [%s] trying parent folder.",
                                path));
                final Optional<String> parentFolder = parentFolder(path);
                if (parentFolder.isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("Unable to determine a valid folder from path: [%s].",
                                    path));
                }
                folderByPath = folderAPI.findFolderByPath(path, host, user, false);
            }

            if (null == folderByPath || UtilMethods.isNotSet(folderByPath.getInode())) {
                throw new NotFoundException(
                        String.format("Unable to determine a valid folder from path: [%s].", path));
            }

            final String resource =
                    uri.getRawQuery() != null ? uri.getPath() + "?" + uri.getRawQuery()
                            : uri.getPath();
            final String asset = asset(resource);

            return builder.resolvedHost(siteByName).host(host).resolvedFolder(folderByPath)
                    .path(path).asset(asset).build();
        } catch (URISyntaxException | DotSecurityException e) {
            throw new IllegalArgumentException("Error Parsing uri:" + url, e);
        }
    }

    /**
     * Extract the relevant file asset name if any otherwise return null
     * @param resource
     * @return
     */
    private String asset(String resource) {
        final int index = resource.lastIndexOf(FORWARD_SLASH);
        if (index > 0) {
            resource = (resource.substring(index)).replace(FORWARD_SLASH, BLANK);
        } else {
            //This should force returning null
            resource = null;
        }
        return "".equals(resource) ? null : resource;
    }

    Optional<String> parentFolder(final String path) {
        if (FORWARD_SLASH.equals(path)) {
            return Optional.empty();
        }
        String workingPath = path;
        if (workingPath.endsWith(FORWARD_SLASH)) {
            workingPath = workingPath.substring(0, workingPath.length() - 1);
        }
        final int index = workingPath.lastIndexOf(FORWARD_SLASH);
        if (index > 0) {
            workingPath = workingPath.substring(0, index);
        }
        return Optional.of(workingPath);
    }

    /**
     * Factory method for creating a new instance of AssetPathResolver
     * @return a new instance of AssetPathResolver
     */
    public static AssetPathResolver newInstance() {
        return new AssetPathResolver();
    }

}
