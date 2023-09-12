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
