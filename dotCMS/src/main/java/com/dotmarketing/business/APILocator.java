package com.dotmarketing.business;

import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotcms.api.tree.TreeableAPI;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.cluster.business.ServerAPIImpl;
import com.dotcms.company.CompanyAPI;
import com.dotcms.company.CompanyAPIFactory;
import com.dotcms.content.elasticsearch.business.*;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotcms.enterprise.RulesAPIProxy;
import com.dotcms.enterprise.ServerActionAPIImplProxy;
import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotcms.enterprise.cache.provider.CacheProviderAPIImpl;
import com.dotcms.enterprise.cluster.action.business.ServerActionAPI;
import com.dotcms.enterprise.linkchecker.LinkCheckerAPIImpl;
import com.dotcms.enterprise.priv.ESSearchProxy;
import com.dotcms.enterprise.publishing.sitesearch.ESSiteSearchAPI;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.notifications.business.NotificationAPIImpl;
import com.dotcms.publisher.assets.business.PushedAssetsAPI;
import com.dotcms.publisher.assets.business.PushedAssetsAPIImpl;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.bundle.business.BundleAPIImpl;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPIImpl;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publisher.environment.business.EnvironmentAPIImpl;
import com.dotcms.publishing.PublisherAPI;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.timemachine.business.TimeMachineAPI;
import com.dotcms.timemachine.business.TimeMachineAPIImpl;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotcms.util.SecurityLoggerServiceAPIFactory;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.uuid.shorty.ShortyIdAPIImpl;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.business.VisitorAPIImpl;
import com.dotcms.rest.api.v1.system.websocket.WebSocketContainerAPI;
import com.dotcms.rest.api.v1.system.websocket.WebSocketContainerAPIFactory;
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
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderAPIImpl;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.form.business.FormAPIImpl;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPIImpl;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPIImpl;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPI;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.business.MenuLinkAPIImpl;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPIImpl;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.business.FieldAPIImpl;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.business.StructureAPIImpl;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPIImpl;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPI;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPIImpl;
import com.dotmarketing.portlets.widget.business.WidgetAPI;
import com.dotmarketing.portlets.widget.business.WidgetAPIImpl;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIImpl;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.sitesearch.business.SiteSearchAuditAPI;
import com.dotmarketing.sitesearch.business.SiteSearchAuditAPIImpl;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.business.TagAPIImpl;
import com.dotmarketing.util.Logger;

/**
 * APILocator is a factory method (pattern) to get single(ton) service objects.
 * This is a kind of implementation, and there may be others.
 * 
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.6.5
 * @since 1.6
 */
public class APILocator extends Locator<APIIndex>{

	protected static APILocator instance;

	/**
	 * Private constructor for the singleton.
	 */
	private APILocator() {
		super();
	}

	/**
	 * Creates a single instance of this class.
	 */
	public synchronized static void init(){
		if(instance != null)
			return;
		instance = new APILocator();
	}

	public static SecurityLoggerServiceAPI getSecurityLogger() {
		return (SecurityLoggerServiceAPI)getInstance(APIIndex.SECURITY_LOGGER_API);
	}

	/**
	 * Creates a single instance of the {@link CompanyAPI} class.
	 *
	 * @return The {@link CompanyAPI} class.
	 */
	public static CompanyAPI getCompanyAPI() {
		return (CompanyAPI) getInstance(APIIndex.COMPANY_API);
	}

	/**
	 * Creates a single instance of the {@link WebSocketContainerAPI} class.
	 *
	 * @return The {@link WebSocketContainerAPI} class.
	 */
	public static WebSocketContainerAPI getWebSocketContainerAPI() {
		return (WebSocketContainerAPI)getInstance(APIIndex.WEB_SOCKET_CONTAINER_API);
	}

	/**
	 * Creates a single instance of the {@link PermissionAPI} class.
	 * 
	 * @return The {@link PermissionAPI} class.
	 */
	public static PermissionAPI getPermissionAPI() {
		return (PermissionAPI)getInstance(APIIndex.PERMISSION_API);
	}

	/**
	 * Creates a single instance of the {@link RoleAPI} class.
	 * 
	 * @return The {@link RoleAPI} class.
	 */
	public static RoleAPI getRoleAPI() {
		return (RoleAPI)getInstance(APIIndex.ROLE_API);
	}

	/**
	 * Creates a single instance of the {@link UserAPI} class.
	 * 
	 * @return The {@link UserAPI} class.
	 */
	public static UserAPI getUserAPI() {
		return (UserAPI)getInstance(APIIndex.USER_API);
	}

	/**
	 * Creates a single instance of the {@link LoginAsAPI} class.
	 *
	 * @return The {@link LoginAsAPI} class.
	 */
	public static LoginAsAPI getLoginAsAPI() {
		return (LoginAsAPI) getInstance(APIIndex.LOGIN_AS_USER_API);
	}

	/**
	 * Creates a single instance of the {@link EventAPI} class.
	 * 
	 * @return The {@link EventAPI} class.
	 */
	public static EventAPI getEventAPI() {
		return (EventAPI)getInstance(APIIndex.EVENT_API);
	}

	/**
	 * Creates a single instance of the {@link CategoryAPI} class.
	 * 
	 * @return The {@link CategoryAPI} class.
	 */
	public static CategoryAPI getCategoryAPI() {
		return (CategoryAPI)getInstance(APIIndex.CATEGORY_API);
	}

	/**
	 * Creates a single instance of the {@link TemplateAPI} class.
	 * 
	 * @return The {@link TemplateAPI} class.
	 */
	public static TemplateAPI getTemplateAPI() {
		return (TemplateAPI)getInstance(APIIndex.TEMPLATE_API);
	}

	/**
	 * Creates a single instance of the {@link TimeMachineAPI} class.
	 * 
	 * @return The {@link TimeMachineAPI} class.
	 */
	public static TimeMachineAPI getTimeMachineAPI() {
		return (TimeMachineAPI)getInstance(APIIndex.TIME_MACHINE_API);
	}

	/**
	 * This will return to you the intercepter which wraps the
	 * {@link ContentletAPI}. It handles all the AOP logic in that it controls
	 * the pre-hooks and post-hooks.
	 * 
	 * @return The {@link ContentletAPI} class for dealing with interceptors.
	 */
	public static ContentletAPI getContentletAPIntercepter() {
		return (ContentletAPI)getInstance(APIIndex.CONTENTLET_API_INTERCEPTER);
	}

	/**
	 * The actual dotCMS {@link ContentletAPI} implementation. This should only
	 * be needed if you don't want the pre/post hooks to fire. A pre hook that
	 * is on the checkin method might need to use this as it doesn't want all
	 * the pre hooks to fire.
	 * 
	 * @return The {@link ContentletAPI} class.
	 */
	public static ContentletAPI getContentletAPIImpl() {
		return (ContentletAPI)getInstance(APIIndex.CONTENTLET_API);
	}

	/**
	 * This is the contentletAPI which an application should use to do ALL
	 * normal {@link ContentletAPI} logic.
	 * 
	 * @return The {@link ContentletAPI} class.
	 */
	public static ContentletAPI getContentletAPI() {
		return (ContentletAPI)getInstance(APIIndex.CONTENTLET_API_INTERCEPTER);
	}

	/**
	 * Creates a single instance of the {@link IdentifierAPI} class.
	 * 
	 * @return The {@link IdentifierAPI} class.
	 */
	public static IdentifierAPI getIdentifierAPI() {
		return (IdentifierAPI)getInstance(APIIndex.IDENTIFIER_API);
	}

	/**
	 * Creates a single instance of the {@link RelationshipAPI} class.
	 * 
	 * @return The {@link RelationshipAPI} class.
	 */
	public static RelationshipAPI getRelationshipAPI(){
		return (RelationshipAPI)getInstance(APIIndex.RELATIONSHIP_API);
	}

	/**
	 * Creates a single instance of the {@link FieldAPI} class.
	 * 
	 * @return The {@link FieldAPI} class.
	 */
	public static FieldAPI getFieldAPI(){
		return (FieldAPI)getInstance(APIIndex.FIELD_API);
	}

	/**
	 * Creates a single instance of the {@link PortletAPI} class.
	 * 
	 * @return The {@link PortletAPI} class.
	 */
	public static PortletAPI getPortletAPI(){
		return (PortletAPI)getInstance(APIIndex.PORTLET_API);
	}

	/**
	 * Creates a single instance of the {@link WidgetAPI} class.
	 * 
	 * @return The {@link WidgetAPI} class.
	 */
	public static WidgetAPI getWidgetAPI(){
		return (WidgetAPI)getInstance(APIIndex.WIDGET_API);
	}

	/**
	 * Creates a single instance of the {@link FormAPI} class.
	 * 
	 * @return The {@link FormAPI} class.
	 */
	public static FormAPI getFormAPI(){
		return (FormAPI)getInstance(APIIndex.FORM_API);
	}

	/**
	 * Creates a single instance of the {@link CalendarReminderAPI} class.
	 * 
	 * @return The {@link CalendarReminderAPI} class.
	 */
	public static CalendarReminderAPI getCalendarReminderAPI(){
		return (CalendarReminderAPI) getInstance(APIIndex.CALENDAR_REMINDER_API);
	}

	/**
	 * Creates a single instance of the {@link PollsAPI} class.
	 * 
	 * @return The {@link PollsAPI} class.
	 */
	public static PollsAPI getPollAPI(){
		return (PollsAPI) getInstance(APIIndex.POLL_API);
	}

	/**
	 * Creates a single instance of the {@link ChainAPI} class.
	 * 
	 * @return The {@link ChainAPI} class.
	 */
	public static ChainAPI getChainAPI(){
		return (ChainAPI) getInstance(APIIndex.CHAIN_API);
	}

	/**
	 * Creates a single instance of the {@link PluginAPI} class.
	 * 
	 * @return The {@link PluginAPI} class.
	 */
	public static PluginAPI getPluginAPI(){
		return (PluginAPI) getInstance(APIIndex.PLUGIN_API);
	}

	/**
	 * Creates a single instance of the {@link LanguageAPI} class.
	 * 
	 * @return The {@link LanguageAPI} class.
	 */
	public static LanguageAPI getLanguageAPI(){
		return (LanguageAPI) getInstance(APIIndex.LANGUAGE_API);
	}

	/**
	 * Creates a single instance of the {@link DistributedJournalAPI} class.
	 * 
	 * @return The {@link DistributedJournalAPI} class.
	 */
	@SuppressWarnings("unchecked")
	public static DistributedJournalAPI<String> getDistributedJournalAPI(){
		return (DistributedJournalAPI<String>) getInstance(APIIndex.DISTRIBUTED_JOURNAL_API);
	}

	/**
	 * Creates a single instance of the {@link FolderAPI} class.
	 * 
	 * @return The {@link FolderAPI} class.
	 */
	public static FolderAPI getFolderAPI(){
		return (FolderAPI) getInstance(APIIndex.FOLDER_API);
	}

	/**
	 * Creates a single instance of the {@link HostAPI} class.
	 * 
	 * @return The {@link HostAPI} class.
	 */
	public static HostAPI getHostAPI(){
		return (HostAPI) getInstance(APIIndex.HOST_API);
	}

	/**
	 * Creates a single instance of the {@link ContainerAPI} class.
	 * 
	 * @return The {@link ContainerAPI} class.
	 */
	public static ContainerAPI getContainerAPI(){
		return (ContainerAPI) getInstance(APIIndex.CONTAINER_API);
	}

	/**
	 * Creates a single instance of the {@link UserProxyAPI} class.
	 * 
	 * @return The {@link UserProxyAPI} class.
	 */
	public static UserProxyAPI getUserProxyAPI(){
		return (UserProxyAPI) getInstance(APIIndex.USER_PROXY_API);
	}

	/**
	 * Creates a single instance of the {@link LayoutAPI} class.
	 * 
	 * @return The {@link LayoutAPI} class.
	 */
	public static LayoutAPI getLayoutAPI(){
		return (LayoutAPI) getInstance(APIIndex.LAYOUT_API);
	}

	/**
	 * Creates a single instance of the {@link HostVariableAPI} class.
	 * 
	 * @return The {@link HostVariableAPI} class.
	 */
	public static HostVariableAPI getHostVariableAPI(){
		return (HostVariableAPI) getInstance(APIIndex.HOST_VARIABLE_API);
	}

	/**
	 * Creates a single instance of the {@link HostVariableAPI} class.
	 * 
	 * @return The {@link HostVariableAPI} class.
	 * @deprecated This API can be used for Legacy {@link File} objects ONLY.
	 *             Files are now represented as content. Please refer to the
	 *             {@link #getFileAssetAPI()}
	 */
	public static FileAPI getFileAPI(){
		return (FileAPI) getInstance(APIIndex.FILE_API);
	}

	/**
	 * Creates a single instance of the {@link HTMLPageAPI} class.
	 * 
	 * @return The {@link HTMLPageAPI} class.
	 * @deprecated This API can be used for Legacy {@link HTMLPage} objects
	 *             ONLY. HTML Pages are now represented as content. Please refer
	 *             to the {@link #getHTMLPageAssetAPI()}
	 */
	public static HTMLPageAPI getHTMLPageAPI(){
		return (HTMLPageAPI) getInstance(APIIndex.HTMLPAGE_API);
	}

	/**
	 * Creates a single instance of the {@link MenuLinkAPI} class.
	 * 
	 * @return The {@link MenuLinkAPI} class.
	 */
	public static MenuLinkAPI getMenuLinkAPI(){
		return (MenuLinkAPI) getInstance(APIIndex.MENULINK_API);
	}

	/**
	 * Creates a single instance of the {@link VirtualLinkAPI} class.
	 * 
	 * @return The {@link VirtualLinkAPI} class.
	 */
	public static VirtualLinkAPI getVirtualLinkAPI(){
		return (VirtualLinkAPI) getInstance(APIIndex.VIRTUALLINK_API);
	}

	/**
	 * Creates a single instance of the {@link DashboardAPI} class.
	 * 
	 * @return The {@link DashboardAPI} class.
	 */
	public static DashboardAPI getDashboardAPI(){
		return (DashboardAPI) getInstance(APIIndex.DASHBOARD_API);
	}

	/**
	 * Creates a single instance of the {@link SiteSearchAPI} class.
	 * 
	 * @return The {@link SiteSearchAPI} class.
	 */
	public static SiteSearchAPI getSiteSearchAPI(){
		return (SiteSearchAPI) getInstance(APIIndex.SITESEARCH_API);
	}

	/**
	 * Creates a single instance of the {@link FileAssetAPI} class.
	 * 
	 * @return The {@link FileAssetAPI} class.
	 */
	public static FileAssetAPI getFileAssetAPI(){
		return (FileAssetAPI) getInstance(APIIndex.FILEASSET_API);
	}

	/**
	 * Creates a single instance of the {@link VersionableAPI} class.
	 * 
	 * @return The {@link VersionableAPI} class.
	 */
	public static VersionableAPI getVersionableAPI(){
		return (VersionableAPI) getInstance(APIIndex.VERSIONABLE_API);
	}

	/**
	 * Creates a single instance of the {@link WorkflowAPI} class.
	 * 
	 * @return The {@link WorkflowAPI} class.
	 */
	public static WorkflowAPI getWorkflowAPI(){
		return (WorkflowAPI) getInstance(APIIndex.WORKFLOW_API);
	}

	/**
	 * Creates a single instance of the {@link CacheProviderAPI} class.
	 * 
	 * @return The {@link CacheProviderAPI} class.
	 */
	public static CacheProviderAPI getCacheProviderAPI () {
		return (CacheProviderAPI) getInstance(APIIndex.CACHE_PROVIDER_API);
	}

	/**
	 * Creates a single instance of the {@link TagAPI} class.
	 * 
	 * @return The {@link TagAPI} class.
	 */
	public static TagAPI getTagAPI(){
		return (TagAPI) getInstance(APIIndex.TAG_API);
	}

	/**
	 * Creates a single instance of the {@link IndiciesAPI} class.
	 * 
	 * @return The {@link IndiciesAPI} class.
	 */
	public static IndiciesAPI getIndiciesAPI() {
	    return (IndiciesAPI) getInstance(APIIndex.INDICIES_API);
	}

	/**
	 * Creates a single instance of the {@link ContentletIndexAPI} class.
	 * 
	 * @return The {@link ContentletIndexAPI} class.
	 */
	public static ContentletIndexAPI getContentletIndexAPI() {
	    return (ContentletIndexAPI) getInstance(APIIndex.CONTENLET_INDEX_API);
	}

	/**
	 * Creates a single instance of the {@link ESIndexAPI} class.
	 * 
	 * @return The {@link ESIndexAPI} class.
	 */
	public static ESIndexAPI getESIndexAPI() {
	    return (ESIndexAPI) getInstance(APIIndex.ES_INDEX_API);
	}

	/**
	 * Creates a single instance of the {@link PublisherAPI} class.
	 * 
	 * @return The {@link PublisherAPI} class.
	 */
	public static PublisherAPI getPublisherAPI() {
	    return (PublisherAPI) getInstance(APIIndex.PUBLISHER_API);
	}

	/**
	 * Creates a single instance of the {@link LinkCheckerAPI} class.
	 * 
	 * @return The {@link LinkCheckerAPI} class.
	 */
	public static LinkCheckerAPI getLinkCheckerAPI() {
	    return (LinkCheckerAPI) getInstance(APIIndex.LINKCHECKER_API);
	}

	/**
	 * Creates a single instance of the {@link PublishingEndPointAPI} class.
	 * 
	 * @return The {@link PublishingEndPointAPI} class.
	 */
	public static PublishingEndPointAPI getPublisherEndPointAPI() {
		return (PublishingEndPointAPI) getInstance(APIIndex.PUBLISHER_ENDPOINT_API);
	}

	/**
	 * Creates a single instance of the {@link StructureAPI} class.
	 * 
	 * @return The {@link StructureAPI} class.
	 */
	public static StructureAPI getStructureAPI() {
	    return (StructureAPI)getInstance(APIIndex.STRUCTURE_API);
	}

	/**
	 * Creates a single instance of the {@link SiteSearchAuditAPI} class.
	 * 
	 * @return The {@link SiteSearchAuditAPI} class.
	 */
	public static SiteSearchAuditAPI getSiteSearchAuditAPI() {
	    return (SiteSearchAuditAPI)getInstance(APIIndex.SITE_SEARCH_AUDIT_API);
	}

	/**
	 * Creates a single instance of the {@link EnvironmentAPI} class.
	 * 
	 * @return The {@link EnvironmentAPI} class.
	 */
	public static EnvironmentAPI getEnvironmentAPI() {
		return (EnvironmentAPI)getInstance(APIIndex.ENVIRONMENT_API);
	}

	/**
	 * Creates a single instance of the {@link BundleAPI} class.
	 * 
	 * @return The {@link BundleAPI} class.
	 */
	public static BundleAPI getBundleAPI() {
		return (BundleAPI)getInstance(APIIndex.BUNDLE_API);
	}

	/**
	 * Creates a single instance of the {@link PushedAssetsAPI} class.
	 * 
	 * @return The {@link PushedAssetsAPI} class.
	 */
	public static PushedAssetsAPI getPushedAssetsAPI() {
		return (PushedAssetsAPI)getInstance(APIIndex.PUSHED_ASSETS_API);
	}
    /**
     * 
     * gets an instance of ShortyAPI
     * 
     * @return The {@link ShortyIdAPI} class.
     */
    public static ShortyIdAPI getShortyAPI() {
		return (ShortyIdAPI) getInstance(APIIndex.SHORTY_ID_API);
    }

	/**
	 * Creates a single instance of the {@link ServerAPI} class.
	 * 
	 * @return The {@link ServerAPI} class.
	 */
	public static ServerAPI getServerAPI() {
		return (ServerAPI)getInstance(APIIndex.SERVER_API);
	}

	/**
	 * Creates a single instance of the {@link NotificationAPI} class.
	 * 
	 * @return The {@link NotificationAPI} class.
	 */
	public static NotificationAPI getNotificationAPI() {
	    return (NotificationAPI)getInstance(APIIndex.NOTIFICATION_API);
	}

	/**
	 * Creates a single instance of the {@link HTMLPageAssetAPI} class.
	 * 
	 * @return The {@link HTMLPageAssetAPI} class.
	 */
	public static HTMLPageAssetAPI getHTMLPageAssetAPI() {
        return (HTMLPageAssetAPI)getInstance(APIIndex.HTMLPAGE_ASSET_API);
    }

	/**
	 * Creates a single instance of the {@link PersonaAPI} class.
	 * 
	 * @return The {@link PersonaAPI} class.
	 */
	public static PersonaAPI getPersonaAPI() {
        return (PersonaAPI)getInstance(APIIndex.PERSONA_API);
    }

	/**
	 * Creates a single instance of the {@link ServerActionAPI} class.
	 * 
	 * @return The {@link ServerActionAPI} class.
	 */
	public static ServerActionAPI getServerActionAPI() {
	    return (ServerActionAPI)getInstance(APIIndex.SERVER_ACTION_API);
	}

	/**
	 * Creates a single instance of the {@link ESSeachAPI} class.
	 * 
	 * @return The {@link ESSeachAPI} class.
	 */
	public static ESSeachAPI getEsSearchAPI () {
		return (ESSeachAPI) getInstance( APIIndex.ES_SEARCH_API );
	}

	/**
	 * Creates a single instance of the {@link RulesAPI} class.
	 * 
	 * @return The {@link RulesAPI} class.
	 */
    public static RulesAPI getRulesAPI () {
		return (RulesAPI) getInstance( APIIndex.RULES_API );
	}

    /**
	 * Creates a single instance of the {@link VisitorAPI} class.
	 * 
	 * @return The {@link VisitorAPI} class.
	 */
    public static VisitorAPI getVisitorAPI () {
		return (VisitorAPI) getInstance( APIIndex.VISITOR_API );
	}

	public static TreeableAPI getTreeableAPI () {return new TreeableAPI();}

	/**
	 * Returns the System Events API that allows other pieces of the application
	 * (or third-party services) to interact with events generated by system
	 * features and react to them.
	 * 
	 * @return An instance of the {@link SystemEventsAPI}.
	 */
	public static SystemEventsAPI getSystemEventsAPI() {
		return (SystemEventsAPI) getInstance(APIIndex.SYSTEM_EVENTS_API);
	}

	/**
	 * Generates a unique instance of the specified dotCMS API.
	 * 
	 * @param index
	 *            - The specified API to retrieve based on the {@link APIIndex}
	 *            class.
	 * @return A singleton of the API.
	 */
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

/**
 * Assists in the creation of singleton objects representing each of the APIs
 * that dotCMS provides for developers. <b>Every new API in the system must be
 * referenced in this class</b>.
 * 
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.6.5
 * @since 1.6
 *
 */
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
	LOGIN_AS_USER_API,
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
	FILE_API,
	HTMLPAGE_API,
	MENULINK_API,
	VIRTUALLINK_API,
	DASHBOARD_API,
	SITESEARCH_API,
	VERSIONABLE_API,
	FILEASSET_API,
	WORKFLOW_API,
	CACHE_PROVIDER_API,
	TAG_API,
	INDICIES_API,
	CONTENLET_INDEX_API,
	PUBLISHER_API,
	ES_INDEX_API,
	LINKCHECKER_API,
	TIME_MACHINE_API,
	PUBLISHER_ENDPOINT_API,
	STRUCTURE_API,
	SITE_SEARCH_AUDIT_API,
	ENVIRONMENT_API,
	BUNDLE_API,
	SERVER_API,
	PUSHED_ASSETS_API,
	NOTIFICATION_API,
	HTMLPAGE_ASSET_API,
	PERSONA_API,
	SERVER_ACTION_API,
	ES_SEARCH_API,
    RULES_API,
    VISITOR_API,
	SHORTY_ID_API,
	SYSTEM_EVENTS_API,
	WEB_SOCKET_CONTAINER_API,
	COMPANY_API,
	SECURITY_LOGGER_API;

	Object create() {
		switch(this) {
		case PERMISSION_API: return new PermissionBitAPIImpl(FactoryLocator.getPermissionFactory());
		case ROLE_API: return new RoleAPIImpl();
		case USER_API: return new UserAPIImpl();
		case LOGIN_AS_USER_API: return LoginAsAPIImpl.getInstance();
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
		case FILE_API: return new FileAPIImpl();
		case HTMLPAGE_API: return new HTMLPageAPIImpl();
		case MENULINK_API: return new MenuLinkAPIImpl();
		case VIRTUALLINK_API: return new VirtualLinkAPIImpl();
		case DASHBOARD_API: return new DashboardAPIImpl();
		case SITESEARCH_API: return new ESSiteSearchAPI();
		case FILEASSET_API: return new FileAssetAPIImpl();
		case VERSIONABLE_API: return new VersionableAPIImpl();
		case WORKFLOW_API : return new WorkflowAPIImpl();
		case CACHE_PROVIDER_API : return new CacheProviderAPIImpl();
		case TAG_API: return new TagAPIImpl();
		case INDICIES_API: return new IndiciesAPIImpl();
		case CONTENLET_INDEX_API: return new ESContentletIndexAPI();
		case ES_INDEX_API: return new ESIndexAPI();
		case PUBLISHER_API: return new PublisherAPIImpl();
		case TIME_MACHINE_API: return new TimeMachineAPIImpl();
		case LINKCHECKER_API: return new LinkCheckerAPIImpl();
		case PUBLISHER_ENDPOINT_API: return new PublishingEndPointAPIImpl(FactoryLocator.getPublisherEndPointFactory());
		case STRUCTURE_API: return new StructureAPIImpl();
		case SITE_SEARCH_AUDIT_API: return new SiteSearchAuditAPIImpl();
		case ENVIRONMENT_API: return new EnvironmentAPIImpl();
		case BUNDLE_API: return new BundleAPIImpl();
		case PUSHED_ASSETS_API: return new PushedAssetsAPIImpl();
		case SERVER_API: return new ServerAPIImpl();
		case NOTIFICATION_API: return new NotificationAPIImpl();
		case HTMLPAGE_ASSET_API: return new HTMLPageAssetAPIImpl();
		case PERSONA_API: return new PersonaAPIImpl();
		case SERVER_ACTION_API: return new ServerActionAPIImplProxy();
		case ES_SEARCH_API: return new ESSearchProxy();
		case RULES_API: return new RulesAPIProxy();
		case VISITOR_API: return new VisitorAPIImpl();
		case SHORTY_ID_API: return new ShortyIdAPIImpl();
		case SYSTEM_EVENTS_API: return SystemEventsFactory.getInstance().getSystemEventsAPI();
		case WEB_SOCKET_CONTAINER_API:return WebSocketContainerAPIFactory.getInstance().getWebSocketContainerAPI();
		case COMPANY_API: return CompanyAPIFactory.getInstance().getCompanyAPI();
		case SECURITY_LOGGER_API: return SecurityLoggerServiceAPIFactory.getInstance().getSecurityLoggerAPI();
		}
		throw new AssertionError("Unknown API index: " + this);
	}

}
