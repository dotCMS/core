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
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
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
	public void generate(final BundleOutput output, final BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		Set<String> linksIds = config.getLinks();

		try {
			Set<Link> links = new HashSet<>();

			for(String linkId : linksIds) {
				links.add(APILocator.getMenuLinkAPI().findWorkingLinkById(linkId, systemUser, false));
			}

			for(Link link : links) {
				writeLink(output, link);

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



	private void writeLink(final BundleOutput bundleOutput, final Link link)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		List<Link> links = new ArrayList<>();
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


		String myFileUrl = File.separator
				+liveworking + File.separator
				+ h.getHostname() + uri;

		try(final OutputStream outputStream = bundleOutput.addFile(myFileUrl)) {

			BundlerUtil.objectToXML(wrapper, outputStream);
		}

		bundleOutput.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());
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
