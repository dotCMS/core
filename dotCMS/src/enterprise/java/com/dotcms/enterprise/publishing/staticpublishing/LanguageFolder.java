/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.io.File;

/**
 * Represents the language folder contained in a Static Bundle. This class holds the reference to:
 * <ol>
 *     <li>The actual language folder as a {@link File} object.</li>
 *     <li>The {@link Language} object it represents.</li>
 * </ol>
 *
 * @author Freddy Rodriguez
 * @version 21.08
 * @since Aug 2, 2021
 */
public class LanguageFolder implements Comparable<LanguageFolder> {
    private final File languageFolder;
    private final Language language;

    public LanguageFolder(final File languageFolder, final Language language) {
        this.languageFolder = languageFolder;
        this.language = language;
    }

    public File getLanguageFolder() {
        return languageFolder;
    }

    public Language getLanguage() {
        return language;
    }

    @Override
    public int compareTo(final LanguageFolder anotherLanguageFolder) {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

        final long languageId1 = this.getLanguage().getId();
        final long languageId2 = anotherLanguageFolder.getLanguage().getId();
        if (languageId1 == defaultLanguage.getId() && languageId2 == defaultLanguage.getId()) {
            return 0;
        } else if (languageId1 == defaultLanguage.getId()) {
            return 1;
        } else if (languageId2 == defaultLanguage.getId()) {
            return -1;
        } else {
            return ConversionUtils.toInt(languageId1 - languageId2, 0);
        }
    }
}
