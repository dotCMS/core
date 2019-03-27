package com.dotmarketing.tag.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.tag.model.Tag;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 1/28/16
 */
public abstract class TagCache implements Cachable {

    abstract protected void remove ( Tag object );

    abstract protected void removeByInode ( String inode );

    abstract protected Tag get ( String tagId );

    abstract protected Tag get ( String name, String hostId );

    abstract protected List<Tag> getByName ( String name );

    abstract protected List<Tag> getByHost ( String hostId );

    abstract protected List<Tag> getByInode ( String inode );

    abstract protected void putForInode ( String inode, List<Tag> tags );

    abstract protected void putForName ( String name, List<Tag> tags );

    abstract protected void putForHost ( String hostId, List<Tag> tags );

    abstract protected void put ( Tag object );

    abstract public String getTagsByNameGroup ();

    abstract public String getTagsByHostGroup ();

    abstract public String getTagsByInodeGroup ();

    abstract public String getTagByNameHostGroup ();

}