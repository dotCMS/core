package com.dotcms.rest.api.v1.asset;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
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
    IdentifierAPI identifierAPI;

    AssetPathResolver(){
      this(APILocator.getFolderAPI(), APILocator.getHostAPI(), APILocator.getIdentifierAPI());
    }

    AssetPathResolver(final FolderAPI folderAPI, final HostAPI hostAPI, final IdentifierAPI identifierAPI){
        this.folderAPI = folderAPI;
        this.hostAPI = hostAPI;
        this.identifierAPI = identifierAPI;
    }

    public ResolvedAssetAndPath resolve(final String url, final User user) throws DotDataException {
        final ResolvedAssetAndPath.Builder builder = ResolvedAssetAndPath.builder();

        if("/".equals(url) || "//".equals(url)){
            return builder.host(Host.SYSTEM_HOST).resolvedHost(hostAPI.findSystemHost()).build();
        }
        try {
            final URI uri = new URI(url);
            final String host = uri.getHost();
            if(null == host){
                throw new IllegalArgumentException(String.format("Unable to determine host: [%s]. host  must start with a valid protocol or simply // ",url));
            }

            final Host siteByName = hostAPI.findByName(host, user, false);
            if(null == siteByName || UtilMethods.isNotSet(siteByName.getIdentifier())){
                throw new IllegalArgumentException(String.format("Unable to determine a valid host from path: [%s].",host));
            }

            final String path = uri.getPath();
            if(null == path){
                throw new IllegalArgumentException(String.format("Unable to determine path: [%s].",url));
            }

            Folder folderByPath = folderAPI.findFolderByPath(path, siteByName, user, false);
            //if we fail to determine a folder we should try to  find a file looking at parent folder
            if(null == folderByPath || UtilMethods.isNotSet(folderByPath.getInode())){
                Logger.debug(this, String.format("Failed determining path from [%s] trying parent folder.",path));
                final Optional<String> parentFolder = parentFolder(path);
                if(parentFolder.isEmpty()){
                    throw new IllegalArgumentException(String.format("Unable to determine a valid folder from path: [%s].",path));
                }
                folderByPath = findFolderByPath(parentFolder.get(), siteByName, user);
            }

            if(null == folderByPath || UtilMethods.isNotSet(folderByPath.getInode())){
                throw new IllegalArgumentException(String.format("Unable to determine a valid folder from path: [%s].",path));
            }

            final String resource = uri.getRawQuery() != null ? uri.getPath() + "?" + uri.getRawQuery() : uri.getPath();
            final String asset = asset(resource);

            return builder.resolvedHost(siteByName).host(host).resolvedFolder(folderByPath).path(path).asset(asset).build();
        } catch (URISyntaxException | DotSecurityException e) {
            throw new IllegalArgumentException("Error Parsing uri:" + url ,e);
        }
    }

    Folder findFolderByPath(final String path, final Host host, final User user)
            throws DotStateException, DotDataException, DotSecurityException{
        if (path.equals("/")) {
            //Just a marker to indicate upper layers that we are looking at the site's root folder
            Folder folder = new Folder();
            folder.setHostId(host.getIdentifier());
            folder.setPath("/");
            folder.setInode(Folder.ROOT_FOLDER);
        }
        //When passing path like "/" this always resolves System Folder regardless of the host
        //Therefore we needed to handle this case above separately
        return folderAPI.findFolderByPath(path, host, user, false);
    }

    private  String asset(String resource){
        final int index = resource.lastIndexOf("/");
        if(index > 0){
            resource = (resource.substring(index)).replace("/", "");
        } else {
            //This should force returning null
            resource = null;
        }
        return "".equals(resource) ? null : resource;
    }

    Optional<String> parentFolder(final String path) {
        if ("/".equals(path)) {
            return Optional.empty();
        }
        String workingPath = path;
        if (workingPath.endsWith("/")) {
            workingPath = workingPath.substring(0, workingPath.length() - 1);
        }
        final int index = workingPath.lastIndexOf("/");
        if (index > 0) {
            workingPath = workingPath.substring(0, index);
        }
        return Optional.of(workingPath);
    }

    public static AssetPathResolver getInstance(){
        return new AssetPathResolver();
    }

}
