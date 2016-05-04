package com.dotmarketing.cache;

import java.util.List;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.model.Structure;

/**
 * Abstract class that wrapped StructureCache.java where only expose methods
 * that were not deprecated in StructureCache.java
 * 
 * @author rogelioblanco
 *
 */
public abstract class ContentTypeCache implements Cachable {

    abstract String getContainerStructureGroup();

    abstract void add(Structure st);

    abstract Structure getStructureByInode(String inode);

    abstract Structure getStructureByVelocityVarName(String variableName);

    abstract void remove(Structure st);

    abstract String getURLMasterPattern() throws DotCacheException;

    abstract void clearURLMasterPattern();

    abstract void addURLMasterPattern(String pattern);

    abstract void addContainerStructures(
            List<ContainerStructure> containerStructures,
            String containerIdentifier, String containerInode);

    abstract List<ContainerStructure> getContainerStructures(
            String containerIdentifier, String containerInode);

    abstract void removeContainerStructures(String containerIdentifier,
            String containerInode);
}
