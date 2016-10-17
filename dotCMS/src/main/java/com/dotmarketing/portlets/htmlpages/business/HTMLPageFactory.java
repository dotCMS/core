package com.dotmarketing.portlets.htmlpages.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.liferay.portal.model.User;

@Deprecated
public interface HTMLPageFactory {
	
	public void save(HTMLPage htmlPage) throws DotDataException, DotStateException, DotSecurityException;
	public void save(HTMLPage htmlPage, String existingInode)throws DotDataException, DotStateException, DotSecurityException;
	
	public HTMLPage getLiveHTMLPageByPath(String path, Host host) throws DotDataException, DotSecurityException;
	
	public HTMLPage getLiveHTMLPageByPath(String path, String hostId) throws DotDataException, DotSecurityException;

	public int findNumOfContent(HTMLPage page, Container container);

	public Folder getParentFolder(HTMLPage object) throws DotIdentifierStateException, DotDataException, DotSecurityException;

	public Host getParentHost(IHTMLPage object) throws DotIdentifierStateException, DotDataException, DotSecurityException;

	public HTMLPage loadWorkingPageById(String pageId) throws DotDataException;
	
	public HTMLPage loadLivePageById(String pageId) throws DotDataException, DotStateException, DotSecurityException;
	
	public List<HTMLPage> findHtmlPages(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;
	
	/**
	 * Retrieves the list of HTML pages based on the specified criteria.
	 * <p>
	 * As of version 3.1, HTML pages will be represented as content. Therefore,
	 * the returned list will be composed of legacy pages ({@link HTMLPage}) and
	 * new content pages ({@link IHTMLPage}) in order to provide compatibility
	 * with existing pages.
	 * </p>
	 * 
	 * @param user
	 *            - The user in session.
	 * @param includeArchived
	 *            - If <code>true</code>, archived pages will be included in the
	 *            search. Otherwise, <code>false</code>.
	 * @param params
	 *            - A {@link Map} with additional filtering parameters.
	 * @param hostId
	 *            - The current host ID.
	 * @param inode
	 *            - The inode to search. If all results are to be included, set
	 *            to <code>null</code>.
	 * @param identifier
	 *            - The identifier to search. If all results are to be included,
	 *            set to <code>null</code>.
	 * @param parent
	 *            - If specified, represents the parent folder containing the
	 *            requested pages.
	 * @param offset
	 *            - For pagination purposes. Specifies the offset from which
	 *            data will be included in the result.
	 * @param limit
	 *            - The maximum number of results to return.
	 * @param orderBy
	 *            - The criterion used to order the collection.
	 * @return A {@link PaginatedArrayList} containing the consolidated pages
	 *         referenced with the new {@link IHTMLPage} class.
	 * @throws DotSecurityException
	 *             The current user does not have permissions to perform the
	 *             requested operation.
	 * @throws DotDataException
	 *             The page information could not be retrieved.
	 */
	public List<IHTMLPage> findIHtmlPages(User user, boolean includeArchived,
			Map<String, Object> params, String hostId, String inode,
			String identifier, String parent, int offset, int limit,
			String orderBy) throws DotSecurityException, DotDataException;

	public boolean movePage(HTMLPage page, Folder parent)throws DotStateException, DotDataException, DotSecurityException;
	
    public List<String> findUpdatedHTMLPageIdsByURI(Host host, String pattern,boolean include,Date startDate, Date endDate);
	
    /**
	 * Method will replace user references of the given userId in htmlpages 
	 * with the replacement user id  
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	public void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException;
}
