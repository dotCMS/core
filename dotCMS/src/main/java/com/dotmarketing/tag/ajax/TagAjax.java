package com.dotmarketing.tag.ajax;

import com.dotcms.repackage.uk.ltd.getahead.dwr.WebContextFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import com.dotmarketing.tag.business.GenericTagException;
import com.dotmarketing.tag.business.InvalidTagNameLengthException;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.business.TagAlreadyExistsException;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
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
	 * Creates the given tag(s) when they don't exist yet and adds them to a user's profile. With
	 * an empty {@code userId} the tags are only created. Creating tags without an owner needs
	 * access to one of the Tags portlets. Adding tags to a user follows
	 * {@link TagAPI#canEditUserTags(User, String)}, and the logged in user also needs access to one
	 * of the Users or Tags portlets, since tags can be created on any storage host.
	 *
	 * @param tagNames comma-separated tag name(s)
	 * @param userId   id of the user who gets the tags, or empty to only create them
	 * @param hostId   the storage host id
	 * @return the user's tags, plus any errors found while saving
	 * @throws DotSecurityException if the logged in user can't add these tags
	 * @throws DotDataException     if there's a data access error
	 */
	public static Map<String, Object> addTag ( String tagNames, String userId, String hostId ) throws DotDataException, DotSecurityException {

		final User user = getLoggedInUser();
		if (UtilMethods.isSet(userId)) {
			checkCanEditUserTags(user, userId);
			if (!APILocator.getLayoutAPI().doesUserHaveAccessToUsersPortlet(user)) {
				checkCanManageTags(user);
			}
		} else {
			checkCanManageTags(user);
		}

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
					tagAPI.addUserTagInode(createdTag,userId
							);

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

        callbackData.put("tags", tagAPI.getTagsForUserByUserId(userId));

        return callbackData;
	}

	/**
	 * Renames an existing tag or moves it to another storage host. Needs access to one of the Tags
	 * portlets, and the same permissions as deleting the tag ({@link TagAPI#canDeleteTag(User, String)}),
	 * because the change reaches every piece of content that carries it.
	 *
	 * @param tagId   tag to update
	 * @param tagName new tag name
	 * @param hostId  the storage host id
	 * @return any errors found while saving
	 * @throws DotSecurityException if the logged in user can't change the tag
	 * @throws DotDataException     if the user's permissions can't be resolved
	 */
	public static Map<String,Object> updateTag(String tagId, String tagName, String hostId)
			throws DotDataException, DotSecurityException {

		final User user = getLoggedInUser();
		checkCanManageTags(user);
		if (!tagAPI.canDeleteTag(user, tagId)) {
			throw refused(user, "change the tag");
		}

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
    	List<String> saveTagErrors = new ArrayList<>();
    	Map<String,Object> callbackData = new HashMap<>();

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
	 * Gets the tags of a user's profile. Follows {@link TagAPI#canEditUserTags(User, String)}.
	 *
	 * @param userId id of the user
	 * @return a Map with the user's tags under {@code tags}
	 * @throws DotSecurityException if the logged in user can't see that user's tags
	 * @throws DotDataException     if there's a data access error
	 */
	public Map<String, List<Tag>> getTagsByUser ( String userId ) throws DotSecurityException, DotDataException {
		checkCanEditUserTags(getLoggedInUser(), userId);
		try {
			List<Tag> tags = APILocator.getTagAPI().getTagsForUserByUserId(userId);
			Map<String, List<Tag>> map = new HashMap<>();
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
	public void deleteTag ( String tagId ) throws DotDataException, DotSecurityException {
		try {
			deleteTag(getLoggedInUser(), tagId);
		} catch (DotDataException | DotSecurityException e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error deleting tag", e);
			throw e;
		}
	}

	/**
	 * Deletes a tag on behalf of the given user, following the rules of
	 * {@link TagAPI#canDeleteTag(User, String)}.
	 *
	 * @param user  user requesting the deletion
	 * @param tagId id of the tag to be deleted
	 * @throws DotSecurityException if the user can't delete the tag
	 * @throws DotDataException     if there's a data access error
	 */
	static void deleteTag(final User user, final String tagId) throws DotDataException, DotSecurityException {
		if (!tagAPI.deleteTag(user, tagId)) {
			throw new DotSecurityException(String.format(
					"User '%s' lacks permission to delete tag '%s'", user.getUserId(), tagId));
		}
	}


	/**
	 * Checks that a user may see or change the tags of a given user, following
	 * {@link TagAPI#canEditUserTags(User, String)}: the user themselves, an admin or the system user,
	 * or a user with access to one of the Users portlets.
	 *
	 * @param user   user making the change
	 * @param userId id of the user whose tags are changed
	 * @throws DotSecurityException if the user can't change those tags
	 * @throws DotDataException     if the user's portlet access can't be resolved
	 */
	static void checkCanEditUserTags(final User user, final String userId)
			throws DotDataException, DotSecurityException {
		if (tagAPI.canEditUserTags(user, userId)) {
			return;
		}
		throw refused(user, "work with the tags of another user");
	}

	/**
	 * Checks that a user may create, rename or import tags: an admin, or a user with access to the
	 * Tags portlet or the legacy Tag Manager.
	 *
	 * @param user user making the change
	 * @throws DotSecurityException if the user can't manage tags
	 * @throws DotDataException     if the user's portlet access can't be resolved
	 */
	static void checkCanManageTags(final User user) throws DotDataException, DotSecurityException {
		final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
		if (user.isAdmin()
				|| layoutAPI.doesUserHaveAccessToPortlet(PortletID.TAGS.toString(), user)
				|| layoutAPI.doesUserHaveAccessToPortlet(PortletID.TAGS_LEGACY.toString(), user)) {
			return;
		}
		throw refused(user, "manage tags");
	}

	/**
	 * Returns the user logged in to the current DWR request.
	 *
	 * @return the logged in user
	 * @throws DotSecurityException if there's no logged in user
	 */
	private static User getLoggedInUser() throws DotSecurityException {
		final HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
		final User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
		if (user == null) {
			throw new DotSecurityException("There's no logged in user");
		}
		return user;
	}

	/**
	 * Records a refused tag operation once in the security log and builds the exception to throw.
	 * Only the logged in user's id is recorded, never the values sent with the request.
	 *
	 * @param user   the user whose request was refused
	 * @param action what the user tried to do, e.g. "manage tags"
	 * @return the {@link DotSecurityException} to throw
	 */
	private static DotSecurityException refused(final User user, final String action) {
		final String message = String.format("User '%s' lacks permission to %s", user.getUserId(), action);
		SecurityLogger.logInfo(TagAjax.class, message);
		return new DotSecurityException(message);
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

		checkCanEditUserTags(getLoggedInUser(), userId);
		try {
			Tag tag = APILocator.getTagAPI().getTagByTagId(tagNameOrId);

			if ( !UtilMethods.isSet(tag) || !UtilMethods.isSet(tag.getTagId()) ) {
				tag = APILocator.getTagAPI().getTagByNameAndHost(tagNameOrId, Host.SYSTEM_HOST);
			}

			if ( tag == null || !UtilMethods.isSet(tag.getTagId()) ) {
				Logger.warn(this, "Requested Tag [" + tagNameOrId + "] for deletion was not found");
			} else {

                /*
				Removes the relationship between a tag and an inode.
                 NOTE: if the tag does not have more relationships the Tag itself will be remove it.
                 */
				APILocator.getTagAPI().removeTagRelationAndTagWhenPossible(tag.getTagId(), userId, null);
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

	/**
	 * Creates the tags listed in a CSV file, one {@code tag name,host id} pair per line. Needs
	 * access to one of the Tags portlets.
	 *
	 * @param uploadFile contents of the CSV file
	 * @return the number of lines that failed under {@code importSomeTagsFailed}, if any
	 * @throws DotSecurityException if the logged in user can't manage tags
	 * @throws DotDataException     if the user's portlet access can't be resolved
	 */
	public static Map<String,Object> importTags(byte[] uploadFile) throws DotDataException, DotSecurityException {

		checkCanManageTags(getLoggedInUser());

		Map<String,Object> callbackData = new HashMap<>();
		int counterFailedTags = 0;
		try {
			String content = new String(uploadFile);
			StringReader sr = new StringReader(content);
			BufferedReader br = new BufferedReader(sr);


			String str;
		    while ((str = br.readLine()) != null) {
		    	String[] tokens = str.split(",");
		    	if(tokens.length!=2 || tokens[0].isEmpty()) {
		    		counterFailedTags++;
		    		Logger.error(TagAjax.class, "Tag can not be imported because the tag_name or the host_id is empty, trying with the next Tag");
		    		continue;
		    	}
		    	String tagName = tokens[0];
		    	String hostId = tokens[1];
		    	if(!tagName.toLowerCase().contains("tag name") && !hostId.toLowerCase().contains("host id")){
		    		tagName = tagName.replaceAll("\'|\"", "");
		    		hostId = hostId.replaceAll("\'|\"", "");
		    		try {
					    APILocator.getTagAPI().getTagAndCreate(tagName, "", hostId);
					}catch(GenericTagException e){
						counterFailedTags++;
						Logger.error(TagAjax.class,
								"Tag (name:"+tagName+" - host ID:"+hostId+") can not be imported because "+e.getMessage()+ ", trying with the next Tag");
					}
				}
		    }

		    if(counterFailedTags>0){
		    	callbackData.put("importSomeTagsFailed",counterFailedTags);
			}

			br.close();

		} catch(Exception e) {
			//Logging the error because DWR tends to swallow the exceptions
			Logger.error(TagAjax.class, "Error importing tags", e);
		}
		return callbackData;
	}
}
