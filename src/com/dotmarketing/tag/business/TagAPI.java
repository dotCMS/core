package com.dotmarketing.tag.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;

import java.util.List;

public interface TagAPI {


	/**
	 * Get a list of all the tags created
	 * @return list of all tags created
	 */
	public java.util.List<Tag> getAllTags () throws DotHibernateException;

	/**
	 * Gets a Tag by name
	 * @param name name of the tag to get
	 * @return tag
	 */
	public java.util.List<Tag> getTagByName ( String name ) throws DotHibernateException;

	/**
	 * Gets a Tag by a tagId retrieved from a TagInode.
	 *
	 * @param tagId the tag id to get
	 * @return tag
	 */
	public Tag getTagByTagId ( String tagId ) throws DotHibernateException;

	/**
	 * Get the tags seaching by Tag Name and Host identifier 
	 * @param name Tag name
	 * @param hostId Host identifier
	 * @return Tag
	 * @throws DotHibernateException
	 */
	public Tag getTagByNameAndHost ( String name, String hostId ) throws DotHibernateException;

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
	 * @throws DotHibernateException
	 */
	public java.util.List<Tag> getTagsForUserByUserInode ( String userInode ) throws DotHibernateException;

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
	public Tag saveTag ( String tagName, String userId, String hostId ) throws DotHibernateException;

	/**
	 * Creates a new tag
	 * @param tagName name of the new tag
	 * @param userId owner of the new tag
	 * @param hostId host identifier
	 * @param persona indicate if a persona tag
	 * @return Tag
	 * @throws Exception
	 */
	public Tag saveTag ( String tagName, String userId, String hostId, boolean persona ) throws DotHibernateException;


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
	 * @throws DotHibernateException
	 */
	public void updateTag ( String tagId, String tagName ) throws DotHibernateException;

	/**
	 * Updates an existing tag.
	 * @param tagId tag to update
	 * @param tagName owner of the tag
	 * @param updateTagReference
	 * @param hostId the storage host id
	 * @throws Exception
	 */
	public void updateTag ( String tagId, String tagName, boolean updateTagReference, String hostId ) throws DotHibernateException;

	/**
	 * Deletes a tag
	 * @param tag tag to be deleted
	 * @throws DotHibernateException
	 */
	public void deleteTag ( Tag tag ) throws DotHibernateException;


    /**
     * Deletes a tag
     * @param tagId tagId of the tag to be deleted
     * @throws DotHibernateException
     */
	public void deleteTag ( String tagId ) throws DotHibernateException;


	/**
	 * Renames a tag
	 * @param tagName new tag name
	 * @param oldTagName current tag name
	 * @param userId owner of the tag
	 */
	public void editTag(String tagName,String oldTagName, String userId);


	/**
	 * Gets all the tags created, with the respective owner and permission information
	 * @param userId id of the user that searches the tag
	 * @return a complete list of all the tags, with the owner information and the respective permission
	 * information
	 */
	public List<Tag> getAllTag(String userId);

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
	 * @throws DotHibernateException
	 */
	public TagInode addTagInode ( Tag tag, String inode ) throws DotHibernateException;

    /**
	 * Gets all tagInode associated to an object
     * @param inode inode of the object tagged
	 * @return list of all the TagInode where the tags are associated to the object
	 * @throws DotHibernateException
	 */
	public List<TagInode> getTagInodesByInode(String inode) throws DotHibernateException;
	
	/**
	 * Gets all tags associated to an object
	 * @param inode object inode
	 * @return List<Tag>
	 * @throws DotHibernateException
	 */
	public List<Tag> getTagsByInode(String inode) throws DotHibernateException;

    /**
	 * Gets all tags associated to an object
	 * @param tagId tagId of the object tagged
	 * @return list of all the TagInode where the tags are associated to the object
	 * @throws DotHibernateException
	 */
	public List<TagInode> getTagInodesByTagId(String tagId) throws DotHibernateException;


	/**
	 * Gets a tagInode by name and inode
	 * @param name name of the tag
	 * @param inode inode of the object tagged
	 * @return the tagInode
	 * @throws DotHibernateException
	 */
	public TagInode getTagInode ( String name, String inode ) throws DotHibernateException;


	/**
	 * Deletes a TagInode
	 * @param tagInode TagInode to delete
	 * @throws DotHibernateException
	 */
	public void deleteTagInode ( TagInode tagInode ) throws DotHibernateException;

	/**
	 * Deletes a TagInode
	 * @param tag Tag related to the object
	 * @param inode Inode of the object tagged
	 * @throws DotHibernateException
	 */
	public void deleteTagInode ( Tag tag, String inode ) throws DotHibernateException;

	/**
	 * Removes the relationship between a tag and an inode, ALSO <strong>if the tag does not have more relationships the Tag itself will be remove it.</strong>
	 * @param tagId TagId
	 * @param inode inode of the object tagged
	 * @throws DotHibernateException
     */
	public void removeTagRelationAndTagWhenPossible ( String tagId, String inode ) throws DotHibernateException;

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
	 * Gets all the tags given a user List
	 * @param userIds the user id's associated with the tags
	 * @return a complete list of all the tags, with the owner information and the respective permission
	 * information
	 */
	public List<Tag> getAllTagsForUsers(List<String> userIds);


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
	public void updateTagReferences (String hostIdentifier, String oldTagStorageId, String newTagStorageId);

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
