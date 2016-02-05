package com.dotmarketing.tag.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;

import java.util.List;

public interface TagAPI {


	/**
	 * Get a list of all the tags created
	 * @return list of all tags created
	 */
	public java.util.List<Tag> getAllTags () throws DotDataException;

	/**
	 * Gets a Tag by name
	 * @param name name of the tag to get
	 * @return tag
	 */
	public java.util.List<Tag> getTagsByName ( String name ) throws DotDataException;

	/**
	 * Gets a Tag by a tagId retrieved from a TagInode.
	 *
	 * @param tagId the tag id to get
	 * @return tag
	 */
	public Tag getTagByTagId ( String tagId ) throws DotDataException;

	/**
	 * Get the tags seaching by Tag Name and Host identifier 
	 * @param name Tag name
	 * @param hostId Host identifier
	 * @return Tag
	 * @throws DotDataException
	 */
	public Tag getTagByNameAndHost ( String name, String hostId ) throws DotDataException;

	/**
	 * Get the list of tags related to a user by the user Id
	 * @param userId User id
	 * @return List<Tag>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public java.util.List<Tag> getTagsForUserByUserId ( String userId ) throws DotDataException, DotSecurityException;

	/**
	 * Get the list of tags by the users TagInode inode
	 * @param userInode Users TagInode inode
	 * @return List<Tag>
	 * @throws DotDataException
	 */
	public java.util.List<Tag> getTagsForUserByUserInode ( String userInode ) throws DotDataException;

	/**
	 * Gets all tags filtered by tag name and/or host name paginated
	 * @param tagName tag name
	 * @param hostFilter host name
	 * @param globalTagsFilter 
	 * @param sort Tag field to order
	 * @param start first entry to get
	 * @param count max amount of entries to show
	 * @return List<Tag>
	 */
	public java.util.List<Tag> getFilteredTags(String tagName, String hostFilter, boolean globalTagsFilter, String sort, int start, int count);

	/**
	 * Gets a Tag by name, validates the existance of the tag, if it doesn't exists then is created
	 * @param name name of the tag to get
	 * @param userId owner of the tag
	 * @param hostId host identifier
	 * @return Tag
	 * @throws Exception
	 */
	public Tag getTagAndCreate ( String name, String userId, String hostId ) throws DotDataException, DotSecurityException;

	/**
	 * Gets a Tag by name, validates the existance of the tag, if it doesn't exists then is created
	 *
	 * @param name    name of the tag to get
	 * @param hostId  host identifier
	 * @param persona True if is a persona key tag
	 * @return Tag
	 * @throws Exception
	 */
	public Tag getTagAndCreate ( String name, String hostId, boolean persona ) throws DotDataException, DotSecurityException;

	/**
	 * Gets a Tag by name, validates the existance of the tag, if it doesn't exists then is created
	 *
	 * @param name    name of the tag to get
	 * @param userId  owner of the tag
	 * @param hostId  host identifier
	 * @param persona True if is a persona key tag
	 * @return Tag
	 * @throws Exception
	 */
	public Tag getTagAndCreate ( String name, String userId, String hostId, boolean persona ) throws DotDataException, DotSecurityException;

	/**
	 * Gets a Tag by name, validates the existance of the tag, if it doesn't exists then is created
	 * @param name name of the tag to get
	 * @param hostId host identifier
	 * @return Tag
	 * @throws Exception
	 */
	public Tag getTagAndCreate ( String name, String hostId ) throws DotDataException, DotSecurityException;

	/**
	 * Creates a new tag
	 * @param tagName name of the new tag
	 * @param userId owner of the new tag
	 * @param hostId host identifier
	 * @return Tag
	 * @throws Exception
	 */
	public Tag saveTag ( String tagName, String userId, String hostId ) throws DotDataException;

	/**
	 * Creates a new tag
	 * @param tagName name of the new tag
	 * @param userId owner of the new tag
	 * @param hostId host identifier
	 * @param persona indicate if a persona tag
	 * @return Tag
	 * @throws Exception
	 */
	public Tag saveTag ( String tagName, String userId, String hostId, boolean persona ) throws DotDataException;


	/**
	 * Tags an object, validates the existence of a tag(s), creates it if it doesn't exists
	 * and then tags the object
	 * @param tagName tag(s) to create
	 * @param userId owner of the tag
	 * @param inode object to tag
	 * @return a list of all tags assigned to an object
	 * @deprecated it doesn't handle host id. Call getTagsInText then addTagInode on each
	 * @throws Exception
	 */
	public List addTag ( String tagName, String userId, String inode ) throws DotDataException, DotSecurityException;

	/**
	 * Updates an existing tag.
	 * @param tagId tag to update
	 * @param tagName owner of the tag
	 * @throws DotDataException
	 */
	public void updateTag ( String tagId, String tagName ) throws DotDataException;

	/**
	 * Updates an existing tag.
	 * @param tagId tag to update
	 * @param tagName owner of the tag
	 * @param updateTagReference
	 * @param hostId the storage host id
	 * @throws Exception
	 */
	public void updateTag ( String tagId, String tagName, boolean updateTagReference, String hostId ) throws DotDataException;

	/**
	 * Updates the persona attribute of a given tag
	 *
	 * @param tagId
	 * @param enableAsPersona
	 * @throws DotDataException
	 */
	public void enableDisablePersonaTag ( String tagId, boolean enableAsPersona ) throws DotDataException;

	/**
	 * Deletes a tag
	 * @param tag tag to be deleted
	 * @throws DotDataException
	 */
	public void deleteTag ( Tag tag ) throws DotDataException;


    /**
     * Deletes a tag
     * @param tagId tagId of the tag to be deleted
	 * @throws DotDataException
	 */
	public void deleteTag ( String tagId ) throws DotDataException;


	/**
	 * Renames a tag
	 * @param tagName new tag name
	 * @param oldTagName current tag name
	 * @param userId owner of the tag
	 */
	public void editTag(String tagName,String oldTagName, String userId) throws DotDataException;

	/**
	 * Gets a tagInode and a host identifier, if doesn't exists then the tagInode it's created
	 *
	 * @param tagName name of the tag
	 * @param inode   inode of the object tagged
	 * @param hostId  the identifier of host that storage the tag
	 * @return TagInode
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public TagInode addTagInode ( String tagName, String inode, String hostId ) throws DotDataException, DotSecurityException;

	/**
	 * Gets a tagInode and a host identifier, if doesn't exists then the tagInode it's created
	 * @param tag
	 * @param inode inode of the object tagged
	 * @return TagInode
	 * @throws DotDataException
	 */
	public TagInode addTagInode ( Tag tag, String inode ) throws DotDataException;

    /**
	 * Gets all tagInode associated to an object
     * @param inode inode of the object tagged
	 * @return list of all the TagInode where the tags are associated to the object
	 * @throws DotDataException
	 */
	public List<TagInode> getTagInodesByInode ( String inode ) throws DotDataException;
	
	/**
	 * Gets all tags associated to an object
	 * @param inode object inode
	 * @return List<Tag>
	 * @throws DotDataException
	 */
	public List<Tag> getTagsByInode ( String inode ) throws DotDataException;

    /**
	 * Gets all tags associated to an object
	 * @param tagId tagId of the object tagged
	 * @return list of all the TagInode where the tags are associated to the object
	 * @throws DotDataException
	 */
	public List<TagInode> getTagInodesByTagId ( String tagId ) throws DotDataException;


	/**
	 * Gets a tagInode by name and inode
	 * @param tagId id of the tag
	 * @param inode inode of the object tagged
	 * @return the tagInode
	 * @throws DotDataException
	 */
	public TagInode getTagInode ( String tagId, String inode ) throws DotDataException;


	/**
	 * Deletes a TagInode
	 * @param tagInode TagInode to delete
	 * @throws DotDataException
	 */
	public void deleteTagInode ( TagInode tagInode ) throws DotDataException;

	/**
	 * Deletes a TagInode
	 * @param tag Tag related to the object
	 * @param inode Inode of the object tagged
	 * @throws DotDataException
	 */
	public void deleteTagInode ( Tag tag, String inode ) throws DotDataException;

	/**
	 * Removes the relationship between a tag and an inode, ALSO <strong>if the tag does not have more relationships the Tag itself will be remove it.</strong>
	 * @param tagId TagId
	 * @param inode inode of the object tagged
	 * @throws DotDataException
	 */
	public void removeTagRelationAndTagWhenPossible ( String tagId, String inode ) throws DotDataException;

	/**
	 * Deletes an object tag assignment
	 * @param tagName name of the tag
	 * @param inode inode of the object tagged
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public void deleteTagInode ( String tagName, String inode ) throws DotSecurityException, DotDataException;

	/**
	 * Gets a suggested tag(s), by name
	 * @param name name of the tag searched
	 * @param selectedHostId Host identifier
	 * @return list of suggested tags
	 */
	public List<Tag> getSuggestedTag ( String name, String selectedHostId );

	/**
	 * Check if tag is global
	 * @param tag
	 * @return boolean
	 */
	public boolean isGlobalTag(Tag tag);


	/**
	 * Update, copy or move tags if the hosst changes its tag storage
	 * @param oldTagStorageId
	 * @param newTagStorageId
	 */
	public void updateTagReferences ( String hostIdentifier, String oldTagStorageId, String newTagStorageId ) throws DotDataException;

	/**
	 * Extract tag names in the specified text and return the list
	 * of Tag Object found
	 *
	 * @param text   tag name to search
	 * @param hostId Host identifier
	 * @return list of tag found
	 * @throws Exception
	 */
	public List<Tag> getTagsInText ( String text, String hostId ) throws DotSecurityException, DotDataException;

	/**
	 * Extract tag names in the specified text and return the list 
	 * of Tag Object found
	 * 
	 * @param text tag name to search
	 * @param userId User id
	 * @param hostId Host identifier
	 * @return list of tag found
	 * @throws Exception 
	 */
	public List<Tag> getTagsInText ( String text, String userId, String hostId ) throws DotSecurityException, DotDataException;
}
