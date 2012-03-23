package com.dotmarketing.tag.factories;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;

public class TagFactory {

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

	/**
	 * @param permissionAPI the permissionAPI to set
	 */
	public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

	/**
	 * Get a list of all the tags created
	 * @return list of all tags created
	 */
	public static java.util.List<Tag> getAllTags() {
		try {
			HibernateUtil dh = new HibernateUtil(Tag.class);
			dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag");
            List list = dh.list();
        	return list;
		}
		catch (Exception e) {}
		return new ArrayList();
	}

	/**
	 * Get a list of all the tags name created
	 * @return list of all tags name created
	 */
	public static java.util.List<String> getAllTagsName() {
		try {
			List<String> result = new ArrayList<String>();

			List<Tag> tags = getAllTags();
			for (Tag tag: tags) {
				result.add(tag.getTagName());
			}

			return result;
		}
		catch (Exception e) {}
		return new ArrayList();
	}

	/**
	 * Gets a Tag by name
	 * @param name name of the tag to get
	 * @return tag
	 */
	public static java.util.List<Tag> getTagByName(String name) {
        try {
			name = escapeSingleQuote(name);

			HibernateUtil dh = new HibernateUtil(Tag.class);
            dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where lower(tagName) = ?");
            dh.setParam(name.toLowerCase());

            List list = dh.list();
        	return list;
        } catch (Exception e) {
            Logger.warn(Tag.class, "getTagByName failed:" + e, e);
        }
        return new ArrayList();
    }

	/**
	 * Gets all the tag created by an user
	 * @param userId id of the user
	 * @return a list of all the tags created
	 */
	public static java.util.List<Tag> getTagByUser(String userId) {
        try {
            HibernateUtil dh = new HibernateUtil(Tag.class);
            dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where user_id = ?");
            dh.setParam(userId);

            List list = dh.list();

        	return list;
        } catch (Exception e) {
            Logger.warn(Tag.class, "getTagByUser failed:" + e, e);
        }
        return new java.util.ArrayList();
	}

	/**
	 * Gets a Tag by name, validates the existance of the tag, if it doesn't exists then is created
	 * @param name name of the tag to get
	 * @param userId owner of the tag
	 * @return tag
	 */
	public static Tag getTag(String name, String userId) {

		// validating if exists a tag with the name provided
        HibernateUtil dh = new HibernateUtil(Tag.class);
        Tag tag =null;
        try {
			dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where lower(tagName) = ?");
			dh.setParam(name.toLowerCase());

			tag = (Tag) dh.load();
		} catch (DotHibernateException e) {
			Logger.error(TagFactory.class,"getTag failed:" + e, e);
		}
        // if doesn't exists then the tag is created
        if (tag.getTagName() == null) {
        	// creating tag
        	return addTag(name, userId);
        }
        // returning tag
        return tag;
	}

	/**
	 * Creates a new tag
	 * @param tagName name of the new tag
	 * @param userId owner of the new tag
	 * @return new tag created
	 */
    public static Tag addTag(String tagName, String userId) {
		//creates new Tag
    	Tag tag = new Tag();
    	tag.setTagName(tagName);
    	tag.setUserId(userId);
        try {
			HibernateUtil.save(tag);
		} catch (DotHibernateException e) {
			Logger.error(TagFactory.class,"addTag failed:" + e, e);
		}
        return tag;
    }

	/**
	 * Tags an object, validates the existence of a tag(s), creates it if it doesn't exists
	 * and then tags the object
	 * @param tagName tag(s) to create
	 * @param userId owner of the tag
	 * @param inode object to tag
	 * @return a list of all tags assigned to an object
	 * @throws Exception
	 */
	public static List addTag(String tagName, String userId, String inode) throws Exception {
		StringTokenizer tagNameToken = new StringTokenizer(tagName, ",");
		if (tagNameToken.hasMoreTokens()) {
	    	for (; tagNameToken.hasMoreTokens();) {
	    		String tagTokenized = tagNameToken.nextToken().trim();
	    		TagFactory.getTag(tagTokenized, userId, "");
	    		TagFactory.addTagInode(tagTokenized, inode, "");
	    	}
		}
		return getTagInodeByInode(inode);
	}

	/**
     * Deletes a tag
     * @param tag tag to be deleted
     */
    public static void deleteTag(Tag tag) {
        try {
			HibernateUtil.delete(tag);
		} catch (DotHibernateException e) {
			Logger.error(TagFactory.class,"deleteTag failed:" + e, e);
		}
    }

    /**
     * Deletes a tag
     * @param tagName name of the tag to be deleted
     * @param userId id of the tag owner
     */
	public static void deleteTag(String tagName, String userId) {
		Tag tag = getTag(tagName, userId);
    	try {
			HibernateUtil.delete(tag);
		} catch (DotHibernateException e) {
			Logger.error(TagFactory.class,"deleteTag failed:" + e, e);
		}
	}

	/**
	 * Renames a tag
	 * @param tagName new tag name
	 * @param oldTagName current tag name
	 * @param userId owner of the tag
	 */
	public static void editTag(String tagName,String oldTagName, String userId) {
		try {
			tagName = escapeSingleQuote(tagName);
			oldTagName = escapeSingleQuote(oldTagName);

			List tagToEdit = getTagByName(oldTagName);
			Iterator it = tagToEdit.iterator();
			for (int i = 0; it.hasNext(); i++) {
				Tag tag = (Tag)it.next();

				tag.setTagName(tagName);
				HibernateUtil.saveOrUpdate(tag);
			}
		}
		catch (Exception e) {}
	}

	/**
	 * Gets all the tags created, with the respective owner and permission information
	 * @param userId id of the user that searches the tag
	 * @return a complete list of all the tags, with the owner information and the respective permission
	 * information
	 */
	public static List getAllTag(String userId) {
		try {
			User searcherUser = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);

			HibernateUtil dh = new HibernateUtil();
			StringBuffer sb = new StringBuffer();
			sb.append("select Tag.*, User_.firstName, User_.lastName from Tag, User_ ");
			sb.append("where Tag.user_id = User_.userid ");
			sb.append("order by Tag.user_id");
			dh.setQuery(sb.toString());

			List allTags = dh.list();

			java.util.List matchesArray = new ArrayList();
			Iterator it = allTags.iterator();
			for (int i = 0; it.hasNext(); i++) {
				User user = null;

				Map map = (Map)it.next();

				String user_Id = (String) map.get("user_id");
				String tagName = (String) map.get("tagname");
				String firstName = (String) map.get("firstname");
				String lastName = (String) map.get("lastname");
				user = APILocator.getUserAPI().loadUserById(user_Id,APILocator.getUserAPI().getSystemUser(),false);
				UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

				String[] match = new String[6];
				match[0] = (user_Id==null)?"":user_Id;
				match[1] = (tagName==null)?"":tagName;
				match[2] = (firstName==null)?"":firstName;
				match[3] = (lastName==null)?"":lastName;

				// adding read permission
				try {
					_checkUserPermissions(userProxy, searcherUser, PERMISSION_READ);
					match[4] = "true";
				} catch (ActionException ae) {
					match[4] = "false";
				}

				// adding write permission
				try {
					_checkUserPermissions(userProxy, searcherUser, PERMISSION_WRITE);
					match[5] = "true";
				} catch (ActionException ae) {
					match[5] = "false";
				}
				matchesArray.add(match);
			}

			return matchesArray;
		}
		catch (Exception e) {}

		return new ArrayList();
	}

	/**
	 * Gets a tag with the owner information, searching by name
	 * @param name name of the tag
	 * @return the tag with the owner information
	 */
	public static List getTagInfoByName(String name) {
		try {
			name = escapeSingleQuote(name);

			HibernateUtil dh = new HibernateUtil();
			StringBuffer sb = new StringBuffer();
			sb.append("select Tag.*, User_.firstName, User_.lastName from Tag, User_ ");
			sb.append("where Tag.user_id = User_.userid and ");
			sb.append("lower(Tag.tagName) like '%"+name.toLowerCase()+"%' ");
			sb.append("order by Tag.user_id");

			dh.setQuery(sb.toString());

			java.util.List allTags = dh.list();

			return allTags;
		}
		catch (Exception e) {}

		return new ArrayList();
	}

	/**
	 * Checks the permission access of an user over an object
	 * @param webAsset object to validates access
	 * @param user user to validate access
	 * @param permission read or write permission to validates
	 * @throws ActionException
	 * @throws DotDataException
	 */
	protected static void _checkUserPermissions(Inode webAsset, User user,
			int permission) throws ActionException, DotDataException {
		// Checking permissions
		if (!InodeUtils.isSet(webAsset.getInode()))
			return;

		if (!permissionAPI.doesUserHavePermission(webAsset, permission,
				user)) {
			throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
	}

	/**
	 * Gets a tagInode and a host identifier, if doesn't exists then the tagInode it's created
	 * @param tagName name of the tag
	 * @param inode inode of the object tagged
	 * @param hostId the identifier of host that storage the tag
	 * @return a tagInode
	 */

    public static TagInode addTagInode(String tagName, String inode, String hostId) throws Exception {

    	//Ensure the tag exists in the tag table
    	Tag existingTag = getTag(tagName, "", hostId);

    	//validates the tagInode already exists
		TagInode existingTagInode = getTagInode(existingTag.getTagId(), inode);

    	if (existingTagInode.getTagId() == null) {

	    	//the tagInode does not exists, so creates a new TagInode
	    	TagInode tagInode = new TagInode();
	    	tagInode.setTagId(existingTag.getTagId());
	    	/*long i = 0;
	    	try{
	    		i =Long.parseLong(inode);
	    	}catch (Exception e) {
				Logger.error(this, "Unable to get Long value from " + inode, e);
			}*/
	    	tagInode.setInode(inode);
	        HibernateUtil.saveOrUpdate(tagInode);

	        return tagInode;
    	}
    	else {
    		// returning the existing tagInode
    		return existingTagInode;
    	}
    }

    /**
     * Gets all tags associated to an object
     * @param inode inode of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
	public static List getTagInodeByInode(String inode) {
        try {
            HibernateUtil dh = new HibernateUtil(Tag.class);
            dh.setQuery("from tag_inode in class com.dotmarketing.tag.model.TagInode where inode = ?");
            dh.setParam(inode);

            List list = dh.list();
        	return list;

        } catch (Exception e) {
            Logger.warn(Tag.class, "getTagInodeByInode failed:" + e, e);
        }
        return new ArrayList();
	}

	/**
	 * Gets a tagInode by name and inode
	 * @param name name of the tag
	 * @param inode inode of the object tagged
	 * @return the tagInode
	 */
	public static TagInode getTagInode(String tagId, String inode) {
		// getting the tag inode record
        HibernateUtil dh = new HibernateUtil(Tag.class);
        try {
        	  dh.setQuery("from tag_inode in class com.dotmarketing.tag.model.TagInode where tag_id = ? and inode = ?");
     	} catch (DotHibernateException e) {
			Logger.error(TagFactory.class,"getTagInode failed:" + e, e);
		}
        dh.setParam(tagId);
        dh.setParam(inode);

        TagInode tagInode;
        try {
        	tagInode = (TagInode) dh.load();
        }
        catch (Exception ex) {
        	tagInode = new TagInode();
        }
        return tagInode;
	}

	/**
	 * Deletes a TagInode
	 * @param tagInode TagInode to delete
	 */
	public static void deleteTagInode(TagInode tagInode) {
        try {
			HibernateUtil.delete(tagInode);
		} catch (DotHibernateException e) {
			Logger.error(TagFactory.class,"deleteTagInode failed:" + e, e);
		}
    }

	/**
	 * Deletes an object tag assignment(s)
	 * @param tagName name(s) of the tag(s)
	 * @param inode inode of the object tagged
	 * @return a list of all tags assigned to an object
	 * @throws Exception
	 */
	public static List deleteTagInode(String tagName, String inode) {
		StringTokenizer tagNameToken = new StringTokenizer(tagName, ",");
		if (tagNameToken.hasMoreTokens()) {
	    	for (; tagNameToken.hasMoreTokens();) {
	    		String tagTokenized = tagNameToken.nextToken().trim();
	    		TagInode tagInode = getTagInode(tagTokenized, inode);
	        	if (tagInode.getTagId() != null) {
	            	try {
						HibernateUtil.delete(tagInode);
					} catch (DotHibernateException e) {
						Logger.error(TagFactory.class,"deleteTagInode failed:" + e, e);
					}
	        	}
	    	}
		}
		return getTagInodeByInode(inode);
	}

	/**
	 * Escape a single quote
	 * @param tagName string with single quotes
	 * @return single quote string escaped
	 */
	private static String escapeSingleQuote(String tagName) {
		return tagName.replace("'", "''");
	}

	/**
	 * Gets a suggested tag(s), by name
	 * @param name name of the tag searched
	 * @return list of suggested tags
	 */
	@SuppressWarnings("unchecked")
	public static List<Tag> getSuggestedTag(String name) {
		try {
			name = escapeSingleQuote(name);

			HibernateUtil dh = new HibernateUtil(Tag.class);
			dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where lower(tagname) like ?");
	        dh.setParam(name.toLowerCase() + "%");
            List<Tag> list = dh.list();
        	return list;
		}
		catch (Exception e) {}
		return new ArrayList<Tag>();
	}

	/**
	 * Gets all the tags given a user List
	 * @param userIds the user id's associated with the tags
	 * @return a complete list of all the tags, with the owner information and the respective permission
	 * information
	 */
	@SuppressWarnings("unchecked")
	public static List<Tag> getAllTagsForUsers(List<String> userIds) {
		try {
			DotConnect dc = new DotConnect();
			StringBuilder sb = new StringBuilder();
			sb.append("select tag.tagname, tag.user_id from tag, user_ ");
			sb.append("where tag.user_id = user_.userid ");
			if(userIds!=null && !userIds.isEmpty()){
				sb.append(" and user_.userid in (");
				int count = 0;
				for(String id:userIds){
					if(count>0 && count%500==0){
						count=0;
						sb.append(") or user_.userid in (");
					}
					if(count>0){
						sb.append(", '"+id+"'");
					}else{
						sb.append("'"+id+"'");
					}
					count++;
				}
				sb.append(") ");
			}

			sb.append("order by tag.user_id");
			dc.setSQL(sb.toString());

			List<Tag> tags = new ArrayList<Tag>();
			List<Map<String, Object>> results = (ArrayList<Map<String, Object>>)dc.loadResults();
			for (int i = 0; i < results.size(); i++) {
				Map<String, Object> hash = (Map<String, Object>) results.get(i);
				if(!hash.isEmpty()){
					String user_Id = (String) hash.get("user_id");
					String tagName = (String) hash.get("tagname");
					Tag tag = new Tag();
					tag.setTagName(tagName);
					tag.setUserId(user_Id);
					tags.add(tag);

				}
			}

			return tags;
		}
		catch (Exception e) {
			 Logger.warn(TagFactory.class, "getAllTagsForUsers failed:" + e, e);
		}
		return new ArrayList();
	}

	/**
	 * Gets a Tag by name, validates the existance of the tag, if it doesn't exists then is created
	 * @param name name of the tag to get
	 * @param userId owner of the tag
	 * @param hostId
	 * @return tag
	 */
	public static Tag getTag(String name, String userId, String hostId) throws Exception {

		// validating if exists a tag with the name provided
        HibernateUtil dh = new HibernateUtil(List.class);
    	dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where lower(tagName) = ?");
        dh.setParam(name.toLowerCase());

        Tag newTag = new Tag();
        List<Tag> tags = (List<Tag>) dh.list();
        // if doesn't exists then the tag is created
        if (tags == null || tags.size() == 0) {
        	// creating tag
        	return saveTag(name, userId, hostId);
        }
        else {
        	//check if global tag already exists
        	boolean globalTagExists = false;

        	//check if tag exists with same tag name but for a different host
        	boolean tagExists = false;

        	for(Tag tag : tags){

        		if(isGlobalTag(tag)){
        			newTag = tag;
        			globalTagExists = true;
        		}
        		if(tag.getHostId()!=null && tag.getHostId().equals(hostId)){
        			newTag = tag;
            		tagExists = true;
        		}
	    	}

        	if(!globalTagExists){
        		//if global doesn't exist, then save the tag and after it checks if it was stored as a global tag
        		try{
                	if(!tagExists)
                		newTag = saveTag(name, userId, hostId);

                	if(newTag.getHostId().equals(Host.SYSTEM_HOST)){
                		//move references of non-global tags to new global tag and delete duplicate non global tags
                    	for(Tag tag : tags){
                    		List<TagInode> tagInodes = getTagInodeByTagId(tag.getTagId());
                    		for (TagInode tagInode : tagInodes){
                    			updateTagInode(tagInode, newTag.getTagId());
        		            }
                    		deleteTag(tag);
        		    	}
                	}
        		}
        		catch(Exception e){
        			Logger.warn(TagFactory.class, "There was an error saving the tag. There's already a tag for selected host");
        			//return existent tag for selected host
        		}
        	}
        }
        // returning tag
        return newTag;
	}

	/**
	 * Creates a new tag
	 * @param tagName name of the new tag
	 * @param userId owner of the new tag
	 * @param hostId
	 * @return new tag created
	 * @throws DotHibernateException
	 */
    public static Tag saveTag(String tagName, String userId, String hostId) throws Exception {

    	Tag tag = new Tag();
    	//creates new Tag
    	tag.setTagName(tagName);
    	tag.setUserId(userId);

    	Host host = null;

    	if(UtilMethods.isSet(hostId) && !hostId.equals(Host.SYSTEM_HOST)){
    		try{
        		if(!UtilMethods.isSet(hostId)){
        			host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true);
        		}
        		else {
        			host = APILocator.getHostAPI().find(hostId,APILocator.getUserAPI().getSystemUser(), true);
        		}
        	}
        	catch (Exception e) {
        		Logger.error(TagFactory.class, "Unable to load host.");
        	}

    		try {
    			hostId=host.getMap().get("tagStorage").toString();
    		} catch (NullPointerException e) {
    			hostId = Host.SYSTEM_HOST;
    		}

    	}
    	else {
    		hostId = Host.SYSTEM_HOST;
    	}
    	tag.setHostId(hostId);
    	HibernateUtil.save(tag);

    	return tag;
    }

    /**
	 * Check if tag is global
	 * @param tag
	 * @return boolean
	 */
    public static boolean isGlobalTag(Tag tag) {
    	if(tag.getHostId()!=null && tag.getHostId().equals(Host.SYSTEM_HOST))
    		return true;
    	else
    		return false;
    }

    /**
     * Gets all tags associated to an object
     * @param tagId tagId of the object tagged
     * @return list of all the TagInode where the tags are associated to the object
     */
	public static List<TagInode> getTagInodeByTagId(String tagId) {
        try {
            HibernateUtil dh = new HibernateUtil(Tag.class);
            dh.setQuery("from tag_inode in class com.dotmarketing.tag.model.TagInode where tag_id = ?");
            dh.setParam(tagId);

            List list = dh.list();
        	return list;

        } catch (Exception e) {
            Logger.warn(Tag.class, "getTagInodeByTagId failed:" + e, e);
        }
        return new ArrayList();
	}

	private static void updateTagInode(TagInode tagInode, String tagId) throws DotHibernateException {
		tagInode.setTagId(tagId);
		HibernateUtil.saveOrUpdate(tagInode);

	}

	/**
	 * Gets a Tag by a tagId retrieved from a TagInode.
	 * @param tagId the tag id to get
	 * @return tag
	 * @throws DotHibernateException
	 */
	public static Tag getTagByTagId(String tagId) throws DotHibernateException {

        HibernateUtil dh = new HibernateUtil(Tag.class);

    	dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where tag_id = ?");
        dh.setParam(tagId);

        Tag tag = (Tag) dh.load();

        return tag;
	}

	/**
     * Deletes a tag
     * @param tagName name of the tag to be deleted
     * @param userId id of the tag owner
     */
	public static void deleteTag(String tagId)  throws DotHibernateException  {
		Tag tag = getTagByTagId(tagId);
		deleteTag(tag);
	}

	public static void updateTagReferences (String hostIdentifier, String oldTagStorageId, String newTagStorageId) {
		try {
            if(!oldTagStorageId.equals(Host.SYSTEM_HOST) && !oldTagStorageId.equals(newTagStorageId)) {
            	//copy or update tags if the tag storage id has changed when editing the host
            	//or if the previous tag storage was global
    			HibernateUtil dh = new HibernateUtil(Tag.class);
    			dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where host_id = ?");
    			dh.setParam(oldTagStorageId);
                List<Tag> list = (List<Tag>)dh.list();
            	for (Tag tag: list){
                	try{
            			if(hostIdentifier.equals(newTagStorageId) && !newTagStorageId.equals(Host.SYSTEM_HOST)){
                    		//copy old tag to host with new tag storage
                    		tag = saveTag(tag.getTagName(), "", hostIdentifier);
                    	}
                    	else if(newTagStorageId.equals(Host.SYSTEM_HOST)){
                    		//update old tag to global tags
                    		tag = getTag(tag.getTagName(), "", Host.SYSTEM_HOST);
                    	}else {
                    		// update old tag with new tag storage
                    		updateTag(tag.getTagId(), tag.getTagName(), newTagStorageId);
                    	}

                	}catch (Exception e){

                	}
                }
            }


		}
		catch (Exception e) {}


    }

	public static void updateTag(String tagId, String tagName, String hostId) throws Exception  {

		Tag tag = getTagByTagIdAndHostId(tagId,hostId);

    	if(UtilMethods.isSet(tag.getTagId())){
    		tag.setTagName(tagName);
			tag.setUserId("");
			if(UtilMethods.isSet(hostId))
				tag.setHostId(hostId);
			HibernateUtil.saveOrUpdate(tag);
    	}

	}

	public static void updateTag(String tagId, String tagName, boolean updateTagReference, String hostId) throws Exception  {

		Tag tag = getTagByTagId(tagId);
		boolean tagAlreadyExistsForNewTagStorage = false;

		//This block of code prevent saving duplicated tags when editing tag storage from host
		List<Tag> tags = getTagByName(tagName.toLowerCase());

		for(Tag t: tags){
			if(t.getHostId().equals(hostId)){
				//The tag with new tag storage already exists
				tagAlreadyExistsForNewTagStorage = true;
			}
			if(t.getTagId().equals(tagId)){
				//select tag to be updated
				tag = t;
			}
		}

		//update selected tag if it's set and if previous tag storage is different.
    	if(UtilMethods.isSet(tag.getTagId())&&!tagAlreadyExistsForNewTagStorage){
    		tag.setTagName(tagName);
			tag.setUserId("");
			if(updateTagReference){
				if(UtilMethods.isSet(hostId))
					tag.setHostId(hostId);
			}
			HibernateUtil.saveOrUpdate(tag);
    	}

	}

	/**
	 * Gets a Tag by a tagId and a hostId.
	 * @param tagId the tag id to get
	 * @param hostId the host id
	 * @return tag
	 * @throws DotHibernateException
	 */
	public static Tag getTagByTagIdAndHostId(String tagId, String hostId) throws DotHibernateException {

        HibernateUtil dh = new HibernateUtil(Tag.class);

    	dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where tag_id = ? and host_id = ?");
        dh.setParam(tagId);
        dh.setParam(hostId);

        Tag tag = (Tag) dh.load();

        return tag;
	}

	/**
	 * Gets a suggested tag(s), by name
	 * @param name name of the tag searched
	 * @return list of suggested tags
	 */
	@SuppressWarnings("unchecked")
	public static List<Tag> getSuggestedTag(HttpServletRequest request, String name, String selectedHostId) {
		try {
			name = escapeSingleQuote(name);

			String currentHostId = "";
		   	Host currentHost = null;
		   	String hostId = "";

	    	//if there's a host field on form, retrieve it
	    	Host hostOnForm = null;
	    	if(UtilMethods.isSet(selectedHostId)){
	    		try{
	    			hostOnForm = APILocator.getHostAPI().find(selectedHostId,APILocator.getUserAPI().getSystemUser(), true);
	    			selectedHostId=hostOnForm.getMap().get("tagStorage").toString();
		    	}
		    	catch (Exception e) {
		    		selectedHostId=Host.SYSTEM_HOST;
		    		Logger.info(TagFactory.class, "Unable to load current host.");
		    	}
	    	}
			/*
	    	try{

	    		currentHostId = request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID).toString();
	    		currentHost = APILocator.getHostAPI().find(currentHostId,APILocator.getUserAPI().getSystemUser(), true);
	    		hostId=currentHost.getMap().get("tagStorage").toString();

	    	}
	    	catch (Exception e) {
	    		Logger.error(this, "Unable to load current host.");
	    	}*/

			HibernateUtil dh = new HibernateUtil(Tag.class);
			dh.setQuery("from tag in class com.dotmarketing.tag.model.Tag where lower(tagname) like ? and (host_id like ? OR host_id like ?)");
	        dh.setParam(name.toLowerCase() + "%");
	        /*
	        if(UtilMethods.isSet(selectedHostId)){
		        //if structure has a host field, search in selected host
		        dh.setParam(selectedHostId);
	        } else {
		        //search in tag storage for current host
		        dh.setParam(hostId);
	        }*/
	        //search global
	        dh.setParam(selectedHostId);
	        dh.setParam(Host.SYSTEM_HOST);
            List<Tag> list = dh.list();
        	return list;
		}
		catch (Exception e) {}
		return new ArrayList<Tag>();
	}


}
