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

package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.LanguageWrapper;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import org.apache.commons.io.FileUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.util.FileUtil;

import static java.lang.Class.forName;

public class LanguageVariablesBundler implements IBundler {

	public final static String LANGUAGE_EXTENSION = "properties" ;
	private PublishAuditAPI aAPI =  PublishAuditAPI.getInstance();
	private PushPublisherConfig config;

	@Override
	public String getName() {
		return "Language bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig) pc;
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(final BundleOutput output, final BundlerStatus status)
			throws DotBundleException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		//Get messages directory
		String messagesPath = APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "messages";
		File messagesDir = new File(messagesPath);
		try {
			//Get last bundle date
			Date lastBundleDate = aAPI.getLastPublishAuditStatusDate();

			if(messagesDir.exists())
				copyFileToBundle(output, messagesDir, lastBundleDate);

			Set<String> languagesIds = config.getIncludedLanguages();

			for (String id : languagesIds) {
				Language language = APILocator.getLanguageAPI().getLanguage(id);
				writeLanguage(output, language);
			}

		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}

	private void writeLanguage(final BundleOutput output, final Language language)
			throws DotDataException {

		LanguageWrapper wrapper = new LanguageWrapper(language);
		wrapper.setOperation(config.getOperation());

		String uri = Long.toString(language.getId());
		if(!uri.endsWith(LANGUAGE_EXTENSION)){
			uri.replace(LANGUAGE_EXTENSION, "");
			uri.trim();
			uri += LANGUAGE_EXTENSION;
		}

		String myFileUrl = File.separator
				+"live" + File.separator
				+ APILocator.getHostAPI().findSystemHost().getHostname() +File.separator + uri;

		if ( !output.exists(myFileUrl) ) {
			try (final OutputStream outputStream = output.addFile(myFileUrl)){
				BundlerUtil.objectToXML(wrapper, outputStream);
				output.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());
			} catch ( IOException e ) {
				Logger.error( PublisherUtil.class, e.getMessage(), e );
			}
		}
	}



	private void copyFileToBundle(final BundleOutput output, final File messagesDir, final Date lastBundleDate)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		final String myFolderUrl = File.separator + "messages";

		final String bundleId = config.getId();
		Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
		final boolean applyForcePush = bundle == null ? false : bundle.isForcePush();

		for(File lang : FileUtils.listFiles(messagesDir, new String []{"properties"}, false)) {
			long lastMod = lang.lastModified();
			long startTime = -1;
			if(lastBundleDate != null)
				startTime = lastBundleDate.getTime();
			if(lastMod > startTime || applyForcePush) {
				final String destFilePath = myFolderUrl + File.separator + lang.getName();
				output.copyFile(lang, destFilePath);
			}
		}

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Languages bundled for pushing", config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new LanguageVariablesBundlerFilter();
	}

	public class LanguageVariablesBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(LANGUAGE_EXTENSION));
		}

	}
}
