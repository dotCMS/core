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

    public java.util.List<Tag> getAllTags () throws DotHibernateException {
        return tagFactory.getAllTags();
    }

    public java.util.List<Tag> getTagByName ( String name ) throws DotHibernateException {
        return tagFactory.getTagByName(name);
    }

    public java.util.List<Tag> getTagsForUserByUserId ( String userId ) throws DotDataException, DotSecurityException {

        //First lets seach for the user
        UserProxy user = APILocator.getUserProxyAPI().getUserProxy(userId, APILocator.getUserAPI().getSystemUser(), false);

        //And return the tags related to the user
        return getTagsForUserByUserInode(user.getInode());
    }

    public java.util.List<Tag> getTagsForUserByUserInode ( String userInode ) throws DotHibernateException {
        return tagFactory.getTagForUserByUserInode(userInode);
    }

    public java.util.List<Tag> getFilteredTags ( String tagName, String hostFilter, boolean globalTagsFilter, String sort, int start, int count ) {
        return tagFactory.getFilteredTags(tagName, hostFilter, globalTagsFilter, sort, start, count);
    }

    public Tag getTagAndCreate ( String name, String hostId ) throws DotDataException, DotSecurityException {
        return getTagAndCreate(name, "", hostId);
    }

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

    public Tag getTagByTagId ( String tagId ) throws DotHibernateException {
        return tagFactory.getTagByTagId(tagId);
    }

    public Tag getTagByNameAndHost ( String name, String hostId ) throws DotHibernateException {
        return tagFactory.getTagByNameAndHost(name, hostId);
    }

    public Tag saveTag ( String tagName, String userId, String hostId ) throws DotHibernateException {
        return saveTag(tagName, userId, hostId, false);
    }

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

    public void deleteTag ( Tag tag ) throws DotHibernateException {
        List<TagInode> tagInodes = getTagInodesByTagId(tag.getTagId());
        for ( TagInode t : tagInodes ) {
            deleteTagInode(t);
        }

        tagFactory.deleteTag(tag);
    }

    public void deleteTag ( String tagId ) throws DotHibernateException {
        Tag tag = getTagByTagId(tagId);
        deleteTag(tag);
    }

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

    public List getAllTag ( String userId ) {
        return tagFactory.getAllTag(userId);
    }

    public TagInode addTagInode ( String tagName, String inode, String hostId ) throws DotDataException, DotSecurityException {

        //Ensure the tag exists in the tag table
        Tag existingTag = getTagAndCreate(tagName, "", hostId);

        //Create the the tag inode
        return addTagInode(existingTag, inode);
    }

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

    public List<TagInode> getTagInodesByInode ( String inode ) throws DotHibernateException {
        return tagFactory.getTagInodeByInode(inode);
    }

    public List<TagInode> getTagInodesByTagId ( String tagId ) throws DotHibernateException {
        return tagFactory.getTagInodeByTagId(tagId);
    }

    public TagInode getTagInode ( String tagId, String inode ) throws DotHibernateException {
        return tagFactory.getTagInode(tagId, inode);
    }

    public void deleteTagInode ( TagInode tagInode ) throws DotHibernateException {
        tagFactory.deleteTagInode(tagInode);
    }

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

    public void deleteTagInode ( Tag tag, String inode ) throws DotHibernateException {

        TagInode tagInode = getTagInode(tag.getTagId(), inode);
        if ( tagInode != null && UtilMethods.isSet(tagInode.getTagId()) ) {
            deleteTagInode(tagInode);
        }
    }

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

    @SuppressWarnings ( "unchecked" )
    public List<Tag> getAllTagsForUsers ( List<String> userIds ) {
        return tagFactory.getAllTagsForUsers(userIds);
    }

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