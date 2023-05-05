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

package com.dotcms.enterprise.publishing.staticpublishing;

import java.io.Serializable;
import java.net.URL;
import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;

/**
 * Encapsulates the AWSS3 configuration
 * @author jsanca
 */
public final class AWSS3Configuration implements Serializable {

    private final String accessKey;
    private final String secretKey;
    private final URL endPoint;
    private final String region;

    private AWSS3Configuration(final String accessKey, final String secretKey, final String endPoint,
            final String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endPoint = Try.of(()-> new URL(endPoint)).getOrElse((URL) null);
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getEndPoint() {
        return Try.of(()-> endPoint.toString()).getOrElse((String) null);
    }

    public String getRegion() {
        return region;
    }

    public static final class Builder {

        private String accessKey;
        private String secretKey;
        private  String endPoint;
        private  String region;

        public Builder accessKey(final String accessKey) {

            this.accessKey = accessKey;
            return this;
        }

        public Builder secretKey(final String secretKey) {

            this.secretKey = secretKey;
            return this;
        }

        public Builder endPoint(final String endPoint) {

            this.endPoint = endPoint;
            return this;
        }

        public Builder region(final String region) {

            this.region = region;
            return this;
        }

        public AWSS3Configuration build () {

            return new AWSS3Configuration(this.accessKey, this.secretKey, this.endPoint, this.region);
        }
    }
} // E:O:F:AWSS3Configuration.
