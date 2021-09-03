package com.dotcms.publishing.manifest;

import static com.dotcms.util.CollectionsUtils.list;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfoBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.FileUtil;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jersey.repackaged.com.google.common.collect.ImmutableSet;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

public class CSVManifestReader implements ManifestReader{

    final Collection<CSVManifestItem> manifestItemsIncluded;
    final Collection<CSVManifestItem> manifestItemsExcluded;
    //"INCLUDED/EXCLUDED,object type, Id, inode, title, site, folder, excluded by, included by";
    public CSVManifestReader(final File csvManifestFile){
        try {
            final List<String> lines = FileUtils.readLines(csvManifestFile, StandardCharsets.UTF_8);
            lines.remove(0);

            this.manifestItemsIncluded = getManifestInfos(lines, "INCLUDED");
            this.manifestItemsExcluded = getManifestInfos(lines, "EXCLUDED");
        } catch (IOException e) {
            throw new IllegalArgumentException("Not valid " + csvManifestFile.getAbsolutePath(), e);
        }
    }

    private Collection<CSVManifestItem> getManifestInfos(final List<String> lines, final String filterBy) {
        return lines.stream()
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
    public Collection<ManifestInfo> getAssets() {
        final Set set = new HashSet();
        set.addAll(manifestItemsIncluded);
        set.addAll(manifestItemsExcluded);
        return ImmutableSet.copyOf(set);
    }

    private static class CSVManifestItem {
        final ManifestInfo manifestInfo;
        final String reason;

        public CSVManifestItem(final String line) {
            final String[] lineSplit = line.split(",");
            this.manifestInfo = new ManifestInfoBuilder()
                    .objectType(lineSplit[1])
                    .id(lineSplit[2])
                    .inode(lineSplit[3])
                    .title(lineSplit[4])
                    .siteId(lineSplit[5])
                    .path(lineSplit[6])
                    .build();

            reason = lineSplit[0].equals("INCLUDED") ? lineSplit[7] : lineSplit[8];
        }

        public ManifestInfo getManifestInfo() {
            return manifestInfo;
        }

        public String getReason() {
            return reason;
        }
    }
}
