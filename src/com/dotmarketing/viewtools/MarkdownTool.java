package com.dotmarketing.viewtools;

import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.VelocityUtil;
import com.github.rjeschke.txtmark.Configuration;
import com.github.rjeschke.txtmark.Processor;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * Viewtool that can be used to parse text/content for github flavored markdown (gfm).
 * @author Will Ezell
 *
 */
public class MarkdownTool implements ViewTool {

	private HttpServletRequest request;
	private HttpServletResponse response;

	public void init(Object obj) {
		if (obj instanceof ViewContext) {
			this.request = ((ViewContext) obj).getRequest();
			this.response = ((ViewContext) obj).getResponse();
		}
	}
	/**
	 * Parse a String for markdown
	 * @param parse
	 * @return
	 * @throws Throwable
	 */
	public String parse(String parse) throws Throwable {
		return Processor.process(parse, Configuration.builder().forceExtentedProfile().build());
	}
	/**
	 * This method allows you to parse a markdown docuemnt 
	 * by Velocity
	 * 
	 * @param path
	 * @return
	 * @throws Throwable
	 */
	public String parseFile(String path) throws Throwable {
		return parse(getFileContents(path, false));

	}

	/**
	 * This method allows you to parse a markdown docuemnt that is first evaluated
	 * by Velocity
	 * 
	 * @param path
	 * @param parseFirst
	 * @return
	 * @throws Throwable
	 */
	public String parseFile(String path, boolean parseFirst) throws Throwable {
		return parse(getFileContents(path, parseFirst));
	}

	private String getFileContents(String path, boolean parseFirst) throws Throwable {
		boolean EDIT_MODE = false;
		if (request != null) {
			EDIT_MODE = (request.getSession().getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null)
					&& request.getSession().getAttribute("tm_date") == null;
		}

		String templatePath = path;

		Host host = null;
		User user = null;
		FileAsset asset = null;
		long lang = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();

		user = com.liferay.portal.util.PortalUtil.getUser(request);

		// if we have a host
		if (templatePath.contains("//")) {
			templatePath = templatePath.substring(2, templatePath.length());
			String hostName = templatePath.substring(0, templatePath.indexOf('/'));
			templatePath = templatePath.substring(templatePath.indexOf('/'), templatePath.length());
			host = APILocator.getHostAPI().findByName(hostName, user, !EDIT_MODE);
		} else {
			host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		}

		Identifier id = APILocator.getIdentifierAPI().find(host, templatePath);
		if ("contentlet".equals(id.getAssetType())) {
			Contentlet c = APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), !EDIT_MODE, lang, user, true);
			asset = APILocator.getFileAssetAPI().fromContentlet(c);
		}

		if (parseFirst) {
			Context ctx = null;
			if (request != null && response != null) {
				ctx = VelocityUtil.getWebContext(request, response);
			} else {
				ctx = VelocityUtil.getBasicContext();
			}

			templatePath = asset.getFileAsset().getAbsolutePath();
			VelocityEngine ve = VelocityUtil.getEngine();
			Template template = ve.getTemplate(templatePath);

			StringWriter writer = new StringWriter();
			template.merge(ctx, writer);
			return writer.toString();
		} else {
			return new String(FileUtil.getBytes(asset.getFileAsset().getAbsoluteFile()));
		}

	}
	
	
	
	
	
	
}
