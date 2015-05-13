package com.dotmarketing.portlets.htmlpages.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;

//This interface should have default package access
public abstract class HTMLPageCache implements Cachable {

	abstract public IHTMLPage add(IHTMLPage htmlPage) throws DotStateException, DotDataException, DotSecurityException;

	abstract public IHTMLPage get(String key);

	abstract public void clearCache();

	abstract public void remove(IHTMLPage page);
	
	abstract public void remove(String pageIdentifier);
}