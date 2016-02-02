package com.dotmarketing.tag.business;

import com.dotmarketing.beans.Host;
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
     *
     * @return list of all tags created
     */
    public java.util.List<Tag> getAllTags () throws DotHibernateException {
        return tagFactory.getAllTags();
    }

    /**
     * Gets a Tag by name
     *
     * @param name name of the tag to get
     * @return tag
     */
    public java.util.List<Tag> getTagByName ( String name ) throws DotHibernateException {
        return tagFactory.getTagByName(name);
    }

    /**
     * Gets all the tag created by an user
     *
     * @param userId id of the user
     * @return a list of all the tags created
     */
    public java.util.List<Tag> getTagByUser ( String userId ) {
        return tagFactory.getTagByUser(userId);
    }

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
    public java.util.List<Tag> getFilteredTags ( String tagName, String hostFilter, boolean globalTagsFilter, String sort, int start, int count ) {
        return tagFactory.getFilteredTags(tagName, hostFilter, globalTagsFilter, sort, start, count);
    }

    /**
     * Gets a Tag by name, validates the existence of the tag, if it doesn't exists then is created
     *
     * @param name   name of the tag to get
     * @param userId owner of the tag
     * @param hostId
     * @return tag
     */
    public Tag getTagAndCreate ( String name, String userId, String hostId ) throws DotDataException, DotSecurityException {

        Tag newTag = new Tag();

        //Search for tags with this given name
        List<Tag> foundTags = tagFactory.getTagByName(name);

        // if doesn't exists then the tag is created
        if ( foundTags == null || foundTags.size() == 0 ) {
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
            for ( Tag tag : foundTags ) {

                if ( isGlobalTag(tag) ) {
                    newTag = tag;
                    globalTagExists = true;
                }
                if ( tag.getHostId().equals(existHostId) ) {
                    newTag = tag;
                    tagExists = true;
                }
            }

            if ( !globalTagExists ) {
                //if global doesn't exist, then save the tag and after it checks if it was stored as a global tag
                try {
                    if ( !tagExists )
                        newTag = saveTag(name, userId, hostId);

                    if ( newTag.getHostId().equals(Host.SYSTEM_HOST) ) {
                        //move references of non-global tags to new global tag and delete duplicate non global tags
                        for ( Tag tag : foundTags ) {
                            List<TagInode> tagInodes = getTagInodeByTagId(tag.getTagId());
                            for ( TagInode tagInode : tagInodes ) {
                                tagFactory.updateTagInode(tagInode, newTag.getTagId());
                            }
                            deleteTag(tag);
                        }
                    }
                } catch ( Exception e ) {
                    Logger.warn(this, "There was an error saving the tag. There's already a tag for selected host");
                    //return existent tag for selected host
                }
            }
        }
        // returning tag
        return newTag;
    }

    /**
     * Gets a Tag by a tagId retrieved from a TagInode.
     *
     * @param tagId the tag id to get
     * @return tag
     * @throws DotHibernateException
     */
    public Tag getTagByTagId ( String tagId ) throws DotHibernateException {
        return tagFactory.getTagByTagId(tagId);
    }

    public Tag getTagByNameAndHost ( String name, String hostId ) throws DotHibernateException {
        return tagFactory.getTagByNameAndHost(name, hostId);
    }

    public Tag saveTag ( String tagName, String userId, String hostId ) throws DotHibernateException {
        return saveTag(tagName, userId, hostId, false);
    }

    /**
     * Creates a new tag
     *
     * @param tagName name of the new tag
     * @param userId  owner of the new tag
     * @param hostId
     * @param persona
     * @return new tag created
     * @throws DotHibernateException
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
     *
     * @param tagName tag(s) to create
     * @param userId  owner of the tag
     * @param inode   object to tag
     * @return a list of all tags assigned to an object
     * @deprecated it doesn't handle host id. Call getTagsInText then addTagInode on each
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
        return getTagInodeByInode(inode);
    }

    public void updateTag ( String tagId, String tagName ) throws DotHibernateException {
        updateTag(tagId, tagName, false, Host.SYSTEM_HOST);
    }

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
     *
     * @param tag tag to be deleted
     * @throws DotHibernateException
     */
    public void deleteTag ( Tag tag ) throws DotHibernateException {
        List<TagInode> tagInodes = getTagInodeByTagId(tag.getTagId());
        for ( TagInode t : tagInodes ) {
            deleteTagInode(t);
        }

        tagFactory.deleteTag(tag);
    }

    /**
     * Deletes a tag
     *
     * @param tagId The id of the tag to delete
     */
    public void deleteTag ( String tagId ) throws DotHibernateException {
        Tag tag = getTagByTagId(tagId);
        deleteTag(tag);
    }

    /**
     * Renames a tag
     *
     * @param tagName    new tag name
     * @param oldTagName current tag name
     * @param userId     owner of the tag
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
     *
     * @param userId id of the user that searches the tag
     * @return a complete list of all the tags, with the owner information and the respective permission
     * information
     */
    public List getAllTag ( String userId ) {
        return tagFactory.getAllTag(userId);
    }

    /**
     * Gets a tagInode and a host identifier, if doesn't exists then the tagInode it's created
     *
     * @param tagName name of the tag
     * @param inode   inode of the object tagged
     * @param hostId  the identifier of host that storage the tag
     * @return a tagInode
     */
    public TagInode addTagInode ( String tagName, String inode, String hostId ) throws DotDataException, DotSecurityException {

        //Ensure the tag exists in the tag table
        Tag existingTag = getTagAndCreate(tagName, "", hostId);

        //Create the the tag inode
        return addTagInode(existingTag, inode);
    }

    /**
     * Gets a tagInode and a host identifier, if doesn't exists then the tagInode it's created
     *
     * @param tag
     * @param inode inode of the object tagged
     * @return a tagInode
     */
    public TagInode addTagInode ( Tag tag, String inode ) throws DotHibernateException {

        //validates the tagInode already exists
        TagInode existingTagInode = getTagInode(tag.getTagId(), inode);

        if ( existingTagInode.getTagId() == null ) {

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
     * Gets all tags associated to an object
     *
     * @param inode inode of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
    public List<TagInode> getTagInodeByInode ( String inode ) throws DotHibernateException {
        return tagFactory.getTagInodeByInode(inode);
    }

    /**
     * Gets all tags associated to an object
     *
     * @param tagId tagId of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
    public List<TagInode> getTagInodeByTagId ( String tagId ) throws DotHibernateException {
        return tagFactory.getTagInodeByTagId(tagId);
    }

    /**
     * Gets a tagInode by name and inode
     *
     * @param tagId id of the tag
     * @param inode inode of the object tagged
     * @return the tagInode
     */
    public TagInode getTagInode ( String tagId, String inode ) throws DotHibernateException {
        return tagFactory.getTagInode(tagId, inode);
    }

    /**
     * Deletes a TagInode
     *
     * @param tagInode TagInode to delete
     */
    public void deleteTagInode ( TagInode tagInode ) throws DotHibernateException {
        tagFactory.deleteTagInode(tagInode);
    }

    /**
     * Deletes an object tag assignment(s)
     *
     * @param tagName name(s) of the tag(s)
     * @param inode   inode of the object tagged
     * @return a list of all tags assigned to an object
     */

    public List deleteTagInode ( String tagName, String inode ) throws DotSecurityException, DotDataException {
        StringTokenizer tagNameToken = new StringTokenizer(tagName, ",");
        if ( tagNameToken.hasMoreTokens() ) {
            for (; tagNameToken.hasMoreTokens(); ) {
                String tagTokenized = tagNameToken.nextToken().trim();
                Tag tag = getTagAndCreate(tagTokenized, "", "");
                TagInode tagInode = getTagInode(tag.getTagId(), inode);
                if ( tagInode.getTagId() != null ) {
                    deleteTagInode(tagInode);
                }
            }
        }
        return getTagInodeByInode(inode);
    }


    /**
     * Escape a single quote
     *
     * @param tagName string with single quotes
     * @return single quote string escaped
     */
    public String escapeSingleQuote ( String tagName ) {
        return tagName.replace("'", "''");
    }

    /**
     * Gets a suggested tag(s), by name
     *
     * @param name name of the tag searched
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
     *
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
     *
     * @param tag
     * @return boolean
     */
    public boolean isGlobalTag ( Tag tag ) {
        if ( tag.getHostId().equals(Host.SYSTEM_HOST) )
            return true;
        else
            return false;
    }

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
                            getTagAndCreate(tag.getTagName(), "", Host.SYSTEM_HOST);
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

    @Override
    public List<Tag> getTagsByInode ( String inode ) throws DotHibernateException {
        return tagFactory.getTagsByInode(inode);
    }

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