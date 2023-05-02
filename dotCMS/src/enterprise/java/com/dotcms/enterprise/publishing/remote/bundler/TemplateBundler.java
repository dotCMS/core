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
import java.util.HashSet;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.TemplateWrapper;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;

public class TemplateBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;

	public final static String TEMPLATE_EXTENSION = ".template.xml" ;

	@Override
	public String getName() {
		return "Template bundler";
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

		//Get containers linked with the content
		Set<String> templateIds = config.getTemplates();

		try {
			Set<Template> templates = new HashSet<>();

			for(String templateId : templateIds) {
			    Template working = APILocator.getTemplateAPI()
                        .findWorkingTemplate(templateId, systemUser, false);
			    templates.add(working);

			    Template live = APILocator.getTemplateAPI()
                        .findLiveTemplate(templateId, systemUser, false);
			    if(live!=null && InodeUtils.isSet(live.getInode())
			            && !live.getInode().equals(working.getInode())) {
			        // add only if the live version exists and is different to the working
			        templates.add(live);
			    }
			}

			for(Template template : templates) {
				writeTemplate(output, template);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}



	private void writeTemplate(final BundleOutput output, final Template template)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{

		Identifier templateId = APILocator.getIdentifierAPI().find(template.getIdentifier());
		TemplateWrapper wrapper = new TemplateWrapper(templateId, template);
		wrapper.setOperation(config.getOperation());
		wrapper.setVi(APILocator.getVersionableAPI().getVersionInfo(templateId.getId()));

		String liveworking = template.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI()
				.find(template).getURI().replace("/", File.separator);
		if(!uri.endsWith(TEMPLATE_EXTENSION)){
			uri.replace(TEMPLATE_EXTENSION, "");
			uri.trim();
			uri += TEMPLATE_EXTENSION;
		}

		Host h = APILocator.getHostAPI().find(templateId.getHostId(), systemUser, false);

		String myFileUrl = File.separator
				+liveworking + File.separator
				+ h.getHostname() + uri;

		try (final OutputStream outputStream = output.addFile(myFileUrl)) {

			BundlerUtil.objectToXML(wrapper, outputStream);
		}

		output.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Template bundled for pushing. Operation: "+config.getOperation()+", Identifier: "+ template.getIdentifier(), config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new ContainerBundlerFilter();
	}

	public class ContainerBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(TEMPLATE_EXTENSION));
		}

	}
}
