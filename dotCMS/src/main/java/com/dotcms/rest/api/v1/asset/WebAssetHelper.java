package com.dotcms.rest.api.v1.asset;

import com.dotcms.api.tree.TreeableAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.List;
import java.util.stream.Collectors;

public class WebAssetHelper {

    FolderAPI folderAPI;
    HostAPI hostAPI;

    LanguageAPI languageAPI;

    TreeableAPI treeableAPI;

    public WebAssetHelper(final FolderAPI folderAPI, final HostAPI hostAPI, final LanguageAPI languageAPI, final TreeableAPI treeableAPI) {
        this.folderAPI = folderAPI;
        this.hostAPI = hostAPI;
        this.languageAPI = languageAPI;
        this.treeableAPI = treeableAPI;
    }

    public WebAssetHelper() {
        this(APILocator.getFolderAPI(), APILocator.getHostAPI(), APILocator.getLanguageAPI(), APILocator.getTreeableAPI());
    }

    public SimpleWebAsset getAsset(final String path, final User user)
            throws DotDataException, DotSecurityException {

        final ResolvedAssetAndPath assetAndPath = AssetPathResolver.getInstance().resolve(path, user);
        final Host host = assetAndPath.resolvedHost();
        final Folder folder = assetAndPath.resolvedFolder();

        final List<Treeable> assets;
        final List<Folder> subFolder;

        if(Folder.ROOT_FOLDER.equals(folder.getInode())){
           subFolder = folderAPI.findSubFolders(host, user, false);
           assets = treeableAPI.loadAssetsUnderHost(host, user, true, true, false, false);
        } else {
           subFolder = folderAPI.findSubFolders(folder, user, false);
           assets = treeableAPI.loadAssetsUnderFolder(folder, user, true, true, false, false);
        }

        return AssetsFolder.builder()
                .path(folder.getPath())
                .name(folder.getName())
                .modDate(folder.getModDate().toInstant())
                .identifier(folder.getIdentifier())
                .inode(folder.getInode())
                .assets(toAssets(assets))
                .subFolders(toAssetFolders(subFolder))
                .build();
    }

    Iterable<Asset> toAssets(final List<Treeable> assets) {
       return assets.stream().filter(FileAsset.class::isInstance).map(c -> (FileAsset) c).map(this::toAsset).collect(Collectors.toList());
    }

    Asset toAsset(final FileAsset fileAsset) {
        final Language language = languageAPI.getLanguage(fileAsset.getLanguageId());
        final boolean live = Try.of(fileAsset::isLive)
                .getOrElse(() -> {
                    Logger.warn(this, String.format("Unable to determine status for asset: [%s]", fileAsset.getIdentifier()));
                    return false;
                });
        return Asset.builder()
                .name(fileAsset.getFileName())
                .modDate(fileAsset.getModDate().toInstant())
                .identifier(fileAsset.getIdentifier())
                .inode(fileAsset.getInode())
                .path(fileAsset.getPath())
                .sha256(fileAsset.getSha256())
                .size(fileAsset.getFileSize())
                .live(live)
                .lang(language.toString())
                .build();
    }

    Iterable<AssetsFolder> toAssetFolders(final List<Folder> subFolders) {
        return subFolders.stream().map(this::toAssetsFolder).collect(Collectors.toList());
    }

    AssetsFolder toAssetsFolder(final Folder folder) {
        return AssetsFolder.builder()
                .path(folder.getPath())
                .name(folder.getName())
                .modDate(folder.getModDate().toInstant())
                .identifier(folder.getIdentifier())
                .inode(folder.getInode())
                .build();
    }
}
