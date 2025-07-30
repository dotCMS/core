package com.dotcms.telemetry.collectors.theme;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Theme;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * This Metric collects the total size -- in bytes -- of all files that live under every Theme in
 * all Sites in the dotCMS repository.
 *
 * @author Jose Castro
 * @since Mar 25th, 2025
 */
public class TotalSizeOfFilesPerThemeMetricType extends ThemeDataMetricType {

    private static final String GET_FILE_INODES_UNDER_THEME_QUERY =
            "SELECT live_inode " +
                    "FROM contentlet_version_info cvi INNER JOIN identifier id ON cvi.identifier = id.id " +
                    "WHERE id.host_inode = ? AND id.parent_path LIKE ? AND cvi.working_inode = cvi.live_inode " +
                    "AND deleted IS FALSE AND id.asset_type = 'contentlet' AND id.asset_subtype = 'FileAsset'";
    private static final String DATA_MAP_KEY = "//%s%s";


    @Override
    public String getName() {
        return "TOTAL_SIZE_OF_FILES_PER_THEME";
    }

    @Override
    public String getDescription() {
        return "Total size of all files under every Theme in all Sites";
    }

    @Override
    Optional<Object> getValue(final List<Theme> themes) {
        final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
        final Map<String, Object> data = new HashMap<>();
        try {
            DotConnect dc;
            for (final Theme theme : themes) {
                dc = new DotConnect();
                final Set<String> fileInodes = dc.setSQL(GET_FILE_INODES_UNDER_THEME_QUERY)
                        .addParam(theme.getHostId())
                        .addParam(theme.getPath() + "%")
                        .loadObjectResults().stream().map(row -> row.get("live_inode").toString()).collect(Collectors.toSet());
                final AtomicReference<Long> totalSize = new AtomicReference<>((long) 0);
                fileInodes.forEach(inode -> {
                    try {
                        Optional.ofNullable(fileAssetAPI.find(inode, APILocator.systemUser(), false))
                                .ifPresent(fileAsset -> totalSize.updateAndGet(v -> v + fileAsset.getFileSize()));
                    } catch (final DotDataException | DotSecurityException e) {
                        Logger.warn(this, String.format("Failed to retrieve size for File Asset with Inode " +
                                "'%s' in Theme '%s': %s", inode, theme.getName() , ExceptionUtil.getErrorMessage(e)));
                    }
                });
                data.put(String.format(DATA_MAP_KEY, theme.getHost().getHostname(), theme.getPath()),
                        totalSize.get());
            }
        } catch (final DotDataException e) {
            Logger.warn(this, String.format("An error occurred when calculating the total size of files per Theme: %s",
                    ExceptionUtil.getErrorMessage(e)));
        }
        return Optional.of(data);
    }

}
