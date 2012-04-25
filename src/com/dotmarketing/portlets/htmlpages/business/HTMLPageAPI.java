package com.dotmarketing.portlets.htmlpages.business;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public interface HTMLPageAPI {
	
	public enum CopyMode {
		BLANK_HTMLPAGE,			// The new html page will be created without the contents
		USE_SOURCE_CONTENT,	// The new html page will be created with the same contents used in the source html page
		COPY_SOURCE_CONTENT;	// Will copy each content used in the source html page and add them to the new html page 
	}
	
	public static class TemplateContainersReMap {
		
		public static class ContainerRemapTuple {
			
			private Container sourceContainer;
			private Container destinationContainer;

			public ContainerRemapTuple(Container sourceContainer, Container destinationContainer) {
				super();
				this.sourceContainer = sourceContainer;
				this.destinationContainer = destinationContainer;
			}
			public Container getSourceContainer() {
				return sourceContainer;
			}
			public void setSourceContainer(Container oldContainer) {
				this.sourceContainer = oldContainer;
			}
			public Container getDestinationContainer() {
				return destinationContainer;
			}
			public void setDestinationContainer(Container newContainer) {
				this.destinationContainer = newContainer;
			}
			
		}
		
		private Template sourceTemplate;
		private Template destinationTemplate;
		private List<ContainerRemapTuple> containersRemap;

		public TemplateContainersReMap(Template sourceTemplate, Template destinationTemplate,
				List<ContainerRemapTuple> containersRemap) {
			super();
			this.sourceTemplate = sourceTemplate;
			this.destinationTemplate = destinationTemplate;
			this.containersRemap = containersRemap;
		}
		public Template getSourceTemplate() {
			return sourceTemplate;
		}
		public void setSourceTemplate(Template oldTemplate) {
			this.sourceTemplate = oldTemplate;
		}
		public Template getDestinationTemplate() {
			return destinationTemplate;
		}
		public void setDestinationTemplate(Template newTemplate) {
			this.destinationTemplate = newTemplate;
		}
		public List<ContainerRemapTuple> getContainersRemap() {
			return containersRemap;
		}
		public void setContainersRemap(List<ContainerRemapTuple> containersReMap) {
			this.containersRemap = containersReMap;
		}
		
	}
	
	/**
	 * This an HTML copy method used for advanced copy procedures it gives you the ability to Re-map copied HTML page to a new Template and Containers and
	 * copies/moves source content to new assigned Template within it corrects Containers.
	 * 
	 * This methods copies an HTML page to a specified folder. Copying all source properties and permissions. 
	 * 
	 * Also gives you the ability to force overwrite if the page already exists at the destination. 
	 * 
	 * You can also instruct it how the source content will be treated, gives you the options to create a blank page; no content will be associated to it
	 * or to link to same source content; which will reference same content as in the source or at last to copy the content as well; which will create
	 * copies of your source page content and attach them to the new page, if the new content requires a host or folder (dictated by its structure) then
	 * the copy will try to keep the same paths within the destination host.
	 * 
	 * When assigning content to the new page, you are responsible on providing a template/container re-mapping object that guides the copy on what new 
	 * template will be used for the new page and host the content will be mapped to from the old containers of the source to the new containers of the
	 * re-mapping. It is your responsibility to ensure the source and destination templates and containers exists and are properly wired, this method
	 * will not validate the given templates and container re-mapping instructions.   
	 * 
	 * @param htmlPage
	 * @param destination
	 * @param forceOverwrite
	 * @param copyTemplateContainers
	 * @param copyMode
	 * @param user
	 * @param respectFrontendRoles
	 * @return HTMLPage
	 * @exception DotDataException
	 * @exception DotSecurityException
	 */
	public HTMLPage copy(HTMLPage source, Folder destination, boolean forceOverwrite, HTMLPageAPI.CopyMode copyMode, TemplateContainersReMap reMapping,
			User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Copies an HTML page to a specified folder. Copies all source properties and permissions. Also gives you the ability to force overwrite if
	 * the page already exists at the destination. And you can also instruct it to copy the source page Template and Containers creating a brand 
	 * new set of template and containers at the host of the destination folder an it will use those new templates for the new page.
	 * You can also instruct it how the source content will be treated, gives you the options to create a blank page; no content will be associated to it
	 * or to link to same source content; which will reference same content as in the source or at last to copy the content as well; which will create
	 * copies of your source page content and attach them to the new page, if the new content requires a host or folder (dictated by its structure) then
	 * the copy will try to keep the same paths within the destination host. 
	 * 
	 * @param htmlPage
	 * @param destination
	 * @param forceOverwrite
	 * @param copyTemplateContainers
	 * @param copyMode
	 * @param user
	 * @param respectFrontendRoles
	 * @return HTMLPage
	 * @exception DotDataException
	 * @exception DotSecurityException
	 */
	public HTMLPage copy(HTMLPage source, Folder destination, boolean forceOverwrite, boolean copyTemplateContainers, HTMLPageAPI.CopyMode copyMode, 
			User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * Copies the source HTMLPage to a given folder. Currently this method will copy all source properties as well as permissions but will not 
	 * bring the content associated with the source HTMLPage and it will reuse the same template that the source uses. This method doesn't overwrite
	 * pages at the destination, so if a page with the same name already exist at the destination, it will reneme the copy.
	 * 
	 * @param folderToCopyTo
	 * @return
	 * @throws DotDataException 
	 */
	public HTMLPage copy(HTMLPage source, Folder folderToCopyTo, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Return the working HTML Page with the specified HTML Page URL
	 * 
	 * @param htmlPageURL
	 * @param folder
	 * @return HTMLPage
	 * @throws DotDataException 
	 * @throws DotStateException 
	 * @throws DotSecurityException 
	 */
	public HTMLPage getWorkingHTMLPageByPageURL(String htmlPageURL, Folder folder) throws DotStateException, DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param path
	 * @param host
	 * @return HTMLPage from a path on a given host 
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public HTMLPage loadPageByPath(String path, Host host) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param path
	 * @param host
	 * @return HTMLPage from a path on a given hostId
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */ 
	public HTMLPage loadPageByPath(String path, String hostId) throws DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param page
	 * @param container
	 * @return true/false on whether or not a Page has content with a specificed container
	 */
	public boolean hasContent(HTMLPage page, Container container);
	
	/**
	 * Use to method to get the template for the HTMLPage set on the API.  This method will hit the database.
	 * @return Template for the working version of a HTMLPage
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public Template getTemplateForWorkingHTMLPage(HTMLPage htmlpage) throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * 
	 * @param folder to get HTMLPages for
	 * @return a List of all live HTMLPages
	 * @throws DotDataException 
	 * @throws DotStateException 
	 * @throws DotSecurityException 
	 */
	public List<HTMLPage> findLiveHTMLPages(Folder folder) throws DotStateException, DotDataException, DotSecurityException;
	
	/**
	 * 
	 * @param folder to get HTMLPages for
	 * @return a List of all live HTMLPages
	 * @throws DotDataException 
	 * @throws DotStateException 
	 * @throws DotSecurityException 
	 */
	public List<HTMLPage> findWorkingHTMLPages(Folder folder) throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * Retrieves the parent folder of the given page
	 * @param object
	 * @return
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotIdentifierStateException 
	 */
	public  Folder getParentFolder(HTMLPage object) throws DotIdentifierStateException, DotDataException, DotSecurityException;
	
	/**
	 * Retrieves the parent host of the given page
	 * @param object
	 * @return
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotIdentifierStateException 
	 */
	public  Host getParentHost(HTMLPage object) throws DotIdentifierStateException, DotDataException, DotSecurityException;
	
	/**
	 * Save the HTML page.
	 * 
	 * @param newHtmlPage
	 * @param template
	 * @param parentFolder
	 * @param user
	 * @param respectFrontendRoles
	 * @return HTMLPage
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public HTMLPage saveHTMLPage(HTMLPage newHtmlPage, Template template, Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * delete the selected HTML page
	 * 
	 * @param htmlPage
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws Exception
	 */
	public boolean delete(HTMLPage htmlPage, User user, boolean respectFrontendRoles) throws DotSecurityException, Exception;
	
	/**
	 * Will search the DB.  Use the SQLQueryBuilderFactory or GenericQueryBuiilderFactory to build
	 * your query object
	 * @param query
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException 
	 * @throws ValidationException 
	 * @throws ValidationException
	 * @throws DotDataException
	 */
	public List<Map<String, Serializable>> DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException, DotDataException;

	//http://jira.dotmarketing.net/browse/DOTCMS-3392
	/**
	 * Will parse the HTMLPage and return HTMLPage rendering as a String.
	 *  
	 * @param htmlPage
	 * @return String
	 * @throws DotDataException 
	 * @throws DotStateException 
	 * @throws DotSecurityException 
	 */	
	public String getHTML(HTMLPage htmlPage) throws DotStateException, DotDataException, DotSecurityException;
	
	
	/**
	 * Will parse the HTMLPage and return HTMLPage in liveMode or workingMode rendering it as a String.
	 *  
	 * @param htmlPage
	 * @return String
	 * @throws DotDataException 
	 * @throws DotStateException 
	 * @throws DotSecurityException 
	 */	
	public String getHTML(HTMLPage htmlPage, boolean liveMode) throws DotStateException, DotDataException, DotSecurityException;
	/**
	 * Will parse the HTMLPage and return HTMLPage in liveMode or workingMode, include 
	 * the URL Map Content and rendering it as a String.
	 *  
	 * @param htmlPage
	 * @return String
	 * @throws DotDataException 
	 * @throws DotStateException 
	 * @throws DotSecurityException 
	 */	
	public String getHTML(HTMLPage htmlPage, boolean liveMode, String contentId) throws DotStateException, DotDataException, DotSecurityException;
	/**
	 * Will parse the HTMLPage and return HTMLPage in liveMode or workingMode, include 
	 * the URL Map Content and rendering it as a String.
	 *  
	 * @param htmlPage
	 * @return String
	 * @throws DotDataException 
	 * @throws DotStateException 
	 * @throws DotSecurityException 
	 */	
	public String getHTML(HTMLPage htmlPage, boolean liveMode, String contentId,User user) throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * Will parse the HTMLPage and return HTMLPage in liveMode or workingMode, include 
	 * the URL Map Content and rendering it as a String.
	 *  
	 * @param uri
	 * @return String
	 * @throws DotDataException 
	 * @throws DotStateException 
	 * @throws DotSecurityException 
	 */	
	public String getHTML(String uri, Host host,boolean liveMode,String contentId,User user) throws DotStateException, DotDataException, DotSecurityException;
	
	/**
	 * Retrieves the working version of a page based on its identifier
	 * @return
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public HTMLPage loadWorkingPageById(String pageId, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;
	
	/**
	 * Retrieves the Live version of a page based on its identifier
	 * @return
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public HTMLPage loadLivePageById(String pageId, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;
	
	
	
    /**
     * Retrieves all html pages the given user can read 
     * @param user
     * @param includeArchived
     * @param params
     * @param hostId
     * @param inode
     * @param identifier
     * @param parent
     * @param offset
     * @param limit
     * @param orderBy
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
	public List<HTMLPage> findHtmlPages(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;
	
	public boolean movePage(HTMLPage page, Folder parent, User user,boolean respectFrontendRoles) throws DotStateException,	DotDataException, DotSecurityException;	
}