package com.dotcms.contenttype.business;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.contenttype.business.sql.ContentTypeSql;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.DbContentTypeTransformer;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;

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
	public List<ContentType> findAll(String orderBy) throws DotDataException {
		return dbAll(orderBy);
	}
	
	@Override
	public List<ContentType> search(String search, int baseType, String orderBy, int offset,int limit) throws DotDataException {
		return dbSearch(search,baseType, orderBy, offset, limit);
	}
	
	@Override
	public List<ContentType> search(String search, BaseContentTypes baseType, String orderBy, int offset,int limit) throws DotDataException {
		return dbSearch(search,baseType.getType(), orderBy, offset, limit);
	}
	@Override
	public List<ContentType> search(String search, String orderBy, int offset,int limit) throws DotDataException {
		return search(search, 0, orderBy, offset, limit);
	}
	@Override
	public List<ContentType> search(String search, String orderBy) throws DotDataException {
		return search(search,0, orderBy, 0,Config.getIntProperty("PER_PAGE",50));
	}
	
	@Override
	public List<ContentType> search(String search) throws DotDataException {
		return search(search,0, "mod_date desc", 0,Config.getIntProperty("PER_PAGE",50));
	}
	
	@Override
	public List<ContentType> search(String search, String orderBy, int limit) throws DotDataException {
		return search(search,0, orderBy, 0,limit);
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
		

		return LocalTransaction.wrapReturn(() ->{
			return dbSaveUpdate(type);
		}); 


	}
	
	
	private List<ContentType> dbByType(int type) throws DotDataException {
		DotConnect dc = new DotConnect();
		String sql = contentTypeSql.findAll;
		dc.setSQL(String.format(sql, "mod_date desc"));
		try {
			return DbContentTypeTransformer.transform(dc.loadObjectResults());
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}
	private List<ContentType> dbAll(String orderBy) throws DotDataException {
		DotConnect dc = new DotConnect();
		String sql = contentTypeSql.findAll;
		orderBy = SQLUtil.sanitizeSortBy(orderBy);
		dc.setSQL(String.format(sql, orderBy));

		List<Map<String, Object>> results;
		try {
			results = dc.loadObjectResults();
			return DbContentTypeTransformer.transform(results);
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	private ContentType dbById(String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.findById);
		dc.addParam(id);
		List<Map<String, Object>> results;
		try {
			results = dc.loadObjectResults();
			if (results.size() == 0) {
				throw new DotDataException("Content Type with id:" + id + " not found");
			}
			return DbContentTypeTransformer.transform(results.get(0));
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	private ContentType dbByVar(String var) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.findByVar);
		dc.addParam(var);
		List<Map<String, Object>> results;
		try {
			results = dc.loadObjectResults();
			if (results.size() == 0) {
				throw new DotDataException("Content Type with var:" + var + " not found");
			}
			return DbContentTypeTransformer.transform(results.get(0));
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	private ContentType dbSaveUpdate(final ContentType throwawayType) throws DotDataException{

		Date modDate = DateUtils.round(new Date(), Calendar.SECOND);
		ContentType retType = ContentTypeBuilder.builder(throwawayType).from(throwawayType).modDate(modDate).build();

		// assign an inode if needed
		if (retType.inode() == null) {
			retType = ContentTypeBuilder.builder(retType).from(retType).inode(UUID.randomUUID().toString()).build();
			insertInodeInDb(retType);
			insertInDb(retType);
			retType = DbContentTypeTransformer.transformToSubclass(retType);
			
			// set up default fields;
			List<Field> fields = retType.requiredFields();
			FieldApi fapi = new FieldApiImpl().instance();
			for(Field f : fields){
				f = FieldBuilder.builder(f).from(f).contentTypeId(retType.inode()).build();
				fapi.save(f);
			}
			return retType;
			
			
		}
		else{
			updateInodeInDb(retType);
			updateInDb(retType);
			return DbContentTypeTransformer.transformToSubclass(retType);
		}

		
		
		
		
		
	}
	
	private void updateInodeInDb(final ContentType type) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.updateContentTypeInode);
		dc.addParam(type.inode());
		dc.addParam(type.iDate());
		dc.addParam(type.owner());
		dc.addParam(type.inode());
		dc.loadResult();
	}

	private void insertInodeInDb(final ContentType type) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.insertContentTypeInode);
		dc.addParam(type.inode());
		dc.addParam(type.iDate());
		dc.addParam(type.owner());
		dc.loadResult();
	}

	private void updateInDb(ContentType type) throws DotDataException {
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

	private void insertInDb(ContentType type) throws DotDataException {
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

	private List<ContentType> dbSearch(String search, int baseType, String orderBy, int offset, int limit) throws DotDataException{
		int bottom = (baseType==0)?0:baseType;
		int top = (baseType==0)?baseType:100000;
		search = "%" + search + "%";
		DotConnect dc = new DotConnect();
		dc.setMaxRows(limit);
		dc.setStartRow(offset);
		dc.setSQL(String.format(contentTypeSql.SEARCH_ALL_STRUCTURE_FIELDS, SQLUtil.sanitizeSortBy(orderBy)));
		dc.addParam(search );
		dc.addParam(search);
		dc.addParam(search);
		dc.addParam(bottom);
		dc.addParam(top);
		return 	 DbContentTypeTransformer.transform(dc.loadObjectResults());
	}
	
	private int dbCount(String search,  int baseType) throws DotDataException{
		int bottom = (baseType==0)?0:baseType;
		int top = (baseType==0)?100000:baseType;
		
		search = search==null ? "%" : "%" + search + "%";

		DotConnect dc = new DotConnect();
		dc.setSQL(contentTypeSql.COUNT_SEARCH_ALL_STRUCTURE_FIELDS);
		dc.addParam(search );
		dc.addParam(search);
		dc.addParam(search);
		dc.addParam(bottom);
		dc.addParam(top);
		return dc.getInt("test");
	}
	/**
	 * Fields in the db 
	 * inode owner idate type inode name description
	 * default_structure page_detail structuretype system fixed velocity_var_name
	 * url_map_pattern host folder expire_date_var publish_date_var mod_date
	 */
}
