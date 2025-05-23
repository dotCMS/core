package com.dotcms.util;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.TarUtil;
import com.liferay.util.FileUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for exporting assets.
 *
 * @author vico
 */
public class AssetExporterUtil {

    static final String[] ACCEPTED = new String[] {
            "/0/",
            "/1/",
            "/2/",
            "/3/",
            "/4/",
            "/5/",
            "/6/",
            "/7/",
            "/8/",
            "/9/",
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
             // Use TarUtil to create a secure TAR.GZ output stream
             try (final TarArchiveOutputStream taos = TarUtil.createTarGzOutputStream(out)) {
                 FileUtil.listFilesRecursively(PARENT.get(), FILE_FILTER)
                         .stream()
                         .filter(File::isFile)
                         .forEach(file -> {
                             // Get canonical paths for proper path handling
                             try {
                                 // Convert to Path objects for more robust handling
                                 Path parentPath = PARENT.get().toPath().toRealPath();
                                 Path filePath = file.toPath().toRealPath();
                                 
                                 // Create relative path using Path operations
                                 Path relativePath = parentPath.relativize(filePath);
                                 // Prepend "assets/" prefix to the relative path
                                 String entryPath = "assets/" + relativePath.toString().replace('\\', '/');
                                 
                                 Logger.info(
                                         AssetExporterUtil.class,
                                         String.format(
                                                 "Adding file %s (%s) to tar archive",
                                                 file.getName(),
                                                 entryPath));
                                 
                                 // Use TarUtil to safely add the file to the archive
                                 TarUtil.addFileToTar(taos, file, entryPath);
                             } catch (IOException e) {
                                 Logger.error(AssetExporterUtil.class, "Error generating assets file path", e);
                                 throw new DotRuntimeException(e);
                             } catch (Exception e) {
                                 Logger.error(AssetExporterUtil.class, "Error generating assets tar file", e);
                                 throw new DotRuntimeException(e);
                             }
                         });
             }
         }
    }

}
