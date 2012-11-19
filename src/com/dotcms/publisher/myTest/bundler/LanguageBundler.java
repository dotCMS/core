package com.dotcms.publisher.myTest.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public class LanguageBundler implements IBundler {
	
	public final static String LANGUAGE_EXTENSION = ".properties" ;
	private PublishAuditAPI aAPI =  PublishAuditAPI.getInstance();

	@Override
	public String getName() {
		return "Language bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<400)
	        throw new RuntimeException("need an enterprise prime license to run this bundler");
		
		//Get messages directory
		String messagesPath = APILocator.getFileAPI().getRealAssetPath() + File.separator + "messages";
		File messagesDir = new File(messagesPath);
		try {
			//Get last bundle date
			Date lastBundleDate = aAPI.getLastPublishAuditStatusDate();
			
			if(messagesDir.exists())
				copyFileToBundle(bundleRoot, messagesDir, lastBundleDate);
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
		
	}

	
	
	private void copyFileToBundle(File bundleRoot, File messagesDir, Date lastBundleDate)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		
		
		String myFolderUrl = bundleRoot.getPath() + File.separator + "messages";
		File bundleFolderMessages = new File(myFolderUrl);
		
		for(File lang : FileUtils.listFiles(messagesDir, new String []{"properties"}, false)) {
			long lastMod = lang.lastModified();
			long startTime = -1;
			if(lastBundleDate != null)
				startTime = lastBundleDate.getTime();
			if(lastMod > startTime) {
				if(!bundleFolderMessages.exists())
					bundleFolderMessages.mkdirs();
				
				FileUtils.copyFileToDirectory(lang, bundleFolderMessages);
			}
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new LanguageBundlerFilter();
	}
	
	public class LanguageBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(LANGUAGE_EXTENSION));
		}

	}
}
