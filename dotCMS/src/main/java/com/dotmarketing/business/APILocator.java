package com.dotmarketing.business;

import com.dotcms.ai.api.DotAIAPI;
import com.dotcms.ai.api.DotAIAPIFacadeImpl;
import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.AnalyticsAPIImpl;
import com.dotcms.analytics.attributes.CustomAttributeAPI;
import com.dotcms.analytics.attributes.CustomAttributeAPIImpl;
import com.dotcms.analytics.bayesian.BayesianAPI;
import com.dotcms.analytics.bayesian.BayesianAPIImpl;
import com.dotcms.analytics.content.ContentAnalyticsAPI;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotcms.api.tree.TreeableAPI;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserAPIImpl;
import com.dotcms.business.SystemAPI;
import com.dotcms.business.SystemAPIImpl;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.cluster.business.ServerAPIImpl;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.cms.login.LoginServiceAPIFactory;
import com.dotcms.company.CompanyAPI;
import com.dotcms.company.CompanyAPIFactory;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.content.business.json.ContentletJsonAPIImpl;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPIImpl;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.ContentTypeDestroyAPI;
import com.dotcms.contenttype.business.ContentTypeDestroyAPIImpl;
import com.dotcms.contenttype.business.ContentTypeFieldLayoutAPI;
import com.dotcms.contenttype.business.ContentTypeFieldLayoutAPIImpl;
import com.dotcms.contenttype.business.DotAssetAPI;
import com.dotcms.contenttype.business.DotAssetAPIImpl;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.business.FieldAPIImpl;
import com.dotcms.contenttype.business.StoryBlockAPI;
import com.dotcms.contenttype.business.StoryBlockAPIImpl;
import com.dotcms.device.DeviceAPI;
import com.dotcms.device.DeviceAPIImpl;
import com.dotcms.dotpubsub.DotPubSubProvider;
import com.dotcms.dotpubsub.DotPubSubProviderLocator;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotcms.enterprise.RulesAPIProxy;
import com.dotcms.enterprise.ServerActionAPIImplProxy;
import com.dotcms.enterprise.achecker.ACheckerAPI;
import com.dotcms.enterprise.achecker.impl.ACheckerAPIImpl;
import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotcms.enterprise.cache.provider.CacheProviderAPIImpl;
import com.dotcms.enterprise.cluster.action.business.ServerActionAPI;
import com.dotcms.enterprise.linkchecker.LinkCheckerAPIImpl;
import com.dotcms.enterprise.priv.ESSearchProxy;
import com.dotcms.enterprise.publishing.sitesearch.ESSiteSearchAPI;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.business.ExperimentsAPIImpl;
import com.dotcms.graphql.business.GraphqlAPI;
import com.dotcms.graphql.business.GraphqlAPIImpl;
import com.dotcms.health.api.HealthService;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.keyvalue.business.KeyValueAPI;
import com.dotcms.keyvalue.business.KeyValueAPIImpl;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.languagevariable.business.LanguageVariableAPIImpl;
import com.dotcms.mail.MailAPI;
import com.dotcms.mail.MailAPIImpl;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.notifications.business.NotificationAPIImpl;
import com.dotcms.publisher.assets.business.PushedAssetsAPI;
import com.dotcms.publisher.assets.business.PushedAssetsAPIImpl;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.bundle.business.BundleAPIImpl;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditAPIImpl;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPIImpl;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publisher.environment.business.EnvironmentAPIImpl;
import com.dotcms.publishing.PublisherAPI;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.system.websocket.WebSocketContainerAPI;
import com.dotcms.rest.api.v1.system.websocket.WebSocketContainerAPIFactory;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.FileMetadataAPIImpl;
import com.dotcms.storage.FileStorageAPI;
import com.dotcms.storage.FileStorageAPIImpl;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.business.LocalSystemEventsAPIFactory;
import com.dotcms.telemetry.business.MetricsAPI;
import com.dotcms.timemachine.business.TimeMachineAPI;
import com.dotcms.timemachine.business.TimeMachineAPIImpl;
import com.dotcms.util.FileWatcherAPI;
import com.dotcms.util.FileWatcherAPIImpl;
import com.dotcms.util.ReflectionUtils;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotcms.util.SecurityLoggerServiceAPIFactory;
import com.dotcms.uuid.shorty.LegacyShortyIdAPIImpl;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.uuid.shorty.ShortyIdAPIImpl;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.business.VanityUrlAPIImpl;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.VariantAPIImpl;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotcms.visitor.business.VisitorAPIImpl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.business.portal.PortletAPIImpl;
import com.dotmarketing.cms.urlmap.URLMapAPIImpl;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.common.reindex.ReindexQueueAPIImpl;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.MultiTreeAPIImpl;
import com.dotmarketing.image.focalpoint.FocalPointAPI;
import com.dotmarketing.image.focalpoint.FocalPointAPIImpl;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.plugin.business.PluginAPIImpl;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.calendar.business.EventAPIImpl;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPIImpl;
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
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderAPIImpl;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.form.business.FormAPIImpl;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPIImpl;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPI;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.business.MenuLinkAPIImpl;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPIImpl;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.business.StructureAPIImpl;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPIImpl;
import com.dotmarketing.portlets.widget.business.WidgetAPI;
import com.dotmarketing.portlets.widget.business.WidgetAPIImpl;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIImpl;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.sitesearch.business.SiteSearchAuditAPI;
import com.dotmarketing.sitesearch.business.SiteSearchAuditAPIImpl;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.business.TagAPIImpl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.Lazy;

import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * APILocator is a factory method (pattern) to get single(ton) service objects.
 * This is a kind of implementation, and there may be others.
 *
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.6.5
 * @since 1.6
 */
public class APILocator extends Locator<APIIndex> {

	protected static APILocator instance;
	private static final Queue<Closeable> closeableQueue = new ConcurrentLinkedQueue<>();

	/**
	 * Private constructor for the singleton.
	 */
	protected APILocator() {
		super();
	}

	/**
	 * Creates a single instance of this class.
	 */
	public synchronized static void init(){
		if(instance != null) {
			return;
		}

		final String apiLocatorClass = Config.getStringProperty("API_LOCATOR_IMPLEMENTATION", null);
		if (apiLocatorClass != null) {
			instance = (APILocator) ReflectionUtils.newInstance(apiLocatorClass);
		}
		if (instance == null) {
			instance = new APILocator();
		}
	}

	/**
	 * Destroy the current instance and Creates a single instance of this class.
	 * this is only for testing
	 */
	@VisibleForTesting
	public synchronized static void destroyAndForceInit(){

		destroy();
		instance = null;

		String apiLocatorClass = Config.getStringProperty("API_LOCATOR_IMPLEMENTATION", null);
		if (apiLocatorClass != null) {
			instance = (APILocator) ReflectionUtils.newInstance(apiLocatorClass);
		}
		if (instance == null) {
			instance = new APILocator();
		}
	}

	/**
	 * This method is just allowed by the own package to register {@link Closeable} resources
	 * @param closeable
	 */
	static void addCloseableResource (final Closeable closeable) {

		closeableQueue.add(closeable);
	}

	/**
	 * This method must be called just at the end of the webcontainer process, to close Services API resources
	 */
	public static void destroy () {

		Logger.debug(APILocator.class, "Destroying API resources");

		for (Closeable closeable : closeableQueue) {

			if (null != closeable) {

				try {

					Logger.debug(APILocator.class, "Destroying resource: " + closeable);
					closeable.close();
				} catch (IOException e) {

					Logger.error(APILocator.class, "Error on Destroying resource: " + closeable, e);
				}
			}
		}
	} // destroy.

    /**
     * Creates a single instance of the {@link SecurityLoggerServiceAPI} class.
     *
     * @return The {@link SecurityLoggerServiceAPI} class.
     */
	public static SecurityLoggerServiceAPI getSecurityLogger() {
		return (SecurityLoggerServiceAPI)getInstance(APIIndex.SECURITY_LOGGER_API);
	}

	/**
	 * Creates a single instance of the {@link DotAIAPI} class.
	 *
	 * @return The {@link DotAIAPI} class.
	 */
	public static DotAIAPI getDotAIAPI() {

		return  (DotAIAPI)getInstance(APIIndex.ARTIFICIAL_INTELLIGENCE_API);
	}

	/**
	 * Creates a single instance of the {@link JobQueueManagerAPI} class.
	 *
	 * @return The {@link JobQueueManagerAPI} class.
	 */
	public static JobQueueManagerAPI getJobQueueManagerAPI() {
		return (JobQueueManagerAPI) getInstance(APIIndex.JOB_QUEUE_MANAGER_API);
	}

	/**
	 * Creates a single instance of the {@link CompanyAPI} class.
	 *
	 * @return The {@link CompanyAPI} class.
	 */
	public static CompanyAPI getCompanyAPI() {
		return getAPILocatorInstance().getCompanyAPIImpl();
	}

	/**
	 * Returns a single instance of the {@link StoryBlockAPI} class.
	 *
	 * @return The {@link StoryBlockAPI} singleton.
	 */
	public static StoryBlockAPI getStoryBlockAPI() {
		return (StoryBlockAPI)getInstance(APIIndex.STORY_BLOCK_API);
	}

	public static CustomAttributeAPI getAnalyticsCustomAttribute() {
		return (CustomAttributeAPI) getInstance(APIIndex.ANALYTICS_CUSTOM_ATTRIBUTE_API);
	}

    @VisibleForTesting
	protected CompanyAPI getCompanyAPIImpl() {
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

	private static final Lazy<MailAPI> lazyMail = Lazy.of(MailAPIImpl::new);
	                
    public static MailAPI getMailApi() {
        return lazyMail.get();
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
	
    public static DotPubSubProvider getDotPubSubProvider() {
        return (DotPubSubProvider) DotPubSubProviderLocator.provider.get();
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
     * This is the contentletAPI which an application should use to do ALL
     * normal {@link ContentletAPI} logic.
     *
     * @return The {@link ContentletAPI} class.
     */
    public static FocalPointAPI getFocalPointAPI() {
        return (FocalPointAPI)getInstance(APIIndex.FOCAL_POINT_API);
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
	 * @deprecated
	 */
	@Deprecated
	public static com.dotmarketing.portlets.structure.business.FieldAPI getFieldAPI(){
		return (com.dotmarketing.portlets.structure.business.FieldAPI)getInstance(APIIndex.FIELD_API);
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
	 * Creates a single instance of the {@link ReindexQueueAPI} class.
	 *
	 * @return The {@link ReindexQueueAPI} class.
	 */
	public static ReindexQueueAPI getReindexQueueAPI(){
		return (ReindexQueueAPI) getInstance(APIIndex.REINDEX_QUEUE_API);
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
	 * Creates a single instance of the {@link MenuLinkAPI} class.
	 *
	 * @return The {@link MenuLinkAPI} class.
	 */
	public static MenuLinkAPI getMenuLinkAPI(){
		return (MenuLinkAPI) getInstance(APIIndex.MENULINK_API);
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
	 * Creates the {@link FileStorageAPI}
	 * @return FileStorageAPI
	 */
	public static FileStorageAPI getFileStorageAPI(){
		return (FileStorageAPI) getInstance(APIIndex.FILESTORAGE_API);
	}

	/**
	 * Creates the {@link FileStorageAPI}
	 * @return FileStorageAPI
	 */
	public static FileMetadataAPI getFileMetadataAPI(){
		return (FileMetadataAPI) getInstance(APIIndex.CONTENTLET_METADATA_API);
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
	 * Creates a single instance of the {@link LoginServiceAPI} class.
	 *
	 * @return The {@link LoginServiceAPI} class.
	 */
	public static LoginServiceAPI getLoginServiceAPI(){
		return (LoginServiceAPI) getInstance(APIIndex.LOGIN_SERVICE_API);
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
	 * @deprecated As of dotCMS 4.1.0, this API has been deprecated. From now on,
	 *             please use the {@link ContentTypeAPI} class via
	 *             {@link APILocator#getContentTypeAPI(User)} in order to interact
	 *             with Content Types.
	 */
	@Deprecated
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
	 * Creates a single instance of the {@link BrowserAPI} class.
	 *
	 * @return The {@link BrowserAPI} class.
	 */
	public static BrowserAPI getBrowserAPI() {
		return (BrowserAPI)getInstance(APIIndex.BROWSER_API);
	}

	public static TempFileAPI getTempFileAPI() {
	  return new TempFileAPI();
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

	/**
	 * Creates a single instance of the {@link ContentTypeAPI} class setup with the provided arguments
	 * 
	 * @param user
	 *
	 * @return The {@link ContentTypeAPI} class.
	 */
    public static ContentTypeAPI getContentTypeAPI(User user) {
    	return getContentTypeAPI(user, false);
    }

    /**
	 * Creates a single instance of the {@link ContentTypeAPI} class setup with the provided arguments
	 * 
	 * @param user
	 * @param respectFrontendRoles
	 *
	 * @return The {@link ContentTypeAPI} class.
	 */
    public static ContentTypeAPI getContentTypeAPI(User user, boolean respectFrontendRoles) {
    	return getAPILocatorInstance().getContentTypeAPIImpl(user, respectFrontendRoles);
	}

    public static MultiTreeAPI getMultiTreeAPI() {
        return (MultiTreeAPI) getInstance( APIIndex.MULTI_TREE_API );
    }

    @VisibleForTesting
    protected ContentTypeAPI getContentTypeAPIImpl(User user, boolean respectFrontendRoles) {
    	return new ContentTypeAPIImpl(user, respectFrontendRoles);
    }

    /**
     * Creates a single instance of the {@link FieldAPI} class.
     *
     * @return The {@link FieldAPI} class.
     */
    public static FieldAPI getContentTypeFieldAPI() {
		return new FieldAPIImpl();
	}

	/**
	 * Creates a single instance of the {@link com.dotcms.graphql.business.GraphqlAPI} class.
	 *
	 * @return The {@link com.dotcms.graphql.business.GraphqlAPI} class.
	 */
	public static GraphqlAPI getGraphqlAPI() {
		return (GraphqlAPI) getInstance(APIIndex.GRAPHQL_API);
	}

    /**
     * Returns the dotCMS System User object.
     * 
     * @return The System {@link User}.
     */
    public static User systemUser()  {
      try{
        return getUserAPI().getSystemUser();
      }
      catch(Exception e){
        throw new DotStateException(e);
      }
	}

    /**
     * Returns the dotCMS System Host object.
     * 
     * @return The System {@link Host}.
     */
    public static Host systemHost()  {
      try{
        return getHostAPI().findSystemHost();
      }
      catch(Exception e){
        throw new DotStateException(e);
      }
	}

	/**
	 * Returns the default user's time zone that is the default company's time zone
	 * @return The system {@link TimeZone}
	 */
	public static TimeZone systemTimeZone(){
		return getCompanyAPI().getDefaultCompany().getTimeZone();
	}

    /**
     * Creates a single instance of the {@link TreeableAPI} class.
     *
     * @return The {@link TreeableAPI} class.
     */
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
	 * Returns the File Watcher API that allows watch events over directories or files.
	 *
	 * @return An instance of the {@link FileWatcherAPI}.
	 */
	public static FileWatcherAPI getFileWatcherAPI() {
		return (FileWatcherAPI) getInstance(APIIndex.FILE_WATCHER_API);
	}
	
	/**
	 * Creates a single instance of the {@link VanityUrlAPI}
	 *
	 * @return The {@link VanityUrlAPI} class.
	 */
	public static VanityUrlAPI getVanityUrlAPI() {
		return (VanityUrlAPI) getInstance(APIIndex.VANITY_URLS_API);
	}

	/**
     * Creates a single instance of the {@link KeyValueAPI}
     *
     * @return The {@link KeyValueAPI} class.
     */
    public static KeyValueAPI getKeyValueAPI() {
        return (KeyValueAPI) getInstance(APIIndex.KEY_VALUE_API);
    }

	/**
	 * Creates a single instance of the {@link LocalSystemEventsAPI}
	 *
	 * @return The {@link LocalSystemEventsAPI} class.
	 */
	public static LocalSystemEventsAPI getLocalSystemEventsAPI() {
		return (LocalSystemEventsAPI) getInstance(APIIndex.LOCAL_SYSTEM_EVENTS_API);
	}

    /**
     * Creates a single instance of the {@link LanguageVariableAPI}
     *
     * @return The {@link LanguageVariableAPI} class.
     */
    public static LanguageVariableAPI getLanguageVariableAPI() {
        return (LanguageVariableAPI) getInstance(APIIndex.LANGUAGE_VARIABLE_API);
    }

	/**
	 * Creates a single instance of the {@link LanguageVariableAPI}
	 *
	 * @return The {@link LanguageVariableAPI} class.
	 */
	public static HTMLPageAssetRenderedAPI getHTMLPageAssetRenderedAPI() {
		return (HTMLPageAssetRenderedAPI) getInstance(APIIndex.HTMLPAGE_ASSET_RENDERED_API);
	}

	/**
	 * Creates a single instance of the {@link ThemeAPI} class.
	 *
	 * @return The {@link ThemeAPI} class.
	 */
	public static ThemeAPI getThemeAPI() {
		return (ThemeAPI) getInstance(APIIndex.THEME_API);
	}
	
    /**
     * Creates a single instance of the {@link ApiTokenAPI} class.
     *
     * @return The {@link ApiTokenAPI} class.
     */
    public static ApiTokenAPI getApiTokenAPI() {
        return (ApiTokenAPI) getInstance(APIIndex.API_TOKEN_API);
    }

	/**
	 * Creates a single instance of the {@link ThemeAPI} class.
	 *
	 * @return The {@link ThemeAPI} class.
	 */
	public static URLMapAPIImpl getURLMapAPI() {
		return (URLMapAPIImpl) getInstance(APIIndex.URLMAP_API);
	}

	/**
	 * Creates a single instance of the {@link PermissionAPI} class.
	 *
	 * @return The {@link PermissionAPI} class.
	 */
	public static ContentTypeFieldLayoutAPI getContentTypeFieldLayoutAPI() {
		return (ContentTypeFieldLayoutAPI)getInstance(APIIndex.CONTENT_TYPE_FIELD_LAYOUT_API);
	}

	/**
	 * Creates a single instance of the {@link PublishAuditAPIImpl} class.
	 *
	 * @return The {@link PublishAuditAPIImpl} class.
	 */
	public static PublishAuditAPI getPublishAuditAPI() {
		return (PublishAuditAPI) getInstance(APIIndex.PUBLISH_AUDIT_API);
	}

	/**
	 * Single point of entry to the service integration api
	 * @return The {@link AppsAPI} class.
	 */
	public static AppsAPI getAppsAPI(){
	   return (AppsAPI) getInstance(APIIndex.APPS_API);
	}

	/**
	 * Single point of entry to the dot asset api
	 * @return The {@link DotAssetAPI} class.
	 */
	public static DotAssetAPI getDotAssetAPI(){
		return (DotAssetAPI) getInstance(APIIndex.DOT_ASSET_API);
	}

	/**
	 * Creates a single instance of the {@link com.dotcms.device.DeviceAPI} class.
	 *
	 * @return The {@link com.dotcms.device.DeviceAPI} class.
	 */
	public static DeviceAPI getDeviceAPI(){
		return (DeviceAPI) getInstance(APIIndex.DEVICE_API);
	}

	/**
	 * Creates a single instance of the {@link com.dotmarketing.business.DeterministicIdentifierAPI} class.
	 * @return The {@link com.dotmarketing.business.DeterministicIdentifierAPI} class.
	 */
	public static DeterministicIdentifierAPI getDeterministicIdentifierAPI(){
		return (DeterministicIdentifierAPI) getInstance(APIIndex.DETERMINISTIC_IDENTIFIER_API);
	}

	/**
	 * Creates a single instance of the {@link ContentletJsonAPI} class.
	 * @return the instance
	 */
	public static ContentletJsonAPI getContentletJsonAPI(){
		return (ContentletJsonAPI) getInstance(APIIndex.CONTENTLET_JSON_API);
	}

	/**
	 * Creates a single instance of the {@link VariantAPI} class.
	 * @return the instance
	 */
	public static VariantAPI getVariantAPI() {
		return (VariantAPI) getInstance(APIIndex.VARIANT_API);
	}

	/**
	 * Creates a single instance of the {@link com.dotcms.experiments.business.ExperimentsAPI} class.
	 * @return the instance
	 */
	public static ExperimentsAPI getExperimentsAPI(){
		return (ExperimentsAPI) getInstance(APIIndex.EXPERIMENTS_API);
	}

	/**
	 * Creates a single instance of the {@link com.dotcms.analytics.bayesian.BayesianAPI} class.
	 * @return
	 */
	public static BayesianAPI getBayesianAPI(){
		return (BayesianAPI) getInstance(APIIndex.BAYESIAN_API);
	}

	/**
	 * Creates a single instance of the {@link com.dotcms.analytics.bayesian.BayesianAPI} class.
	 * @return
	 */
	public static AnalyticsAPI getAnalyticsAPI() {
		return (AnalyticsAPI) getInstance(APIIndex.ANALYTICS_API);
	}

	/**
	 * Returns a single instance of the {@link ContentTypeDestroyAPI} class.
	 *
	 * @return The {@link ContentTypeDestroyAPI} instance.
	 */
	public static ContentTypeDestroyAPI getContentTypeDestroyAPI() {
		return (ContentTypeDestroyAPI) getInstance(APIIndex.CONTENT_TYPE_DESTROY_API);
	}

	/**
	 * Returns the System Facade API
	 * @return SystemAPI
	 */
	public static SystemAPI getSystemAPI() {
		return (SystemAPI) getInstance(APIIndex.SYSTEM_API);
	}

	/**
	 * Returns a singleton instance of the {@link ACheckerAPI} class.
	 *
	 * @return The {@link ACheckerAPI} instance.
	 */
	public static ACheckerAPI getACheckerAPI() {
		return (ACheckerAPI) getInstance(APIIndex.ACHECKER_API);
	}

	/**
	 * Returns a singleton instance of the {@link ContentAnalyticsAPI} class.
	 *
	 * @return The {@link ContentAnalyticsAPI} instance.
	 */
	public static ContentAnalyticsAPI getContentAnalyticsAPI() {
		return (ContentAnalyticsAPI) getInstance(APIIndex.CONTENT_ANALYTICS_API);
	}

	/**
	 * Returns a single instance of the {@link MetricsAPI} class via CDI.
	 *
	 * @return The {@link MetricsAPI} instance.
	 */
	public static MetricsAPI getMetricsAPI() {
		return CDIUtils.getBeanThrows(MetricsAPI.class);
	}

	/**
	 * Returns the Health Service for programmatic access to health check status
	 * from non-CDI aware code. This provides convenient methods for querying
	 * health status, individual checks, and overall system health.
	 * 
	 * @return The {@link HealthService} CDI bean instance
	 */
	public static HealthService getHealthService() {
		return CDIUtils.getBeanThrows(HealthService.class);
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

		APILocator apiLocatorInstance = getAPILocatorInstance();

		Object serviceRef = apiLocatorInstance.getServiceInstance(index);

		if( Logger.isDebugEnabled(APILocator.class) ) {
			Logger.debug(APILocator.class, apiLocatorInstance.audit(index));
		}

		return serviceRef;
	}

	/**
	 * Creates a unique instance of this API Locator.
	 * 
	 * @return A new instance of the {@link APILocator}.
	 */
	private static APILocator getAPILocatorInstance() {
		if(instance == null){
			init();
			if(instance == null){
				Logger.fatal(APILocator.class,"CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
				throw new DotRuntimeException("CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
			}
		}
		return instance;
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
	REINDEX_QUEUE_API,
	EVENT_API,
	EVENT_RECURRENCE_API,
	PERMISSION_API,
	ROLE_API,
	USER_API,
	LOGIN_AS_USER_API,
	LOGIN_SERVICE_API,
	RELATIONSHIP_API,
	FIELD_API,
	IDENTIFIER_API,
	PORTLET_API,
	WIDGET_API,
	CHAIN_API,
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
	SECURITY_LOGGER_API,
	FILE_WATCHER_API,
	KEY_VALUE_API,
	LOCAL_SYSTEM_EVENTS_API,
	LANGUAGE_VARIABLE_API,
	VANITY_URLS_API,
	MULTI_TREE_API,
	HTMLPAGE_ASSET_RENDERED_API,
	THEME_API,
	API_TOKEN_API,
	GRAPHQL_API,
	URLMAP_API,
	CONTENT_TYPE_FIELD_LAYOUT_API,
	PUBLISH_AUDIT_API,
	FOCAL_POINT_API,
	APPS_API,
	DOT_ASSET_API,
	BROWSER_API,
	FILESTORAGE_API,
	CONTENTLET_METADATA_API,
	DEVICE_API,
	DETERMINISTIC_IDENTIFIER_API,
	CONTENTLET_JSON_API,
	STORY_BLOCK_API,
	ARTIFICIAL_INTELLIGENCE_API,
	VARIANT_API,
	EXPERIMENTS_API,
	BAYESIAN_API,
	ANALYTICS_API,
	CONTENT_TYPE_DESTROY_API,
	SYSTEM_API,
	ACHECKER_API,
	CONTENT_ANALYTICS_API,
	JOB_QUEUE_MANAGER_API,
	ANALYTICS_CUSTOM_ATTRIBUTE_API;

	Object create() {
		switch(this) {
    		case PERMISSION_API: return new PermissionBitAPIImpl();
    		case ROLE_API: return new RoleAPIImpl();
    		case USER_API: return new UserAPIImpl();
    		case LOGIN_AS_USER_API: return LoginAsAPIImpl.getInstance();
    		case LOGIN_SERVICE_API: return LoginServiceAPIFactory.getInstance().getLoginService();
    		case EVENT_API: return new EventAPIImpl();
    		case CATEGORY_API: return new CategoryAPIImpl();
    		case CONTENTLET_API: return new  ESContentletAPIImpl();
    		case CONTENTLET_API_INTERCEPTER: return new ContentletAPIInterceptor();
    		case RELATIONSHIP_API: return new RelationshipAPIImpl();
    		case IDENTIFIER_API: return new IdentifierAPIImpl();
    		case FIELD_API: return new com.dotmarketing.portlets.structure.business.FieldAPIImpl();
    		case PORTLET_API: return new PortletAPIImpl();
    		case WIDGET_API: return new WidgetAPIImpl();
    		case PLUGIN_API: return new PluginAPIImpl();
    		case LANGUAGE_API: return new LanguageAPIImpl();
    		case REINDEX_QUEUE_API : return new ReindexQueueAPIImpl();
    		case TEMPLATE_API : return new TemplateAPIImpl();
    		case FOLDER_API: return new FolderAPIImpl();
    		case CONTAINER_API: return new ContainerAPIImpl();
    		case USER_PROXY_API : return new UserProxyAPIImpl();
    		case HOST_API : return new HostAPIImpl();
    		case LAYOUT_API : return new LayoutAPIImpl();
    		case HOST_VARIABLE_API : return new HostVariableAPIImpl();
    		case FORM_API: return new FormAPIImpl();
    		case MENULINK_API: return new MenuLinkAPIImpl();
    		case DASHBOARD_API: return new DashboardAPIImpl();
    		case SITESEARCH_API: return new ESSiteSearchAPI();
    		case FILEASSET_API: return new FileAssetAPIImpl();
    		case VERSIONABLE_API: return new VersionableAPIImpl();
    		case WORKFLOW_API : return new WorkflowAPIImpl();
    		case CACHE_PROVIDER_API : return new CacheProviderAPIImpl();
    		case TAG_API: return new TagAPIImpl();
    		case INDICIES_API: return new IndiciesAPIImpl();
    		case CONTENLET_INDEX_API: return new ContentletIndexAPIImpl();
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
    		case SHORTY_ID_API: return Config.getBooleanProperty("dotshortyapi_use_legacy", false)? new LegacyShortyIdAPIImpl(): new ShortyIdAPIImpl();
    		case SYSTEM_EVENTS_API: return SystemEventsFactory.getInstance().getSystemEventsAPI();
    		case WEB_SOCKET_CONTAINER_API:return WebSocketContainerAPIFactory.getInstance().getWebSocketContainerAPI();
    		case COMPANY_API: return CompanyAPIFactory.getInstance().getCompanyAPI();
    		case SECURITY_LOGGER_API: return SecurityLoggerServiceAPIFactory.getInstance().getSecurityLoggerAPI();
    		case FILE_WATCHER_API: return createFileWatcherAPI();
    		case VANITY_URLS_API: return createVanityUrlAPI();
			case KEY_VALUE_API: return new KeyValueAPIImpl();
    		case LANGUAGE_VARIABLE_API: return new LanguageVariableAPIImpl();
			case LOCAL_SYSTEM_EVENTS_API: return LocalSystemEventsAPIFactory.getInstance().getLocalSystemEventsAPI();
			case MULTI_TREE_API: return new MultiTreeAPIImpl();
			case HTMLPAGE_ASSET_RENDERED_API: return new HTMLPageAssetRenderedAPIImpl();
			case THEME_API: return new ThemeAPIImpl();
			case GRAPHQL_API: return  new GraphqlAPIImpl();
	        case API_TOKEN_API: return new ApiTokenAPI();
			case URLMAP_API: return new URLMapAPIImpl();
			case CONTENT_TYPE_FIELD_LAYOUT_API: return new ContentTypeFieldLayoutAPIImpl();
			case PUBLISH_AUDIT_API: return PublishAuditAPIImpl.getInstance();
			case FOCAL_POINT_API: return new FocalPointAPIImpl();
			case APPS_API: return AppsAPI.INSTANCE.get();
			case DOT_ASSET_API: return new DotAssetAPIImpl();
			case BROWSER_API: return new BrowserAPIImpl();
			case FILESTORAGE_API: return new FileStorageAPIImpl();
			case CONTENTLET_METADATA_API: return new FileMetadataAPIImpl();
			case DEVICE_API: return new DeviceAPIImpl();
			case DETERMINISTIC_IDENTIFIER_API: return new DeterministicIdentifierAPIImpl();
			case CONTENTLET_JSON_API: return new ContentletJsonAPIImpl();
			case STORY_BLOCK_API: return new StoryBlockAPIImpl();
			case VARIANT_API: return new VariantAPIImpl();
			case EXPERIMENTS_API: return new ExperimentsAPIImpl();
			case BAYESIAN_API: return new BayesianAPIImpl();
			case ANALYTICS_API: return new AnalyticsAPIImpl();
			case CONTENT_TYPE_DESTROY_API: return new ContentTypeDestroyAPIImpl();
			case SYSTEM_API: return new SystemAPIImpl();
			case ARTIFICIAL_INTELLIGENCE_API: return new DotAIAPIFacadeImpl();
			case ACHECKER_API: return new ACheckerAPIImpl();
			case CONTENT_ANALYTICS_API: return CDIUtils.getBeanThrows(ContentAnalyticsAPI.class);
			case JOB_QUEUE_MANAGER_API: return CDIUtils.getBeanThrows(JobQueueManagerAPI.class);
			case ANALYTICS_CUSTOM_ATTRIBUTE_API: return new CustomAttributeAPIImpl();
		}
		throw new AssertionError("Unknown API index: " + this);
	}

	/**
	 * Correctly initializes a new single instance of the {@link VanityUrlAPI}.
	 *
	 * @return The {@link VanityUrlAPI}.
	 */
	private static VanityUrlAPI createVanityUrlAPI () {

		return new VanityUrlAPIImpl();
	}

    /**
     * Correctly initializes a new single instance of the {@link FileWatcherAPI}.
     * 
     * @return The {@link FileWatcherAPI}.
     */
	private static FileWatcherAPI createFileWatcherAPI () {

		FileWatcherAPIImpl fileWatcherAPI = null;

		try {

			fileWatcherAPI = new FileWatcherAPIImpl();
			APILocator.addCloseableResource(fileWatcherAPI);
		} catch (IOException e) {
			Logger.error(APILocator.class, "The File Watcher API couldn't be created", e);
		}

		return fileWatcherAPI;
	} // createFileWatcherAPI.

}
