package com.dotcms.util;

import com.dotcms.tika.TikaUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.rainerhahnekamp.sneakythrow.Sneaky;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mime Type Utils
 * @author jsanca
 */
public class MimeTypeUtils {

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
            }
        }

        return mimeType;
    }
}
