package com.dotmarketing.tag.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.*;

public class TagAPIImpl implements TagAPI {

    private TagFactory tagFactory = FactoryLocator.getTagFactory();

    @Override
    public java.util.List<Tag> getAllTags () throws DotDataException {
        return tagFactory.getAllTags();
    }

    @Override
    public java.util.List<Tag> getTagsByName ( String name ) throws DotDataException {
        return tagFactory.getTagsByName(name);
    }

    @Override
    public java.util.List<Tag> getTagsForUserByUserId ( String userId ) throws DotDataException, DotSecurityException {

        //First lets seach for the user
        UserProxy user = APILocator.getUserProxyAPI().getUserProxy(userId, APILocator.getUserAPI().getSystemUser(), false);

        //And return the tags related to the user
        return getTagsForUserByUserInode(user.getInode());
    }

    @Override
    public java.util.List<Tag> getTagsForUserByUserInode ( String userInode ) throws DotDataException {
        return tagFactory.getTagsForUserByUserInode(userInode);
    }

    @Override
    public java.util.List<Tag> getFilteredTags ( String tagName, String hostFilter, boolean globalTagsFilter, String sort, int start, int count ) {
        return tagFactory.getFilteredTags(tagName, hostFilter, globalTagsFilter, true, sort, start, count);
    }

    @Override
    public Tag getTagAndCreate ( String name, String hostId ) throws DotDataException, DotSecurityException {
        return getTagAndCreate(name, "", hostId, false, false);
    }

    @Override
    public Tag getTagAndCreate ( String name, String userId, String hostId ) throws DotDataException, DotSecurityException {
        return getTagAndCreate(name, userId, hostId, false, false);
    }

    @Override
    public Tag getTagAndCreate ( String name, String hostId, boolean persona ) throws DotDataException, DotSecurityException {
        return getTagAndCreate(name, "", hostId, persona, false);
    }

    @Override
    public Tag getTagAndCreate(String name, String userId, String hostId, boolean persona, boolean searchInSystemHost) throws DotDataException, DotSecurityException {

        boolean localTransaction = false;

        try {

            //Check for a transaction and start one if required
            localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

            Tag newTag = new Tag();

            //Search for tags with this given name
            Tag existingTag = null;

            //Before to decide if exist or not lets give it a try to the System host
            if ( searchInSystemHost ) {

                /*
                When we find multiple possible tags with the same tag name we need to choose what tag to use.
                First to choose are Persona tags, then the tag with the given host id and finally the tag living in the
                system host
                 */

                Tag personaTag = null;
                Tag hostTag = null;
                Tag globalTag = null;

                List<Tag> existingTags = tagFactory.getTagsByName(name);
                if ( existingTags != null ) {
                    for ( Tag foundTag : existingTags ) {

                        String currentTagHostId = foundTag.getHostId();

                        //Only use tags living in the given and system host
                        if ( currentTagHostId.equals(Host.SYSTEM_HOST) || currentTagHostId.equals(hostId) ) {

                            if ( currentTagHostId.equals(Host.SYSTEM_HOST) ) {
                                globalTag = foundTag;
                            } else if ( currentTagHostId.equals(hostId) ) {
                                hostTag = foundTag;
                            }

                            if ( foundTag.isPersona() ) {
                                personaTag = foundTag;
                            }

                        }
                    }

                    if ( personaTag != null ) {
                        existingTag = personaTag;
                    } else if ( hostTag != null ) {
                        existingTag = hostTag;
                    } else {
                        existingTag = globalTag;
                    }

                }
            } else {
                existingTag = tagFactory.getTagByNameAndHost(name, hostId);
            }

            // if doesn't exists then the tag is created
            if ( existingTag == null || !UtilMethods.isSet(existingTag.getTagId()) ) {
                // creating tag
                return saveTag(name, userId, hostId, persona);
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

                if ( isGlobalTag(existingTag) ) {
                    newTag = existingTag;
                    globalTagExists = true;
                }
                if ( existingTag.getHostId().equals(existHostId) ) {
                    newTag = existingTag;
                    tagExists = true;
                }

                if ( !globalTagExists ) {
                    //if global doesn't exist, then save the tag and after it checks if it was stored as a global tag
                    if ( !tagExists ) {
                        newTag = saveTag(name, userId, hostId, persona);
                    }

                    if ( newTag.getHostId().equals(Host.SYSTEM_HOST) ) {
                        //move references of non-global tags to new global tag and delete duplicate non global tags
                        List<TagInode> tagInodes = getTagInodesByTagId(existingTag.getTagId());
                        for ( TagInode tagInode : tagInodes ) {
                            tagFactory.updateTagInode(tagInode, newTag.getTagId());
                        }
                        deleteTag(existingTag);
                    }
                }
            }

            //Everything ok..., committing the transaction
            if ( localTransaction ) {
                HibernateUtil.commitTransaction();
            }

            return newTag;

        } catch ( Exception e ) {
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        }

    }

    @Override
    public Tag getTagByTagId ( String tagId ) throws DotDataException {
        return tagFactory.getTagByTagId(tagId);
    }

    @Override
    public Tag getTagByNameAndHost ( String name, String hostId ) throws DotDataException {
        return tagFactory.getTagByNameAndHost(name, hostId);
    }

    @Override
    public Tag saveTag ( String tagName, String userId, String hostId ) throws DotDataException {
        return saveTag(tagName, userId, hostId, false);
    }

    @Override
    public Tag saveTag ( String tagName, String userId, String hostId, boolean persona ) throws DotDataException {

        boolean localTransaction = false;

        if (tagName == null || tagName.length() > 255){
            throw new InvalidTagNameLengthException( tagName );
        }

        try {

            //Check for a transaction and start one if required
            localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

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

            } else {
                hostId = Host.SYSTEM_HOST;
            }
            tag.setHostId(hostId);
            
            Tag foundTagInStorage = tagFactory.getTagByNameAndHost(tag.getTagName(),tag.getHostId());
            
            if(foundTagInStorage == null){
            	foundTagInStorage = tagFactory.createTag(tag);
            }

            //Everything ok..., committing the transaction
            if ( localTransaction ) {
                HibernateUtil.commitTransaction();
            }

            return foundTagInStorage;
        } catch ( Exception e ) {
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }

            throw new GenericTagException( e );
        }
    }

    @Override
    @Deprecated
    public List addUserTag(String tagName, String userId, String inode) throws DotDataException, DotSecurityException {
        return addContentleTag(tagName, userId, inode, inode);
    }

    @Override
    @Deprecated
    public List addContentleTag(String tagName, String userId, String inode, String fieldVarName) throws DotDataException, DotSecurityException {

        boolean localTransaction = false;

        try {

            //Check for a transaction and start one if required
            localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

            StringTokenizer tagNameToken = new StringTokenizer(tagName, ",");
            if ( tagNameToken.hasMoreTokens() ) {
                for (; tagNameToken.hasMoreTokens(); ) {
                    String tagTokenized = tagNameToken.nextToken().trim();
                    Tag createdTag = getTagAndCreate(tagTokenized, userId, "");
                    addContentletTagInode(createdTag, inode, fieldVarName);
                }
            }

            List<TagInode> tagInodes = getTagInodesByInode(inode);

            //Everything ok..., committing the transaction
            if ( localTransaction ) {
                HibernateUtil.commitTransaction();
            }

            return tagInodes;
        } catch ( Exception e ) {
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        }
    }

    @Override
    public void updateTag ( String tagId, String tagName ) throws DotDataException {
        updateTag(tagId, tagName, false, Host.SYSTEM_HOST);
    }

    @Override
    public void updateTag ( String tagId, String tagName, boolean updateTagReference, String hostId ) throws DotDataException {

        Tag tag = getTagByTagId(tagId);
        boolean tagAlreadyExistsForNewTagStorage = false;

        //This block of code prevent saving duplicated tags when editing tag storage from host
        List<Tag> tags = getTagsByName(tagName);

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

    @Override
    public void enableDisablePersonaTag ( String tagId, boolean enableAsPersona ) throws DotDataException {

        //First check if the requested tag exist
        Tag foundTag = getTagByTagId(tagId);
        if ( foundTag != null && UtilMethods.isSet(foundTag.getTagId()) ) {

            if ( foundTag.isPersona() == enableAsPersona ) {
                return;//Nothing to update
            }

            foundTag.setPersona(enableAsPersona);
            //Update the tag
            tagFactory.updateTag(foundTag);
        }
    }

    @Override
    public void deleteTag ( Tag tag ) throws DotDataException {
        //First delete the references to this tag
        deleteTagInodesByTagId(tag.getTagId());
        //And finally remove the tag
        tagFactory.deleteTag(tag);
    }

    @Override
    public void deleteTag ( String tagId ) throws DotDataException {
        Tag tag = getTagByTagId(tagId);
        deleteTag(tag);
    }

    @Override
    public void editTag ( String tagName, String oldTagName, String userId ) throws DotDataException {

        tagName = escapeSingleQuote(tagName);
        oldTagName = escapeSingleQuote(oldTagName);

        List<Tag> tagToEdit = getTagsByName(oldTagName);
        Iterator it = tagToEdit.iterator();
        while ( it.hasNext() ) {
            Tag tag = (Tag) it.next();

            tag.setTagName(tagName.toLowerCase());
            tag.setModDate(new Date());

            tagFactory.updateTag(tag);
        }
    }

    @Override
    public TagInode addUserTagInode(String tagName, String inode, String hostId) throws DotDataException, DotSecurityException {
        return addContentletTagInode(tagName, inode, hostId, inode);
    }

    @Override
    public TagInode addContentletTagInode(String tagName, String inode, String hostId, String fieldVarName) throws DotDataException, DotSecurityException {

        //Ensure the tag exists in the tag table
        Tag existingTag = getTagAndCreate(tagName, "", hostId);

        //Create the the tag inode
        return addContentletTagInode(existingTag, inode, fieldVarName);
    }

    @Override
    public TagInode addUserTagInode(Tag tag, String inode) throws DotDataException {
        return addContentletTagInode(tag, inode, inode);
    }

    @Override
    public TagInode addContentletTagInode(Tag tag, String inode, String fieldVarName) throws DotDataException {

        boolean localTransaction = false;

        try {

            //Check for a transaction and start one if required
            localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

            //validates the tagInode already exists
            TagInode existingTagInode = getTagInode(tag.getTagId(), inode, fieldVarName);

            if ( existingTagInode == null || existingTagInode.getTagId() == null ) {

                //the tagInode does not exists, so create a new TagInode
                TagInode tagInode = new TagInode();
                tagInode.setTagId(tag.getTagId());
                tagInode.setInode(inode);
                tagInode.setFieldVarName(fieldVarName);
                tagInode.setModDate(new Date());

                existingTagInode = tagFactory.createTagInode(tagInode);
            }

            //Everything ok..., committing the transaction
            if ( localTransaction ) {
                HibernateUtil.commitTransaction();
            }

            return existingTagInode;

        } catch (Exception e) {
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        }
    }

    @Override
    public List<TagInode> getTagInodesByInode ( String inode ) throws DotDataException {
        return tagFactory.getTagInodesByInode(inode);
    }

    @Override
    public List<Tag> getTagsByInodeAndFieldVarName(String inode, String fieldVarName) throws DotDataException {
        return tagFactory.getTagsByInodeAndFieldVarName(inode, fieldVarName);
    }

    @Override
    public List<TagInode> getTagInodesByTagId ( String tagId ) throws DotDataException {
        return tagFactory.getTagInodesByTagId(tagId);
    }

    @Override
    public TagInode getTagInode ( String tagId, String inode, String fieldVarName ) throws DotDataException {
        return tagFactory.getTagInode(tagId, inode, fieldVarName);
    }

    @Override
    public void deleteTagInode ( TagInode tagInode ) throws DotDataException {
        tagFactory.deleteTagInode(tagInode);
    }

    @Override
    public void removeTagRelationAndTagWhenPossible ( String tagId, String inode, String fieldVarName ) throws DotDataException {

        boolean localTransaction = false;

        try {

            //Check for a transaction and start one if required
            localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

            Boolean existRelationship = false;
            //Get the tag inode we want to remove
            TagInode tagInodeToRemove = tagFactory.getTagInode(tagId, inode, fieldVarName);
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

            //Everything ok..., committing the transaction
            if ( localTransaction ) {
                HibernateUtil.commitTransaction();
            }

        } catch ( Exception e ) {
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        }

    }

    @Override
    public void deleteTagInodesByInode(String inode) throws DotDataException {
        tagFactory.deleteTagInodesByInode(inode);
    }

    @Override
    public void deleteTagInodesByTagId(String tagId) throws DotDataException {
        tagFactory.deleteTagInodesByTagId(tagId);
    }

    @Override
    public void deleteTagInodesByInodeAndFieldVarName(String inode, String fieldVarName) throws DotDataException {
        tagFactory.deleteTagInodesByInodeAndFieldVarName(inode, fieldVarName);    	
    }

    @Override
    public void deleteTagInode ( Tag tag, String inode, String fieldVarName ) throws DotDataException {

        TagInode tagInode = getTagInode(tag.getTagId(), inode, fieldVarName);
        if ( tagInode != null && UtilMethods.isSet(tagInode.getTagId()) ) {
            deleteTagInode(tagInode);
        }
    }

    @Override
    public void deleteTagInode ( String tagName, String inode, String fieldVarName ) throws DotSecurityException, DotDataException {

        StringTokenizer tagNameToken = new StringTokenizer(tagName, ",");
        if ( tagNameToken.hasMoreTokens() ) {

            for (; tagNameToken.hasMoreTokens(); ) {

                String tagTokenized = tagNameToken.nextToken().trim();

                //Search for tags with the given name
                List<Tag> foundTags = getTagsByName(tagTokenized);
                if ( foundTags != null && !foundTags.isEmpty() ) {

                    for ( Tag foundTag : foundTags ) {
                        //Delete the related tag inode
                        deleteTagInode(foundTag, inode, fieldVarName);
                    }
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

    @Override
    @SuppressWarnings ( "unchecked" )
    public List<Tag> getSuggestedTag(String name, String selectedHostId) throws DotDataException {

        name = escapeSingleQuote(name);

        //if there's a host field on form, retrieve it
        Host hostOnForm;
        if ( UtilMethods.isSet(selectedHostId) ) {
            try {
                hostOnForm = APILocator.getHostAPI().find(selectedHostId, APILocator.getUserAPI().getSystemUser(), true);
                selectedHostId = hostOnForm.getMap().get("tagStorage").toString();
            } catch (Exception e) {
                Logger.error(this, "Unable to load current host.");
            }
        }

        return tagFactory.getSuggestedTags(name, selectedHostId);
    }

    /**
	 * Check if tag is global
	 * @param tag
	 * @return boolean
	 */
    private boolean isGlobalTag ( Tag tag ) {
        if ( tag.getHostId().equals(Host.SYSTEM_HOST) )
            return true;
        else
            return false;
    }

    @Override
    public void updateTagReferences(String hostIdentifier, String oldTagStorageId, String newTagStorageId) throws DotDataException, DotSecurityException {

        boolean localTransaction = false;

        try {

            if ( !oldTagStorageId.equals(Host.SYSTEM_HOST) && !oldTagStorageId.equals(newTagStorageId) ) {

                //Check for a transaction and start one if required
                localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

                //copy or update tags if the tag storage id has changed when editing the host
                //or if the previous tag storage was global
                List<Tag> list = tagFactory.getTagsByHost(oldTagStorageId);

                List<Tag> hostTagList = tagFactory.getTagsByHost(hostIdentifier);

                for ( Tag tag : list ) {
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
                }

                //Everything ok..., committing the transaction
                if ( localTransaction ) {
                    HibernateUtil.commitTransaction();
                }
            }
        } catch ( Exception e ) {
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        }

    }

    @Override
    public List<Tag> getTagsByInode ( String inode ) throws DotDataException {
        return tagFactory.getTagsByInode(inode);
    }

    @Override
    public List<Tag> getTagsInText ( String text, String hostId ) throws DotSecurityException, DotDataException {
        return getTagsInText(text, "", hostId);
    }

    @Override
    public List<Tag> getTagsInText ( String text, String userId, String hostId ) throws DotSecurityException, DotDataException {

        boolean localTransaction = false;

        try {

            //Check for a transaction and start one if required
            localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

            List<Tag> tags = new ArrayList<>();

            //Split the given list of tasks
            String[] tagNames = text.split("[,\\n\\t\\r]");
            for ( String tagname : tagNames ) {
                tagname = tagname.trim();
                if ( tagname.length() > 0 ) {
                    /*
                    Search for this given tag and create it if does not exist, the search in order to define
                    if the tag exist will include the system host
                     */
                    tags.add(getTagAndCreate(tagname, userId, hostId, false, true));
                }
            }

            //Everything ok..., committing the transaction
            if ( localTransaction ) {
                HibernateUtil.commitTransaction();
            }

            return tags;
        } catch (Exception e) {
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        }
    }

}