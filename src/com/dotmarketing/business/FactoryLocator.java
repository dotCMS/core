package com.dotmarketing.business;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.content.elasticsearch.business.IndiciesFactory;
import com.dotcms.content.elasticsearch.business.IndiciesFactoryImpl;
import com.dotcms.enterprise.DashboardProxy;
import com.dotcms.journal.business.ESDistributedJournalFactoryImpl;
import com.dotmarketing.common.business.journal.DistributedJournalFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.plugin.business.PluginFactory;
import com.dotmarketing.plugin.business.PluginFactoryDBImpl;
import com.dotmarketing.portlets.calendar.business.CalendarReminderFactory;
import com.dotmarketing.portlets.calendar.business.CalendarReminderFactoryImpl;
import com.dotmarketing.portlets.calendar.business.EventFactory;
import com.dotmarketing.portlets.calendar.business.EventFactoryImpl;
import com.dotmarketing.portlets.categories.business.CategoryFactory;
import com.dotmarketing.portlets.categories.business.CategoryFactoryImpl;
import com.dotmarketing.portlets.chains.business.ChainFactory;
import com.dotmarketing.portlets.chains.business.ChainFactoryImpl;
import com.dotmarketing.portlets.checkurl.business.LinkCheckerFactory;
import com.dotmarketing.portlets.checkurl.business.LinkCheckerFactoryImpl;
import com.dotmarketing.portlets.containers.business.ContainerFactory;
import com.dotmarketing.portlets.containers.business.ContainerFactoryImpl;
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
import com.dotmarketing.portlets.dashboard.business.DashboardFactory;
import com.dotmarketing.portlets.files.business.FileFactory;
import com.dotmarketing.portlets.files.business.FileFactoryImpl;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.folders.business.FolderFactoryImpl;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableFactory;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableFactoryImpl;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageFactory;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageFactoryImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageFactory;
import com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryImpl;
import com.dotmarketing.portlets.links.business.MenuLinkFactory;
import com.dotmarketing.portlets.links.business.MenuLinkFactoryImpl;
import com.dotmarketing.portlets.templates.business.TemplateFactory;
import com.dotmarketing.portlets.templates.business.TemplateFactoryImpl;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkFactory;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkFactoryImpl;
import com.dotmarketing.portlets.workflows.business.WorkFlowFactory;
import com.dotmarketing.portlets.workflows.business.WorkflowFactoryImpl;
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

	private FactoryLocator() {
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
    
    public static ContentletFactory getContentletFactory(){
    	return (ContentletFactory)getInstance(FactoryIndex.CONTENTLET_FACTORY);
    }
   
    public static ChainFactory getChainFactory(){
    	return (ChainFactory)getInstance(FactoryIndex.CHAIN_FACTORY);
    }
    
    public static PluginFactory getPluginFactory(){
    	return (PluginFactory)getInstance(FactoryIndex.PLUGIN_FACTORY);
    }
    
    public static LanguageFactory getLanguageFactory(){
    	return (LanguageFactory)getInstance(FactoryIndex.LANGUAGE_FACTORY);
    }
    
    public static DistributedJournalFactory<String> getDistributedJournalFactory(){
    	return (DistributedJournalFactory<String>)getInstance(FactoryIndex.DISTRIBUTED_JOURNAL_FACTORY);
    }
    
    public static UserFactory getUserFactory(){
    	return (UserFactory)getInstance(FactoryIndex.USER_FACTORY);
    }
    
    public static CalendarReminderFactory getCalendarReminderFactory(){
    	return (CalendarReminderFactory) getInstance(FactoryIndex.CALENDAR_REMINDER_FACTORY);
    } 
    
       public static TemplateFactory getTemplateFactory(){
    	return (TemplateFactory) getInstance(FactoryIndex.TEMPLATE_FACTORY);
    } 
    
    
    public static UserProxyFactory getUserProxyFactory(){
    	return (UserProxyFactory) getInstance(FactoryIndex.USER_PROXY_FACTORY);
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
        
    public static FileFactory getFileFactory(){
    	return (FileFactory) getInstance(FactoryIndex.FILE_FACTORY);
    }
    
    public static HTMLPageFactory getHTMLPageFactory(){
    	return (HTMLPageFactory) getInstance(FactoryIndex.HTMLPAGE_FACTORY);
    }
    
    public static MenuLinkFactory getMenuLinkFactory(){
    	return (MenuLinkFactory) getInstance(FactoryIndex.MENULINK_FACTORY);
    }
    
    public static ContainerFactory getContainerFactory(){
    	return (ContainerFactory) getInstance(FactoryIndex.CONTAINER_FACTORY);
    }
    
    public static VirtualLinkFactory getVirtualLinkFactory(){
    	return (VirtualLinkFactory) getInstance(FactoryIndex.VIRTUALLINK_FACTORY);
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
	CHAIN_FACTORY,
	CALENDAR_REMINDER_FACTORY,
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
	VIRTUALLINK_FACTORY,
	IDENTIFIER_FACTORY,
	VERSIONABLE_FACTORY,
	FOLDER_FACTORY,
	DASHBOARD_FACTORY,
	WORKFLOWS_FACTORY,
	INDICIES_FACTORY,
	LINKCHECKER_FACTORY;
	
	
	Object create() {
		switch(this) {
			case PERMISSION_FACTORY: return new PermissionBitFactoryImpl(CacheLocator.getPermissionCache());
            case CALENDAR_EVENT_FACTORY: return new EventFactoryImpl();
            case CATEGORY_FACTORY: return new CategoryFactoryImpl();
            case CONTENTLET_FACTORY: return new ESContentFactoryImpl();
            case PLUGIN_FACTORY: return new PluginFactoryDBImpl();
            case CHAIN_FACTORY: return new ChainFactoryImpl();
            case LANGUAGE_FACTORY: return new LanguageFactoryImpl();
            case DISTRIBUTED_JOURNAL_FACTORY: return new ESDistributedJournalFactoryImpl<String>("0");
            case USER_FACTORY : return new UserFactoryLiferayImpl();
            case CALENDAR_REMINDER_FACTORY: return new CalendarReminderFactoryImpl();
            case TEMPLATE_FACTORY: return new TemplateFactoryImpl();
            case HOST_VARIABLE_FACTORY: return new HostVariableFactoryImpl();
            case LAYOUT_FACTORY : return new LayoutFactoryImpl();
            case USER_PROXY_FACTORY: return new UserProxyFactoryImpl() {};
            case ROLE_FACTORY : return new RoleFactoryImpl();
            case FILE_FACTORY : return new FileFactoryImpl();
            case HTMLPAGE_FACTORY : return new HTMLPageFactoryImpl();
            case MENULINK_FACTORY : return new MenuLinkFactoryImpl();
            case CONTAINER_FACTORY : return new ContainerFactoryImpl();
            case VIRTUALLINK_FACTORY : return new VirtualLinkFactoryImpl();
            case DASHBOARD_FACTORY : return DashboardProxy.getDashboardFactory();
            case IDENTIFIER_FACTORY : return new IdentifierFactoryImpl();
            case VERSIONABLE_FACTORY : return new VersionableFactoryImpl();
            case FOLDER_FACTORY : return new FolderFactoryImpl();
            case WORKFLOWS_FACTORY :return new WorkflowFactoryImpl();
            case INDICIES_FACTORY: return new IndiciesFactoryImpl();
            case LINKCHECKER_FACTORY: return new LinkCheckerFactoryImpl();
		}
		throw new AssertionError("Unknown Factory Index: " + this);
	}
}

