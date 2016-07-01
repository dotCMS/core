package com.dotcms.contenttype.business;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.dotcms.contenttype.business.sql.FieldSql;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableField;
import com.dotcms.contenttype.transform.DbFieldTransformer;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;

public class FieldFactoryImpl implements FieldFactory {

	final FieldSql sql ;


	public FieldFactoryImpl() {
		sql = FieldSql.instance;
	}


	@Override
	public Field byId(String id) throws DotDataException {
		return findInDb(id);
	}

	
	@Override
	public  List<Field> byContentTypeId(String id) throws DotDataException {
		return findByContentTypeInDb(id);
	}
	
	@Override
	public  List<Field> byContentTypeVar(String var) throws DotDataException {
		return findByContentTypeVarInDb(var);
	}
	
	@Override
	public  void delete(String id) throws DotDataException {
		try{
			 LocalTransaction.closeIfNeeded(() ->{
				 return deleteInDb(id);
			});
		}
		catch(Exception e){
			throw new DotDataException(e.getMessage(),e);
		}
	}
	
	@Override
	public Field save(Field field) throws DotDataException {
		boolean inserting=false;
		if(field.inode()==null){
			field =  ImmutableField.copyOf(field).withInode(UUID.randomUUID().toString());
			inserting=true;
		}
		
		if(!inserting){
			if(inodeCount(field)==0){
				inserting=true;
			}
		}
		//assign a column if we need to
		if(field.dbColumn() ==null){
			field = assignAvailableColumn(field);
		}
		if(inserting){
			insertInodeInDb(field);
			insertFieldInDb(field);
		}
		else{
			updateInodeInDb(field);
			updateFieldInDb(field);
		}


		return field;
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
		dc.addParam(field.type());
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
		dc.addParam(new Date());
		dc.addParam(field.inode());
		dc.addParam(field.inode());
		dc.loadResult();


	}
	
	private void insertFieldInDb(Field field) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.insertField);
		dc.addParam(field.inode());
		dc.addParam(field.contentTypeId());
		dc.addParam(field.name());
		dc.addParam(field.type());
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
		dc.addParam(new Date());
		
		dc.loadResult();
		
		

	}
	
	private int inodeCount(Field field) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(sql.inodeCount);
		dc.addParam(field.inode());
		return dc.getInt("inode_count");


	}
	

	private Field assignAvailableColumn(Field field) throws DotDataException {
		DotConnect dc = new DotConnect();
		String dataType = field.dataType().toString();
		dc.setSQL(sql.selectFieldOfDbType);
		dc.addParam(field.contentTypeId());
		dc.addParam(dataType + "%");
		List<Map<String, Object>> rows = dc.loadObjectResults();
		Set<String> columns = new TreeSet<String>();
		for(int i=0;i< rows.size();i++){
			columns.add((String) rows.get(i).get("field_contentlet"));
		}
		String column =null;
		for(int i=0;i< Config.getIntProperty("db.number.of.contentlet.columns.per.datatype", 25);i++){
			if(!columns.contains(dataType+(i+1))){
				column= dataType+(i+1);
				break;
			}
		}
		if(column==null){
			throw new DotDataException("No more columns for datatype:" +dataType );
		}
		return ImmutableField.copyOf(field).withDbColumn(column);
		
		




	}
	
}
