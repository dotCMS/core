package com.dotcms.util;

/**
 * CloseUtils
 * @author jsanca
 */
public class CloseUtils {

    /**
     * Closes a AutoCloseable unconditionally.
     * @param closeables {@link AutoCloseable}
     */
    public static void closeQuietly (AutoCloseable... closeables) {

        if (null != closeables) {

            for (AutoCloseable closeable : closeables) {

                if (null != closeable) {

                    try {

                        closeable.close();
                    } catch (Exception e) {
                        // quiet
                    }
                }
            }
        }
    } // closeQuietly

} // E:O:F:CloseUtils.
