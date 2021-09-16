package com.dotcms.publishing.manifest;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
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

    public final static String BUNDLE_ID_METADATA_NAME = "Bundle ID";
    public final static String OPERATION_METADATA_NAME = "Operation";
    public final static String FILTER_METADATA_NAME = "Filter";

    public final static String HEADERS_LINE =
            "INCLUDED/EXCLUDED,object type, Id, inode, title, site, folder, excluded by, included by";
    private FileWriter csvWriter;

    private File manifestFile;
    private Map<String, String> metaData;

    private synchronized void create() {
        try {
            manifestFile = File.createTempFile("ManifestBuilder_", ".csv");

            csvWriter = new FileWriter(manifestFile);

            if (UtilMethods.isSet(metaData)) {
                for (Entry<String, String> headersEntry : metaData.entrySet()) {
                    writeLine(String.format("#%s:%s", headersEntry.getKey(),
                            headersEntry.getValue()));
                }
            }

            writeLine(HEADERS_LINE);
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void writeLine(String headersLine) throws IOException {
        if (manifestFile == null) {
            create();
        }

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
                manifestInfo.inode(),
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
            throw new IllegalStateException("Should include any asset first");
        }

        return manifestFile;
    }

    @Override
    public void close() {
        if (csvWriter != null) {
            CloseUtils.closeQuietly(csvWriter);
        }
    }

    /**
     * Add a comment in the first lines of the Manifest file, for exaple the follow code:
     *
     * <code>
     *     csvManifestBuilder.addHeader("first_header", "This is the first header")
     *     csvManifestBuilder.addHeader("second_header", "This is the second header")
     * </code>
     *
     * it going to produce the follow lines into the manifest file
     *
     * <pre>
     * #first_header:This is the first header
     * #second_header:This is the second header
     *
     * INCLUDED/EXCLUDED,object type, Id, inode, title, site, folder, excluded by, included by
     * ...
     * </pre>
     * @param name
     * @param value
     */
    @Override
    public void addMetadata(final String name, final String value){

        if (UtilMethods.isSet(manifestFile)) {
            throw new IllegalStateException("It not possible add header after call include or exclude methods");
        }

        if (!UtilMethods.isSet(metaData)) {
            metaData = new LinkedHashMap<>();
        }

        this.metaData.put(name, value);
    }

}
