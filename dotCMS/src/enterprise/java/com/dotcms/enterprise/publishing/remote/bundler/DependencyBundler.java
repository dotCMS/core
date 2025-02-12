/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.publisher.util.dependencies.DependencyManager;
import java.io.File;
import java.io.FileFilter;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class DependencyBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;
	PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	FolderAPI fAPI = APILocator.getFolderAPI();

	public final static String CONTENT_EXTENSION = ".content.xml" ;

	@Override
	public String getName() {
		return "Dependency bundler";
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
			Logger.fatal(DependencyBundler.class,e.getMessage(),e);
		}
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(BundleOutput output, BundlerStatus status)
			throws DotBundleException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		try {
			DependencyManager dp = new DependencyManager(systemUser, config);
			dp.setDependencies();

		} catch (Exception e) {
			throw new DotBundleException(e.getMessage(),e);
		}
	}


	@Override
	public FileFilter getFileFilter(){
		return new ContentBundlerFilter();
	}

	public class ContentBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(CONTENT_EXTENSION));
		}

	}

}
