package com.dotcms.publishing.manifest;

import static com.dotcms.util.CollectionsUtils.list;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfoBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.FileUtil;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jersey.repackaged.com.google.common.collect.ImmutableSet;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Util class to read a CSV Manifest file
 */
public class CSVManifestReader implements ManifestReader{

    private Collection<CSVManifestItem> manifestItemsIncluded;
    private Collection<CSVManifestItem> manifestItemsExcluded;
    private Map<String, String> metaData;

    final static String COMMENTED_LINE_START = "#";

    public CSVManifestReader(final Reader manifestReader){
        init(manifestReader);
    }

    public CSVManifestReader(final File csvManifestFile){
        init(csvManifestFile);
    }

    private void init(final File csvManifestFile){
        try{
            init(new FileReader(csvManifestFile));
        } catch (IOException e) {
            throw new IllegalArgumentException("Not valid " + csvManifestFile.getAbsolutePath(), e);
        }
    }

    private void init(final Reader manifestReader){
        try {
            final List<String> lines = IOUtils.readLines(manifestReader);

            this.manifestItemsIncluded = getManifestInfos(lines, "INCLUDED");
            this.manifestItemsExcluded = getManifestInfos(lines, "EXCLUDED");
            this.metaData = getAllMetaData(lines);
        } catch (IOException e) {
            throw new IllegalArgumentException("Not valid InputStream manifest: " + e.getMessage(), e);
        }
    }

    private Map<String, String> getAllMetaData(final List<String> lines) {
        return lines.stream()
                .filter(line -> line.startsWith(COMMENTED_LINE_START))
                .map(line -> line.split(StringPool.COLON))
                .map(lineSplitted -> new String[]{lineSplitted[0].substring(1), lineSplitted[1]})
                .collect(Collectors.toMap(lineSplitted -> lineSplitted[0], lineSplitted -> lineSplitted[1]));
    }

    private Collection<CSVManifestItem> getManifestInfos(final List<String> lines, final String filterBy) {
        return lines.stream()
                .filter(line -> !line.startsWith(COMMENTED_LINE_START))
                .filter(line -> !line.equals(CSVManifestBuilder.HEADERS_LINE))
                .filter(line -> line.startsWith(filterBy))
                .map(CSVManifestItem::new)
                .collect(toSet());
    }

    @Override
    public Collection<ManifestInfo> getIncludedAssets() {
        return manifestItemsIncluded.stream()
                .map(CSVManifestItem::getManifestInfo)
                .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
    }

    @Override
    public Collection<ManifestInfo> getExcludedAssets() {
        return manifestItemsExcluded.stream()
                .map(CSVManifestItem::getManifestInfo)
                .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
    }

    @Override
    public Collection<ManifestInfo> getAssets(final ManifestReason manifestReason) {
        return Stream.concat(manifestItemsIncluded.stream(), manifestItemsExcluded.stream())
                .filter(csvManifestItem -> manifestReason.getMessage().equals(csvManifestItem.getReason()))
                .map(CSVManifestItem::getManifestInfo)
                .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
    }

    @Override
    public String getMetadata(final String name){
        return metaData.get(name);
    }

    @Override
    public Collection<ManifestInfo> getAssets() {
        final Set<ManifestInfo> set = Stream.concat(manifestItemsIncluded.stream(), manifestItemsExcluded.stream())
                .map(CSVManifestItem::getManifestInfo)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(set);
    }

    private static class CSVManifestItem {
        final ManifestInfo manifestInfo;
         String reason;

        public CSVManifestItem(final String line) {
            final String[] lineSplit = line.split(StringPool.COMMA);
            this.manifestInfo = new ManifestInfoBuilder()
                    .objectType(lineSplit[1])
                    .id(lineSplit[2])
                    .inode(lineSplit[3])
                    .title(lineSplit[4])
                    .siteId(lineSplit[5])
                    .path(lineSplit[6])
                    .build();

            reason = lineSplit[0].equals("INCLUDED") ? lineSplit[8] : lineSplit[7];
        }

        public ManifestInfo getManifestInfo() {
            return manifestInfo;
        }

        public String getReason() {
            return reason;
        }
    }
}
