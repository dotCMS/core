package com.dotmarketing.portlets.links.factories;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.query.GenericQueryFactory.BuilderType;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * @author  will
 */
public class LinkFactory {
    
	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	private static HostAPI hostAPI = APILocator.getHostAPI();
	private static SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();

	/**
	 * @param permissionAPIRef the permissionAPI to set
	 */
	public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

	public static java.util.List getChildrenLinkByOrder(Inode i) {
        HibernateUtil dh = new HibernateUtil(Link.class);
        List<Link> list =null;
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.links.model.Link where ? in inode.parents.elements order by sort_order");
			dh.setParam(i.getInode());
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(LinkFactory.class, "getChildrenLinkByOrder failed:" + e, e);
		}
        return list;
    }
    
    public static java.util.List getActiveLinks() {
        HibernateUtil dh = new HibernateUtil(Link.class);
        List<Link> list =null;
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.links.model.Link where type='links'");
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(LinkFactory.class, "getActiveLinks failed:" + e, e);
		}
        return list;
    }

    public static java.util.List getLinksByOrderAndParent(String orderby,Inode o) {
        HibernateUtil dh = new HibernateUtil(Link.class);
        List<Link> list=null ;
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.links.model.Link where ? in inode.parents.elements and working = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " or live = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " order by " + orderby);
			dh.setParam(o.getInode());
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(LinkFactory.class, "getLinksByOrderAndParent failed:" + e, e);
		}
        return list;
    }

    public static java.util.List getLinksByOrder(String orderby) {
        HibernateUtil dh = new HibernateUtil(Link.class);
        List<Link> list=null ;
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.links.model.Link where type='links' and working = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " or live = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " order by " + orderby);
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(LinkFactory.class, "getLinksByOrder failed:" + e, e);
		}
        return list;
    }
 
    public static java.util.List getLinkChildrenByCondition(Inode o,String condition) {
        try {

            final DotConnect dc = new DotConnect();
            dc.setSQL(
			"SELECT links.*, links_1_.* from links links, identifier identifier, inode links_1_ where identifier.parent_path = ? and identifier.id = links.identifier and " +
			"links_1_.inode = links.inode and links_1_.type='links' and " +
			"identifier.host_inode =(select host_inode from identifier where id = ?)" +
                    (condition!=null && !condition.isEmpty()? " and " + condition:"") + " order by url, sort_order");

            dc.addParam(APILocator.getIdentifierAPI().find(o).getPath());
            dc.addParam(o.getIdentifier());


            return TransformerLocator.createLinkTransformer(dc.loadObjectResults()).asList();

        } catch (Exception e) {
			Logger.error(LinkFactory.class, "getLinkChildrenByCondition failed:" + e, e);
        }

        return new java.util.ArrayList();
    }

   	public static java.util.List getLinkByCondition(String condition) {
		HibernateUtil dh = new HibernateUtil(Link.class);
		List<Link> list =null;
		try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.links.model.Link where type='links' and " + condition + " order by url, sort_order");
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(LinkFactory.class, "getLinkByCondition failed:" + e, e);
		} 
		return list;
	}

    
    public static java.util.List getLinkChildren(Inode o) {
        try {
            HibernateUtil dh = new HibernateUtil(Link.class);
            dh.setQuery("from inode in class com.dotmarketing.portlets.links.model.Link where ? in inode.parents.elements order by inode, sort_order");
            dh.setParam(o.getInode());

            return dh.list();
        } catch (Exception e) {
			Logger.error(LinkFactory.class, "getLinkChildren failed:" + e, e);
        }

        return new java.util.ArrayList();
    }

	public static Link getLinkByLiveAndFolderAndTitle(Inode parent , String title) {
		try {
			HibernateUtil dh = new HibernateUtil(Link.class);
			dh.setQuery("from inode in class com.dotmarketing.portlets.links.model.Link where ? in inode.parents.elements and title =  ? and live = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue());
			dh.setParam(parent.getInode());
			dh.setParam(title);
			return (Link) dh.load();
		} catch (Exception e) {
			Logger.error(LinkFactory.class, "getLinkByLiveAndFolderAndTitle failed:" + e, e);
		}

		return new Link();
	}

    public static Link getLinkByFriendlyName(String friendlyName) {
        HibernateUtil dh = new HibernateUtil(Link.class);
        Link link =null;
        try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.links.model.Link where friendly_name = ? and type='links' and live=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue());
			dh.setParam(friendlyName);
			link = (Link) dh.load();
		} catch (DotHibernateException e) {
			Logger.error(LinkFactory.class, "getLinkByFriendlyName failed:" + e, e);
		}
        return link;
    }

    public static Link getLinkFromInode(String strInode, String userId) throws DotDataException, DotStateException, DotSecurityException {

        Logger.debug(LinkFactory.class, "running getLinkFromInode(String strInode, String userId)");
    	
        com.dotmarketing.beans.Inode inode =
                com.dotmarketing.factories.InodeFactory.getInode(strInode, com.dotmarketing.beans.Inode.class);
    		
    	if(inode instanceof Link){
    		
    		return ((com.dotmarketing.portlets.links.model.Link) inode);	
    	}
    	
    	return (new Link());


    }

    public static Link copyLink ( Link currentLink, Folder parent ) throws DotDataException, DotStateException, DotSecurityException {
        return copyLink( currentLink, parent, null );
    }

    public static Link copyLink ( Link currentLink, Host host ) throws DotDataException, DotStateException, DotSecurityException {
        return copyLink( currentLink, null, host );
    }

    private static Link copyLink ( Link currentLink, Folder parent, Host host ) throws DotDataException, DotStateException, DotSecurityException {

        Link newLink = new Link();
        newLink.copy( currentLink );

        //First lets verify if already exist
        Boolean exist;
        if ( parent != null ) {
            exist = existsLinkWithTitleInFolder( currentLink.getTitle(), parent );
        } else {
            exist = existsLinkWithTitleInFolder( currentLink.getTitle(), host );
        }

        if ( exist ) {
            newLink.setFriendlyName( currentLink.getFriendlyName() + " (COPY) " );
            newLink.setTitle( currentLink.getTitle() + " (COPY) " );
        } else {
            newLink.setFriendlyName( currentLink.getFriendlyName() );
            newLink.setTitle( currentLink.getTitle() );
        }
        newLink.setProtocal( currentLink.getProtocal() );
        newLink.setLinkCode( currentLink.getLinkCode() );
        newLink.setLinkType( currentLink.getLinkType() );

        //persists the webasset
        HibernateUtil.saveOrUpdate( newLink );

        //adding to the parent folder
        //parent.addChild(newLink);

        //creates new identifier for this webasset and persists it
        Identifier newIdent;
        if ( parent != null ) {
            newIdent = APILocator.getIdentifierAPI().createNew( newLink, parent );
        } else {
            newIdent = APILocator.getIdentifierAPI().createNew( newLink, host );
        }

        newLink.setIdentifier( newIdent.getId() );
        HibernateUtil.saveOrUpdate( newLink );

        APILocator.getVersionableAPI().setWorking( newLink );
        if ( currentLink.isLive() ) {
            APILocator.getVersionableAPI().setLive( newLink );
        }

        //Copy permissions
        permissionAPI.copyPermissions( currentLink, newLink );

		systemEventsAPI.pushAsync(SystemEventType.COPY_LINK, new Payload(currentLink, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(currentLink.getModUser(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

        return newLink;
    }

    public static boolean moveLink ( Link currentLink, Folder parent ) throws DotStateException, DotDataException, DotSecurityException {
        return moveLink( currentLink, parent, null );
    }

    public static boolean moveLink ( Link currentLink, Host host ) throws DotStateException, DotDataException, DotSecurityException {
        return moveLink( currentLink, null, host );
    }

    /**
     * Method used to move a link from folder
     *
     * @param currentLink link to move
     * @param parent
     * @param host
     * @return true if the move succeeded, false if another link with the same name exists on the destination
     * @throws DotDataException
     * @throws DotStateException
     * @throws DotSecurityException
     */
    private static boolean moveLink ( Link currentLink, Folder parent, Host host ) throws DotStateException, DotDataException, DotSecurityException {

        //First lets verify if already exist
        Boolean exist;
        if ( parent != null ) {
            exist = existsLinkWithTitleInFolder( currentLink.getTitle(), parent );
        } else {
            exist = existsLinkWithTitleInFolder( currentLink.getTitle(), host );
        }

        if ( exist ) {
            return false;
        }

        //Link identifier
        Identifier identifier = com.dotmarketing.business.APILocator.getIdentifierAPI().find( currentLink );

        // gets working container
        WebAsset workingWebAsset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion( identifier, APILocator.getUserAPI().getSystemUser(), false );

        // gets old parent
        Folder oldParent = null;
        try{
        	oldParent = APILocator.getFolderAPI().findParentFolder( workingWebAsset, APILocator.getUserAPI().getSystemUser(), false );
        }catch(Exception e){
        	Logger.debug(LinkFactory.class,"link reference to old parent folder not found");
        }
        /*oldParent.deleteChild(workingWebAsset);
          if ((liveWebAsset != null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
              oldParent.deleteChild(liveWebAsset);
          }

          // Adding to new parent
          parent.addChild(workingWebAsset);
          if ((liveWebAsset != null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
              parent.addChild(liveWebAsset);
          }*/

        if ( parent != null ) {

            Host newHost;
            try {
                User systemUser = APILocator.getUserAPI().getSystemUser();
                newHost = hostAPI.findParentHost( parent, systemUser, false );
            } catch ( DotDataException e ) {
                Logger.error( LinkFactory.class, e.getMessage(), e );
                throw new DotRuntimeException( e.getMessage(), e );
            } catch ( DotSecurityException e ) {
                Logger.error( LinkFactory.class, e.getMessage(), e );
                throw new DotRuntimeException( e.getMessage(), e );
            }

            identifier.setHostId( newHost.getIdentifier() );
            identifier.setURI( workingWebAsset.getURI( parent ) );
        } else {
            identifier.setHostId( host.getIdentifier() );
            identifier.setURI( '/' + currentLink.getInode() );
        }
        
        APILocator.getIdentifierAPI().updateIdentifierURI(currentLink, parent);
        CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(identifier.getId());
        
        if(APILocator.getPermissionAPI().isInheritingPermissions(currentLink)) {
            APILocator.getPermissionAPI().removePermissions(currentLink);
        }

        //Refresh the menus
        if ( parent != null ) {
        	if(oldParent != null){
        		RefreshMenus.deleteMenu( oldParent, parent );
        	}else{
        		RefreshMenus.deleteMenu(parent);
        	}
            CacheLocator.getNavToolCache().removeNav(parent.getHostId(), parent.getInode());
        } else {
            RefreshMenus.deleteMenu( oldParent );
        }
        if(oldParent != null){
        	CacheLocator.getNavToolCache().removeNav(oldParent.getHostId(), oldParent.getInode());
        }

		systemEventsAPI.pushAsync(SystemEventType.MOVE_LINK, new Payload(currentLink, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(currentLink.getModUser(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

        return true;
    }

    private static boolean existsLinkWithTitleInFolder ( String title, Folder parent ) throws DotStateException, DotDataException, DotSecurityException {

        List<Link> links = APILocator.getFolderAPI().getLinks( parent, APILocator.getUserAPI().getSystemUser(), false );
        for ( Link link : links ) {
            if ( title.equalsIgnoreCase( link.getTitle() ) ) {
                return (InodeUtils.isSet( link.getInode() ));
            }
        }
        return false;
    }

    private static boolean existsLinkWithTitleInFolder ( String title, Host host ) throws DotStateException, DotDataException, DotSecurityException {

        List<Link> links = APILocator.getFolderAPI().getLinks( host, APILocator.getUserAPI().getSystemUser(), false );
        for ( Link link : links ) {
            if ( title.equalsIgnoreCase( link.getTitle() ) ) {
                return (InodeUtils.isSet( link.getInode() ));
            }
        }
        return false;
    }
	
    public static boolean renameLink (Link link, String newName, User user) throws Exception {

    	// Checking permissions
    	if (!permissionAPI.doesUserHavePermission(link, PERMISSION_WRITE, user))
    		throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);

    	//getting old file properties
    	Folder folder = APILocator.getFolderAPI().findParentFolder(link, user,false);
    	CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
    	
    	Identifier ident = APILocator.getIdentifierAPI().find(link);

    	Link newLinkVersion = new Link();
    	newLinkVersion.copy(link);
    	// sets filename for this new file
    	newLinkVersion.setTitle(newName);
    	newLinkVersion.setFriendlyName(newName);

    	if(existsLinkWithTitleInFolder(newName, folder) || link.isLocked())
    		return false;

    	List<Versionable> versions = APILocator.getVersionableAPI().findAllVersions(ident);
    	
    	
    	for (Versionable version : versions) {
            Link menuLink = (Link)version;
	    	// sets filename for this new file
            menuLink.setTitle(newName);
            menuLink.setFriendlyName(newName);

	    	
	    	HibernateUtil.saveOrUpdate(menuLink);

    	}
    	
    	ident.setURI(link.getURI(folder));
    	//HibernateUtil.saveOrUpdate(ident);
    	APILocator.getIdentifierAPI().save(ident);
    	
    	//RefreshMenus.deleteMenus();
    	RefreshMenus.deleteMenu(link);
    	CacheLocator.getNavToolCache().removeNavByPath(ident.getHostId(), ident.getParentPath());

    	return true;

    }
	public static List<Map<String, Serializable>> DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,DotDataException {
		Map<String, String> dbColToObjectAttribute = new HashMap<String, String>();
		String fromClause=query.getFromClause();
		fromClause=fromClause.replaceAll("menulink", "links");
		query.setFromClause(fromClause);
		query.setBuilderType(BuilderType.MENU_LINK_TABLE);
		if(UtilMethods.isSet(query.getSelectAttributes())){
			
			if(!query.getSelectAttributes().contains("title")){
				query.getSelectAttributes().add("title");
			}
		}else{
			List<String> atts = new ArrayList<String>();
			atts.add("*");
			atts.add("title");
			query.setSelectAttributes(atts);
		}
				
		return QueryUtil.DBSearch(query, dbColToObjectAttribute, null, user, true, respectFrontendRoles);
	}

	/**
		 * Method will replace user references of the given userId in MenuLinks 
		 * with the replacement user id 
		 * @param userId User Identifier
		 * @param replacementUserId The user id of the replacement user
		 * @throws DotDataException There is a data inconsistency
		 * @throws DotStateException There is a data inconsistency
		 * @throws DotSecurityException 
		 */
		public static void updateUserReferences(String userId, String replacementUserId) throws DotDataException, DotStateException, DotSecurityException {
		    DotConnect dc = new DotConnect();
	        try {
	           dc.setSQL("select inode from links where mod_user = ?");
	           dc.addParam(userId);
	           List<HashMap<String, String>> links = dc.loadResults();
	           
	           dc.setSQL("UPDATE links set mod_user = ? where mod_user = ? ");
	           dc.addParam(replacementUserId);
	           dc.addParam(userId);
	           dc.loadResult();
	           
	           dc.setSQL("update link_version_info set locked_by=? where locked_by  = ?");
	           dc.addParam(replacementUserId);
	           dc.addParam(userId);
	           dc.loadResult();
	           
	           for(HashMap<String, String> ident:links){
	             String inode = ident.get("inode");
	             Link link = FactoryLocator.getMenuLinkFactory().load(inode);
	             CacheLocator.getNavToolCache().removeNav(link.getHostId(),link.getInode());
	    		 RefreshMenus.deleteMenu(link);
	          }
	        } catch (DotDataException e) {
	            Logger.error(LinkFactory.class,e.getMessage(),e);
	            throw new DotDataException(e.getMessage(), e);
	        }
		}	
	
}
