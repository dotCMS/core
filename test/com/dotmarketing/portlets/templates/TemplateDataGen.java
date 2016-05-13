package com.dotmarketing.portlets.templates;

import java.util.Date;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public class TemplateDataGen {
	private String body;
	private String footer;
	private String header;
	private String friendlyName;
	private String image;
	private String selectedImage;
	private String title;
	
	
	private static final HostAPI hostAPI = APILocator.getHostAPI();
	private static final TemplateAPI templateAPI = APILocator.getTemplateAPI();
	private static final String type = "template";
	private static final Host defaultHost;
	private static final User user;
	
	static {
        try {
            user = APILocator.getUserAPI().getSystemUser();
            defaultHost = hostAPI.findDefaultHost(user, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	public Template next(){
		//Create the new template
        Template template = new Template();
        template.setBody(this.body);
        template.setFooter(this.footer);
        template.setFriendlyName(this.friendlyName);
        template.setHeader(this.header);
        template.setIDate(new Date());
        template.setImage(this.image);
        template.setModDate(new Date());
        template.setModUser(user.getUserId());
        template.setOwner(user.getUserId());
        template.setSelectedimage(this.selectedImage);
        template.setShowOnMenu(true);
        template.setSortOrder(2);
        template.setTitle(this.title);
        template.setType(type);
        
        return template;
	}
	
	public Template nextPersisted() throws DotDataException, DotSecurityException{
		Template template = next();
		templateAPI.saveTemplate( template, defaultHost, user, false);
		return template;
	}
	
	public void remove(Template template) throws DotSecurityException, Exception{
		templateAPI.delete(template, user, false);
	}
	
	public TemplateDataGen body(String body){
		this.body = body;
		return this;
	}
	
	public TemplateDataGen footer(String footer){
		this.footer = footer;
		return this;
	}
	
	public TemplateDataGen friendlyName(String friendlyName){
		this.friendlyName = friendlyName;
		return this;
	}
	
	public TemplateDataGen header(String header){
		this.header = header;
		return this;
	}
	
	public TemplateDataGen image(String image){
		this.image = image;
		return this;
	}
	
	public TemplateDataGen selectedImage(String selectedImage){
		this.selectedImage = selectedImage;
		return this;
	}
	
	public TemplateDataGen title(String title){
		this.title = title;
		return this;
	}
	
}
