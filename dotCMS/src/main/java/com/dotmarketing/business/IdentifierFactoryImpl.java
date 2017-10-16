package com.dotmarketing.business;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Parameter;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

/**
 * Implementation class for the {@link IdentifierFactory}.
 * 
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 *
 */
public class IdentifierFactoryImpl extends IdentifierFactory {

	private IdentifierCache ic = CacheLocator.getIdentifierCache();

	@Override
	protected List<Identifier> findByURIPattern(String assetType, String uri,boolean hasLive, boolean pullDeleted,boolean include,Host host)throws DotDataException {
		return findByURIPattern(assetType, uri, hasLive, pullDeleted,include, host, null, null);
	}
	
	@Override
	protected List<Identifier> findByURIPattern(String assetType, String uri, boolean hasLive,boolean onlyDeleted, boolean include, Host host, Date startDate, Date endDate) throws DotDataException {
		DotConnect dc = new DotConnect();
		StringBuilder bob = new StringBuilder("select distinct i.* from identifier i ");
		
		if(DbConnectionFactory.isMySql()){
			bob.append("where concat(parent_path, asset_name) ");
		}else if (DbConnectionFactory.isMsSql()) {
			bob.append("where (parent_path + asset_name) ");
		}else {
			bob.append("where (parent_path || asset_name) ");
		}
		bob.append((include ? "":"NOT ") + "LIKE ? and host_inode = ? and asset_type = ? ");
		if(startDate != null){
			bob.append(" and vi.version_ts >= ? ");
		}
		if(endDate != null){
			bob.append(" and vi.version_ts <= ? ");
		}
		if(onlyDeleted){
			bob.append(" and vi.deleted=" + DbConnectionFactory.getDBTrue() + " ");
		}
		dc.setSQL(bob.toString());
		dc.addParam(uri.replace("*", "%"));
		dc.addParam(host.getIdentifier());
		dc.addParam(assetType);
		if(startDate != null){
			dc.addParam(startDate);
		}
		if(endDate != null){
			dc.addParam(endDate);
		}
		return convertDotConnectMapToPOJO(dc.loadResults());
	}

	/**
	 * 
	 * @param results
	 * @return
	 */
	private List<Identifier> convertDotConnectMapToPOJO(List<Map<String,String>> results){
		List<Identifier> ret = new ArrayList<Identifier>();
		if(results == null || results.size()==0){
			return ret;
		}
		
		for (Map<String, String> map : results) {
			Identifier i = new Identifier();
			i.setAssetName(map.get("asset_name"));
			i.setAssetType(map.get("asset_type"));
			i.setHostId(map.get("host_inode"));
			i.setId(map.get("id"));
			i.setParentPath(map.get("parent_path"));
			ret.add(i);
		}
		return ret;
	}

	@Override
	protected void updateIdentifierURI(Versionable webasset, Folder folder) throws DotDataException {
		Identifier identifier = find(webasset);
		Identifier folderId = find(folder);
		ic.removeFromCacheByVersionable(webasset);
		identifier.setURI(folderId.getPath() + identifier.getInode());
		if (webasset instanceof Contentlet){
			Contentlet c =(Contentlet) webasset;
			if ( c.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET ) {
				FileAsset fa = APILocator.getFileAssetAPI().fromContentlet( c );
				identifier.setURI( folderId.getPath() + fa.getFileName() );
			} else if ( c.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE ) {
				HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet( c );
				identifier.setAssetName( htmlPageAsset.getPageUrl() );
			}
		}
		saveIdentifier(identifier);
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	private Identifier check404(Identifier value) {
	    return value!=null && value.getAssetType()!=null && value.getAssetType().equals(IdentifierAPI.IDENT404) ? new Identifier() : value;
	}

	/**
	 * 
	 * @param hostId
	 * @param uri
	 * @return
	 */
	private Identifier build404(String hostId, String uri) {
	    Identifier obj = new Identifier();
	    obj.setHostId(hostId);
	    obj.setAssetName(uri);
	    obj.setParentPath(null);
	    obj.setId(null);
	    obj.setAssetType(IdentifierAPI.IDENT404);
	    return obj;
	}

	/**
	 * 
	 * @param x
	 * @return
	 */
	private Identifier build404(String x) {
        Identifier obj = new Identifier();
        obj.setHostId(null);
        obj.setAssetName(null);
        obj.setParentPath(null);
        obj.setId(x);
        obj.setAssetType(IdentifierAPI.IDENT404);
        return obj;
    }

	@Override
	protected Identifier loadByURIFromCache(Host host, String uri) {
		return check404(ic.getIdentifier(host, uri));
	}

	@Override
	protected Identifier findByURI(Host host, String uri) throws DotHibernateException {
		return findByURI(host.getIdentifier(), uri);
	}

	@Override
	protected Identifier findByURI(String siteId, String uri) throws DotHibernateException {
		Identifier identifier = ic.getIdentifier(siteId, uri);
		if (identifier != null) {
			return check404(identifier);
		}

		HibernateUtil dh = new HibernateUtil(Identifier.class);
		String parentPath = uri.substring(0, uri.lastIndexOf("/") + 1).toLowerCase();
		String assetName = uri.substring(uri.lastIndexOf("/") + 1).toLowerCase();
		dh.setQuery("from identifier in class com.dotmarketing.beans.Identifier where parent_path = ? and asset_name = ? and host_inode = ?");
		dh.setParam(parentPath);
		dh.setParam(assetName);
		dh.setParam(siteId);
		identifier = (Identifier) dh.load();
		
		if(identifier==null || !InodeUtils.isSet(identifier.getId())) {
		    identifier = build404(siteId,uri);
		}

		ic.addIdentifierToCache(identifier);
		return check404(identifier);
	}

	@Override
	protected List<Identifier> findByParentPath(String siteId, String parent_path) throws DotHibernateException {
	    if(!parent_path.endsWith("/")) {
	        parent_path=parent_path+"/";
	    }
	    parent_path = parent_path.toLowerCase();

        HibernateUtil dh = new HibernateUtil(Identifier.class);
        dh.setQuery("from identifier in class com.dotmarketing.beans.Identifier where parent_path = ? and host_inode = ?");
        dh.setParam(parent_path);
        dh.setParam(siteId);
        return (List<Identifier>) dh.list();
    }

	@Override
	protected Identifier loadFromDb(String identifier) throws DotDataException, DotStateException {
		if (identifier == null) {
			throw new DotStateException("identifier is null");
		}
		HibernateUtil hu = new HibernateUtil(Identifier.class);
		return (Identifier) hu.load(identifier);
	}

	@Override
	protected Identifier loadFromDb(Versionable versionable) throws DotDataException {
		if (versionable == null) {
			throw new DotStateException("versionable is null");
		}
		return loadFromDb(versionable.getVersionId());
	}

	@Override
	protected Identifier loadFromCache(String identifier) {
		return check404(ic.getIdentifier(identifier));
	}

	@Override
	protected Identifier loadFromCache(Host host, String uri) {
		return check404(ic.getIdentifier(host, uri));
	}

	@Override
	protected Identifier loadFromCache(Versionable versionable) {
		String idStr= ic.getIdentifierFromInode(versionable);
		if(idStr ==null) return null;
		return check404(ic.getIdentifier(idStr)); 
	}

	@Override
	protected Identifier loadFromCacheFromInode(String inode) {
		String idStr= ic.getIdentifierFromInode(inode);
		if(idStr ==null) return null;
		return check404(ic.getIdentifier(idStr)); 
	}

	@Override
	protected Identifier find(Versionable versionable) throws DotDataException {
		if (versionable == null) {
			throw new DotStateException("Versionable cannot be null");
		}
		Identifier id = null;
		String idStr = ic.getIdentifierFromInode(versionable);

		if(UtilMethods.isSet(idStr)){
			id= find(idStr);
		}
		else{
			id= find(versionable.getVersionId());
		}
		
		return id;
	}

	@Override
	protected Identifier createNewIdentifier(Versionable versionable, Folder folder) throws DotDataException {
	    return createNewIdentifier(versionable,folder,UUIDGenerator.generateUuid());
	}

	@Override
	protected Identifier createNewIdentifier(Versionable versionable, Folder folder, String existingId) throws DotDataException {
		User systemUser = APILocator.getUserAPI().getSystemUser();
		String uuid=existingId;
		Identifier identifier = new Identifier();
		Identifier parentId = APILocator.getIdentifierAPI().find(folder);
		if(versionable instanceof Folder) {
			identifier.setAssetType(Identifier.ASSET_TYPE_FOLDER);
			identifier.setAssetName(((Folder) versionable).getName().toLowerCase());
		} else {
			String uri = versionable.getVersionType() + "." + versionable.getInode();
			identifier.setId(uuid);
			if(versionable instanceof Contentlet){
				Contentlet cont = (Contentlet)versionable;
				if (cont.getStructure().getStructureType() == BaseContentType.FILEASSET.getType()) {
					try {
						uri = cont.getBinary(FileAssetAPI.BINARY_FIELD)!=null?cont.getBinary(FileAssetAPI.BINARY_FIELD).getName():"";
						if(UtilMethods.isSet(cont.getStringProperty(FileAssetAPI.FILE_NAME_FIELD))) {
							uri = cont.getStringProperty(FileAssetAPI.FILE_NAME_FIELD);
						}
					} catch (IOException e) {
						Logger.debug(this, "An error occurred while assigning Binary Field: " + e.getMessage());
					}
				} else if (cont.getStructure().getStructureType() == BaseContentType.HTMLPAGE.getType()) {
				    uri = cont.getStringProperty(HTMLPageAssetAPI.URL_FIELD) ;
				}
				identifier.setAssetType(Identifier.ASSET_TYPE_CONTENTLET);
				identifier.setParentPath(parentId.getPath());
				identifier.setAssetName(uri.toLowerCase());
			} else if (versionable instanceof WebAsset) {
				identifier.setURI(((WebAsset) versionable).getURI(folder));
				identifier.setAssetType(versionable.getVersionType());
				if(versionable instanceof Link)
				    identifier.setAssetName(versionable.getInode());
			} else{
				identifier.setURI(uri);
				identifier.setAssetType(versionable.getVersionType());
			}
			identifier.setId(null);
		}
		Host site;
		try {
			site = APILocator.getHostAPI().findParentHost(folder, systemUser, false);
		} catch (DotSecurityException e) {
			throw new DotStateException(
					String.format("Parent site of folder '%s' could not be found.", folder.getName()));
		}
		if(Identifier.ASSET_TYPE_FOLDER.equals(identifier.getAssetType()) && APILocator.getHostAPI().findSystemHost().getIdentifier().equals(site.getIdentifier())){
			throw new DotStateException("A folder cannot be saved on the system host.");
			
		}
		identifier.setHostId(site.getIdentifier());
		identifier.setParentPath(parentId.getPath());
		if(uuid!=null) {
			HibernateUtil.saveWithPrimaryKey(identifier, uuid);
			ic.removeFromCacheByIdentifier(identifier.getId());
			ic.removeFromCacheByURI(identifier.getHostId(), identifier.getURI());
		} else {
			saveIdentifier(identifier);
		}
		versionable.setVersionId(identifier.getId());
		return identifier;
	}

	@Override
	protected Identifier createNewIdentifier ( Versionable versionable, Host site ) throws DotDataException {
	    return createNewIdentifier(versionable,site,UUIDGenerator.generateUuid());
	}

	@Override
    protected Identifier createNewIdentifier ( Versionable versionable, Host site, String existingId) throws DotDataException {
        String uuid = existingId;
        Identifier identifier = new Identifier();
        if ( versionable instanceof Folder ) {
            identifier.setAssetType(Identifier.ASSET_TYPE_FOLDER);
			identifier.setAssetName(((Folder) versionable).getName().toLowerCase());
            identifier.setParentPath( "/" );
        } else {
            String uri = versionable.getVersionType() + "." + versionable.getInode();
            identifier.setId( uuid );
            if ( versionable instanceof Contentlet) {
                Contentlet cont = (Contentlet) versionable;
                if (cont.getStructure().getStructureType() == BaseContentType.FILEASSET.getType()) {
                    // special case when it is a file asset as contentlet
                    try {
                        uri = cont.getBinary( FileAssetAPI.BINARY_FIELD ) != null ? cont.getBinary( FileAssetAPI.BINARY_FIELD ).getName() : "";
                    } catch ( IOException e ) {
                        throw new DotDataException( e.getMessage(), e );
                    }
                } else if (cont.getStructure().getStructureType() == BaseContentType.HTMLPAGE.getType()) {
                    uri = cont.getStringProperty(HTMLPageAssetAPI.URL_FIELD) ;
                }
                identifier.setAssetType(Identifier.ASSET_TYPE_CONTENTLET);
                identifier.setParentPath( "/" );
                identifier.setAssetName( uri.toLowerCase() );
            } else if ( versionable instanceof Link ) {
                identifier.setAssetName( versionable.getInode() );
                identifier.setParentPath("/");
            } else if(versionable instanceof Host) {
				identifier.setAssetName(versionable.getInode());
				identifier.setAssetType(Identifier.ASSET_TYPE_CONTENTLET);
				identifier.setParentPath("/");
			} else {
                identifier.setURI( uri );
            }
            identifier.setId( null );
        }
        identifier.setHostId( site != null ? site.getIdentifier() : null );
        if ( uuid != null ) {
            HibernateUtil.saveWithPrimaryKey( identifier, uuid );
            ic.removeFromCacheByIdentifier(identifier.getId());
            ic.removeFromCacheByURI(identifier.getHostId(), identifier.getURI() );
        } else {
            saveIdentifier( identifier );
        }
        versionable.setVersionId( identifier.getId() );
        return identifier;
    }

	@Override
	protected List<Identifier> loadAllIdentifiers() throws DotHibernateException {
		HibernateUtil dh = new HibernateUtil(Identifier.class);
		List<Identifier> identList;
		dh.setQuery("from identifier in class com.dotmarketing.beans.Identifier");
		identList = (List<Identifier>) dh.list();
		return identList;
	}

	@Override
	protected boolean isIdentifier(String identifierInode) {
		DotConnect dc = new DotConnect();
		dc.setSQL("select count(*) as count from identifier where id = ?");
		dc.addParam(identifierInode);
		ArrayList<Map<String, String>> results = new ArrayList<Map<String, String>>();
		try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(IdentifierFactoryImpl.class, e.getMessage(), e);
		}
		int count = Parameter.getInt(results.get(0).get("count"), 0);
		if (count > 0) {
			return true;
		}
		return false;
	}

	@Override
	protected Identifier find(String x) throws DotStateException, DotDataException {
		Identifier id = ic.getIdentifier(x);
		if (id != null && UtilMethods.isSet(id.getId())) {
			return check404(id);
		}
		id = loadFromDb(x);
		if(id==null || !InodeUtils.isSet(id.getId())) {
		    id = build404(x);
		}
		ic.addIdentifierToCache(id);
		return check404(id);
	}

	@Override
	protected Identifier saveIdentifier(Identifier id) throws DotDataException {
		Identifier loadedObject = id;
		if ( id != null && UtilMethods.isSet(id.getId()) ) {
			// Load it from the db in order to avoid NonUniqueObjectException:
			// a different object with the same identifier value was already
			// associated with the session
			loadedObject = loadFromDb(id.getId());
			//Copy the changed properties back
			loadedObject.setAssetName(id.getAssetName().toLowerCase());
			loadedObject.setAssetType(id.getAssetType());
			loadedObject.setParentPath(id.getParentPath().toLowerCase());
			loadedObject.setHostId(id.getHostId());
			loadedObject.setOwner(id.getOwner());
			loadedObject.setSysExpireDate(id.getSysExpireDate());
			loadedObject.setSysPublishDate(id.getSysPublishDate());
		}
		try {
			HibernateUtil.saveOrUpdate(loadedObject);
		} catch (DotHibernateException e) {
			Logger.error(IdentifierFactoryImpl.class, "saveIdentifier failed:" + e, e);
			throw new DotDataException(e.toString());
		}
		ic.removeFromCacheByIdentifier(loadedObject.getId());
		ic.removeFromCacheByURI(loadedObject.getHostId(), loadedObject.getURI());
		id=null;
		return loadedObject;
	}

	@Override
	protected void deleteIdentifier(Identifier ident) throws DotDataException {
		DotConnect db = new DotConnect();
		try {
		    db.setSQL("delete from template_containers where template_id = ?");
		    db.addParam(ident.getId());
		    db.loadResult();
			db.setSQL("delete from permission where inode_id = ?");
			db.addParam(ident.getInode());
			db.loadResult();
			db.setSQL("delete from permission_reference where asset_id = ? or reference_id = ? ");
			db.addParam(ident.getInode());
			db.addParam(ident.getInode());
			db.loadResult();
			db.setSQL("delete from tree where child = ? or parent =?");
			db.addParam(ident.getInode());
			db.addParam(ident.getInode());
			db.loadResult();
			db.setSQL("delete from multi_tree where child = ? or parent1 =? or parent2 = ?");
			db.addParam(ident.getInode());
			db.addParam(ident.getInode());
			db.addParam(ident.getInode());
			db.loadResult();
			db.setSQL("select inode from "+ Inode.Type.valueOf(ident.getAssetType().toUpperCase()).getTableName() +" where inode=?");
			db.addParam(ident.getInode());
			List<Map<String,Object>> deleteme = db.loadResults();

			String versionInfoTable=Inode.Type.valueOf(ident.getAssetType().toUpperCase()).getVersionTableName();
			if(versionInfoTable!=null) {
			    db.setSQL("delete from "+versionInfoTable+" where identifier = ?");
			    db.addParam(ident.getId());
			    db.loadResult();
			}
			db.setSQL("select id from workflow_task where webasset = ?");
			db.addParam(ident.getId());
			List<Map<String,Object>> tasksToDelete=db.loadResults();
			for(Map<String,Object> task : tasksToDelete) {
			    WorkflowTask wft = APILocator.getWorkflowAPI().findTaskById((String)task.get("id"));
			    APILocator.getWorkflowAPI().deleteWorkflowTask(wft);
			}
			db.setSQL("delete from " + Inode.Type.valueOf(ident.getAssetType().toUpperCase()).getTableName() + " where identifier = ?");
			db.addParam(ident.getId());
			db.loadResult();

			StringWriter sw =  new StringWriter();
			sw.append(" ( '03d3k' ");
			for(Map<String,Object> m : deleteme){
				sw.append(",'" + m.get("inode") + "' ");
			}
			sw.append("  ) ");
			db.setSQL("delete from inode where inode in " + sw.toString());
			db.loadResult();

			ic.removeFromCacheByIdentifier(ident.getId());
			ic.removeFromCacheByURI(ident.getHostId(), ident.getURI());
		} catch (Exception e) {
			Logger.error(IdentifierFactoryImpl.class, "deleteIdentifier failed:" + e, e);
			throw new DotDataException(e.toString());
		}
	}

	@Override
	protected String getAssetTypeFromDB(String identifier) throws DotDataException{
		String assetType = null;
		try{
			DotConnect dotConnect = new DotConnect();
			List<Map<String, Object>> results = new ArrayList<Map<String,Object>>();
			// First try to search in Table Identifier.
			dotConnect.setSQL("SELECT asset_type FROM identifier WHERE id = ?");
			dotConnect.addParam(identifier);
			Connection connection = DbConnectionFactory.getConnection();
			results = dotConnect.loadObjectResults(connection);
			if(!results.isEmpty()){
				assetType = results.get(0).get("asset_type").toString();
			}
		} catch (DotDataException e) {
			Logger.error(IdentifierFactoryImpl.class, String
					.format("Error trying find the Asset Type from identifier=[%s]: %s", identifier, e.getMessage()));
			throw new DotDataException(String
					.format("Error trying find the Asset Type from identifier=[%s]", identifier), e);
		} finally{
			DbConnectionFactory.closeConnection();
		}
		return assetType;
	}

}
