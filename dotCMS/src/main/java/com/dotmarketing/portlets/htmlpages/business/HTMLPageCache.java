package com.dotmarketing.portlets.htmlpages.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;

// This interface should have default package access
public abstract class HTMLPageCache implements Cachable {

  public abstract IHTMLPage add(IHTMLPage htmlPage)
      throws DotStateException, DotDataException, DotSecurityException;

  public abstract IHTMLPage get(String key);

  public abstract void clearCache();

  public abstract void remove(IHTMLPage page);

  public abstract void remove(String pageIdentifier);
}
