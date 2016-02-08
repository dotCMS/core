package com.dotmarketing.tag.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.tag.model.TagInode;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 2/2/16
 */
public abstract class TagInodeCache implements Cachable {

    abstract protected TagInode get ( String tagId, String inode, String fieldVarName );

    abstract protected void put ( TagInode object );

    abstract protected List<TagInode> getByTagId ( String tagId );

    abstract protected void putForTagId ( String tagId, List<TagInode> tagInodes );

    abstract protected List<TagInode> getByInode ( String inode );

    abstract protected void putForInode ( String inode, List<TagInode> tagInodes );

    abstract protected void remove ( TagInode object );

    abstract protected void removeByTagId ( String tagId );

    abstract protected void removeByInode ( String inode );

    abstract protected String getTagInodesByTagIdGroup ();

    abstract protected String getTagInodesByInodeGroup ();
}