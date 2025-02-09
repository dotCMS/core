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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.FolderWrapper;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;

public class FolderBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;
	PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	FolderAPI fAPI = APILocator.getFolderAPI();

	public final static String FOLDER_EXTENSION = ".folder.xml" ;

	@Override
	public String getName() {
		return "Folder bundler";
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
			Logger.fatal(FolderBundler.class,e.getMessage(),e);
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

		Set<String> folders = config.getFolders();

		try {
			for (String folder : folders) {
				writeFolderTree(output, folder);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}



	private void writeFolderTree(BundleOutput bundleOutput, String idFolder)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		//Get Folder tree
		Folder folder = fAPI.find(idFolder, systemUser, false);
		String folderName = folder.getName();
		List<String> path = new ArrayList<>();
		List<FolderWrapper> folderWrappers = new ArrayList<>();
		while(folder != null && !folder.isSystemFolder()) {
			path.add(folder.getName());

			// include parent folders only when publishing
			if(config.getOperation().equals(PushPublisherConfig.Operation.PUBLISH) || folder.getName().equals(folderName)) {
				
				Host host =  APILocator.getHostAPI().find(folder.getHostId(), systemUser, false);
				folderWrappers.add(
						new FolderWrapper(folder,
								APILocator.getIdentifierAPI().find(folder.getIdentifier()),
								host,
								APILocator.getIdentifierAPI().find(host.getIdentifier()),config.getOperation()));
			}

			folder = fAPI.findParentFolder(folder, systemUser, false);
		}

		if(path.size() > 0) {
			Collections.reverse(path);
			Collections.reverse(folderWrappers);
			StringBuilder b = new StringBuilder(File.separator);
			for (String f : path) {
				b.append(f);
				b.append(File.separator);
				
				// exclude other folders but the one being pushed, when unpublishing
				if(config.getOperation().equals(PushPublisherConfig.Operation.UNPUBLISH) && !f.equals(folderName)) {
					continue;
				}

				String myFolderUrl = File.separator + "ROOT" +
						b.toString();

				File fsFolder = new File(myFolderUrl);
				bundleOutput.mkdirs(myFolderUrl);

				FolderWrapper wrapper = folderWrappers.remove(0);
				String myFileUrl = fsFolder.getParent()+ File.separator +
						wrapper.getFolder().getIdentifier()+FOLDER_EXTENSION;

				try (final OutputStream outputStream = bundleOutput.addFile(myFileUrl)) {
					BundlerUtil.objectToXML(wrapper, outputStream);
				}
			}
		}

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Folder bundled for pushing. Operation: "+config.getOperation()+", Id: "+ idFolder, config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new FolderBundlerFilter();
	}

	public class FolderBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(FOLDER_EXTENSION));
		}

	}

}
