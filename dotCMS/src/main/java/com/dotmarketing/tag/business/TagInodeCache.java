package com.dotmarketing.tag.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.tag.model.TagInode;
import java.util.List;

/** @author Jonathan Gamba Date: 2/2/16 */
public abstract class TagInodeCache implements Cachable {

  protected abstract TagInode get(String tagId, String inode, String fieldVarName);

  protected abstract void put(TagInode object);

  protected abstract List<TagInode> getByTagId(String tagId);

  protected abstract void putForTagId(String tagId, List<TagInode> tagInodes);

  protected abstract List<TagInode> getByInode(String inode);

  protected abstract void putForInode(String inode, List<TagInode> tagInodes);

  protected abstract void remove(TagInode object);

  protected abstract void removeByTagId(String tagId);

  protected abstract void removeByInode(String inode);

  protected abstract String getTagInodesByTagIdGroup();

  protected abstract String getTagInodesByInodeGroup();
}
