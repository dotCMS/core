package com.dotcms.publisher.myTest.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.myTest.PushPublisherConfig;
import com.dotcms.publisher.myTest.wrapper.HTMLPageWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class HTMLPageBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	
	public final static String HTML_EXTENSION = ".html.xml" ;

	@Override
	public String getName() {
		return "Folder bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig) pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();

		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(HTMLPageBundler.class,e.getMessage(),e);
		}
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<200)
	        throw new RuntimeException("need an enterprise license to run this bundler");
		
		//Get HTML pages linked with the content
		Set<String> htmlIds = config.getHTMLPages();
		
		try {
			List<HTMLPage> htmlPages = new ArrayList<HTMLPage>();
			
			for (String htmlId : htmlIds) {
				htmlPages.add(APILocator.getHTMLPageAPI().loadLivePageById(htmlId, systemUser, false));
				htmlPages.add(APILocator.getHTMLPageAPI().loadWorkingPageById(htmlId, systemUser, false));
			}
			
			for(HTMLPage page : htmlPages) {
				writePage(bundleRoot, page);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
		
	}

	
	
	private void writePage(File bundleRoot, HTMLPage page)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		Identifier pageId = APILocator.getIdentifierAPI().find(page);
		HTMLPageWrapper wrapper = 
				new HTMLPageWrapper(page, pageId);
		
		String liveworking = page.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI().find(page).getURI().replace("/", File.separator);
		if(!uri.endsWith(HTML_EXTENSION)){
			uri.replace(HTML_EXTENSION, "");
			uri.trim();
			uri += HTML_EXTENSION;
		}
		
		Host h = APILocator.getHostAPI().find(pageId.getHostId(), systemUser, false);
		
		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() + File.separator
				+ config.getLanguage() + uri;

		File htmlFile = new File(myFileUrl);
		htmlFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, htmlFile, true);
		htmlFile.setLastModified(Calendar.getInstance().getTimeInMillis());
	}

	@Override
	public FileFilter getFileFilter(){
		return new HTMLPageBundlerFilter();
	}
	
	public class HTMLPageBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(HTML_EXTENSION));
		}

	}

}
