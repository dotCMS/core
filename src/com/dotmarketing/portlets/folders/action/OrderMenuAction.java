package com.dotmarketing.portlets.folders.action;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;
/**
 * @author Maria
 */

public class OrderMenuAction extends DotPortletAction {

	public static boolean debug = false;

	private PermissionAPI perAPI= null;
	User user = null;
	FolderAPI fapi = APILocator.getFolderAPI();
	ActionRequest actionRequest = null;

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {

		perAPI = APILocator.getPermissionAPI();
		user = _getUser(req);
		actionRequest = req;

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
					String folderInode = (folder==null) ? FolderAPI.SYSTEM_FOLDER : folder.getInode();
					CacheLocator.getNavToolCache().removeNav(id.getHostId(), folderInode);
					CacheLocator.getHTMLPageCache().remove(id.getId());
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

			Folder folder = fapi.findFolderByPath(path, host, user, false);

			//gets menu items for this folder
			java.util.List<Inode> itemsList = fapi.findMenuItems(folder, user, false);

			//Cleaning the results to only display the ones under the current language.
			long currentLanguageId = WebAPILocator.getLanguageWebAPI().getLanguage(((ActionRequestImpl) actionRequest).getHttpServletRequest()).getId();

			//If we have the current language we can proceed to clean the HTML pages.
			if(UtilMethods.isSet(currentLanguageId)) {
				Iterator<Inode> itemsIter = itemsList.iterator();

				while(itemsIter.hasNext()) {
					Object item = itemsIter.next();

					//Make sure the object is HTMLPage.
					if(item instanceof HTMLPageAsset){
						//We need to make sure that the HTMLPage has the languageId. If not we leave in the results.
						if (UtilMethods.isSet(((HTMLPageAsset) item).getLanguageId())) {
							//If the values do not match, we remove the result from the list.
							if(currentLanguageId != ((HTMLPageAsset) item).getLanguageId()){
								itemsIter.remove();
							}
						}
					}
				}
			}

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

	/**
	 * Receives the updated order of the items in the menu list and saves the
	 * changes. Note that XSS validation is performed on the query String, so
	 * it's important to avoid using dangerous characters for parameter names.
	 * 
	 * @param req
	 *            - The HTTP request.
	 * @param res
	 *            - The HTTP response.
	 * @param config
	 * @param form
	 * @return
	 * @throws Exception
	 *             An error occurred during the save process or the query String
	 *             parameters may be incorrect.
	 */
	private List<Treeable> _orderMenuItemsDragAndDrop(ActionRequest req,
			ActionResponse res, PortletConfig config, ActionForm form)
			throws Exception {
		List<Treeable> ret = new ArrayList<Treeable>();
		try {
			Enumeration<?> parameterNames = req.getParameterNames();
			HashMap<String, HashMap<Integer, String>> hashMap = new HashMap<String, HashMap<Integer, String>>();
			while (parameterNames.hasMoreElements()) {
				String parameterName = (String) parameterNames.nextElement();
				if (parameterName.startsWith("list")) {
					String value = req.getParameter(parameterName);
					// Restore square brackets which are NOT allowed in URLs
					parameterName = parameterName.replaceAll("__", "[");
					parameterName = parameterName.replaceAll("---", "]");
					String smallParameterName = parameterName.substring(0,
							parameterName.indexOf("["));
					String indexString = parameterName.substring(
							parameterName.indexOf("[") + 1,
							parameterName.indexOf("]"));
					int index = Integer.parseInt(indexString);
					if (hashMap.get(smallParameterName) == null) {
						HashMap<Integer, String> hashInodes = new HashMap<Integer, String>();
						hashInodes.put(index, value);
						hashMap.put(smallParameterName, hashInodes);
					} else {
						HashMap<Integer, String> hashInodes = (HashMap<Integer, String>) hashMap
								.get(smallParameterName);
						hashInodes.put(index, value);
					}
				}
			}
			Set<String> keys = hashMap.keySet();
			Iterator<String> keysIterator = keys.iterator();
			while (keysIterator.hasNext()) {
				String key = keysIterator.next();
				HashMap<Integer, String> hashInodes = hashMap.get(key);
				for (int i = 0; i < hashInodes.size(); i++) {
					String inode = (String) hashInodes.get(i);
					Inode asset = (Inode) InodeFactory.getInode(inode,
							Inode.class);
					Contentlet c = null;
					try {
						c = APILocator.getContentletAPI().find(inode, user,
								false);
					} catch (ClassCastException cce) {
						// Continue
					}
					if (asset instanceof Folder) {
						((Folder) asset).setSortOrder(i);
						ret.add(((Folder) asset));
					}
					if (asset instanceof WebAsset && !asset.getType().equals("contentlet")) {
						((WebAsset) asset).setSortOrder(i);
						ret.add(((WebAsset) asset));
					}
					if (APILocator.getFileAssetAPI().isFileAsset(c)) {
						ret.add(c);
						c.setSortOrder(i);
						APILocator.getContentletAPI().refresh(c);
					}

					HibernateUtil.saveOrUpdate(asset);

					if(asset.getType().equals("contentlet")){
						ret.add(c);

						String[] listOfJustOne = {asset.getIdentifier()};
						List<Language> languagesList = APILocator.getLanguageAPI().getLanguages();

						for (Language language : languagesList) {

							try {
								List<Contentlet> contentlets = APILocator.getContentletAPI()
										.findContentletsByIdentifiers(listOfJustOne, true, language.getId(),
												APILocator.getUserAPI().getSystemUser(), false);

								for (Contentlet contentlet : contentlets) {
									Inode assetContent = InodeFactory.getInode(contentlet.getInode(), Inode.class);
									((WebAsset) assetContent).setSortOrder(i);
									HibernateUtil.saveOrUpdate(assetContent);

									CacheLocator.getContentletCache().remove(contentlet.getInode());
									CacheLocator.getHTMLPageCache().remove(contentlet.getInode());
									CacheLocator.getHTMLPageCache().remove(contentlet.getIdentifier());
								}
							} catch (DotContentletStateException e){ //This exception in case we don't have identifiers for that content.
								Logger.debug(this.getClass(),
										"No contents with identifier: " + asset.getIdentifier()
												+ " for language: " + language.getId());
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			Logger.error(this,
					"_orderMenuItemsDragAndDrop: Exception ocurred.", ex);
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
		l.add(buildNavigationTree(items, depth, user));
		l.add(new Boolean(hasMenuPubPer));
		return l;
	}

	/**
	 * Builds the navigation tree containing all the items in the list and the
	 * files, HTML pages, links, folders contained recursively by those items
	 * until the specified depth. This method will change later
	 *
	 * @param items
	 * @param depth
	 * @throws DotDataException
	 */
	protected List<Object> buildNavigationTree(List items, int depth, User user) throws DotDataException {
		depth = depth - 1;
		int level = 0;
		List<Object> v = new ArrayList<Object>();
		InternalCounter counter = new OrderMenuAction().new InternalCounter();
		counter.setCounter(0);
		List<Integer> ids = new ArrayList<Integer>();
		List l = buildNavigationTree(items, ids, level, counter, depth, user);
		StringBuffer sb = new StringBuffer("");
		if (l != null && l.size() > 0) {
			sb = (StringBuffer) l.get(0);
			sb.append("<script language='javascript'>\n");
			for (int i = ids.size() - 1; i >= 0; i--) {
				int internalCounter = (Integer) ids.get(i);
				String id = "list" + internalCounter;
				String className = "class" + internalCounter;
				String sortCreate = "Sortable.create(\"" + id + "\",{dropOnEmpty:true,tree:true,constraint:false,only:\"" + className
						+ "\"});\n";
				sb.append(sortCreate);
			}

			sb.append("\n");
			sb.append("function serialize(){\n");
			sb.append("var values = \"\";\n");
			for (int i = 0; i < ids.size(); i++) {
				int internalCounter = (Integer) ids.get(i);
				String id = "list" + internalCounter;
				String sortCreate = "values += \"&\" + Sortable.serialize('" + id + "');\n";
				sb.append(sortCreate);
			}
			sb.append("values = values.replace(/\\[/g, '__');\n");
			sb.append("values = values.replace(/\\]/g, '---');\n");
			sb.append("return values;\n");
			sb.append("}\n");

			sb.append("</script>\n");

			sb.append("<style>\n");
			for (int i = 0; i < ids.size(); i++) {
				int internalCounter = (Integer) ids.get(i);
				String className = "class" + internalCounter;
				String style = "li." + className + " { cursor: move;}\n";
				sb.append(style);
			}
			sb.append("</style>\n");
		}
		v.add(sb.toString());
		if (l != null && l.size() > 0) {
			v.add(l.get(1));
		} else {
			v.add(new Boolean(false));
		}

		return v;
	}

	/**
	 * Builds the navigation tree containing all the items and the files, HTML
	 * pages, links, folders contained recursively by those items in the list
	 * until the specified depth. This method will change later
	 *
	 * @param items
	 * @param ids
	 * @param level
	 * @param counter
	 * @param depth
	 * @throws DotDataException
	 */
	protected List buildNavigationTree(List items, List<Integer> ids, int level, InternalCounter counter, int depth, User user)
			throws DotDataException {
		boolean show = true;
		StringBuffer sb = new StringBuffer();
		List v = new ArrayList<Object>();
		int internalCounter = counter.getCounter();
		String className = "class" + internalCounter;
		String id = "list" + internalCounter;
		ids.add(internalCounter);
		counter.setCounter(++internalCounter);

		sb.append("<ul id='" + id + "' >\n");
		if (items != null) {
			Iterator itemsIter = items.iterator();
			while (itemsIter.hasNext()) {
				Permissionable item = (Permissionable) itemsIter.next();
				String title = "";
				String inode = "";
				if (item instanceof Folder) {
					Folder folder = ((Folder) item);
					title = folder.getTitle();
					title = retrieveTitle(title, user);
					inode = folder.getInode();
					if (folder.isShowOnMenu()) {
						if (!APILocator.getPermissionAPI().doesUserHavePermission(folder, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
							show = false;
						}
						if (APILocator.getPermissionAPI().doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, false)) {

							sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >\n" + title + "\n");
							List childs = fapi.findMenuItems(folder, user, false);
							if (childs.size() > 0) {
								int nextLevel = level + 1;
								if (depth > 0) {
									List<Object> l = getNavigationTree(childs, ids, nextLevel, counter, depth, user);
									if (show) {
										show = ((Boolean) l.get(1)).booleanValue();
									}
									sb.append((StringBuffer) (l.get(0)));
								}
							}
							sb.append("</li>\n");
						}
					}
				} else {
					if(item instanceof FileAsset){
						FileAsset fa =(FileAsset)item;
						title = fa.getTitle();
						title = retrieveTitle(title, user);
						inode = fa.getInode();
						if (fa.isShowOnMenu()) {
							if (!APILocator.getPermissionAPI().doesUserHavePermission(fa, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
								show = false;
							}
							if (APILocator.getPermissionAPI().doesUserHavePermission(fa, PermissionAPI.PERMISSION_READ, user, false)) {
								sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");
							}
						}
					}else if(item instanceof IHTMLPage){
						IHTMLPage asset = ((IHTMLPage) item);
						title = asset.getTitle();
						title = retrieveTitle(title, user);
						inode = asset.getInode();
						if (asset.isShowOnMenu()) {
							if (!APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
								show = false;
							}
							if (APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_READ, user, false)) {
								sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");
							}
						}
					}else{
						WebAsset asset = ((WebAsset) item);
						title = asset.getTitle();
						title = retrieveTitle(title, user);
						inode = asset.getInode();
						if (asset.isShowOnMenu()) {
							if (!APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
								show = false;
							}
							if (APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_READ, user, false)) {
								sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");
							}
						}
					}
				}
			}
		}

		sb.append("</ul>\n");
		v.add(sb);
		v.add(new Boolean(show));

		return v;
	}

	private String retrieveTitle(String title, User user) {
		try {
			String regularExpressionString = "(.*)\\$glossary.get\\('(.*)'\\)(.*)";
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regularExpressionString);
			Matcher matcher = pattern.matcher(title);
			if (matcher.matches()) {
				String tempTitle = matcher.group(2);
				tempTitle = matcher.group(1) + LanguageUtil.get(user, tempTitle) + matcher.group(3);
				title = tempTitle;
			}
		} catch (Exception ex) {
			Logger.warn(this.getClass(), "retrieveTitle failed:" + ex);
		}
		return title;
	}

	/**
	 * Gets the tree containing all the items in the list. If the item is a
	 * folder, gets the files and folders contained by the folder. It is
	 * executed recursively until reaching the depth specified. This method will
	 * change later
	 *
	 * @param items
	 * @param ids
	 * @param level
	 * @param counter
	 * @param depth
	 * @throws DotDataException
	 */
	protected List getNavigationTree(List items, List<Integer> ids, int level, InternalCounter counter, int depth, User user)
			throws DotDataException {
		boolean show = true;
		StringBuffer sb = new StringBuffer();
		List v = new ArrayList<Object>();
		int internalCounter = counter.getCounter();
		String className = "class" + internalCounter;
		String id = "list" + internalCounter;
		ids.add(internalCounter);
		counter.setCounter(++internalCounter);

		sb.append("<ul id='" + id + "' >\n");
		Iterator itemsIter = items.iterator();
		while (itemsIter.hasNext()) {
			Permissionable item = (Permissionable) itemsIter.next();
			String title = "";
			String inode = "";
			if (item instanceof Folder) {
				Folder folder = ((Folder) item);
				title = folder.getTitle();
				title = retrieveTitle(title, user);
				inode = folder.getInode();
				if (folder.isShowOnMenu()) {
					if (!APILocator.getPermissionAPI().doesUserHavePermission(folder, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
						show = false;
					}
					if (APILocator.getPermissionAPI().doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, false)) {

						sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >\n" + title + "\n");
						List childs = fapi.findMenuItems(folder, user, false);
						if (childs.size() > 0) {

							int nextLevel = level + 1;

							if (depth > 1) {
								sb.append(getNavigationTree(childs, ids, nextLevel, counter, depth-1, user).get(0));
							}
						}
						sb.append("</li>\n");
					}
				}
			} else {
				if(item instanceof FileAsset){
					FileAsset fa =(FileAsset)item;
					title = fa.getTitle();
					title = retrieveTitle(title, user);
					inode = fa.getInode();
					if (fa.isShowOnMenu()) {
						if (!APILocator.getPermissionAPI().doesUserHavePermission(fa, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
							show = false;
						}
						if (APILocator.getPermissionAPI().doesUserHavePermission(fa, PermissionAPI.PERMISSION_READ, user, false)) {
							sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");
						}
					}
				}else if(item instanceof IHTMLPage){
					IHTMLPage asset = ((IHTMLPage) item);
					title = asset.getTitle();
					title = retrieveTitle(title, user);
					inode = asset.getInode();
					if (asset.isShowOnMenu()) {
						if (!APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
							show = false;
						}

						//Cleaning the results to only display the ones under the current language.
						long currentLanguageId = WebAPILocator.getLanguageWebAPI().getLanguage(((ActionRequestImpl) actionRequest).getHttpServletRequest()).getId();

						//If we have the current language we can proceed to clean the HTML pages.
						if(UtilMethods.isSet(currentLanguageId)) {
							//We need to make sure that the HTMLPage has the languageId. If not we leave in the results.
							if (UtilMethods.isSet(asset.getLanguageId())) {
								//If the values match we include the page in the list.
								if(currentLanguageId == asset.getLanguageId()){
									if (APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_READ, user, false)) {
										sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");
									}
								}
							}
						}
					}
				}else{
					WebAsset asset = ((WebAsset) item);
					title = asset.getTitle();
					title = retrieveTitle(title, user);
					inode = asset.getInode();
					if (asset.isShowOnMenu()) {
						if (!APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_PUBLISH, user, false)) {
							show = false;
						}
						if (APILocator.getPermissionAPI().doesUserHavePermission(asset, PermissionAPI.PERMISSION_READ, user, false)) {
							sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");
						}
					}
				}
			}
		}
		sb.append("</ul>\n");
		v.add(sb);
		v.add(new Boolean(show));

		return v;
	}

	private class InternalCounter
	{
		private int counter;

		public int getCounter()
		{
			return counter;
		}

		public void setCounter(int counter)
		{
			this.counter = counter;
		}
	}

}
