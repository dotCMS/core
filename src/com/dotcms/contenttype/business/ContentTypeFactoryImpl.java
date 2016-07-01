package com.dotcms.contenttype.business;

import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.business.sql.ContentTypeSql;
import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.DbContentTypeTransformer;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;

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
	public List<ContentType> findByBaseType(BaseContentTypes type) throws DotDataException {
		return dbByType(type.getType());
	}
	
	@Override
	public List<ContentType> findByBaseType(int type) throws DotDataException {
		return dbByType(type);
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

}
