package com.dotcms.rest.api.v1.asset;

import com.dotcms.api.tree.TreeableAPI;
import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.browser.BrowserQuery.Builder;
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
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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

    BrowserAPI browserAPI;

    public WebAssetHelper(final FolderAPI folderAPI, final HostAPI hostAPI,
            final LanguageAPI languageAPI, final TreeableAPI treeableAPI,
            final FileAssetAPI fileAssetAPI, final BrowserAPI browserAPI) {
        this.folderAPI = folderAPI;
        this.hostAPI = hostAPI;
        this.languageAPI = languageAPI;
        this.treeableAPI = treeableAPI;
        this.fileAssetAPI = fileAssetAPI;
        this.browserAPI = browserAPI;
    }

    public WebAssetHelper() {
        this(APILocator.getFolderAPI(), APILocator.getHostAPI(),
                APILocator.getLanguageAPI(),
                APILocator.getTreeableAPI(), APILocator.getFileAssetAPI(),
                APILocator.getBrowserAPI());
    }

    public WebAssetView getAsset(final String path, final User user)
            throws DotDataException, DotSecurityException {

        final ResolvedAssetAndPath assetAndPath = AssetPathResolver.newInstance()
                .resolve(path, user);
        final Host host = assetAndPath.resolvedHost();
        final Folder folder = assetAndPath.resolvedFolder();
        final String assetName = assetAndPath.asset();

        final List<FileAsset> assets;

        final Builder builder = BrowserQuery.builder();
        builder.showDotAssets(false)
                .withUser(user)
                .showFiles(true)
                .showFolders(true)
                .showArchived(false)
                .showWorking(true)
                .showLinks(false)
                .showDotAssets(false)
                .showImages(true)
                .showContent(true);

        if (null != assetName) {

            //We're requesting an asset specifically therefore we need to find it and  build the response
            builder.withFilter(assetName);
            final Map<String, Object> folderContent = browserAPI.getFolderContent(builder.build());
            System.out.println(folderContent);

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

                builder.withHostOrFolderId(host.getIdentifier());
                final Map<String, Object> folderContent = browserAPI.getFolderContent(builder.build());
                System.out.println(folderContent);

                subFolders = folderAPI.findSubFolders(host, user, false);
            } else {

                builder.withHostOrFolderId(folder.getInode());
                final Map<String, Object> folderContent = browserAPI.getFolderContent(builder.build());
                System.out.println(folderContent);


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

    public void getContentsUnderFolder(BrowserQuery query)
            throws DotDataException, DotSecurityException {
        final Map<String, Object> folderContent = browserAPI.getFolderContent(query);
        @SuppressWarnings("unchecked")
        final List<Map<String,Object>> list = (List)folderContent.get("list");
        //TODO: need to get from this list the attribute 'type'
        //then from there we can determine if it's a folder or a contetlet
        //For folders we can use FolderTransformer but for the returned map we can not use ContentletTransformer
        //The results from this browserAPI have never been seen as a Contetlet Per se apparently they have only been used to generate json
        //for that purpose we need to grab the inodes and load the contentlets from there it's easier
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
