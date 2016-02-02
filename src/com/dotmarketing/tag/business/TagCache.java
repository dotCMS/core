package com.dotmarketing.tag.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.tag.model.Tag;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 1/28/16
 */
public abstract class TagCache implements Cachable {

    /**
     * This method removes the category entry from the cache
     * based on the category inode
     *
     * @param object
     * @throws DotDataException
     * @throws DotCacheException
     */
    abstract protected void remove ( Tag object );

    abstract protected void removeByInode ( String inode );

    /**
     * This method get a category object from the cache based
     * on the passed inode, if the object does not exist
     * in cache a null value is returned
     *
     * @param id
     * @return
     * @throws DotDataException
     */
    abstract protected Tag get ( String tagId );

    abstract protected Tag get ( String name, String hostId );

    /**
     * This method get a category object from the cache based
     * on the passed inode, if the object does not exist
     * in cache a null value is returned
     *
     * @param id
     * @return
     * @throws DotDataException
     */
    abstract protected List<Tag> getByName ( String name );

    abstract protected List<Tag> getByHost ( String hostId );

    abstract protected List<Tag> getByInode ( String inode );

    abstract protected void putForInode ( String inode, List<Tag> tags );

    abstract protected void putForName ( String name, List<Tag> tags );

    abstract protected void putForHost ( String hostId, List<Tag> tags );

    /**
     * This method puts a category object in cache
     * using the category inode as key this method also
     * triggers the removal of children and parents from the cache
     *
     * @param object
     * @throws DotDataException
     * @throws DotCacheException
     */
    abstract protected void put ( Tag object );

    /**
     * use to get the group name used in the cache
     *
     * @return
     */
    abstract public String getTagsByNameGroup ();

    abstract public String getTagsByHostGroup ();

    abstract public String getTagsByInodeGroup ();

    abstract public String getTagByNameHostGroup ();

}