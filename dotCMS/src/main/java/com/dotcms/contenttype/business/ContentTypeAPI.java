package com.dotcms.contenttype.business;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * Through this API you will be able to access, delete, create and modify the {@link ContentType}, there are some
 * Content Types that are already set by dotcms:
 * <p><ul>
 * <li> Host
 * <li> Folder
 * <li> File
 * <li> Forms
 * <li> HTMLPage
 * <li> Menu Link
 * <li> Container
 * <li> Template
 * <li> User
 * <li> Calendar Event
 * </ul><p>
 * 
 * 
 * @author Will Ezell
 *
 */
public interface ContentTypeAPI {


  /**
   * Name of Structures already set by dotcms
   */
  final Set<String> reservedStructureNames = ImmutableSet.of("host", "folder", "file", "forms", "html page",
      "menu link", "container", "template", "user");

  /**
   * Variable Name of the Structures already set by dotcms
   */
  final Set<String> reservedStructureVars = ImmutableSet.of("host", "folder", "file", "forms", "htmlpage", "menulink",
		  "container", "template", "user", "calendarEvent");

  /**
   * Deletes the specified Content Type. By default, the process that actually deletes it is
   * asynchronous, meaning that a separate thread/transaction takes care of it. You can change this
   * behavior by updating the value of the {@link ContentTypeAPIImpl#DELETE_CONTENT_TYPE_ASYNC}
   * property to {@code true}.
   *
   * @param contentType The {@link ContentType} being deleted.
   *
   * @throws DotSecurityException The User accessing this API does not have the required
   *                              permissions to perform this action.
   * @throws DotDataException     An error occurred when interacting with the database.
   */
  void delete(final ContentType contentType) throws DotSecurityException, DotDataException;

  /**
   * Deletes the specified Content Type. By default, the process that actually deletes it is
   * asynchronous, meaning that a separate thread/transaction takes care of it. With this
   * implementation, you can force dotCMS to <b>wait for the deletion process to be over before
   * moving on</b>. Therefore, given the implications related to performance, this method must be
   * used carefully
   *
   * @param contentType The {@link ContentType} being deleted.
   *
   * @throws DotSecurityException The User accessing this API does not have the required
   *                              permissions to perform this action.
   * @throws DotDataException     An error occurred when interacting with the database.
   */
  void deleteSync(final ContentType contentType) throws DotSecurityException, DotDataException;

  /**
   * Returns the Content Type that matches the specified Inode or Velocity Variable Name.
   *
   * @param inodeOrVar Either the Inode or the Velocity var name representing the Content Type to
   *                   find.
   *
   * @return The {@link ContentType} that was requested.
   *
   * @throws DotSecurityException  The user does not have permissions to perform this action.
   * @throws DotDataException      Error occurred when performing the action.
   * @throws NotFoundInDbException The Content Type was not found in the database.
   */
  ContentType find(final String inodeOrVar) throws DotSecurityException, DotDataException;

  /**
   * Returns a list of Content Types based on the specified list of Velocity Variable Names. If
   * one or more Velocity Variable Names don't exist in the content repository, or if current User
   * doesn't have access to them, they will not be added to the result list.
   *
   * @param varNames The list of Velocity Variable Names each corresponding to a Content Type.
   * @param filter   Optional filtering parameter used to query for a specific Content Type name
   *                 or Variable Name.
   * @param offset   The specified offset in the result set, for pagination purposes.
   * @param limit    The specified limit in the result set, for pagination purposes.
   * @param orderBy  The order-by clause, which is internally sanitized by the API. For more
   *                 information, please refer to
   *                 {@link com.dotmarketing.common.util.SQLUtil#ORDERBY_WHITELIST}
   * @return The list of {@link ContentType} objects matching the specified variable names.
   * @throws DotSecurityException The User accessing this API does not have the required
   *                              permissions to perform this action.
   * @throws DotDataException     An error occurred when interacting with the data source.
   */
  List<ContentType> find(final List<String> varNames, final String filter, final int offset, final int limit,
                                   final String orderBy) throws DotSecurityException, DotDataException;

  /**
   * Finds All the Content Types that exists in the system
   * 
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> findAll() throws DotDataException;

  /**
   * Finds All the Content Types that exists in the system but takes into consideration the {@link LicenseLevel}.
   * If the {@link LicenseLevel} is Community, only the content types that are not an {@link com.dotcms.contenttype.model.type.EnterpriseType} will be returned
   *
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> findAllRespectingLicense() throws DotDataException;

  /**
   * Returns the default structure (Content Generic is the one shipped by dotcms)
   * 
   * @return Content Type Object
   * @throws DotDataException Error occurred when performing the action.
   * @throws DotSecurityException The user does not have permissions to perform this action.
   */
  ContentType findDefault() throws DotDataException, DotSecurityException;

  /**
   * Finds all the Content Types in the system filtered by the {@link BaseContentType}, orders it according the given column.
   * 
   * @param type Base Content Type that will be searched
   * @param orderBy Specifies an order criteria for the results
   * @param limit Amount of results
   * @param offset Start position of the resulting list
   * @return List of Content Types Objects that belong to the Base Content Type specified.
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> findByBaseType(BaseContentType type, String orderBy, int limit, int offset) throws DotDataException;

  /**
   * Finds all the Content Types in the system filtered by the {@link BaseContentType}.
   * 
   * @param type Base Content Type that will be searched
   * @return List of Content Types Objects that belong to the Base Content Type specified.
   * @throws DotDataException Error occurred when performing the action.
   * @throws DotSecurityException The user does not have permissions to perform this action.
   */
  List<ContentType> findByType(BaseContentType type) throws DotDataException, DotSecurityException;

  /**
   * Finds all the Content Types in the system, orders it according the given column.
   *
   * @param orderBy Specifies an order criteria for the results
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> findAll(String orderBy) throws DotDataException;

  /**
   * Retrieves All the Content Types that have set an URL Map.
   * 
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
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

  /**
   * Returns a list of Content Types whose URL map pattern matches the given URL string using regex comparison.
   * This method converts URL map patterns (e.g., "/news/{urlTitle}") to regex patterns and tests them against the provided URL.
   *
   * @param urlMap The URL string to match against content type URL map patterns.
   *
   * @return The list of {@link ContentType} objects whose URL map patterns match the given URL.
   *
   * @throws DotDataException An error occurred when interacting with the data source.
   */
  List<ContentType> findByUrlMapPattern(final String urlMap) throws DotDataException;

  /**
   * Counts the amount of Content Types in the DB filtered by the given condition.
   * 
   * @return Amount of Content Types
   * @throws DotDataException Error occurred when performing the action.
   */
  int count(String condition) throws DotDataException;

  /**
   * Counts the amount of Content Types in the DB filtered by the given condition and the BaseContentType.
   * 
   * @param condition Condition that the Content Type needs to meet
   * @param base Base Content Type that wants to be searched
   * @param hostId hostId where the content type lives, pass null to bring from all sites.
   * @return Amount of Content Types
   * @throws DotDataException Error occurred when performing the action.
   */
  int count(String condition, BaseContentType base, String hostId) throws DotDataException;

  /**
   * Counts the amount of Content Types in the DB filtered by the given condition, the Base
   * Content Type, and the specific Sites they live in.
   *
   * @param condition Condition that the Content Type needs to meet.
   * @param base      The {@link BaseContentType} that must be searched for. If you need to get all
   *                  types, use {@link BaseContentType#ANY}.
   * @param siteIds   The list of Site IDs or Site Keys -- aka, site names -- where the Content
   *                  Types live in.
   *
   * @return The total number of Content Types that meet the search criteria and live in the
   * specified Sites.
   *
   * @throws DotDataException An error occurred when retrieving information from the database.
   */
  int countForSites(final String condition, final BaseContentType base, final List<String> siteIds) throws DotDataException;

  /**
   * Counts the amount of Content Types in the DB filtered by the given condition and the BaseContentType.
   *
   * @param condition Condition that the Content Type needs to meet
   * @param base Base Content Type that wants to be searched
   * @return Amount of Content Types
   * @throws DotDataException Error occurred when performing the action.
   */
  int count(String condition, BaseContentType base) throws DotDataException;

  /**
   * Return the total count of content types stored in the system.
   * 
   * @return Total Count of Content Types
   * @throws DotDataException Error occurred when performing the action.
   */
  int count() throws DotDataException;

  /**
   * Returns a suggestion for the Velocity Variable Name.
   * 
   * @param tryVar Velocity Variable Name
   * @return Suggestion for the Velocity Variable Name
   * @throws DotDataException Error occurred when performing the action.
   */
  String suggestVelocityVar(String tryVar) throws DotDataException;

  /**
   * Set as Default Content Type the given Content Type
   * 
   * @param type Content Type that is going the be the default one.
   * @return The same Content Type Object given in the parameter
   * @throws DotDataException Error occurred when performing the action.
   * @throws DotSecurityException The user does not have permissions to perform this action.
   */
  ContentType setAsDefault(ContentType type) throws DotDataException, DotSecurityException;

  /**
   * Retrieves a list of Structures with their respective URL Map. If the Structure does not have a URL Map is not added.
   * Check the existence of the URL Map by the method {@link #findUrlMapped()}
   * 
   * @return List of {@link SimpleStructureURLMap} Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<SimpleStructureURLMap> findStructureURLMapPatterns() throws DotDataException;

  /**
   * Moves all the Content Types that lives in a specific folder to the System Folder.
   * 
   * @param folder Folder where the Content Types currently lives.
   * @throws DotDataException Error occurred when performing the action.
   */
  void moveToSystemFolder(Folder folder) throws DotDataException;

  /**
   * Saves a new content type based on another existing type
   * @param copyContentTypeBean {@link CopyContentTypeBean}
   * @return ContentType
   * @throws DotDataException Error occurred when performing the action.
   * @throws DotSecurityException The user does not have permissions to perform this action.
   */
  ContentType copyFrom(CopyContentTypeBean copyContentTypeBean) throws DotDataException, DotSecurityException;

  /**
   * Creates a copy of an existing Content Type and saves it to the specified Site.
   *
   * @param copyContentTypeBean The {@link CopyContentTypeBean} object containing the data of the
   *                            Content Type being copied.
   * @param destinationSite     The {@link Host} object representing the Site where the Content
   *                            Type will be saved.
   *
   * @return The {@link ContentType} object representing the new Content Type.
   *
   * @throws DotDataException     An error occurred when interacting with the database.
   * @throws DotSecurityException The User accessing this API does not have the required
   *                              permissions to perform this action.
   */
  ContentType copyFrom(final CopyContentTypeBean copyContentTypeBean, final Host destinationSite) throws DotDataException, DotSecurityException;

  /**
   * Creates a copy of an existing Content Type and saves it to the specified Site.
   *
   * @param copyContentTypeBean    The {@link CopyContentTypeBean} object containing the data of
   *                               the Content Type being copied.
   * @param destinationSite        The {@link Host} object representing the Site where the Content
   *                               Type will be saved.
   * @param copyRelationshipFields If Relationship Fields must be copied as well, set this to
   *                               {@code true}.
   *
   * @return The {@link ContentType} object representing the new Content Type.
   *
   * @throws DotDataException     An error occurred when interacting with the database.
   * @throws DotSecurityException The User accessing this API does not have the required
   *                              permissions to perform this action.
   */
  ContentType copyFrom(final CopyContentTypeBean copyContentTypeBean, final Host destinationSite, final boolean copyRelationshipFields) throws DotDataException, DotSecurityException;

  /**
   * Creates a copy of an existing Content Type and saves it.
   *
   * @param copyContentTypeBean The {@link CopyContentTypeBean} object containing the data of the
   *                            Content Type being copied.
   *
   * @return The {@link ContentType} object representing the new Content Type.
   *
   * @throws DotDataException     An error occurred when interacting with the database.
   * @throws DotSecurityException The User accessing this API does not have the required
   *                              permissions to perform this action.
   */
  ContentType copyFromAndDependencies(final CopyContentTypeBean copyContentTypeBean) throws DotDataException, DotSecurityException;

  /**
   * Creates a copy of an existing Content Type and saves it to the specified Site. Additionally,
   * the Workflow Schemes being used by the original Content Type will be assigned to the copied
   * Content Type.
   *
   * @param copyContentTypeBean The {@link CopyContentTypeBean} object containing the data of the
   *                            Content Type being copied.
   * @param destinationSite     The {@link Host} object representing the Site where the Content
   *                            Type will be saved.
   *
   * @return The {@link ContentType} object representing the new Content Type.
   *
   * @throws DotDataException     An error occurred when interacting with the database.
   * @throws DotSecurityException The User accessing this API does not have the required
   *                              permissions to perform this action.
   */
  ContentType copyFromAndDependencies(final CopyContentTypeBean copyContentTypeBean, final Host destinationSite) throws DotDataException, DotSecurityException;

  /**
   * Creates a copy of an existing Content Type and saves it to the specified Site. Additionally,
   * the Workflow Schemes being used by the original Content Type will be assigned to the copied
   * Content Type.
   *
   * @param copyContentTypeBean    The {@link CopyContentTypeBean} object containing the data of
   *                               the Content Type being copied.
   * @param destinationSite        The {@link Host} object representing the Site where the Content
   *                               Type will be saved.
   * @param copyRelationshipFields If Relationship Fields must be copied as well, set this to
   *                               {@code true}.
   *
   * @return The {@link ContentType} object representing the new Content Type.
   *
   * @throws DotDataException     An error occurred when interacting with the database.
   * @throws DotSecurityException The User accessing this API does not have the required
   *                              permissions to perform this action.
   */
  ContentType copyFromAndDependencies(final CopyContentTypeBean copyContentTypeBean, final Host destinationSite, final boolean copyRelationshipFields) throws DotDataException, DotSecurityException;

  /**
   * Saves a new Content Type.
   * 
   * @param type Content Type that is going to be modified
   * @return Content Type Object saved.
   * @throws DotDataException Error occurred when performing the action.
   * @throws DotSecurityException The user does not have permissions to perform this action.
   */
  ContentType save(ContentType type) throws DotDataException, DotSecurityException;

  /**
   * Returns a List of Content Types recently used based on the Base Content Type.
   * 
   * @param type Base Content Type which is going to be the filter.
   * @param numberToShow Amount of results
   * @return List of Content Type Objects
   * @throws DotDataException
   */
  List<ContentType> recentlyUsed(BaseContentType type, int numberToShow) throws DotDataException;
  
  /**
   * Returns a List of content types based on the given condition
   * 
   * @param condition Condition that the Content Type needs to meet
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> search(String condition) throws DotDataException;

  /**
   * Returns a List of content types based on the given condition, organized by the given column.
   * 
   * @param condition Condition that the Content Type needs to meet
   * @param orderBy Specifies an order criteria for the results
   * @param limit Amount of results
   * @param offset Start position of the resulting list
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> search(String condition, String orderBy, int limit, int offset) throws DotDataException;

  /**
   * Returns a List of content types based on the given condition, organized by the given column.
   *
   * @param condition Condition that the Content Type needs to meet
   * @param orderBy Specifies an order criteria for the results
   * @param limit Amount of results
   * @param offset Start position of the resulting list
   * @param hostId hostId where the contentType lives
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> search(String condition, String orderBy, int limit, int offset,String hostId) throws DotDataException;

  /**
   * Returns a List of content type based on the given condition and the Base Content Type, organized by the given column.
   * 
   * @param condition Condition that the Content Type needs to meet
   * @param base Base Content Type that wants to be searched
   * @param orderBy Specifies an order criteria for the results
   * @param limit Amount of results
   * @param offset Start position of the resulting list
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> search(String condition, BaseContentType base, String orderBy, int limit, int offset)
      throws DotDataException;

  /**
   * Returns a List of content type based on the given condition and the Base Content Type, organized by the given column.
   *
   * @param condition Condition that the Content Type needs to meet
   * @param base Base Content Type that wants to be searched
   * @param orderBy Specifies an order criteria for the results
   * @param limit Amount of results
   * @param offset Start position of the resulting list

   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> search(String condition, BaseContentType base, String orderBy, int limit, int offset, String hostId)
          throws DotDataException;

  /**
   * Returns a List of content type based on the given condition and the Base Content Type,
   * organized by the given column.
   *
   * @param condition           Condition that the Content Type needs to meet
   * @param base                Base Content Type that wants to be searched
   * @param orderBy             Specifies an order criteria for the results
   * @param limit               Amount of results
   * @param offset              Start position of the resulting list, skipping the value of records
   *                            passed by param. e.g:
   *                            offset = 0 -> start from the first record
   *                            offset = 10 -> start from the #11 record
   * @param requestedContentTypes The Content Types that are explicitly requested to be included.
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> search(String condition, BaseContentType base, String orderBy, int limit,
          int offset, String hostId, List<String> requestedContentTypes)
          throws DotDataException;

  /**
   * Returns a list of Content Types based on the specified list of search criteria. In
   * particular, this method allows you to search for Content Types in a specific list of Sites
   * only, not in all the dotCMS content repository.
   *
   * @param sites     The list of one or more Sites to search for Content Types. You can pass down
   *                  their Identifiers or Site Keys.
   * @param condition Allows you to add more conditions to the query via SQL code. It's internally
   *                  sanitized by this Factory.
   * @param base      The {@link BaseContentType} to search for.
   * @param orderBy   The order-by clause, which is internally sanitized by this Factory.
   * @param limit     The maximum number of returned items in the result set, for pagination
   *                  purposes.
   * @param offset    Start position of the result list, skipping the value of records passed
   *                  by param. e.g:
   *                  offset = 0 -> start from the first record
   *                  offset = 10 -> start from the #11 record
   *
   * @return The list of {@link ContentType} objects matching the specified search criteria.
   *
   * @throws DotDataException An error occurred when retrieving information from the database.
   */
  List<ContentType> search(final List<String> sites, final String condition, final BaseContentType base, final String orderBy, final int limit, final int offset)
          throws DotDataException;

  /**
   * Returns a list of Content Types based on the specified list of search criteria. In
   * particular, this method allows you to search for Content Types in a specific list of Sites
   * only, not in all the dotCMS content repository.
   *
   * @param sites     The list of one or more Sites to search for Content Types. You can pass down
   *                  their Identifiers or Site Keys.
   * @param condition Allows you to add more conditions to the query via SQL code. It's internally
   *                  sanitized by this Factory.
   * @param base      The {@link BaseContentType} to search for.
   * @param orderBy   The order-by clause, which is internally sanitized by this Factory.
   * @param limit     The maximum number of returned items in the result set, for pagination
   *                  purposes.
   * @param offset    Start position of the result list, skipping the value of records passed
   *                  by param. e.g:
   *                  offset = 0 -> start from the first record
   *                  offset = 10 -> start from the #11 record
   * @param includeContentTypeIds
   *                  The Content Types that are explicitly required to be included.
   *
   * @return The list of {@link ContentType} objects matching the specified search criteria.
   *
   * @throws DotDataException An error occurred when retrieving information from the database.
   */
  List<ContentType> search(final List<String> sites, final String condition,
          final BaseContentType base, final String orderBy, final int limit, final int offset,
          List<String> includeContentTypeIds)
          throws DotDataException;

  /**
   * Searches for Content Types matching multiple base types in a single efficient database query.
   * This method uses a UNION query to combine results from multiple base types, sort them,
   * and paginate efficiently at the database level.
   * <p>
   * This is significantly more performant and scalable than querying each type separately
   * and combining results in memory, especially with large numbers of content types.
   *
   * @param condition          Filter condition that Content Types must meet. It's internally
   *                           sanitized by the API.
   * @param types              Collection of Base Content Types to search for (must not be empty).
   * @param orderBy            The order-by clause, which is internally sanitized by the API.
   * @param limit              Maximum number of items to return in the result set, for pagination.
   *                           Use -1 for no limit (up to 10000).
   * @param offset             The page offset in the result set, for pagination purposes.
   * @param siteId             The ID of the Site that Content Types live in. Can be null or empty for all sites.
   * @param requestedContentTypes Optional list of specific content type variables to ensure are included.
   *
   * @return The list of {@link ContentType} objects matching the criteria, sorted and paginated.
   *
   * @throws DotDataException An error occurred when retrieving information from the database.
   */
  List<ContentType> searchMultipleTypes(final String condition, final java.util.Collection<BaseContentType> types,
                                        final String orderBy, final int limit, final int offset,
                                        final String siteId, final List<String> requestedContentTypes)
          throws DotDataException;

  /**
   * Return the number of entries for each content types
   *
   * @return return a Map where the keys are the content types' variable name and the values are the number of entries
   * @throws DotDataException
   */
  Map<String, Long> getEntriesByContentTypes() throws DotStateException;

  /**
   * Return the number of entries for each content types in a specific site
   *
   * @return return a Map where the keys are the content types' variable name and the values are the number of entries
   * @throws DotDataException
   */
  Map<String, Long> getEntriesByContentTypes(final String siteId) throws DotStateException;
  
  /**
   * Save or update a Content Type. If the Content Type already exist
   * then it's going to update the fields with the values set on the fields
   * parameter
   * 
   * @param contentType Content Type that is going to be modified
   * @param newFields Content Type list of fields
   * @return Content Type Object saved.
   * @throws DotDataException Error occurred when performing the action.
   * @throws DotSecurityException The user does not have permissions to perform this action.
   */
  ContentType save(ContentType contentType, List<Field> newFields) throws DotDataException, DotSecurityException;
  
  /**
   * Save or update a Content Type. If the Content Type already exist
   * then it's going to update the fields and fields variables with the values set 
   * on the fields and fieldVariables parameters 
   * 
   * @param contentType Content Type that is going to be modified
   * @param newFields Content Type list of fields
   * @param newFieldVariables ContentType list of field variables
   * @return Content Type Object saved.
   * @throws DotDataException Error occurred when performing the action.
   * @throws DotSecurityException The user does not have permissions to perform this action.
   */
  ContentType save(ContentType contentType, List<Field> newFields, List<FieldVariable> newFieldVariables) throws DotDataException, DotSecurityException;
  
  /**
   * Update the Content Type mod_date and clean the cache
   * @param type Content Type that is going to be modified
   * @return true if the mod_date was updated, false if not
   * @throws DotDataException 
   */
   boolean updateModDate(ContentType type) throws DotDataException;

  boolean updateModDate(Field field) throws DotDataException;

  /**
   * Remove url mapping and detail page from a specific content type
   * @param contentType Content Type that is going to be modified
   * @throws DotSecurityException
   * @throws DotDataException
   */
  void unlinkPageFromContentType(ContentType contentType)
          throws DotSecurityException, DotDataException;

    /**
     * Given a content type, verifies if the content type can be used considering the {@link LicenseLevel}.
     * If the {@link LicenseLevel} is Community, only the content types that are not an {@link com.dotcms.contenttype.model.type.EnterpriseType} will be allowed
     * @param contentType
     * @return
     */
    boolean isContentTypeAllowed(ContentType contentType);

  /**
   * Return the count of {@link ContentType} assigned to not SYSTEM_WORFLOW
   * @return
   */
  long countContentTypeAssignedToNotSystemWorkflow() throws DotDataException;

}
