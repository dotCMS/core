package com.dotmarketing.business;

import com.dotcms.analytics.attributes.CustomAttributeFactory;
import com.dotcms.analytics.attributes.CustomAttributeFactoryImpl;
import com.dotcms.analytics.content.ContentAnalyticsFactory;
import com.dotcms.business.SystemTableFactory;
import com.dotcms.business.SystemTableFactoryImpl;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.cluster.business.ServerFactory;
import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.content.elasticsearch.business.IndiciesFactory;
import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.business.RelationshipFactory;
import com.dotcms.contenttype.business.RelationshipFactoryImpl;
import com.dotcms.cube.CubeJSClientFactory;
import com.dotcms.cube.CubeJSClientFactoryImpl;
import com.dotcms.enterprise.DashboardProxy;
import com.dotcms.enterprise.RulesFactoryProxy;
import com.dotcms.enterprise.ServerActionFactoryImplProxy;
import com.dotcms.enterprise.cluster.ServerFactoryImpl;
import com.dotcms.enterprise.cluster.action.business.ServerActionFactory;
import com.dotcms.enterprise.linkchecker.LinkCheckerFactoryImpl;
import com.dotcms.enterprise.rules.RulesFactory;
import com.dotcms.experiments.business.ExperimentsFactory;
import com.dotcms.experiments.business.ExperimentsFactoryImpl;
import com.dotcms.languagevariable.business.LanguageVariableFactory;
import com.dotcms.languagevariable.business.LanguageVariableFactoryImpl;
import com.dotcms.notifications.business.NotificationFactory;
import com.dotcms.notifications.business.NotificationFactoryImpl;
import com.dotcms.publisher.assets.business.PushedAssetsFactory;
import com.dotcms.publisher.assets.business.PushedAssetsFactoryImpl;
import com.dotcms.publisher.bundle.business.BundleFactory;
import com.dotcms.publisher.bundle.business.BundleFactoryImpl;
import com.dotcms.publisher.endpoint.business.PublishingEndPointFactory;
import com.dotcms.publisher.endpoint.business.PublishingEndPointFactoryImpl;
import com.dotcms.publisher.environment.business.EnvironmentFactory;
import com.dotcms.publisher.environment.business.EnvironmentFactoryImpl;
import com.dotcms.variant.VariantFactory;
import com.dotcms.variant.VariantFactoryImpl;
import com.dotmarketing.business.portal.PortletFactory;
import com.dotmarketing.business.portal.PortletFactoryImpl;
import com.dotmarketing.common.reindex.ReindexQueueFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.plugin.business.PluginFactory;
import com.dotmarketing.plugin.business.PluginFactoryDBImpl;
import com.dotmarketing.portlets.calendar.business.EventFactory;
import com.dotmarketing.portlets.calendar.business.EventFactoryImpl;
import com.dotmarketing.portlets.categories.business.CategoryFactory;
import com.dotmarketing.portlets.categories.business.CategoryFactoryImpl;
import com.dotmarketing.portlets.containers.business.ContainerFactory;
import com.dotmarketing.portlets.containers.business.ContainerFactoryImpl;
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
import com.dotmarketing.portlets.contentlet.business.HostFactory;
import com.dotmarketing.portlets.contentlet.business.HostFactoryImpl;
import com.dotmarketing.portlets.dashboard.business.DashboardFactory;
import com.dotmarketing.portlets.fileassets.business.FileAssetFactory;
import com.dotmarketing.portlets.fileassets.business.FileAssetFactoryImpl;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.folders.business.FolderFactoryImpl;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableFactory;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableFactoryImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageFactory;
import com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryImpl;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerFactory;
import com.dotmarketing.portlets.links.business.MenuLinkFactory;
import com.dotmarketing.portlets.links.business.MenuLinkFactoryImpl;
import com.dotmarketing.portlets.personas.business.PersonaFactory;
import com.dotmarketing.portlets.personas.business.PersonaFactoryImpl;
import com.dotmarketing.portlets.templates.business.TemplateFactory;
import com.dotmarketing.portlets.templates.business.TemplateFactoryImpl;
import com.dotmarketing.portlets.workflows.business.WorkFlowFactory;
import com.dotmarketing.portlets.workflows.business.WorkflowFactoryImpl;
import com.dotmarketing.tag.business.TagFactory;
import com.dotmarketing.tag.business.TagFactoryImpl;
import com.dotmarketing.util.Logger;

/**
 * FactoryLocator is a factory method to get single(ton) service objects.
 * This is a kind of implementation, and there may be others.
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.6
 * @since 1.6
 */

public class FactoryLocator extends Locator<FactoryIndex>{

	private static FactoryLocator instance;

	FactoryLocator() {
		super();
	}

	public synchronized static void init(){
		if(instance != null)
			return;
		instance = new FactoryLocator();
	}

	public static PermissionFactory getPermissionFactory() {
		return (PermissionFactory)getInstance(FactoryIndex.PERMISSION_FACTORY);
	}

    public static EventFactory getEventFactory() {
        return (EventFactory)getInstance(FactoryIndex.CALENDAR_EVENT_FACTORY);
    }

    public static CategoryFactory getCategoryFactory() {
        return (CategoryFactory)getInstance(FactoryIndex.CATEGORY_FACTORY);
    }
    
    public static RelationshipFactory getRelationshipFactory() {
      return (RelationshipFactory)getInstance(FactoryIndex.RELATIONSHIP_FACTORY);
    }
    public static ContentletFactory getContentletFactory(){
    	return (ContentletFactory)getInstance(FactoryIndex.CONTENTLET_FACTORY);
    }


    public static PluginFactory getPluginFactory(){
    	return (PluginFactory)getInstance(FactoryIndex.PLUGIN_FACTORY);
    }

    public static LanguageFactory getLanguageFactory(){
    	return (LanguageFactory)getInstance(FactoryIndex.LANGUAGE_FACTORY);
    }

    public static ReindexQueueFactory getReindexQueueFactory() {
        return (ReindexQueueFactory) getInstance(FactoryIndex.DISTRIBUTED_JOURNAL_FACTORY);
    }

    public static UserFactory getUserFactory(){
    	return (UserFactory)getInstance(FactoryIndex.USER_FACTORY);
    }

    /**
     * @deprecated Use {@link FactoryLocator#getUserFactory()} instead
     * @return
     */
    @Deprecated
    public static UserFactoryLiferay getUserFactoryLiferay(){
        return (UserFactoryLiferay) getInstance(FactoryIndex.USER_FACTORY_LIFERAY);
    }

       public static TemplateFactory getTemplateFactory(){
    	return (TemplateFactory) getInstance(FactoryIndex.TEMPLATE_FACTORY);
    }



    public static RoleFactory getRoleFactory(){
    	return (RoleFactory) getInstance(FactoryIndex.ROLE_FACTORY);
    }

    public static HostVariableFactory getHostVariableFactory(){
    	return (HostVariableFactory) getInstance(FactoryIndex.HOST_VARIABLE_FACTORY);
    }

    public static LayoutFactory getLayoutFactory(){
    	return (LayoutFactory) getInstance(FactoryIndex.LAYOUT_FACTORY);
    }

    public static MenuLinkFactory getMenuLinkFactory(){
    	return (MenuLinkFactory) getInstance(FactoryIndex.MENULINK_FACTORY);
    }

    public static ContainerFactory getContainerFactory(){
    	return (ContainerFactory) getInstance(FactoryIndex.CONTAINER_FACTORY);
    }

    public static DashboardFactory getDashboardFactory(){
    	return (DashboardFactory) getInstance(FactoryIndex.DASHBOARD_FACTORY);
    }
    public static IdentifierFactory getIdentifierFactory(){
    	return (IdentifierFactory) getInstance(FactoryIndex.IDENTIFIER_FACTORY);
    }
    public static VersionableFactory getVersionableFactory(){
    	return (VersionableFactory) getInstance(FactoryIndex.VERSIONABLE_FACTORY);
    }
    public static FolderFactory getFolderFactory(){
    	return (FolderFactory) getInstance(FactoryIndex.FOLDER_FACTORY);
    }
    public static WorkFlowFactory getWorkFlowFactory(){
    	return (WorkFlowFactory) getInstance(FactoryIndex.WORKFLOWS_FACTORY);
    }
    public static IndiciesFactory getIndiciesFactory(){
        return (IndiciesFactory) getInstance(FactoryIndex.INDICIES_FACTORY);
    }

    public static LinkCheckerFactory getLinkCheckerFactory() {
        return (LinkCheckerFactory) getInstance(FactoryIndex.LINKCHECKER_FACTORY);
    }

    public static PublishingEndPointFactory getPublisherEndPointFactory(){
    	return (PublishingEndPointFactory) getInstance(FactoryIndex.PUBLISHER_END_POINT_FACTORY);

    }

    public static EnvironmentFactory getEnvironmentFactory(){
    	return (EnvironmentFactory) getInstance(FactoryIndex.ENVIRONMENT_FACTORY);
    }

    public static BundleFactory getBundleFactory(){
    	return (BundleFactory) getInstance(FactoryIndex.BUNDLE_FACTORY);
    }

    public static PushedAssetsFactory getPushedAssetsFactory(){
    	return (PushedAssetsFactory) getInstance(FactoryIndex.PUSHED_ASSETS_FACTORY);
    }

    public static ServerFactory getServerFactory(){
        return (ServerFactory) getInstance(FactoryIndex.SERVER_FACTORY);
    }

    public static NotificationFactory getNotificationFactory(){
        return (NotificationFactory) getInstance(FactoryIndex.NOTIFICATION_FACTORY);
    }
    
    public static ServerActionFactory getServerActionFactory(){
        return (ServerActionFactory) getInstance(FactoryIndex.SERVER_ACTION_FACTORY);
    }

    public static RulesFactory getRulesFactory(){
        return (RulesFactory) getInstance(FactoryIndex.RULES_FACTORY);
    }

    public static TagFactory getTagFactory(){
        return (TagFactory) getInstance(FactoryIndex.TAG_FACTORY);
    }
    
    public static PersonaFactory getPersonaFactory(){
        return (PersonaFactory) getInstance(FactoryIndex.PERSONA_FACTORY);
    }
    public static ContentTypeFactory getContentTypeFactory(){
        return (ContentTypeFactory)  new ContentTypeFactoryImpl();
    }
    public static FieldFactory getFieldFactory(){
        return (FieldFactory)  new FieldFactoryImpl();
    }

    public static FileAssetFactory getFileAssetFactory() {
        return (FileAssetFactory)getInstance(FactoryIndex.FileAsset_Factory);
    }

    /**
     * Returns the Factory object that handles operations related to Sites in dotCMS.
     *
     * @return An instance of the {@link HostFactory} object.
     */
    public static HostFactory getHostFactory() {
        return (HostFactory) getInstance(FactoryIndex.HOST_FACTORY);
    }

    /**
     * Returns the Factory object that handles operations related to {@link com.dotcms.variant.model.Variant} in dotCMS.
     * Returns the Factory object that handles operations related to {@link Variant} in dotCMS.
     *
     * @return An instance of the {@link VariantFactory} object.
     */
    public static VariantFactory getVariantFactory() {
        return (VariantFactory) getInstance(FactoryIndex.VARIANT_FACTORY);
    }

    public static ExperimentsFactory getExperimentsFactory() {
        return (ExperimentsFactory) getInstance(FactoryIndex.EXPERIMENTS_FACTORY);
    }

    public static SystemTableFactory getSystemTableFactory() {
        return (SystemTableFactory) getInstance(FactoryIndex.SYSTEM_TABLE_FACTORY);
    }

    public static CubeJSClientFactory getCubeJSClientFactory() {
        return CDIUtils.getBeanThrows(CubeJSClientFactory.class);
    }

    /**
     * Returns the Factory object that handles operations related to {@link LanguageVariable} in dotCMS.
     * @return
     */
    public static LanguageVariableFactory getLanguageVariableFactory() {
        return (LanguageVariableFactory) getInstance(FactoryIndex.LANGUAGE_VARIABLE_FACTORY);
    }

    public static CustomAttributeFactory getAnalyticsCustomAttributeFactory() {
        return (CustomAttributeFactory) getInstance(FactoryIndex.ANALYTICS_CUSTOM_ATTRIBUTES_FACTORY);
    }

    /**
     * Returns the Factory object that handles operations related to {@link ContentAnalyticsFactory}
     * in dotCMS.
     *
     * @return An instance of the {@link ContentAnalyticsFactory} object.
     */
    public static ContentAnalyticsFactory getContentAnalyticsFactory() {
        return CDIUtils.getBeanThrows(ContentAnalyticsFactory.class);
    }

    /**
     * Returns a singleton of the {@link PortletFactory} class.
     *
     * @return The {@link PortletFactory} singleton.
     */
    public static PortletFactory getPortletFactory() {
        return (PortletFactory) getInstance(FactoryIndex.PORTLET_FACTORY);
    }

    private static Object getInstance(FactoryIndex index) {

		if(instance == null){
			init();
			if(instance == null){
				Logger.fatal(FactoryLocator.class,"CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
				throw new DotRuntimeException("CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
			}
		}

		Object serviceRef = instance.getServiceInstance(index);

		Logger.debug(FactoryLocator.class, instance.audit(index));

		return serviceRef;

	 }

	@Override
	protected Object createService(FactoryIndex enumObj) {
		return enumObj.create();
	}

	@Override
	protected Locator<FactoryIndex> getLocatorInstance() {
		return instance;
	}

}

enum FactoryIndex
{
	PERMISSION_FACTORY,
	CALENDAR_EVENT_FACTORY,
	CALENDAR_EVENT_RECURRENCE_FACTORY,
	CATEGORY_FACTORY,
	CONTENTLET_FACTORY,
	DISTRIBUTED_JOURNAL_FACTORY,
	PLUGIN_FACTORY,
	LANGUAGE_FACTORY,
	USER_FACTORY,
    USER_FACTORY_LIFERAY,
	CHAIN_FACTORY,
	USER_PROXY_FACTORY,
	TEMPLATE_FACTORY,
	ROLE_FACTORY,
	LAYOUT_FACTORY,
	HOST_VARIABLE_FACTORY,
	HOST_FACTORY,
	FILE_FACTORY,
	HTMLPAGE_FACTORY,
	MENULINK_FACTORY,
	CONTAINER_FACTORY,
	IDENTIFIER_FACTORY,
	VERSIONABLE_FACTORY,
	FOLDER_FACTORY,
	DASHBOARD_FACTORY,
	WORKFLOWS_FACTORY,
	INDICIES_FACTORY,
	LINKCHECKER_FACTORY,
	PUBLISHER_END_POINT_FACTORY,
	ENVIRONMENT_FACTORY,
	BUNDLE_FACTORY,
	PUSHED_ASSETS_FACTORY,
	SERVER_FACTORY,
	NOTIFICATION_FACTORY, 
	SERVER_ACTION_FACTORY,
	RULES_FACTORY,
	TAG_FACTORY,
	PERSONA_FACTORY,
	CONTENTTYPE_FACTORY_2,
	RELATIONSHIP_FACTORY,
	FIELD_FACTORY_2,
    FileAsset_Factory,
    VARIANT_FACTORY,
    EXPERIMENTS_FACTORY,
    SYSTEM_TABLE_FACTORY,
    LANGUAGE_VARIABLE_FACTORY,
    PORTLET_FACTORY,
    ANALYTICS_CUSTOM_ATTRIBUTES_FACTORY;

	Object create() {
		switch(this) {
			case PERMISSION_FACTORY: return new PermissionBitFactoryImpl(CacheLocator.getPermissionCache());
            case CALENDAR_EVENT_FACTORY: return new EventFactoryImpl();
            case CATEGORY_FACTORY: return new CategoryFactoryImpl();
            case CONTENTLET_FACTORY: return new ESContentFactoryImpl();
            case PLUGIN_FACTORY: return new PluginFactoryDBImpl();
            case LANGUAGE_FACTORY: return new LanguageFactoryImpl();
            case DISTRIBUTED_JOURNAL_FACTORY: return new ReindexQueueFactory();
            case USER_FACTORY : return new UserFactoryImpl();
            case USER_FACTORY_LIFERAY : return new UserFactoryLiferayImpl();
            case TEMPLATE_FACTORY: return new TemplateFactoryImpl();
            case HOST_VARIABLE_FACTORY: return new HostVariableFactoryImpl();
            case LAYOUT_FACTORY : return new LayoutFactoryImpl();
            case ROLE_FACTORY : return new RoleFactoryImpl();
            case MENULINK_FACTORY : return new MenuLinkFactoryImpl();
            case CONTAINER_FACTORY : return new ContainerFactoryImpl();
            case DASHBOARD_FACTORY : return DashboardProxy.getDashboardFactory();
            case IDENTIFIER_FACTORY : return new IdentifierFactoryImpl();
            case VERSIONABLE_FACTORY : return new VersionableFactoryImpl();
            case FOLDER_FACTORY : return new FolderFactoryImpl();
            case WORKFLOWS_FACTORY :return new WorkflowFactoryImpl();
            case INDICIES_FACTORY: return new IndiciesFactory();
            case LINKCHECKER_FACTORY: return new LinkCheckerFactoryImpl();
            case PUBLISHER_END_POINT_FACTORY: return new PublishingEndPointFactoryImpl();
            case ENVIRONMENT_FACTORY: return new EnvironmentFactoryImpl();
            case BUNDLE_FACTORY: return new BundleFactoryImpl();
            case PUSHED_ASSETS_FACTORY: return new PushedAssetsFactoryImpl();
            case SERVER_FACTORY: return new ServerFactoryImpl();
            case NOTIFICATION_FACTORY: return new NotificationFactoryImpl();
            case SERVER_ACTION_FACTORY: return new ServerActionFactoryImplProxy();
            case RULES_FACTORY: return new RulesFactoryProxy();
            case PERSONA_FACTORY: return new PersonaFactoryImpl();
            case RELATIONSHIP_FACTORY: return RelationshipFactoryImpl.instance();
            case TAG_FACTORY: return new TagFactoryImpl();
            case FileAsset_Factory: return new FileAssetFactoryImpl();
            case HOST_FACTORY : return new HostFactoryImpl();
            case VARIANT_FACTORY : return new VariantFactoryImpl();
            case EXPERIMENTS_FACTORY: return new ExperimentsFactoryImpl();
            case SYSTEM_TABLE_FACTORY: return new SystemTableFactoryImpl();
            case LANGUAGE_VARIABLE_FACTORY: return new LanguageVariableFactoryImpl();
            case PORTLET_FACTORY: return new PortletFactoryImpl();
            case ANALYTICS_CUSTOM_ATTRIBUTES_FACTORY: return new CustomAttributeFactoryImpl();
		}
		throw new AssertionError("Unknown Factory Index: " + this);
	}

}
