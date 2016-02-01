package com.dotmarketing.tag.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
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
    private String byHostCacheGroup = "tagsByHostCache";

    //Region's name for the cache
    private String[] groupNames = { primaryGroup, byNameCacheGroup };

    public TagCacheImpl () {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    protected Tag get ( String tagId ) throws DotDataException {
        try {
            return (Tag) cache.get(getPrimaryGroup() + tagId, getPrimaryGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected Tag get ( String name, String hostId ) throws DotDataException {

        name = name.toLowerCase();

        try {
            return (Tag) cache.get(getTagByNameHostGroup() + name + "_" + hostId, getTagByNameHostGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected List<Tag> getByName ( String name ) throws DotDataException {

        name = name.toLowerCase();

        try {
            return (List<Tag>) cache.get(getTagsByNameGroup() + name, getTagsByNameGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected List<Tag> getByHost ( String hostId ) throws DotDataException {
        try {
            return (List<Tag>) cache.get(getTagsByHostGroup() + hostId, getTagsByHostGroup());
        } catch ( DotCacheException e ) {
            Logger.debug(this, "Cache Entry not found", e);
            return null;
        }
    }

    @Override
    protected void putForName ( String name, List<Tag> tags ) throws DotDataException {
        name = name.toLowerCase();
        cache.put(getTagsByNameGroup() + name, tags, getTagsByNameGroup());
    }

    @Override
    protected void putForHost ( String hostId, List<Tag> tags ) throws DotDataException {
        cache.put(getTagsByHostGroup() + hostId, tags, getTagsByHostGroup());
    }

    @Override
    protected void put ( Tag object ) throws DotDataException, DotCacheException {
        //Adding the tag by id
        cache.put(getPrimaryGroup() + object.getTagId(), object, getPrimaryGroup());
    }

    @Override
    protected void remove ( Tag object ) throws DotDataException, DotCacheException {
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
    public void clearCache () {
        cache.flushGroup(getPrimaryGroup());
        cache.flushGroup(getTagByNameHostGroup());
        cache.flushGroup(getTagsByNameGroup());
        cache.flushGroup(getTagsByHostGroup());
    }

    public String[] getGroups () {
        return groupNames;
    }

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
    public String getTagByNameHostGroup () {
        return byNameAndHostCacheGroup;
    }

}