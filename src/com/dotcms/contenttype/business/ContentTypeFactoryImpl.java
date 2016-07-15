package com.dotcms.contenttype.business;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.UUID;

import com.dotcms.contenttype.business.sql.ContentTypeSql;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.model.type.FormContentType;
import com.dotcms.contenttype.transform.contenttype.DbContentTypeTransformer;
import com.dotcms.contenttype.transform.contenttype.ImplClassContentTypeTransformer;
import com.dotcms.contenttype.transform.contenttype.ToStructureTransformer;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;

public class ContentTypeFactoryImpl implements ContentTypeFactory {

	final ContentTypeSql contentTypeSql;

	public ContentTypeFactoryImpl() {
		contentTypeSql = ContentTypeSql.getInstance();
	}

	@Override
	public ContentType find(String id) throws DotDataException {
		return dbById(id);
	}

	@Override
	public ContentType findByVar(final String var) throws DotDataException {
		return dbByVar(var);
	}

	@Override
	public List<ContentType> findAll() throws DotDataException {
		return dbAll("mod_date desc");
	}

	@Override
	public void delete(ContentType type) throws DotDataException {
		LocalTransaction.wrapReturn(() -> {
			return dbDelete(type);
		});
	}
	
	@Override
	public void delete(ContentType type, List<Observer> observers) throws DotDataException {
		 LocalTransaction.wrapReturn(() -> {
			return dbDelete(type);
		});
	}
	@Override
	public List<ContentType> findAll(String orderBy) throws DotDataException {
		return dbAll(orderBy);
	}

	@Override
	public List<ContentType> search(String search, int baseType, String orderBy, int offset, int limit) throws DotDataException {
		return dbSearch(search, baseType, orderBy, offset, limit);
	}

	@Override
	public List<ContentType> search(String search, BaseContentTypes baseType, String orderBy, int offset, int limit)
			throws DotDataException {
		return dbSearch(search, baseType.getType(), orderBy, offset, limit);
	}

	@Override
	public List<ContentType> search(String search, String orderBy, int offset, int limit) throws DotDataException {
		return search(search, 0, orderBy, offset, limit);
	}

	@Override
	public List<ContentType> search(String search, String orderBy) throws DotDataException {
		return search(search, 0, orderBy, 0, Config.getIntProperty("PER_PAGE", 50));
	}

	@Override
	public List<ContentType> search(String search) throws DotDataException {
		return search(search, 0, "mod_date desc", 0, Config.getIntProperty("PER_PAGE", 50));
	}

	@Override
	public List<ContentType> search(String search, String orderBy, int limit) throws DotDataException {
		return search(search, 0, orderBy, 0, limit);
	}

	@Override
	public int searchCount(String search) throws DotDataException {
		return dbCount(search, 0);
	}

	@Override
	public int searchCount(String search, int baseType) throws DotDataException {
		return dbCount(search, baseType);
	}

	@Override
	public int searchCount(String search, BaseContentTypes baseType) throws DotDataException {
		return dbCount(search, baseType.getType());
	}

	@Override
	public List<ContentType> findByBaseType(BaseContentTypes type) throws DotDataException {
		return dbByType(type.getType());
	}

	@Override
	public List<ContentType> findByBaseType(int type) throws DotDataException {
		return dbByType(type);
	}

	@Override
	public ContentType save(ContentType type) throws DotDataException {
		return LocalTransaction.wrapReturn(() -> {
			return dbSaveUpdate(type);
		});
	}

	private List<ContentType> dbByType(int type) throws DotDataException {
		DotConnect dc = new DotConnect();
		String sql = contentTypeSql.findAll;
		dc.setSQL(String.format(sql, "mod_date desc"));

		return new DbContentTypeTransformer(dc.loadObjectResults()).asList();

	}

	private List<ContentType> dbAll(String orderBy) throws DotDataException {
		DotConnect dc = new DotConnect();
		String sql = contentTypeSql.findAll;
		orderBy = SQLUtil.sanitizeSortBy(orderBy);
		dc.setSQL(String.format(sql, orderBy));

		return new DbContentTypeTransformer(dc.loadObjectResults()).asList();

	}

	private ContentType dbById(String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.findById);
		dc.addParam(id);
		List<Map<String, Object>> results;

		results = dc.loadObjectResults();
		if (results.size() == 0) {
			throw new NotFoundInDbException("Content Type with id:" + id + " not found");
		}
		return new DbContentTypeTransformer(results.get(0)).from();

	}

	private ContentType dbByVar(String var) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.findByVar);
		dc.addParam(var);
		List<Map<String, Object>> results;

		results = dc.loadObjectResults();
		if (results.size() == 0) {
			throw new NotFoundInDbException("Content Type with var:" + var + " not found");
		}
		return new DbContentTypeTransformer(results.get(0)).from();

	}

	private ContentType dbSaveUpdate(final ContentType throwawayType) throws DotDataException {

		Date modDate = DateUtils.round(new Date(), Calendar.SECOND);
		ContentType retType = ContentTypeBuilder.builder(throwawayType).from(throwawayType).modDate(modDate).build();

		// assign an inode if needed
		if (retType.inode() == null) {
			retType = ContentTypeBuilder.builder(retType).from(retType).inode(UUID.randomUUID().toString()).build();
			dbInodeInsert(retType);
			dbInsert(retType);
			retType = new ImplClassContentTypeTransformer(retType).from();

			// set up default fields;
			List<Field> fields = retType.requiredFields();
			FieldApi fapi = new FieldApiImpl().instance();
			for (Field f : fields) {
				f = FieldBuilder.builder(f).contentTypeId(retType.inode()).build();
				fapi.save(f);
			}
			return retType;

		} else {
			dbInodeUpdate(retType);
			dbUpdate(retType);
			return new ImplClassContentTypeTransformer(retType).from();
		}
	}

	private void dbInodeUpdate(final ContentType type) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.updateContentTypeInode);
		dc.addParam(type.inode());
		dc.addParam(type.iDate());
		dc.addParam(type.owner());
		dc.addParam(type.inode());
		dc.loadResult();
	}

	private void dbInodeInsert(final ContentType type) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.insertContentTypeInode);
		dc.addParam(type.inode());
		dc.addParam(type.iDate());
		dc.addParam(type.owner());
		dc.loadResult();
	}

	private void dbUpdate(ContentType type) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.updateContentType);
		dc.addParam(type.name());
		dc.addParam(type.description());
		dc.addParam(type.defaultStructure());
		dc.addParam(type.pagedetail());
		dc.addParam(type.baseType());
		dc.addParam(type.system());
		dc.addParam(type.fixed());
		dc.addParam(type.velocityVarName());
		dc.addParam(type.urlMapPattern());
		dc.addParam(type.host());
		dc.addParam(type.folder());
		dc.addParam(type.expireDateVar());
		dc.addParam(type.publishDateVar());
		dc.addParam(type.modDate());
		dc.addParam(type.inode());
		dc.loadResult();
	}

	private void dbInsert(ContentType type) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.insertContentType);
		dc.addParam(type.inode());
		dc.addParam(type.name());
		dc.addParam(type.description());
		dc.addParam(type.defaultStructure());
		dc.addParam(type.pagedetail());
		dc.addParam(type.baseType().getType());
		dc.addParam(type.system());
		dc.addParam(type.fixed());
		dc.addParam(type.velocityVarName());
		dc.addParam(type.urlMapPattern());
		dc.addParam(type.host());
		dc.addParam(type.folder());
		dc.addParam(type.expireDateVar());
		dc.addParam(type.publishDateVar());
		dc.addParam(type.modDate());
		dc.loadResult();
	}

	private boolean dbDelete(ContentType type) throws DotDataException {

		// default structure can't be deleted
		if (type.defaultStructure()) {
			throw new DotDataException("Can't delete default structure");
		}

		// deleting fields
		APILocator.getFieldAPI2().deleteFieldsByContentType(type);

		// delete Forms entry if it is a form structure
		if (type instanceof FormContentType) {
			deleteFormEntries((FormContentType) type);
		}

		// make sure folders don't refer to this structure as default fileasset
		// structure
		if (type instanceof FileAssetContentType) {
			updateFolderFileAssetReferences((FileAssetContentType) type);
		}

		
		deleteContentByType(type);
		// remove structure permissions
		APILocator.getPermissionAPI().removePermissions(type);

		// remove structure itself
		FactoryLocator.getContentTypeFactory2().delete(type);
		return true;
	}

	private List<ContentType> dbSearch(String search, int baseType, String orderBy, int offset, int limit) throws DotDataException {
		int bottom = (baseType == 0) ? 0 : baseType;
		int top = (baseType == 0) ? baseType : 100000;
		search = "%" + search + "%";
		DotConnect dc = new DotConnect();
		dc.setMaxRows(limit);
		dc.setStartRow(offset);
		dc.setSQL(String.format(contentTypeSql.SEARCH_ALL_STRUCTURE_FIELDS, SQLUtil.sanitizeSortBy(orderBy)));
		dc.addParam(search);
		dc.addParam(search);
		dc.addParam(search);
		dc.addParam(bottom);
		dc.addParam(top);
		return new ImplClassContentTypeTransformer(dc.loadResults()).asList();
	}

	private int dbCount(String search, int baseType) throws DotDataException {
		int bottom = (baseType == 0) ? 0 : baseType;
		int top = (baseType == 0) ? 100000 : baseType;

		search = search == null ? "%" : "%" + search + "%";

		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.COUNT_SEARCH_ALL_STRUCTURE_FIELDS);
		dc.addParam(search);
		dc.addParam(search);
		dc.addParam(search);
		dc.addParam(bottom);
		dc.addParam(top);
		return dc.getInt("test");
	}

	private void updateFolderFileAssetReferences(FileAssetContentType type) throws DotDataException {
		ContentType defaultFileAssetStructure = findByVar(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
		DotConnect dc = new DotConnect();
		dc.setSQL("update folder set default_file_type = ? where default_file_type = ?");
		dc.addParam(defaultFileAssetStructure.inode());
		dc.addParam(type.inode());
		dc.loadResult();
	}

	private void deleteFormEntries(FormContentType form) throws DotDataException {
		ContentType forms = findByVar(FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME);

		try {
			APILocator.getContentletAPI().delete(
					APILocator.getContentletAPI().search("+structureInode:" + form.inode() + " +structureInode:" + form.inode(), 0, 0, "",
							APILocator.systemUser(), false), APILocator.systemUser(), false);
		} catch (Exception e) {
			throw new DotDataException("cannot delete form entries", e);
		} 

	}
	
	
	private void deleteContentByType(ContentType type) throws DotDataException{
 	    // permissions have already been checked at this point
 	    int limit = 200;
 	    int offset = 0;
 	    ContentletAPI conAPI=APILocator.getContentletAPI();
 	    List<Contentlet> contentlets=null;
 	    do {
 	        try {
				contentlets = conAPI.search("+contenttype:" + type.inode(), limit, offset, "mod_date", APILocator.systemUser(), false);
	 	        for(Contentlet contentlet : contentlets){
	 	        	conAPI.destroy(contentlets, APILocator.systemUser(), false);
	 	        }
			} catch (DotDataException | DotSecurityException e) {
				throw new DotDataException("cannot delete deleteContentByType", e);
			}
 	    } while(contentlets.size()>0);
	}
	/**
	 * Fields in the db inode owner idate type inode name description
	 * default_structure page_detail structuretype system fixed
	 * velocity_var_name url_map_pattern host folder expire_date_var
	 * publish_date_var mod_date
	 */
}
