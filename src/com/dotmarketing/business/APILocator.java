
package com.dotmarketing.business;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPIImpl;
import com.dotcms.enterprise.cmis.CMISAPI;
import com.dotcms.enterprise.cmis.CMISAPIImpl;
import com.dotcms.publishing.PublisherAPI;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.publishing.sitesearch.ESSiteSearchAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.business.portal.PortletAPIImpl;
import com.dotmarketing.cms.polls.business.PollsAPI;
import com.dotmarketing.cms.polls.business.PollsAPILiferayImpl;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.common.business.journal.DistributedJournalAPIImpl;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.plugin.business.PluginAPIImpl;
import com.dotmarketing.portlets.calendar.business.CalendarReminderAPI;
import com.dotmarketing.portlets.calendar.business.CalendarReminderAPIImpl;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.calendar.business.EventAPIImpl;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPIImpl;
import com.dotmarketing.portlets.chains.business.ChainAPI;
import com.dotmarketing.portlets.chains.business.ChainAPIImpl;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.ContainerAPIImpl;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIInterceptor;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPIImpl;
import com.dotmarketing.portlets.dashboard.business.DashboardAPI;
import com.dotmarketing.portlets.dashboard.business.DashboardAPIImpl;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPIImpl;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.business.FileAPIImpl;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderAPIImpl;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.form.business.FormAPIImpl;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPIImpl;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPIImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPIImpl;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.business.MenuLinkAPIImpl;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.business.FieldAPIImpl;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPIImpl;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPI;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPIImpl;
import com.dotmarketing.portlets.widget.business.WidgetAPI;
import com.dotmarketing.portlets.widget.business.WidgetAPIImpl;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIImpl;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.business.TagAPIImpl;
import com.dotmarketing.util.Logger;


/**
 * APILocator is a factory method (pattern) to get single(ton) service objects. 
 * This is a kind of implementation, and there may be others.
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.6.5
 * @since 1.6
 */

public class APILocator extends Locator<APIIndex>{

	protected static APILocator instance;

	private APILocator() {
		super();
	}

	public synchronized static void init(){
		if(instance != null)
			return;
		instance = new APILocator();
	}

	public static PermissionAPI getPermissionAPI() {
		return (PermissionAPI)getInstance(APIIndex.PERMISSION_API);
	}

	public static RoleAPI getRoleAPI() {
		return (RoleAPI)getInstance(APIIndex.ROLE_API);
	}

	public static UserAPI getUserAPI() {
		return (UserAPI)getInstance(APIIndex.USER_API);
	}

	public static EventAPI getEventAPI() {
		return (EventAPI)getInstance(APIIndex.EVENT_API);
	}

	public static CategoryAPI getCategoryAPI() {
		return (CategoryAPI)getInstance(APIIndex.CATEGORY_API);
	}

	public static TemplateAPI getTemplateAPI() {
		return (TemplateAPI)getInstance(APIIndex.TEMPLATE_API);
	}

	public static TimeMachineAPI getTimeMachineAPI() {
		return (TimeMachineAPI)getInstance(APIIndex.TIME_MACHINE_API);
	}

	/**
	 * This will return to you the intercepter which wraps the contentletAPI
	 * It handles all the AOP logic in that it controls the pre-hooks and post hooks
	 * @return
	 */
	public static ContentletAPI getContentletAPIntercepter() {
		return (ContentletAPI)getInstance(APIIndex.CONTENTLET_API_INTERCEPTER);
	}
	
	/**
	 * The actual dotcms contentletAP implementation.  This should only
	 * be needed if you don't want the pre/post hooks to fire. A pre hook
	 * that is on the checkin method might need to use this as it doesn't want 
	 * all the pre hooks to fire. 
	 * @return
	 */
	public static ContentletAPI getContentletAPIImpl() {
		return (ContentletAPI)getInstance(APIIndex.CONTENTLET_API);
	}
	
	/**
	 * This is the contentletAPI which an application should use to do ALL
	 * normal contentletAPI logic
	 * @return
	 */
	public static ContentletAPI getContentletAPI() {
		return (ContentletAPI)getInstance(APIIndex.CONTENTLET_API_INTERCEPTER);
	}
	

	public static IdentifierAPI getIdentifierAPI() {
		return (IdentifierAPI)getInstance(APIIndex.IDENTIFIER_API);
	}

	public static RelationshipAPI getRelationshipAPI(){
		return (RelationshipAPI)getInstance(APIIndex.RELATIONSHIP_API);
	}

	public static FieldAPI getFieldAPI(){
		return (FieldAPI)getInstance(APIIndex.FIELD_API);
	}

	public static PortletAPI getPortletAPI(){
		return (PortletAPI)getInstance(APIIndex.PORTLET_API);
	}

	public static WidgetAPI getWidgetAPI(){
		return (WidgetAPI)getInstance(APIIndex.WIDGET_API);
	}
	
	public static FormAPI getFormAPI(){
		return (FormAPI)getInstance(APIIndex.FORM_API);
	}

	public static CalendarReminderAPI getCalendarReminderAPI(){
		return (CalendarReminderAPI) getInstance(APIIndex.CALENDAR_REMINDER_API);
	}
	
	public static PollsAPI getPollAPI(){
		return (PollsAPI) getInstance(APIIndex.POLL_API);
	}

	public static ChainAPI getChainAPI(){
		return (ChainAPI) getInstance(APIIndex.CHAIN_API);
	}
	
	public static PluginAPI getPluginAPI(){
		return (PluginAPI) getInstance(APIIndex.PLUGIN_API);
	}
	
	public static LanguageAPI getLanguageAPI(){
		return (LanguageAPI) getInstance(APIIndex.LANGUAGE_API);
	}
	
	@SuppressWarnings("unchecked")
	public static DistributedJournalAPI<String> getDistributedJournalAPI(){
		return (DistributedJournalAPI<String>) getInstance(APIIndex.DISTRIBUTED_JOURNAL_API);
	}
	
	public static FolderAPI getFolderAPI(){
		return (FolderAPI) getInstance(APIIndex.FOLDER_API);
	}
	
	public static HostAPI getHostAPI(){
		return (HostAPI) getInstance(APIIndex.HOST_API);
	}

	public static ContainerAPI getContainerAPI(){
		return (ContainerAPI) getInstance(APIIndex.CONTAINER_API);
	}

	public static UserProxyAPI getUserProxyAPI(){
		return (UserProxyAPI) getInstance(APIIndex.USER_PROXY_API);
	}
	
	public static LayoutAPI getLayoutAPI(){
		return (LayoutAPI) getInstance(APIIndex.LAYOUT_API);
	}
	
	public static HostVariableAPI getHostVariableAPI(){
		return (HostVariableAPI) getInstance(APIIndex.HOST_VARIABLE_API);
	}
	
	public static CMISAPI getCMISAPI(){
		return (CMISAPI) getInstance(APIIndex.CMIS_API);
	}
	
	public static FileAPI getFileAPI(){
		return (FileAPI) getInstance(APIIndex.FILE_API);
	}
	
	public static HTMLPageAPI getHTMLPageAPI(){
		return (HTMLPageAPI) getInstance(APIIndex.HTMLPAGE_API);
	}
	
	public static MenuLinkAPI getMenuLinkAPI(){
		return (MenuLinkAPI) getInstance(APIIndex.MENULINK_API);
	}
	
	public static VirtualLinkAPI getVirtualLinkAPI(){
		return (VirtualLinkAPI) getInstance(APIIndex.VIRTUALLINK_API);
	}
	public static DashboardAPI getDashboardAPI(){
		return (DashboardAPI) getInstance(APIIndex.DASHBOARD_API);
	}
	
	public static SiteSearchAPI getSiteSearchAPI(){
		return (SiteSearchAPI) getInstance(APIIndex.SITESEARCH_API);
	}
	public static FileAssetAPI getFileAssetAPI(){
		return (FileAssetAPI) getInstance(APIIndex.FILEASSET_API);
	}
	public static VersionableAPI getVersionableAPI(){
		return (VersionableAPI) getInstance(APIIndex.VERSIONABLE_API);
	}
	public static WorkflowAPI getWorkflowAPI(){
		return (WorkflowAPI) getInstance(APIIndex.WORKFLOW_API);
	}
	
	public static TagAPI getTagAPI(){
		return (TagAPI) getInstance(APIIndex.TAG_API);
	}
	
	public static IndiciesAPI getIndiciesAPI() {
	    return (IndiciesAPI) getInstance(APIIndex.INDICIES_API);
	}
	
	public static ContentletIndexAPI getContentletIndexAPI() {
	    return (ContentletIndexAPI) getInstance(APIIndex.CONTENLET_INDEX_API);
	}
	public static ESIndexAPI getESIndexAPI() {
	    return (ESIndexAPI) getInstance(APIIndex.ES_INDEX_API);
	}
	public static PublisherAPI getPublisherAPI() {
	    return (PublisherAPI) getInstance(APIIndex.PUBLISHER_API);
	}
	private static Object getInstance(APIIndex index) {

		if(instance == null){
			init();
			if(instance == null){
				Logger.fatal(APILocator.class,"CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
				throw new DotRuntimeException("CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
			}
		}

		Object serviceRef = instance.getServiceInstance(index);

		if( Logger.isDebugEnabled(APILocator.class) ) {
			Logger.debug(APILocator.class, instance.audit(index));
		}

		return serviceRef;

	}

	@Override
	protected Object createService(APIIndex enumObj) {
		return enumObj.create();
	}

	@Override
	protected Locator<APIIndex> getLocatorInstance() {
		return instance;
	}

}

enum APIIndex
{ 
	CATEGORY_API,
	CONTENTLET_API,
	CONTENTLET_API_INTERCEPTER,
	DISTRIBUTED_JOURNAL_API,
	EVENT_API,
	EVENT_RECURRENCE_API,
	PERMISSION_API,
	ROLE_API,
	USER_API,
	RELATIONSHIP_API,
	FIELD_API,
	IDENTIFIER_API,
	PORTLET_API,
	WIDGET_API,
	CHAIN_API,
	CALENDAR_REMINDER_API,
	PLUGIN_API,
	LANGUAGE_API,
	POLL_API,
	TEMPLATE_API,
	FOLDER_API,
	HOST_API,
	CONTAINER_API,
	USER_PROXY_API,
	LAYOUT_API,
	HOST_VARIABLE_API,
	FORM_API,
	CMIS_API,
	FILE_API,
	HTMLPAGE_API,
	MENULINK_API,
	VIRTUALLINK_API,
	DASHBOARD_API,
	SITESEARCH_API,
	VERSIONABLE_API,
	FILEASSET_API,
	WORKFLOW_API,
	TAG_API,
	INDICIES_API,
	CONTENLET_INDEX_API,
	PUBLISHER_API,
	ES_INDEX_API,
	TIME_MACHINE_API;
	Object create() {
		switch(this) {
		case PERMISSION_API: return new PermissionBitAPIImpl(FactoryLocator.getPermissionFactory());
		case ROLE_API: return new RoleAPIImpl();
		case USER_API: return new UserAPIImpl();
		case EVENT_API: return new EventAPIImpl(); 
		case CATEGORY_API: return new CategoryAPIImpl();
		case CONTENTLET_API: return new  ESContentletAPIImpl();
		case CONTENTLET_API_INTERCEPTER: return new ContentletAPIInterceptor();
		case RELATIONSHIP_API: return new RelationshipAPIImpl();
		case IDENTIFIER_API: return new IdentifierAPIImpl();
		case FIELD_API: return new FieldAPIImpl();
		case PORTLET_API: return new PortletAPIImpl();
		case WIDGET_API: return new WidgetAPIImpl();
		case CALENDAR_REMINDER_API: return new CalendarReminderAPIImpl();
		case POLL_API: return new PollsAPILiferayImpl();
		case CHAIN_API: return new ChainAPIImpl();
		case PLUGIN_API: return new PluginAPIImpl();
		case LANGUAGE_API: return new LanguageAPIImpl();
		case DISTRIBUTED_JOURNAL_API : return new DistributedJournalAPIImpl<String>();
		case TEMPLATE_API : return new TemplateAPIImpl();
		case FOLDER_API: return new FolderAPIImpl();
		case CONTAINER_API: return new ContainerAPIImpl();
		case USER_PROXY_API : return new UserProxyAPIImpl();
		case HOST_API : return new HostAPIImpl();
		case LAYOUT_API : return new LayoutAPIImpl();
		case HOST_VARIABLE_API : return new HostVariableAPIImpl();
		case FORM_API: return new FormAPIImpl();
		case CMIS_API: return new CMISAPIImpl();
		case FILE_API: return new FileAPIImpl();
		case HTMLPAGE_API: return new HTMLPageAPIImpl();
		case MENULINK_API: return new MenuLinkAPIImpl();
		case VIRTUALLINK_API: return new VirtualLinkAPIImpl();
		case DASHBOARD_API: return new DashboardAPIImpl();
		case SITESEARCH_API: return new ESSiteSearchAPI();
		case FILEASSET_API: return new FileAssetAPIImpl();
		case VERSIONABLE_API: return new VersionableAPIImpl();
		case WORKFLOW_API : return new WorkflowAPIImpl(); 
		case TAG_API: return new TagAPIImpl();
		case INDICIES_API: return new IndiciesAPIImpl();
		case CONTENLET_INDEX_API: return new ESContentletIndexAPI();
		case ES_INDEX_API: return new ESIndexAPI();
		case PUBLISHER_API: return new PublisherAPIImpl();
		case TIME_MACHINE_API: return new TimeMachineAPIImpl();
		}
		throw new AssertionError("Unknown API index: " + this);
	}
}
