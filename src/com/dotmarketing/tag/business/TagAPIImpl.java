package com.dotmarketing.tag.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.*;

public class TagAPIImpl implements TagAPI {

    private TagFactory tagFactory = FactoryLocator.getTagFactory();

    /**
	 * Get a list of all the tags created
	 * @return list of all tags created
	 */
    public java.util.List<Tag> getAllTags () throws DotHibernateException {
        return tagFactory.getAllTags();
    }

    /**
	 * Gets a Tag by name
	 * @param name name of the tag to get
	 * @return tag
	 */
    public java.util.List<Tag> getTagByName ( String name ) throws DotHibernateException {
        return tagFactory.getTagByName(name);
    }

    /**
	 * Get the list of tags related to a user by the user Id
	 * @param userId User id
	 * @return List<Tag>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
    public java.util.List<Tag> getTagsForUserByUserId ( String userId ) throws DotDataException, DotSecurityException {

        //First lets seach for the user
        UserProxy user = APILocator.getUserProxyAPI().getUserProxy(userId, APILocator.getUserAPI().getSystemUser(), false);

        //And return the tags related to the user
        return getTagsForUserByUserInode(user.getInode());
    }

    /**
	 * Get the list of tags by the users TagInode inode
	 * @param userInode Users TagInode inode
	 * @return List<Tag>
	 * @throws DotHibernateException
	 */
    public java.util.List<Tag> getTagsForUserByUserInode ( String userInode ) throws DotHibernateException {
        return tagFactory.getTagForUserByUserInode(userInode);
    }

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
    public java.util.List<Tag> getFilteredTags ( String tagName, String hostFilter, boolean globalTagsFilter, String sort, int start, int count ) {
        return tagFactory.getFilteredTags(tagName, hostFilter, globalTagsFilter, sort, start, count);
    }

    /**
	 * Gets a Tag by name, validates the existance of the tag, if it doesn't exists then is created
	 * @param name name of the tag to get
	 * @param hostId host identifier
	 * @return Tag
	 * @throws Exception
	 */
    public Tag getTagAndCreate ( String name, String hostId ) throws DotDataException, DotSecurityException {
        return getTagAndCreate(name, "", hostId);
    }

    /**
	 * Gets a Tag by name, validates the existance of the tag, if it doesn't exists then is created
	 * @param name name of the tag to get
	 * @param userId owner of the tag
	 * @param hostId host identifier
	 * @return Tag
	 * @throws Exception
	 */
    public Tag getTagAndCreate ( String name, String userId, String hostId ) throws DotDataException, DotSecurityException {

        Tag newTag = new Tag();

        //Search for tags with this given name
        Tag tag = tagFactory.getTagByNameAndHost(name, hostId);

        // if doesn't exists then the tag is created
        if ( tag == null || !UtilMethods.isSet(tag.getTagId()) ) {
            // creating tag
            return saveTag(name, userId, hostId);
        } else {

            String existHostId;

            //check if global tag already exists
            boolean globalTagExists = false;

            //check if tag exists with same tag name but for a different host
            boolean tagExists = false;

            Host host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), true);
            if ( host.getMap().get("tagStorage") == null ) {
                existHostId = host.getMap().get("identifier").toString();
            } else {
                existHostId = host.getMap().get("tagStorage").toString();
            }

            if ( isGlobalTag(tag) ) {
                newTag = tag;
                globalTagExists = true;
            }
            if ( tag.getHostId().equals(existHostId) ) {
                newTag = tag;
                tagExists = true;
            }

            if ( !globalTagExists ) {
                //if global doesn't exist, then save the tag and after it checks if it was stored as a global tag
                if ( !tagExists ) {
                    newTag = saveTag(name, userId, hostId);
                }

                if ( newTag.getHostId().equals(Host.SYSTEM_HOST) ) {
                    //move references of non-global tags to new global tag and delete duplicate non global tags
                    List<TagInode> tagInodes = getTagInodesByTagId(tag.getTagId());
                    for ( TagInode tagInode : tagInodes ) {
                        tagFactory.updateTagInode(tagInode, newTag.getTagId());
                    }
                    deleteTag(tag);
                }
            }
        }

        return newTag;
    }

    /**
	 * Gets a Tag by a tagId retrieved from a TagInode.
	 *
	 * @param tagId the tag id to get
	 * @return tag
	 */
    public Tag getTagByTagId ( String tagId ) throws DotHibernateException {
        return tagFactory.getTagByTagId(tagId);
    }

    /**
	 * Get the tags seaching by Tag Name and Host identifier 
	 * @param name Tag name
	 * @param hostId Host identifier
	 * @return Tag
	 * @throws DotHibernateException
	 */
    public Tag getTagByNameAndHost ( String name, String hostId ) throws DotHibernateException {
        return tagFactory.getTagByNameAndHost(name, hostId);
    }

    /**
	 * Creates a new tag
	 * @param tagName name of the new tag
	 * @param userId owner of the new tag
	 * @param hostId host identifier
	 * @return Tag
	 * @throws Exception
	 */
    public Tag saveTag ( String tagName, String userId, String hostId ) throws DotHibernateException {
        return saveTag(tagName, userId, hostId, false);
    }

    /**
	 * Creates a new tag
	 * @param tagName name of the new tag
	 * @param userId owner of the new tag
	 * @param hostId host identifier
	 * @param persona indicate if a persona tag
	 * @return Tag
	 * @throws Exception
	 */
    public Tag saveTag ( String tagName, String userId, String hostId, boolean persona ) throws DotHibernateException {

        Tag tag = new Tag();
        //creates new Tag
        tag.setTagName(tagName.toLowerCase());
        tag.setUserId(userId);
        tag.setPersona(persona);
        tag.setModDate(new Date());

        Host host = null;

        if ( UtilMethods.isSet(hostId) && !hostId.equals(Host.SYSTEM_HOST) ) {
            try {
                if ( !UtilMethods.isSet(hostId) ) {
                    host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true);
                } else {
                    host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), true);
                }
            } catch ( Exception e ) {
                Logger.error(this, "Unable to load host.");
            }

            if ( host.getMap().get("tagStorage") == null ) {
                hostId = host.getMap().get("identifier").toString();
            } else {
                hostId = host.getMap().get("tagStorage").toString();
            }

    		/*try {
                hostId=host.getMap().get("tagStorage").toString();
    		} catch(NullPointerException e) {
    			hostId = Host.SYSTEM_HOST;
    			Logger.info(this, "No tag storage for Host, chosing global");
    		}*/

        } else {
            hostId = Host.SYSTEM_HOST;
        }
        tag.setHostId(hostId);

        return tagFactory.saveTag(tag);
    }

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
    public List addTag ( String tagName, String userId, String inode ) throws DotDataException, DotSecurityException {
        StringTokenizer tagNameToken = new StringTokenizer(tagName, ",");
        if ( tagNameToken.hasMoreTokens() ) {
            for (; tagNameToken.hasMoreTokens(); ) {
                String tagTokenized = tagNameToken.nextToken().trim();
                Tag createdTag = getTagAndCreate(tagTokenized, userId, "");
                addTagInode(createdTag, inode);
            }
        }
        return getTagInodesByInode(inode);
    }

    /**
	 * Updates an existing tag.
	 * @param tagId tag to update
	 * @param tagName owner of the tag
	 * @throws DotHibernateException
	 */
    public void updateTag ( String tagId, String tagName ) throws DotHibernateException {
        updateTag(tagId, tagName, false, Host.SYSTEM_HOST);
    }

    /**
	 * Updates an existing tag.
	 * @param tagId tag to update
	 * @param tagName owner of the tag
	 * @param updateTagReference
	 * @param hostId the storage host id
	 * @throws Exception
	 */
    public void updateTag ( String tagId, String tagName, boolean updateTagReference, String hostId ) throws DotHibernateException {

        Tag tag = getTagByTagId(tagId);
        boolean tagAlreadyExistsForNewTagStorage = false;

        //This block of code prevent saving duplicated tags when editing tag storage from host
        List<Tag> tags = getTagByName(tagName);

        for ( Tag t : tags ) {
            if ( t.getHostId().equals(hostId) ) {
                //The tag with new tag storage already exists
                tagAlreadyExistsForNewTagStorage = true;
            }
            if ( t.getTagId().equals(tagId) ) {
                //select tag to be updated
                tag = t;
            }
        }

        //update selected tag if it's set and if previous tag storage is different.
        if ( UtilMethods.isSet(tag.getTagId()) && !tagAlreadyExistsForNewTagStorage ) {
            tag.setTagName(tagName.toLowerCase());
            tag.setUserId("");
            if ( updateTagReference ) {
                if ( UtilMethods.isSet(hostId) )
                    tag.setHostId(hostId);
            }

            tag.setModDate(new Date());
            tagFactory.updateTag(tag);
        }

    }

    /**
	 * Deletes a tag
	 * @param tag tag to be deleted
	 * @throws DotHibernateException
	 */
    public void deleteTag ( Tag tag ) throws DotHibernateException {
        List<TagInode> tagInodes = getTagInodesByTagId(tag.getTagId());
        for ( TagInode t : tagInodes ) {
            deleteTagInode(t);
        }

        tagFactory.deleteTag(tag);
    }

    /**
     * Deletes a tag
     * @param tagId tagId of the tag to be deleted
     * @throws DotHibernateException
     */
    public void deleteTag ( String tagId ) throws DotHibernateException {
        Tag tag = getTagByTagId(tagId);
        deleteTag(tag);
    }

    /**
	 * Renames a tag
	 * @param tagName new tag name
	 * @param oldTagName current tag name
	 * @param userId owner of the tag
	 */
    public void editTag ( String tagName, String oldTagName, String userId ) {
        try {
            tagName = escapeSingleQuote(tagName);
            oldTagName = escapeSingleQuote(oldTagName);

            List tagToEdit = getTagByName(oldTagName);
            Iterator it = tagToEdit.iterator();
            for ( int i = 0; it.hasNext(); i++ ) {
                Tag tag = (Tag) it.next();

                tag.setTagName(tagName.toLowerCase());
                tag.setModDate(new Date());

                tagFactory.updateTag(tag);
            }
        } catch ( Exception e ) {
            Logger.error(e, "Error editing Tag");
        }
    }

    /**
	 * Gets all the tags created, with the respective owner and permission information
	 * @param userId id of the user that searches the tag
	 * @return a complete list of all the tags, with the owner information and the respective permission
	 * information
	 */
    public List<Tag> getAllTag ( String userId ) {
        return tagFactory.getAllTag(userId);
    }

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
    public TagInode addTagInode ( String tagName, String inode, String hostId ) throws DotDataException, DotSecurityException {

        //Ensure the tag exists in the tag table
        Tag existingTag = getTagAndCreate(tagName, "", hostId);

        //Create the the tag inode
        return addTagInode(existingTag, inode);
    }

    /**
	 * Gets a tagInode and a host identifier, if doesn't exists then the tagInode it's created
	 * @param tag
	 * @param inode inode of the object tagged
	 * @return TagInode
	 * @throws DotHibernateException
	 */
    public TagInode addTagInode ( Tag tag, String inode ) throws DotHibernateException {

        //validates the tagInode already exists
        TagInode existingTagInode = getTagInode(tag.getTagId(), inode);

        if ( existingTagInode == null || existingTagInode.getTagId() == null ) {

            //the tagInode does not exists, so creates a new TagInode
            TagInode tagInode = new TagInode();
            tagInode.setTagId(tag.getTagId());
            tagInode.setInode(inode);
            tagInode.setModDate(new Date());

            return tagFactory.saveTagInode(tagInode);
        } else {
            // returning the existing tagInode
            return existingTagInode;
        }
    }

    /**
	 * Gets all tagInode associated to an object
     * @param inode inode of the object tagged
	 * @return list of all the TagInode where the tags are associated to the object
	 * @throws DotHibernateException
	 */
    public List<TagInode> getTagInodesByInode ( String inode ) throws DotHibernateException {
        return tagFactory.getTagInodeByInode(inode);
    }

    /**
	 * Gets all tags associated to an object
	 * @param tagId tagId of the object tagged
	 * @return list of all the TagInode where the tags are associated to the object
	 * @throws DotHibernateException
	 */
    public List<TagInode> getTagInodesByTagId ( String tagId ) throws DotHibernateException {
        return tagFactory.getTagInodeByTagId(tagId);
    }

    /**
	 * Gets a tagInode by name and inode
	 * @param name name of the tag
	 * @param inode inode of the object tagged
	 * @return the tagInode
	 * @throws DotHibernateException
	 */
    public TagInode getTagInode ( String tagId, String inode ) throws DotHibernateException {
        return tagFactory.getTagInode(tagId, inode);
    }

    /**
	 * Deletes a TagInode
	 * @param tagInode TagInode to delete
	 * @throws DotHibernateException
	 */
    public void deleteTagInode ( TagInode tagInode ) throws DotHibernateException {
        tagFactory.deleteTagInode(tagInode);
    }

    /**
	 * Removes the relationship between a tag and an inode, ALSO <strong>if the tag does not have more relationships the Tag itself will be remove it.</strong>
	 * @param tagId TagId
	 * @param inode inode of the object tagged
	 * @throws DotHibernateException
     */
    public void removeTagRelationAndTagWhenPossible ( String tagId, String inode ) throws DotHibernateException {

        Boolean existRelationship = false;
        //Get the tag inode we want to remove
        TagInode tagInodeToRemove = tagFactory.getTagInode(tagId, inode);
        if ( UtilMethods.isSet(tagInodeToRemove) && UtilMethods.isSet(tagInodeToRemove.getTagId()) ) {
            existRelationship = true;
        }

        //Get the tag we want to remove
        Tag tagToRemove = tagFactory.getTagByTagId(tagId);

        //First lets search for the relationships of this tag
        List<TagInode> tagInodes = APILocator.getTagAPI().getTagInodesByTagId(tagId);

        if ( tagInodes != null && !tagInodes.isEmpty() ) {

            if ( !existRelationship && tagInodes.size() > 0 ) {
                //This mean we can NOT remove the tag as it is still related with other inodes
                return;
            } else if ( existRelationship && tagInodes.size() > 1 ) {//Current relation and more??

                //Delete the tag inode relationship
                tagFactory.deleteTagInode(tagInodeToRemove);

                //And this mean we can NOT remove the tag as it is still related with other inodes
                return;
            }
        }

        if ( existRelationship ) {
            //Delete the tag inode relationship
            tagFactory.deleteTagInode(tagInodeToRemove);
        }

        //If this tag has not relationships remove it
        if ( UtilMethods.isSet(tagToRemove) && UtilMethods.isSet(tagToRemove.getTagId()) ) {
            tagFactory.deleteTag(tagToRemove);
        }

    }

    /**
	 * Deletes a TagInode
	 * @param tag Tag related to the object
	 * @param inode Inode of the object tagged
	 * @throws DotHibernateException
	 */
    public void deleteTagInode ( Tag tag, String inode ) throws DotHibernateException {

        TagInode tagInode = getTagInode(tag.getTagId(), inode);
        if ( tagInode != null && UtilMethods.isSet(tagInode.getTagId()) ) {
            deleteTagInode(tagInode);
        }
    }

    /**
	 * Deletes an object tag assignment
	 * @param tagName name of the tag
	 * @param inode inode of the object tagged
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
    public void deleteTagInode ( String tagName, String inode ) throws DotSecurityException, DotDataException {

        StringTokenizer tagNameToken = new StringTokenizer(tagName, ",");
        if ( tagNameToken.hasMoreTokens() ) {

            for (; tagNameToken.hasMoreTokens(); ) {

                String tagTokenized = tagNameToken.nextToken().trim();

                //Find the tag
                Tag tag = getTagByNameAndHost(tagTokenized, Host.SYSTEM_HOST);
                //Delete the related tag inode
                if ( tag != null && UtilMethods.isSet(tag.getTagId()) ) {
                    deleteTagInode(tag, inode);
                }
            }
        }
    }

    /**
     * Escape a single quote
     *
     * @param tagName string with single quotes
     * @return single quote string escaped
     */
    private String escapeSingleQuote ( String tagName ) {
        return tagName.replace("'", "''");
    }

    /**
	 * Gets a suggested tag(s), by name
	 * @param name name of the tag searched
	 * @param selectedHostId Host identifier
	 * @return list of suggested tags
	 */
    @SuppressWarnings ( "unchecked" )
    public List<Tag> getSuggestedTag ( String name, String selectedHostId ) {
        try {
            name = escapeSingleQuote(name);

            //if there's a host field on form, retrieve it
            Host hostOnForm;
            if ( UtilMethods.isSet(selectedHostId) ) {
                try {
                    hostOnForm = APILocator.getHostAPI().find(selectedHostId, APILocator.getUserAPI().getSystemUser(), true);
                    selectedHostId = hostOnForm.getMap().get("tagStorage").toString();
                } catch ( Exception e ) {
                    Logger.error(this, "Unable to load current host.");
                }
            }

            return tagFactory.getTagsLikeNameAndHostIncludingSystemHost(name, selectedHostId);
        } catch ( Exception e ) {
            Logger.error(e, "Error retrieving suggested tags");
        }
        return new ArrayList<>();
    }

    /**
	 * Gets all the tags given a user List
	 * @param userIds the user id's associated with the tags
	 * @return a complete list of all the tags, with the owner information and the respective permission
	 * information
	 */
    @SuppressWarnings ( "unchecked" )
    public List<Tag> getAllTagsForUsers ( List<String> userIds ) {
        return tagFactory.getAllTagsForUsers(userIds);
    }

    /**
	 * Check if tag is global
	 * @param tag
	 * @return boolean
	 */
    public boolean isGlobalTag ( Tag tag ) {
        if ( tag.getHostId().equals(Host.SYSTEM_HOST) )
            return true;
        else
            return false;
    }

    /**
	 * Update, copy or move tags if the hosst changes its tag storage
	 * @param oldTagStorageId
	 * @param newTagStorageId
	 */
    public void updateTagReferences ( String hostIdentifier, String oldTagStorageId, String newTagStorageId ) {

        try {
            if ( !oldTagStorageId.equals(Host.SYSTEM_HOST) && !oldTagStorageId.equals(newTagStorageId) ) {
                //copy or update tags if the tag storage id has changed when editing the host
                //or if the previous tag storage was global
                List<Tag> list = tagFactory.getTagByHost(oldTagStorageId);

                List<Tag> hostTagList = tagFactory.getTagByHost(hostIdentifier);

                for ( Tag tag : list ) {
                    try {
                        if ( (hostIdentifier.equals(newTagStorageId) && hostTagList.size() == 0) && !newTagStorageId.equals(Host.SYSTEM_HOST) ) {
                            //copy old tag to host with new tag storage
                            saveTag(tag.getTagName(), "", hostIdentifier);
                        } else if ( newTagStorageId.equals(Host.SYSTEM_HOST) ) {
                            //update old tag to global tags
                            getTagAndCreate(tag.getTagName(), Host.SYSTEM_HOST);
                        } else if ( hostIdentifier.equals(newTagStorageId) && hostTagList.size() > 0 || hostIdentifier.equals(oldTagStorageId) ) {
                            // update old tag with new tag storage
                            updateTag(tag.getTagId(), tag.getTagName(), true, newTagStorageId);
                        }

                    } catch ( Exception e ) {
                        Logger.error(e, "Error updating Tag references");
                    }
                }
            }
        } catch ( Exception e ) {
            Logger.error(e, "Error updating Tag references");
        }

    }

    /**
	 * Gets all tags associated to an object
	 * @param inode object inode
	 * @return List<Tag>
	 * @throws DotHibernateException
	 */
    @Override
    public List<Tag> getTagsByInode ( String inode ) throws DotHibernateException {
        return tagFactory.getTagsByInode(inode);
    }

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
    @Override
    public List<Tag> getTagsInText ( String text, String userId, String hostId ) throws DotSecurityException, DotDataException {
        List<Tag> tags = new ArrayList<>();
        String[] tagNames = text.split("[,\\n\\t\\r]");
        for ( String tagname : tagNames ) {
            tagname = tagname.trim();
            if ( tagname.length() > 0 )
                tags.add(getTagAndCreate(tagname, userId, hostId));
        }
        return tags;
    }

}