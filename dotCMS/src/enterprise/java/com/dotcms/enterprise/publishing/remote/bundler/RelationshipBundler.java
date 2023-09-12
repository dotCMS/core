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
