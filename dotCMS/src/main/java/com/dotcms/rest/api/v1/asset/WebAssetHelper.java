package com.dotcms.rest.api.v1.asset;

import static com.dotmarketing.util.UtilMethods.isNotSet;

import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.browser.BrowserQuery.Builder;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.api.v1.asset.view.AssetVersionsView;
import com.dotcms.rest.api.v1.asset.view.AssetView;
import com.dotcms.rest.api.v1.asset.view.FolderView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.BooleanUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

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

    PermissionAPI permissionAPI;

    /**
     * Constructor
     * @param params
     */
    WebAssetHelper(final WebAssetHelperParams params){
        this.languageAPI = params.languageAPI();
        this.fileAssetAPI = params.fileAssetAPI();
        this.contentletAPI = params.contentletAPI();
        this.browserAPI = params.browserAPI();
        this.tempFileAPI = params.tempFileAPI();
        this.contentTypeAPI = params.contentTypeAPI();
        this.folderAPI = params.folderAPI();
        this.permissionAPI = params.permissionAPI();
    }

    /**
     * Default constructor
     */
    WebAssetHelper() {
        this(WebAssetHelperParams.builder()
            .browserAPI(APILocator.getBrowserAPI())
            .contentletAPI(APILocator.getContentletAPI())
            .fileAssetAPI(APILocator.getFileAssetAPI())
            .languageAPI(APILocator.getLanguageAPI())
            .tempFileAPI(APILocator.getTempFileAPI())
            .contentTypeAPI(APILocator.getContentTypeAPI(APILocator.systemUser()))
            .folderAPI(APILocator.getFolderAPI())
            .permissionAPI(APILocator.getPermissionAPI())
            .build()
        );
    }

    /**
     * Entry point here it is determined if the path is a folder or an asset.
     * If it is a folder it will return a FolderView, if it is an asset it will return an AssetView
     * @param path the path to the asset
     * @param user current user
     * @return the asset view
     * @throws DotDataException any data related exception
     * @throws DotSecurityException any security violation exception
     */
    public WebAssetView getAssetInfo(final String path, final User user)
            throws DotDataException, DotSecurityException {
        final ResolvedAssetAndPath assetAndPath = AssetPathResolver.newInstance()
                .resolve(path, user);
        return getAssetInfo(assetAndPath,false, user);
    }

    /**
     * Entry point here it is determined if the path is a folder or an asset.
     * If it is a folder it will return a FolderView, if it is an asset it will return an AssetView
     * @param assetAndPath resolved asset and path
     * @param user current user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    WebAssetView getAssetInfo(final ResolvedAssetAndPath assetAndPath, final boolean showArchived, final User user)
            throws DotDataException, DotSecurityException {

        final Host host = assetAndPath.resolvedHost();
        final Folder folder = assetAndPath.resolvedFolder();
        final String assetName = assetAndPath.asset();

        final List<Contentlet> assets;

        final Builder builder = BrowserQuery.builder();
        builder.showDotAssets(false)
                .withUser(user)
                .showFiles(true)
                .showFolders(true)
                .showArchived(showArchived)
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

            final List<Contentlet> folderContent = sortByIdentifier(
                    collectAllVersions(builder)
            );
            assets = folderContent.stream().filter(Objects::nonNull).map(
                    Contentlet.class::cast).collect(Collectors.toList());
            if (assets.isEmpty()) {
                throw new NotFoundInDbException(String.format(" Asset [%s] not found", assetName));
            }
            final Contentlet asset = assets.get(0);
            return AssetVersionsView.builder()
                    .identifier(asset.getIdentifier())
                    .archived(asset.isArchived())
                    .versions(toAssets(assets))
                    .build();
        } else {
            Logger.debug(this, String.format("Retrieving a folder by name: [%s] " , folder.getName()));
            final List<Treeable> folderContent = browserAPI.getFolderContentList(builder.build(), false);
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

           AssetVersionsView versionsView = null;
           if(!assets.isEmpty()){
               //We're looking at a non-empty folder
               final Contentlet asset = assets.get(0);
               versionsView =
                       AssetVersionsView.builder()
                               .identifier(asset.getIdentifier())
                               .archived(asset.isArchived())
                               .versions(toAssets(assets)).build();
           }

            return FolderView.builder()
                    .sortOrder(folder.getSortOrder())
                    .filesMasks(folder.getFilesMasks())
                    .defaultFileType(folder.getDefaultFileType())
                    .host(folder.getHost().getHostname())
                    .path(folder.getPath())
                    .name(folder.getName())
                    .modDate(folder.getModDate().toInstant())
                    .identifier(folder.getIdentifier())
                    .inode(folder.getInode())
                    .subFolders(toAssetFolders(subFolders))
                    .assets(versionsView)
                    .build();
        }
    }

    /**
     * Exclude contentlets that are not live or working and sort them by identifier
     * This to improve presentation of the assets in our API
     * @param contentlets
     * @return
     */
    List<Contentlet> sortByIdentifier(Collection<Contentlet> contentlets) {
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
     * This Predicate is used to deal with a very rare situation on which a file might not have binary metadata associated
     * for example hidden files. In the rare event of a hidden file being uploaded to the system, we need to make sure they don't break the API response
     */
    Predicate<Contentlet> nonNullMetadata = c -> {
        try {
            return null != c.getBinaryMetadata(FileAssetAPI.BINARY_FIELD);
        } catch (DotDataException e) {
            return false;
        }
    };

    /**
     * Converts a list of folders to a list of {@link FolderView}
     * @param assets folders to convert
     * @return list of {@link FolderView}
     */
    Iterable<AssetView> toAssets(final Collection<? extends Treeable> assets) {

        return assets.stream().filter(Contentlet.class::isInstance).map(Contentlet.class::cast)
                .filter(Contentlet::isFileAsset)
                .filter(nonNullMetadata)
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
                .host(folder.getHost().getHostname())
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
    @CloseDBIfOpened
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
            final String status = BooleanUtils.toString(form.live(), "live", "working", "unspecified");
            throw new NotFoundInDbException(
                    " Asset [" + assetName + "] not found for lang ["+form.language()+"] and live status ["+ status +"]"
            );
        }
        final Contentlet asset = assets.get(0);
        return fileAssetAPI.fromContentlet(asset);
    }


    /**
     * Saves or updates an asset given the asset path lang and version that will be used to locate it
     * @param request the request
     * @param form the form data
     * @param user current user
     * @return the asset view
     * @throws DotDataException any data related exception
     * @throws DotSecurityException any security violation exception
     * @throws IOException any IO exception
     */
    @WrapInTransaction
    public WebAssetView saveUpdateAsset(final HttpServletRequest request, final FileUploadData form,
            final User user) throws DotDataException, DotSecurityException, IOException {

        final FormDataContentDisposition contentDisposition = form.getContentDisposition();
        final String fileName = contentDisposition.getFileName();
        final InputStream fileInputStream = form.getFileInputStream();
        final FileUploadDetail detail = form.getDetail();
        final String assetPath = detail.getAssetPath();
        final boolean live = detail.isLive();
        final Optional<Language> lang = parseLang(detail.getLanguage(), true);

        if (lang.isEmpty()) {
            throw new IllegalArgumentException("Unable to determine language for asset.");
        }

        final ResolvedAssetAndPath assetAndPath = AssetPathResolver.newInstance()
                .resolve(assetPath, user, true);

        final Host host = assetAndPath.resolvedHost();
        final Folder folder = assetAndPath.resolvedFolder();
        final String assetName = null != assetAndPath.asset() ? assetAndPath.asset() : fileName;

        if (null == fileInputStream) {
            //if no FileInputStream  is present we return the folder info
            //because we support creating folders by just specifying the path
            //At least we have to have read permissions to the folder
            //if we fail to verify read permissions we need to throw an exception therefore the exception will roll back the folder creation
            checkFolderReadPermissions(user, folder);
            return toAssetsFolder(folder);
        }

        final DotTempFile tempFile = tempFileAPI.createTempFile(assetName, request,
                fileInputStream);
        try {
            final WebAssetView assetView = Try.of(() -> getAssetInfo(assetAndPath, true, user)).getOrNull();
            Contentlet savedAsset = null;

            if (assetView instanceof AssetVersionsView) {

                final AssetVersionsView versionsView = (AssetVersionsView) assetView;
                //so we have an asset version. We need to update it or create a new version of it probably in a different language

                final List<AssetView> versions = versionsView.versions();
                // We trust that the asset we're getting here is working and or live which is the inode we need to do the checkout
                final AssetView asset = versions.get(0);

                checkFolderPublishPermissions(user, folder);
                handleArchivedVersions(user, asset, lang.get());
                //now checkout and create a new version of the asset in the given language
                final Contentlet checkout = contentletAPI.checkout(asset.inode(), user, false);
                updateFileAsset(tempFile.file, host, folder, lang.get(), checkout);
                savedAsset = checkinOrPublish(checkout, user, live);

            } else {
                checkFolderPublishPermissions(user, folder);
                //asset does not exist. So create new one
                final Contentlet contentlet = makeFileAsset(tempFile.file, host, folder, user, lang.get());
                savedAsset = checkinOrPublish(contentlet, user, live);
            }
            final FileAsset fileAsset = fileAssetAPI.fromContentlet(savedAsset);
            return toAsset(fileAsset);
        } finally {
            disposeTempFile(tempFile);
        }
    }

    /**
     * Checks if the user has read permissions for the given folder
     * @param user the user performing the action
     * @param folder the folder to check
     * @throws DotDataException any data related exception
     * @throws DotSecurityException any security violation exception
     */
    private void checkFolderReadPermissions(User user, Folder folder) throws DotDataException, DotSecurityException {
        if(!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, false)){
            throw new DotSecurityException(String.format("User [%s] does not have permission to read folder [%s]",
                    user.getUserId(), folder.getInode()));
        }
    }

    /**
     * Checks if the user has write permissions for the given folder
     * @param user the user performing the action
     * @param folder the folder to check
     * @throws DotDataException any data related exception
     * @throws DotSecurityException any security violation exception
     */
    private void checkFolderPublishPermissions(User user, Folder folder) throws DotDataException, DotSecurityException {
        if(!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_PUBLISH, user, false)){
            throw new DotSecurityException(String.format("User [%s] does not have permission to write to folder [%s]",
                    user.getUserId(), folder.getInode()));
        }
    }

    /**
     * If we want to be successful publishing content that has been archived we need to get rid of any archived version
     * @param user the user performing the action
     * @param asset the asset to check
     * @param language the language to check
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private void handleArchivedVersions(User user, AssetView asset, Language language)
            throws DotSecurityException, DotDataException {
        //Always verify if any prior version remains archived for the give language, if so get rid of the problem
        contentletAPI.findAllVersions( new Identifier(asset.identifier()), user, false)
                .stream().filter(contentlet -> language.getId() == contentlet.getLanguageId())
                .forEach(contentlet -> handleArchivedContent(contentlet, user));
    }

    /**
     * Handles archived content
     * @param contentlet the contentlet to handle
     * @param user the user performing the action
     */
    private void handleArchivedContent(final Contentlet contentlet, final User user) {
        try {
            if (contentlet.isArchived()) {
                contentletAPI.unarchive(contentlet, user, false);
            }
        } catch (DotSecurityException | DotDataException e) {
            Logger.warn(WebAssetHelper.class, String.format(
                    "An error occurred handling archived piece of content with id [%s]. ",
                    contentlet.getIdentifier()), e);
        }
    }

    /**
     * Deletes an asset given the asset path that will be used to locate it
     * @param tempFile the temp file
     * @throws DotDataException any data related exception
     * @throws DotSecurityException any security violation exception
     * @throws IOException any IO exception
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
     * @param checkout the contentlet to check in or publish
     * @param user the user performing the action
     * @param live if true the contentlet will be published
     * @return the contentlet
     * @throws DotDataException any data related exception
     * @throws DotSecurityException any security violation exception
     */
    Contentlet checkinOrPublish(final Contentlet checkout, User user, final boolean live)
            throws DotDataException, DotSecurityException {

        var contentletToProcess = checkout;
        var checkedIn = false;

        if (isNotSet(checkout.getInode())) {
            contentletToProcess = contentletAPI.checkin(contentletToProcess, user, false);
            checkedIn = true;
        }

        if (live) {
            //Live means publish, so we need to publish the contentlet
            contentletAPI.publish(contentletToProcess, user, false);
        } else {

            if (checkout.isLive()) {
                //if the desired state is working we need to unpublish the contentlet
                contentletAPI.unpublish(contentletToProcess, user, false);
            } else if (!checkedIn) {
                return contentletAPI.checkin(contentletToProcess, user, false);
            }
        }

        return contentletToProcess;
    }

    /**
     * Creates a new file asset with the given file, folder and language
     * So it can be saved within dotCMS
     * @param file the file to save
     * @param folder the folder to save the file
     * @param lang the language to save the file
     * @return the contentlet
     * @throws DotDataException any data related exception
     * @throws DotSecurityException any security violation exception
     */
    Contentlet makeFileAsset(final File file, final Host host, final Folder folder, final User user, final Language lang)
            throws DotDataException, DotSecurityException {
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(contentTypeAPI.find("FileAsset").id());
        return updateFileAsset(file, host, folder, lang, contentlet);
    }


    /**
     * Updates a file asset with the given file, folder and language
     * @param file the file to update
     * @param folder the folder to update the file
     * @param lang the language to update the file
     * @param contentlet the contentlet to update
     * @return the contentlet
     */
    Contentlet updateFileAsset(final File file, final Host host, final Folder folder, final Language lang, final Contentlet contentlet){
        final String name = file.getName();
        contentlet.setProperty(FileAssetAPI.TITLE_FIELD, name);
        contentlet.setProperty(FileAssetAPI.FILE_NAME_FIELD, name);
        contentlet.setProperty(FileAssetAPI.BINARY_FIELD, file);
        if(!folder.isSystemFolder()){
            contentlet.setFolder(folder.getInode());
        } else {
            contentlet.setHost(host.getIdentifier());
        }
        contentlet.setLanguageId(lang.getId());
        return contentlet;
    }

    /**
     * archive an asset
     * @param assetPath the asset path
     * @param user current user
     * @throws DotDataException any data related exception
     * @throws DotSecurityException any security violation exception
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
     * @param assetPath the asset path
     * @param user current user
     * @throws DotDataException any data related exception
     * @throws DotSecurityException any security violation exception
     */
    @WrapInTransaction
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

    /**
     * Deletes a folder
     * @param path the folder path
     * @param user current user
     * @throws DotDataException any data related exception
     * @throws DotSecurityException any security violation exception
     */
    @WrapInTransaction
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
     * @param language the language to split
     * @return the split character
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
