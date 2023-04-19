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

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.util.HashMap;
import java.util.Map;

/**
 * This object will contain all the information after a the Bucket Interpolation, we need to know about the matches,
 * language and Host in order oto use them on the Prefix to avoid putting wrong information under a bucket name.
 *
 * Created by Oscar Arrieta on 1/10/17.
 */
public class BucketInterpolation {

    private String bucketName;
    private Map<String, Object> bucketNameMatches;
    private Language language;
    private Host host;

    public BucketInterpolation(String bucketName,
                               Map<String, Object> bucketNameMatches,
                               Language language,
                               Host host){
         this.bucketName = bucketName;
         this.bucketNameMatches = bucketNameMatches;
         this.language = language;
         this.host = host;

    }

    public BucketInterpolation(String bucketName){
        this.bucketName = bucketName;
        this.bucketNameMatches = new HashMap<>();
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public Map<String, Object> getBucketNameMatches() {
        return bucketNameMatches;
    }

    public void setBucketNameMatches(Map<String, Object> bucketNameMatches) {
        this.bucketNameMatches = bucketNameMatches;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BucketInterpolation that = (BucketInterpolation) o;

        if (!bucketName.equals(that.bucketName)) {
            return false;
        }
        if (language != null ? !language.equals(that.language) : that.language != null) {
            return false;
        }
        return host != null ? host.getHostname().equals(that.host.getHostname()) : that.host == null;
    }

    @Override
    public int hashCode() {
        int result = bucketName.hashCode();
        result = 31 * result + (language != null ? language.toString().hashCode() : 0);
        result = 31 * result + (host != null ? host.getHostname().hashCode() : 0);
        return result;
    }
}
