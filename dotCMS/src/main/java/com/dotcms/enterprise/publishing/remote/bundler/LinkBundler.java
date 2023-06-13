package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.LinkWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
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
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		Set<String> linksIds = config.getLinks();

		try {
			Set<Link> links = new HashSet<Link>();

			for(String linkId : linksIds) {
				links.add(APILocator.getMenuLinkAPI().findWorkingLinkById(linkId, systemUser, false));
			}

			for(Link link : links) {
				writeLink(bundleRoot, link);

				if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
        			PushPublishLogger.log(getClass(), "Link bundled for pushing. Operation : "+config.getOperation()+", Identifier: "+ link.getIdentifier(), config.getId());
        		}
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
		List<Link> links = new ArrayList<Link>();
		Identifier linkId = APILocator.getIdentifierAPI().find(link.getIdentifier());
		Host h = APILocator.getHostAPI().find(linkId.getHostId(), systemUser, false);
		VersionInfo info =APILocator.getVersionableAPI().getVersionInfo(linkId.getId());

		Link workingLink = APILocator.getMenuLinkAPI().find(info.getWorkingInode() , APILocator.getUserAPI().getSystemUser(), false);
		workingLink.setParent(linkId.getParentPath());
		workingLink.setHostId(linkId.getHostId());
		links.add(workingLink);
		if(info.getLiveInode() != null && ! info.getLiveInode().equals(info.getWorkingInode())){
			Link liveLink = APILocator.getMenuLinkAPI().find(info.getLiveInode() , APILocator.getUserAPI().getSystemUser(), false);
			liveLink.setHostId(linkId.getHostId());
			liveLink.setParent(linkId.getParentPath());
			liveLink.setIdentifier(linkId.getId());
			links.add(0,liveLink);
		}

		LinkWrapper wrapper = new LinkWrapper(linkId, links);

		wrapper.setVi(info);
		wrapper.setOperation(config.getOperation());

		String liveworking = workingLink.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI()
				.find(workingLink).getURI().replace("/", File.separator);
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
