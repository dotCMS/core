package com.dotcms.telemetry.collectors.site;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Collects the count of sites with thumbnails set
 */
public class CountOfSitesWithThumbnailsMetricType implements MetricType {

    private static final String ALL_SITES_INODES = "SELECT c.inode AS inode \n" +
            "FROM contentlet c JOIN structure s\n" +
            "ON c.structure_inode = s.inode\n" +
            "JOIN contentlet_version_info cvi\n" +
            "ON (c.inode = cvi.working_inode)\n" +
            "WHERE s.name = 'Host' AND c.identifier <> 'SYSTEM_HOST';\n";


    @Override
    public String getName() {
        return "SITES_WITH_THUMBNAIL_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of sites with Thumbnails";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.SITES;
    }

    @Override
    public Optional<Object> getValue() {
        return Optional.of(getCountOfSitesWithThumbnails());
    }

    private int getCountOfSitesWithThumbnails() {
        int hostsWithThumbnailsCount = 0;

        try {
            final List<String> allSitesInodes = getAllSitesInodes();

            for (String siteInode : allSitesInodes) {
                final File hostThumbnail = Try.of(() -> APILocator.getContentletAPI().getBinaryFile(siteInode,
                        Host.HOST_THUMB_KEY, APILocator.systemUser())).getOrNull();
                if (hostThumbnail != null) {
                    hostsWithThumbnailsCount++;
                }
            }

        } catch(Exception e) {
            Logger.debug(this, "Error counting Sites with thumbnails");
        }

        return hostsWithThumbnailsCount;
    }

    private List<String> getAllSitesInodes() throws DotDataException {
        final DotConnect db = new DotConnect();
        final List<Map<String, Object>> results = db.setSQL(ALL_SITES_INODES).loadObjectResults();
        return results.stream().map(map -> (String) map.get("inode")).collect(Collectors.toList());
    }

}
