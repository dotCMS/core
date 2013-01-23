package com.dotcms.publisher.pusher.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.TemplateWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
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
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<400)
	        throw new RuntimeException("need an enterprise prime license to run this bundler");
		
		//Get containers linked with the content
		Set<String> templateIds = config.getTemplates();
		
		try {
			Set<Template> templates = new HashSet<Template>();
			
			for(String templateId : templateIds) {
				templates.add(APILocator.getTemplateAPI()
						.findLiveTemplate(templateId, systemUser, false));
				templates.add(APILocator.getTemplateAPI()
						.findWorkingTemplate(templateId, systemUser, false));
			}
			
			for(Template template : templates) {
				writeTemplate(bundleRoot, template);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
		
	}

	
	
	private void writeTemplate(File bundleRoot, Template template)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		
		Identifier templateId = APILocator.getIdentifierAPI().find(template.getIdentifier());
		TemplateWrapper wrapper = 
				new TemplateWrapper(templateId, template);
		
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
		
		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() + uri;

		File templateFile = new File(myFileUrl);
		templateFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, templateFile, true);
		templateFile.setLastModified(Calendar.getInstance().getTimeInMillis());
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
