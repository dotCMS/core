/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.io.FileFilter;

import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.util.Logger;

public class BundleXMLAsc implements IBundler {

    
    @Override
    public String getName() {
        return "Bundle XML asc";
    }

    @Override
    public void setConfig(PublisherConfig pc) {
    }

    @Override
    public void setPublisher(IPublisher publisher) {
    }


    @Override
    public void generate(BundleOutput output, BundlerStatus status) throws DotBundleException {
        /*
        File bundleXMLasc = new File(bundleRoot,"bundle.xml.asc");
        File bundleXML = new File(bundleRoot,"bundle.xml");
        try {
            byte[] data=FileUtils.readFileToByteArray(bundleXML);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5=md.digest(data);
           // FileUtils.writeByteArrayToFile(bundleXMLasc, LicenseManager.getInstance().createBundleXMLasc(md5));
        }
        catch(Exception ex) {
            throw new DotBundleException(ex.getMessage(), ex);
        }
        */
        Logger.debug(this.getClass(), "No longer Implemented");
    }

    @Override
    public FileFilter getFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().equals("bundle.xml.asc");
            }
        };
    }

}
