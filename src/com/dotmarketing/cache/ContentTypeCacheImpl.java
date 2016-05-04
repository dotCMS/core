package com.dotmarketing.cache;

import java.util.List;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.model.Structure;

/**
 * Wrapper cache implementation for StructureCache.java.
 * 
 * @author rogelioblanco
 *
 */
public class ContentTypeCacheImpl extends ContentTypeCache {

    @Override
    public String getPrimaryGroup() {
        return StructureCache.getPrimaryGroup();
    }

    @Override
    public String getContainerStructureGroup() {
        return StructureCache.getContainerStructureGroup();
    }

    @Override
    public String[] getGroups() {
        return StructureCache.getGroups();
    }

    @Override
    public void clearCache() {
        StructureCache.clearCache();
    }

    @Override
    public void add(Structure st) {
        StructureCache.addStructure(st);
    }

    @Override
    public Structure getStructureByInode(String inode) {
        return StructureCache.getStructureByInode(inode);
    }

    @Override
    public Structure getStructureByVelocityVarName(String variableName) {
        return StructureCache.getStructureByVelocityVarName(variableName);
    }

    @Override
    public void remove(Structure st) {
        StructureCache.removeStructure(st);
    }

    @Override
    public String getURLMasterPattern() throws DotCacheException {
        return StructureCache.getURLMasterPattern();
    }

    @Override
    public void clearURLMasterPattern() {
        StructureCache.clearURLMasterPattern();
    }

    @Override
    public void addURLMasterPattern(String pattern) {
        StructureCache.addURLMasterPattern(pattern);
    }

    @Override
    public void addContainerStructures(
            List<ContainerStructure> containerStructures,
            String containerIdentifier, String containerInode) {
        StructureCache.addContainerStructures(containerStructures,
                containerIdentifier, containerInode);
    }

    @Override
    public List<ContainerStructure> getContainerStructures(
            String containerIdentifier, String containerInode) {
        return StructureCache.getContainerStructures(containerIdentifier,
                containerInode);
    }

    @Override
    public void removeContainerStructures(String containerIdentifier,
            String containerInode) {
        StructureCache.removeContainerStructures(containerIdentifier,
                containerInode);
    }
}
