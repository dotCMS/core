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

import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.google.common.collect.ImmutableList;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.StructureWrapper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;

public class StructureBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;

	public final static String STRUCTURE_EXTENSION = ".structure.xml" ;

	@Override
	public String getName() {
		return "Structure bundler";
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
	public void generate(final BundleOutput bundleOutput, final BundlerStatus status) throws DotBundleException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		Set<String> structures = config.getStructures();

		try {
			for (String str : structures) {
				Structure s = CacheLocator.getContentTypeCache().getStructureByInode(str);

				writeStructure(bundleOutput, s, config.getOperation());
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}



	private void writeStructure(final BundleOutput bundleRoot, final Structure structure, final Operation op)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
        
		List<FieldVariable> fieldVariables=new ArrayList<>();
        for(Field ff : fields) {
            fieldVariables.addAll(
                    APILocator.getFieldAPI().getFieldVariablesForField(ff.getInode(), APILocator.getUserAPI().getSystemUser(), false));
        }
		
		StructureWrapper wrapper = new StructureWrapper(structure,fields,fieldVariables);
		
		wrapper.setOperation(op);
		List<WorkflowScheme> schemes = APILocator.getWorkflowAPI().findSchemesForStruct(structure);
		final ImmutableList.Builder<String> schemeIds = new ImmutableList.Builder<>();
		final ImmutableList.Builder<String> schemeNames = new ImmutableList.Builder<>();
		for(WorkflowScheme scheme : schemes){
			schemeIds.add(scheme.getId());
			schemeNames.add(scheme.getName());
		}
		wrapper.setWorkflowSchemaIds(schemeIds.build());
		wrapper.setWorkflowSchemaNames(schemeNames.build());
		
		String liveworking = structure.isLive() ? "live" :  "working";

		String uri = structure.getInode();
		if(!uri.endsWith(STRUCTURE_EXTENSION)){
			uri.replace(STRUCTURE_EXTENSION, "");
			uri.trim();
			uri += STRUCTURE_EXTENSION;
		}

		Host h = APILocator.getHostAPI().find(structure.getHost(), systemUser, false);

		String myFileUrl = File.separator
				+liveworking + File.separator
				+ h.getHostname() +File.separator + uri;

		try(final OutputStream outputStream = bundleRoot.addFile(myFileUrl)) {

			BundlerUtil.objectToXML(wrapper, outputStream);
		}

		bundleRoot.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Structure bundled for pushing. Operation: "+config.getOperation()+", Id: "+ structure.getInode(), config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new FolderBundlerFilter();
	}

	public class FolderBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(STRUCTURE_EXTENSION));
		}

	}
}
