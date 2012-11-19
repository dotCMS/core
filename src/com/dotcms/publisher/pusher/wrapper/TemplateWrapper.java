package com.dotcms.publisher.pusher.wrapper;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.templates.model.Template;

public class TemplateWrapper {
	private Identifier templateId;
	private Template template;
	private VersionInfo vi;
	
	public TemplateWrapper(Identifier templateId, Template template) {
		this.templateId = templateId;
		this.template = template;
	}

	public Identifier getTemplateId() {
		return templateId;
	}

	public void setTemplateId(Identifier templateId) {
		this.templateId = templateId;
	}

	public Template getTemplate() {
		return template;
	}

	public void setTemplate(Template template) {
		this.template = template;
	}

	/**
	 * @return the vi
	 */
	public VersionInfo getVi() {
		return vi;
	}

	/**
	 * @param vi the vi to set
	 */
	public void setVi(VersionInfo vi) {
		this.vi = vi;
	}
}
