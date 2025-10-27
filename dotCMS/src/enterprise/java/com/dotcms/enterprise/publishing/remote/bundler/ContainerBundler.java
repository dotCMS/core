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
import java.util.HashSet;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.ContainerWrapper;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;

public class ContainerBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;

	PublisherAPI pubAPI = null;

	public final static String CONTAINER_EXTENSION = ".container.xml" ;
    public final static String CONTAINER_EXTENSION_JSON = ".container.json";

	@Override
	public String getName() {
		return "Container bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig) pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		pubAPI = PublisherAPI.getInstance();
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

		//Get containers linked with the content
		Set<String> containerIds = config.getContainers();

		try {
			Set<Container> containers = new HashSet<>();

			for (String containerId : containerIds) {
				Container working = APILocator.getContainerAPI().
                        getWorkingContainerById(containerId, systemUser, false);
				Container live = APILocator.getContainerAPI().
						getLiveContainerById(containerId, systemUser, false);

			    containers.add(working);

				if(live!=null && InodeUtils.isSet(live.getInode())
				        && !live.getInode().equals(working.getInode())) {
				    // add the live if exists and is different than the working
				    containers.add(live);
				}
			}

			for(Container container : containers) {
				writeContainer(output, container);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}



	private void writeContainer(final BundleOutput bundleOutput, final Container container)
			throws DotDataException, DotSecurityException, IOException {
		Identifier containerId = APILocator.getIdentifierAPI().find(container.getIdentifier());
		ContainerWrapper wrapper =
				new ContainerWrapper(containerId, container);
		wrapper.setOperation(config.getOperation());
		wrapper.setCvi(APILocator.getVersionableAPI().getVersionInfo(containerId.getId()));

		wrapper.setCsList(APILocator.getContainerAPI().getContainerStructures(container));
		
		String liveworking = container.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI()
				.find(container).getURI().replace("/", File.separator);
		if(!uri.endsWith(CONTAINER_EXTENSION)){
			uri.replace(CONTAINER_EXTENSION, "");
			uri.trim();
			uri += CONTAINER_EXTENSION;
		}

		Host h = APILocator.getHostAPI().find(containerId.getHostId(), systemUser, false);

		String myFileUrl = File.separator
				+liveworking + File.separator
				+ h.getHostname() + uri;

		try(final OutputStream outputStream = bundleOutput.addFile(myFileUrl)) {
            BundlerUtil.writeObject(wrapper, outputStream, myFileUrl);
			bundleOutput.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());
		}

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Container bundled for pushing. Operation: "+config.getOperation()+", Identifier: "+ container.getIdentifier(), config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
        return new ExtensionFileFilter(CONTAINER_EXTENSION, CONTAINER_EXTENSION_JSON);

    }



}
