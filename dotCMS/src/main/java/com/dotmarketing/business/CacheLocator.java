package com.dotmarketing.business;

import com.dotcms.auth.providers.jwt.factories.ApiTokenCache;
import com.dotcms.business.SystemCache;
import com.dotcms.cache.KeyValueCache;
import com.dotcms.cache.KeyValueCacheImpl;
import com.dotcms.cache.VanityUrlCache;
import com.dotcms.cache.VanityUrlCacheImpl;
import com.dotcms.content.elasticsearch.business.IndiciesCache;
import com.dotcms.content.elasticsearch.business.IndiciesCacheImpl;
import com.dotcms.contenttype.business.ContentTypeCache2;
import com.dotcms.contenttype.business.ContentTypeCache2Impl;
import com.dotcms.csspreproc.CSSCache;
import com.dotcms.csspreproc.CSSCacheImpl;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.notifications.business.NewNotificationCache;
import com.dotcms.notifications.business.NewNotificationCacheImpl;
import com.dotcms.publisher.assets.business.PushedAssetsCache;
import com.dotcms.publisher.assets.business.PushedAssetsCacheImpl;
import com.dotcms.publisher.endpoint.business.PublishingEndPointCache;
import com.dotcms.publisher.endpoint.business.PublishingEndPointCacheImpl;
import com.dotcms.rendering.velocity.services.DotResourceCache;
import com.dotcms.rendering.velocity.viewtools.navigation.NavToolCache;
import com.dotcms.rendering.velocity.viewtools.navigation.NavToolCacheImpl;
import com.dotcms.repackage.com.google.common.base.Optional;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.jgroups.NullTransport;
import com.dotmarketing.business.portal.PortletCache;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.cache.FolderCache;
import com.dotmarketing.cache.FolderCacheImpl;
import com.dotmarketing.cache.LegacyContentTypeCacheImpl;
import com.dotmarketing.cache.MultiTreeCache;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.logConsole.model.LogMapperCache;
import com.dotmarketing.logConsole.model.LogMapperCacheImpl;
import com.dotmarketing.plugin.business.PluginCache;
import com.dotmarketing.plugin.business.PluginCacheImpl;
import com.dotmarketing.portlets.categories.business.CategoryCache;
import com.dotmarketing.portlets.categories.business.CategoryCacheImpl;
import com.dotmarketing.portlets.containers.business.ContainerCache;
import com.dotmarketing.portlets.containers.business.ContainerCacheImpl;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.business.ContentletCacheImpl;
import com.dotmarketing.portlets.contentlet.business.HostCache;
import com.dotmarketing.portlets.contentlet.business.HostCacheImpl;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariablesCache;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariablesCacheImpl;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageCache;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageCacheImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageCache;
import com.dotmarketing.portlets.languagesmanager.business.LanguageCacheImpl;
import com.dotmarketing.portlets.links.business.MenuLinkCache;
import com.dotmarketing.portlets.links.business.MenuLinkCacheImpl;
import com.dotmarketing.portlets.rules.business.RulesCache;
import com.dotmarketing.portlets.rules.business.RulesCacheImpl;
import com.dotmarketing.portlets.rules.business.SiteVisitCache;
import com.dotmarketing.portlets.rules.business.SiteVisitCacheImpl;
import com.dotmarketing.portlets.structure.factories.RelationshipCache;
import com.dotmarketing.portlets.structure.factories.RelationshipCacheImpl;
import com.dotmarketing.portlets.templates.business.TemplateCache;
import com.dotmarketing.portlets.templates.business.TemplateCacheImpl;
import com.dotmarketing.portlets.workflows.business.WorkflowCache;
import com.dotmarketing.portlets.workflows.business.WorkflowCacheImpl;
import com.dotmarketing.tag.business.TagCache;
import com.dotmarketing.tag.business.TagCacheImpl;
import com.dotmarketing.tag.business.TagInodeCache;
import com.dotmarketing.tag.business.TagInodeCacheImpl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;



/**
 * FactoryLocator is a factory method to get single(ton) service objects.
 * This is a kind of implementation, and there may be others.
 * 
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.6
 * @since 1.6
 */
public class CacheLocator extends Locator<CacheIndex>{



	private static CacheLocator instance;
	private final DotCacheAdministrator adminCache;
    private CacheTransport eeTransport;
    private final CacheTransport nullTransport= new NullTransport();

	private CacheLocator(){
	    super();
		long start = System.currentTimeMillis();
		eeTransport = loadCacheTransport();
		adminCache = loadCacheAdministrator();
        adminCache.initProviders();
        
        System.setProperty(WebKeys.DOTCMS_STARTUP_TIME_CACHE, String.valueOf(System.currentTimeMillis() - start));
		/*
		Initializing the Cache Providers:

		 It needs to be initialized in a different call as the providers depend on the
		 license level, and the license level needs an already created instance of the CacheLocator
		 to work.
		 */
        instance=this;

	}
	
	@VisibleForTesting
    public CacheLocator(DotCacheAdministrator cacheAdministrator, CacheTransport transport) {
        super();
        long start = System.currentTimeMillis();
        this.eeTransport = transport;
        this.adminCache = cacheAdministrator;
        this.adminCache.initProviders();

        System.setProperty(WebKeys.DOTCMS_STARTUP_TIME_CACHE, String.valueOf(System.currentTimeMillis() - start));
        /*
         * Initializing the Cache Providers:
         * 
         * It needs to be initialized in a different call as the providers depend on the license level, and
         * the license level needs an already created instance of the CacheLocator to work.
         */

        instance = this;
    }
	
	
	
	
	private CacheTransport loadCacheTransport(){
        String cTransClass = Config.getStringProperty("CACHE_INVALIDATION_TRANSPORT_CLASS","com.dotmarketing.business.jgroups.JGroupsCacheTransport");
        return Try.of(()-> (CacheTransport)Class.forName(cTransClass).newInstance()).onFailure(e->Logger.warnAndDebug(CacheLocator.class, e)).getOrElse(nullTransport);

	    
	}
   private DotCacheAdministrator loadCacheAdministrator(){
       String clazz = Config.getStringProperty("cache.locator.class", ChainableCacheAdministratorImpl.class.getCanonicalName());
       
       Logger.info(CacheLocator.class, "loading cache administrator: "+clazz);
       return Try.of(()->new CommitListenerCacheWrapper((DotCacheAdministrator) Class.forName(clazz).newInstance())).getOrElseThrow(e->new DotRuntimeException(e));
    }
	

	public static SystemCache getSystemCache() {
		return (SystemCache)getInstance(CacheIndex.System);
	}

	public static PermissionCache getPermissionCache() {
		return (PermissionCache)getInstance(CacheIndex.Permission);
	}

    public static RoleCache getRoleCache() {
        return (RoleCache)getInstance(CacheIndex.Role);
    }

    public static com.dotmarketing.business.RoleCache getCmsRoleCache() {
        return (com.dotmarketing.business.RoleCache)getInstance(CacheIndex.CMSRole);
    }

	public static CategoryCache getCategoryCache() {
		return (CategoryCache)getInstance(CacheIndex.Category);
	}

	public static TagCache getTagCache() {
		return (TagCache)getInstance(CacheIndex.Tag);
	}

	public static TagInodeCache getTagInodeCache() {
		return (TagInodeCache)getInstance(CacheIndex.TagInode);
	}

	public static ContentletCache getContentletCache() {
		return (ContentletCache)getInstance(CacheIndex.Contentlet);
	}


    public static DotResourceCache getVeloctyResourceCache(){
        return (DotResourceCache)getInstance(CacheIndex.Velocity2);
    }
    public static LogMapperCache getLogMapperCache () {
        return ( LogMapperCache ) getInstance( CacheIndex.LogMapper );
    }

	public static RelationshipCache getRelationshipCache() {
		return (RelationshipCache)getInstance(CacheIndex.Relationship);
	}

	public static PluginCache getPluginCache() {
		return (PluginCache)getInstance(CacheIndex.Plugin);
	}

	public static LanguageCache getLanguageCache() {
		return (LanguageCache)getInstance(CacheIndex.Language);
	}

	public static UserCache getUserCache() {
		return (UserCache)getInstance(CacheIndex.User);
	}

	public static UserProxyCache getUserProxyCache() {
		return (UserProxyCache)getInstance(CacheIndex.Userproxy);
	}

	public static LayoutCache getLayoutCache() {
		return (LayoutCache)getInstance(CacheIndex.Layout);
	}
    public static PortletCache getPortletCache() {
        return (PortletCache)getInstance(CacheIndex.PortletCache);
    }
	public static IdentifierCache getIdentifierCache() {
		return (IdentifierCache)getInstance(CacheIndex.Identifier);
	}

	public static HTMLPageCache getHTMLPageCache() {
		return (HTMLPageCache)getInstance(CacheIndex.HTMLPage);
	}

	public static MenuLinkCache getMenuLinkCache() {
		return (MenuLinkCache)getInstance(CacheIndex.Menulink);
	}

	public static ContainerCache getContainerCache() {
		return (ContainerCache)getInstance(CacheIndex.Container);
	}

	public static TemplateCache getTemplateCache() {
		return (TemplateCache)getInstance(CacheIndex.Template);
	}

	public static HostCache getHostCache() {
		return (HostCache)getInstance(CacheIndex.Host);
	}

	public static BlockDirectiveCache getBlockDirectiveCache() {
		return (BlockDirectiveCache)getInstance(CacheIndex.Block_Directive);
	}

	public static BlockPageCache getBlockPageCache() {
		return (BlockPageCache) getInstance(CacheIndex.Block_Page);
	}

	public static VersionableCache getVersionableCache() {
		return (VersionableCache)getInstance(CacheIndex.Versionable);
	}

	public static FolderCache getFolderCache() {
		return (FolderCache)getInstance(CacheIndex.FolderCache);
	}
	public static WorkflowCache getWorkFlowCache() {
		return (WorkflowCache) getInstance(CacheIndex.WorkflowCache);
	}

	public static HostVariablesCache getHostVariablesCache() {
		return (HostVariablesCache)getInstance(CacheIndex.HostVariables);
	}

	public static IndiciesCache getIndiciesCache() {
	    return (IndiciesCache)getInstance(CacheIndex.Indicies);
	}

    
	public static NavToolCache getNavToolCache() {
	    return (NavToolCache) getInstance(CacheIndex.NavTool);
	}

	public static PublishingEndPointCache getPublishingEndPointCache() {
		return (PublishingEndPointCache)getInstance(CacheIndex.PublishingEndPoint);
	}

	public static PushedAssetsCache getPushedAssetsCache() {
		return (PushedAssetsCache)getInstance(CacheIndex.PushedAssets);
	}

	public static CSSCache getCSSCache() {
	    return (CSSCache)getInstance(CacheIndex.CSSCache);
	}

	public static NewNotificationCache getNewNotificationCache() {
		return (NewNotificationCache)getInstance(CacheIndex.NewNotification);
	}

	public static RulesCache getRulesCache() {
		return (RulesCache) getInstance(CacheIndex.RulesCache);
	}
	
	public static SiteVisitCache getSiteVisitCache() {
		return (SiteVisitCache) getInstance(CacheIndex.SiteVisitCache);
	}
    public static ContentTypeCache getContentTypeCache() {
        return (ContentTypeCache) getInstance(CacheIndex.ContentTypeCache);
    }
    
    public static ContentTypeCache2 getContentTypeCache2() {
        return (ContentTypeCache2) getInstance(CacheIndex.ContentTypeCache2);
    }

    public static VanityUrlCache getVanityURLCache() {
		return (VanityUrlCache) getInstance(CacheIndex.VanityURLCache);
	}
    
    public static MultiTreeCache getMultiTreeCache() {
        return (MultiTreeCache) getInstance(CacheIndex.MultiTreeCache);
    }
    /**
     * 
     * @return
     */
    public static KeyValueCache getKeyValueCache() {
    	return (KeyValueCache) getInstance(CacheIndex.KeyValueCache);
    }
    public static ApiTokenCache getApiTokenCache() {
        return (ApiTokenCache) getInstance(CacheIndex.ApiTokenCache);
    }
	/**
	 * The legacy cache administrator will invalidate cache entries within a cluster
	 * on a put where the non legacy one will not.
	 * @return
	 */
	public static DotCacheAdministrator getCacheAdministrator(){
		return getInstance().adminCache;
	}
	

   public static CacheTransport getCacheTransport(){
       return LicenseManager.getInstance().getLevel() > 200  ? getInstance().eeTransport :getInstance().nullTransport;
   }
   
    private static CacheLocator getInstance() {
        if (instance == null) {
            synchronized (CacheLocator.class) {
                instance = instance!=null ? instance : new CacheLocator();
            }
            if (instance == null) {
                Logger.fatal(CacheLocator.class, "CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
                throw new DotRuntimeException("CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
            }
        }
        return instance;
    }
	
    public static void init(){
        CacheLocator.getInstance();
    }
	
	private static Object getInstance(CacheIndex index) {

		Object serviceRef = getInstance().getServiceInstance(index);

		Logger.debug(CacheLocator.class, instance.audit(index));

		return serviceRef;
	 }

	@Override
	protected Object createService(CacheIndex enumObj) {
		return enumObj.create();
	}

	@Override
	protected Locator<CacheIndex> getLocatorInstance() {
		return instance;
	}

	public static CacheIndex[] getCacheIndexes(){
		return CacheIndex.values();
	}

	public static Cachable getCache (String value) {
		return (Cachable)getInstance(CacheIndex.getCacheIndex(value));
	}

}

/**
 * 
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.6
 * @since 1.6
 *
 */
enum CacheIndex
{
	System("System"),
	Permission("Permission"),
	CMSRole("CMS Role"),
	Role("Role"),
	Category("Category"),
	Tag("Tag"),
	TagInode("TagInode"),
	Contentlet("Contentlet"),
	LogMapper("LogMapper"),
	Relationship("Relationship"),
	Plugin("Plugin"),
	Language("Language"),
	User("User"),
	Layout("Layout"),
	Userproxy("User Proxy"),
	Host("Host"),
	HTMLPage("Page"),
	Menulink("Menu Link"),
	Container("Container"),
	Template("Template"),
	Identifier("Identifier"),
	Versionable("Versionable"),
	FolderCache("FolderCache"),
	WorkflowCache("Workflow Cache"),
	HostVariables("Host Variables"),
	Block_Directive("Block Directive"),
	Block_Page("Block Page"),
	Indicies("Indicies"),
	NavTool("Navigation Tool"),
	PublishingEndPoint("PublishingEndPoint Cache"),
	PushedAssets("PushedAssets Cache"),
	CSSCache("Processed CSS Cache"),
	RulesCache("Rules Cache"),
	SiteVisitCache("Rules Engine - Site Visits"),
	NewNotification("NewNotification Cache"),
	VanityURLCache("Vanity URL Cache"),
	ContentTypeCache("Legacy Content Type Cache"),
	ContentTypeCache2("New Content Type Cache"),
	Velocity2("Velocity2"),
	NavTool2("Navigation Tool2"),
	MultiTreeCache("MultiTree Cache"),
	ApiTokenCache("ApiTokenCache"),
	PortletCache("PortletCache"),
	KeyValueCache("Key/Value Cache");

	Cachable create() {
		switch(this) {
			case System: return new SystemCache();
			case Permission: return new PermissionCacheImpl();
	      	case Category: return new CategoryCacheImpl();
	      	case Tag: return new TagCacheImpl();
	      	case TagInode: return new TagInodeCacheImpl();
	      	case Role: return new RoleCacheImpl();
	      	case Contentlet: return new ContentletCacheImpl();
	        case Velocity2 : return new DotResourceCache();
	      	case Relationship: return new RelationshipCacheImpl();
	        case LogMapper: return new LogMapperCacheImpl();
	      	case Plugin : return new PluginCacheImpl();
	      	case Language : return new LanguageCacheImpl();
	      	case User : return new UserCacheImpl();
	      	case Userproxy : return new UserProxyCacheImpl();
	      	case Layout : return new LayoutCacheImpl();
	      	case CMSRole : return new com.dotmarketing.business.RoleCacheImpl();
	      	case HTMLPage : return new HTMLPageCacheImpl();
	      	case Menulink : return new MenuLinkCacheImpl();
	      	case Container : return new ContainerCacheImpl();
	      	case Template : return new TemplateCacheImpl();
	      	case Host : return new HostCacheImpl();
	      	case Identifier : return new IdentifierCacheImpl();
	      	case HostVariables : return new HostVariablesCacheImpl();
	      	case Block_Directive : return new BlockDirectiveCacheImpl();
	      	case Block_Page : return new BlockPageCacheImpl();
	      	case Versionable : return new VersionableCacheImpl();
	      	case FolderCache : return new FolderCacheImpl();
	      	case WorkflowCache : return new WorkflowCacheImpl();
	      	case Indicies: return new IndiciesCacheImpl();
	        case NavTool: return new NavToolCacheImpl();
	      	case PublishingEndPoint: return new PublishingEndPointCacheImpl();
	      	case PushedAssets: return new PushedAssetsCacheImpl();
	      	case CSSCache: return new CSSCacheImpl();
	      	case NewNotification: return new NewNotificationCacheImpl();
	      	case RulesCache : return new RulesCacheImpl();
	      	case SiteVisitCache : return new SiteVisitCacheImpl();
	      	case ContentTypeCache: return new LegacyContentTypeCacheImpl();
	      	case ContentTypeCache2: return new ContentTypeCache2Impl();
	      	case VanityURLCache : return new VanityUrlCacheImpl();
	      	case KeyValueCache : return new KeyValueCacheImpl();
	      	case MultiTreeCache : return new MultiTreeCache();
	      	case ApiTokenCache : return new ApiTokenCache();
	      	case PortletCache : return new PortletCache();
	      	
		}
		throw new AssertionError("Unknown Cache index: " + this);
	}

	private String value;

	CacheIndex (String value) {
		this.value = value;
	}

	public String toString () {
		return value;
	}

	public static CacheIndex getCacheIndex (String value) {
		CacheIndex[] types = CacheIndex.values();
		for (CacheIndex type : types) {
			if (type.value.equals(value))
				return type;
		}
		return null;
	}

}
