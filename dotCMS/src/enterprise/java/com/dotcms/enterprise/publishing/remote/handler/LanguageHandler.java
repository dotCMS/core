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

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.LanguageBundler;
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
	private PublisherConfig config;

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

	private void handleLanguages(Collection<File> languages) throws DotPublishingException, DotDataException {
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
                                    "= '%s')as there are remaining content dependencies: %s", localLanguage,
                                    remoteLanguage, ex.getMessage());
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
            final String errorMsg = String.format("An error occurred when processing Language in '%s' with ID '%s': %s",
                    workingOn, (null == remoteLanguage ? "(empty)" : remoteLanguage), (null == remoteLanguage ? "(empty)" : remoteLanguage
                            .getId()), e.getMessage());
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
			    //The slot is taken.. This is very rare but still a possibility. So will just add a new Lang and move on.
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
