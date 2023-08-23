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

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.google.common.collect.ImmutableList;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.FieldVariableTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.ContentTypeWrapper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

public class ContentTypeBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;

	public final static String CONTENT_TYPE_EXTENSION = ".contentType.json" ;

	@Override
	public String getName() {
		return "ContentType bundler";
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

	@CloseDBIfOpened
	@Override
	public void generate(final BundleOutput output, final BundlerStatus status) throws DotBundleException {

	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

			throw new RuntimeException("need an enterprise pro license to run this bundler");
		}

		final Set<String> structures = config.getStructures();

		try {
			for (final String structureId : structures) {

				final Structure structure = CacheLocator.getContentTypeCache()
						.getStructureByInode(structureId);

				writeContentType(output, structure, config.getOperation());
			}
		} catch (Exception e) {

			Logger.error(this, e.getMessage(), e);
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
	}



	private void writeContentType(final BundleOutput bundleOutput,
								  final Structure structure,
								  final Operation operation)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException {

		final com.dotmarketing.portlets.structure.business.FieldAPI fieldAPI = APILocator.getFieldAPI();
		final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
		final ContentType contentType = new StructureTransformer(structure).from();
		final List<Field> fields      = new LegacyFieldTransformer(
				FieldsCache.getFieldsByStructureInode(structure.getInode())).asList();
		final List<FieldVariable> fieldVariables = new ArrayList<>();

        for(final Field field : fields) {

            fieldVariables.addAll(new FieldVariableTransformer(fieldAPI.getFieldVariablesForField(field.inode(),
					APILocator.systemUser(), false)).newFieldList());
        }

		final ContentTypeWrapper wrapper =
				new ContentTypeWrapper(contentType, fields, fieldVariables);
		
		wrapper.setOperation(operation);
		this.setWorkflowInformation(structure, contentType, workflowAPI, wrapper);

		final String liveWorking = structure.isLive()? "live" : "working";
		String uri = structure.getInode();

		if(!uri.endsWith(CONTENT_TYPE_EXTENSION)){
			uri.replace(CONTENT_TYPE_EXTENSION, StringPool.BLANK);
			uri.trim();
			uri += CONTENT_TYPE_EXTENSION;
		}

		final Host host        = APILocator.getHostAPI().find(structure.getHost(), systemUser, false);
		final String myFileUrl = File.separator
				+liveWorking + File.separator
				+ host.getHostname() +File.separator + uri;

		try (final OutputStream outputStream = bundleOutput.addFile(myFileUrl)) {
			BundlerUtil.objectToJSON(wrapper, outputStream);
		}

		bundleOutput.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Content Type bundled for pushing. Operation: "+config.getOperation()+", Id: "+ structure.getInode(), config.getId());
		}
	}

	private void setWorkflowInformation(final Structure structure,
										final ContentType contentType,
										final WorkflowAPI workflowAPI,
										final ContentTypeWrapper wrapper) throws DotDataException, DotSecurityException {

		final List<WorkflowScheme> schemes = workflowAPI.findSchemesForStruct(structure);
		final ImmutableList.Builder<String> schemeIds   = new ImmutableList.Builder<>();
		final ImmutableList.Builder<String> schemeNames = new ImmutableList.Builder<>();

		for(final WorkflowScheme scheme : schemes) {

			schemeIds.add(scheme.getId());
			schemeNames.add(scheme.getName());
		}

		wrapper.setWorkflowSchemaIds(schemeIds.build());
		wrapper.setWorkflowSchemaNames(schemeNames.build());

		wrapper.setSystemActionMappings(workflowAPI.findSystemActionsByContentType(
				contentType, APILocator.systemUser()));
	}

	@Override
	public FileFilter getFileFilter() {

		return new FolderBundlerFilter();
	}

	public class FolderBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(CONTENT_TYPE_EXTENSION));
		}

	}

	@Override
	public void setPublisher(IPublisher publisher) {
	}
}
