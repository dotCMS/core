/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.util.Logger;

import java.io.File;

public class BundleXMLascHandler implements IHandler {

    private PublisherConfig pc;

    public BundleXMLascHandler(PublisherConfig pc) {
        this.pc = pc;
    }

    @Override
    public void handle(File bundleFolder) throws Exception {
        /*
        File bundleXMLasc = new File(bundleFolder,"bundle.xml.asc");
        File bundleXML = new File(bundleFolder,"bundle.xml");
        try {
            byte[] data=FileUtils.readFileToByteArray(bundleXML);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5=md.digest(data);
            byte[] asc=FileUtils.readFileToByteArray(bundleXMLasc);
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < md5.length; i++) {
                if ((0xff & md5[i]) < 0x10) {
                    hexString.append("0"
                            + Integer.toHexString((0xFF & md5[i])));
                } else {
                    hexString.append(Integer.toHexString(0xFF & md5[i]));
                }
            }
           LicenseManager.getInstance().checkBundleXMLasc(hexString.toString(), asc);
        }
        catch(Exception ex) {
            throw new DotBundleException(ex.getMessage(), ex);
        }
        */ 
        Logger.debug(this.getClass(), "No longer Implemented");
    }

    @Override
    public String getName() {
        return "Bundle XML asc Handler";
    }

}
