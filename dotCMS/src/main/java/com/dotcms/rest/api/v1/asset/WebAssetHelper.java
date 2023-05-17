package com.dotcms.rest.api.v1.asset;

import com.dotcms.api.tree.TreeableAPI;
import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.browser.BrowserQuery.Builder;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.api.v1.asset.view.AssetVersionsView;
import com.dotcms.rest.api.v1.asset.view.AssetView;
import com.dotcms.rest.api.v1.asset.view.FolderView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Treeable;
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
import java.util.Optional;
import java.util.stream.Collectors;

public class WebAssetHelper {

    LanguageAPI languageAPI;

    FileAssetAPI fileAssetAPI;

    BrowserAPI browserAPI;

    public WebAssetHelper(
            final LanguageAPI languageAPI,
            final FileAssetAPI fileAssetAPI, final BrowserAPI browserAPI) {
        this.languageAPI = languageAPI;
        this.fileAssetAPI = fileAssetAPI;
        this.browserAPI = browserAPI;
    }

    public WebAssetHelper() {
        this(
                APILocator.getLanguageAPI(),
                APILocator.getFileAssetAPI(),
                APILocator.getBrowserAPI());
    }

    public WebAssetView getAsset(final String path, final User user)
            throws DotDataException, DotSecurityException {

        final ResolvedAssetAndPath assetAndPath = AssetPathResolver.newInstance()
                .resolve(path, user);
        final Host host = assetAndPath.resolvedHost();
        final Folder folder = assetAndPath.resolvedFolder();
        final String assetName = assetAndPath.asset();

        final List<Treeable> assets;

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
            if(folder.isSystemFolder()){
                builder.withHostOrFolderId(host.getIdentifier());
            } else {
                builder.withHostOrFolderId(folder.getInode());
            }
            builder.withFilter(assetName);
            final List<Treeable> folderContent = browserAPI.getFolderContentList(builder.build());
            assets = folderContent.stream().filter(Contentlet.class::isInstance).collect(Collectors.toList());
            if (assets.isEmpty()) {
                throw new NotFoundInDbException(String.format(" Asset [%s] not found", assetName));
            }
            return AssetVersionsView.builder()
                    .versions(toAssets(assets))
                    .build();
        } else {
            final List<Treeable> folderContent;
            //We're requesting a folder and all of its contents
            final List<Folder> subFolders;
            //We're not really looking at system but / is mapped as system folder therefore
            //whenever system folder pops up we need to find the folders straight under host
            if (folder.isSystemFolder()) {

                builder.withHostOrFolderId(host.getIdentifier());
                folderContent = browserAPI.getFolderContentList(builder.build());

            } else {

                builder.withHostOrFolderId(folder.getInode());
                folderContent = browserAPI.getFolderContentList(builder.build());
            }

            subFolders = folderContent.stream().filter(Folder.class::isInstance)
                    .map(f -> (Folder) f).collect(Collectors.toList());
            assets = folderContent.stream().filter(Contentlet.class::isInstance)
                    .map(f -> (Contentlet) f).collect(Collectors.toList());

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

    Iterable<AssetView> toAssets(final List<Treeable> assets) {
        return assets.stream().filter(Contentlet.class::isInstance).map(Contentlet.class::cast)
                .filter(Contentlet::isFileAsset)
                .map(contentlet -> fileAssetAPI.fromContentlet(contentlet)).map(this::toAsset)
                .collect(Collectors.toList());
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
