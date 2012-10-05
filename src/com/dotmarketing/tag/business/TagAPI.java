package com.dotmarketing.tag.business;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;

public interface TagAPI {


	/**
	 * Get a list of all the tags created
	 * @return list of all tags created
	 */
	public java.util.List<Tag> getAllTags();


	/**
	 * Get a list of all the tags name created
	 * @return list of all tags name created
	 */
	public java.util.List<String> getAllTagsName();


	/**
	 * Gets a Tag by name
	 * @param name name of the tag to get
	 * @return tag
	 */
	public java.util.List<Tag> getTagByName(String name);


	/**
	 * Gets all the tag created by an user
	 * @param userId id of the user
	 * @return a list of all the tags created
	 */
	public java.util.List<Tag> getTagByUser(String userId);

	/**
	 * Gets all tags filtered by tag namd and/or host name
	 * @param tagName tag name
	 * @param hostFilter
	 * @param globalTagsFilter host name
	 * @return a list of tags filtered by tag name or host name
	 */
	public java.util.List<Tag> getFilteredTags(String tagName, String hostFilter, boolean globalTagsFilter, String sort, int start, int count);
	
		

	/**
	 * Gets a Tag by name, validates the existance of the tag, if it doesn't exists then is created
	 * @param name name of the tag to get
	 * @param userId owner of the tag
	 * @return tag
	 * @throws Exception
	 */
	public Tag getTag(String name, String userId, String hostId) throws Exception;


	/**
	 * Gets a Tag by a tagId retrieved from a TagInode.
	 * @param tagId the tag id to get
	 * @return tag
	 */
	public Tag getTagByTagId(String tagId)  throws DotHibernateException ;


	/**
	 * Creates a new tag
	 * @param tagName name of the new tag
	 * @param userId owner of the new tag
	 * @return new tag created
	 * @throws Exception
	 */
    public Tag saveTag(String tagName, String userId, String hostId) throws Exception;


	/**
	 * Tags an object, validates the existence of a tag(s), creates it if it doesn't exists
	 * and then tags the object
	 * @param tagName tag(s) to create
	 * @param userId owner of the tag
	 * @param inode object to tag
	 * @return a list of all tags assigned to an object
	 * @throws Exception
	 */
	public List addTag(String tagName, String userId, String inode) throws Exception;


	/**
	 * Updates an existing tag.
	 * @param tagId tag to update
	 * @param tagName owner of the tag
	 * @param hostId the storage host id
	 * @throws Exception
	 */
	public void updateTag(String tagId, String tagName, boolean updateTagReference, String hostId) throws Exception;


	/**
     * Deletes a tag
     * @param tag tag to be deleted
     */
    public void deleteTag(Tag tag)  throws DotHibernateException ;


    /**
     * Deletes a tag
     * @param tagName name of the tag to be deleted
     * @param userId id of the tag owner
     */
	public void deleteTag(String tagId)  throws DotHibernateException ;


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
	public List getAllTag(String userId);


	/**
	 * Gets a tag with the owner information, searching by name
	 * @param name name of the tag
	 * @return the tag with the owner information
	 */
	public List getTagInfoByName(String name);


	/**
	 * Checks the permission access of an user over an object
	 * @param webAsset object to validates access
	 * @param user user to validate access
	 * @param permission read or write permission to validates
	 * @throws ActionException
	 * @throws DotDataException
	 */
	public void _checkUserPermissions(Inode webAsset, User user,
			int permission) throws ActionException, DotDataException;


	/**
	 * Gets a tagInode and a host identifier, if doesn't exists then the tagInode it's created
	 * @param tagName name of the tag
	 * @param inode inode of the object tagged
	 * @param hostId the identifier of host that storage the tag
	 * @return a tagInode
	 * @throws Exception
	 */

    public TagInode addTagInode(String tagName, String inode, String hostId) throws Exception;

    /**
     * Gets all tagInode associated to an object
     * @param inode inode of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
	public List<TagInode> getTagInodeByInode(String inode);
	
	/**
	 * Gets all tags associated to an object
	 * 
	 * @param inode
	 * @return
	 */
	public List<Tag> getTagsByInode(String inode);

    /**
     * Gets all tags associated to an object
     * @param tagId tagId of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
	public List getTagInodeByTagId(String tagId);


	/**
	 * Gets a tagInode by name and inode
	 * @param name name of the tag
	 * @param inode inode of the object tagged
	 * @return the tagInode
	 */
	public TagInode getTagInode(String name, String inode)  throws DotHibernateException ;


	/**
	 * Deletes a TagInode
	 * @param tagInode TagInode to delete
	 */
	public void deleteTagInode(TagInode tagInode)  throws DotHibernateException ;


	/**
	 * Deletes an object tag assignment(s)
	 * @param tagName name(s) of the tag(s)
	 * @param inode inode of the object tagged
	 * @return a list of all tags assigned to an object
	 * @throws Exception
	 */
	public List deleteTagInode(String tagName, String inode) throws Exception;

	/**
	 * Escape a single quote
	 * @param tagName string with single quotes
	 * @return single quote string escaped
	 */
	public String escapeSingleQuote(String tagName);


	/**
	 * Gets a suggested tag(s), by name
	 * @param req
	 * @param name name of the tag searched
	 * @param selectedHostId
	 * @return list of suggested tags
	 */
	public List<Tag> getSuggestedTag(HttpServletRequest req, String name, String selectedHostId);


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
}
