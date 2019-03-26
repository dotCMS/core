package com.dotmarketing.tag.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.tag.model.Tag;
import java.util.List;

/** @author Jonathan Gamba Date: 1/28/16 */
public abstract class TagCache implements Cachable {

  protected abstract void remove(Tag object);

  protected abstract void removeByInode(String inode);

  protected abstract Tag get(String tagId);

  protected abstract Tag get(String name, String hostId);

  protected abstract List<Tag> getByName(String name);

  protected abstract List<Tag> getByHost(String hostId);

  protected abstract List<Tag> getByInode(String inode);

  protected abstract void putForInode(String inode, List<Tag> tags);

  protected abstract void putForName(String name, List<Tag> tags);

  protected abstract void putForHost(String hostId, List<Tag> tags);

  protected abstract void put(Tag object);

  public abstract String getTagsByNameGroup();

  public abstract String getTagsByHostGroup();

  public abstract String getTagsByInodeGroup();

  public abstract String getTagByNameHostGroup();
}
