package com.dotcms.datagen;

import java.util.Date;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;

/**
 * Class used to create {@link Template} objects for test purposes
 * 
 * @author Nollymar Longa
 */
public class TemplateDataGen extends AbstractDataGen<Template> {
	private String body;
	private String footer;
	private String header;
	private String friendlyName;
	private String image;
	private String selectedImage;
	private String title;

	private static final TemplateAPI templateAPI = APILocator.getTemplateAPI();
	private static final String type = "template";

	/**
	 * Creates a new {@link Template} instance kept in memory (not persisted)
	 * 
	 * @return Template instance created
	 */
	@Override
	public Template next() {
		// Create the new template
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

	/**
	 * Creates a new {@link Template} instance and persists it in DB
	 * 
	 * @return A new Template instance persisted in DB
	 */
	@Override
	public Template nextPersisted() {
		return persist(next());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dotcms.datagen.DataGen#persist(java.lang.Object)
	 */
	@Override
	public Template persist(Template template) {
		try {
			return templateAPI.saveTemplate(template, defaultHost, user, false);
		} catch (DotDataException | DotSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Deletes a given {@link Template} instance
	 * 
	 * @param template
	 *            to be removed
	 */
	@Override
	public void remove(Template template) {
		try {
			templateAPI.delete(template, user, false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets body property to the TemplateDataGen instance. This will be used
	 * when a new {@link Template} instance is created
	 * 
	 * @param body
	 * @return TemplateDataGen with body property set
	 */
	public TemplateDataGen body(String body) {
		this.body = body;
		return this;
	}

	/**
	 * Sets footer property to the TemplateDataGen instance. This will be used
	 * when a new {@link Template} instance is created
	 * 
	 * @param footer
	 * @return TemplateDataGen with footer property set
	 */
	public TemplateDataGen footer(String footer) {
		this.footer = footer;
		return this;
	}

	/**
	 * Sets friendlyName property to the TemplateDataGen instance. This will be
	 * used when a new {@link Template} instance is created
	 * 
	 * @param friendlyName
	 * @return TemplateDataGen with friendlyName property set
	 */
	public TemplateDataGen friendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
		return this;
	}

	/**
	 * Sets header property to the TemplateDataGen instance. This will be used
	 * when a new {@link Template} instance is created
	 * 
	 * @param header
	 * @return TemplateDataGen with header property set
	 */
	public TemplateDataGen header(String header) {
		this.header = header;
		return this;
	}

	/**
	 * Sets image property to the TemplateDataGen instance. This will be used
	 * when a new {@link Template} instance is created
	 * 
	 * @param image
	 * @return TemplateDataGen with image property set
	 */
	public TemplateDataGen image(String image) {
		this.image = image;
		return this;
	}

	/**
	 * Sets selectedImage property to the TemplateDataGen instance. This will be
	 * used when a new {@link Template} instance is created
	 * 
	 * @param selectedImage
	 * @return TemplateDataGen with selectedImage property set
	 */
	public TemplateDataGen selectedImage(String selectedImage) {
		this.selectedImage = selectedImage;
		return this;
	}

	/**
	 * Sets title property to the TemplateDataGen instance. This will be used
	 * when a new {@link Template} instance is created
	 * 
	 * @param title
	 * @return TemplateDataGen with title property set
	 */
	public TemplateDataGen title(String title) {
		this.title = title;
		return this;
	}

}
