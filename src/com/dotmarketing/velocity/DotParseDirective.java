package com.dotmarketing.velocity;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;

public class DotParseDirective extends Directive {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public String getName() {
		return "dotParse";
	}

	public int getType() {
		return LINE;
	}


	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException,
			ParseErrorException, MethodInvocationException {

		
		HttpServletRequest request = (HttpServletRequest) context.get("request");


		
		boolean EDIT_MODE = false;
		if (request != null) {
			EDIT_MODE = (request.getSession().getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null)
			        && request.getSession().getAttribute("tm_date")==null;
		}

		String templatePath = String.valueOf(node.jjtGetChild(0).value(context));


        Host host = null;
		User user = null;
        FileAsset asset = null;
		long lang = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
        try {
        	user = com.liferay.portal.util.PortalUtil.getUser(request);
        	
        	//if we have a host
	        if(templatePath.contains("//")){
	        	templatePath = templatePath.substring(2,templatePath.length());
	        	String hostName = templatePath.substring(0,templatePath.indexOf('/'));
	        	templatePath = templatePath.substring(templatePath.indexOf('/'), templatePath.length());
	        	host = APILocator.getHostAPI().findByName(hostName, user, !EDIT_MODE);
	        }
	        else{
	    		host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
	        }

	        Identifier id = APILocator.getIdentifierAPI().find(host, templatePath);
	        if("contentlet".equals(id.getAssetType())){
	        	Contentlet c = APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), !EDIT_MODE, lang, user, true);
	        	asset = APILocator.getFileAssetAPI().fromContentlet(c);
	        }
	        else{
		        if(EDIT_MODE){
		        	asset = (FileAsset) APILocator.getVersionableAPI().findWorkingVersion(id, user, true);
		        }
		        else{
		        	asset = (FileAsset) APILocator.getVersionableAPI().findLiveVersion(id, user, true);
		        }

	        }
	        templatePath = asset.getFileAsset().getAbsolutePath();

			VelocityEngine ve = VelocityUtil.getEngine();
			Template template = ve.getTemplate(templatePath);
			template.merge(context, writer);

	        return true;
	        
	        
		} catch (Exception e) {
			throw new ResourceNotFoundException(e);
		}
        


		

		

		
		
		
		
	}

}
