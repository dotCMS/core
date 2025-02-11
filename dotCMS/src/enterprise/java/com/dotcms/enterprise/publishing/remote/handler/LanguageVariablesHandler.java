/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.LanguageVariablesBundler;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Language Variable-related information
 * inside a bundle and saves it in the receiving instance. This class will read and process only the Language data
 * files.
 * <p>
 * Language Variables allow you to create multiple versions of the variable in different lanaguages, all of which use
 * the same key, but have different values depending on the language. You can then reference the key from within any
 * element, and dotCMS will automatically return the appropriate value for the key depending on the user's chosen
 * language.
 *
 * @author Anibal Gomez
 * @since Dec 15, 2016
 */
public class LanguageVariablesHandler implements IHandler {

	private PublisherConfig config;

	public LanguageVariablesHandler(PublisherConfig config) {
		this.config = config;
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(File bundleFolder) throws Exception {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
		//For each content take the wrapper and save it on DB
        Collection<File> messages = new ArrayList<>();
        if(new File(bundleFolder + File.separator + "messages").exists()){
        	messages = FileUtil.listFilesRecursively(new File(bundleFolder + File.separator + "messages"), new LanguageVariablesBundler().getFileFilter());
        }

		handleMessages(messages);
	}

	private void handleMessages(Collection<File> messages) throws DotPublishingException, DotDataException, IOException{
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
		String messagesPath = APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "messages";
		File messagesDir = new File(messagesPath);

		for(File language: messages){
			FileUtils.copyFileToDirectory(language, messagesDir, false);

			PushPublishLogger.log(getClass(), PushPublishHandler.LANGUAGE_FILE, PushPublishAction.PUBLISH,
					null, null, language.getName(), config.getId());
		}

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LANGUAGE_FILE_FLUSH_CACHE", true)){
			List<String> languagesForReload = getLanguagesFromMessagesFiles(messages);
			if(messages != null && messages.size() > 0) {
				for(String lang: languagesForReload){
					//Cleaning cache
					String[] lang_split = lang.split("[_]");
		            removeLanguageKeys( lang_split[0], lang_split.length==2?lang_split[1]:null );
		            //Force the reading of the languages files as we add/remove/edit keys
		            MultiMessageResources multiMessages = (MultiMessageResources) Config.CONTEXT.getAttribute( Globals.MESSAGES_KEY );
		            multiMessages.reload();
				}
			}
		}
	}

	private void removeLanguageKeys(String languageCode, String countryCode){
		Logger.debug(this, "BEGIN: Remove Language Key: languageCode: " + languageCode + ", countryCode: " + countryCode);
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = CacheLocator.getLanguageCache().getPrimaryGroup() + "_Keys_" + (countryCode != null?languageCode + "_" + countryCode:languageCode);
        cache.remove(languageKey, CacheLocator.getLanguageCache().getPrimaryGroup());
        Logger.debug(this, "END:   Remove Language Key: languageCode: " + languageCode + ", countryCode: " + countryCode);
	}

	/**
	 * I use this method for retrieve the language and (optionally) the country codes from the messages files that are into the current bundle
	 *
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * May 21, 2013 - 4:29:09 PM
	 */
	private List<String> getLanguagesFromMessagesFiles(Collection<File> messages){
		List<String> languages = new ArrayList<>();
		StringBuffer sb;
		String[] tmp;
		String name;
		for(File f:messages){
			name = f.getName();
			tmp = name.split("[_]");
			// the language file has this pattern: cms_language_<languageCode>[_<countryCode>].properties
			// if the split has lenght = 4 then this file has the country code too, otherwise if the lenght = 3 the countryCode there isn't
			sb = new StringBuffer();
			if (tmp.length > 2) {
				sb.append(tmp[2].split(
					"[.]")[0]); // the languageCode: the "." split is mandatory because if the file hasn't a countryCode the tmp[2] string is <languageCode>.properties
				if (tmp.length == 4) { // we have the countryCode too
					sb.append("_");
					sb.append(
						tmp[3].split("[.]")[0]); // need the split because the last element is <countryCode>.properties
				}
				languages.add(sb.toString());
			}
		}
		return languages;
	}

}
