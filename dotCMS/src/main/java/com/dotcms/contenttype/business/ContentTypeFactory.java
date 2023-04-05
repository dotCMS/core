package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;

import java.util.Collection;
import java.util.List;

/**
 * Provides direct data source-level access to information related to Content Types in dotCMS. This Factory provides the
 * API with the appropriate data so users and services can interact with it.
 *
 * @author Will Ezell
 * @since Jun 29th, 2016
 */
public interface ContentTypeFactory {

	default ContentTypeFactory instance(){
		return new ContentTypeFactoryImpl();
	}

	ContentType find(String idOrVar) throws DotDataException;

	/**
	 * Returns a list of Content Types based on the specified list of Velocity Variable Names.
	 *
	 * @param varNames The list of Velocity Variable Names each corresponding to a Content Type.
	 * @param filter   Optional filtering parameter used to query for a specific Content Type name or Variable Name.
	 * @param offset   The specified offset in the result set, for pagination purposes.
	 * @param limit    The specified limit in the result set, for pagination purposes.
	 * @param orderBy  The order-by clause, which is internally sanitized by the API.
	 *
	 * @return The list of {@link ContentType} objects matching the specified variable names.
	 *
	 * @throws DotDataException An error occurred when interacting with the data source.
	 */
	List<ContentType> find(final Collection<String> varNames, final String filter, final int offset, final int limit,
						   final String orderBy) throws DotDataException;

	List<ContentType> findAll() throws DotDataException;

	List<ContentType> findAll(String orderBy) throws DotDataException;

	List<ContentType> findByBaseType(BaseContentType type) throws DotDataException;

	List<ContentType> findByBaseType(int type) throws DotDataException;

	ContentType save(ContentType type) throws DotDataException;

	List<ContentType> search(String search, String orderBy) throws DotDataException;

	List<ContentType> search(String search, String orderBy, int limit) throws DotDataException;
	
	List<ContentType> search(String search, String orderBy, int limit, int offset) throws DotDataException;

	List<ContentType> search(String search) throws DotDataException;

	int searchCount(String search) throws DotDataException;

	int searchCount(String search, int baseType) throws DotDataException;

	List<ContentType> search(String search, int baseType, String orderBy, int limit, int offset) throws DotDataException;
	
	List<ContentType> search(String search, BaseContentType type, String orderBy, int limit, int offset) throws DotDataException;

	List<ContentType> search(String search, int type, String orderBy, int limit, int offset,String hostId) throws DotDataException;

	int searchCount(String search, BaseContentType baseType) throws DotDataException;

	void delete(ContentType type) throws DotDataException;

	String suggestVelocityVar(String tryVar) throws DotDataException;

	ContentType findDefaultType() throws DotDataException;

	ContentType setAsDefault(ContentType type) throws DotDataException;

	List<ContentType> findUrlMapped() throws DotDataException;

	List<ContentType> search(String search, int limit) throws DotDataException;

  	void validateFields(ContentType type);
  
  	void updateModDate(ContentType type) throws DotDataException;

	/**
	 * This could be considered putting a shallow mark on the CT
	 * This way we can immediately exclude it from any process that might want to use it
	 * @param type
	 * @throws DotDataException
	 */
	void markForDeletion(ContentType type) throws DotDataException;

}
