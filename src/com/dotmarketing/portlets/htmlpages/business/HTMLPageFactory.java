package com.dotmarketing.portlets.htmlpages.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.liferay.portal.model.User;

public interface HTMLPageFactory {
	
	public void save(HTMLPage htmlPage) throws DotDataException, DotStateException, DotSecurityException;
	
	public HTMLPage getLiveHTMLPageByPath(String path, Host host) throws DotDataException, DotSecurityException;
	
	public HTMLPage getLiveHTMLPageByPath(String path, String hostId) throws DotDataException, DotSecurityException;

	public int findNumOfContent(HTMLPage page, Container container);

	public Folder getParentFolder(HTMLPage object) throws DotIdentifierStateException, DotDataException, DotSecurityException;

	public Host getParentHost(HTMLPage object) throws DotIdentifierStateException, DotDataException, DotSecurityException;

	public HTMLPage loadWorkingPageById(String pageId) throws DotDataException;
	
	public HTMLPage loadLivePageById(String pageId) throws DotDataException, DotStateException, DotSecurityException;
	
	public List<HTMLPage> findHtmlPages(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;

	public boolean movePage(HTMLPage page, Folder parent)throws DotStateException, DotDataException, DotSecurityException;
	
}