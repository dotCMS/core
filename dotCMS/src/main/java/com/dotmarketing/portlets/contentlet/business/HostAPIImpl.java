package com.dotmarketing.portlets.contentlet.business;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.SQLQueryFactory;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

/**
 * @author jtesser
 * @author david torres
 *
 */
public class HostAPIImpl implements HostAPI {

	private ContentletFactory conFac = FactoryLocator.getContentletFactory();
	private HostCache hostCache = CacheLocator.getHostCache();
	private Host systemHost;
	private final SystemEventsAPI systemEventsAPI;

	public HostAPIImpl() {
		this.systemEventsAPI = APILocator.getSystemEventsAPI();
	}

	/**
	 *
	 * @return the default host from cache.  If not found, returns from content search and adds to cache
	 * @throws DotSecurityException, DotDataException
	 */
	public Host findDefaultHost(User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

		Host host = null;
		try{
			host  = hostCache.getDefaultHost();
			if(host != null){
				if(APILocator.getPermissionAPI().doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
					return host;
				}
			}
		}
		catch(Exception e){
			Logger.debug(HostAPIImpl.class, e.getMessage());
		}

		try {
	    	Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
	    	List<Contentlet> list = null;
	    	try{
	    		list = APILocator.getContentletAPI().search("+structureInode:" + st.getInode() + " +working:true +host.isdefault:true ", 0, 0, null, APILocator.getUserAPI().getSystemUser(), respectFrontendRoles);
	    	}
	    	catch(Exception e){
	    		Logger.warn(this, "Content Index is fouled up, need to try db: " + e.getMessage());
	    	}
	    	if(list == null || list.size() ==0)
	    		return createDefaultHost();

			else if (list.size() >1){
				Logger.fatal(this, "More of one host is marked as default!!");
			}
			host = new Host(list.get(0));
			if(APILocator.getPermissionAPI().doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
				hostCache.add(host);
				return host;
			}
			throw new DotSecurityException("User : " + user.getUserId()+ " does not have permission to a host");
		} catch (Exception e) {

			if(user!=null && !user.equals(APILocator.getUserAPI().getDefaultUser())){
				Logger.error(HostAPIImpl.class, e.getMessage(), e);
			}

			throw new DotRuntimeException(e.getMessage(), e);
		}

	}

	/**
	 * This method takes a server name (from a web request) and maps it to a host.
	 * It is designed to do a lightweight cache lookup to get the mapping from server name -> host
	 * and to prevent unnecessary lucene lookups
	 * @param serverName
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */

	public Host resolveHostName(String serverName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Host host = hostCache.getHostByAlias(serverName);
		
		if(host == null){
			User systemUser = APILocator.getUserAPI().getSystemUser();
			
			try {
				host = findByNameNotDefault(serverName, systemUser, respectFrontendRoles);
			} catch (Exception e) {
				return findDefaultHost(systemUser, respectFrontendRoles);
			}
			
			if(host == null){
				host = findByAlias(serverName, systemUser, respectFrontendRoles);
			}
			
			//If no host matches then we set the default host.
			if(host == null){
				host = findDefaultHost(systemUser, respectFrontendRoles);
			}
			
			if(host != null){
				hostCache.addHostAlias(serverName, host);
			}
		}
		
		if(APILocator.getPermissionAPI().doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
			return host;
		} else {
			String u = (user != null) ? user.getUserId() : null;
			String h = (host != null) ? host.getHostname() : null;
			throw new DotSecurityException("User: " +  u + " does not have read permissions to " + h );
		}
	}

	/**
	 *
	 * @param hostName
	 * @param user
	 * @param respectFrontendRoles
	 * @return the host with the passed in name or default in error case
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public Host findByName(String hostName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		
		try {
			return findByNameNotDefault(hostName, user, respectFrontendRoles);
		} catch (Exception e) {
			
			try {
				User systemUser = APILocator.getUserAPI().getSystemUser();
				return findDefaultHost(systemUser, respectFrontendRoles);
				
			} catch(Exception ex){
				throw new DotRuntimeException(e.getMessage(), e);
			}
		}
	}
	
	/**
	 *
	 * @param hostName
	 * @param user
	 * @param respectFrontendRoles
	 * @return the host with the passed in name or null in error case
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	private Host findByNameNotDefault(String hostName, User user, boolean respectFrontendRoles) {
		Host host = null;
		
		try{
			host  = hostCache.get(hostName);
			
			if(host != null){
				if(APILocator.getPermissionAPI().doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
					return host;
				}
			}
			
		} catch(Exception e){
			Logger.debug(HostAPIImpl.class, e.getMessage(), e);
		}

		try {
			Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
			String query = "+structureInode:" + st.getInode() +
					" +working:true +Host.hostName:" + hostName;

			List<Contentlet> list = APILocator.getContentletAPI().search(query, 0, 0, null, user, respectFrontendRoles);
			
			if(list.size() > 1) {
				Logger.fatal(this, "More of one host has the same name or alias = " + hostName + "!!");
				int i=0;
				
				for(Contentlet c : list){
					Logger.fatal(this, "\tdupe Host " + (i+1) + ": " + list.get(i).getTitle() );
					i++;
				}
				
			}else if (list.size() == 0){
				return null;
			}
			
			host = new Host(list.get(0));
			hostCache.add(host);

			return host;
			
		}  catch (Exception e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * @return the host with the passed in name
	 */
	public Host findByAlias(String alias, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Host host = null;

		try {
			Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");

			List<Contentlet> list = APILocator.getContentletAPI().search("+structureInode:" + st.getInode() +
					" +working:true +Host.aliases:" + alias, 0, 0, null, user, respectFrontendRoles);
			if(list.size() > 1){
				for(Contentlet cont: list){
					boolean isDefaultHost = (Boolean)cont.get("isDefault");
					if(isDefaultHost){
						host = new Host(cont);
						if(host.isDefault()){
							break;
						}
					}
				}
				if(host==null){
					Logger.error(this, "More of one host match the same alias " + alias + "!!");
					host = new Host(list.get(0));
				}
			}else if (list.size() == 0){
				return null;
			}else{
				host = new Host(list.get(0));
			}
			return host;
		} catch (Exception e) {
			Logger.error(HostAPIImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}


	public Host find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if(!UtilMethods.isSet(id)){
			return null;
		}
		if("SYSTEM_HOST".equals(id)){
			return findSystemHost();
		}
		Host host  = hostCache.get(id);

		if(host ==null){
		    HibernateUtil hu=new HibernateUtil(ContentletVersionInfo.class);
		    hu.setQuery("from "+ContentletVersionInfo.class.getName()+" where identifier=?");
		    hu.setParam(id);
		    ContentletVersionInfo vinfo=(ContentletVersionInfo) hu.load();
		    if(vinfo!=null && UtilMethods.isSet(vinfo.getIdentifier())) {
		        String hostInode=vinfo.getWorkingInode();
    			Contentlet cont= APILocator.getContentletAPI().find(hostInode, APILocator.getUserAPI().getSystemUser(), respectFrontendRoles);
    			Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
    			if(cont.getStructureInode().equals(st.getInode())) {
    			    host=new Host(cont);
    			    hostCache.add(host);
    			}
		    }
		}

		if(host != null){
			if(!APILocator.getPermissionAPI().doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
				String u = (user != null) ? user.getUserId() : null;

				String message = "User " + u + " does not have permission to host:" + host.getHostname();
				Logger.error(HostAPIImpl.class, message);
				throw new DotSecurityException(message);
			}
		}
		return host;

	}

	/**
	 * Retrieves the list of all hosts in the system
	 * @throws DotSecurityException
	 * @throws DotDataException
	 *
	 */
	public List<Host> findAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		try {
			Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
			List<Contentlet> list = APILocator.getContentletAPI().search("+structureInode:" + st.getInode() + " +working:true", 0, 0, null, user, respectFrontendRoles);
			return convertToHostList(list);
		} catch (Exception e) {
			Logger.error(HostAPIImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Retrieves the list of all hosts in the system
	 * @throws DotSecurityException
	 * @throws DotDataException
	 *
	 */
	public List<Host> findAllFromDB(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		List<Host> hosts =new ArrayList<Host>();

		String sql = "select  c.title, c.inode from contentlet_version_info clvi, contentlet c, structure s  " +
				" where c.structure_inode = s.inode and  s.name = 'Host' and c.identifier <> ? and clvi.working_inode = c.inode ";

		DotConnect dc = new DotConnect();
		dc.setSQL(sql);
		dc.addParam(Host.SYSTEM_HOST);
		@SuppressWarnings("unchecked")
		List<Map<String,String>> ret = dc.loadResults();
		for(Map<String,String> m : ret) {
			String inode=m.get("inode");
			Contentlet con=APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), false);
			hosts.add(new Host(con));
		}

		return hosts;
	}


	/**
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public Host save(Host host, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		if(host != null){
			hostCache.remove(host);
		}
		Contentlet c;
		try {
			c = APILocator.getContentletAPI().checkout(host.getInode(), user, respectFrontendRoles);
		} catch (DotContentletStateException e) {
			Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
			c = new Contentlet();
			c.setStructureInode(st.getInode());
		}
		APILocator.getContentletAPI().copyProperties(c, host.getMap());;
		c.setInode("");
		c = APILocator.getContentletAPI().checkin(c, user, respectFrontendRoles);

		if(host.isWorking() || host.isLive()){
			APILocator.getVersionableAPI().setLive(c);
		}
		Host savedHost =  new Host(c);

		updateDefaultHost(host, user, respectFrontendRoles);
		hostCache.clearAliasCache();
		return savedHost;

	}

	public void updateDefaultHost(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{
		// If host is marked as default make sure that no other host is already set to be the default
		if(host.isDefault()) {
			ContentletAPI conAPI = APILocator.getContentletAPI();
			List<Host> hosts= findAllFromDB(user, respectFrontendRoles);
			Host otherHost;
			Contentlet otherHostContentlet;
			for(Host h : hosts){
				if(h.getIdentifier().equals(host.getIdentifier())){
					continue;
				}
				// if this host is the default as well then ours should become the only one
				if(h.isDefault()){
					boolean isHostRunning = h.isLive();
					otherHostContentlet = APILocator.getContentletAPI().checkout(h.getInode(), user, respectFrontendRoles);
					otherHost =  new Host(otherHostContentlet);
					hostCache.remove(otherHost);
					otherHost.setDefault(false);
					if(host.getMap().containsKey(Contentlet.DONT_VALIDATE_ME))
					    otherHost.setProperty(Contentlet.DONT_VALIDATE_ME, true);
					if(host.getMap().containsKey(Contentlet.DISABLE_WORKFLOW))
					    otherHost.setProperty(Contentlet.DISABLE_WORKFLOW,true);

					Contentlet cont = conAPI.checkin(otherHost, user, respectFrontendRoles);
					if(isHostRunning) {
						otherHost = new Host(cont);
						publish(otherHost, user, respectFrontendRoles);
					}
				}
			}
		}
	}

	public List<Host> getHostsWithPermission(int permissionType, boolean includeArchived, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		try {
			Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
			List<Contentlet> list = APILocator.getContentletAPI().search("+structureInode:" + st.getInode() + " +working:true", 0, 0, null, user, respectFrontendRoles);
			list = APILocator.getPermissionAPI().filterCollection(list, permissionType, respectFrontendRoles, user);
			if (includeArchived) {
				return convertToHostList(list);
			} else {
				List<Host> hosts = convertToHostList(list);

				List<Host> filteredHosts = new ArrayList<Host>();
				for (Host host: hosts) {
					if (!host.isArchived())
						filteredHosts.add(host);
				}

				return filteredHosts;
			}
		} catch (Exception e) {
			Logger.error(HostAPIImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	public List<Host> getHostsWithPermission(int permissionType, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getHostsWithPermission(permissionType, true, user, respectFrontendRoles);
	}

	public Host findSystemHost (User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if(systemHost != null){
			return systemHost;
		}

		try {
		    String systemHostSql = "select id from identifier where id = ?";
			DotConnect db  = new DotConnect();
			db.setSQL(systemHostSql);
			db.addParam(Host.SYSTEM_HOST);
			List<Map<String, Object>> rs = db.loadObjectResults();
			if(rs.isEmpty()) {
			    // TODO: Be aware that this line may cause an infinite loop.
				createSystemHost();
			} else {
			    final String systemHostId = (String) rs.get(0).get("id");
			    this.systemHost = DBSearch(systemHostId, user, respectFrontendRoles);
			}
			if(rs.size() > 1){
				Logger.fatal(this, "There is more than one working version of the system host!!");
			}
		} catch (Exception e) {
			Logger.error(HostAPIImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return systemHost;
	}

	public Host findSystemHost () throws DotDataException {

		try {
			return findSystemHost(	APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotSecurityException e) {
			Logger.error(HostAPIImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

	}





	public Host findParentHost(Folder folder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if(folder.getIdentifier() !=null){
			return find(APILocator.getIdentifierAPI().find(folder.getIdentifier()).getHostId(), user, respectFrontendRoles);
		}
		return findDefaultHost(user, respectFrontendRoles);
	}

	public Host findParentHost(WebAsset asset, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if(asset.getIdentifier()!=null){
			return find(APILocator.getIdentifierAPI().find(asset.getIdentifier()).getHostId(), user, respectFrontendRoles);
		}

		return null;
	}

	public Host findParentHost(Treeable asset, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if(asset.getIdentifier()!=null){
			return find(APILocator.getIdentifierAPI().find(asset.getIdentifier()).getHostId(), user, respectFrontendRoles);
		}
		return null;
	}

	public boolean doesHostContainsFolder(Host parent, String folderName) throws DotDataException, DotSecurityException {
		List<Folder> trees = APILocator.getFolderAPI().findFoldersByHost(parent,APILocator.getUserAPI().getSystemUser(),false);
		for (Folder folder : trees) {
			if (folder.getName().equals(folderName))
				return true;
		}
		return false;

	}

	public void delete(final Host host, final User user, final boolean respectFrontendRoles) {
	    delete(host,user,respectFrontendRoles,false);
	}

	public void delete(final Host host, final User user, final boolean respectFrontendRoles, boolean runAsSepareThread) {


		class DeleteHostThread extends Thread {

			public void run() {
				try {
					deleteHost();
				} catch (Exception e) {
					// send notification
					try {
						final I18NMessage errorMessage = new I18NMessage("notifications_host_deletion_error",
								host.getHostname(), e.getMessage());

						APILocator.getNotificationAPI().generateNotification(
								new I18NMessage("notification.hostapi.delete.error.title"), // title = Host Notification
								errorMessage,
								null, // no actions
								NotificationLevel.ERROR,
								NotificationType.GENERIC,
								user.getUserId(),
								user.getLocale()
						);

					} catch (DotDataException e1) {
						Logger.error(HostAPIImpl.class, "error saving Notification", e);
					}

					Logger.error(HostAPIImpl.class, e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(), e);
				}
			}
			
			/**
			 * Gradually deletes a whole site (host) by removing each piece of
			 * content in it (e.g., removes old files, HTML and Content pages,
			 * links, contentlets, and so on).
			 * 
			 * @throws Exception
			 *             An error occurred when executing a delete method.
			 */
			public void deleteHost() throws Exception {
				if(host != null){
					hostCache.remove(host);
				}

				// Remove Old 1.9 Files
				FileAPI fileAPI = APILocator.getFileAPI();
				List<File> files = fileAPI.findFiles(user, true, null, host.getIdentifier(), null, null, null, 0, -1, null);
				for (File file : files) {
					fileAPI.delete(file, user, respectFrontendRoles);
				}

				DotConnect dc = new DotConnect();

				// Remove HTML Pages
				HTMLPageAPI htmlPageAPI = APILocator.getHTMLPageAPI();
				List<HTMLPage> pages = htmlPageAPI.findHtmlPages(user, true, null, host.getIdentifier(), null, null, null, 0, -1, null);
				for (HTMLPage page : pages) {
					htmlPageAPI.delete(page, user, respectFrontendRoles);
				}

				// Remove Links
				MenuLinkAPI linkAPI = APILocator.getMenuLinkAPI();
				List<Link> links = linkAPI.findLinks(user, true, null, host.getIdentifier(), null, null, null, 0, -1, null);
				for (Link link : links) {
					linkAPI.delete(link, user, respectFrontendRoles);
				}

				// Remove Contentlet
				ContentletAPI contentAPI = APILocator.getContentletAPI();
				contentAPI.deleteByHost(host, user, respectFrontendRoles);

				// Remove Folders
				FolderAPI folderAPI = APILocator.getFolderAPI();
				List<Folder> folders = folderAPI.findFoldersByHost(host, user, respectFrontendRoles);
				for (Folder folder : folders) {
					folderAPI.delete(folder, user, respectFrontendRoles);
				}

				// Remove Templates
				TemplateAPI templateAPI = APILocator.getTemplateAPI();
				List<Template> templates = templateAPI.findTemplates(user, true, null, host.getIdentifier(), null, null, null, 0, -1, null);
				for (Template template : templates) {
					dc.setSQL("delete from template_containers where template_id = ?");
					dc.addParam(template.getIdentifier());
					dc.loadResult();

					dc.setSQL("select inode, identifier from htmlpage where template_id = ?");
					dc.addParam(template.getIdentifier());
					List<HashMap<String, Object>> htmlpages =  dc.loadResults();
					for (HashMap<String, Object> folderMap : htmlpages) {
						String identifier = (String) folderMap.get("identifier");
						HTMLPage page = htmlPageAPI.loadWorkingPageById(identifier, user, respectFrontendRoles);
						htmlPageAPI.delete(page, user, respectFrontendRoles);
					}

					templateAPI.delete(template, user, respectFrontendRoles);
				}

				// Remove Containers
				ContainerAPI containerAPI = APILocator.getContainerAPI();
				List<Container> containers = containerAPI.findContainers(user, true, null, host.getIdentifier(), null, null, null, 0, -1, null);
				for (Container container : containers) {
					containerAPI.delete(container, user, respectFrontendRoles);
				}

				// Remove Structures
				List<Structure> structures = StructureFactory.getStructures(" host = '" + host.getIdentifier() + "'", null, 0, 0, null);
				for (Structure structure : structures) {
					List<Contentlet> structContent = contentAPI.findByStructure(structure, user, respectFrontendRoles, 0, 0);
					for (Contentlet c : structContent) {
						//We are deleting a site/host, we don't need to validate anything.
						c.setProperty(Contentlet.DONT_VALIDATE_ME, true);
						contentAPI.delete(c, user, respectFrontendRoles);
					}
					StructureFactory.deleteStructure(structure);
				}

				// wipe bad old containers
                dc.setSQL("delete from container_structures where exists (select * from identifier where host_inode=? and container_structures.container_id=id)");
                dc.addParam(host.getIdentifier());
                dc.loadResult();

				String[] assets = {Inode.Type.CONTAINERS.getTableName(),"template","htmlpage","links"};
				for(String asset : assets) {
				    dc.setSQL("select inode from "+asset+" where exists (select * from identifier where host_inode=? and id="+asset+".identifier)");
	                dc.addParam(host.getIdentifier());
	                for(Map row : (List<Map>)dc.loadResults()) {
	                    dc.setSQL("delete from "+asset+" where inode=?");
	                    dc.addParam(row.get("inode"));
	                    dc.loadResult();
	                }
				}

				// Double-check that ALL contentlets are effectively removed  
				// before using dotConnect to kill bad identifiers
				List<Contentlet> remainingContenlets = contentAPI
						.findContentletsByHost(host, user, respectFrontendRoles);
				if (remainingContenlets != null
						&& remainingContenlets.size() > 0) {
					contentAPI.deleteByHost(host, user, respectFrontendRoles);
				}
				
                // kill bad identifiers pointing to the host
                dc.setSQL("delete from identifier where host_inode=?");
                dc.addParam(host.getIdentifier());
                dc.loadResult();

				// Remove Host
				Contentlet c = contentAPI.find(host.getInode(), user, respectFrontendRoles);
				contentAPI.delete(c, user, respectFrontendRoles);
				hostCache.remove(host);
				hostCache.clearAliasCache();

			}
		}

		DeleteHostThread thread = new DeleteHostThread();

		if(runAsSepareThread) {
			thread.start();
		} else {
			thread.run();
		}
	}

	public void archive(Host host, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException,
			DotContentletStateException {
		if(host != null){
			hostCache.remove(host);
		}
		Contentlet c = APILocator.getContentletAPI().find(host.getInode(), user, respectFrontendRoles);
		//retrieve all hosts that have this current host as tag storage host
		List<Host> hosts = retrieveHostsPerTagStorage(host.getTagStorage(), user);
		for(Host h: hosts) {
			if(h.getIdentifier() != null){
				if(!h.getIdentifier().equals(host.getIdentifier())){
					//prevents changing tag storage for archived host.
					//the tag storage will change for all hosts which tag storage is archived host
					h.setTagStorage(h.getIdentifier());
					h = save(h, user, true);
				}
			}
		}
		APILocator.getContentletAPI().archive(c, user, respectFrontendRoles);
		host.setModDate(new Date ());
		hostCache.clearAliasCache();

		systemEventsAPI.push(SystemEventType.ARCHIVE_SITE, new Payload(c, Visibility.PERMISSION,
				String.valueOf(PermissionAPI.PERMISSION_READ)));
	}

	public void unarchive(Host host, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException,
			DotContentletStateException {
		if(host != null){
			hostCache.remove(host);
		}
		Contentlet c = APILocator.getContentletAPI().find(host.getInode(), user, respectFrontendRoles);
		APILocator.getContentletAPI().unarchive(c, user, respectFrontendRoles);
		host.setModDate(new Date ());
		hostCache.clearAliasCache();

		systemEventsAPI.push(SystemEventType.UN_ARCHIVE_SITE, new Payload(c, Visibility.PERMISSION,
				String.valueOf(PermissionAPI.PERMISSION_READ)));
	}

	private synchronized Host createDefaultHost() throws DotDataException,
	DotSecurityException {
    	Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
		List<Field> fields = FieldsCache.getFieldsByStructureInode(st.getInode());
		Field isDefault = null;
    	for(Field f : fields){
    		if("isDefault".equalsIgnoreCase(f.getVelocityVarName())){
    			isDefault=f;
    		}

    	}


		User systemUser = APILocator.getUserAPI().getSystemUser();



		DotConnect dc = new DotConnect();
		dc.setSQL("select working_inode from contentlet_version_info join contentlet on (contentlet.inode = contentlet_version_info.working_inode) " +
				  " where " + isDefault.getFieldContentlet() +" = ? and structure_inode =?");
		dc.addParam(true);
		dc.addParam(st.getInode());

		Host defaultHost = new Host();

		String inode = dc.getString("working_inode");
		if(!UtilMethods.isSet(inode)){

			defaultHost.setDefault(true);
			defaultHost.setHostname("localhost");


			for(Field f: defaultHost.getStructure().getFields()){
				if(f.isRequired() && UtilMethods.isSet(f.getDefaultValue())){


					defaultHost.setProperty(f.getVelocityVarName(), f.getDefaultValue());
				}

			}


			defaultHost = save(defaultHost, systemUser, false);
		} else {
			 defaultHost = new Host(APILocator.getContentletAPI().find(inode, systemUser, false));
		}

		hostCache.remove(defaultHost);

		return defaultHost;

	}


	private synchronized Host createSystemHost() throws DotDataException,
	DotSecurityException {

		User systemUser = APILocator.getUserAPI().getSystemUser();
		String systemHostSql = "select id from identifier where id = ?";
		DotConnect db  = new DotConnect();
		db.setSQL(systemHostSql);
		db.addParam(Host.SYSTEM_HOST);
		List<Map<String, Object>> rs = db.loadObjectResults();
		if(rs.isEmpty()) {
			Host systemHost = new Host();
			systemHost.setDefault(false);
			systemHost.setInode("dbdec5e2-c02b-4cfb-83a9-4a4c4f3b2eb0");
			systemHost.setHostname("system");
			systemHost.setSystemHost(true);
			systemHost.setHost(null);
			systemHost.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
			systemHost = new Host(conFac.save(systemHost));
			systemHost.setIdentifier(Host.SYSTEM_HOST);
			systemHost.setModDate(new Date());
			systemHost.setModUser(systemUser.getUserId());
			systemHost.setOwner(systemUser.getUserId());
			systemHost.setHost(null);
			systemHost.setFolder(null);
			conFac.save(systemHost);
			APILocator.getVersionableAPI().setWorking(systemHost);
			this.systemHost = systemHost;
		} else {
		    final String systemHostId = (String) rs.get(0).get("id");
            this.systemHost =  APILocator.getHostAPI().DBSearch(systemHostId, systemUser, false);
		}
		return systemHost;
	}
	
	private List<Host> convertToHostList(List<Contentlet> list) {
		List<Host> hosts = new ArrayList<Host>();
		for(Contentlet c : list) {
			hosts.add(new Host(c));
		}
		return hosts;
	}

	public void publish(Host host, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException {

		if(host != null){
			hostCache.remove(host);
		}
		Contentlet c = APILocator.getContentletAPI().find(host.getInode(), user, respectFrontendRoles);
		APILocator.getContentletAPI().publish(c, user, respectFrontendRoles);
		hostCache.add(host);
		hostCache.clearAliasCache();

	}

	public void unpublish(Host host, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException {
		if(host != null){
			hostCache.remove(host);
		}
		Contentlet c = APILocator.getContentletAPI().find(host.getInode(), user, respectFrontendRoles);
		APILocator.getContentletAPI().unpublish(c, user, respectFrontendRoles);
		hostCache.add(host);
		hostCache.clearAliasCache();
	}

	public void makeDefault(Host host, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException {
		host.setDefault(true);
		save(host, user, respectFrontendRoles);
	}

	public Host DBSearch(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if (!UtilMethods.isSet(id))
			return null;

		final String languageIdColumn = "language_id";
		final String isDefaultColumn = "isDefault";

		Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
		List<Field> fields = FieldsCache.getFieldsByStructureInode(st.getInode());

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT inode," + languageIdColumn);
		for (Field field: fields) {
			if (APILocator.getFieldAPI().valueSettable(field) && !field.getFieldType().equals(Field.FieldType.BINARY.toString())) {
				sql.append(", " + field.getVelocityVarName());
			}
		}
		sql.append(" FROM host ");
		sql.append("WHERE identifier='" + id + "'");
		
		SQLQueryFactory sqlQueryFactory = new SQLQueryFactory(sql.toString());
		Query query = sqlQueryFactory.getQuery();

		List<Map<String, Serializable>> list = APILocator.getContentletAPI().DBSearch(query, user, respectFrontendRoles);
		if (1 < list.size())
			Logger.error(this, "More of one working version of host match the same identifier " + id + "!!");
		else if (list.size() == 0)
			return null;

		Host host = new Host();
		
		for (String key: list.get(0).keySet()) {
			Object value = list.get(0).get(key);
			if ( key.equals(languageIdColumn) ) {
				if ( value instanceof Number){ //Hibernate maps Oracle NUMBER to BigDecimal.
					host.setProperty(Contentlet.LANGUAGEID_KEY, ((Number) value).longValue());
				} else {
					host.setProperty(Contentlet.LANGUAGEID_KEY, value);
				}
			} if (key.equals(isDefaultColumn)) { 
				host.setProperty(Host.IS_DEFAULT_KEY, DbConnectionFactory.isDBTrue(value.toString()));
			} else {
				host.setProperty(key, value);
			}
		}
		host.setProperty(Contentlet.MOD_DATE_KEY, new Date());//We don't really need this value for the system host but to avoid problems casting that field....
		if ("SYSTEM_HOST".equals(id)) {
			host.setProperty(Host.SYSTEM_HOST_KEY, true);
		}

		return host;
	}

	public void updateCache(Host host) {
		hostCache.remove(host);
		hostCache.clearAliasCache();
		hostCache.add(new Host(host));
	}

	public List<String> parseHostAliases(Host host) {
		List<String> ret = new ArrayList<String>();
		if(host.getAliases() == null){
			return ret;
		}
		StringTokenizer tok = new StringTokenizer(host.getAliases(), ", \n\r\t");
		while (tok.hasMoreTokens()) {
			ret.add(tok.nextToken());
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public void updateVirtualLinks(Host workinghost,Host updatedhost) throws DotDataException {//DOTCMS-5025

		String workingHostName = workinghost.getHostname();
		String updatedHostName = updatedhost.getHostname();
		if(!workingHostName.equals(updatedHostName)) {
    		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
    		List<VirtualLink> resultList = new ArrayList<VirtualLink>();
    		dh.setQuery("select inode from inode in class " + VirtualLink.class.getName() + " where lower(inode.url) like ?");
    		dh.setParam(workingHostName.toLowerCase()+":/%");
    		resultList = dh.list();
    		for (VirtualLink vl : resultList) {
    			String workingURL = vl.getUrl();
    			String newURL = updatedHostName.toLowerCase()+workingURL.substring(workingHostName.length());//gives url with updatedhostname
    			vl.setUrl(newURL);
    			HibernateUtil.saveOrUpdate(vl);
    			VirtualLinksCache.removePathFromCache(vl.getUrl());
    		}
		}
	}

	@SuppressWarnings("unchecked")
	public void updateMenuLinks(Host workinghost,Host updatedhost) throws DotDataException {//DOTCMS-5090

		String workingHostName = workinghost.getHostname();
		String updatedHostName = updatedhost.getHostname();
		if(!workingHostName.equals(updatedHostName)) {
    		HibernateUtil dh = new HibernateUtil(Link.class);
    		List<Link> resultList = new ArrayList<Link>();
    		dh.setQuery("select asset from asset in class " + Link.class.getName() + " where asset.url like ?");
    		dh.setParam(workingHostName+"/%");
    		resultList = dh.list();
    		for(Link link : resultList){
    			String workingURL = link.getUrl();
    			String newURL = updatedHostName+workingURL.substring(workingHostName.length());//gives url with updatedhostname
    			link.setUrl(newURL);
    			try {
                    APILocator.getMenuLinkAPI().save(link, APILocator.getUserAPI().getSystemUser(), false);
                } catch (DotSecurityException e) {
                    throw new RuntimeException(e);
                }
    		}
		}

	}

	public List<Host> retrieveHostsPerTagStorage (String tagStorageId, User user) {
		List<Host> hosts = new ArrayList<Host>();
		List<Host> allHosts = new ArrayList<Host>();
		try {
			allHosts = findAll(user, true);
		} catch (DotDataException e) {
			e.printStackTrace();
		} catch (DotSecurityException e) {
			e.printStackTrace();
		}

		if (allHosts.size() > 0) {
			for (Host h: allHosts) {
				if(h.isSystemHost())
					continue;
				if (h.getTagStorage() != null){
					if(h.getTagStorage().equals(tagStorageId))
						hosts.add(h);
				}
			}
		}

		return hosts;

	}
}
