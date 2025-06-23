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
import com.dotcms.enterprise.publishing.remote.bundler.LanguageBundler;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.LanguageWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Language-related information inside a
 * bundle and saves it in the receiving instance. This class will read and process only the {@link Language} data
 * files.
 * <p>
 * Languages must be created and configured in dotCMS before contributors may create and edit content in different
 * languages on your site.
 *
 * @author root
 * @since Mar 7, 2013
 */
public class LanguageHandler implements IHandler {

	private final PublisherConfig config;

	public LanguageHandler(PublisherConfig config) {
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
		Collection<File> languages = FileUtil.listFilesRecursively(bundleFolder, new LanguageBundler().getFileFilter());

		handleLanguages(languages);
	}

	private void handleLanguages(Collection<File> languages) throws DotPublishingException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
			throw new RuntimeException("need an enterprise pro license to run this");
		}
        File workingOn = null;
        Language remoteLanguage = null;
	    try{

			final List<Language> publishCollectedLanguages = new ArrayList<>();
	        final XStream xstream = XStreamHandler.newXStreamInstance();

	        for(final File languageFile: languages) {
                workingOn = languageFile;
	        	if(languageFile.isDirectory()) {
	        	  continue;
	        	}
                LanguageWrapper languageWrapper;
				try(final InputStream input = Files.newInputStream(languageFile.toPath())){
                    languageWrapper = (LanguageWrapper) xstream.fromXML(input);
				}

	        	remoteLanguage = languageWrapper.getLanguage();

	        	if(languageWrapper.getOperation().equals(PushPublisherConfig.Operation.PUBLISH)) {
	        	   if( remoteLanguage.getId() > 0) {
	        	       publishCollectedLanguages.add(remoteLanguage);
	        	   }
	        	} else { // UNPUBLISH
					//We can not trust in the given language id, could be different to the one on this server
					final Language localLanguage = APILocator.getLanguageAPI().getLanguage(remoteLanguage.getLanguageCode(), remoteLanguage.getCountryCode());

					if(UtilMethods.isSet(localLanguage) && localLanguage.getId() > 0) {
	        		    try {
	        		        APILocator.getLanguageAPI().deleteLanguage(localLanguage);

							PushPublishLogger.log(getClass(), PushPublishHandler.LANGUAGE, PushPublishAction.UNPUBLISH,
									Long.toString(localLanguage.getId()), null, localLanguage.getLanguage(), config.getId());
	        		    } catch (final Exception ex) {
                            final String errorMsg = String.format("Failed to delete local Language '%s' (remote lang " +
                                    "= '%s') as there are remaining content dependencies: %s", localLanguage,
                                    remoteLanguage, ExceptionUtil.getErrorMessage(ex));
                            Logger.error(this, errorMsg, ex);
	        		        throw new DotPublishingException(errorMsg, ex);
	        		    }
	        		}
	        	}
	        }

            // These collections should only contain elements when processing a PUBLISH Operation.
            // sorting is important so we don't stumble into one of the ids that gets consecutively generated by the db.
            publishCollectedLanguages.sort(Comparator.comparingLong(Language::getId));
			for(final Language language:publishCollectedLanguages){
                 addLanguageAndNormalizeConflicts(language);
			}

    	} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when processing Language in '%s' with ISO '%s' [%s]: %s",
                    workingOn, (null == remoteLanguage ? "(empty)" : remoteLanguage), (null == remoteLanguage ? "(empty)" : remoteLanguage
                            .getId()), ExceptionUtil.getErrorMessage(e));
            Logger.error(this.getClass(), errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
    	}
    }

	/**
	 * This method, add/replace or skips the creation of languages.
	 * In case of a conflict (Two different languages having the same id) this will attempt replacing any existing language with the one from the sender.
	 * @param remoteLanguage the remote language coming from the sender
	 */
	private void addLanguageAndNormalizeConflicts(final Language remoteLanguage) {
		final LanguageAPI languageAPI = APILocator.getLanguageAPI();
		final Language localLanguage = languageAPI
				.getLanguage(remoteLanguage.getLanguageCode(), remoteLanguage.getCountryCode());
		Logger.debug(getClass(), () -> "Remote: " + remoteLanguage + ", Local: " + localLanguage);

		// language exists on the receiver
		if (UtilMethods.isSet(localLanguage) && localLanguage.getId() > 0) {
			if (localLanguage.getId() != remoteLanguage
					.getId()) { //If there's a mismatch we ignore the language from the remote sender and use the local instance.
				Logger.warn(this.getClass(), () ->
						" Language '" + remoteLanguage + "' was found on the receiver under a different id. Remote Language will be ignored. "
				);
				config.mapRemoteLanguage(remoteLanguage.getId(), localLanguage);
			} else {
				//if no conflicts we could say that the local language id can be mapped to the local Language Instance.
				config.mapRemoteLanguage(localLanguage.getId(), localLanguage);
			}
		} else {
			// language does NOT exist on the receiver.
			Logger.debug(this.getClass(), ()->
					"Language " + remoteLanguage + " was not found and will be added.");
            //Check the existence of the id. See if the remote language id exists here.
			if(!languageAPI.hasLanguage(remoteLanguage.getId())){
			    languageAPI.saveLanguage(remoteLanguage);
			    //The slot isn't taken. Insert Language as it comes from the sender.
				config.mapRemoteLanguage(remoteLanguage.getId(), remoteLanguage);
				Logger.debug(this.getClass(), ()->
						"No conflict was detected for language '" + remoteLanguage + "'. Language was added as it came in.");
			} else {
			    //The slot is taken. This is very rare but still a possibility. So will just add a new Lang and move on.
				final Language newLang = newLanguageInstance(remoteLanguage);
                languageAPI.saveLanguage(newLang);
				config.mapRemoteLanguage(remoteLanguage.getId(), newLang);
				Logger.warn(this.getClass(), ()->
						"A conflict was detected for language '" + remoteLanguage + "'. Language will be added under a brand new id.");
			}
		}
	}

	/**
	 * Utility method to create a fresh instance of a Language Useful for adding new language copying
	 * the vales from another.
	 */
	private Language newLanguageInstance(final Language language) {
		return new Language(0, language.getLanguageCode(), language.getCountryCode(),
				language.getLanguage(), language.getCountry()
		);
	}

}
