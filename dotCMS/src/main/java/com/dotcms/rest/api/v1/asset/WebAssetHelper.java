package com.dotcms.rest.api.v1.asset;

import com.dotcms.api.tree.TreeableAPI;
import com.dotcms.rest.api.v1.asset.view.AssetVersionsView;
import com.dotcms.rest.api.v1.asset.view.AssetView;
import com.dotcms.rest.api.v1.asset.view.FolderView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WebAssetHelper {

    FolderAPI folderAPI;
    HostAPI hostAPI;

    LanguageAPI languageAPI;

    TreeableAPI treeableAPI;

    FileAssetAPI fileAssetAPI;

    public WebAssetHelper(final FolderAPI folderAPI, final HostAPI hostAPI,
            final LanguageAPI languageAPI, final TreeableAPI treeableAPI,
            final FileAssetAPI fileAssetAPI) {
        this.folderAPI = folderAPI;
        this.hostAPI = hostAPI;
        this.languageAPI = languageAPI;
        this.treeableAPI = treeableAPI;
        this.fileAssetAPI = fileAssetAPI;
    }

    public WebAssetHelper() {
        this(APILocator.getFolderAPI(), APILocator.getHostAPI(), APILocator.getLanguageAPI(),
                APILocator.getTreeableAPI(), APILocator.getFileAssetAPI());
    }

    public WebAssetView getAsset(final String path, final User user)
            throws DotDataException, DotSecurityException {

        final ResolvedAssetAndPath assetAndPath = AssetPathResolver.newInstance()
                .resolve(path, user);
        final Host host = assetAndPath.resolvedHost();
        final Folder folder = assetAndPath.resolvedFolder();
        final String assetName = assetAndPath.asset();

        final List<FileAsset> assets;

        if (null != assetName) {
            //We're requesting an asset specifically therefore we need to find it and  build the response
            assets = fileAssetAPI.findVersionsByName(host, folder, assetName, user);
            return AssetVersionsView.builder()
                    .versions(toAssets(assets))
                    .build();
        } else {
           //We're requesting a folder and all of its contents
            final List<Folder> subFolders;
            //We're not really looking at system but / is mapped as system folder therefore
            //whenever system folder pops up we need to find the folders straight under host
            if(Folder.SYSTEM_FOLDER.equals(folder.getInode())){
                subFolders = folderAPI.findSubFolders(host, user, false);
            } else {
                subFolders = folderAPI.findSubFolders(folder, user, false);
            }
            final Map<Identifier, List<FileAsset>> versionsUnderFolder = fileAssetAPI.findVersionsUnderFolder(
                    host, folder, user);

            assets = versionsUnderFolder.values().stream().flatMap(List::stream).collect(Collectors.toList());

            return FolderView.builder()
                    .path(folder.getPath())
                    .name(folder.getName())
                    .modDate(folder.getModDate().toInstant())
                    .identifier(folder.getIdentifier())
                    .inode(folder.getInode())
                    .subFolders(toAssetFolders(subFolders))
                    .assets(
                            AssetVersionsView.builder().versions(toAssets(assets)).build()
                    )
                    .build();
        }
    }

    Iterable<AssetView> toAssets(final List<FileAsset> assets) {
        return assets.stream().map(this::toAsset).collect(Collectors.toList());
    }

    AssetView toAsset(final FileAsset fileAsset) {
        final Language language = languageAPI.getLanguage(fileAsset.getLanguageId());
        final boolean live = Try.of(fileAsset::isLive)
                .getOrElse(() -> {
                    Logger.warn(this, String.format("Unable to determine status for asset: [%s]",
                            fileAsset.getIdentifier()));
                    return false;
                });
        return AssetView.builder()
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

    Iterable<FolderView> toAssetFolders(final List<Folder> subFolders) {
        return subFolders.stream().map(this::toAssetsFolder).collect(Collectors.toList());
    }

    FolderView toAssetsFolder(final Folder folder) {
        return FolderView.builder()
                .path(folder.getPath())
                .name(folder.getName())
                .modDate(folder.getModDate().toInstant())
                .identifier(folder.getIdentifier())
                .inode(folder.getInode())
                .build();
    }
}
