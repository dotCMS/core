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

import com.dotcms.publisher.pusher.wrapper.ContentWrapper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains two maps:
 * existingContent = this contains a pair with the content id and the lang id of the remote and the local content
 * languagesMap = this contains just the lang id of the remote and local
 */
class ExistingContentMapping {

    private final Map<Pair<String,Long>, Pair<String,Long>> existingContent;
    private final Map<Long, Long> languagesMap;

    ExistingContentMapping() {
        this.existingContent = new HashMap<>();
        this.languagesMap = new HashMap<>();
    }

    boolean hasExistingContent(final Pair<String, Long> contentIdAndLang) {
        return existingContent.containsKey(contentIdAndLang);
    }

    String getExistingContentIdentifier(final Pair<String, Long> contentIdAndLang) {
        return existingContent.get(contentIdAndLang).getLeft();
    }

    Pair<String, Long> getExistingContentIdentifierAndLangId(final Pair<String, Long> contentIdAndLang) {
        return existingContent.get(contentIdAndLang);
    }

    void addExistingContent(
            final Pair<String, Long> sourceContentIdAndLang,
            final Pair<String, Long> targetContentIdAndLang) {
        existingContent.put(sourceContentIdAndLang, targetContentIdAndLang);
    }

    Pair<Long,Long> getRemoteLocalLanguages(final ContentWrapper contentWrapper) {
        final Language remoteLang = contentWrapper.getLanguage();
        final long remoteContentLangId = contentWrapper.getContent().getLanguageId();
        if (UtilMethods.isSet(remoteLang) && remoteLang.getId() > 0) {
            return getRemoteLocalLanguages(remoteLang);
        } else {
            return Pair.of(remoteContentLangId, remoteContentLangId);
        }
    }

    Pair<Long,Long> getRemoteLocalLanguages(final Language remoteLang) {
        final long remoteLangId = remoteLang.getId();
        if (languagesMap.containsKey(remoteLangId)) {
            return Pair.of(remoteLangId, languagesMap.get(remoteLangId));
        } else {
            return addRemoteLocalLanguagesMapping(remoteLang);
        }
    }

    long getLocalForRemoteLanguage(final long remoteLangId) {
        if (languagesMap.containsKey(remoteLangId)) {
            return languagesMap.get(remoteLangId);
        } else {
            return remoteLangId;
        }
    }

    private Pair<Long,Long> addRemoteLocalLanguagesMapping(final Language remoteLang) {

        final Language localLang = APILocator.getLanguageAPI().getLanguage(
                remoteLang.getLanguageCode(), remoteLang.getCountryCode());
        if(UtilMethods.isSet(localLang) && localLang.getId() > 0) {
            languagesMap.put(remoteLang.getId(), localLang.getId());
            return Pair.of(remoteLang.getId(), localLang.getId());
        } else {
            return Pair.of(remoteLang.getId(), remoteLang.getId());
        }

    }

}