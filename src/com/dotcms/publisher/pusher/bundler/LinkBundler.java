package com.dotcms.publisher.pusher.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.LinkWrapper;
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
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class LinkBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	
	public final static String LINK_EXTENSION = ".link.xml" ;

	@Override
	public String getName() {
		return "Link bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig) pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();

		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(ContainerBundler.class,e.getMessage(),e);
		}
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<400)
	        throw new RuntimeException("need an enterprise prime license to run this bundler");
		
		Set<String> linksIds = config.getLinks();
		
		try {
			Set<Link> links = new HashSet<Link>();
			
			for(String linkId : linksIds) {
				links.add(APILocator.getMenuLinkAPI().findWorkingLinkById(linkId, systemUser, false));
			}
			
			for(Link link : links) {
				writeLink(bundleRoot, link);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
		
	}

	
	
	private void writeLink(File bundleRoot, Link link)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		
		Identifier linkId = APILocator.getIdentifierAPI().find(link.getIdentifier());
		link.setParent(linkId.getParentPath());
		Host h = APILocator.getHostAPI().find(linkId.getHostId(), systemUser, false);
		link.setHostId(linkId.getHostId());
		LinkWrapper wrapper = new LinkWrapper(linkId, link);
		
		wrapper.setVi(APILocator.getVersionableAPI().getVersionInfo(linkId.getId()));
		
		String liveworking = link.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI()
				.find(link).getURI().replace("/", File.separator);
		if(!uri.endsWith(LINK_EXTENSION)){
			uri.replace(LINK_EXTENSION, "");
			uri.trim();
			uri += LINK_EXTENSION;
		}
		
		
		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() + uri;

		File templateFile = new File(myFileUrl);
		templateFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, templateFile, true);
		templateFile.setLastModified(Calendar.getInstance().getTimeInMillis());
	}

	@Override
	public FileFilter getFileFilter(){
		return new LinkBundlerFilter();
	}
	
	public class LinkBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(LINK_EXTENSION));
		}

	}
}
