package com.dotmarketing.tag.ajax;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.uk.ltd.getahead.dwr.WebContextFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.usermanager.factories.UserManagerListBuilderFactory;
import com.dotmarketing.portlets.usermanager.struts.UserManagerListSearchForm;
import com.dotmarketing.tag.business.InvalidTagNameLengthException;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.business.TagAlreadyExistsException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.SessionMessages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

public class TagAjax {

	private static TagAPI tagAPI = APILocator.getTagAPI();

	/**
	 * Tags an object, validates the existence of a tag(s), creates it if it doesn't exists
	 * and then tags the object
	 * @param tagNames tag(s) to create
	 * @param userId owner of the tag
	 * @param hostId the storage host id
	 * @return a list of all tags assigned to an object
	 */
	public static Map<String, Object> addTag ( String tagNames, String userId, String hostId ) throws DotDataException, DotSecurityException {

		if ( !UtilMethods.isSet(hostId) ) {
			hostId = Host.SYSTEM_HOST;
		}

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		List<String> saveTagErrors = new ArrayList<>();
		Map<String, Object> callbackData = new HashMap<>();

    	hostId=hostId.trim();
    	
    	StringTokenizer tagNameToken = new StringTokenizer(tagNames, ",");
    	if (tagNameToken.hasMoreTokens()) {
	    	for (; tagNameToken.hasMoreTokens();) {
	    		String tagName = tagNameToken.nextToken().trim();

	    		try{
					Tag createdTag = APILocator.getTagAPI().getTagAndCreate(tagName, userId, hostId);
					String tagStorageForHost = "";
	    			Host host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(),true);

	    			if(host==null) {

	    				HttpSession session = WebContextFactory.get().getSession();
	    				hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	    				host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(),true);
	    			}
					tagAPI.addUserTagInode(createdTag,
							APILocator.getUserProxyAPI().getUserProxy(userId, APILocator.getUserAPI().getSystemUser(), false).getInode());

	    			if(host!=null && host.getIdentifier()!=null && host.getIdentifier().equals(Host.SYSTEM_HOST))
	    				tagStorageForHost = Host.SYSTEM_HOST;
	    			else {
	    				try {
	    					tagStorageForHost = host.getMap().get("tagStorage").toString();
	    				} catch(NullPointerException e) {
	    					tagStorageForHost = Host.SYSTEM_HOST;
	    				}
	    			}

	    			if (UtilMethods.isSet(tagStorageForHost)){
						if ( !createdTag.getHostId().equals(tagStorageForHost) && createdTag.getHostId().equals(Host.SYSTEM_HOST) ) {
							saveTagErrors.add("Global Tag Already Exists");
	    					SessionMessages.clear(req.getSession());
	    				}

	    			}

	    		}catch(Exception e){
					//Logging the error because DWR tends to swallow the exceptions
					Logger.error(TagAjax.class, e.getMessage(), e);
					saveTagErrors.add(e.getMessage());
	    			SessionMessages.clear(req.getSession());
	    		}finally{
	    			if(saveTagErrors != null && saveTagErrors.size() > 0){
	    				callbackData.put("saveTagErrors", saveTagErrors);
	    				SessionMessages.clear(req.getSession());
	    			}

	    		}
	    	}
    	}

        callbackData.put("tags", new TagAjax().getTagByUser(userId));

        return callbackData;
	}

	/**
	 * Updates an existing tag.
	 * @param tagId tag to update
	 * @param tagName New tagname to be use
	 * @param hostId the storage host id
	 */
	public static Map<String,Object> updateTag(String tagId, String tagName, String hostId){

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
    	List<String> saveTagErrors = new ArrayList<String>();
    	Map<String,Object> callbackData = new HashMap<String,Object>();

    	hostId=hostId.trim();

        try{
			APILocator.getTagAPI().updateTag(tagId, tagName, false, hostId);
		}catch(Exception e){
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "There was an error saving the tag", e);

			saveTagErrors.add("There was an error saving the tag.");
        	SessionMessages.clear(req.getSession());
        }finally{
        	if(saveTagErrors != null && saveTagErrors.size() > 0){
        		callbackData.put("saveTagErrors", saveTagErrors);
        		SessionMessages.clear(req.getSession());
        	}
        }

        return callbackData;
	}


	/**
	 * Tags the users in selected in the User Manager, validates the existence of a tag(s), creates it if it doesn't exists
	 * and then tags the object
	 * @param tagName tag(s) to create
	 * @param userId owner of the tag
	 * @return a list of all tags assigned to an object
	 */
	public static void addTagFullCommand(String tagName, String userId) throws DotDataException, DotSecurityException {
		try
		{
			HttpSession session = WebContextFactory.get().getSession();

			//Get all the user of the filter
			UserManagerListSearchForm searchFormFullCommand = (UserManagerListSearchForm) session.getAttribute(WebKeys.USERMANAGERLISTPARAMETERS);
			searchFormFullCommand.setStartRow(0);
			searchFormFullCommand.setMaxRow(0);
			List matches = UserManagerListBuilderFactory.doSearch(searchFormFullCommand);

			//Get the Iterator and the userIds
			Iterator it = matches.iterator();
			for (int i = 0; it.hasNext(); i++)
			{
				String userTagId = (String) ((Map) it.next()).get("userid");
				String inode = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userTagId,APILocator.getUserAPI().getSystemUser(), false).getInode();
				APILocator.getTagAPI().addUserTag(tagName, userId, inode);
			}
		}
		catch(Exception ex)
		{
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error adding tags", ex);
			throw ex;
		}
	}

	/**
	 * Gets all the tag created by an user
	 * @param userId id of the user
	 * @return a list of all the tags created
	 */
	public List<Tag> getTagByUser ( String userId ) throws DotSecurityException, DotDataException {
		try {
			return APILocator.getTagAPI().getTagsForUserByUserId(userId);
		} catch (Exception e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error retrieving tags", e);
			throw e;
		}
	}

	/**
	 * Gets all the tag created by an user
	 * @param userId id of the user
	 * @return a Map with a list of all the tags created
	 */
	public Map<String, List<Tag>> getTagsByUser ( String userId ) throws DotSecurityException, DotDataException {
		try {
			List<Tag> tags = APILocator.getTagAPI().getTagsForUserByUserId(userId);
			Map<String, List<Tag>> map = new HashMap<String, List<Tag>>();
			map.put("tags", tags);
			return map;
		} catch (Exception e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error retrieving tags", e);
			throw e;
		}
	}

	/**
	 * Deletes a tag
	 * @param tagId id of the tag to be deleted
	 * @return list of all the tags, with the owner information and the respective permission
	 */
	public void deleteTag ( String tagId ) throws DotDataException {
		try {
			tagAPI.deleteTag(tagId);
		} catch (DotDataException e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error deleting tag", e);
			throw e;
		}
	}

	/**
	 * Gets a tag with the owner information, searching by name
	 * @param tagName name of the tag
	 * @return the tag with the owner information
	 */
	public static List<Tag> getTagByName ( String tagName ) throws DotDataException {
		try {
			return APILocator.getTagAPI().getTagsByName(tagName);
		} catch (DotDataException e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error retrieving tag", e);
			throw e;
		}
	}

	/**
	 * Gets all tags associated to an object
	 * @param inode inode of the object tagged
	 * @return list of all the TagInode where the tags are associated to the object
	 */
	public static List<TagInode> getTagInodeByInode(String inode) throws DotDataException {
		try {
			return APILocator.getTagAPI().getTagInodesByInode(inode);
		} catch (DotDataException e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error retrieving tags", e);
			throw e;
		}
	}

	/**
	 * Deletes an object tag assignment(s)
	 * @param tagName name(s) of the tag(s)
	 * @param inode inode of the object tagged
	 * @return a list of all tags assigned to an object
	 */
	public void deleteTagInode ( String tagName, String inode ) throws DotDataException, DotSecurityException {
		try {
			APILocator.getTagAPI().deleteTagInode(tagName, inode, null);
		} catch (Exception e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error deleting tag", e);
			throw e;
		}
	}

	/**
	 * Deletes an object tag assignment
	 * @param tagNameOrId name/id of the tag to remove
	 * @param userId user related with the tag
	 * @return a list of all tags assigned to a user
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Map<String, List<Tag>> deleteTag ( String tagNameOrId, String userId ) throws DotDataException, DotSecurityException {

		try {
			Tag tag = APILocator.getTagAPI().getTagByTagId(tagNameOrId);

			if ( !UtilMethods.isSet(tag) || !UtilMethods.isSet(tag.getTagId()) ) {
				tag = APILocator.getTagAPI().getTagByNameAndHost(tagNameOrId, Host.SYSTEM_HOST);
			}

			if ( tag == null || !UtilMethods.isSet(tag.getTagId()) ) {
				Logger.warn(this, "Requested Tag [" + tagNameOrId + "] for deletion was not found");
			} else {

				//Retrieve the user
				UserProxy user = APILocator.getUserProxyAPI().getUserProxy(userId, APILocator.getUserAPI().getSystemUser(), false);

                /*
				Removes the relationship between a tag and an inode.
                 NOTE: if the tag does not have more relationships the Tag itself will be remove it.
                 */
				APILocator.getTagAPI().removeTagRelationAndTagWhenPossible(tag.getTagId(), user.getInode(), null);
			}

			List<Tag> tags = APILocator.getTagAPI().getTagsForUserByUserId(userId);
			Map<String, List<Tag>> map = new HashMap<>();
			map.put("tags", tags);
			return map;
		} catch (Exception e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error deleting tag", e);
			throw e;
		}
	}

	public static void deleteTagFullCommand(String tagName) throws DotDataException, DotSecurityException {
		try
		{
			HttpSession session = WebContextFactory.get().getSession();

			//Get all the user of the filter
			UserManagerListSearchForm searchFormFullCommand = (UserManagerListSearchForm) session.getAttribute(WebKeys.USERMANAGERLISTPARAMETERS);
			searchFormFullCommand.setStartRow(0);
			searchFormFullCommand.setMaxRow(0);
			List matches = UserManagerListBuilderFactory.doSearch(searchFormFullCommand);

			//Get the Iterator and the userIds
			Iterator it = matches.iterator();
			for (int i = 0; it.hasNext(); i++)
			{
				String userTagId = (String) ((Map) it.next()).get("userid");
				String inode = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userTagId,APILocator.getUserAPI().getSystemUser(), false).getInode();
				APILocator.getTagAPI().deleteTagInode(tagName, inode, null);
			}
		}
		catch(Exception ex)
		{
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error deleting tags", ex);
			throw ex;
		}
	}

	/**
	 * Gets a suggested tag(s), by name and host
	 *
	 * @param tagName                Fragment of the name we are looking for
	 * @param selectedHostOrFolderId Host where to search for the tags (Including SYSTEM_HOST)
	 * @return list of suggested tags
	 */
	public List<Tag> getSuggestedTag(String tagName, String selectedHostOrFolderId) throws DotDataException {

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		try {
			User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
			Host host = APILocator.getHostAPI().find(selectedHostOrFolderId, currentUser, false);
			if ( (!UtilMethods.isSet(host) || !UtilMethods.isSet(host.getInode())) && UtilMethods.isSet(selectedHostOrFolderId) ) {
				selectedHostOrFolderId = APILocator.getFolderAPI().find(selectedHostOrFolderId, currentUser, false).getHostId();
			}
		} catch ( Exception e ) {
			Logger.error(TagAjax.class, e.getMessage(), e);
		}

		try {
			return APILocator.getTagAPI().getSuggestedTag(tagName, selectedHostOrFolderId);
		} catch (Exception e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error retrieving tags", e);
			throw e;
		}
	}

	/**
	 * Get a list of all the tags created
	 * @return list of all tags created
	 */
	public List<Tag> getAllTags () throws DotDataException {
		try {
			return APILocator.getTagAPI().getAllTags();
		} catch (DotDataException e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(this, "Error retrieving tags", e);
			throw e;
		}
	}

	public static Map<String,Object> importTags(byte[] uploadFile) {

		Map<String,Object> callbackData = new HashMap<String,Object>();
		try {
			UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
			WebContext ctx = WebContextFactory.get();
			HttpServletRequest request = ctx.getHttpServletRequest();
			User user = uWebAPI.getLoggedInUser(request);

			String content = new String(uploadFile);
			StringReader sr = new StringReader(content);
			BufferedReader br = new BufferedReader(sr);


			String str;
		    while ((str = br.readLine()) != null) {
		    	String[] tokens = str.split(",");
		    	if(tokens.length>2) {
		    		continue;
		    	}
		    	String tagName = UtilMethods.isSet(tokens[0])?tokens[0]:null;
		    	String hostId = UtilMethods.isSet(tokens[1])?tokens[1]:null;
		    	if(!tagName.toLowerCase().contains("tag name") && !hostId.toLowerCase().contains("host id")){
		    		tagName = tagName.replaceAll("\'|\"", "");
		    		hostId = hostId.replaceAll("\'|\"", "");
					APILocator.getTagAPI().getTagAndCreate(tagName, "", hostId);
				}
		    }

			br.close();

		} catch(Exception e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error importing tags", e);
		}
		return callbackData;
	}
}
