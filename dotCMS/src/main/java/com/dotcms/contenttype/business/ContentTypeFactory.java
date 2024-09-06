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

	String INODE_COLUMN = "inode";
	String NAME_COLUMN = "name";
	String DESCRIPTION_COLUMN = "description";
	String DEFAULT_STRUCTURE_COLUMN = "default_structure";
	String REVIEW_INTERVAL_COLUMN = "review_interval";
	String REVIEWER_ROLE_COLUMN = "reviewer_role";
	String PAGE_DETAIL_COLUMN = "page_detail";
	String STRUCTURE_TYPE_COLUMN = "structuretype";
	String SYSTEM_COLUMN = "system";
	String FIXED_COLUMN = "fixed";
	String VELOCITY_VAR_NAME_COLUMN = "velocity_var_name";
	String URL_MAP_PATTERN_COLUMN = "url_map_pattern";
	String HOST_COLUMN = "host";
	String FOLDER_COLUMN = "folder";
	String EXPIRE_DATE_VAR_COLUMN = "expire_date_var";
	String PUBLISH_DATE_VAR_COLUMN = "publish_date_var";
	String MOD_DATE_COLUMN = "mod_date";
	String SORT_ORDER_COLUMN = "sort_order";
	String ICON_COLUMN = "icon";
	String MARKED_FOR_DELETION_COLUMN = "marked_for_deletion";
	String METADATA_COLUMN = "metadata";

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

	/**
	 * Returns a list of Content Types based on the specified list of search criteria that live in
	 * the specified Site.

	 * @param search  Allows you to add more conditions to the query via SQL code. It's internally
	 *                sanitized by the API.
	 * @param type    The Base Content Type to search for.
	 * @param orderBy The order-by clause, which is internally sanitized by the API.
	 * @param limit   The maximum number of returned items in the result set, for pagination
	 *                purposes.
	 * @param offset  The page number of the result set, for pagination purposes.
	 * @param hostId  The ID of the Site that the Content Types live in.
	 *
	 * @return The list of {@link ContentType} objects matching the specified search criteria.
	 *
	 * @throws DotDataException An error occurred when retrieving information from the database.
	 */
	List<ContentType> search(String search, int type, String orderBy, int limit, int offset,String hostId) throws DotDataException;

	/**
	 * Returns a list of Content Types based on the specified list of search criteria. In
	 * particular, this method allows you to search for Content Types in a specific list of Sites.
	 *
	 * @param sites   The list of one or more Site IDs to search for Content Types.
	 * @param search  Allows you to add more conditions to the query via SQL code. It's internally
	 *                sanitized by this Factory.
	 * @param type    The Base Content Type to search for.
	 * @param orderBy The order-by clause, which is internally sanitized by this Factory.
	 * @param limit   The maximum number of returned items in the result set, for pagination
	 *                purposes.
	 * @param offset  The requested page number of the result set, for pagination purposes.
	 *
	 * @return The list of {@link ContentType} objects matching the specified search criteria.
	 *
	 * @throws DotDataException An error occurred when retrieving information from the database.
	 */
	List<ContentType> search(final List<String> sites, final String search, final int type, final String orderBy, final int limit, final int offset) throws DotDataException;

	int searchCount(String search, BaseContentType baseType) throws DotDataException;

	void delete(ContentType type) throws DotDataException;

	String suggestVelocityVar(String tryVar) throws DotDataException;

	ContentType findDefaultType() throws DotDataException;

	ContentType setAsDefault(ContentType type) throws DotDataException;

	List<ContentType> findUrlMapped() throws DotDataException;

	/**
	 * Returns a list of {@link ContentType#urlMapPattern()} for a specified
	 * {@link com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset}'s id.
	 *
	 * @param pageIdentifier The {@link com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset}'s id to search for.
	 *
	 * @return The list of {@link ContentType#urlMapPattern()}  that are link to the specified
	 * {@link com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset}'s id.
	 *
	 * @throws DotDataException An error occurred when interacting with the data source.
	 */
	List<String> findUrlMappedPattern(final String pageIdentifier) throws DotDataException;

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

	/**
	 * Return the count of {@link ContentType} assigned to not SYSTEM_WORFLOW
	 * @return
	 */
	long countContentTypeAssignedToNotSystemWorkflow() throws DotDataException;

}
