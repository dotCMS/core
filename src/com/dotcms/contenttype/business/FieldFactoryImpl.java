package com.dotcms.contenttype.business;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.dotcms.contenttype.business.sql.FieldSql;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.transform.DbFieldTransformer;
import com.dotcms.contenttype.util.FieldBuilderUtil;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;

public class FieldFactoryImpl implements FieldFactory {

	final FieldSql sql;

	public FieldFactoryImpl() {
		sql = FieldSql.instance;
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
		try {
			LocalTransaction.wrap(() -> {
				return deleteInDb(id);
			});
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@Override
	public Field save(final Field throwAwayField) throws DotDataException {
		boolean inserting = false;
		Date modDate = DateUtils.round(new Date(), Calendar.SECOND);
		Field retField = FieldBuilderUtil.resolveBuilder(throwAwayField).from(throwAwayField).modDate(modDate).build();
		// assign a db column if we need to
		if (retField.dbColumn() == null) {
			retField = FieldBuilderUtil.resolveBuilder(retField).from(retField).dbColumn(assignAvailableColumn(retField)).build();
		}
		// assign an inode if needed
		if (retField.inode() == null) {
			retField = FieldBuilderUtil.resolveBuilder(retField).from(retField).inode(UUID.randomUUID().toString()).build();
			inserting = true;
		}

		if(!retField.acceptedDataTypes().contains(retField.dataType())){
			throw new DotDataException("Field Type:" + retField.type() + " does not accept datatype " + retField.dataType());
		}
		
		// if we don't think we are inserting, test
		if (!inserting) {
			if (inodeCount(retField) == 0) {
				inserting = true;
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
		try {
			results = dc.loadObjectResults();

			return DbFieldTransformer.transform(results);
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	private List<Field> findByContentTypeVarInDb(String var) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.findByContentTypeVar);
		dc.addParam(var);
		List<Map<String, Object>> results;
		try {
			results = dc.loadObjectResults();

			return DbFieldTransformer.transform(results);
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	private Field findInDb(String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.findById);
		dc.addParam(id);
		List<Map<String, Object>> results;
		try {
			results = dc.loadObjectResults();
			if (results.size() == 0) {
				throw new DotDataException("Content Type with id:" + id + " not found");
			}
			return DbFieldTransformer.transform(results.get(0));
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
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
		if(field instanceof ImmutableConstantField){
			return "constant";
		}
		DotConnect dc = new DotConnect();
		String dataType = field.dataType().toString();
		dc.setSQL(sql.selectFieldOfDbType);
		dc.addParam(field.contentTypeId());
		dc.addParam(dataType + "%");
		List<Map<String, Object>> rows = dc.loadObjectResults();
		Set<String> columns = new TreeSet<String>();
		for (int i = 0; i < rows.size(); i++) {
			columns.add((String) rows.get(i).get("field_contentlet"));
		}
		String column = null;
		for (int i = 0; i < Config.getIntProperty("db.number.of.contentlet.columns.per.datatype", 25); i++) {
			if (!columns.contains(dataType + (i + 1))) {
				column = dataType + (i + 1);
				break;
			}
		}
		if (column == null) {
			throw new DotDataException("No more columns for datatype:" + dataType);
		}
		return column;

	}

}
