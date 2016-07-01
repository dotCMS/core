package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;

public interface ContentTypeFactory {
	
	
	default ContentTypeFactory instance(){
		return new ContentTypeFactoryImpl();
	}
	
	
	ContentType find(String id) throws DotDataException;
	
	ContentType findByVar(String id) throws DotDataException;

	List<ContentType> findAll() throws DotDataException;

	List<ContentType> findAll(String orderBy) throws DotDataException;

	List<ContentType> findByBaseType(BaseContentTypes type) throws DotDataException;

	List<ContentType> findByBaseType(int type) throws DotDataException;
	
}
