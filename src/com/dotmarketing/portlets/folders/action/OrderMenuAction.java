package com.dotmarketing.portlets.folders.action;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.util.servlet.SessionMessages;
/**
 * @author Maria
 */

public class OrderMenuAction extends DotPortletAction {

	public static boolean debug = false;

	private PermissionAPI perAPI= null;
	User user = null;
	FolderAPI fapi = APILocator.getFolderAPI();
	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
	throws Exception {

		perAPI = APILocator.getPermissionAPI();
		user = _getUser(req);

		try {
			String cmd = req.getParameter("cmd");

			int startLevel = Integer.parseInt(req.getParameter("startLevel"));
			int depth = Integer.parseInt(req.getParameter("depth"));

			req.setAttribute("depth", new Integer(depth));
			Hashtable h = _getMenuItems(req,res,config,form, startLevel);

			List<Object> items = (List)h.get("menuItems");
			boolean show = ((Boolean)h.get("showSaveButton")).booleanValue();

			Folder parentFolder = (Folder)h.get("mainMenuFolder");
			List<Object> l = _getHtmlTreeList(items, show, depth);

			if(!((Boolean)l.get(1)).booleanValue()){
				SessionMessages.add(req, "error", "error.menu.reorder.user_has_not_permission");
			}

			req.setAttribute("htmlTreeList", l.get(0));
			req.setAttribute("showSaveButton", l.get(1));
			List<Treeable> navs = new ArrayList<Treeable>();
			//This condition works while saving the reordered menu
			if (((cmd != null) && cmd.equals("generatemenu"))) {
				HibernateUtil.startTransaction();
				//regenerates menu files
				boolean doReorderMenu = false;
				if(l != null && (List)l.get(0) != null){
					doReorderMenu = ((Boolean)l.get(1)).booleanValue();
				}
				if(doReorderMenu){
					navs = _orderMenuItemsDragAndDrop(req,res,config,form);
				}else{
					Logger.warn(this, "Possible hack attack: User submitting menu post of which they have no permissions to");
					_sendToReferral(req,res,req.getParameter("referer"));
					return;
				}
				RefreshMenus.deleteMenus();

				HibernateUtil.commitTransaction();
				_sendToReferral(req,res,req.getParameter("referer"));
				
				
				// we have to clear navs after db commit
				for(Treeable treeable : navs){
					Identifier id = APILocator.getIdentifierAPI().find(treeable.getIdentifier());
					if("folder".equals(id.getAssetType())){
						CacheLocator.getNavToolCache().removeNavByPath(id.getHostId(), id.getPath());
					}
					Folder folder = APILocator.getFolderAPI().findParentFolder(treeable, user, false);
					String folderId = (folder==null) ? FolderAPI.SYSTEM_FOLDER : folder.getIdentifier();
					CacheLocator.getNavToolCache().removeNav(id.getHostId(), folderId);
				}
				
				return;
			}

			if (((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.REORDER))) {
				//prepublish
				_orderMenuItems(req,res,config,form);
			}

			//This part is executed while listing


			req.setAttribute("startLevel", new Integer(startLevel));
			req.setAttribute("depth", new Integer(depth));

			setForward(req,"portlet.ext.folders.order_menu");

		} catch (ActionException ae) {
			_handleException(ae,req);
		}
	}

	private Hashtable _getMenuItems(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, int startLevel)
	throws Exception {

		Hashtable h = new Hashtable();
		User user = _getUser(req);
		String pagePath = req.getParameter("pagePath");
		String hostId = req.getParameter("hostId");
		String path = null;

		String [] pathTokens = pagePath.split("/");
		if(startLevel <= 1){
			path = "/";
		}else{
			path = "";
			for(int i = 1; i < startLevel; i++){
				path += "/" + pathTokens[i];
			}
		}

		boolean userHasPublishPermission = true;
		Host host = APILocator.getHostAPI().find(hostId, user, true);
		if (!path.equals("/")) {

			Folder folder = fapi.findFolderByPath(path, host,user,false);

			//gets menu items for this folder
			java.util.List<Inode> itemsList = fapi.findMenuItems(folder,user,false);
			userHasPublishPermission = _findPublishPermissionExists(itemsList);
			h.put("menuItems", itemsList);
			req.setAttribute(WebKeys.MENU_MAIN_FOLDER,folder);
		}
		else {
			java.util.List<Folder> itemsList = fapi.findSubFolders(host, true);
			h.put("menuItems", itemsList);
			userHasPublishPermission = _findPublishPermissionExists(itemsList);
			req.setAttribute(WebKeys.MENU_ITEMS,itemsList);
		}

		h.put("showSaveButton", new Boolean(userHasPublishPermission));
		return h;
	}

	private List<Treeable> _orderMenuItemsDragAndDrop(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form)
	throws Exception {
		List<Treeable> ret = new ArrayList<Treeable>();
		try
		{
			Enumeration parameterNames = req.getParameterNames();
			HashMap<String,HashMap<Integer, String>> hashMap = new HashMap<String,HashMap<Integer, String>>();
			while(parameterNames.hasMoreElements())
			{
				String parameterName = (String) parameterNames.nextElement();
				if(parameterName.startsWith("list"))
				{
					String value = req.getParameter(parameterName);
					String smallParameterName = parameterName.substring(0,parameterName.indexOf("["));
					String indexString = parameterName.substring(parameterName.indexOf("[") + 1,parameterName.indexOf("]"));
					int index = Integer.parseInt(indexString);
					if(hashMap.get(smallParameterName) == null)
					{
						HashMap<Integer, String> hashInodes = new HashMap<Integer, String>();
						hashInodes.put(index,value);
						hashMap.put(smallParameterName,hashInodes);
					}
					else
					{
						HashMap<Integer, String> hashInodes = (HashMap<Integer, String>) hashMap.get(smallParameterName);
						hashInodes.put(index,value);
					}
				}
			}


			Set<String> keys = hashMap.keySet();
			Iterator keysIterator = keys.iterator();
			while(keysIterator.hasNext())
			{
				String key = (String) keysIterator.next();
				HashMap hashInodes = (HashMap) hashMap.get(key);

				for(int i = 0;i < hashInodes.size();i++)
				{
					String inode = (String) hashInodes.get(i);
					Inode asset = (Inode) InodeFactory.getInode(inode,Inode.class);
					Contentlet c = null;
					try {
						c = APILocator.getContentletAPI().find(inode, user, false);
					} catch(ClassCastException cce) {
					}

					if (asset instanceof Folder) {
						((Folder)asset).setSortOrder(i);
						ret.add(((Folder)asset));
					} 
					if (asset instanceof WebAsset)  {
						((WebAsset)asset).setSortOrder(i);
						ret.add(((WebAsset)asset));
					} 
					if (APILocator.getFileAssetAPI().isFileAsset(c))  {
						ret.add(c);
						c.setSortOrder(i);
						APILocator.getContentletAPI().refresh(c);
					}
					HibernateUtil.saveOrUpdate(asset);
				}
			}
		}
		catch(Exception ex)
		{
			Logger.error(this, "_orderMenuItemsDragAndDrop: Exception ocurred.", ex);
			throw ex;
		}
		return ret;
	}

	private void _orderMenuItems(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form)
	throws Exception {

		//gets item inode that's being moved
		String itemInode = req.getParameter("item");

		//gets folder object from folderParent inode
		Folder folder = (Folder) InodeFactory.getInode(req.getParameter("folderParent"),Folder.class);

		java.util.List itemsList = new ArrayList();

		if (InodeUtils.isSet(folder.getInode())) {
			//gets menu items for this folder parent
			itemsList = fapi.findMenuItems(folder,user,false);
		}
		else {
			//if i dont have a parent folder im ordering all the folder on the root for this folder
			String hostId = req.getParameter("hostId");
			Host host = APILocator.getHostAPI().find(hostId, user, false);
			itemsList = fapi.findSubFolders(host,true);
		}

		int increment = 0;

		if (req.getParameter("move").equals("up")) {
			//if it's up
			increment = -3;
		}
		else {
			//if it's down
			increment = 3;
		}

		Iterator i = itemsList.iterator();
		int x = 0;
		while (i.hasNext()) {

			Inode item = (Inode) i.next();
			Contentlet c = null;
			try {
				c = APILocator.getContentletAPI().find(item.getInode(), user, false);
			} catch(ClassCastException cce) {
			}

			if (item.getInode().equalsIgnoreCase( itemInode)) {
				//this is the item to move
				if (item instanceof Folder) {
					((Folder)item).setSortOrder(x + increment);
				}
				else if(item instanceof WebAsset) {
					((WebAsset)item).setSortOrder(x + increment);
				} if (APILocator.getFileAssetAPI().isFileAsset(c))  {
					c.setSortOrder(x + increment);
					APILocator.getContentletAPI().refresh(c);
				}
			}
			else {
				//all other items
				if (item instanceof Folder) {
					((Folder)item).setSortOrder(x);
				}
				else if(item instanceof WebAsset) {
					((WebAsset)item).setSortOrder(x);
				}  if (APILocator.getFileAssetAPI().isFileAsset(c))  {
					c.setSortOrder(x);
					APILocator.getContentletAPI().refresh(c);
				}
			}
			x = x + 2;
		}

		SessionMessages.add(req, "message", "message.menu.reordered");

	}

	/**
	 * This is a utility method that checks for the type of each permissionable object in a list and if at least one of them fails to have the permission,
	 * returns false
	 *
	 * @param itemsList
	 * @return
	 * @throws DotDataException
	 */
	private boolean _findPublishPermissionExists(List itemsList) throws DotDataException{
		boolean userHasPublishPermission = true;
		for(int i = 0; i < itemsList.size(); i++){
			if((itemsList.get(i) instanceof HTMLPage && !(perAPI.doesUserHavePermission((HTMLPage)itemsList.get(i), PermissionAPI.PERMISSION_PUBLISH, user)))){
				userHasPublishPermission = false;
			}
		}
		return userHasPublishPermission;
	}
	/**
	 * This method returns a List object which contains two elements. The first one is the String representing the HTML tree and the other one is
	 * a boolean indicating if the Save button will be shown to the user depending on the permissions
	 *
	 * @param items
	 * @param show
	 * @param depth
	 * @return
	 * @throws DotDataException
	 */
	private List _getHtmlTreeList(List items, boolean show, int depth) throws DotDataException{
		boolean userHasEditPermission = true;
		boolean hasMenuPubPer = show;

		//boolean showSaveButton = ((Boolean)request.getAttribute("editPermission")).booleanValue();

		//Iterator iterator = items.iterator();

		Object o= null;
		List<Object> v = new ArrayList();

		if(items != null){
			for(int i = 0; i < items.size(); i++){
				v.add(items.get(i));
			}
			items.clear();
			for(int i = 0; i < v.size(); i++){
				o = v.get(i);

				if(!perAPI.doesUserHavePermission((com.dotmarketing.business.Permissionable)o, PermissionAPI.PERMISSION_READ, user, false)){
					userHasEditPermission = false;
				}else{
					userHasEditPermission = true;
				}


				if(!userHasEditPermission){
					o =  null;
				}else{
					if(o instanceof Folder && !((Folder)(o)).isShowOnMenu()){
						o = null;
					}
					if((o instanceof File && !((File)(o)).isShowOnMenu())){
						o = null;
					}
					if((o instanceof Link && !((Link)(o)).isShowOnMenu())){
						o = null;
					}
					if((o instanceof HTMLPage && !((HTMLPage)(o)).isShowOnMenu())){
						o = null;
					}
				}
				if (o != null){
					items.add(o);
				}
			}
		}



		List<Object> l = new ArrayList();
		l.add(fapi.buildNavigationTree(items, depth, user));
		l.add(new Boolean(hasMenuPubPer));
		return l;
	}

}
