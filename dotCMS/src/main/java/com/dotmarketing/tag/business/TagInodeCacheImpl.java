package com.dotmarketing.tag.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 2/2/16
 */
public class TagInodeCacheImpl extends TagInodeCache {

    private DotCacheAdministrator cache;

    private String primaryGroup = "TagInodeCache";
    private String byTagIdCacheGroup = "tagInodesByTagIdCache";
    private String byInodeCacheGroup = "tagInodesByInodeCache";

    //Region's name for the cache
    private String[] groupNames = { primaryGroup, byTagIdCacheGroup, byInodeCacheGroup };

    public TagInodeCacheImpl () {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    protected TagInode get ( String tagId, String inode, String fieldVarName ) {
        try {

            if ( fieldVarName == null ) {
                fieldVarName = "";
            }

            return (TagInode) cache.get(getPrimaryGroup() + tagId + "_" + inode + "_" + fieldVarName, getPrimaryGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected void put ( TagInode object ) {

        String fieldVarName = "";
        if ( UtilMethods.isSet(object.getFieldVarName()) ) {
            fieldVarName = object.getFieldVarName();
        }

        //First clean up list references to this inode and tag id
        cache.remove(getTagInodesByInodeGroup() + object.getInode(), getTagInodesByInodeGroup());
        cache.remove(getTagInodesByTagIdGroup() + object.getTagId(), getTagInodesByTagIdGroup());

        //Adding the tag inode using the ids
        cache.put(getPrimaryGroup() + object.getTagId() + "_" + object.getInode() + "_" + fieldVarName, object, getPrimaryGroup());
    }

    @Override
    protected List<TagInode> getByTagId ( String tagId ) {
        try {
            return (List<TagInode>) cache.get(getTagInodesByTagIdGroup() + tagId, getTagInodesByTagIdGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected void putForTagId ( String tagId, List<TagInode> tagInodes ) {
        cache.put(getTagInodesByTagIdGroup() + tagId, tagInodes, getTagInodesByTagIdGroup());
    }

    @Override
    protected List<TagInode> getByInode ( String inode ) {
        try {
            return (List<TagInode>) cache.get(getTagInodesByInodeGroup() + inode, getTagInodesByInodeGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected void putForInode ( String inode, List<TagInode> tagInodes ) {
        cache.put(getTagInodesByInodeGroup() + inode, tagInodes, getTagInodesByInodeGroup());
    }

    @Override
    protected void remove ( TagInode object ) {

        String fieldVarName = "";
        if ( UtilMethods.isSet(object.getFieldVarName()) ) {
            fieldVarName = object.getFieldVarName();
        }

        //Removing by id
        cache.remove(getPrimaryGroup() + object.getTagId() + "_" + object.getInode() + "_" + fieldVarName, getPrimaryGroup());
        //Removing by tag id
        cache.remove(getTagInodesByTagIdGroup() + object.getTagId(), getTagInodesByTagIdGroup());
        //Removing by inode
        cache.remove(getTagInodesByInodeGroup() + object.getInode(), getTagInodesByInodeGroup());
    }

    @Override
    protected void removeByTagId ( String tagId ) {
        List<TagInode> cachedObjects = getByTagId(tagId);
        if ( cachedObjects != null && !cachedObjects.isEmpty() ) {
            for ( TagInode cachedObject : cachedObjects ) {
                remove(cachedObject);
            }
        }
    }

    @Override
    protected void removeByInode(String inode) {
        List<TagInode> cachedObjects = getByInode(inode);
        if (cachedObjects != null && !cachedObjects.isEmpty()) {
            for (TagInode cachedObject : cachedObjects) {
                remove(cachedObject);
            }
        }
    }

    @Override
    public void clearCache () {
        cache.flushGroup(getPrimaryGroup());
        cache.flushGroup(getTagInodesByTagIdGroup());
        cache.flushGroup(getTagInodesByInodeGroup());
    }

    @Override
    public String[] getGroups () {
        return groupNames;
    }

    @Override
    public String getPrimaryGroup () {
        return primaryGroup;
    }

    @Override
    public String getTagInodesByTagIdGroup () {
        return byTagIdCacheGroup;
    }

    @Override
    public String getTagInodesByInodeGroup () {
        return byInodeCacheGroup;
    }

}