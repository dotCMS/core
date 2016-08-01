package com.dotmarketing.cache;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.model.Structure;

/**
 * 
 * @author rogelioblanco
 *
 */
public abstract class ContentTypeCache implements Cachable {
    public abstract String getContainerStructureGroup();

    @Deprecated
    public abstract void add(Structure st);
    @Deprecated
    public abstract Structure getStructureByInode(String inode);
    @Deprecated
    public abstract Structure getStructureByVelocityVarName(String variableName);
    @Deprecated
    public abstract Structure getStructureByName(String variableName);
    @Deprecated
    public abstract boolean hasStructureByVelocityVarName(String varname);
    @Deprecated
    public abstract boolean hasStructureByInode(String inode);
    @Deprecated
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
 

	public abstract void add(ContentType type);
	
    public abstract void remove(ContentType type);
    
    public abstract ContentType byInode(String inode);
    public abstract ContentType byVar(String var);
}
