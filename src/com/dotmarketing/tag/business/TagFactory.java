package com.dotmarketing.tag.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;

import java.util.List;

public interface TagFactory {

    /**
     * Get a list of all the tags created
     *
     * @return list of all tags created
     */
    public java.util.List<Tag> getAllTags () throws DotDataException;

    /**
     * Gets a Tag by name
     *
     * @param name name of the tag to get
     * @return tag
     */
    public java.util.List<Tag> getTagsByName ( String name ) throws DotDataException;

    public java.util.List<Tag> getTagsByHost ( String hostId ) throws DotDataException;

    public List<Tag> getTagsLikeNameAndHostIncludingSystemHost ( String name, String hostId ) throws DotDataException;

    public Tag getTagByNameAndHost ( String name, String hostId ) throws DotDataException;

    /**
     * Gets a Tag by a tagId retrieved from a TagInode.
     *
     * @param tagId the tag id to get
     * @return tag
     * @throws DotDataException
     */
    public Tag getTagByTagId ( String tagId ) throws DotDataException;

    /**
     * Gets all the tag created by an user
     *
     * @param userId id of the user
     * @return a list of all the tags created
     */
    public java.util.List<Tag> getTagsForUserByUserInode ( String userInode ) throws DotDataException;

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

    public void updateTagInode ( TagInode tagInode, String tagId ) throws DotDataException;

    /**
     * Creates a new tag
     *
     * @param tagName name of the new tag
     * @param userId  owner of the new tag
     * @param hostId
     * @return new tag created
     * @throws DotDataException
     */
    public Tag createTag ( Tag tag ) throws DotDataException;

    public TagInode createTagInode ( TagInode tagInode ) throws DotDataException;

    public void updateTag ( Tag tag ) throws DotDataException;

    /**
     * Deletes a tag
     *
     * @param tag tag to be deleted
     * @throws DotDataException
     */
    public void deleteTag ( Tag tag ) throws DotDataException;

    /**
     * Gets all tags associated to an object
     * @param inode inode of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
    public List<TagInode> getTagInodesByInode ( String inode ) throws DotDataException;

    /**
     * Gets all tags associated to an object
     * @param tagId tagId of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
    public List<TagInode> getTagInodesByTagId ( String tagId ) throws DotDataException;

    /**
     * Gets a tagInode by name and inode
     *
     * @param tagId id of the tag
     * @param inode inode of the object tagged
     * @return the tagInode
     */
    public TagInode getTagInode ( String tagId, String inode, String fieldVarName ) throws DotDataException;

    /**
     * Deletes a TagInode
     *
     * @param tagInode TagInode to delete
     */
    public void deleteTagInode ( TagInode tagInode ) throws DotDataException;

    public List<Tag> getTagsByInode ( String inode ) throws DotDataException;

}