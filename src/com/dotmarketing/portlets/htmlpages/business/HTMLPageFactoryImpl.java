package com.dotmarketing.portlets.htmlpages.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPageVersionInfo;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class HTMLPageFactoryImpl implements HTMLPageFactory {
	static HTMLPageCache htmlPageCache = CacheLocator.getHTMLPageCache();

	public void save(HTMLPage htmlPage) throws DotDataException, DotStateException, DotSecurityException {
	    CacheLocator.getIdentifierCache().removeFromCacheByVersionable(htmlPage);
		
	    HibernateUtil.save(htmlPage);

		htmlPageCache.remove(htmlPage);

		WorkingCache.removeAssetFromCache(htmlPage);
		WorkingCache.addToWorkingAssetToCache(htmlPage);
		LiveCache.removeAssetFromCache(htmlPage);
		APILocator.getVersionableAPI().setWorking(htmlPage);
		if (htmlPage.isLive()) {
			LiveCache.addToLiveAssetToCache(htmlPage);
		}
	}

	public HTMLPage getLiveHTMLPageByPath(String path, Host host) throws DotDataException, DotSecurityException {
	    return getLiveHTMLPageByPath (path, host.getIdentifier());
	}

	public HTMLPage getLiveHTMLPageByPath(String path, String hostId) throws DotDataException, DotSecurityException {
		Host host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false);
        Identifier id = APILocator.getIdentifierAPI().find(host, path);

        Logger.debug(HTMLPageFactory.class, "Looking for page : " + path);
		Logger.debug(HTMLPageFactory.class, "got id " + id.getInode());

        //if this page does not exist, create it, add it to the course folder, use the course template, etc...
        if(!InodeUtils.isSet(id.getInode())){
            return  new HTMLPage();
        }

	    return (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(id,APILocator.getUserAPI().getSystemUser(),false);

	}

	public int findNumOfContent(HTMLPage page, Container container) {
		DotConnect dc = new DotConnect();
		StringBuffer buffy = new StringBuffer();
		buffy.append("select count(t.child) as contentletCount ");
		buffy.append("from multi_tree t ");
		buffy.append("where t.parent1 = ? and t.parent2 = ?");
		dc.setSQL(buffy.toString());
		dc.addParam(page.getInode());
		dc.addParam(container.getInode());
		int count = dc.getInt("contentletCount");
		return count;
	}


	public Folder getParentFolder(HTMLPage object) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		Folder folder = APILocator.getFolderAPI().findParentFolder(object, APILocator.getUserAPI().getSystemUser(),false);
		return folder;
	}

	public Host getParentHost(HTMLPage object) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		HostAPI hostAPI = APILocator.getHostAPI();

		Folder folder = APILocator.getFolderAPI().findParentFolder(object, APILocator.getUserAPI().getSystemUser(),false);
		Host host;
		try {
			User systemUser = APILocator.getUserAPI().getSystemUser();
			host = hostAPI.findParentHost(folder, systemUser, false);
		} catch (DotDataException e) {
			Logger.error(HTMLPageFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(HTMLPageFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return host;
	}

	@SuppressWarnings("unchecked")
	public HTMLPage loadWorkingPageById(String pageId) throws DotDataException {
    	HibernateUtil hu = new HibernateUtil(HTMLPage.class);
    	hu.setSQLQuery("select {htmlpage.*} from htmlpage, inode htmlpage_1_, htmlpage_version_info htmlvi " +
    			" where htmlpage_1_.inode = htmlpage.inode and htmlpage.identifier=htmlvi.identifier " +
    			" and htmlvi.working_inode=htmlpage.inode " +
    			" and htmlpage.identifier = ? ");
    	hu.setParam(pageId);
    	List<HTMLPage> pages = hu.list();
    	if(pages.size() == 0)
    		return null;
    	return pages.get(0);
	}

	@SuppressWarnings("unchecked")
	public HTMLPage loadLivePageById(String pageId) throws DotDataException, DotStateException, DotSecurityException {
		HTMLPage page = htmlPageCache.get(pageId);
		if(page ==null){
	    	HibernateUtil hu = new HibernateUtil(HTMLPage.class);
	    	hu.setSQLQuery("select {htmlpage.*} from htmlpage, inode htmlpage_1_, htmlpage_version_info htmlvi " +
	    			" where htmlpage_1_.inode = htmlpage.inode and htmlpage.identifier=htmlvi.identifier" +
	    			" and htmlvi.live_inode=htmlpage.inode " +
	    			" and htmlpage.identifier = ? ");
	    	hu.setParam(pageId);
	    	List<HTMLPage> pages = hu.list();
	    	if(pages.size() == 0)
	    		return null;

	    	page = pages.get(0);
	    	htmlPageCache.add(page);

		}
    	return page;
	}
	public List<HTMLPage> findHtmlPages(User user, boolean includeArchived,
			Map<String, Object> params, String hostId, String inode, String identifier, String parent,
			int offset, int limit, String orderBy) throws DotSecurityException,
			DotDataException {

		PaginatedArrayList<HTMLPage> assets = new PaginatedArrayList<HTMLPage>();
		List<Permissionable> toReturn = new ArrayList<Permissionable>();
		int internalLimit = 500;
		int internalOffset = 0;
		boolean done = false;

		StringBuffer conditionBuffer = new StringBuffer();

		List<Object> paramValues =null;
		if(params!=null && params.size()>0){
			conditionBuffer.append(" and (");
			paramValues = new ArrayList<Object>();
			int counter = 0;
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				if(counter==0){
					if(entry.getValue() instanceof String){
						if(entry.getKey().equalsIgnoreCase("inode")){
							conditionBuffer.append(" asset." + entry.getKey()+ " = '" + entry.getValue() + "'");
						}else{
							conditionBuffer.append(" lower(asset." + entry.getKey()+ ") like ? ");
							paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
						}
					}else{
						conditionBuffer.append(" asset." + entry.getKey()+ " = " + entry.getValue());
					}
				}else{
					if(entry.getValue() instanceof String){
						if(entry.getKey().equalsIgnoreCase("inode")){
							conditionBuffer.append(" OR asset." + entry.getKey()+ " = '" + entry.getValue() + "'");
						}else{
							conditionBuffer.append(" OR lower(asset." + entry.getKey()+ ") like ? ");
							paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
						}
					}else{
						conditionBuffer.append(" OR asset." + entry.getKey()+ " = " + entry.getValue());
					}
				}

				counter+=1;
			}
			conditionBuffer.append(" ) ");
		}

		StringBuffer query = new StringBuffer();
		query.append("select asset from asset in class " + HTMLPage.class.getName() + ", " +
				"inode in class " + Inode.class.getName()+", identifier in class " + Identifier.class.getName() +
				((!includeArchived)? (", htmlvi in class "+HTMLPageVersionInfo.class.getName()) : ""));
		if(UtilMethods.isSet(parent)){
			if(APILocator.getIdentifierAPI().find(parent).getAssetType().equals(new Template().getType()))
			  query.append(" where asset.inode = inode.inode and asset.identifier = identifier.id and asset.templateId = '"+parent+"' ");
			else
				query.append(" ,tree in class " + Tree.class.getName() + " where asset.inode = inode.inode " +
					    "and asset.identifier = identifier.id and tree.parent = '"+parent+"' and tree.child=asset.inode");
		}else{
			query.append(" where asset.inode = inode.inode and asset.identifier = identifier.id");
		}
		if(!includeArchived)
		    query.append(" and identifier.id=htmlvi.identifier and htmlvi.workingInode=asset.inode and htmlvi.deleted="+DbConnectionFactory.getDBFalse());
		if(UtilMethods.isSet(hostId)){
			query.append(" and identifier.hostId = '"+ hostId +"'");
		}
		if(UtilMethods.isSet(inode)){
			query.append(" and asset.inode = '"+ inode +"'");
		}
		if(UtilMethods.isSet(identifier)){
			query.append(" and asset.identifier = '"+ identifier +"'");
		}
		if(!UtilMethods.isSet(orderBy)){
			orderBy = "modDate desc";
		}

		List<HTMLPage> resultList = new ArrayList<HTMLPage>();
		HibernateUtil dh = new HibernateUtil(HTMLPage.class);
		String type;
		int countLimit = 100;
		int size = 0;
		try {
			type = ((Inode) HTMLPage.class.newInstance()).getType();
			query.append(" and asset.type='"+type+ "' " + conditionBuffer.toString() + " order by asset." + orderBy);
			dh.setQuery(query.toString());

			if(paramValues!=null && paramValues.size()>0){
				for (Object value : paramValues) {
					dh.setParam((String)value);
				}
			}

			while(!done) {
				dh.setFirstResult(internalOffset);
				dh.setMaxResults(internalLimit);
				resultList = dh.list();
				PermissionAPI permAPI = APILocator.getPermissionAPI();
				toReturn.addAll(permAPI.filterCollection(resultList, PermissionAPI.PERMISSION_READ, false, user));
				if(countLimit > 0 && toReturn.size() >= countLimit + offset)
					done = true;
				else if(resultList.size() < internalLimit)
					done = true;

				internalOffset += internalLimit;
			}

			if(offset > toReturn.size()) {
				size = 0;
			} else if(countLimit > 0) {
				int toIndex = offset + countLimit > toReturn.size()?toReturn.size():offset + countLimit;
				size = toReturn.subList(offset, toIndex).size();
			} else if (offset > 0) {
				size = toReturn.subList(offset, toReturn.size()).size();
			}

			assets.setTotalResults(size);

			if(limit!=-1) {
				int from = offset<toReturn.size()?offset:0;
				int pageLimit = 0;
				for(int i=from;i<toReturn.size();i++){
					if(pageLimit<limit){
						assets.add((HTMLPage) toReturn.get(i));
						pageLimit+=1;
					}else{
						break;
					}

				}
			} else {
				for(int i=0;i<toReturn.size();i++){
					assets.add((HTMLPage) toReturn.get(i));
				}
			}

		} catch (Exception e) {

			Logger.error(HTMLPageFactoryImpl.class, "findHtmlPages failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return assets;
	}

	/**
     * Move a file into the given directory
     *
     * @param page
     *            File to be moved
     * @param newFolder
     *            Destination Folder
     * @return true if move success, false otherwise
     * @throws DotDataException
     * @throws DotStateException
     * @throws DotSecurityException
     */
    public  boolean movePage(HTMLPage page, Folder newFolder) throws DotStateException, DotDataException, DotSecurityException {

		Identifier identifier = APILocator.getIdentifierAPI().find(page);

		if (page.isWorking()) {
			// gets identifier for this webasset and changes the uri and
			// persists it
			identifier.setHostId(APILocator.getHostAPI().findParentHost(newFolder, APILocator.getUserAPI().getSystemUser(), false).getIdentifier());
			identifier.setURI(page.getURI(newFolder));
			APILocator.getIdentifierAPI().save(identifier);
		}

		// Add to Preview and Live Cache
		if (page.isLive()) {
			LiveCache.removeAssetFromCache(page);
			LiveCache.addToLiveAssetToCache(page);
		}
		if (page.isWorking()) {
			WorkingCache.removeAssetFromCache(page);
			WorkingCache.addToWorkingAssetToCache(page);
			CacheLocator.getIdentifierCache().removeFromCacheByVersionable(page);

		}

		// republishes the page to reset the VTL_SERVLETURI variable
		if (page.isLive()) {
			PageServices.invalidate(page);
		}

		return true;

    }
}