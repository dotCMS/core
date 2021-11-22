
package com.dotcms.util;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for exporting assets.
 *
 * @author vic
 */
public class AssetExporterUtil {

    enum EXPORT_VERSION {
        LIVE, LIVE_WORKING;
    }

    static final String LIVE_WORKING_SQL =
                    "select working_inode, live_inode from contentlet_version_info where working_inode > ? order by" +
                            " working_inode limit 10000";
    static final String[] ACCEPTED = new String[] {
            "/0/",
            "/1/",
            "/2/",
            "/3/",
            "/4/",
            "/5/",
            "/6/",
            "/7/",
            "/a/",
            "/b/",
            "/c/",
            "/d/",
            "/e/",
            "/f/",
            "dotcms.sql.gz"
    };
    static final Lazy<File> PARENT = Lazy.of(() -> new File(ConfigUtils.getAbsoluteAssetsRootPath()));
    static final FileFilter FILE_FILTER = pathname -> {
        for (String dir : ACCEPTED) {
            if ((pathname.getPath()+"/").contains(dir)) {
                return true;
            }
        }
        return false;
    };

    /**
     * Utility private and empty constructor.
     */
    private AssetExporterUtil() {}
    
    /**
     * Builds the tar.gz file containing the assets.
     * 
     * @param out output stream
     */
    public static void exportAssets(OutputStream out) throws IOException {
        try (TarArchiveOutputStream taos =
                     new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(out)))) {
            taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            FileUtil.listFilesRecursively(PARENT.get(), FILE_FILTER)
                    .stream()
                    .filter(File::isFile)
                    .forEach(file -> {
                        final String relativeFilePath = file.getPath().replace(PARENT.get().getPath(), "assets");
                        TarArchiveEntry tarEntry = new TarArchiveEntry(file, relativeFilePath);
                        tarEntry.setSize(file.length());
                        try {
                            taos.putArchiveEntry(tarEntry);
                            try (InputStream in = Files.newInputStream(file.toPath())) {
                                taos.write(IOUtils.toByteArray(in));
                            }
                            taos.closeArchiveEntry();
                        } catch (Exception e) {
                            throw new DotRuntimeException(e);
                        }
                    });
        }
    }

    // TODO: verify if this is still the case
    /**
     * Unused at this time, intended to load a list of live/working inodes to check against so old
     * versions do not get exported.
     * 
     * @param version version
     * @param lastCursorId last cursor id
     * @return loaded inodes
     */
    static Tuple2<String, Set<String>> loadUpInodes(final EXPORT_VERSION version, final String lastCursorId) {
        final DotConnect db = new DotConnect().setSQL(LIVE_WORKING_SQL).addParam(lastCursorId);
        final Set<String> inodes = new HashSet<>();
        final StringWriter nextCursor = new StringWriter();
        try {
            db.loadObjectResults()
                    .forEach(m -> {
                        final String live = (String) m.get("live_inode");
                        final String working = (String) m.get("working_inode");

                        nextCursor.getBuffer().setLength(0);
                        nextCursor.write(working);

                        if (version == EXPORT_VERSION.LIVE && UtilMethods.isEmpty(live)) {
                            return;
                        }

                        if (version == EXPORT_VERSION.LIVE) {
                            inodes.add(live);
                            return;
                        }

                        if (version == EXPORT_VERSION.LIVE_WORKING) {
                            inodes.add(working);
                            if (!working.equals(live)) {
                                inodes.add(live);
                            }
                        }
            });
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }

        return Tuple.of(nextCursor.toString(), inodes);
    }

}
