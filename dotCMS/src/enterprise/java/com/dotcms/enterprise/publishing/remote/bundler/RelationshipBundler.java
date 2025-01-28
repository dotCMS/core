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
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.*;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.RelationshipWrapper;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PushPublishLogger;

public class RelationshipBundler implements IBundler {

	private PushPublisherConfig config;
	public final static String RELATIONSHIP_EXTENSION = ".relationship.xml" ;

	@Override
	public String getName() {
		return "Relationship Bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig)pc;

	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(final BundleOutput output, final BundlerStatus status) throws DotBundleException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		Set<String> relationships = config.getRelationships();

		try {
			for(String relInode : relationships){
				PushPublishLogger.log(getClass(), "Relationship INODE: " + relInode);
				Relationship rel = rel = APILocator.getRelationshipAPI().byInode(relInode);
				writeRelationship(output, rel, config.getOperation());
			}
		}catch(Exception e){
			status.addFailure();
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}


	private void writeRelationship(final BundleOutput bundleOutput, final Relationship relationship, final Operation op)
			throws IOException, DotBundleException, DotDataException, DotSecurityException, DotPublisherException {
		RelationshipWrapper wrapper = new RelationshipWrapper(relationship, op);
		String liveworking = relationship.isLive() ? "live" :  "working";

		String uri = relationship.getInode();
		if(!uri.endsWith(RELATIONSHIP_EXTENSION)){
			uri.replace(RELATIONSHIP_EXTENSION, "");
			uri.trim();
			uri += RELATIONSHIP_EXTENSION;
		}

		Structure parent = CacheLocator.getContentTypeCache().getStructureByInode(relationship.getParentStructureInode());

		Host h = APILocator.getHostAPI().find(parent.getHost(), APILocator.getUserAPI().getSystemUser(), false);

		String myFileUrl = File.separator
				+liveworking + File.separator
				+ h.getHostname() +File.separator + uri;

		try (final OutputStream outputStream = bundleOutput.addFile(myFileUrl)) {

			BundlerUtil.objectToXML(wrapper, outputStream);
		}

		bundleOutput.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Relationship bundled for pushing. Operation: "+config.getOperation()+", Id: "+ relationship.getInode(), config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new RelationshipBundlerFilter();
	}

	public class RelationshipBundlerFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return (pathname.isDirectory() || pathname.getName().endsWith(RELATIONSHIP_EXTENSION));
		}

	}

}
