package com.dotmarketing.portlets.htmlpages.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;

//This interface should have default package access
public abstract class HTMLPageCache implements Cachable {

	abstract protected HTMLPage add(HTMLPage htmlPage) throws DotStateException, DotDataException, DotSecurityException;

	abstract protected HTMLPage get(String key);

	abstract public void clearCache();

	abstract public void remove(HTMLPage page);
	
	abstract public void remove(String pageIdentifier);
}