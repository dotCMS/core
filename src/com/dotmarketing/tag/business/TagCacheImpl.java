package com.dotmarketing.tag.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 1/28/16
 */
public class TagCacheImpl extends TagCache {

    private DotCacheAdministrator cache;

    private String primaryGroup = "TagCache";
    private String byNameCacheGroup = "tagsByNameCache";
    private String byNameAndHostCacheGroup = "tagByNameAndHostCache";
    private String byInodeCacheGroup = "tagsByInodeCache";
    private String byHostCacheGroup = "tagsByHostCache";

    //Region's name for the cache
    private String[] groupNames = { primaryGroup, byNameCacheGroup, byNameAndHostCacheGroup, byInodeCacheGroup, byHostCacheGroup};

    public TagCacheImpl () {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    protected Tag get ( String tagId ) {
        try {
            return (Tag) cache.get(getPrimaryGroup() + tagId, getPrimaryGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected Tag get ( String name, String hostId ) {

        name = name.toLowerCase();

        try {
            return (Tag) cache.get(getTagByNameHostGroup() + name + "_" + hostId, getTagByNameHostGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected List<Tag> getByName ( String name ) {

        name = name.toLowerCase();

        try {
            return (List<Tag>) cache.get(getTagsByNameGroup() + name, getTagsByNameGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected List<Tag> getByHost ( String hostId ) {
        try {
            return (List<Tag>) cache.get(getTagsByHostGroup() + hostId, getTagsByHostGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected List<Tag> getByInode ( String inode ) {
        try {
            return (List<Tag>) cache.get(getTagsByInodeGroup() + inode, getTagsByInodeGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected void putForInode ( String inode, List<Tag> tags ) {
        cache.put(getTagsByInodeGroup() + inode, tags, getTagsByInodeGroup());
    }

    @Override
    protected void putForName ( String name, List<Tag> tags ) {
        name = name.toLowerCase();
        cache.put(getTagsByNameGroup() + name, tags, getTagsByNameGroup());
    }

    @Override
    protected void putForHost ( String hostId, List<Tag> tags ) {
        cache.put(getTagsByHostGroup() + hostId, tags, getTagsByHostGroup());
    }

    @Override
    protected void put ( Tag object ) {

        //First clean up list references to this tag name and host
        //Removing by name
        cache.remove(getTagsByNameGroup() + object.getTagName().toLowerCase(), getTagsByNameGroup());
        //Removing by host
        cache.remove(getTagsByHostGroup() + object.getHostId(), getTagsByHostGroup());

        //Adding the tag by id
        cache.put(getPrimaryGroup() + object.getTagId(), object, getPrimaryGroup());
        cache.put(getTagByNameHostGroup() + object.getTagName().toLowerCase() + "_" + object.getHostId(), object, getTagByNameHostGroup());
    }

    @Override
    protected void remove ( Tag object ) {
        //Removing by id
        cache.remove(getPrimaryGroup() + object.getTagId(), getPrimaryGroup());
        //Removing by name and host
        cache.remove(getTagByNameHostGroup() + object.getTagName().toLowerCase() + "_" + object.getHostId(), getTagByNameHostGroup());
        //Removing by name
        cache.remove(getTagsByNameGroup() + object.getTagName().toLowerCase(), getTagsByNameGroup());
        //Removing by host
        cache.remove(getTagsByHostGroup() + object.getHostId(), getTagsByHostGroup());
    }

    @Override
    protected void removeByInode ( String inode ) {
        cache.remove(getTagsByInodeGroup() + inode, getTagsByInodeGroup());
    }

    @Override
    public void clearCache() {
        for (String cacheGroup : getGroups()) {
            cache.flushGroup(cacheGroup);
        }
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
    public String getTagsByNameGroup () {
        return byNameCacheGroup;
    }

    @Override
    public String getTagsByHostGroup () {
        return byHostCacheGroup;
    }

    @Override
    public String getTagsByInodeGroup () {
        return byInodeCacheGroup;
    }

    @Override
    public String getTagByNameHostGroup () {
        return byNameAndHostCacheGroup;
    }

}