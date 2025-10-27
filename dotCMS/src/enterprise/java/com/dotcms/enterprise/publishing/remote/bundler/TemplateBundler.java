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

    public final static String[] TEMPLATE_EXTENSION = {".template.xml", ".template.json"};

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
        Host h = APILocator.getHostAPI().find(templateId.getHostId(), systemUser, false);

        for (String extension : TEMPLATE_EXTENSION) {
            String uri = APILocator.getIdentifierAPI()
                    .find(template).getURI().replace("/", File.separator);
            if (!uri.endsWith(extension)) {
                uri.replace(extension, "");
                uri.trim();
                uri += extension;
            }

            String myFileUrl = File.separator
                    + liveworking + File.separator
                    + h.getHostname() + uri;

            try (final OutputStream outputStream = output.addFile(myFileUrl)) {

                BundlerUtil.writeObject(wrapper, outputStream, myFileUrl);
            }
            output.setLastModified(myFileUrl, Calendar.getInstance().getTimeInMillis());
        }


		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Template bundled for pushing. Operation: "+config.getOperation()+", Identifier: "+ template.getIdentifier(), config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
        return new ExtensionFileFilter(TEMPLATE_EXTENSION);
	}


}
