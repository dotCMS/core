package com.dotmarketing.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
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
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
	protected List<Identifier> findByURIPattern(final String assetType, String uri, boolean include, Host host) throws DotDataException {
		DotConnect dc = new DotConnect();
		StringBuilder bob = new StringBuilder("select distinct i.* from identifier i ");

		bob.append("where i.full_path_lc ");
		bob.append((include ? "":"NOT ") + "LIKE ? and host_inode = ? and asset_type = ? ");

		dc.setSQL(bob.toString());
		dc.addParam(uri.replace("*", "%").toLowerCase());
		dc.addParam(host.getIdentifier());
		dc.addParam(assetType);



		return TransformerLocator.createIdentifierTransformer(dc.loadObjectResults()).asList();


	}

	@Override
	protected void updateIdentifierURI(Versionable webasset, Folder folder) throws DotDataException {
		Identifier identifier = find(webasset);
		Identifier folderId = find(folder.getIdentifier());
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
		final boolean is404 = value!=null && value.getAssetType()!=null && value.getAssetType().equals(IdentifierAPI.IDENT404);
		if(is404){
			Logger.debug(this, "404 Identifier found: " + value.toString());
		}
		return is404 ? new Identifier() : value;
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
	protected Identifier findByURI(final Host host, String uri) throws DotDataException {
		return findByURI(host.getIdentifier(), uri);
	}

	@Override
	protected Identifier findByURI(final String siteId, final String uri) throws DotDataException {
		Identifier identifier = ic.getIdentifier(siteId, uri);
		if (identifier != null) {
			return check404(identifier);
		}

		final DotConnect dc = new DotConnect();
		final String parentPath = uri.substring(0, uri.lastIndexOf("/") + 1).toLowerCase();
		final String assetName = uri.substring(uri.lastIndexOf("/") + 1).toLowerCase();
		dc.setSQL("select * from identifier i where i.full_path_lc = ? and host_inode = ?");
		dc.addParam((parentPath + assetName).toLowerCase());
		dc.addParam(siteId);

		List<Identifier> results = null;
		

		results = TransformerLocator.createIdentifierTransformer(dc.loadObjectResults()).asList();



		if (results != null && !results.isEmpty()){
			identifier = results.get(0);
		}


		if(identifier==null || !InodeUtils.isSet(identifier.getId())) {
		    identifier = build404(siteId,uri);
		}

		ic.addIdentifierToCache(identifier);
		return check404(identifier);
	}

	@Override
	protected List<Identifier> findByParentPath(final String siteId, String parentPath) throws DotDataException {

	    final Identifier identifier = findByURI(siteId, parentPath);
        if(!UtilMethods.isSet(identifier) || !UtilMethods.isSet(identifier.getId()) ){
           return Collections.emptyList();
        }
        parentPath = identifier.getURI();

	    if(!parentPath.endsWith("/")) {
	        parentPath=parentPath+"/";
	    }

		DotConnect dc = new DotConnect();
		dc.setSQL("select * from identifier where parent_path = ? and host_inode = ?");
		dc.addParam(parentPath);
		dc.addParam(siteId);

		return TransformerLocator.createIdentifierTransformer(dc.loadObjectResults()).asList();

	}

	@Override
	protected Identifier loadFromDb(String identifier) throws DotDataException, DotStateException {
		if (identifier == null) {
			throw new DotStateException("identifier is null");
		}

		DotConnect dc = new DotConnect();
		dc.setSQL("select * from identifier where id = ?");
		dc.addParam(identifier);

		List<Identifier> results = null;


		results = TransformerLocator.createIdentifierTransformer(dc.loadObjectResults()).asList();



		return  (results != null && !results.isEmpty())?results.get(0):null;

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
	protected Identifier createNewIdentifier(final Folder folder, final Folder parent, final String existingId) throws DotDataException {
		final Identifier identifier = new Identifier();

		identifier.setId(existingId!=null?existingId:UUIDGenerator.generateUuid());

		identifier.setAssetType(Identifier.ASSET_TYPE_FOLDER);
		identifier.setAssetName(folder.getName());
		identifier.setOwner(folder.getOwner());

		if (Host.SYSTEM_HOST.equals(parent.getHostId())) {
			Logger.error(this, "A folder cannot be saved on the system host.");
			throw new DotStateException("A folder cannot be saved on the system host.");

		}

		identifier.setHostId(parent.getHostId());
		identifier.setParentPath(parent.getPath());

		identifier.setCreateDate(folder.getIDate()!=null? folder.getIDate():new Date());
		saveIdentifier(identifier);
		folder.setIdentifier(identifier.getId());
		folder.setPath(identifier.getPath());
		return identifier;
	}

	@Override
	protected Identifier createNewIdentifier (final Folder folder, final Host site, final String existingId) throws DotDataException {
		Identifier identifier = new Identifier();
		if (existingId !=  null) {
			identifier.setId(existingId);
		}else {
			identifier.setId(UUIDGenerator.generateUuid());
		}

		identifier.setAssetType(Identifier.ASSET_TYPE_FOLDER);
		identifier.setAssetName(folder.getName());
		identifier.setParentPath( "/" );
		identifier.setOwner(folder.getOwner());
		identifier.setHostId( site != null ? site.getIdentifier() : null );
		identifier.setCreateDate(folder.getIDate()!=null? folder.getIDate():new Date());

		saveIdentifier( identifier );

		folder.setIdentifier(identifier.getId());
		folder.setPath(identifier.getPath());
		return identifier;
	}

	@Override
	protected Identifier createNewIdentifier(final Versionable versionable, final Folder folder, final String existingId) throws DotDataException {
		final Identifier identifier = new Identifier();

		identifier.setId(existingId!=null?existingId:UUIDGenerator.generateUuid());

		String uri = versionable.getVersionType() + "." + versionable.getInode();
		if(versionable instanceof Contentlet){
			final Contentlet contentlet = (Contentlet)versionable;
			final boolean isCopyContentlet = contentlet.getBoolProperty(Contentlet.IS_COPY_CONTENTLET);
			if (contentlet.isFileAsset()) {
				final String contentletAssetNameCopy = contentlet.getStringProperty(Contentlet.CONTENTLET_ASSET_NAME_COPY);
				if (isCopyContentlet && UtilMethods.isSet(contentletAssetNameCopy)) {
					uri = contentletAssetNameCopy;
				} else {
					try {
						uri = contentlet.getBinary(FileAssetAPI.BINARY_FIELD) != null
								? contentlet.getBinary(FileAssetAPI.BINARY_FIELD).getName()
								: StringPool.BLANK;
						if (UtilMethods.isSet(contentlet
								.getStringProperty(FileAssetAPI.FILE_NAME_FIELD))) {
							uri = contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD);
						}
					} catch (IOException e) {
						Logger.debug(this,
								"An error occurred while assigning Binary Field: " + e
										.getMessage());
					}
				}
			} else if (contentlet.isHTMLPage()) {
				uri = contentlet.getStringProperty(HTMLPageAssetAPI.URL_FIELD) ;
			}
			identifier.setAssetType(Identifier.ASSET_TYPE_CONTENTLET);
			identifier.setParentPath(folder.getPath());
			identifier.setAssetName(uri);
			identifier.setAssetSubType(contentlet.getContentType().variable());
		} else if (versionable instanceof WebAsset) {
			identifier.setURI(((WebAsset) versionable).getURI(folder));
			identifier.setAssetType(versionable.getVersionType());
			if(versionable instanceof Link)
				identifier.setAssetName(versionable.getInode());
		} else{
			identifier.setURI(uri);
			identifier.setAssetType(versionable.getVersionType());
		}

		identifier.setOwner((versionable instanceof WebAsset)
				? ((WebAsset) versionable).getOwner() : versionable.getModUser());

		identifier.setHostId(folder.getHostId());
		identifier.setParentPath(folder.getPath());

        final Inode inode;
        try {
            if (versionable.getInode() != null) {
                inode = InodeUtils.getInode(versionable.getInode());
                identifier.setCreateDate(inode.getIDate());
            } else {
                identifier.setCreateDate(new Date());
            }
        } catch (DotSecurityException e) {
            throw new DotStateException(e.getMessage(), e);
        }


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
			identifier.setAssetName(((Folder) versionable).getName());
			identifier.setParentPath( "/" );
			identifier.setOwner(((Folder) versionable).getOwner());
		} else {
			//If this is going to be moved before the save we have to have a way to know the inode ahead of time
			String uri = versionable.getVersionType() + "." + versionable.getInode();
			if (versionable instanceof Contentlet) {
				Contentlet cont = (Contentlet) versionable;
				if (cont.isFileAsset()) {
					// special case when it is a file asset as contentlet
					uri = String.class.cast(cont.getMap().get(FileAssetAPI.FILE_NAME_FIELD));
					if (!UtilMethods.isSet(uri)) {
						try {
							// fallback
							uri = cont.getBinary(FileAssetAPI.BINARY_FIELD) != null ? cont
									.getBinary(FileAssetAPI.BINARY_FIELD).getName()
									: StringPool.BLANK;
						} catch (IOException e) {
							throw new DotDataException(e.getMessage(), e);
						}
					}
				} else if (cont.getStructure().getStructureType()
						== BaseContentType.HTMLPAGE.getType()) {
					uri = cont.getStringProperty(HTMLPageAssetAPI.URL_FIELD);
				}
				identifier.setAssetType(Identifier.ASSET_TYPE_CONTENTLET);
				identifier.setParentPath("/");
				identifier.setAssetName(uri);
				identifier.setAssetSubType(cont.getContentType().variable());
			} else if (versionable instanceof Link) {
				identifier.setAssetName(versionable.getInode());
				identifier.setParentPath("/");
			} else if (versionable instanceof Host) {
				identifier.setAssetName(versionable.getInode());
				identifier.setAssetType(Identifier.ASSET_TYPE_CONTENTLET);
				identifier.setParentPath("/");
				identifier.setAssetSubType(Host.HOST_VELOCITY_VAR_NAME);
			} else {
				identifier.setURI(uri);
			}

			identifier.setOwner((versionable instanceof WebAsset)
					? ((WebAsset) versionable).getOwner() : versionable.getModUser());
		}
        identifier.setHostId( site != null ? site.getIdentifier() : null );

        final Inode inode;
        try {
            if (versionable.getInode() != null) {
                inode = InodeUtils.getInode(versionable.getInode());
                identifier.setCreateDate(inode!=null?inode.getIDate():new Date());
            } else {
                identifier.setCreateDate(new Date());
            }
        } catch (DotSecurityException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        saveIdentifier( identifier );

        versionable.setVersionId( identifier.getId() );
        return identifier;
    }

	@Override
	protected List<Identifier> loadAllIdentifiers() throws DotDataException {

		DotConnect dc = new DotConnect();
		dc.setSQL("select * from identifier");

		return TransformerLocator.createIdentifierTransformer(dc.loadObjectResults()).asList();

	}

	@Override
	protected boolean isIdentifier(String identifierInode) {
        return new DotConnect()
                        .setSQL("select count(id) as test from identifier where id=?")
                        .addParam(identifierInode)
                        .getInt("test")>0;
	}

	@Override
	protected Identifier find(final String identifier) throws DotDataException {

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
    protected Identifier saveIdentifier(final Identifier id) throws DotDataException {
        if (id == null) {
            throw new DotRuntimeException("identifier to save cannot be null");
        }

        String query;
        if (UtilMethods.isSet(id.getId())) {
            if (isIdentifier(id.getId())) {
                query = "UPDATE identifier set parent_path=?, asset_name=?, host_inode=?, asset_type=?, syspublish_date=?, sysexpire_date=?, owner=?, create_date=?, asset_subtype=? where id=?";
            } else {
                query = "INSERT INTO identifier (parent_path,asset_name,host_inode,asset_type,syspublish_date,sysexpire_date,owner,create_date,asset_subtype,id) values (?,?,?,?,?,?,?,?,?,?)";
            }
        } else {
            id.setId(UUIDGenerator.generateUuid());
            query = "INSERT INTO identifier (parent_path,asset_name,host_inode,asset_type,syspublish_date,sysexpire_date,owner,create_date,asset_subtype,id) values (?,?,?,?,?,?,?,?,?,?)";
        }

        DotConnect dc = new DotConnect();
        dc.setSQL(query);

        dc.addParam(id.getParentPath());
        dc.addParam(id.getAssetName());
        dc.addParam(id.getHostId());
        dc.addParam(id.getAssetType());
        dc.addParam(id.getSysPublishDate());
        dc.addParam(id.getSysExpireDate());

        dc.addParam(id.getOwner());
        dc.addParam(id.getCreateDate());
        dc.addParam(id.getAssetSubType());
        dc.addParam(id.getId());

        
        dc.loadResult();


        ic.removeFromCacheByIdentifier(id.getId());
        ic.removeFromCacheByURI(id.getHostId(), id.getURI());
        return id;

    }

    @Override
    protected void deleteIdentifier(Identifier ident) throws DotDataException {
        DotConnect db = new DotConnect();
        try {
            db.setSQL("delete from template_containers where template_id = ?");
            db.addParam(ident.getId());
            db.loadResult();
            
            db.setSQL("delete from permission where inode_id = ?");
            db.addParam(ident.getId());
            db.loadResult();
            
            db.setSQL("delete from permission_reference where asset_id = ? or reference_id = ? ");
            db.addParam(ident.getId());
            db.addParam(ident.getId());
            db.loadResult();
            
            db.setSQL("delete from tree where child = ? or parent =?");
            db.addParam(ident.getId());
            db.addParam(ident.getId());
            db.loadResult();
            
            db.setSQL("delete from multi_tree where child = ? or parent1 =? or parent2 = ?");
            db.addParam(ident.getId());
            db.addParam(ident.getId());
            db.addParam(ident.getId());
            db.loadResult();

			//This is valid for folders as they aren't considered inodes anymore
			final String tableName = Try.of(
							() -> (Inode.Type.valueOf(ident.getAssetType().toUpperCase()).getTableName()))
					.getOrElse(ident.getAssetType());
            
            db.setSQL("select inode from " + tableName + " where inode=?");
            db.addParam(ident.getId());
            List<Map<String, Object>> deleteme = db.loadResults();

            final String versionInfoTable = Try.of(
					() -> (Inode.Type.valueOf(ident.getAssetType().toUpperCase()).getVersionTableName())).getOrNull();

            if (versionInfoTable != null) {
                db.setSQL("delete from " + versionInfoTable + " where identifier = ?");
                db.addParam(ident.getId());
                db.loadResult();
            }
            db.setSQL("select id from workflow_task where webasset = ?");
            db.addParam(ident.getId());
            List<Map<String, Object>> tasksToDelete = db.loadResults();
            for (Map<String, Object> task : tasksToDelete) {
                WorkflowTask wft = APILocator.getWorkflowAPI().findTaskById((String) task.get("id"));
                APILocator.getWorkflowAPI().deleteWorkflowTask(wft, APILocator.systemUser());
            }
            db.setSQL("delete from " + tableName + " where identifier = ?");
            db.addParam(ident.getId());
            db.loadResult();

            StringWriter sw = new StringWriter();
            sw.append(" ( 'IM_A_FAKE_INODE_TO_START_THE_LIST' ");
            for (Map<String, Object> m : deleteme) {
                sw.append(",'" + m.get("inode") + "' ");
            }
            sw.append("  ) ");
            db.setSQL("delete from inode where inode in " + sw.toString());
            db.loadResult();

            db.setSQL("delete from identifier where id=?");
            db.addParam(ident.getId());
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
		}
		return assetType;
	}

	@Override
	protected void updateUserReferences(final String userId, final String replacementUserId)throws DotDataException, DotSecurityException{
		DotConnect dc = new DotConnect();

		//Get all ids that will be updated, to remove it from cache
		dc.setSQL("select id from identifier where owner = ?");
		dc.addParam(userId);
		final List<HashMap<String, String>> identifiers = dc.loadResults();

		//Update owner
		dc.setSQL("update identifier set owner = ? where owner = ?");
		dc.addParam(replacementUserId);
		dc.addParam(userId);
		dc.loadResult();

		//Remove all identifier that were updated from cache
		for(final HashMap<String,String> id : identifiers){
			ic.removeFromCacheByIdentifier(id.get("id"));
		}
	}
}
