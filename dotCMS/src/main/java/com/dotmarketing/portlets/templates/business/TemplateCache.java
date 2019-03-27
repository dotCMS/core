package com.dotmarketing.portlets.templates.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.templates.model.Template;

//This interface should have default package access
public abstract class TemplateCache implements Cachable {

	abstract protected Template add(String key, Template template);

	abstract protected Template get(String key);

	abstract public void clearCache();

	abstract public void remove(String key);
}