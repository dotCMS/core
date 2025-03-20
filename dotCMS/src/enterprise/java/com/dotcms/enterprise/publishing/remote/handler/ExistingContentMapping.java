/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
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
