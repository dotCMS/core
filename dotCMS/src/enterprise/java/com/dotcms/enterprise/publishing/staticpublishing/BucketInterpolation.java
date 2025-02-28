/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
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
