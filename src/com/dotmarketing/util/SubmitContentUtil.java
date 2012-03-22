package com.dotmarketing.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.WebKeys.WorkflowStatuses;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * This Util class generate the content
 * @author Oswaldo
 *
 */
public class SubmitContentUtil {

	private static final FileAPI fileAPI = APILocator.getFileAPI();
	private static ContentletAPI conAPI = APILocator.getContentletAPI();
	@SuppressWarnings("unchecked")
	private static PermissionAPI perAPI = APILocator.getPermissionAPI();
	private static RelationshipAPI relAPI = APILocator.getRelationshipAPI();
	private static final String ROOT_FILE_FOLDER = "/submitted_content/";
	private static LanguageAPI langAPI = APILocator.getLanguageAPI();

	/**
	 * Get the user if the user is not logged return default AnonymousUser
	 * @param userId The userId
	 * @return User
	 * @exception DotDataException
	 */
	public static User getUserFromId(String userId) throws DotDataException{
		User user = null;

		if(UtilMethods.isSet(userId)){
			try {
				user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			} catch (Exception e) {
				Logger.error(SubmitContentUtil.class, e.getMessage(), e);
				throw new DotDataException(e.getMessage(), e);
			}
		}else{
			user = APILocator.getUserAPI().getAnonymousUser();
		}

		return user;
	}

	/**
	 * Get the list of contents by relationship if exists. 
	 * @param structure The content structure
	 * @param contentlet The content
	 * @param parametersOptions The macro form options parameters
	 * @return Map<Relationship,List<Contentlet>>
	 * @throws DotSecurityException 
	 **/
	private static Map<Relationship,List<Contentlet>> getRelationships(Structure structure, Contentlet contentlet, String parametersOptions, User user) throws DotDataException, DotSecurityException{

		Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<Relationship, List<Contentlet>>();
		if(contentlet == null)
			return contentRelationships;
		List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(contentlet.getStructure());
		for (Relationship rel : rels) {

			String[] opt = parametersOptions.split(";");
			for(String text: opt){
				if(text.indexOf(rel.getRelationTypeValue()) != -1){

					String[] identArray = text.substring(text.indexOf("=")+1).replaceAll("\\[", "").replaceAll("\\]", "").split(",");

					List<Contentlet> cons = conAPI.findContentletsByIdentifiers(identArray, true, langAPI.getDefaultLanguage().getId(), user, true);
					if(cons.size()>0){
						contentRelationships.put(rel, cons);
					}
				}
			}
		}
		return contentRelationships;
	}

	/**
	 * Adds a image or file to a content
	 * @param contentlet
	 * @param uploadedFile
	 * @param user
	 * @throws DotDataException 
	 * @throws DotSecurityExceptionlanguageId
	 */
	private static Contentlet addFileToContentlet(Contentlet contentlet, Field field,Host host, java.io.File uploadedFile, User user, String title)throws DotSecurityException, DotDataException{
		String identifier = String.valueOf(contentlet.getIdentifier());
		String folderPath = ROOT_FILE_FOLDER+contentlet.getStructure().getName()+"/"+identifier.substring(0, 1)+"/"+identifier.substring(1, 2)+"/"+identifier+"/";
		try {
			FileAsset file = saveFile(user,host,uploadedFile,folderPath, title);
			conAPI.setContentletProperty(contentlet, field, ((FileAsset)file).getIdentifier());
			return contentlet;
		} catch (Exception e) {
			Logger.error(SubmitContentUtil.class, e.getMessage());
			throw new DotDataException("File could not be saved. "+e.getMessage());
		}
	}

	/**
	 * Save the file uploaded
	 * @param user the user that save the file
	 * @param host Current host
	 * @param uploadedFile
	 * @param folder The folder where the file is going to be save
	 * @param title The filename
	 * @return File
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private static FileAsset saveFile(User user, Host host, java.io.File uploadedFile, String folderPath, String title) throws Exception {

		Folder folder = APILocator.getFolderAPI().findFolderByPath(folderPath, host,APILocator.getUserAPI().getSystemUser(),false);
		if(!InodeUtils.isSet(folder.getInode() )){
			folder = APILocator.getFolderAPI().createFolders(folderPath, host,user,false);
			Permission newPerm = new Permission();
			newPerm.setPermission(perAPI.PERMISSION_PUBLISH);
			newPerm.setRoleId(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
			newPerm.setInode(folder.getInode());
			perAPI.save(newPerm, folder, APILocator.getUserAPI().getSystemUser(), false);
		}

		byte[] bytes = FileUtil.getBytes(uploadedFile);

		if (bytes!=null) {

			String name = UtilMethods.getFileName(title);
			int counter = 1;
			while(fileAPI.fileNameExists(folder, name)) {
				name = name + counter;
				counter++;
			}
			while(APILocator.getFileAssetAPI().fileNameExists(host,folder, name, "")) {
				name = name + counter;
				counter++;
			}
			
			Contentlet cont = new Contentlet();
			cont.setStructureInode(folder.getDefaultFileType());
			cont.setStringProperty(FileAssetAPI.TITLE_FIELD, UtilMethods.getFileName(name));
			cont.setFolder(folder.getInode());
			cont.setHost(host.getIdentifier());
			cont.setBinary(FileAssetAPI.BINARY_FIELD, uploadedFile);
			APILocator.getContentletAPI().checkin(cont, user,false);
			APILocator.getVersionableAPI().setLive(cont);
			return APILocator.getFileAssetAPI().fromContentlet(cont);
	

		}

		return null;

	}

	/**
	 * Set the field value, to a content according the content structure
	 * @param structure The content structure
	 * @param contentlet The content
	 * @param fieldName The field name
	 * @param value The field value
	 * @throws DotDataException
	 */
	private static void setField(Structure structure, Contentlet contentlet, String fieldName, String[] values) throws DotDataException{

		Field field = structure.getFieldVar(fieldName);
		String value="";
		if(UtilMethods.isSet(field) && APILocator.getFieldAPI().valueSettable(field)){
			try{
				if(field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) || field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())){
					for(String temp : values){
						value = temp+","+value;
					}
				}else {
					value = VelocityUtil.cleanVelocity(values[0]);
				}
				conAPI.setContentletProperty(contentlet, field, value);

			}catch(Exception e){
				Logger.debug(SubmitContentUtil.class, e.getMessage());	
			}
		}
	}

	/**
	 * Create a new content, setting the content values with the specified list of param values
	 * @param structureName The content structure name
	 * @param parametersName The fields names
	 * @param values The fields values
	 * @return Contentlet
	 * @throws DotDataException
	 */
	private static Contentlet setAllFields(String structureName, List<String> parametersName, List<String[]> values) throws DotDataException{

		Structure st = StructureCache.getStructureByName(structureName);
		Contentlet contentlet = new Contentlet();
		contentlet.setStructureInode(st.getInode());
		contentlet.setLanguageId(langAPI.getDefaultLanguage().getId());

		for(int i=0; i < parametersName.size(); i++){
			String fieldname = parametersName.get(i);
			String[] fieldValue = values.get(i);
			setField(st, contentlet, fieldname, fieldValue);
		}

		return contentlet;
	}
	

	/**
	 * Create a work flow task for the new content created and send a email to the corresponding role moderator users
	 * @param contentlet The content
	 * @param user The user that add the content
	 * @param moderatorRole The role to assign the work flow
	 * @throws DotDataException 
	 * @throws DotDataException
	 */
	public static void createWorkFlowTask(Contentlet contentlet, String userId, String moderatorRoleId) throws DotDataException{

		User user = getUserFromId(userId);
		StringBuffer changeHist = new StringBuffer("Task Added<br>");
		WorkflowTask task = new WorkflowTask();

		changeHist.append("Title: " + UtilHTML.escapeHTMLSpecialChars(contentlet.getTitle()) + "<br>");
		task.setTitle("A new content titled: " + UtilHTML.escapeHTMLSpecialChars(contentlet.getTitle())+ " has been posted.");
		task.setDescription("A new content titled \"" + UtilHTML.escapeHTMLSpecialChars(contentlet.getTitle().trim()) + 
				"\" has been posted by " + UtilHTML.escapeHTMLSpecialChars(user.getFullName()) + " ("+user.getEmailAddress()+")");
		changeHist.append("Description: " + UtilHTML.escapeHTMLSpecialChars(task.getDescription()) + "<br>");

		Role role = APILocator.getRoleAPI().loadRoleById(moderatorRoleId);
		task.setBelongsTo(role.getId());
		task.setAssignedTo("Nobody");
		task.setModDate(new Date());
		task.setCreationDate(new Date());
		
		task.setStatus(WorkflowStatuses.OPEN.toString());
		changeHist.append("Due Date: " + UtilMethods.dateToHTMLDate(task.getDueDate()) + " -> <br>");
		task.setDueDate(null);
		task.setWebasset(contentlet.getInode());
		//APILocator.getWorkflowAPI().saveWorkflowTask(task);

		//Save the work flow comment
		WorkflowComment taskComment = new WorkflowComment ();
		taskComment.setComment(task.getDescription());
		taskComment.setCreationDate(new Date());
		taskComment.setPostedBy(user.getUserId());
		taskComment.setWorkflowtaskId(task.getId());
		APILocator.getWorkflowAPI().saveComment(taskComment);

		//Save the work flow history
		WorkflowHistory hist = new WorkflowHistory ();
		hist.setChangeDescription("Task Creation");
		hist.setCreationDate(new Date ());
		hist.setMadeBy(user.getUserId());
		hist.setWorkflowtaskId(task.getId());
		APILocator.getWorkflowAPI().saveWorkflowHistory(hist);
		
		//WorkflowEmailUtil.sendWorkflowChangeEmails (task, "New user content has been submitted", "New Task", null);        


	}

	/**
	 * This method read the parameters an create a new content with the categories and relationships
	 * specified.
	 * @param st	Structure
	 * @param cats  Category list
	 * @param userId	UserId
	 * @param parametersName	List of structure fields name
	 * @param values	List of fields values
	 * @param options	String with flags and relationship options
	 * @param autoPublish Boolean to publish or not the content
	 * @return Contentlet
	 * @throws DotContentletStateException
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	@SuppressWarnings("unchecked")
	public static Contentlet createContent(Structure st, ArrayList<Category> cats, String userId, List<String> parametersName,List<String[]> values, String options,List<Map<String,Object>> fileParameters, boolean autoPublish) throws DotContentletStateException, DotDataException, DotSecurityException{

		Contentlet contentlet = null;

		/*try {*/
		/**
		 * Get the current user
		 */
		User user = getUserFromId(userId);

		/**
		 * Content inherit structure permissions
		 */
		List<Permission> permissionList = perAPI.getPermissions(st);

		/**
		 * Set the content values
		 */
		contentlet = SubmitContentUtil.setAllFields(st.getName(), parametersName, values);

		/**
		 * Get the required relationships
		 */
		Map<Relationship,List<Contentlet>> relationships = SubmitContentUtil.getRelationships(st, contentlet, options, user);

		/**
		 * Validating content fields
		 */
		conAPI.validateContentlet(contentlet,relationships,cats);

		/**
		 * Saving Content
		 */
		contentlet = conAPI.checkin(contentlet, relationships, cats, permissionList, user, true);
		
		if(autoPublish)
		    APILocator.getVersionableAPI().setLive(contentlet);

		/**
		 * Saving file and images
		 */

		if(fileParameters.size() > 0){

			for(Map<String,Object> value : fileParameters){
				Field field = (Field)value.get("field");
				java.io.File uploadedFile = (java.io.File)value.get("file");
				String title = (String)value.get("title");
				Host host = (Host)value.get("host");
				contentlet = addFileToContentlet(contentlet, field,host, uploadedFile, user, title);
			}
			contentlet = conAPI.checkinWithoutVersioning(contentlet, relationships, cats, permissionList, user, true);
		}

		/*}catch(Exception e){

			Logger.error(SubmitContentUtil.class, e.getMessage());
			throw new DotContentletStateException("Unable to perform checkin. "+e.getMessage());

		}*/

		return contentlet;
	}



}
