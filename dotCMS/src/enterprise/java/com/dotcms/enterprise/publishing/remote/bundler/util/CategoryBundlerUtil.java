/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
