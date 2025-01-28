/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditAPIImpl;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.LanguageWrapper;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PushPublishLogger;

/**
 * This bundler will take the list of {@link Language} objects that are being
 * pushed and will write them in the file system in the form of an XML file.
 * This information will be part of the bundle that will be pushed to the
 * destination server.
 * 
 * @author Jorge Urdaneta
 * @version 1.0
 * @since Mar 7, 2013
 *
 */
public class LanguageBundler implements IBundler {

	public final static String LANGUAGE_EXTENSION = ".language.xml" ;
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
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this bundler");
	    }
	    PublishAuditAPI publishAuditAPI = PublishAuditAPIImpl.getInstance();
		PublishAuditHistory currentStatusHistory = null;
		try {

			Set<String> languagesIds = config.getIncludedLanguages();
			// Updating the audit table
			if (!config.isDownloading() && languagesIds.size() > 0) {
				currentStatusHistory = publishAuditAPI.getPublishAuditStatus(config.getId()).getStatusPojo();
				if (currentStatusHistory == null) {
					currentStatusHistory = new PublishAuditHistory();
				}
				if (currentStatusHistory.getBundleStart() == null) {
					currentStatusHistory.setBundleStart(new Date());
					PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
					publishAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLING,
							currentStatusHistory);
				}
			}
			for (String id : languagesIds) {
				Language language = APILocator.getLanguageAPI().getLanguage(id);
				writeLanguage(output, language);
			}
			// Updating the audit table
			if (currentStatusHistory != null && !config.isDownloading() && languagesIds.size() > 0) {
				currentStatusHistory = publishAuditAPI.getPublishAuditStatus(config.getId()).getStatusPojo();
				currentStatusHistory.setBundleEnd(new Date());
				PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
				publishAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLING,
						currentStatusHistory);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}

	/**
	 * Writes the properties of a {@link Language} object to the file system, so
	 * that it can be bundled and pushed to the destination server.
	 * 

	 * @param output
	 *            - The root location of the bundle in the file system.
	 * @throws IOException
	 *             An error occurred when writing the rule to the file system.
	 * @throws DotDataException
	 *             An error occurred accessing the database.
	 * @throws DotSecurityException
	 *             The current user does not have the required permissions to
	 *             perform this action.
	 */
	private void writeLanguage(final BundleOutput output, final Language language)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{

		LanguageWrapper wrapper = new LanguageWrapper(language);
		wrapper.setOperation(config.getOperation());

		String uri = language.getLanguageCode() + "_" + language.getCountryCode();
		if(!uri.endsWith(LANGUAGE_EXTENSION)){
			uri.replace(LANGUAGE_EXTENSION, "");
			uri.trim();
			uri += LANGUAGE_EXTENSION;
		}

		String myFileUrl = File.separator
				+"live" + File.separator
				+ APILocator.getHostAPI().findSystemHost().getHostname() +File.separator + uri;

		if (!output.exists(myFileUrl)) {
			try (final OutputStream outputStream = output.addFile(myFileUrl)) {
				BundlerUtil.objectToXML(wrapper, outputStream);
				output.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());
			}
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new LanguageBundlerFilter();
	}

	/**
	 * A simple file filter that looks for Language data files inside a bundle.
	 * 
	 * @author Jorge Urdaneta
	 * @version 1.0
	 * @since Mar 7, 2013
	 *
	 */
	public class LanguageBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(LANGUAGE_EXTENSION));
		}

	}
}
