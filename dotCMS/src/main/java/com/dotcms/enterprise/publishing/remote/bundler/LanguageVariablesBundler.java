package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
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
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PushPublishLogger;

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
	public void generate(File bundleRoot, BundlerStatus status)
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
				copyFileToBundle(bundleRoot, messagesDir, lastBundleDate);

			Set<String> languagesIds = config.getLanguages();

			for (String id : languagesIds) {
				Language language = APILocator.getLanguageAPI().getLanguage(id);
				writeLanguage(bundleRoot, language);
			}

		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}

	private void writeLanguage(File bundleRoot, Language language)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{

		LanguageWrapper wrapper = new LanguageWrapper(language);
		wrapper.setOperation(config.getOperation());

		String uri = Long.toString(language.getId());
		if(!uri.endsWith(LANGUAGE_EXTENSION)){
			uri.replace(LANGUAGE_EXTENSION, "");
			uri.trim();
			uri += LANGUAGE_EXTENSION;
		}

		String myFileUrl = bundleRoot.getPath() + File.separator
				+"live" + File.separator
				+ APILocator.getHostAPI().findSystemHost().getHostname() +File.separator + uri;

		File strFile = new File(myFileUrl);
		if(!strFile.exists()){
			strFile.mkdirs();

			BundlerUtil.objectToXML(wrapper, strFile, true);
			strFile.setLastModified(Calendar.getInstance().getTimeInMillis());
		}
	}



	private void copyFileToBundle(File bundleRoot, File messagesDir, Date lastBundleDate)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		final String myFolderUrl = bundleRoot.getPath() + File.separator + "messages";
		File bundleFolderMessages = new File(myFolderUrl);

		final String bundleId = config.getId();
		Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
		final boolean applyForcePush = bundle == null ? false : bundle.isForcePush();

		for(File lang : FileUtils.listFiles(messagesDir, new String []{"properties"}, false)) {
			long lastMod = lang.lastModified();
			long startTime = -1;
			if(lastBundleDate != null)
				startTime = lastBundleDate.getTime();
			if(lastMod > startTime || applyForcePush) {
				if(!bundleFolderMessages.exists())
					bundleFolderMessages.mkdirs();

				FileUtils.copyFileToDirectory(lang, bundleFolderMessages);
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
