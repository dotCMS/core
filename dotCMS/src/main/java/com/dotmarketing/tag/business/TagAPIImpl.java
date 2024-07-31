package com.dotmarketing.tag.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import io.vavr.control.Try;

import java.util.*;

/**
 * Implementation class for the {@link TagAPI} interface.
 *
 * @author root
 * @since Mar 22, 2012
 */
public class TagAPIImpl implements TagAPI {

    private final TagFactory tagFactory = FactoryLocator.getTagFactory();

    @CloseDBIfOpened
    @Override
    public List<Tag> getAllTags () throws DotDataException {
        return tagFactory.getAllTags();
    }

    @CloseDBIfOpened
    public Set<String> findTopTags(final String siteId) throws DotDataException {

        Logger.debug(this, ()-> "Finding top tags for siteId: " + siteId);
        return this.tagFactory.getTopTagsBySiteId(siteId);
    }

    @CloseDBIfOpened
    @Override
    public List<Tag> getTagsByName ( String name ) throws DotDataException {
        return tagFactory.getTagsByName(name);
    }

    @Override
    public List<Tag> getTagsForUserByUserId ( String userId ) throws DotDataException, DotSecurityException {

        
        //And return the tags related to the user
        return getTagsForUserByUserInode(userId);
    }

    @CloseDBIfOpened
    @Override
    public List<Tag> getTagsForUserByUserInode ( String userInode ) throws DotDataException {
        return tagFactory.getTagsForUserByUserInode(userInode);
    }

    @CloseDBIfOpened
    @Override
    public List<Tag> getFilteredTags ( String tagName, String hostFilter, boolean globalTagsFilter, String sort, int start, int count ) {
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
    @WrapInTransaction
    public Tag getTagAndCreate(final String tagName, final String userId, final String siteId, final boolean persona, final boolean searchInSystemHost) throws DotDataException, DotSecurityException {

        try {

            Tag newTag = new Tag();

            //Search for tags with this given name
            Tag existingTag = null;

            final String tagNameNoPersona = tagName.endsWith(":persona")
                    ? tagName.substring(0, tagName.indexOf(":persona"))
                    : tagName;

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

                final List<Tag> existingTags = tagFactory.getTagsByName(tagNameNoPersona);
                if ( existingTags != null ) {
                    for (final Tag foundTag : existingTags) {

                        final String currentTagHostId = foundTag.getHostId();

                        //Only use tags living in the given and system host
                        if ( currentTagHostId.equals(Host.SYSTEM_HOST) || currentTagHostId.equals(siteId) ) {

                            if ( currentTagHostId.equals(Host.SYSTEM_HOST) ) {
                                globalTag = foundTag;
                            } else if ( currentTagHostId.equals(siteId) ) {
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
                existingTag = tagFactory.getTagByNameAndHost(tagNameNoPersona, siteId);
            }

            // if doesn't exists then the tag is created
            if ( existingTag == null || !UtilMethods.isSet(existingTag.getTagId()) ) {
                // creating tag
                return saveTag(tagNameNoPersona, userId, siteId, persona);
            } else {

                String existHostId;

                //check if global tag already exists
                boolean globalTagExists = false;

                //check if tag exists with same tag name but for a different host
                boolean tagExists = false;

                final Host host = APILocator.getHostAPI().find(siteId, APILocator.getUserAPI().getSystemUser(), true);
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
                        newTag = saveTag(tagNameNoPersona, userId, siteId, persona);
                    }

                    if ( newTag.getHostId().equals(Host.SYSTEM_HOST) ) {
                        //move references of non-global tags to new global tag and delete duplicate non global tags
                        final List<TagInode> tagInodes = getTagInodesByTagId(existingTag.getTagId());
                        for (final TagInode tagInode : tagInodes) {
                            tagFactory.updateTagInode(tagInode, newTag.getTagId());
                        }
                        deleteTag(existingTag);
                    }
                }
            }

            return newTag;

        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when getting and creating Tag with name '%s' under " +
                    "Site ID '%s': %s", tagName, siteId, e.getMessage()));
            throw e;
        }
    }

    @CloseDBIfOpened
    @Override
    public Tag getTagByTagId ( String tagId ) throws DotDataException {
        return tagFactory.getTagByTagId(tagId);
    }

    @CloseDBIfOpened
    @Override
    public Tag getTagByNameAndHost ( String name, String hostId ) throws DotDataException {
        return tagFactory.getTagByNameAndHost(name, hostId);
    }

    @Override
    public Tag saveTag ( String tagName, String userId, String hostId ) throws DotDataException {
        return saveTag(tagName, userId, hostId, false);
    }

    @Override
    public Tag saveTag (final String tagName, final String userId, String siteId, final boolean persona ) throws DotDataException {

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

            Host site = null;

            if ( UtilMethods.isSet(siteId) && !siteId.equals(Host.SYSTEM_HOST) ) {
                try {
                    if ( !UtilMethods.isSet(siteId) ) {
                        site = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true);
                    } else {
                        site = APILocator.getHostAPI().find(siteId, APILocator.getUserAPI().getSystemUser(), true);
                    }
                } catch (final Exception e) {
                    Logger.error(this, String.format("Unable to load Site ID '%s'", siteId));
                }

                if ( site.getMap().get("tagStorage") == null ) {
                    siteId = site.getMap().get("identifier").toString();
                } else {
                    siteId = site.getMap().get("tagStorage").toString();
                }

            } else {
                siteId = Host.SYSTEM_HOST;
            }
            tag.setHostId(siteId);
            
            Tag foundTagInStorage = tagFactory.getTagByNameAndHost(tag.getTagName(),tag.getHostId());
            
            if(foundTagInStorage == null){
            	foundTagInStorage = tagFactory.createTag(tag);
            }

            //Everything ok..., committing the transaction
            if ( localTransaction ) {
                HibernateUtil.closeAndCommitTransaction();
            }

            return foundTagInStorage;
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when saving Tag with name '%s' under Site ID " +
                    "'%s'", tagName, siteId);
            Logger.error(this, errorMsg, e);
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }

            throw new GenericTagException(errorMsg, e);
        } finally {
            if ( localTransaction ) {
                HibernateUtil.closeSessionSilently();
            }
        }
    }

    @Override
    @Deprecated
    public List addUserTag(String tagName, String userId, String inode) throws DotDataException, DotSecurityException {
        return addContentleTag(tagName, userId, inode, inode);
    }

    @Override
    @Deprecated
    public List addContentleTag(final String tagName, final String userId, final String inode, final String fieldVarName) throws DotDataException, DotSecurityException {

        boolean localTransaction = false;

        try {

            //Check for a transaction and start one if required
            localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

            final StringTokenizer tagNameToken = new StringTokenizer(tagName, ",");
            if ( tagNameToken.hasMoreTokens() ) {
                for (; tagNameToken.hasMoreTokens(); ) {
                    final String tagTokenized = tagNameToken.nextToken().trim();
                    final Tag createdTag = getTagAndCreate(tagTokenized, userId, "");
                    addContentletTagInode(createdTag, inode, fieldVarName);
                }
            }

            final List<TagInode> tagInodes = getTagInodesByInode(inode);

            //Everything ok..., committing the transaction
            if ( localTransaction ) {
                HibernateUtil.closeAndCommitTransaction();
            }

            return tagInodes;
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when adding delimited Tags '%s' to Contentlet Inode '%s'" +
                    " to field '%s'", tagName, inode, fieldVarName);
            Logger.error(this, errorMsg, e);
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        } finally {
            if ( localTransaction ) {
                HibernateUtil.closeSessionSilently();
            }
        }
    }

    @Override
    public void updateTag ( String tagId, String tagName ) throws DotDataException {
        updateTag(tagId, tagName, false, Host.SYSTEM_HOST);
    }

    @WrapInTransaction
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

    @WrapInTransaction
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

    @WrapInTransaction
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

    @WrapInTransaction
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

    @WrapInTransaction
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
    public TagInode addContentletTagInode(final Tag tag, final String inode, final String fieldVarName) throws DotDataException {

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
                HibernateUtil.closeAndCommitTransaction();
            }

            return existingTagInode;

        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when adding Tag ID '%s' to Contentlet Inode '%s'" +
                    " to field '%s'", tag.getTagId(), inode, fieldVarName);
            Logger.error(this, errorMsg, e);
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        } finally {
            if ( localTransaction ) {
                HibernateUtil.closeSessionSilently();
            }
        }
    }

    @CloseDBIfOpened
    @Override
    public List<TagInode> getTagInodesByInode ( String inode ) throws DotDataException {
        return tagFactory.getTagInodesByInode(inode);
    }

    @CloseDBIfOpened
    @Override
    public List<Tag> getTagsByInodeAndFieldVarName(String inode, String fieldVarName) throws DotDataException {
        return tagFactory.getTagsByInodeAndFieldVarName(inode, fieldVarName);
    }

    @CloseDBIfOpened
    @Override
    public List<TagInode> getTagInodesByTagId ( String tagId ) throws DotDataException {
        return tagFactory.getTagInodesByTagId(tagId);
    }

    @CloseDBIfOpened
    @Override
    public TagInode getTagInode ( String tagId, String inode, String fieldVarName ) throws DotDataException {
        return tagFactory.getTagInode(tagId, inode, fieldVarName);
    }

    @WrapInTransaction
    @Override
    public void deleteTagInode ( TagInode tagInode ) throws DotDataException {
        tagFactory.deleteTagInode(tagInode);
    }

    @Override
    public void removeTagRelationAndTagWhenPossible (final String tagId, final String inode, final String fieldVarName ) throws DotDataException {

        boolean localTransaction = false;

        try {

            //Check for a transaction and start one if required
            localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

            Boolean existRelationship = false;
            //Get the tag inode we want to remove
            final TagInode tagInodeToRemove = tagFactory.getTagInode(tagId, inode, fieldVarName);
            if ( UtilMethods.isSet(tagInodeToRemove) && UtilMethods.isSet(tagInodeToRemove.getTagId()) ) {
                existRelationship = true;
            }

            //Get the tag we want to remove
            final Tag tagToRemove = tagFactory.getTagByTagId(tagId);

            //First lets search for the relationships of this tag
            final List<TagInode> tagInodes = APILocator.getTagAPI().getTagInodesByTagId(tagId);

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
                HibernateUtil.closeAndCommitTransaction();
            }

        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when removing Tag Relation and possibly Tag " +
                    "itself with ID '%s' to Contentlet Inode '%s' to field '%s'", tagId, inode, fieldVarName);
            Logger.error(this, errorMsg, e);
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        } finally {
            if ( localTransaction ) {
                HibernateUtil.closeSessionSilently();
            }
        }
    }

    @WrapInTransaction
    @Override
    public void deleteTagInodesByInode(String inode) throws DotDataException {
        tagFactory.deleteTagInodesByInode(inode);
    }

    @WrapInTransaction
    @Override
    public void deleteTagInodesByTagId(String tagId) throws DotDataException {
        tagFactory.deleteTagInodesByTagId(tagId);
    }

    @WrapInTransaction
    @Override
    public void deleteTagInodesByInodeAndFieldVarName(String inode, String fieldVarName) throws DotDataException {
        tagFactory.deleteTagInodesByInodeAndFieldVarName(inode, fieldVarName);    	
    }

    @WrapInTransaction
    @Override
    public void deleteTagInode ( Tag tag, String inode, String fieldVarName ) throws DotDataException {

        TagInode tagInode = getTagInode(tag.getTagId(), inode, fieldVarName);
        if ( tagInode != null && UtilMethods.isSet(tagInode.getTagId()) ) {
            deleteTagInode(tagInode);
        }
    }

    @WrapInTransaction
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

    @CloseDBIfOpened
    @Override
    @SuppressWarnings ( "unchecked" )
    public List<Tag> getSuggestedTag(String name, String selectedSiteId) throws DotDataException {

        name = escapeSingleQuote(name);

        //if there's a host field on form, retrieve it
        if ( UtilMethods.isSet(selectedSiteId) ) {
            try {
                final Host siteOnForm = APILocator.getHostAPI().find(selectedSiteId, APILocator.getUserAPI().getSystemUser(), true);
                selectedSiteId = Try.of(()->siteOnForm.getMap().get("tagStorage").toString()).getOrElse(selectedSiteId);
            } catch (Exception e) {
                Logger.error(this, String.format("Unable to load current Site ID '%s'", selectedSiteId),e);
            }
        }

        return tagFactory.getSuggestedTags(name, selectedSiteId);
    }

    /**
	 * Check if tag is global
	 * @param tag
	 * @return boolean
	 */
    private boolean isGlobalTag ( Tag tag ) {
        return ( tag.getHostId().equals(Host.SYSTEM_HOST) );
    }

    @Override
    public void updateTagReferences(final String siteId, final String oldTagStorageId, final String newTagStorageId) throws DotDataException, DotSecurityException {

        boolean localTransaction = false;

        try {

            if ( !oldTagStorageId.equals(Host.SYSTEM_HOST) && !oldTagStorageId.equals(newTagStorageId) ) {

                //Check for a transaction and start one if required
                localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

                //copy or update tags if the tag storage id has changed when editing the host
                //or if the previous tag storage was global
                final List<Tag> list = tagFactory.getTagsByHost(oldTagStorageId);

                final List<Tag> siteTagList = tagFactory.getTagsByHost(siteId);

                for (final Tag tag : list) {
                    if ( (siteId.equals(newTagStorageId) && siteTagList.size() == 0) && !newTagStorageId.equals(Host.SYSTEM_HOST) ) {
                        //copy old tag to host with new tag storage
                        saveTag(tag.getTagName(), "", siteId);
                    } else if ( newTagStorageId.equals(Host.SYSTEM_HOST) ) {
                        //update old tag to global tags
                        getTagAndCreate(tag.getTagName(), Host.SYSTEM_HOST);
                    } else if ( siteId.equals(newTagStorageId) && siteTagList.size() > 0 || siteId.equals(oldTagStorageId) ) {
                        // update old tag with new tag storage
                        updateTag(tag.getTagId(), tag.getTagName(), true, newTagStorageId);
                    }
                }

                //Everything ok..., committing the transaction
                if ( localTransaction ) {
                    HibernateUtil.closeAndCommitTransaction();
                }
            }
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when updating Tag references on Site ID '%s' " +
                    "from old storage '%s' to new storage '%s'", siteId, oldTagStorageId, newTagStorageId);
            Logger.error(this, errorMsg, e);
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        } finally {
            if ( localTransaction ) {
                HibernateUtil.closeSessionSilently();
            }
        }

    }

    @CloseDBIfOpened
    @Override
    public List<Tag> getTagsByInode ( String inode ) throws DotDataException {
        return tagFactory.getTagsByInode(inode);
    }

    @Override
    public List<Tag> getTagsInText ( String text, String hostId ) throws DotSecurityException, DotDataException {
        return getTagsInText(text, "", hostId);
    }

    @Override
    public List<Tag> getTagsInText (final String text, final String userId, final String siteId) throws DotSecurityException, DotDataException {

        boolean localTransaction = false;

        try {

            //Check for a transaction and start one if required
            localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

            final List<Tag> tags = new ArrayList<>();

            //Split the given list of tasks
            final String[] tagNames = text.split("[,\\n\\t\\r]");
            for (String tagname : tagNames) {
                tagname = tagname.trim();
                if ( tagname.length() > 0 ) {
                    /*
                    Search for this given tag and create it if does not exist, the search in order to define
                    if the tag exist will include the system host
                     */
                    tags.add(getTagAndCreate(tagname, userId, siteId, false, true));
                }
            }

            //Everything ok..., committing the transaction
            if ( localTransaction ) {
                HibernateUtil.closeAndCommitTransaction();
            }

            return tags;
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when getting Tags from text '%s' in Site ID " +
                    "'%s'", text, siteId);
            Logger.error(this, errorMsg, e);
            if ( localTransaction ) {
                HibernateUtil.rollbackTransaction();
            }
            throw e;
        } finally {
            if ( localTransaction ) {
                HibernateUtil.closeSessionSilently();
            }
        }
    }

    @Override
    public List<Tag> getTagsByHostId(final String hostId) throws DotDataException {
        return tagFactory.getTagsByHost(hostId);
    }

    @Override
    public void deleteTagsByHostId(final String hostId) throws DotDataException {
        final List<Tag> tags = getTagsByHostId(hostId);
        for(final Tag tag : tags){
            deleteTag(tag);
        }
    }



}
