package com.dotcms.publisher.myTest.wrapper;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.templates.model.Template;

public class TemplateWrapper {
	private Identifier templateId;
	private Template template;
	
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
}
