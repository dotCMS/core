package com.dotmarketing.cms.content.submit.util;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.*;
import com.dotmarketing.util.WebKeys.WorkflowStatuses;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This Util class generate the content
 * @author Oswaldo
 *
 */
@Deprecated
public class SubmitContentUtil {

	public static final String errorFieldVariable = "errorFieldMessage";
	private static FieldAPI fieldAPI = APILocator.getFieldAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static ContentletAPI conAPI = APILocator.getContentletAPI();
	@SuppressWarnings("unchecked")
	private static PermissionAPI perAPI = APILocator.getPermissionAPI();
	private static RelationshipAPI relAPI = APILocator.getRelationshipAPI();
	private static RoleAPI roleAPI = APILocator.getRoleAPI();
	private static final String ROOT_FILE_FOLDER = "/submitted_content/";
	private static String[] dateFormats = new String[] { "yyyy-MM-dd", "yyyy-MM-dd HH:mm", "d-MMM-yy", "MMM-yy", "MMMM-yy", "d-MMM", "dd-MMM-yyyy", "MM/dd/yyyy hh:mm aa", "MM/dd/yy HH:mm",
		"MM/dd/yyyy HH:mm", "MMMM dd, yyyy", "M/d/y", "M/d", "EEEE, MMMM dd, yyyy", "MM/dd/yyyy",
		"hh:mm:ss aa", "HH:mm:ss"};
	private static String customDatePattern = "";
	private static String customDateTimePattern = "";

	/**
	 * Get the user if the user is not logged return default AnonymousUser
	 * @param userId The userId
	 * @return User
	 * @exception DotDataException
	 */
	public static User getUserFromId(String userId) throws DotDataException{
		User user = null;
		try {
			if(UtilMethods.isSet(userId)){

				user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),true);

			}else{
				user =APILocator.getUserAPI().getAnonymousUser();
			}
		} catch (NoSuchUserException e) {
			Logger.error(SubmitContentUtil.class, e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(SubmitContentUtil.class, e.getMessage(),e);
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
	private static Map<Relationship,List<Contentlet>> getRelationships(Structure structure, Contentlet contentlet, String parametersOptions,List<String> parametersName,List<String[]> values, User user) throws DotDataException, DotSecurityException{
		LanguageAPI lAPI = APILocator.getLanguageAPI();
		Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<Relationship, List<Contentlet>>();
		if(contentlet == null)
			return contentRelationships;
		List<Relationship> rels = FactoryLocator.getRelationshipFactory().byContentType(contentlet.getStructure());
		for (Relationship rel : rels) {

			String[] opt = parametersOptions.split(";");
			for(String text: opt){
				if(text.indexOf(rel.getRelationTypeValue()) != -1){

					String[] identArray = text.substring(text.indexOf("=")+1).replaceAll("\\[", "").replaceAll("\\]", "").split(",");

					List<Contentlet> cons = conAPI.findContentletsByIdentifiers(identArray, true, lAPI.getDefaultLanguage().getId(), user, true);
					if(cons.size()>0){
						contentRelationships.put(rel, cons);
					}
				}
			}
			for(int i=0; i < parametersName.size(); i++){
				String fieldname = parametersName.get(i);
				String[] fieldValue = values.get(i);
				if(fieldname.indexOf(rel.getRelationTypeValue()) != -1){
					List<Contentlet> cons = conAPI.findContentletsByIdentifiers(fieldValue, true, lAPI.getDefaultLanguage().getId(), user, true);
					if(cons.size()>0){
						if(contentRelationships.containsKey(rel)){
							cons.addAll(contentRelationships.get(rel));
						}
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
	private static void addFileToContentlet(Contentlet contentlet, Field field,Host host, java.io.File uploadedFile, User user, String title)throws DotSecurityException, DotDataException{
		String folderPath = ROOT_FILE_FOLDER+contentlet.getStructure().getName();
		try {
			FileAsset file = saveFile(user,host,uploadedFile,folderPath, title);
			contentlet.setStringProperty(field.getVelocityVarName(), file.getIdentifier());
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

		Folder folder = APILocator.getFolderAPI().findFolderByPath(folderPath, host,user,false);
		if(!UtilMethods.isSet(folder.getInode())){
			User systemUser = APILocator.getUserAPI().getSystemUser();
			folder = APILocator.getFolderAPI().createFolders(folderPath, host,APILocator.getUserAPI().getSystemUser(),false);
		}

		byte[] bytes = FileUtil.getBytes(uploadedFile);

		if (bytes!=null) {
			String newFileName = "";
			String name = UtilMethods.getFileName(title);
			int counter = 1;
			String fileName = name + "." + UtilMethods.getFileExtension(title);

			while(APILocator.getFileAssetAPI().fileNameExists(host,folder, name, "")) {
				newFileName  = name +"("+ counter+")";
				fileName = newFileName + "." + UtilMethods.getFileExtension(title);
				counter++;
			}
            if(UtilMethods.isSet(newFileName)){
            	name = newFileName;
            }
            Contentlet cont = new Contentlet();
			cont.setStructureInode(folder.getDefaultFileType());
			cont.setStringProperty(FileAssetAPI.TITLE_FIELD, UtilMethods.getFileName(name));
			cont.setFolder(folder.getInode());
			cont.setHost(host.getIdentifier());
			cont.setBinary(FileAssetAPI.BINARY_FIELD, uploadedFile);
			if(CacheLocator.getContentTypeCache().getStructureByInode(cont.getStructureInode()).getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET)
				cont.setStringProperty("fileName", title);
			cont = APILocator.getContentletAPI().checkin(cont, user, true);
			if(APILocator.getPermissionAPI().doesUserHavePermission(cont, PermissionAPI.PERMISSION_PUBLISH, user))
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
				if(field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
					value = VelocityUtil.cleanVelocity(values[0]);
					Host host = APILocator.getHostAPI().find(value, APILocator.getUserAPI().getSystemUser(), false);
					if(host!=null && InodeUtils.isSet(host.getIdentifier())){
						contentlet.setHost(host.getIdentifier());
						contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
					}else{
						Folder folder = APILocator.getFolderAPI().find(value, APILocator.getUserAPI().getSystemUser(), false);
						if(folder!=null && InodeUtils.isSet(folder.getInode())){
							contentlet.setHost(folder.getHostId());
							contentlet.setFolder(folder.getInode());
						}
					}
				}else if(field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) || field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())){
					if (field.getFieldContentlet().startsWith("float") || field.getFieldContentlet().startsWith("integer")) {
						value = values[0];
					} else {
						for(String temp : values){
							value = temp+","+value;
						}
					}
				}else if(field.getFieldType().equals(Field.FieldType.DATE.toString())){
					value = VelocityUtil.cleanVelocity(values[0]);
					if(value instanceof String){
						if(UtilMethods.isSet(customDatePattern)){
							Date dateValue = new SimpleDateFormat(customDatePattern).parse(value);
							conAPI.setContentletProperty(contentlet, field, dateValue);
							return;
						}
						value = value+" 00:00:00";
					}
				}else if(field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())){
					value = VelocityUtil.cleanVelocity(values[0]);
					if(value instanceof String){
						if(UtilMethods.isSet(customDateTimePattern)){
							Date dateTimeValue = new SimpleDateFormat(customDateTimePattern).parse(value);
							conAPI.setContentletProperty(contentlet, field, dateTimeValue);
							return;
						}
					}
				} else {

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
	private static Contentlet setAllFields(String structureVelVarName, List<String> parametersName, List<String[]> values) throws DotDataException{
		LanguageAPI lAPI = APILocator.getLanguageAPI();
		Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(structureVelVarName);
		String contentletInode = null;
		long contentLanguageId = 1;
		Field fileField = new Field(),imageField=new Field(),binaryField=new Field();
		List<Field> fields = FieldsCache.getFieldsByStructureInode(st.getInode());
		for (Field field : fields) {
			if(parametersName.contains(field.getVelocityVarName()+"oldFileInode"))
				fileField =field;
			else if(parametersName.contains(field.getVelocityVarName()+"oldImageInode"))
				imageField= field;
			else if(parametersName.contains(field.getVelocityVarName()+"oldBinaryInode"))
				binaryField= field;
		}
		
		for(int i=0; i < parametersName.size(); i++){
			if(parametersName.get(i).equals("customDatePattern"))
				customDatePattern = values.get(i)[0];
			if(parametersName.get(i).equals("customDateTimePattern"))
				customDateTimePattern = values.get(i)[0];
			if(parametersName.get(i).equals("contentLanguageId"))
				contentLanguageId = Long.parseLong(values.get(i)[0]);
		}
		
			 
		Contentlet contentlet = new Contentlet();
		contentlet.setStructureInode(st.getInode());
		contentlet.setLanguageId(contentLanguageId);

		for(int i=0; i < parametersName.size(); i++){
			String fieldname = parametersName.get(i);
			String[] fieldValue = values.get(i);
			setField(st, contentlet, fieldname, fieldValue);
			
			//To Update Content
			if(fieldname.equalsIgnoreCase("contentIdentifier"))
				contentlet.setIdentifier(VelocityUtil.cleanVelocity(values.get(i)[0]));
			else if(fieldname.equalsIgnoreCase("contentInode"))
				contentletInode = VelocityUtil.cleanVelocity(values.get(i)[0]);
			else if(fieldname.equalsIgnoreCase(fileField.getVelocityVarName()+"oldFileInode")){
				if(UtilMethods.isSet(VelocityUtil.cleanVelocity(values.get(i)[0]))){
					APILocator.getContentletAPI().setContentletProperty(contentlet, fileField, VelocityUtil.cleanVelocity(values.get(i)[0]));
				}
			}
			else if(fieldname.equalsIgnoreCase(imageField.getVelocityVarName()+"oldImageInode")){
				if(UtilMethods.isSet(VelocityUtil.cleanVelocity(values.get(i)[0]))){
					APILocator.getContentletAPI().setContentletProperty(contentlet, imageField, VelocityUtil.cleanVelocity(values.get(i)[0]));
				}
			}
			else if(fieldname.equalsIgnoreCase(binaryField.getVelocityVarName()+"oldBinaryInode")){
				if(UtilMethods.isSet(VelocityUtil.cleanVelocity(values.get(i)[0]))){
					User user = APILocator.getUserAPI().getSystemUser();
					try {
						File newFile = APILocator.getContentletAPI().getBinaryFile(contentletInode, binaryField.getVelocityVarName(), user);
						contentlet.setBinary(binaryField.getVelocityVarName(), newFile);
					} catch (Exception e) {
						Logger.debug(SubmitContentUtil.class, e.getMessage());
					} 
				}
			}
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
	public static void createWorkFlowTask(Contentlet contentlet, String userId, String moderatorRole) throws DotDataException{

		User user = getUserFromId(userId);
		StringBuffer changeHist = new StringBuffer("Task Added<br>");
		WorkflowTask task = new WorkflowTask();

		changeHist.append("Title: " + UtilHTML.escapeHTMLSpecialChars(contentlet.getTitle()) + "<br>");
		task.setTitle("A new content titled: " + UtilHTML.escapeHTMLSpecialChars(contentlet.getTitle())+ " has been posted.");
		task.setDescription("A new content titled \"" + UtilHTML.escapeHTMLSpecialChars(contentlet.getTitle().trim()) +
				"\" has been posted by " + UtilHTML.escapeHTMLSpecialChars(user.getFullName()) + " ("+user.getEmailAddress()+")");
		changeHist.append("Description: " + UtilHTML.escapeHTMLSpecialChars(task.getDescription()) + "<br>");

		Role role = roleAPI.loadRoleByKey(moderatorRole);
		task.setBelongsTo(role.getId());
		task.setAssignedTo("Nobody");
		task.setModDate(new Date());
		task.setCreationDate(new Date());
		task.setCreatedBy(user.getUserId());

		task.setStatus(WorkflowStatuses.OPEN.toString());
		changeHist.append("Due Date: " + UtilMethods.dateToHTMLDate(task.getDueDate()) + " -> <br>");
		task.setDueDate(null);
		task.setWebasset(contentlet.getInode());
		task.setLanguageId(contentlet.getLanguageId());

		//HibernateUtil.saveOrUpdate(task);

		//Save the work flow comment
		WorkflowComment taskComment = new WorkflowComment ();
		taskComment.setComment(task.getDescription());
		taskComment.setCreationDate(new Date());
		taskComment.setPostedBy(user.getUserId());
		HibernateUtil.saveOrUpdate(taskComment);
		relAPI.addRelationship(task.getInode(), taskComment.getInode(), "child");

		//Save the work flow history
		WorkflowHistory hist = new WorkflowHistory ();
		hist.setChangeDescription("Task Creation");
		hist.setCreationDate(new Date ());
		hist.setMadeBy(user.getUserId());
		HibernateUtil.saveOrUpdate(hist);
		relAPI.addRelationship(task.getInode(), hist.getInode(), "child");

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
	 * @param formHost host for form contentlet
	 * @return Contentlet
	 * @throws DotContentletStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@SuppressWarnings("unchecked")
	public static Contentlet createContent(Structure st, ArrayList<Category> cats, String userId, List<String> parametersName,List<String[]> values, String options,List<Map<String,Object>> fileParameters, boolean autoPublish, Host formHost) throws DotContentletStateException, DotDataException, DotSecurityException{
		return createContent(st, cats, userId, parametersName, values, options, fileParameters, autoPublish, formHost, null);
	}

	@SuppressWarnings("unchecked")
	public static Contentlet createContent(Structure st, ArrayList<Category> cats, String userId, List<String> parametersName,List<String[]> values, String options,List<Map<String,Object>> fileParameters, boolean autoPublish, Host formHost, String moderatorRole) throws DotContentletStateException, DotDataException, DotSecurityException{

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
		contentlet = SubmitContentUtil.setAllFields(st.getVelocityVarName(), parametersName, values);
		contentlet.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());


		/**
		 * Get the required relationships
		 */
		Map<Relationship,List<Contentlet>> relationships = SubmitContentUtil.getRelationships(st, contentlet, options,parametersName,values, user);


		/**
		 * Validating content fields
		 *
		 */
		//conAPI.validateContentlet(contentlet,relationships,cats);

		/**
		 * Set the binary field values
		 * http://jira.dotmarketing.net/browse/DOTCMS-3463
		 *
		 */
		if(fileParameters.size() > 0){
			for(Map<String,Object> value : fileParameters){
				Field field = (Field)value.get("field");
				java.io.File file = (java.io.File)value.get("file"); 
				if(file!=null){
					try {
						contentlet.setBinary(field.getVelocityVarName(), file);
					} catch (IOException e) {

					}
				}
				else if(field.isRequired()) {
				    DotContentletValidationException cve = new DotContentletValidationException("Contentlet's fields are not valid");
				    cve.addRequiredField(field);
				    throw cve;
				}
		     }
		}

		if (st.getStructureType() == Structure.STRUCTURE_TYPE_FORM) {
			contentlet.setHost(formHost.getIdentifier());
			Host host = APILocator.getHostAPI().find(formHost.getIdentifier(), APILocator.getUserAPI().getSystemUser(), false);
			if (!perAPI.doesUserHavePermissions(host,"PARENT:"+PermissionAPI.PERMISSION_READ+", CONTENTLETS:"+PermissionAPI.PERMISSION_WRITE+"", user)) {
				throw new DotSecurityException("User doesn't have write permissions to Contentlet");
			}
		}

		/**
		 * If the moderator field is set, a work flow task is created
		 */
		if(UtilMethods.isSet(moderatorRole)){

			if(!UtilMethods.isSet(contentlet.getActionId()))
				contentlet.setActionId(APILocator.getWorkflowAPI().findEntryAction(contentlet, user).getId());

			String contentletTitle = "";

	        List<Field> fields = FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());

	        for (Field fld : fields) {
	                if(fld.isListed()){
	                    contentletTitle = contentlet.getMap().get(fld.getVelocityVarName()).toString();
	                    contentletTitle = contentletTitle.length() > 250 ? contentletTitle.substring(0,250) : contentletTitle;
	                }
	        }
			contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, "A new content titled \"" + UtilHTML.escapeHTMLSpecialChars(contentletTitle.trim()) +
					"\" has been posted by " + UtilHTML.escapeHTMLSpecialChars(user.getFullName()) + " ("+user.getEmailAddress()+")");

			contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, roleAPI.loadRoleByKey(moderatorRole).getId());
		}
		
		/*
		* Saving file and images
		*/
		if(fileParameters.size() > 0) { 
		    for(Map<String,Object> value : fileParameters) {
		         Field field = (Field)value.get("field");
		         //http://jira.dotmarketing.net/browse/DOTCMS-3463
		         if(field.getFieldType().equals(Field.FieldType.IMAGE.toString())|| 
		                 field.getFieldType().equals(Field.FieldType.FILE.toString())){
		           java.io.File uploadedFile = (java.io.File)value.get("file");
		           try {
		             if(!UtilMethods.isSet(FileUtil.getBytes(uploadedFile)))
		               continue;
		           } catch (IOException e) {
		               Logger.error(SubmitContentUtil.class, e.getMessage());
		           }
		           String title = (String)value.get("title");
		           Host host = (Host)value.get("host");
		           addFileToContentlet(contentlet, field,host, uploadedFile, user, title);
		         }
            }
		}

		/**
		 * Saving Content
		 */
		contentlet = conAPI.checkin(contentlet, relationships, cats, permissionList, user, true);

		if(autoPublish)
		    APILocator.getVersionableAPI().setLive(contentlet);

		return contentlet;
	}

	/**
	 * Check if a para is tupe file or image
	 * @param structure
	 * @param paramName
	 * @return boolean
	 */
	public static boolean imageOrFileParam(Structure structure, String paramName){

		Field field = structure.getFieldVar(paramName);
		if(UtilMethods.isSet(field) && (field.getFieldType().equals(Field.FieldType.FILE.toString()) || field.getFieldType().equals(Field.FieldType.IMAGE.toString()) || field.getFieldType().equals(Field.FieldType.BINARY.toString()))){
			return true;
		}
		return false;
	}

	/**
	 * Get the specified field message. If the field attribute exist
	 * @param field Field
	 * @param fieldAttribute Name of the field attribute
	 * @return String
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public static String getCustomizedFieldErrorMessage(Field field, String fieldAttribute) throws DotDataException, DotSecurityException{
		String errorMessage = null;
		List<FieldVariable> fieldVariables = fieldAPI.getFieldVariablesForField(field.getInode(), userAPI.getSystemUser(), false);		
		for(FieldVariable fv : fieldVariables){
			if(fv.getKey().equals(fieldAttribute)){
				errorMessage = fv.getValue();
				break;
			}
		}
		return errorMessage;
	}

}
