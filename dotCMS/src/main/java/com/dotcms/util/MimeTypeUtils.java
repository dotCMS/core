package com.dotcms.util;

import com.dotcms.tika.TikaUtils;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import javax.activation.MimeType;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mime Type Utils
 * @author jsanca
 */
public class MimeTypeUtils {

    public static final String ACCEPT_ALL = "*/*";
    public static final String MIME_TYPE_APP_OCTET_STREAM = "application/octet-stream";

    // defect nothing default implementation
    private static MimeTypeDetector mimeTypeDetector = path -> null;

    /**
     * Sets a mime type detector
     * @param mimeTypeDetector
     */
    public static synchronized void setMimeTypeDetector(final MimeTypeDetector mimeTypeDetector) {
        if (null != mimeTypeDetector) {
            MimeTypeUtils.mimeTypeDetector = mimeTypeDetector;
        }
    }

    /**
     * Gets the mime type of a file.
     * @param binary {@link File}
     * @return String
     */
    public static String getMimeType (final File binary) {
        if(binary==null) {
            return FileAsset.UNKNOWN_MIME_TYPE;
        }
        final Path path = binary.toPath();
        String mimeType = Try.of(() -> mimeTypeDetector.detectMimeType(path)).getOrNull();

        if  (!UtilMethods.isSet(mimeType)) {

             mimeType = Try.of(() -> Files.probeContentType(path)).getOrNull();
        }

        if  (!UtilMethods.isSet(mimeType)) {

            mimeType = Try.of(()->Config.CONTEXT.getMimeType(binary.getAbsolutePath())).getOrNull();

            if( !UtilMethods.isSet(mimeType)){
                try {
                    mimeType = new TikaUtils().detect(binary);
                } catch(Throwable e) {
                    Logger.warn(MimeTypeUtils.class, "Unable to parse MIME Type for : " + binary);
                    Logger.warn(MimeTypeUtils.class, e.getMessage() + e.getStackTrace()[0]);
                }

                if(!UtilMethods.isSet(mimeType)) {
                    mimeType = FileAsset.UNKNOWN_MIME_TYPE;
                }
            }
        }

        return mimeType;
    }

    /**
     * See if one mime type1 match into another
     * @param mimeType1 String
     * @param mimeType2 String
     * @return boolean
     */
    public static boolean match (final String mimeType1, final String mimeType2) {

        if (ACCEPT_ALL.equals(mimeType1)) {
            return true;
        }

        if (FileAsset.UNKNOWN_MIME_TYPE.equals(mimeType1) ||
                FileAsset.UNKNOWN_MIME_TYPE.equals(mimeType2)) {

            return false;
        }

        final MimeType mimeTypeObj1 = Try.of(() -> new MimeType(mimeType1)).getOrNull();
        final MimeType mimeTypeObj2 = Try.of(() -> new MimeType(mimeType2)).getOrNull();
        return mimeTypeObj1 != null && mimeTypeObj2 != null && mimeTypeObj1.match(mimeTypeObj2);
    }
}
