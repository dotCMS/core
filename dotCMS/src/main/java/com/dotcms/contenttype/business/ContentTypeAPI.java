package com.dotcms.contenttype.business;

import com.dotcms.enterprise.license.LicenseLevel;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;

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
   * Deletes the given Content Type
   * 
   * @param st Content Type that will be deleted
   * @throws DotSecurityException The user does not have permissions to perform this action.
   * @throws DotDataException Error occurred when performing the action.
   */
  void delete(ContentType st) throws DotSecurityException, DotDataException;

  /**
   * Find a Content Type given the inode
   * 
   * @param inodeOrVar Either the Inode or the Velocity var name representing the Structure to find
   * @return Content Type Object
   * @throws DotSecurityException The user does not have permissions to perform this action.
   * @throws DotDataException Error occurred when performing the action.
   */
  ContentType find(String inodeOrVar) throws DotSecurityException, DotDataException;

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
   * @param type Base Content Type that will be search
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
   * Counts the amount of Content Types in the DB filtered by the given condition.
   * 
   * @return Amount of Content Types
   * @throws DotDataException Error occurred when performing the action.
   */
  int count(String condition) throws DotDataException;

  /**
   * Counts the amount of Content Types in the DB filtered by the given condition and the BaseContentType.
   * 
   * @param condition Condition that the Content Type needs to met
   * @param base Base Content Type that wants to be search
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
   * @param condition Condition that the Content Type needs to met
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> search(String condition) throws DotDataException;

  /**
   * Returns a List of content types based on the given condition, organized by the given column.
   * 
   * @param condition Condition that the Content Type needs to met
   * @param orderBy Specifies an order criteria for the results
   * @param limit Amount of results
   * @param offset Start position of the resulting list
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> search(String condition, String orderBy, int limit, int offset) throws DotDataException;

  /**
   * Returns a List of content type based on the given condition and the Base Content Type, organized by the given column.
   * 
   * @param condition Condition that the Content Type needs to met
   * @param base Base Content Type that wants to be search
   * @param orderBy Specifies an order criteria for the results
   * @param limit Amount of results
   * @param offset Start position of the resulting list
   * @return List of Content Types Objects
   * @throws DotDataException Error occurred when performing the action.
   */
  List<ContentType> search(String condition, BaseContentType base, String orderBy, int limit, int offset)
      throws DotDataException;


  /**
   * Return the number of entries for each content types
   *
   * @return return a Map where the keys are the content types' variable name and the values are the number of entries
   * @throws DotDataException
   */
  Map<String, Long> getEntriesByContentTypes() throws DotDataException;
  
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
}
