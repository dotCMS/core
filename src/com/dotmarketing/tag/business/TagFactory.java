package com.dotmarketing.tag.business;

import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;

import java.util.List;

public interface TagFactory {

    /**
     * Get a list of all the tags created
     *
     * @return list of all tags created
     */
    public java.util.List<Tag> getAllTags ();

    /**
     * Gets a Tag by name
     *
     * @param name name of the tag to get
     * @return tag
     */
    public java.util.List<Tag> getTagByName ( String name ) throws DotDataException, DotCacheException;

    public java.util.List<Tag> getTagByHost ( String hostId ) throws DotDataException, DotCacheException;

    public List<Tag> getTagsLikeNameAndHostIncludingSystemHost ( String name, String hostId ) throws DotHibernateException;

    public Tag getTagByNameAndHost ( String name, String hostId ) throws DotDataException, DotCacheException;

    /**
     * Gets a Tag by a tagId retrieved from a TagInode.
     *
     * @param tagId the tag id to get
     * @return tag
     * @throws DotHibernateException
     */
    public Tag getTagByTagId ( String tagId ) throws DotDataException, DotCacheException;

    /**
     * Gets all the tag created by an user
     *
     * @param userId id of the user
     * @return a list of all the tags created
     */
    public java.util.List<Tag> getTagByUser ( String userId );

    /**
     * Gets all tags filtered by tag name and/or host name
     *
     * @param tagName          tag name
     * @param hostFilter
     * @param globalTagsFilter
     * @param sort
     * @param start
     * @param count
     * @return a list of tags filtered by tag name or host name
     */
    public java.util.List<Tag> getFilteredTags ( String tagName, String hostFilter, boolean globalTagsFilter, String sort, int start, int count );

    public void updateTagInode ( TagInode tagInode, String tagId ) throws DotDataException, DotCacheException;

    /**
     * Creates a new tag
     *
     * @param tagName name of the new tag
     * @param userId  owner of the new tag
     * @param hostId
     * @return new tag created
     * @throws DotHibernateException
     */
    public Tag saveTag ( Tag tag ) throws Exception;

    public TagInode saveTagInode ( TagInode tagInode ) throws Exception;

    public void updateTag ( Tag tag ) throws DotDataException, DotCacheException;

    /**
     * Deletes a tag
     *
     * @param tag tag to be deleted
     * @throws DotHibernateException
     */
    public void deleteTag ( Tag tag ) throws DotDataException, DotCacheException;

    /**
     * Gets all the tags created, with the respective owner and permission information
     * @param userId id of the user that searches the tag
     * @return a complete list of all the tags, with the owner information and the respective permission
     * information
     */
    public List getAllTag ( String userId );

    /**
     * Gets all tags associated to an object
     * @param inode inode of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
    public List<TagInode> getTagInodeByInode ( String inode );

    /**
     * Gets all tags associated to an object
     * @param tagId tagId of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
    public List<TagInode> getTagInodeByTagId ( String tagId );

    /**
     * Gets a tagInode by name and inode
     *
     * @param tagId id of the tag
     * @param inode inode of the object tagged
     * @return the tagInode
     */
    public TagInode getTagInode ( String tagId, String inode ) throws DotHibernateException;

    /**
     * Deletes a TagInode
     *
     * @param tagInode TagInode to delete
     */
    public void deleteTagInode ( TagInode tagInode ) throws DotHibernateException;

    /**
     * Escape a single quote
     *
     * @param tagName string with single quotes
     * @return single quote string escaped
     */
    public String escapeSingleQuote ( String tagName );

    /**
     * Gets all the tags given a user List
     * @param userIds the user id's associated with the tags
     * @return a complete list of all the tags, with the owner information and the respective permission
     * information
     */
    public List<Tag> getAllTagsForUsers ( List<String> userIds );

    public List<Tag> getTagsByInode ( String inode );

}