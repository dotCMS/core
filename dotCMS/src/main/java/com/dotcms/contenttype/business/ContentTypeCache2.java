package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import java.util.List;

public interface ContentTypeCache2 extends Cachable {
  public static final String primaryGroup = "ContentTypeCache";
  public static final String containerStructureGroup = "ContainerStructureCache";
  public static final String MASTER_STRUCTURE = "dotMaster_Structure";
  // region names for the cache
  public static final String[] groups = {primaryGroup, containerStructureGroup, MASTER_STRUCTURE};

  void remove(ContentType type);

  void removeContainerStructures(String containerIdentifier, String containerInode);

  List<ContainerStructure> getContainerStructures(
      String containerIdentifier, String containerInode);

  void addContainerStructures(
      List<ContainerStructure> containerStructures,
      String containerIdentifier,
      String containerInode);

  void addURLMasterPattern(String pattern);

  void clearURLMasterPattern();

  String getURLMasterPattern() throws DotCacheException;

  void add(ContentType type);

  ContentType byVarOrInode(String varOrInode);
}
