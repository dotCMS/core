package com.dotmarketing.business;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgroups.JChannel;

import com.dotcms.content.elasticsearch.business.IndiciesCache;
import com.dotcms.content.elasticsearch.business.IndiciesCacheImpl;
import com.dotmarketing.cache.FolderCache;
import com.dotmarketing.cache.FolderCacheImpl;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.plugin.business.PluginCache;
import com.dotmarketing.plugin.business.PluginCacheImpl;
import com.dotmarketing.portlets.categories.business.CategoryCache;
import com.dotmarketing.portlets.categories.business.CategoryCacheImpl;
import com.dotmarketing.portlets.chains.business.ChainCache;
import com.dotmarketing.portlets.chains.business.ChainCacheImpl;
import com.dotmarketing.portlets.containers.business.ContainerCache;
import com.dotmarketing.portlets.containers.business.ContainerCacheImpl;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.business.ContentletCacheImpl;
import com.dotmarketing.portlets.contentlet.business.HostCache;
import com.dotmarketing.portlets.contentlet.business.HostCacheImpl;
import com.dotmarketing.portlets.files.business.FileCache;
import com.dotmarketing.portlets.files.business.FileCacheImpl;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariablesCache;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariablesCacheImpl;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageCache;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageCacheImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageCache;
import com.dotmarketing.portlets.languagesmanager.business.LanguageCacheImpl;
import com.dotmarketing.portlets.links.business.MenuLinkCache;
import com.dotmarketing.portlets.links.business.MenuLinkCacheImpl;
import com.dotmarketing.portlets.structure.factories.RelationshipCache;
import com.dotmarketing.portlets.structure.factories.RelationshipCacheImpl;
import com.dotmarketing.portlets.templates.business.TemplateCache;
import com.dotmarketing.portlets.templates.business.TemplateCacheImpl;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkCache;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkCacheImpl;
import com.dotmarketing.portlets.workflows.business.WorkflowCache;
import com.dotmarketing.portlets.workflows.business.WorkflowCacheImpl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.velocity.DotResourceCache;


/**
 * FactoryLocator is a factory method to get single(ton) service objects. 
 * This is a kind of implementation, and there may be others.
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.6
 * @since 1.6
 */

public class CacheLocator extends Locator<CacheIndex>{
	
    private static class CommitListenerCacheWrapper implements DotCacheAdministrator {
        DotCacheAdministrator dotcache;
        public CommitListenerCacheWrapper(DotCacheAdministrator dotcache) { this.dotcache=dotcache; }
        public Set<String> getKeys(String group) { return dotcache.getKeys(group); }
        public void flushAll() { dotcache.flushAll(); }
        public void flushGroup(String group) { dotcache.flushGroup(group); }
        public void flushAlLocalOnlyl() { dotcache.flushAlLocalOnlyl(); }
        public void flushGroupLocalOnly(String group) { dotcache.flushGroupLocalOnly(group); }
        public Object get(String key, String group) throws DotCacheException { return dotcache.get(key, group); }
        public void remove(String key, String group) { dotcache.remove(key,group); }
        public void removeLocalOnly(String key, String group) { dotcache.removeLocalOnly(key, group); }
        public void shutdown() { dotcache.shutdown(); }
        public JChannel getJGroupsChannel() { return dotcache.getJGroupsChannel(); }
        public List<Map<String, Object>> getCacheStatsList() { return dotcache.getCacheStatsList(); }
        public Class getImplementationClass() { return dotcache.getClass(); }
        public void put(final String key, final Object content, final String group) {
            try {
                HibernateUtil.addCommitListener(new Runnable() {
                   public void run() {
                       dotcache.put(key, content, group);
                   } 
                });
            } catch (DotHibernateException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
	private static CacheLocator instance;
	private static DotCacheAdministrator adminCache;
	
	private CacheLocator() {
		super();
	}
	
	public synchronized static void init(){
		if(instance != null)
			return;
		
		String clazz = Config.getStringProperty("cache.locator.class", DotGuavaCacheAdministratorImpl.class.getCanonicalName());
		Logger.info(CacheLocator.class, "loading cache administrator: "+clazz);
		try{
			adminCache = new CommitListenerCacheWrapper((DotCacheAdministrator) Class.forName(clazz).newInstance());
			
		}
		catch(Exception e){
			Logger.fatal(CacheLocator.class, "Unable to load Cache Admin:" + clazz);
		}
		
		instance = new CacheLocator();
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
	
	public static ContentletCache getContentletCache() {
		return (ContentletCache)getInstance(CacheIndex.Contentlet);
	}
	
	public static DotResourceCache getVeloctyResourceCache(){
		return (DotResourceCache)getInstance(CacheIndex.Velocity);
	}
		
	public static ChainCache getChainCache(){
		return (ChainCache)getInstance(CacheIndex.Chain);
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
	
	public static FileCache getFileCache() {
		return (FileCache)getInstance(CacheIndex.File);
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

	public static VersionableCache getVersionableCache() {
		return (VersionableCache)getInstance(CacheIndex.Versionable);
	}
	public static FolderCache getFolderCache() {
		return (FolderCache)getInstance(CacheIndex.FolderCache);
	}
	public static WorkflowCache getWorkFlowCache() {
		return (WorkflowCache) getInstance(CacheIndex.WorkflowCache);
	}
	
	public static VirtualLinkCache getVirtualLinkCache() {
		return (VirtualLinkCache) getInstance(CacheIndex.VirtualLinkCache);
	}
	
	public static HostVariablesCache getHostVariablesCache() {
		return (HostVariablesCache)getInstance(CacheIndex.HostVariables);
	}
	
	public static IndiciesCache getIndiciesCache() {
	    return (IndiciesCache)getInstance(CacheIndex.Indicies);
	}

	/**
	 * The legacy cache administrator will invalidate cache entries within a cluster
	 * on a put where the non legacy one will not.  
	 * @return
	 */
	public static DotCacheAdministrator getCacheAdministrator(){
		return adminCache;
	}
	
	private static Object getInstance(CacheIndex index) {

		if(instance == null){
			init();
			if(instance == null){
				Logger.fatal(CacheLocator.class, "CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
				throw new DotRuntimeException("CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
			}
		}
		
		Object serviceRef = instance.getServiceInstance(index);

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



enum CacheIndex 
{
	Permission("Permission"), 
	CMSRole("CMS Role"),
	Role("Role"), 
	Category("Category"),
	Contentlet("Contentlet"),
	Chain("Chain"),
	Relationship("Relationship"),
	Plugin("Plugin"),
	Language("Language"),
	User("User"),
	Velocity("Velocity"),
	Layout("Layout"),
	Userproxy("User Proxy"),
	Host("Host"),
	File("File"),
	HTMLPage("Page"),
	Menulink("Menu Link"),
	Container("Container"),
	Template("Template"),
	Identifier("Identifier"),
	Versionable("Versionable"),
	FolderCache("FolderCache"),
	WorkflowCache("Workflow Cache"),
	VirtualLinkCache("Virtual Link Cache"),
	HostVariables("Host Variables"),
	Block_Directive("Block Directive"),
	Indicies("Indicies");
	
	Cachable create() {
		switch(this) {
		case Permission: return new PermissionCacheImpl();
      	case Category: return new CategoryCacheImpl();
      	case Role: return new RoleCacheImpl();
      	case Contentlet: return new ContentletCacheImpl();
      	case Velocity : return new DotResourceCache();
      	case Relationship: return new RelationshipCacheImpl();
      	case Chain : return new ChainCacheImpl();
      	case Plugin : return new PluginCacheImpl();
      	case Language : return new LanguageCacheImpl();
      	case User : return new UserCacheImpl();
      	case Userproxy : return new UserProxyCacheImpl();
      	case Layout : return new LayoutCacheImpl();
      	case CMSRole : return new com.dotmarketing.business.RoleCacheImpl();
      	case File : return new FileCacheImpl();
      	case HTMLPage : return new HTMLPageCacheImpl();
      	case Menulink : return new MenuLinkCacheImpl();
      	case Container : return new ContainerCacheImpl();
      	case Template : return new TemplateCacheImpl();
      	case Host : return new HostCacheImpl();
      	case Identifier : return new IdentifierCacheImpl();
      	case HostVariables : return new HostVariablesCacheImpl();
      	case Block_Directive : return new BlockDirectiveCacheImpl();
      	case Versionable : return new VersionableCacheImpl();
      	case FolderCache : return new FolderCacheImpl();
      	case WorkflowCache : return new WorkflowCacheImpl();
      	case VirtualLinkCache : return new VirtualLinkCacheImpl();
      	case Indicies: return new IndiciesCacheImpl();
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

