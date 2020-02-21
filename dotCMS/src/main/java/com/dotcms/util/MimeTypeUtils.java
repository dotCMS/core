package com.dotcms.util;

import com.dotcms.tika.TikaUtils;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.rainerhahnekamp.sneakythrow.Sneaky;

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

    /**
     * Gets the mime type of a file.
     * @param binary {@link File}
     * @return String
     */
    public static String getMimeType (final File binary) {

        final Path path = binary.toPath();
        String mimeType = Sneaky.sneak(() -> Files.probeContentType(path));

        if  (!UtilMethods.isSet(mimeType)) {

            mimeType    = Config.CONTEXT.getMimeType(binary.getAbsolutePath());

            if( !UtilMethods.isSet(mimeType)){
                try {
                    mimeType = new TikaUtils().detect(binary);
                } catch(Exception e) {
                    Logger.warn(MimeTypeUtils.class, e.getMessage() +  e.getStackTrace()[0]);
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

        final MimeType mimeTypeObj1 = Sneaky.sneak(() -> new MimeType(mimeType1));
        final MimeType mimeTypeObj2 = Sneaky.sneak(() -> new MimeType(mimeType2));
        return mimeTypeObj1.match(mimeTypeObj2);
    }
}
