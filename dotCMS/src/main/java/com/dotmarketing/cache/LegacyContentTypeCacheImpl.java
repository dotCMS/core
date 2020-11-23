package com.dotmarketing.cache;

import java.util.Collection;
import java.util.Map;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.rendering.velocity.services.ContentTypeLoader;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author David
 */
@Deprecated
public class LegacyContentTypeCacheImpl extends ContentTypeCache {


  private final DotCacheAdministrator cache;

  // region's name for the cache


  public LegacyContentTypeCacheImpl() {
    cache = CacheLocator.getCacheAdministrator();
  }


  public void add(Structure st) {


  }

  private Structure byVar(String inode) {
    try {
      ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(inode);
      return new StructureTransformer(type).asStructure();
    } catch (Exception e) {
      Logger.warn(this.getClass(), "Structure with inode: '" + inode + "' not found in db");
    }


    return new Structure();
  }

  public Structure getStructureByInode(String inode) {

    return byVar(inode);
  }

  /**
   * This methods retrieves the structure from the cache based in the structure name.
   * 
   * This methods tries to retrieve the structure from the cache, if the structure were not found in
   * the cache, it would try to find it in database and store it in cache.
   * 
   * <b>NOTE:</b> This method runs the same code than getStructureByType the name and the type of a
   * structure are synonyms
   * 
   * @param name Name of the structure
   * @return The structure from cache
   * 
   * @deprecated getting the structure by its name might not be safe, since the structure name can
   *             be changed by the user, use getStructureByVelocityVarName
   */
  public Structure getStructureByName(String name) {
    return byVar(name);
  }

  /**
   * This methods retrieves the structure from the cache based in the structure velocity variable
   * name which gets set once and never changes.
   * 
   * This methods tries to retrieve the structure from the cache, if the structure were not found in
   * the cache, it would try to find it in database and store it in cache.
   * 
   * @param variableName Name of the structure
   * @return The structure from cache
   * 
   */
  public Structure getStructureByVelocityVarName(String variableName) {

    return byVar(variableName);
  }

  /**
   * @see getStructureByType(String)
   * 
   * @param type Type of the structure
   * @return The structure from cache
   * 
   * @deprecated getting the structure by its name might not be safe, since the structure name can
   *             be changed by the user, use getStructureByVelocityVarName
   */
  public Structure getStructureByType(String type) {
    return byVar(type);
  }

  /**
   * @deprecated getting the structure by its name might not be safe, since the structure name can
   *             be changed by the user, use getStructureByVelocityVarName
   */
  public boolean hasStructureByType(String name) {
    return getStructureByType(name) != null;
  }

  /**
   * @deprecated getting the structure by its name might not be safe, since the structure name can
   *             be changed by the user, use getStructureByVelocityVarName
   */
  public boolean hasStructureByName(String name) {
    return getStructureByVelocityVarName(name) != null;
  }

  public boolean hasStructureByVelocityVarName(String varname) {
    return getStructureByVelocityVarName(varname) != null;
  }

  public boolean hasStructureByInode(String inode) {
    return getStructureByInode(inode) != null;
  }


  public void remove(Structure st) {
      new ContentTypeLoader().invalidate(st);
    ContentType type = new StructureTransformer(st).from();
    super.remove(type);
  }



  public void removeContainerStructures(String containerIdentifier, String containerInode) {
    cache.remove(containerStructureGroup + containerIdentifier + containerInode, containerStructureGroup);
  }



  public void clearCache() {

  }

  public String[] getGroups() {
    return groups;
  }

  public String getPrimaryGroup() {
    return primaryGroup;
  }

  @Override
  public void addRecents(Structure.Type type, User user, int nRecents, Collection<Map<String, Object>> recents) {


  }

  public Collection<Map<String, Object>> getRecents(Structure.Type type, User user, int nRecents) {

    // get this from the
    return ImmutableList.of(ImmutableMap.of());
  }

  public void clearRecents(String userId) {

  }

  public void clearContainerStructures() {

    cache.flushGroup(containerStructureGroup);
  }


}
