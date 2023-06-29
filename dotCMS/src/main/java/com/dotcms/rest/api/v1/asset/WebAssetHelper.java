package com.dotcms.rest.api.v1.asset;

import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.browser.BrowserQuery.Builder;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.api.v1.asset.view.AssetVersionsView;
import com.dotcms.rest.api.v1.asset.view.AssetView;
import com.dotcms.rest.api.v1.asset.view.FolderView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.BooleanUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jetbrains.annotations.NotNull;

/**
 * In typical dotCMS resource fashion this class is responsible for undertaking the heavy lifting
 * Our Resource classes are responsible for handling the request and response. and call out to helpers
 */
public class WebAssetHelper {

    public static final String SORT_BY = "modDate";
    LanguageAPI languageAPI;

    FileAssetAPI fileAssetAPI;

    ContentletAPI contentletAPI;

    BrowserAPI browserAPI;

    TempFileAPI tempFileAPI;

    ContentTypeAPI contentTypeAPI;

    FolderAPI folderAPI;

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
            final BrowserAPI browserAPI,
            final TempFileAPI tempFileAPI,
            final ContentTypeAPI contentTypeAPI,
            final FolderAPI folderAPI
    ){
        this.languageAPI = languageAPI;
        this.fileAssetAPI = fileAssetAPI;
        this.contentletAPI = contentletAPI;
        this.browserAPI = browserAPI;
        this.tempFileAPI = tempFileAPI;
        this.contentTypeAPI = contentTypeAPI;
        this.folderAPI = folderAPI;
    }

    /**
     * Default constructor
     */
    WebAssetHelper() {
        this(
                APILocator.getLanguageAPI(),
                APILocator.getFileAssetAPI(),
                APILocator.getContentletAPI(),
                APILocator.getBrowserAPI(),
                APILocator.getTempFileAPI(),
                APILocator.getContentTypeAPI(APILocator.systemUser()),
                APILocator.getFolderAPI()
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
    public WebAssetView getAssetInfo(final String path, final User user)
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
                .showLinks(false)
                .showDotAssets(false)
                .showImages(true)
                .showContent(true)
                .sortBy(SORT_BY)
                .sortByDesc(true)
        ;

        //We're not really looking at system but / is mapped as system folder therefore
        //whenever system folder pops up we need to find the folders straight under host
        if (folder.isSystemFolder()) {
            builder.withHostOrFolderId(host.getIdentifier());
        } else {
            builder.withHostOrFolderId(folder.getInode());
        }

        if (null != assetName) {
            Logger.debug(this, String.format("Asset name: [%s]" , assetName));
            //We're requesting an asset specifically therefore we need to find it and  build the response
            builder.withFileName(assetName);

            final List<Treeable> folderContent = sortByIdentifier(
                    collectAllVersions(builder)
            );
            assets = folderContent.stream().filter(Contentlet.class::isInstance).collect(Collectors.toList());
            if (assets.isEmpty()) {
                throw new NotFoundInDbException(String.format(" Asset [%s] not found", assetName));
            }
            return AssetVersionsView.builder()
                    .versions(toAssets(assets))
                    .build();
        } else {
            Logger.debug(this, String.format("Retrieving a folder by name: [%s] " , folder.getName()));
            final List<Treeable> folderContent = browserAPI.getFolderContentList(builder.build());
            //We're requesting a folder and all of its contents
            final List<Folder> subFolders = folderContent.stream().filter(Folder.class::isInstance)
                    .map(f -> (Folder) f).collect(Collectors.toList());
            //Once we get the folder contents we need to include all other versions per identifier
            final Set<String> identifiers = folderContent.stream().filter(Contentlet.class::isInstance)
                    .map(f -> (Contentlet) f).map(Contentlet::getIdentifier)
                    .collect(Collectors.toSet());
           // Once we have all the identifiers corresponding to the assets under the folder we need to fetch all of their versions
           assets = sortByIdentifier(
                   contentletAPI.findLiveOrWorkingVersions(identifiers, user, false)
           );

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
     * Exclude contentlets that are not live or working and sort them by identifier
     * This to improve presentation of the assets in our API
     * @param contentlets
     * @return
     */
    List<Treeable> sortByIdentifier(Collection<Contentlet> contentlets) {
        return contentlets.stream()
                .sorted(Comparator.comparing(Contentlet::getIdentifier))
                .collect(Collectors.toList());
    }

    /**
     * We need to combine the live and working contents of a folder in one single list
     * That since the BrowserAPI is designed to return one or the other at a time
     * This needs to be wrapped in a set cuz when there's only one version of the asset, both live and working point to the same inode
     * Meaning the same asset would get returned twice
     * @param builder
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    final List<Contentlet> collectAllVersions(final Builder builder)
            throws DotDataException, DotSecurityException {
        final Optional<Treeable> first = browserAPI.getFolderContentList(
                builder.showWorking(true).build()).stream().findFirst();

        if(first.isPresent()){
           final String identifier = first.get().getIdentifier();
          return
                  contentletAPI.findLiveOrWorkingVersions(Set.of(identifier), APILocator.systemUser(),
                          false);
        }
        return List.of();
    }

    /**
     * Converts a list of folders to a list of {@link FolderView}
     * @param assets folders to convert
     * @return list of {@link FolderView}
     */
    Iterable<AssetView> toAssets(final Collection<Treeable> assets) {

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
                    Logger.warn(this, String.format("Unable to determine if asset: [%s] is live",
                            fileAsset.getIdentifier()));
                    return false;
                });

        final boolean working = Try.of(fileAsset::isWorking)
                .getOrElse(() -> {
                    Logger.warn(this, String.format("Unable to determine if asset: [%s] is in working state ",
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
                SORT_BY, fileAsset.getModDate().toInstant()
        );

        return AssetView.builder()
                .sortOrder(fileAsset.getSortOrder())
                .name(fileAsset.getFileName())
                .modDate(fileAsset.getModDate().toInstant())
                .identifier(fileAsset.getIdentifier())
                .inode(fileAsset.getInode())
                .live(live)
                .working(working)
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

    /**
     * Retrieve the binary from the given request params
     * @param form
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public FileAsset getAsset(final AssetsRequestForm form, final User user)
            throws DotDataException, DotSecurityException {

        final String path = form.assetPath();
        final ResolvedAssetAndPath assetAndPath = AssetPathResolver.newInstance()
                .resolve(path, user);
        final Host host = assetAndPath.resolvedHost();
        final Folder folder = assetAndPath.resolvedFolder();
        final String assetName = assetAndPath.asset();

        if (null == assetName) {
            throw new IllegalArgumentException("Unspecified Asset name.");
        }

        final boolean live = form.live();
        final Optional<Language> language = parseLang(form.language(),false);
        if (language.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Language [%s] not found.", form.language()));
        }

        final Builder builder = BrowserQuery.builder();
        builder.showDotAssets(false)
                .withUser(user)
                .showFiles(true)
                .showFolders(true)
                .showArchived(false)
                .showWorking(!live)
                .showLinks(false)
                .showDotAssets(false)
                .showImages(true)
                .showContent(true)
                .withLanguageId(language.get().getId())
                .sortBy(SORT_BY)
                .sortByDesc(true)
        ;

        //We're requesting an asset specifically therefore we need to find it and  build the response
        if (folder.isSystemFolder()) {
            builder.withHostOrFolderId(host.getIdentifier());
        } else {
            builder.withHostOrFolderId(folder.getInode());
        }
        builder.withFileName(assetName);
        final List<Treeable> folderContent = browserAPI.getFolderContentList(builder.build());
        final List<Contentlet> assets = folderContent.stream().filter(Contentlet.class::isInstance).map(
                Contentlet.class::cast).collect(Collectors.toList());
        if (assets.isEmpty()) {
            throw new NotFoundInDbException(
                    String.format(" Asset [%s] not found for lang [%s] and working/live state [%b] ",
                            assetName, form.language(),
                            BooleanUtils.toString(form.live(), "live", "working", "unspecified"))
            );
        }
        final Contentlet asset = assets.get(0);
        return fileAssetAPI.fromContentlet(asset);
    }


    /**
     * Saves or updates an asset given the asset path lang and version that will be used to locate it
     * @param request
     * @param form
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    public WebAssetView saveUpdateAsset(final HttpServletRequest request, final FileUploadData form,
            final User user) throws DotDataException, DotSecurityException, IOException {

        final FormDataContentDisposition contentDisposition = form.getContentDisposition();
        final String fileName = contentDisposition.getFileName();
        final InputStream fileInputStream = form.getFileInputStream();
        final FileUploadDetail detail = form.getDetail();
        final String assetPath = detail.getAssetPath();
        final boolean live = detail.getLive();
        final Optional<Language> lang = parseLang(detail.getLanguage(), true);

        if(lang.isEmpty()){
            throw new IllegalArgumentException("Unable to determine what language for asset.");
        }

        final ResolvedAssetAndPath assetAndPath = AssetPathResolver.newInstance()
                .resolve(assetPath, user, true);

        final Host host = assetAndPath.resolvedHost();
        final Folder folder = assetAndPath.resolvedFolder();
        final String assetName = null != assetAndPath.asset() ? assetAndPath.asset() : fileName;

        if (null == fileInputStream) {
            return toAssetsFolder(folder);
        }

        final DotTempFile tempFile = tempFileAPI.createTempFile(assetName, request,
                fileInputStream);
        try {

            final Builder builder = BrowserQuery.builder();
            builder.showDotAssets(false)
                    .withUser(user)
                    .showFiles(true)
                    .showFolders(true)
                    .showArchived(true) // yes we need to take into account archived assets here
                    .showWorking(!live)
                    .showLinks(false)
                    .showDotAssets(false)
                    .showImages(true)
                    .showContent(true)
                    .withLanguageId(lang.get().getId())
                    .sortBy(SORT_BY)
                    .sortByDesc(true)
            ;

            if (folder.isSystemFolder()) {
                builder.withHostOrFolderId(host.getIdentifier());
            } else {
                builder.withHostOrFolderId(folder.getInode());
            }

            builder.withFileName(assetName);
            final List<Treeable> folderContent = browserAPI.getFolderContentList(builder.build());
            final List<Contentlet> assets = folderContent.stream()
                    .filter(Contentlet.class::isInstance).map(Contentlet.class::cast)
                    .collect(Collectors.toList());

            Contentlet savedAsset = null;

            if (assets.isEmpty()) {
                //The file does not exist
                final Contentlet contentlet = makeFileAsset(tempFile.file, folder, lang.get());
                savedAsset = checkinOrPublish(contentlet, user, live);

            } else {

                final Optional<Contentlet> found = assets.stream()
                        .filter(contentlet -> lang.get().getId() == contentlet.getLanguageId())
                        .findFirst();

                if (found.isEmpty()) {
                    //We're required to create a new version in a different language
                    final Contentlet contentlet = makeFileAsset(tempFile.file, folder, lang.get());
                    savedAsset = checkinOrPublish(contentlet, user, live);

                } else {
                    final Contentlet asset = found.get();

                    if(asset.isArchived()){
                        contentletAPI.unarchive(asset, user, false);
                    }

                    final Contentlet checkout = contentletAPI.checkout(asset.getInode(), user, false);

                    updateFileAsset(tempFile.file, folder, lang.get(), checkout);
                    savedAsset = checkinOrPublish(checkout, user, live);
                }
            }

            final FileAsset fileAsset = fileAssetAPI.fromContentlet(savedAsset);
            return toAsset(fileAsset);
        } finally {
            disposeTempFile(tempFile);
        }
    }

    /**
     * Deletes an asset given the asset path that will be used to locate it
     * @param tempFile
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    void disposeTempFile(final DotTempFile tempFile){
        final File file = tempFile.file;
        try {
            Path parentFolder = file.getParentFile().toPath();
            Files.delete(parentFolder);
        } catch (IOException e) {
           Logger.debug(this, e.getMessage(), e);
        }
    }

    /**
     * checkin or publish the given contentlet
     * @param checkout
     * @param user
     * @param live
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Contentlet checkinOrPublish(final Contentlet checkout, User user, final boolean live) throws DotDataException, DotSecurityException {
        if(live){
            contentletAPI.publish(checkout, user, false);
            return checkout;
        } else {
            if(checkout.isLive()){
                contentletAPI.unpublish(checkout, user, false);
            }
        }
        return contentletAPI.checkin(checkout, user, false);
    }

    /**
     * Creates a new file asset with the given file, folder and language
     * So it can be saved within dotCMS
     * @param file
     * @param folder
     * @param lang
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Contentlet makeFileAsset(final File file, final Folder folder, Language lang)
            throws DotDataException, DotSecurityException {
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(contentTypeAPI.find("FileAsset").id());
        return updateFileAsset(file, folder, lang, contentlet);
    }


    /**
     * Updates a file asset with the given file, folder and language
     * @param file
     * @param folder
     * @param lang
     * @param contentlet
     * @return
     */
    Contentlet updateFileAsset(final File file, final Folder folder, final Language lang, final Contentlet contentlet){
        final String name = file.getName();
        contentlet.setProperty(FileAssetAPI.TITLE_FIELD, name);
        contentlet.setProperty(FileAssetAPI.FILE_NAME_FIELD, name);
        contentlet.setProperty(FileAssetAPI.BINARY_FIELD, file);
        contentlet.setFolder(folder.getInode());
        contentlet.setLanguageId(lang.getId());
        return contentlet;
    }

    /**
     * archive an asset
     * @param assetPath
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public void archiveAsset(final String assetPath, final User user)
            throws DotDataException, DotSecurityException {
        final WebAssetView assetInfo = getAssetInfo(assetPath, user);
        if(assetInfo instanceof AssetVersionsView){
            final AssetVersionsView fileAssetView = (AssetVersionsView) assetInfo;
             final String identifier = fileAssetView.versions().get(0).identifier();
             final Contentlet fileAsset = contentletAPI.findContentletByIdentifierAnyLanguage(identifier) ;
            if(!fileAsset.isArchived()){
                contentletAPI.archive(fileAsset, user, false);
            }
        } else {
            throw new IllegalArgumentException(String.format("The path [%s] can not be resolved as an asset", assetPath));
        }
    }

    /**
     * Delete an asset
     * @param assetPath
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public void deleteAsset(final String assetPath, final User user)
            throws DotDataException, DotSecurityException {
        final WebAssetView assetInfo = getAssetInfo(assetPath, user);
        if(assetInfo instanceof AssetVersionsView){
            final AssetVersionsView fileAssetView = (AssetVersionsView) assetInfo;
            final String identifier = fileAssetView.versions().get(0).identifier();
            final Contentlet fileAsset = contentletAPI.findContentletByIdentifierAnyLanguage(identifier) ;
            if(!fileAsset.isArchived()){
                contentletAPI.archive(fileAsset, user, false);
            }
            contentletAPI.destroy(fileAsset, user, false);
        } else {
            throw new IllegalArgumentException(String.format("The path [%s] can not be resolved as an asset", assetPath));
        }
    }

    public void deleteFolder(final String path, final User user)
            throws DotDataException, DotSecurityException {
        final WebAssetView assetInfo = getAssetInfo(path, user);
        if(assetInfo instanceof FolderView){
            final FolderView folderView = (FolderView) assetInfo;
            final Folder folder = folderAPI.find(folderView.inode(), user, false);
            folderAPI.delete(folder, user, false);
        } else {
            throw new IllegalArgumentException(String.format("The path [%s] can not be resolved as a folder", path));
        }
    }

    /**
     * Parses the language param and returns the respective Language object
     * @param language the language param
     * @param defaultLangFallback if true, it will return the default language if the language param is not found
     * @return
     */
    Optional<Language> parseLang(final String language, final boolean defaultLangFallback) {
        Language resolvedLang = Try.of(() -> {
                    //Typically locales are separated by a dash, but our Language API uses an underscore in the toString method
                    //So here I'm preparing for both cases
                    final Optional<String> splitBy = splitBy(language);
                    if (splitBy.isEmpty()) {
                        return languageAPI.getLanguage(language,null);
                    }
                    final String[] split = language.split(splitBy.get(), 2);
                    return languageAPI.getLanguage(split[0], split[1]);
                }
        ).getOrNull();
        if (defaultLangFallback  && (null == resolvedLang || resolvedLang.getId() == 0)) {
            resolvedLang = languageAPI.getDefaultLanguage();
            Logger.warn(this,
                    String.format("Unable to get language from param [%s]. Defaulting to [%s].",
                            language, resolvedLang));
        }
        return Optional.ofNullable(resolvedLang);
    }

    /**
     * Splits the language by either a dash or underscore
     * @param language
     * @return
     */
    Optional<String> splitBy(final String language){
        if(language.contains("-")){
          return Optional.of("-");
        }
        if(language.contains("_")){
            return Optional.of("_");
        }
        return Optional.empty();
    }

    /**
     * Creates a new instance of {@link WebAssetHelper}
     * @return {@link WebAssetHelper}
     */
    public static WebAssetHelper newInstance(){
        return new WebAssetHelper();
    }

}
