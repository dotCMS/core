package com.dotcms.enterprise.publishing.remote.bundler.util;

import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publishing.BundlerUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.io.File;
import java.util.Calendar;

/**
 * Util class used by Bundlers {@link com.dotcms.enterprise.publishing.remote.bundler.CategoryBundler}
 * and {@link com.dotcms.enterprise.publishing.remote.bundler.CategoryFullBundler}
 *
 * @author Oscar Arrieta on 7/17/17.
 */
public class CategoryBundlerUtil {

    /**
     * Util method o write to file system the category using BundlerUtil.objectToXM().
     */
    public static void writeCategory(String fileExtension, File bundleRoot,
            CategoryWrapper categoryWrapper) throws DotSecurityException, DotDataException {

        String liveWorking = categoryWrapper.getCategory().isLive() ? "live" : "working";

        String uri = categoryWrapper.getCategory().getInode();

        if (!uri.endsWith(fileExtension)) {
            uri.replace(fileExtension, "");
            uri.trim();
            uri += fileExtension;
        }

        String myFileUrl = bundleRoot.getPath()
                + File.separator
                + liveWorking + File.separator
                + APILocator.getHostAPI().findSystemHost().getHostname()
                + File.separator
                + uri;

        File strFile = new File(myFileUrl);

        if (!strFile.exists()) {
            strFile.mkdirs();

            BundlerUtil.objectToXML(categoryWrapper, strFile, true);
            strFile.setLastModified(Calendar.getInstance().getTimeInMillis());
        }
    }

}
