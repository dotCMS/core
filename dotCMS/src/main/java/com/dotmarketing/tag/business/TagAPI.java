package com.dotmarketing.tag.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Set;

/**
 * Provides access to all the operations related to the handling of Tags in dotCMS.
 * <p>Tags are a method of labeling content with one or more terms so that content can be found and extracted
 * dynamically for display on a page. Tags may be single words, or phrases of multiple words separated by spaces.</p>
 *
 * @author root
 * @since Mar 22, 2012
 */
public interface TagAPI {

	/**
	 * Get a list of top the tags for a given contentlet
	 * @param siteId
	 * @return list of tags
	 */
	Set<String> findTopTags(final String siteId) throws DotDataException;
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
	 * Gets the count of tags filtered by tag name and/or host name
	 * @param tagName tag name filter
	 * @param hostFilter host name or ID filter
	 * @param globalTagsFilter include global tags
	 * @return count of filtered tags
	 * @throws DotDataException
	 */
	public long getFilteredTagsCount(String tagName, String hostFilter, boolean globalTagsFilter) throws DotDataException;

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
	 * @param name               name of the tag to get
	 * @param userId             owner of the tag
	 * @param siteId             Site identifier
	 * @param persona            True if is a persona key tag
	 * @param searchInSystemHost True if we want to search in the system host before to decide if a tag with the given
	 *                           name exist or not
	 * @return Tag
	 * @throws Exception
	 */
	public Tag getTagAndCreate(String name, String userId, String siteId, boolean persona, boolean searchInSystemHost) throws DotDataException, DotSecurityException;

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
	 * and then tags the User
	 *
	 * @param tagName tag(s) to create
	 * @param userId  owner of the tag
	 * @param inode   User to tag
	 * @return a list of all tags assigned to an object
	 * @throws Exception
	 * @deprecated it doesn't handle host id. Call getTagsInText then addUserTagInode on each
	 */
	public List addUserTag(String tagName, String userId, String inode) throws DotDataException, DotSecurityException;

	/**
	 * Tags an object, validates the existence of a tag(s), creates it if it doesn't exists
	 * and then tags the Contentlet
	 *
	 * @param tagName      tag(s) to create
	 * @param userId       owner of the tag
	 * @param inode        Contenlet to tag
	 * @param fieldVarName var name of the tag field related to the given Contentlet inode
	 * @return a list of all tags assigned to an object
	 * @throws Exception
	 * @deprecated it doesn't handle host id. Call getTagsInText then addContentletTagInode on each
	 */
	public List addContentleTag(String tagName, String userId, String inode, String fieldVarName) throws DotDataException, DotSecurityException;

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
	 * Deletes multiple tags in a single batch operation
	 * @param tagIds tag IDs of the tags to be deleted
	 * @throws DotDataException
	 */
	public void deleteTags ( String... tagIds ) throws DotDataException;

	/**
	 * Checks if a user has permission to delete a tag based on contentlet associations.
	 * <p>If the tag has no contentlet associations (orphan tag), deletion is allowed.
	 * If the tag is associated with contentlets, the user must have EDIT permission on ALL of them.</p>
	 *
	 * @param user  the user to check permissions for
	 * @param tagId tagId of the tag to check
	 * @return null if deletion is allowed, or an error message explaining why deletion is denied
	 * @throws DotDataException if there's a data access error
	 */
	public String canDeleteTag(User user, String tagId) throws DotDataException;

	/**
	 * Deletes a tag after verifying the user has EDIT permission on all associated contentlets.
	 * <p>If the tag has no contentlet associations (orphan tag), deletion is allowed.
	 * If the tag is associated with contentlets, the user must have EDIT permission on ALL of them.</p>
	 *
	 * @param user  the user requesting the deletion (used for permission checks)
	 * @param tagId tagId of the tag to be deleted
	 * @throws DotDataException     if there's a data access error
	 * @throws DotSecurityException if the user lacks EDIT permission on any associated contentlet
	 */
	public void deleteTag(User user, String tagId) throws DotDataException, DotSecurityException;

	/**
	 * Renames a tag
	 * @param tagName new tag name
	 * @param oldTagName current tag name
	 * @param userId owner of the tag
	 */
	public void editTag(String tagName,String oldTagName, String userId) throws DotDataException;

	/**
	 * Creates the TagInode relationship between a given tag name and a given User inode.
	 * <br><strong>Note: If a tag with the given tag name does not exist a Tag with that name will be created.</strong>
	 *
	 * @param tagName      Tag name of the tag to relate with the Contentlet inode
	 * @param inode        inode of the object tagged
	 * @param hostId       Host id where the tag name must be found
	 * @return TagInode
	 * @throws DotDataException
	 */
	public TagInode addUserTagInode(String tagName, String inode, String hostId) throws DotDataException, DotSecurityException;

	/**
	 * Creates the TagInode relationship between a given tag name and a given Contentlet inode.
	 * <br><strong>Note: If a tag with the given tag name does not exist a Tag with that name will be created.</strong>
	 *
	 * @param tagName      Tag name of the tag to relate with the Contentlet inode
	 * @param inode        inode of the object tagged
	 * @param hostId       Host id where the tag name must be found
	 * @param fieldVarName var name of the tag field related to the given Contentlet inode
	 * @return TagInode
	 * @throws DotDataException
	 */
	public TagInode addContentletTagInode(String tagName, String inode, String hostId, String fieldVarName) throws DotDataException, DotSecurityException;

	/**
	 * Creates the TagInode relationship between a given tag and a given User inode
	 *
	 * @param tag
	 * @param inode inode of the object tagged
	 * @return TagInode
	 * @throws DotDataException
	 */
	public TagInode addUserTagInode(Tag tag, String inode) throws DotDataException;

	/**
	 * Creates the TagInode relationship between a given tag and a given Contentlet inode
	 * @param tag Tag to relate with the Contentlet inode
	 * @param inode inode of the object tagged
	 * @param fieldVarName var name of the tag field related to the given Contentlet inode
	 * @return TagInode
	 * @throws DotDataException
	 */
	public TagInode addContentletTagInode(Tag tag, String inode, String fieldVarName) throws DotDataException;

    /**
	 * Gets all tagInode associated to an object
     * @param inode inode of the object tagged
	 * @return list of all the TagInode where the tags are associated to the object
	 * @throws DotDataException
	 */
	public List<TagInode> getTagInodesByInode ( String inode ) throws DotDataException;

	/**
	 * Gets all tags associated to given inode and field var name
	 * @param inode inode of the object tagged
	 * @param fieldVarName velocity var name of a field
	 * @return a list with all the tags associated with the given inode and field var name
	 * @throws DotDataException
	 */
	List<Tag> getTagsByInodeAndFieldVarName(String inode, String fieldVarName) throws DotDataException;
	
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
	 * @param fieldVarName var name of the tag field related to the inode if the inode belongs to a Contentlet otherwise
	 *                     send null
	 * @return the tagInode
	 * @throws DotDataException
	 */
	public TagInode getTagInode ( String tagId, String inode, String fieldVarName ) throws DotDataException;

	/**
	 * Deletes a TagInode
	 * @param tagInode TagInode to delete
	 * @throws DotDataException
	 */
	public void deleteTagInode ( TagInode tagInode ) throws DotDataException;

	/**
	 * Deletes TagInodes references by inode
	 *
	 * @param inode inode reference to delete
	 * @throws DotDataException
	 */
	public void deleteTagInodesByInode(String inode) throws DotDataException;

	/**
	 * Deletes TagInodes references by tag id
	 *
	 * @param tagId tag reference to delete
	 * @throws DotDataException
	 */
	public void deleteTagInodesByTagId(String tagId) throws DotDataException;

	/**
	 * Deletes associated to given inode and field var name
	 * @param inode inode of the object tagged
	 * @param fieldVarName velocity var name of a field
	 * @throws DotDataException
	 */
    public void deleteTagInodesByInodeAndFieldVarName(String inode, String fieldVarName) throws DotDataException;

	/**
	 * Deletes a TagInode
	 * @param tag Tag related to the object
	 * @param inode Inode of the object tagged
	 * @param fieldVarName var name of the tag field related to the inode if the inode belongs to a Contentlet otherwise
	 *                     send null
	 * @throws DotDataException
	 */
	public void deleteTagInode ( Tag tag, String inode, String fieldVarName ) throws DotDataException;

	/**
	 * Removes the relationship between a tag and an inode, ALSO <strong>if the tag does not have more relationships the Tag itself will be remove it.</strong>
	 * @param tagId TagId
	 * @param inode inode of the object tagged
	 * @param fieldVarName var name of the tag field related to the inode if the inode belongs to a Contentlet otherwise
	 *                     send null
	 * @throws DotDataException
	 */
	public void removeTagRelationAndTagWhenPossible ( String tagId, String inode, String fieldVarName ) throws DotDataException;

	/**
	 * Deletes an object tag assignment
	 *
	 * @param tagName name of the tag
	 * @param inode   inode of the object tagged
	 * @param fieldVarName var name of the tag field related to the inode if the inode belongs to a Contentlet otherwise
	 *                     send null
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public void deleteTagInode ( String tagName, String inode, String fieldVarName ) throws DotSecurityException, DotDataException;

	/**
	 * Gets a suggested tag(s), by name
	 * @param name name of the tag searched
	 * @param selectedHostId Host identifier
	 * @return list of suggested tags
	 */
	public List<Tag> getSuggestedTag(String name, String selectedHostId) throws DotDataException;

	/**
	 * Update, copy or move tags if the hosst changes its tag storage
	 *
	 * @param hostIdentifier
	 * @param oldTagStorageId
	 * @param newTagStorageId
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void updateTagReferences(String hostIdentifier, String oldTagStorageId, String newTagStorageId) throws DotDataException, DotSecurityException;

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

	public List<Tag> getTagsByHostId(final String hostId) throws DotDataException;

	public void deleteTagsByHostId(final String hostId) throws DotDataException;

}
