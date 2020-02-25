package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.FileUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ContainerAsFileDataGen extends AbstractDataGen<FileAssetContainer> {

    private class ContentTypeContent {
        private ContentType contentType;
        private String content;

        public ContentTypeContent(ContentType contentType, String content) {
            this.contentType = contentType;
            this.content = content;
        }

        public ContentType getContentType() {
            return contentType;
        }

        public String getContent() {
            return content;
        }
    }

    private Host host;
    private String folderName = "/large-column" + System.currentTimeMillis();
    private List<ContentTypeContent> contentTypes = new ArrayList<>();

    private String metaDataCode =
            "$dotJSON.put(\"title\", \"Test Container\")\n" +
                    "$dotJSON.put(\"max_contentlets\", 25)\n" +
                    "$dotJSON.put(\"notes\", \"Medium Column:Blog,Events,Generic,Location,Media,News,Documents,Products\")\n";

    private FileAsset metaData;
    private List<FileAsset> structures = new ArrayList<>();

    public ContainerAsFileDataGen() throws DotSecurityException, DotDataException {
        host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
    }

    @Override
    public FileAssetContainer next() {
        return new FileAssetContainer();
    }

    public ContainerAsFileDataGen host(final Host host) {
        this.host = host;
        return this;
    }

    public ContainerAsFileDataGen folderName(final String folderName) {
        this.folderName = folderName;
        return this;
    }

    public ContainerAsFileDataGen contentType(final ContentType contentType, final String content) {
        this.contentTypes.add(new ContentTypeContent(contentType,  content));
        return this;
    }

    public ContainerAsFileDataGen metadataCode(final String metaDataCode) {
        this.metaDataCode = metaDataCode;
        return this;
    }

    public List<ContentType> getContentTypes() {
        return contentTypes.stream()
                .map(contentTypeContent -> contentTypeContent.getContentType())
                .collect(Collectors.toList());
    }

    public FileAsset getMetaData() {
        return metaData;
    }

    public Set<Contentlet> getStructures() {
        return ImmutableSet.copyOf(structures);
    }

    @WrapInTransaction
    @Override
    public FileAssetContainer persist(FileAssetContainer object) {
        structures.clear();
        try {
            final Folder containerFolder = createFileAsContainerFolderIfNeeded();

            if (contentTypes.isEmpty()) {
                contentTypes = getDefaultContentTypes();
            }

            for (final ContentTypeContent contentTypeContent : contentTypes) {
                final ContentType contentType = contentTypeContent.getContentType();
                final String contentTypeName = contentType.variable();
                final java.io.File file = java.io.File.createTempFile(contentTypeName, ".vtl");
                FileUtil.write(file, contentTypeContent.getContent());
                final Contentlet structure = new FileAssetDataGen(containerFolder, file)
                        .host(host)
                        .setProperty("title", contentTypeName)
                        .setProperty("fileName", contentTypeName + ".vtl")
                        .nextPersisted();
                structures.add(APILocator.getFileAssetAPI().fromContentlet(structure));
            }

            final java.io.File file = java.io.File.createTempFile("container", ".vtl");
            FileUtil.write(file, metaDataCode);

            final Contentlet container = new FileAssetDataGen(containerFolder, file)
                    .host(host)
                    .setProperty("title", Constants.CONTAINER_META_INFO_FILE_NAME)
                    .setProperty("fileName", Constants.CONTAINER_META_INFO_FILE_NAME).nextPersisted();

            metaData = APILocator.getFileAssetAPI().fromContentlet(container);
            return toFileAssetContainer(object, containerFolder, metaData, structures);
        } catch (DotDataException | DotSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileAssetContainer toFileAssetContainer(final FileAssetContainer container,
            final Folder containerFolder, final FileAsset metaInfoFileAsset, final List<FileAsset> structures) {
        container.setIdentifier(metaInfoFileAsset.getIdentifier());
        container.setInode(metaInfoFileAsset.getInode());
        container.setOwner(metaInfoFileAsset.getOwner());
        container.setIDate(metaInfoFileAsset.getIDate());
        container.setModDate(metaInfoFileAsset.getModDate());
        container.setModUser(metaInfoFileAsset.getModUser());
        container.setShowOnMenu(metaInfoFileAsset.isShowOnMenu());
        container.setSortOrder(containerFolder.getSortOrder());
        container.setTitle(containerFolder.getTitle());
        container.setMaxContentlets(10);
        container.setLanguage(metaInfoFileAsset.getLanguageId());
        container.setFriendlyName(
                (String) metaInfoFileAsset.getMap().getOrDefault("", container.getTitle()));
        //container.setPath(this.buildPath(host, containerFolder, includeHostOnPath));
        //preLoop.ifPresent (value -> container.setPreLoop (value));
        //postLoop.ifPresent(value -> container.setPostLoop(value));
        //this.setMetaInfo (containerMetaInfo, container);
        container.setContainerStructuresAssets(structures);
        return container;
    }

    private boolean checkFileAsContainerPathExist() throws DotSecurityException, DotDataException {
        final Folder folder = APILocator.getFolderAPI()
                .findFolderByPath(Constants.CONTAINER_FOLDER_PATH, host, APILocator.systemUser(),
                        false);
        return (null != folder && UtilMethods.isSet(folder.getIdentifier()));
    }

    private void creatApplicationContainerFolderIfNeeded()
            throws DotDataException, DotSecurityException {
        if (!checkFileAsContainerPathExist()) {
            APILocator.getFolderAPI()
                    .createFolders(Constants.CONTAINER_FOLDER_PATH, host, APILocator.systemUser(),
                            false);
        }
    }

    private synchronized Folder createFileAsContainerFolderIfNeeded()
            throws DotDataException, DotSecurityException {
        creatApplicationContainerFolderIfNeeded();
        String fullPath = Constants.CONTAINER_FOLDER_PATH + "/" + folderName;
        fullPath = !fullPath.endsWith("/") ? fullPath + "/" : fullPath;

        Folder folder = APILocator.getFolderAPI()
                .findFolderByPath(fullPath, host, APILocator.systemUser(), false);
        if (null == folder || !UtilMethods.isSet(folder.getIdentifier())) {
            folder = APILocator.getFolderAPI()
                    .createFolders(fullPath, host, APILocator.systemUser(), false);
        }
        return folder;
    }

    private List<ContentTypeContent> getDefaultContentTypes(){
        return new ArrayList<>(ImmutableSet.of(
                new ContentTypeContent(TestDataUtils.getDocumentLikeContentType(), "lol"),
                new ContentTypeContent(TestDataUtils.getProductLikeContentType(), "lol"),
                new ContentTypeContent(TestDataUtils.getNewsLikeContentType(), "lol")
        ));
    }
}
