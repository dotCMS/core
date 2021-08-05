package com.dotcms.publishing.manifest;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.FileUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CSV Manifest file builder, creae a manifest file with the headers:
 *
 * - INCLUDE/EXCLUDE: if the asset was INCLUDE or EXCLUDE
 * - object type: Asset's {@link PusheableAsset}
 * - Id: Asset's id
 * - title: Asset's Title
 * - site: Asset's Site
 * - folder: Asset's Folder
 * - exclude by: reason why the asset was EXCLUDE, if the asset was INCLUDE then it is blank
 * - include by: reason why the asset was INCLUDE, if the asset was EXCLUDE then it is blank
 */
public class CSVManifestBuilder implements ManifestBuilder {
    private final static String HEADERS_LINE =
            "INCLUDED/EXCLUDED,object type, Id, title, site, folder, excluded by, included by";
    private FileWriter csvWriter;

    private File manifestFile;

    public void create() {
        try {
            manifestFile = File.createTempFile("ManifestBuilder_", ".csv");

            csvWriter = new FileWriter(manifestFile);
            writeLine(HEADERS_LINE);
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void writeLine(String headersLine) throws IOException {
        csvWriter.append(headersLine);
        csvWriter.append("\n");
    }

    public <T> void include(final ManifestItem manifestItem, final String reason){
        final ManifestInfo manifestInfo = manifestItem.getManifestInfo();
        final String line = getManifestFileIncludeLine(manifestInfo, reason);

        try {
            writeLine(line);
        } catch (IOException e) {
            throw new DotRuntimeException(
                    String.format("Error writing in the Manifest file: %s, error %s",
                            manifestFile.getAbsolutePath(), e.getMessage()), e);
        }
    }

    private String getManifestFileIncludeLine(final ManifestInfo manifestInfo,
            final String includeReason) {
        return getManifestFileLine("INCLUDED", manifestInfo, includeReason, StringPool.BLANK);
    }

    private String getManifestFileExcludeLine(final ManifestInfo manifestInfo,
            final String excludeReason) {
        return getManifestFileLine("EXCLUDED", manifestInfo, StringPool.BLANK, excludeReason);
    }

    private String getManifestFileLine(
            final String includeExclude, final ManifestInfo manifestInfo,
            final String includeReason, final String excludeReason) {

        return list(
                includeExclude,
                manifestInfo.objectType(),
                manifestInfo.id(),
                manifestInfo.title(),
                manifestInfo.site(),
                manifestInfo.folder(),
                excludeReason,
                includeReason).stream().collect(Collectors.joining(","));
    }

    public <T> void exclude(final ManifestItem manifestItem, final String reason){
        final ManifestInfo manifestInfo = manifestItem.getManifestInfo();
        final String line = getManifestFileExcludeLine(manifestInfo, reason);

        try {
            writeLine(line);
        } catch (IOException e) {
            throw new DotRuntimeException(
                    String.format("Error writing in the Manifest file: %s, error %s",
                            manifestFile.getAbsolutePath(), e.getMessage()), e);
        }
    }

    public File getManifestFile(){

        if (manifestFile == null) {
            throw new IllegalStateException("Must call create method before");
        }

        return manifestFile;
    }

    @Override
    public void close() {
        if (csvWriter != null) {
            CloseUtils.closeQuietly(csvWriter);
        }
    }

}
