package com.dotmarketing.business.cache.util;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.util.FileUtil;

import java.io.File;
import java.util.Date;

/**
 * @author Jonathan Gamba
 *         Date: 9/2/15
 */
public class CacheUtil {

    public static void Moveh2dbDir () throws Exception {

        File h2dbDir = new File(ConfigUtils.getDynamicContentPath() + File.separator + "h2db");
        File trashDir = new File(ConfigUtils.getDynamicContentPath() + File.separator + "trash" + File.separator + "h2db" + APILocator.getContentletIndexAPI().timestampFormatter.format(new Date()));

        //move the dotsecure/h2db dir to dotsecure/trash/h2db{timestamp}
        //FileUtil.move(h2dbDir, trashDir);
        FileUtil.copyDirectory(h2dbDir, trashDir);
        FileUtil.deltree(h2dbDir, false);

        //fire a separate thread that deletes the contents of the dotsecure/trash directory.
        new deleteTrashDir().start();
    }

    private static class deleteTrashDir extends Thread {

        @Override
        public void run () {
            File trashDir = new File(ConfigUtils.getDynamicContentPath() + File.separator + "trash");
            FileUtil.deltree(trashDir, false);
        }
    }

}