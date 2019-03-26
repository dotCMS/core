package com.dotmarketing.portlets.templates.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.templates.model.Template;

// This interface should have default package access
public abstract class TemplateCache implements Cachable {

  protected abstract Template add(String key, Template template);

  protected abstract Template get(String key);

  public abstract void clearCache();

  public abstract void remove(String key);
}
