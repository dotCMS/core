package com.dotmarketing.cache;


import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.business.ContentTypeCache2Impl;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

/**
 * 
 * @author rogelioblanco
 *
 */
public abstract class ContentTypeCache extends ContentTypeCache2Impl {


    public abstract void add(Structure st);

    @Deprecated
    public abstract Structure getStructureByInode(String inode);

    @Deprecated
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

    public abstract void addRecents(Structure.Type type, User user, int nRecents, Collection<Map<String, Object>> recents);

    public abstract Collection<Map<String, Object>> getRecents(Structure.Type type, User user, int nRecents);

    public abstract void clearRecents(String userId);

    /**
     * Cleans the containers/structures
     */
    public abstract void clearContainerStructures();
}
