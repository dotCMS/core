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
    public final static String[] RELATIONSHIP_EXTENSIONS = {".relationship.xml", ".relationship.json"};

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
        Structure parent = CacheLocator.getContentTypeCache()
                .getStructureByInode(relationship.getParentStructureInode());

        Host h = APILocator.getHostAPI().find(parent.getHost(), APILocator.getUserAPI().getSystemUser(), false);
        for (String extension : RELATIONSHIP_EXTENSIONS) {
            String uri = relationship.getInode();
            if (!uri.endsWith(extension)) {
                uri.replace(extension, "");
                uri.trim();
                uri += extension;
            }

            String myFileUrl = File.separator
                    + liveworking + File.separator
                    + h.getHostname() + File.separator + uri;

            try (final OutputStream outputStream = bundleOutput.addFile(myFileUrl)) {

                BundlerUtil.writeObject(wrapper, outputStream, myFileUrl);
            }

            bundleOutput.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());
        }
		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Relationship bundled for pushing. Operation: "+config.getOperation()+", Id: "+ relationship.getInode(), config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
        return new ExtensionFileFilter(RELATIONSHIP_EXTENSIONS);
	}



}
