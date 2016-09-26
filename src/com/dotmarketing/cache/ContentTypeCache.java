package com.dotmarketing.cache;

import com.dotcms.contenttype.business.ContentTypeCache2Impl;
import com.dotmarketing.portlets.structure.model.Structure;

/**
 * 
 * @author rogelioblanco
 *
 */
public abstract class ContentTypeCache extends ContentTypeCache2Impl {


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




}
