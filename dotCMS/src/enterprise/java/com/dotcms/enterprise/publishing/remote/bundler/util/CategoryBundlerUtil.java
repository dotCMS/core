/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.bundler.util;

import com.dotcms.publisher.pusher.wrapper.CategoryWrapper;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.PublisherUtil;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
    public static void writeCategory(final BundleOutput output,
                                     final String fileExtension, final CategoryWrapper categoryWrapper)
            throws DotSecurityException, DotDataException {

        String liveWorking = categoryWrapper.getCategory().isLive() ? "live" : "working";

        String uri = categoryWrapper.getCategory().getInode();

        if (!uri.endsWith(fileExtension)) {
            uri.replace(fileExtension, "");
            uri.trim();
            uri += fileExtension;
        }

        String myFileUrl = File.separator
                + liveWorking + File.separator
                + APILocator.getHostAPI().findSystemHost().getHostname()
                + File.separator
                + uri;

        if (!output.exists(myFileUrl)) {
            try(final OutputStream outputStream = output.addFile(myFileUrl)) {
                BundlerUtil.objectToXML(categoryWrapper, outputStream);
                output.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());
            } catch (IOException e ) {
                Logger.error( PublisherUtil.class, e.getMessage(), e );
            }
        }
    }

}
