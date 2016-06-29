package com.dotcms.contenttype.business;

import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.business.sql.FieldSql;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.DbFieldTransformer;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

public class FieldFactoryImpl implements FieldFactory {

	final FieldSql sql ;


	public FieldFactoryImpl() {
		sql = FieldSql.instance;
	}


	@Override
	public Field find(String id) throws DotDataException {
		return findInDb(id);
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

	@Override
	public ContentType findByVar(String id) {
		return null;
	}

}
