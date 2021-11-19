
package com.dotcms.util;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
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
import java.util.Arrays;
import java.util.Date;
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
            "select working_inode, live_inode" +
                    " from contentlet_version_info" +
                    " where working_inode > ?" +
                    " order by working_inode limit 10000";
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
    static final FileFilter FILE_FILTER = pathname -> Arrays
            .stream(ACCEPTED)
            .anyMatch(dir ->
                    (pathname.getPath() + "/").contains(dir));

    /**
     * Utility private and empty constructor.
     */
    private AssetExporterUtil() {}

    /**
     * Resolves the file name that's going to be used to be used as recommendation when saving.
     *
     * @return file name
     */
    public static String resolveFileName() {
        final String hostName = Try
                .of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false).getHostname())
                .getOrElse("dotcms");
        return StringUtils.sanitizeFileName(hostName)
                + "_assets_"
                + DateUtil.EXPORTING_DATE_FORMAT.format(new Date())
                + ".tar.gz";
    }

    /**
     * Builds the tar.gz file containing the assets.
     * 
     * @param out output stream
     */
     public static void exportAssets(final OutputStream out) throws IOException {
         synchronized (AssetExporterUtil.class) {
             try (final TarArchiveOutputStream taos =
                          new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(out)))) {
                 taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
                 taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

                 FileUtil.listFilesRecursively(PARENT.get(), FILE_FILTER)
                         .stream()
                         .filter(File::isFile)
                         .forEach(file -> {
                             final String relativeFilePath = file.getPath().replace(PARENT.get().getPath(), "assets");
                             Logger.info(
                                     AssetExporterUtil.class,
                                     String.format(
                                             "Adding file %s (%s) to zip output stream",
                                             file.getName(),
                                             relativeFilePath));
                             final TarArchiveEntry tarEntry = new TarArchiveEntry(file, relativeFilePath);
                             tarEntry.setSize(file.length());
                             try {
                                 taos.putArchiveEntry(tarEntry);
                                 try (InputStream in = Files.newInputStream(file.toPath())) {
                                     taos.write(IOUtils.toByteArray(in));
                                 }
                                 taos.closeArchiveEntry();
                             } catch (Exception e) {
                                 Logger.error(AssetExporterUtil.class, "Error generating assets zip file", e);
                                 throw new DotRuntimeException(e);
                             }
                         });
             }
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
    @CloseDBIfOpened
    static Tuple2<String, Set<String>> loadUpInodes(final EXPORT_VERSION version, final String lastCursorId) {
        final DotConnect db = new DotConnect().setSQL(LIVE_WORKING_SQL).addParam(lastCursorId);
        final Set<String> inodes = new HashSet<>();
        final StringWriter nextCursor = new StringWriter();
        try {
            db.loadObjectResults()
                    .forEach(contentVersion -> {
                        final String live = (String) contentVersion.get("live_inode");
                        final String working = (String) contentVersion.get("working_inode");

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
            Logger.error(AssetExporterUtil.class, "Cannot load Inodes", e);
            throw new DotRuntimeException(e);
        }

        return Tuple.of(nextCursor.toString(), inodes);
    }

}
