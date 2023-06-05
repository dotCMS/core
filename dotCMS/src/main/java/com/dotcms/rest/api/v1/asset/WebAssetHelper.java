package com.dotcms.rest.api.v1.asset;

import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.browser.BrowserQuery.Builder;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.api.v1.asset.view.AssetVersionsView;
import com.dotcms.rest.api.v1.asset.view.AssetView;
import com.dotcms.rest.api.v1.asset.view.FolderView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In typical dotCMS resource fashion this class is responsible for undertaking the heavy lifting
 * Our Resource classes are responsible for handling the request and response. and call out to helpers
 */
public class WebAssetHelper {

    LanguageAPI languageAPI;

    FileAssetAPI fileAssetAPI;

    ContentletAPI contentletAPI;

    BrowserAPI browserAPI;

    /**
     * Constructor for testing
     * @param languageAPI
     * @param fileAssetAPI
     * @param contentletAPI
     * @param browserAPI
     */
    WebAssetHelper(
            final LanguageAPI languageAPI,
            final FileAssetAPI fileAssetAPI,
            final ContentletAPI contentletAPI,
            final BrowserAPI browserAPI) {
        this.languageAPI = languageAPI;
        this.fileAssetAPI = fileAssetAPI;
        this.contentletAPI = contentletAPI;
        this.browserAPI = browserAPI;
    }

    /**
     * Default constructor
     */
    WebAssetHelper() {
        this(
                APILocator.getLanguageAPI(),
                APILocator.getFileAssetAPI(),
                APILocator.getContentletAPI(),
                APILocator.getBrowserAPI()
        );
    }

    /**
     * Entry point here it is determined if the path is a folder or an asset.
     * If it is a folder it will return a FolderView, if it is an asset it will return an AssetView
     * @param path
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
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
            Logger.debug(this, String.format("Asset name: [%s]" , assetName));
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
            Logger.debug(this, String.format("Retrieving a folder by name: [%s] " , folder.getName()));
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
                    .sortOrder(folder.getSortOrder())
                    .filesMasks(folder.getFilesMasks())
                    .defaultFileType(folder.getDefaultFileType())
                    .host(folder.getHostId())
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

    /**
     * Converts a list of folders to a list of {@link FolderView}
     * @param assets folders to convert
     * @return list of {@link FolderView}
     */
    Iterable<AssetView> toAssets(final List<Treeable> assets) {
        return assets.stream().filter(Contentlet.class::isInstance).map(Contentlet.class::cast)
                .filter(Contentlet::isFileAsset)
                .map(contentlet -> fileAssetAPI.fromContentlet(contentlet)).map(this::toAsset)
                .collect(Collectors.toList());
    }

    /**
     * Converts a file asset to a {@link AssetView}
     * @param fileAsset file asset to convert
     * @return {@link AssetView}
     */
    AssetView toAsset(final FileAsset fileAsset) {
        final Language language = languageAPI.getLanguage(fileAsset.getLanguageId());
        final boolean live = Try.of(fileAsset::isLive)
                .getOrElse(() -> {
                    Logger.warn(this, String.format("Unable to determine status for asset: [%s]",
                            fileAsset.getIdentifier()));
                    return false;
                });

        final Map<String, ? extends Serializable> metadata = Map.of(
                "name", fileAsset.getUnderlyingFileName(),
                "title", fileAsset.getFileTitle(),
                "path", fileAsset.getPath(),
                "sha256", fileAsset.getSha256(),
                "contentType", fileAsset.getMimeType(),
                "size", fileAsset.getFileSize(),
                "isImage", fileAsset.isImage(),
                "width", fileAsset.getWidth(),
                "height", fileAsset.getHeight(),
                "modDate", fileAsset.getModDate().toInstant()
        );

        return AssetView.builder()
                .sortOrder(fileAsset.getSortOrder())
                .name(fileAsset.getFileName())
                .modDate(fileAsset.getModDate().toInstant())
                .identifier(fileAsset.getIdentifier())
                .inode(fileAsset.getInode())
                .live(live)
                .lang(language.toString())
                .metadata(metadata)
                .build();
    }

    /**
     * Converts a list of folders to a list of {@link FolderView}
     * @param subFolders The folders to convert
     * @return list of {@link FolderView}
     */
    Iterable<FolderView> toAssetFolders(final List<Folder> subFolders) {
        return subFolders.stream().map(this::toAssetsFolder).collect(Collectors.toList());
    }

    /**
     * Converts a folder to a {@link FolderView}
     * @param folder The folder to convert
     * @return {@link FolderView}
     */
    FolderView toAssetsFolder(final Folder folder) {
        return FolderView.builder()
                .path(folder.getPath())
                .name(folder.getName())
                .title(folder.getTitle())
                .host(folder.getHostId())
                .filesMasks(folder.getFilesMasks())
                .defaultFileType(folder.getDefaultFileType())
                .showOnMenu(folder.isShowOnMenu())
                .modDate(folder.getModDate().toInstant())
                .identifier(folder.getIdentifier())
                .inode(folder.getInode())
                .build();
    }

    public void createOrReplaceAsset(final String rawPath, final String fileName, final InputStream fileInputStream,  final User user)
            throws DotDataException, DotSecurityException, IOException {

        final ResolvedAssetAndPath assetAndPath = AssetPathResolver.newInstance()
                .resolve(rawPath, user, true);

        final Host host = assetAndPath.resolvedHost();
        final Folder folder = assetAndPath.resolvedFolder();
        final String assetName = assetAndPath.asset();

        System.out.println(host);
        System.out.println(folder);
        System.out.println(assetName);

        if(null != fileInputStream) {
            //TODO validate size length

            final Path tempFile = Files.createTempFile(assetName, "temp");

            Files.copy(fileInputStream, tempFile);

            final Contentlet contentlet = makeFileAsset(tempFile, folder);
            final Contentlet savedContentlet = contentletAPI.checkin(contentlet, user, false);

        }
    }

        Contentlet makeFileAsset(final Path filePath, final Folder folder){
            final File file = filePath.toFile();
            final Contentlet contentlet = new Contentlet();
            final String fileName = file.getName();
            contentlet.setProperty(FileAssetAPI.TITLE_FIELD, fileName);
            contentlet.setProperty(FileAssetAPI.FILE_NAME_FIELD, fileName);
            contentlet.setProperty(FileAssetAPI.BINARY_FIELD, file);
            contentlet.setFolder(folder.getInode());
            return contentlet;
        }



    /**
     * Creates a new instance of {@link WebAssetHelper}
     * @return {@link WebAssetHelper}
     */
    public static WebAssetHelper newInstance(){
        return new WebAssetHelper();
    }

}
