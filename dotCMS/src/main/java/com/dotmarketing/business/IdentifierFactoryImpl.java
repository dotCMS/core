package com.dotmarketing.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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
import com.liferay.portal.model.User;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	protected List<Identifier> findByURIPattern(String assetType, String uri, boolean include, Host host) throws DotDataException {
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

		dc.setSQL(bob.toString());
		dc.addParam(uri.replace("*", "%"));
		dc.addParam(host.getIdentifier());
		dc.addParam(assetType);

		return convertDotConnectMapToPOJO(dc.loadResults());
	}

	/**
	 * 
	 * @param results
	 * @return
	 */
	private List<Identifier> convertDotConnectMapToPOJO(List<Map<String,String>> results){
		List<Identifier> ret = new ArrayList<>();
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
	protected Identifier findByURI(Host host, String uri) throws DotDataException {
		return findByURI(host.getIdentifier(), uri);
	}

	@Override
	protected Identifier findByURI(String siteId, String uri) throws DotDataException {
		Identifier identifier = ic.getIdentifier(siteId, uri);
		if (identifier != null) {
			return check404(identifier);
		}

		DotConnect dc = new DotConnect();
		String parentPath = uri.substring(0, uri.lastIndexOf("/") + 1).toLowerCase();
		String assetName = uri.substring(uri.lastIndexOf("/") + 1).toLowerCase();
		dc.setSQL("select * from identifier where parent_path = ? and asset_name = ? and host_inode = ?");
		dc.addParam(parentPath);
		dc.addParam(assetName);
		dc.addParam(siteId);

		List<Identifier> results = convertDotConnectMapToPOJO(dc.loadResults());

		if (results != null && results.size() > 0){
			identifier = results.get(0);
		}

		
		if(identifier==null || !InodeUtils.isSet(identifier.getId())) {
		    identifier = build404(siteId,uri);
		}

		ic.addIdentifierToCache(identifier);
		return check404(identifier);
	}

	@Override
	protected List<Identifier> findByParentPath(String siteId, String parent_path) throws DotDataException {
	    if(!parent_path.endsWith("/")) {
	        parent_path=parent_path+"/";
	    }
	    parent_path = parent_path.toLowerCase();

		DotConnect dc = new DotConnect();
		dc.setSQL("select * from identifier where parent_path = ? and host_inode = ?");
		dc.addParam(parent_path);
		dc.addParam(siteId);
        return convertDotConnectMapToPOJO(dc.loadResults());
    }

	@Override
	protected Identifier loadFromDb(String identifier) throws DotDataException, DotStateException {
		if (identifier == null) {
			throw new DotStateException("identifier is null");
		}

		DotConnect dc = new DotConnect();
		dc.setSQL("select * from identifier where id = ?");
		dc.addParam(identifier);

		List<Identifier> results = convertDotConnectMapToPOJO(dc.loadResults());

		if (results != null && results.size() > 0){
			return  results.get(0);
		}

		return null;
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
		Identifier identifier = new Identifier();

		if (existingId !=  null) {
			identifier.setId(existingId);
		}else {
			identifier.setId(UUIDGenerator.generateUuid());
		}
		Identifier parentId = APILocator.getIdentifierAPI().find(folder);
		if(versionable instanceof Folder) {
			identifier.setAssetType(Identifier.ASSET_TYPE_FOLDER);
			identifier.setAssetName(((Folder) versionable).getName().toLowerCase());
		} else {
			String uri = versionable.getVersionType() + "." + versionable.getInode();
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

		saveIdentifier(identifier);

		versionable.setVersionId(identifier.getId());
		return identifier;
	}

	@Override
	protected Identifier createNewIdentifier ( Versionable versionable, Host site ) throws DotDataException {
	    return createNewIdentifier(versionable,site,UUIDGenerator.generateUuid());
	}

	@Override
    protected Identifier createNewIdentifier ( Versionable versionable, Host site, String existingId) throws DotDataException {
        Identifier identifier = new Identifier();
        if (existingId !=  null) {
			identifier.setId(existingId);
		}else {
			identifier.setId(UUIDGenerator.generateUuid());
		}

        if ( versionable instanceof Folder ) {
            identifier.setAssetType(Identifier.ASSET_TYPE_FOLDER);
			identifier.setAssetName(((Folder) versionable).getName().toLowerCase());
            identifier.setParentPath( "/" );
        } else {
            String uri = versionable.getVersionType() + "." + versionable.getInode();
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
        }
        identifier.setHostId( site != null ? site.getIdentifier() : null );

        saveIdentifier( identifier );

        versionable.setVersionId( identifier.getId() );
        return identifier;
    }

	@Override
	protected List<Identifier> loadAllIdentifiers() throws DotDataException {

		DotConnect dc = new DotConnect();
		dc.setSQL("select * from identifier");

		return convertDotConnectMapToPOJO(dc.loadResults());

	}

	@Override
	protected boolean isIdentifier(String identifierInode) {
		DotConnect dc = new DotConnect();
		dc.setSQL("select count(1) as count from identifier where id = ?");
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
	protected Identifier find(String identifier) throws DotStateException, DotDataException {
		Identifier id = ic.getIdentifier(identifier);
		if (id != null && UtilMethods.isSet(id.getId())) {
			return check404(id);
		}
		id = loadFromDb(identifier);
		if(id==null || !InodeUtils.isSet(id.getId())) {
		    id = build404(identifier);
		}
		ic.addIdentifierToCache(id);
		return check404(id);
	}

	@Override
	protected Identifier saveIdentifier(Identifier id) throws DotDataException {
		String query;
		if (id != null) {
			if (UtilMethods.isSet(id.getId())) {

				if (isIdentifier(id.getId())) {
					query = "UPDATE identifier set parent_path=?, asset_name=?, host_inode=?, asset_type=?, syspublish_date=?, sysexpire_date=? where id=?";
				} else{
					query = "INSERT INTO identifier (parent_path,asset_name,host_inode,asset_type,syspublish_date,sysexpire_date,id) values (?,?,?,?,?,?,?)";
				}
			} else {
				id.setId(UUIDGenerator.generateUuid());
				query = "INSERT INTO identifier (parent_path,asset_name,host_inode,asset_type,syspublish_date,sysexpire_date,id) values (?,?,?,?,?,?,?)";
			}

			DotConnect dc = new DotConnect();
			dc.setSQL(query);

			dc.addParam(id.getParentPath());
			dc.addParam(id.getAssetName());
			dc.addParam(id.getHostId());
			dc.addParam(id.getAssetType());
			dc.addParam(id.getSysPublishDate());
			dc.addParam(id.getSysExpireDate());
			dc.addParam(id.getId());

			try{
				dc.loadResult();
			}catch(DotDataException e){
				Logger.error(IdentifierFactoryImpl.class, "saveIdentifier failed:" + e, e);
				throw new DotDataException(e.toString());
			}


			ic.removeFromCacheByIdentifier(id.getId());
			ic.removeFromCacheByURI(id.getHostId(), id.getURI());
			return id;
		}
		return null;
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
			List<Map<String, Object>> results;
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
