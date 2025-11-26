package com.dotmarketing.portlets.htmlpageasset.business;

import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import com.dotmarketing.portlets.containers.model.Container;

/**
 * Provides utility methods to interact with the upgraded version of HTML pages.
 * 
 * @author Jorge Urdaneta
 * @version 1.1
 * @since 08-28-2014
 *
 */
public interface HTMLPageAssetAPI {
    
    static final String URL_FIELD="url";
    static final String URL_FIELD_NAME="Url";
    
    static final String HOST_FOLDER_FIELD="hostfolder";
    static final String HOST_FOLDER_FIELD_NAME="Host or Folder";
    
    static final String TITLE_FIELD="title";
    static final String TITLE_FIELD_NAME="Title";
    
    static final String FRIENDLY_NAME_FIELD="friendlyName";
    static final String FRIENDLY_NAME_FIELD_NAME="Friendly Name";
    
    static final String SORT_ORDER_FIELD = "sortOrder";
    static final String SORT_ORDER_FIELD_NAME = "Sort Order";
    
    static final String SHOW_ON_MENU_FIELD = "showOnMenu";
    static final String SHOW_ON_MENU_FIELD_NAME = "Show On Menu";
    
    static final String REDIRECT_URL_FIELD="redirecturl";
    static final String REDIRECT_URL_FIELD_NAME="Redirect URL";
    
    static final String HTTPS_REQUIRED_FIELD="httpsreq";
    static final String HTTPS_REQUIRED_FIELD_NAME="HTTPS Required";
    
    static final String CACHE_TTL_FIELD="cachettl";
    static final String CACHE_TTL_FIELD_NAME="Cache TTL (seconds)";
    
    static final String SEO_DESCRIPTION_FIELD="seodescription";
    static final String SEO_DESCRIPTION_FIELD_NAME="SEO Description";
    
    static final String SEO_KEYWORDS_FIELD="seokeywords";
    static final String SEO_KEYWORDS_FIELD_NAME="SEO Keywords";
    
    static final String PAGE_METADATA_FIELD="pagemetadata";
    static final String PAGE_METADATA_FIELD_NAME="Page Metadata"; 
    
    static final String TEMPLATE_FIELD="template";
    static final String TEMPLATE_FIELD_NAME="Template";
    
    static final String DEFAULT_HTMLPAGE_ASSET_STRUCTURE_NAME="HTMLPage Asset";
    static final String DEFAULT_HTMLPAGE_ASSET_STRUCTURE_DESCRIPTION="Default Structure for Pages";
    static final String DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME="htmlpageasset";
    static final String DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE="c541abb1-69b3-4bc5-8430-5e09e5239cc8";
    
    static final String ADVANCED_PROPERTIES_TAB="advancedtab";
    static final String ADVANCED_PROPERTIES_TAB_NAME="Advanced Properties";
    
    void createHTMLPageAssetBaseFields(Structure structure) throws DotDataException, DotStateException;
    
    Template getTemplate(IHTMLPage page, boolean preview) throws DotDataException, DotSecurityException;
    Host getParentHost(IHTMLPage page) throws DotDataException, DotStateException, DotSecurityException;

    HTMLPageAsset fromContentlet(Contentlet content);

    IHTMLPage getPageByPath(String path, Host host, Long languageId, Boolean live) throws DotDataException, DotSecurityException;

    List<IHTMLPage> getLiveHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

    /**
     * Returns the a list of live HTML pages living directly under a given host
     *
     * @param parent
     * @param user
     * @param respectFrontEndRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<IHTMLPage> getLiveHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException;

    List<IHTMLPage> getWorkingHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

    /**
     * Returns the a list of working HTML pages living directly under a given host
     *
     * @param parent
     * @param user
     * @param respectFrontEndRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<IHTMLPage> getWorkingHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException;

    List<IHTMLPage> getDeletedHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

    /**
     * Returns the a list of archived HTML pages living directly under a given host
     *
     * @param parent
     * @param user
     * @param respectFrontEndRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    List<IHTMLPage> getDeletedHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException;

    List<IHTMLPage> getHTMLPages(Object parent, boolean live, boolean deleted, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

	/**
	 * Returns the Html Pages associated to the container
	 *
	 * @param containerId {@link String}
	 * @return List of IHTMLPage
	 */
	List<IHTMLPage> getHTMLPagesByContainer(final String containerId) throws DotDataException, DotSecurityException;

	/**
	 * Returns a list of HTML pages that meet the specified filtering criteria.
	 * 
	 * @param parent
	 * @param live
	 * @param deleted
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @param user
	 * @param respectFrontEndRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<IHTMLPage> getHTMLPages(Object parent, boolean live, boolean deleted,
			int limit, int offset, String sortBy, User user,
			boolean respectFrontEndRoles) throws DotDataException,
			DotSecurityException;

    Folder getParentFolder(IHTMLPage htmlPage) throws DotDataException, DotSecurityException;
        
    String getHostDefaultPageType(Host host);

    String getHostDefaultPageType(String hostId) throws DotDataException, DotSecurityException;

    boolean rename(HTMLPageAsset page, String newName, User user) throws DotDataException, DotSecurityException;

    boolean move(HTMLPageAsset page, Folder parent, User user)throws DotDataException, DotSecurityException;
    
    boolean move(HTMLPageAsset page, Host host, User user)throws DotDataException, DotSecurityException;

	/**
	 * Gets a list of Page Contentlets using a specific Template
	 * @param template
	 * @param user to check for permissions
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> findPagesByTemplate(Template template, User user, boolean respectFrontendRoles)
			throws  DotDataException, DotSecurityException;

	/**
	 * Gets a list of Page Contentlets using a specific Template
	 * @param template
	 * @param user
	 * @param respectFrontendRoles
	 * @param limit
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Contentlet> findPagesByTemplate(Template template, User user, boolean respectFrontendRoles, int limit)
			throws  DotDataException, DotSecurityException;

    List<String> findUpdatedHTMLPageIdsByURI(Host host, String pattern, boolean include, Date startDate, Date endDate);
    
    public String getHTML(IHTMLPage htmlPage, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException;

	public String getHTML(IHTMLPage htmlPage, boolean liveMode, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException;

	public String getHTML(IHTMLPage htmlPage, boolean liveMode,
			String contentId, String userAgent) throws DotStateException,
			DotDataException, DotSecurityException;
	
	public String getHTML(IHTMLPage htmlPage, boolean liveMode,
			String contentId, User user, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException;

    public String getHTML(IHTMLPage htmlPage, boolean liveMode,
                          String contentId, User user, long langId, String userAgent)
            throws DotStateException, DotDataException, DotSecurityException;

	public String getHTML(String uri, Host host, boolean liveMode,
			String contentId, User user, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException;

	public String getHTML(String uri, Host host, boolean liveMode,
			String contentId, User user, long langId, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException;
	
	/**
	 * 
	 */
	public static class TemplateContainersReMap {

		/**
		 * 
		 */
		public static class ContainerRemapTuple {

			private Container sourceContainer;
			private Container destinationContainer;

			public ContainerRemapTuple(Container sourceContainer,
					Container destinationContainer) {
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

		public TemplateContainersReMap(Template sourceTemplate,
				Template destinationTemplate,
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

    IHTMLPage findPage(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * Returns page based on identifier and respects the language fallback settings
     * @param identifier
     * @param tryLang
     * @param live
     * @param user
     * @param respectFrontEndPermissions
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    IHTMLPage findByIdLanguageFallback(Identifier identifier, long tryLang, boolean live, User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;

    /**
     * Returns page based on identifier and respects the language fallback settings
     * @param identifier identifier of page
     * @param tryLang requested language
     * @param live live or working version of page
     * @param user user to perform operation with
     * @param respectFrontEndPermissions respect front end permissions
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    IHTMLPage findByIdLanguageFallback(String identifier, long tryLang, boolean live, User user, boolean respectFrontEndPermissions)
            throws DotDataException, DotSecurityException;



	/**
	 * Returns page based on identifier and respects the language fallback settings
	 * If a non-standard variant is passed in and it is not found, this will also fall back to the
	 * DEFAULT variant as well.
	 * @param identifier identifier of page
	 * @param tryLang requested language
	 * @param tryVariant requested variant
	 * @param live live or working version of page
	 * @param user user to perform operation with
	 * @param respectFrontEndPermissions respect front end permissions
	 *
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	IHTMLPage findByIdLanguageVariantFallback(String identifier, long tryLang, String tryVariant, boolean live, User user, boolean respectFrontEndPermissions)
			throws DotSecurityException;



}
