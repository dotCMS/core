package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class FileAssetDataGen extends ContentletDataGen {

    public FileAssetDataGen(final Host host, final File file) throws DotSecurityException, DotDataException {
        this(file);
        this.host = host;
    }

    public FileAssetDataGen(final Folder folder, final File file)
            throws DotSecurityException, DotDataException {
        this(file);
        this.folder = folder;
    }

    public FileAssetDataGen(final Folder folder, final String content)
            throws DotSecurityException, DotDataException, IOException {
        this(folder, getFile("FileAssetDataGen_" + System.currentTimeMillis(), "", content));
    }

    public FileAssetDataGen(final File file) throws DotDataException, DotSecurityException {

        super(APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("FileAsset").id());
        this.user = APILocator.systemUser();
        final String fileName = file.getName();
        setProperty(FileAssetAPI.TITLE_FIELD, fileName);
        setProperty(FileAssetAPI.FILE_NAME_FIELD, fileName);
        setProperty(FileAssetAPI.BINARY_FIELD, file);
    }

    public FileAssetDataGen title(final String title) {
        setProperty(FileAssetAPI.TITLE_FIELD, title);
        return this;
    }

    public static Contentlet createFileAsset(
            final Folder folder,
            final String fileName,
            final String suffix)
            throws IOException, DotSecurityException, DotDataException {

        return  createFileAsset(folder, fileName, suffix, "helloworld");
    }

    private static Contentlet createFileAsset(
            final Folder folder,
            final String fileName,
            final String suffix,
            final String content)
            throws IOException, DotSecurityException, DotDataException {
        final File file = getFile(fileName, suffix, content);

        return  new FileAssetDataGen(folder, file).nextPersisted();
    }

    public static FileAssetDataGen createFileAssetDataGen(
            final Folder folder,
            final String fileName,
            final String suffix,
            final String content)
            throws IOException, DotSecurityException, DotDataException {
        final File file = getFile(fileName, suffix, content);

        return  new FileAssetDataGen(folder, file);
    }

    @NotNull
    private static File getFile(String fileName, String suffix, String content) throws IOException {
        final File file = File.createTempFile(fileName, suffix);
        FileUtil.write(file, content);
        return file;
    }

    public static FileAssetDataGen createImageFileAssetDataGen(final File imageFile)
            throws IOException, DotDataException, DotSecurityException {

        final File tempFile = File.createTempFile("getPageWithImage", ".jpg");
        FileUtils.copyFile(imageFile, tempFile);

        return new FileAssetDataGen(tempFile);
    }
}