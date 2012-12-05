package com.dotmarketing.business;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Parameter;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 *
 * @author will
 *
 */
public class IdentifierFactoryImpl extends IdentifierFactory {

	IdentifierCache ic = CacheLocator.getIdentifierCache();

	@Override
	protected List<Identifier> findByURIPattern(String assetType, String uri,boolean hasLive, boolean pullDeleted,boolean include,Host host)throws DotDataException {
		return findByURIPattern(assetType, uri, hasLive, pullDeleted,include, host, null, null);
	}
	
	@Override
	protected List<Identifier> findByURIPattern(String assetType, String uri, boolean hasLive,boolean onlyDeleted, boolean include, Host host, Date startDate, Date endDate) throws DotDataException {
		DotConnect dc = new DotConnect();
		StringBuilder bob = new StringBuilder("select distinct i.* from identifier i ");
		if(assetType.equals(new HTMLPage().getType())){
			bob.append("join " + assetType + "_version_info vi on (i.id = vi.identifier) join " + assetType + " a on (" + (hasLive ? "vi.live_inode":"vi.working_inode") + " = a.inode) ");
		}
		
		if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
			bob.append("where concat(parent_path, asset_name) ");
		}else if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)) {
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
	
	protected void updateIdentifierURI(Versionable webasset, Folder folder) throws DotDataException {

		Identifier identifier = find(webasset);
		Identifier folderId = find(folder);
		ic.removeFromCacheByVersionable(webasset);
		
		if (webasset instanceof HTMLPage) {
			identifier.setURI(folderId.getPath() + ((HTMLPage) webasset).getPageUrl());
		} else if (webasset instanceof File) {
			identifier.setURI(folderId.getPath() + ((File) webasset).getFileName());
		} else if (webasset instanceof Link) {
			identifier.setURI(folderId.getPath() + ((Link) webasset).getProtocal() + ((Link) webasset).getUrl());
		}

		else {
			identifier.setURI(folderId.getPath() + identifier.getInode());
		}

		saveIdentifier(identifier);

	}

	protected Identifier loadByURIFromCache(Host host, String uri) {
		return ic.getIdentifier(host, uri);
	}

	protected Identifier findByURI(Host host, String uri) throws DotHibernateException {
		return findByURI(host.getIdentifier(), uri);
	}

	protected Identifier findByURI(String hostId, String uri) throws DotHibernateException {

		Identifier identifier = ic.getIdentifier(uri, hostId);
		if (identifier != null) {
			return identifier;
		}

		HibernateUtil dh = new HibernateUtil(Identifier.class);
		String parentPath = uri.substring(0, uri.lastIndexOf("/") + 1);
		String assetName = uri.substring(uri.lastIndexOf("/") + 1);

		dh.setQuery("from identifier in class com.dotmarketing.beans.Identifier where parent_path=? and asset_name = ? and host_inode = ?");
		dh.setParam(parentPath);
		dh.setParam(assetName);
		dh.setParam(hostId);
		identifier = (Identifier) dh.load();

		ic.addIdentifierToCache(identifier);
		return identifier;
	}
	
	protected List<Identifier> findByParentPath(String hostId, String parent_path) throws DotHibernateException {
	    if(!parent_path.endsWith("/"))
	        parent_path=parent_path+"/";
	    
        HibernateUtil dh = new HibernateUtil(Identifier.class);
        dh.setQuery("from identifier in class com.dotmarketing.beans.Identifier where parent_path=? and host_inode = ?");
        dh.setParam(parent_path);
        dh.setParam(hostId);
        return (List<Identifier>) dh.list();
    }

	protected Identifier loadFromDb(String identifier) throws DotDataException, DotStateException {
		if (identifier == null) {
			throw new DotStateException("identifier is null");
		}

		HibernateUtil hu = new HibernateUtil(Identifier.class);
		return (Identifier) hu.load(identifier);

	}

	protected Identifier loadFromDb(Versionable versionable) throws DotDataException {
		if (versionable == null) {
			throw new DotStateException("versionable is null");
		}
		return loadFromDb(versionable.getVersionId());
	}

	protected Identifier loadFromCache(String identifier) {
		return ic.getIdentifier(identifier);
	}

	protected Identifier loadFromCache(Host host, String uri) {
		return ic.getIdentifier(host, uri);
	}

	protected Identifier loadFromCache(Versionable versionable) {

		String idStr= ic.getIdentifierFromInode(versionable);
		if(idStr ==null) return null;
		return ic.getIdentifier(idStr);
	}

	/**
	 * This method checks cache first, then db. If found in the database it will
	 * stick the result in the cache.
	 *
	 * @param inode
	 *            This takes an inode and finds the identifier for it.
	 * @return the Identifier inode
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	protected Identifier find(Versionable versionable) throws DotDataException {
		if (versionable == null) {
			throw new DotStateException("Versionable cannot be null");
		}
		Identifier id = null;
		String idStr = ic.getIdentifierFromInode(versionable);

		if(idStr !=null){
			id= find(idStr);
		}
		else{
			id= find(versionable.getVersionId());
			if(id != null){
				ic.addIdentifierToCache(id, versionable);
			}
		}
	

		return id;
		
		
	}
	
	protected Identifier createNewIdentifier(Versionable versionable, Folder folder) throws DotDataException {
	    return createNewIdentifier(versionable,folder,UUIDGenerator.generateUuid());
	}
	
	protected Identifier createNewIdentifier(Versionable versionable, Folder folder, String existingId) throws DotDataException {

		User systemUser = APILocator.getUserAPI().getSystemUser();
		HostAPI hostAPI = APILocator.getHostAPI();

		String uuid=existingId;

		Identifier identifier = new Identifier();
		Identifier parentId=APILocator.getIdentifierAPI().find(folder);

		if(versionable instanceof Folder) {
			identifier.setAssetType("folder");
			identifier.setAssetName(((Folder) versionable).getName());
		}
		else {
			String uri = versionable.getVersionType() + "." + versionable.getInode();
			identifier.setId(uuid);
			if(versionable instanceof Contentlet){
				Contentlet cont = (Contentlet)versionable;
				if(cont.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET){
					try {
						uri = cont.getBinary(FileAssetAPI.BINARY_FIELD)!=null?cont.getBinary(FileAssetAPI.BINARY_FIELD).getName():"";
						if(UtilMethods.isSet(cont.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)))//DOTCMS-7093
							uri = cont.getStringProperty(FileAssetAPI.FILE_NAME_FIELD);
					} catch (IOException e) {
						// TODO
					}
				}
				identifier.setAssetType("contentlet");
				identifier.setParentPath(parentId.getPath());
				identifier.setAssetName(uri);
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

		Host host;
		try {
			host = hostAPI.findParentHost(folder, systemUser, false);
		} catch (DotSecurityException e) {
			throw new DotStateException("I can't find the parent host!");
		}
		
		if("folder".equals(identifier.getAssetType()) && APILocator.getHostAPI().findSystemHost().getIdentifier().equals(host.getIdentifier())){
			throw new DotStateException("You cannot save a folder on the system host");
			
		}
		

		identifier.setHostId(host.getIdentifier());
		identifier.setParentPath(parentId.getPath());

		if(uuid!=null)
			HibernateUtil.saveWithPrimaryKey(identifier, uuid);
		else
			saveIdentifier(identifier);

		versionable.setVersionId(identifier.getId());

		return identifier;
	}
	
	protected Identifier createNewIdentifier ( Versionable versionable, Host host ) throws DotDataException {
	    return createNewIdentifier(versionable,host,UUIDGenerator.generateUuid());
	}

    /**
     * Creates a new Identifier for a given versionable asset under a given Host
     *
     * @param versionable
     * @param host
     * @return
     * @throws DotDataException
     */
    protected Identifier createNewIdentifier ( Versionable versionable, Host host, String existingId) throws DotDataException {

        String uuid = existingId;
        Identifier identifier = new Identifier();

        if ( versionable instanceof Folder ) {
            identifier.setAssetType( "folder" );
            identifier.setAssetName( ((Folder) versionable).getName() );
            identifier.setParentPath( "/" );
        } else {

            String uri = versionable.getVersionType() + "." + versionable.getInode();
            identifier.setId( uuid );

            if ( versionable instanceof Contentlet && ((Contentlet) versionable).getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET ) {

                // special case when it is a file asset as contentlet
                Contentlet cont = (Contentlet) versionable;
                try {
                    uri = cont.getBinary( FileAssetAPI.BINARY_FIELD ) != null ? cont.getBinary( FileAssetAPI.BINARY_FIELD ).getName() : "";
                } catch ( IOException e ) {
                    throw new DotDataException( e.getMessage(), e );
                }
                identifier.setAssetType( "contentlet" );
                identifier.setParentPath( "/" );
                identifier.setAssetName( uri );
            } else if ( versionable instanceof HTMLPage ) {
                identifier.setURI( '/' + ((HTMLPage) versionable).getPageUrl() );
            } else if ( versionable instanceof Link ) {
                identifier.setAssetName( versionable.getInode() );
                identifier.setParentPath("/");
            } else if(versionable instanceof Host) {
				identifier.setAssetName(versionable.getInode());
				identifier.setAssetType("contentlet");
				identifier.setParentPath("/");
				//identifier.setURI(uri);
			} else {
                identifier.setURI( uri );
            }

            identifier.setId( null );
        }

        identifier.setHostId( host != null ? host.getIdentifier() : null );

        if ( uuid != null ) {
            HibernateUtil.saveWithPrimaryKey( identifier, uuid );
        } else {
            saveIdentifier( identifier );
        }

        versionable.setVersionId( identifier.getId() );

        return identifier;
    }

	@SuppressWarnings("unchecked")
	protected List<Identifier> loadAllIdentifiers() throws DotHibernateException {
		HibernateUtil dh = new HibernateUtil(Identifier.class);
		List<Identifier> identList;
		// dh.setQuery("from inode in class com.dotmarketing.beans.Identifier where type='identifier' ");

		dh.setQuery("from identifier in class com.dotmarketing.beans.Identifier");
		identList = (List<Identifier>) dh.list();

		return identList;

	}

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

	// http://jira.dotmarketing.net/browse/DOTCMS-4970

	protected Identifier find(String x) throws DotStateException, DotDataException {

		Identifier id = ic.getIdentifier(x);
		if (id != null && UtilMethods.isSet(id.getInode())) {
			return id;
		}

		id = loadFromDb(x);

		ic.addIdentifierToCache(id);

		return id;
	}

	protected Identifier saveIdentifier(Identifier identifier) throws DotDataException {
		try {
			HibernateUtil.saveOrUpdate(identifier);
		} catch (DotHibernateException e) {
			Logger.error(IdentifierFactoryImpl.class, "saveIdentifier failed:" + e, e);
			throw new DotDataException(e.toString());
		}
		return identifier;
	}

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

			db.setSQL("select inode from "+ident.getAssetType()+" where inode=?");
			db.addParam(ident.getInode());
			List<Map<String,Object>> deleteme = db.loadResults();

			String versionInfoTable=UtilMethods.getVersionInfoTableName(ident.getAssetType());
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
			
			db.setSQL("delete from " + ident.getAssetType()+ " where identifier = ?");
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

			//HibernateUtil.delete(ident);
		} catch (Exception e) {
			Logger.error(IdentifierFactoryImpl.class, "deleteIdentifier failed:" + e, e);
			throw new DotDataException(e.toString());
		}
	}

}
