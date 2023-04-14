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
