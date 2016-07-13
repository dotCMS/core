package com.dotcms.contenttype.business;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.dotcms.contenttype.business.sql.FieldSql;
import com.dotcms.contenttype.exception.OverFieldLimitException;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.transform.field.DbFieldTransformer;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;

public class FieldFactoryImpl implements FieldFactory {

	final FieldSql sql;

	public FieldFactoryImpl() {
		sql = FieldSql.getInstance();
	}

	@Override
	public Field byId(String id) throws DotDataException {
		return findInDb(id);
	}

	@Override
	public List<Field> byContentTypeId(String id) throws DotDataException {
		return findByContentTypeInDb(id);
	}

	@Override
	public List<Field> byContentTypeVar(String var) throws DotDataException {
		return findByContentTypeVarInDb(var);
	}

	@Override
	public void delete(String id) throws DotDataException {
		LocalTransaction.wrap(() -> {
			return deleteInDb(id);
		});
	}
	
	@Override
	public Field save(final Field throwAwayField) throws DotDataException {
		return LocalTransaction.wrapReturn(() ->{
			return dbSaveUpdate(throwAwayField);
		});
	}
	
	
	private Field dbSaveUpdate(final Field throwAwayField) throws DotDataException {
		boolean inserting = false;
		Date modDate = DateUtils.round(new Date(), Calendar.SECOND);
		Field retField = FieldBuilder.builder(throwAwayField).modDate(modDate).build();
		// assign a db column if we need to
		if (retField.dbColumn() == null) {
			retField = FieldBuilder.builder(retField).dbColumn(assignAvailableColumn(retField)).build();
		}
		// assign an inode if needed
		if (retField.inode() == null) {
			retField = FieldBuilder.builder(retField).inode(UUID.randomUUID().toString()).build();
			inserting = true;
		}

		if(!retField.acceptedDataTypes().contains(retField.dataType())){
			throw new DotDataException("Field Type:" + retField.type() + " does not accept datatype " + retField.dataType());
		}
		if(retField.contentTypeId()==null){
			throw new DotDataException("Field Type:" + retField.type() + " does not have a contenttype.inode set");
		}
		
		// if we don't think we are inserting, test
		if (!inserting) {
			if (inodeCount(retField) == 0) {
				inserting = true;
			}
		}
		
		//make sure we are properly indexed
		if((retField.searchable() || retField.listed()) || retField instanceof HostFolderField){
			if(!retField.indexed()){
				retField = FieldBuilder.builder(retField).indexed(true).build();
			}
		}

		if (inserting) {
			insertInodeInDb(retField);
			insertFieldInDb(retField);
		} else {
			updateInodeInDb(retField);
			updateFieldInDb(retField);
		}

		return retField;
	}

	
	private List<Field> findByContentTypeInDb(String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.findByContentType);
		dc.addParam(id);
		List<Map<String, Object>> results;
		results = dc.loadObjectResults();
		return new DbFieldTransformer(results).asList();

	}

	private List<Field> findByContentTypeVarInDb(String var) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.findByContentTypeVar);
		dc.addParam(var);
		List<Map<String, Object>> results;
		results = dc.loadObjectResults();
		return new DbFieldTransformer(results).asList();

	}

	private Field findInDb(String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.findById);
		dc.addParam(id);
		List<Map<String, Object>> results;

		results = dc.loadObjectResults();
		if (results.size() == 0) {
			throw new DotDataException("Content Type with id:" + id + " not found");
		}
		return new DbFieldTransformer(results.get(0)).from();

	}

	private boolean deleteInDb(String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.deleteById);
		dc.addParam(id);
		dc.loadResult();
		dc.setSQL(sql.deleteInodeById);
		dc.addParam(id);
		dc.loadResult();
		return true;
	}

	private void updateInodeInDb(Field field) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.updateFieldInode);
		dc.addParam(field.inode());
		dc.addParam(field.iDate());
		dc.addParam(field.owner());
		dc.addParam(field.inode());
		dc.loadResult();
	}

	private void insertInodeInDb(Field field) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.insertFieldInode);
		dc.addParam(field.inode());
		dc.addParam(field.iDate());
		dc.addParam(field.owner());
		dc.loadResult();
	}

	private void updateFieldInDb(Field field) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.updateField);
		dc.addParam(field.contentTypeId());
		dc.addParam(field.name());
		dc.addParam(field.type().getCanonicalName());
		dc.addParam(field.relationType());
		dc.addParam(field.dbColumn());
		dc.addParam(field.required());
		dc.addParam(field.indexed());
		dc.addParam(field.listed());
		dc.addParam(field.variable());
		dc.addParam(field.sortOrder());
		dc.addParam(field.values());
		dc.addParam(field.regexCheck());
		dc.addParam(field.hint());
		dc.addParam(field.defaultValue());
		dc.addParam(field.fixed());
		dc.addParam(field.readOnly());
		dc.addParam(field.searchable());
		dc.addParam(field.unique());
		dc.addParam(field.modDate());
		dc.addParam(field.inode());
		dc.loadResult();

	}

	private void insertFieldInDb(Field field) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.insertField);
		dc.addParam(field.inode());
		dc.addParam(field.contentTypeId());
		dc.addParam(field.name());
		dc.addParam(field.type().getCanonicalName());
		dc.addParam(field.relationType());
		dc.addParam(field.dbColumn());
		dc.addParam(field.required());
		dc.addParam(field.indexed());
		dc.addParam(field.listed());
		dc.addParam(field.variable());
		dc.addParam(field.sortOrder());
		dc.addParam(field.values());
		dc.addParam(field.regexCheck());
		dc.addParam(field.hint());
		dc.addParam(field.defaultValue());
		dc.addParam(field.fixed());
		dc.addParam(field.readOnly());
		dc.addParam(field.searchable());
		dc.addParam(field.unique());
		dc.addParam(field.modDate());

		dc.loadResult();

	}

	private int inodeCount(Field field) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.inodeCount);
		dc.addParam(field.inode());
		return dc.getInt("inode_count");

	}

	private String assignAvailableColumn(Field field) throws DotDataException {
		

		
		DotConnect dc = new DotConnect();
		
		if(field instanceof HostFolderField || field instanceof TagField){
			dc.setSQL(this.sql.selectCountOfType);
			dc.addParam(field.contentTypeId());
			dc.addParam(LegacyFieldTypes.getLegacyName(field.type() + "%"));
			dc.addParam(LegacyFieldTypes.getImplClass(field.type() + "%"));
			int x =dc.getInt("test");
			if(x>0){
				throw new OverFieldLimitException("Only one " + field.type() + " per ContentType");
			}
		}
		
		if(field.dataType() == DataTypes.CONSTANT 
				|| field.dataType() == DataTypes.SECTION_DIVIDER  
				|| field.dataType() == DataTypes.SYSTEM ){
			return field.dataType().toString();
		}

		
		String dataType = field.dataType().toString();
		dc.setSQL(this.sql.selectFieldOfDbType);
		dc.addParam(field.contentTypeId());
		dc.addParam(dataType + "%");
		List<Map<String, Object>> rows = dc.loadObjectResults();
		Set<String> columns = new TreeSet<String>();
		for (int i = 0; i < rows.size(); i++) {
			columns.add((String) rows.get(i).get("field_contentlet"));
		}

		for (int i = 0; i < Config.getIntProperty("db.number.of.contentlet.columns.per.datatype", 25); i++) {
			if (!columns.contains(dataType + (i + 1))) {
				return dataType + (i + 1);

			}
		}
		
		throw new OverFieldLimitException("No more columns for datatype:" + dataType);
		

	}

}
