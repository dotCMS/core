package com.dotmarketing.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

/**
 * 
 * @author rogelioblanco
 *
 */
public abstract class ContentTypeCache implements Cachable {
    public abstract String getContainerStructureGroup();

    public abstract void add(Structure st);

    public abstract Structure getStructureByInode(String inode);

    public abstract Structure getStructureByName(String name);

    public abstract Structure getStructureByVelocityVarName(String variableName);

    @Deprecated
    public abstract Structure getStructureByType(String type);

    @Deprecated
    public abstract boolean hasStructureByType(String name);

    @Deprecated
    public abstract boolean hasStructureByName(String name);

    public abstract boolean hasStructureByVelocityVarName(String varname);

    public abstract boolean hasStructureByInode(String inode);

    public abstract void remove(Structure st);

    public abstract String getURLMasterPattern() throws DotCacheException;

    public abstract void clearURLMasterPattern();

    public abstract void addURLMasterPattern(String pattern);

    public abstract void addContainerStructures(
            List<ContainerStructure> containerStructures,
            String containerIdentifier, String containerInode);

    public abstract List<ContainerStructure> getContainerStructures(
            String containerIdentifier, String containerInode);

    public abstract void removeContainerStructures(String containerIdentifier,
            String containerInode);
    
    public abstract String getStructuresByTypeGroup();
    
    public abstract List<Structure> getStructuresByType(int structureType);
    
    public abstract void addStructuresByType(List<Structure> structures, int structureType);
    
    public abstract void removeStructuresByType(int structureType);

    public abstract void addRecents(Structure.Type type, User user, int nRecents, Collection<Map<String, Object>> recents);

    public abstract Collection<Map<String, Object>> getRecents(Structure.Type type, User user, int nRecents);

    public abstract void clearRecents(String userId);
}
