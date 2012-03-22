package com.dotmarketing.portlets.calendar.cms.action;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.calendar.cms.struts.EventForm;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.servlet.UploadServletRequest;

/**
 * 
 * Action that let you submit an event from the frontend and it will saved into the cms
 * check the struts-cms.xml to see how this action is mapped to struts
 * 
 * @author Roger Marin
 * @author David Torres
 * 
 */
public class AddEvent extends DispatchAction
{
	private EventAPI eventAPI;
	private CategoryAPI catAPI;
	private UserWebAPI userAPI;
	private LanguageAPI langAPI;
	private HostWebAPI hostWebAPI;
	private PermissionAPI perAPI;
	private ContentletAPI conAPI;
	
	public AddEvent(){
	   eventAPI = APILocator.getEventAPI();
	   catAPI = APILocator.getCategoryAPI();
	   userAPI = WebAPILocator.getUserWebAPI();
	   langAPI = APILocator.getLanguageAPI();
	   hostWebAPI = WebAPILocator.getHostWebAPI();
	   perAPI = APILocator.getPermissionAPI();
	   conAPI = APILocator.getContentletAPI();
	}
	
	public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		return mapping.findForward("addEvent");
	}
	
	public ActionForward submitEvent(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception{
		Logger.debug(AddEvent.class, "Saving Calendar Event");
		try{
			HibernateUtil.startTransaction();
			String path= Config.getStringProperty("CALENDAR_FILES_PATH");
			User currentUser = userAPI.getLoggedInUser(request);
			boolean respectFrontendRoles = !userAPI.isLoggedToBackend(request);
			ActionErrors ae = new ActionErrors();

			
			if (!UtilMethods.isSet(currentUser)) {
				boolean allowEventWithoutUser = Config.getBooleanProperty("ADD_EVENT_WITHOUT_USER",false);
				if(allowEventWithoutUser)
				{
					currentUser = APILocator.getUserAPI().getSystemUser();
				}
				else
				{
					return new ActionForward("/dotCMS/login?referrer="+mapping.findForward("addEvent").getPath(),true);
				}
			}
			
			String startDateDate = request.getParameter("startDateDate");
			String startDateTime = request.getParameter("startDateTime");
			String endDateDate = request.getParameter("endDateDate");
			String endDateTime = request.getParameter("endDateTime");
			String description = request.getParameter("description");
			String[] categoriesArray = request.getParameterValues("categories");
			String title = request.getParameter("title");
			String tags = request.getParameter("tags");
			String location = request.getParameter("location");
			String link = request.getParameter("link");
			String options = 
				(request.getParameter("options") != null?PublicEncryptionFactory.decryptString(request.getParameter("options")):"").replaceAll(" ", ""); 
				
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
			SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("MM/dd/yyyy");
			
			Date startDate = null;
			Date endDate = null;
			try {
				startDate = dateFormat.parse(startDateDate + " " + startDateTime);
			} catch (ParseException e) {
				ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.invalid", "From date"));
				saveMessages(request, ae);				
				return mapping.findForward("addEvent");
			}
			
			try {
				endDate = dateFormat.parse(endDateDate + " " + endDateTime);
			} catch (ParseException e) {
				ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.invalid", "To date"));
				saveMessages(request, ae);				
				return mapping.findForward("addEvent");
			}
			
			try {
				endDate = dateFormat.parse(endDateDate + " " + endDateTime);
			} catch (ParseException e) {
				ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.invalid", "To date"));
				saveMessages(request, ae);				
				return mapping.findForward("addEvent");
			}
			
			if(!request.getParameter("recurrenceOccurs").equals("never")){
				try {
					dateOnlyFormat.parse(request.getParameter("recurrenceEnds"));
				} catch (ParseException e) {
					ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.invalid", "Recurrence end date"));
					saveMessages(request, ae);				
					return mapping.findForward("addEvent");
				}
			}			
			
			//Checking for the folder to store the submitted files
			Host host = hostWebAPI.getCurrentHost(request);
			Event event = new Event();
			Language language = langAPI.getDefaultLanguage();
			Folder folder = APILocator.getFolderAPI().findFolderByPath(path, host,APILocator.getUserAPI().getSystemUser(),false);
            Structure structure = StructureCache.getStructureByName("Event");
            event.setStructureInode(structure.getInode());
			if (!InodeUtils.isSet(folder.getInode())){
				folder = APILocator.getFolderAPI().createFolders(path, host,userAPI.getSystemUser(),false);
			}

			List<Category> categoriesList  =  new ArrayList<Category>();
			if (categoriesArray != null) {
				for (String cat : categoriesArray ) {
						Category node = (Category) catAPI.find(cat, currentUser, respectFrontendRoles);
						if(node!=null){
						  categoriesList.add(node);
					 } 
				}
			}
			
			event.setStartDate(startDate);
			event.setEndDate(endDate);
			event.setTitle(title);
			event.setTags(tags);
			event.setLocation(location);
			event.setLink(link);
			event.setDescription(description);
			event.setLanguageId(language.getId());
				
			FileAsset cmsFile = null;
			FileAsset cmsImage = null;
			
			//Get file type parameters
			if (request instanceof UploadServletRequest)
			{
				UploadServletRequest uploadReq = (UploadServletRequest) request;
				
				java.io.File file = uploadReq.getFile("file");
				java.io.File image = uploadReq.getFile("image");
				
				if(file != null && file.length() > 0) {
					String fileName = uploadReq.getFileName("file");
					cmsFile = saveFile(currentUser, host, file, folder, fileName);
					event.setProperty("file", cmsFile.getIdentifier());
				}
					
				if(image != null && image.length() > 0) {
					String fileName = uploadReq.getFileName("image");
					cmsImage = saveFile(currentUser, host, image, folder, fileName);
					event.setProperty("image", cmsImage.getIdentifier());
				}					
					
			}	

			try {
				PermissionAPI perAPI = APILocator.getPermissionAPI();
				List<Permission> pers = perAPI.getPermissions(event.getStructure());
				APILocator.getContentletAPI().checkin(event, categoriesList, pers, currentUser, false);
				APILocator.getVersionableAPI().setWorking(event);
			} catch (DotContentletValidationException ex) {
				
				Map<String, List<Field>> fields = ex.getNotValidFields();
				List<Field> reqFields = fields.get("required");
				for(Field f : reqFields) {
					if(!f.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {
						ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required", f.getFieldName()));
					} else {
						ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.category.required", f.getFieldName()));
					}
				}
				
				saveMessages(request, ae);				
				return mapping.findForward("addEvent");
				
			} catch (DotSecurityException e) {
				ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("you-do-not-have-the-required-permissions"));
				saveMessages(request, ae);				
				return mapping.findForward("addEvent");			
			}
			  
			Contentlet cont = conAPI.find(event.getInode(), currentUser, respectFrontendRoles);
			if(cmsFile != null) {
				conAPI.addFileToContentlet(cont, cmsFile.getInode(), "Event:file", currentUser, true);	
			}
					
			if(cmsImage != null) {
				conAPI.addFileToContentlet(cont, cmsImage.getInode(), "Event:image", currentUser, true);
			}					

			if(!request.getParameter("recurrenceOccurs").equals("never")){

				EventForm ef = (EventForm) lf;
				
			
				
				//EventRecurrence recurrence = new EventRecurrence();
				Date startRecurrenceDate = startDate;
				Date endRecurrenceDate   = ef.getRecurrenceEndsDate();
				event.setRecurs(true);
				event.setRecurrenceStart(startRecurrenceDate);
				event.setRecurrenceEnd(endRecurrenceDate);
				event.setNoRecurrenceEnd(ef.isNoEndDate());
				SimpleDateFormat dateFormatS = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				event.setOriginalStartDate(dateFormatS.parse(ef.getOriginalStartDate()));
	            String baseEventId = ef.getDisconnectedFrom();
				
				if(UtilMethods.isSet(baseEventId)){
					event.setDisconnectedFrom(baseEventId);
					event.setOriginalStartDate(dateFormatS.parse(ef.getOriginalStartDate()));
				}

				if(ef.getRecurrenceOccurs().equals("daily")){
					event.setRecurrenceInterval(ef.getRecurrenceIntervalDaily());
					event.setOccursEnum(Event.Occurrency.DAILY);
				}else if(ef.getRecurrenceOccurs().equals("monthly")){
					event.setRecurrenceDayOfWeek(ef.getRecurrenceDayOfWeek());
					event.setRecurrenceWeekOfMonth(ef.getRecurrenceWeekOfMonth());
					event.setRecurrenceInterval(ef.getRecurrenceIntervalMonthly());
					event.setOccursEnum(Event.Occurrency.MONTHLY);
				}else if(ef.getRecurrenceOccurs().equals("weekly")){
					String[] recurrenceDaysOfWeek = ef.getRecurrenceDaysOfWeek();   
					String daysOfWeek = "";
					for (String day : recurrenceDaysOfWeek) {
						daysOfWeek += day + ",";
					}
					event.setRecurrenceDaysOfWeek(daysOfWeek);
					event.setRecurrenceInterval(ef.getRecurrenceIntervalWeekly());
					event.setOccursEnum(Event.Occurrency.WEEKLY);
				}else if(ef.getRecurrenceOccurs().equals("annually")){
					event.setRecurrenceInterval(ef.getRecurrenceIntervalYearly());
					event.setOccursEnum(Event.Occurrency.WEEKLY);
					event.setRecurrenceDayOfWeek(ef.getRecurrenceDayOfWeek());
					event.setRecurrenceWeekOfMonth(ef.getRecurrenceWeekOfMonth());
					event.setRecurrenceMonthOfYear(ef.getRecurrenceMonthOfYear());
				}
				
				List<Category> eventCategories = catAPI.getParents(event, currentUser, true);
				List<Permission> eventPermissions  = perAPI.getPermissions(event);

                APILocator.getContentletAPI().checkin(event, eventCategories, eventPermissions, currentUser, false);
				
				if(UtilMethods.isSet(baseEventId)){
					Event baseEvent  = eventAPI.find(baseEventId, true, currentUser, true);
					baseEvent.addDateToIgnore(dateFormatS.parse(ef.getOriginalStartDate()));
					eventCategories = catAPI.getParents(baseEvent, currentUser, true);
					eventPermissions  = perAPI.getPermissions(baseEvent);
					APILocator.getContentletAPI().checkin(baseEvent, eventCategories, eventPermissions, currentUser, false);
				}
				
			} else {
				if(options.contains("autoPublish=true")) {
					try {
						conAPI.publish(event, currentUser, false);
					} catch(DotSecurityException ex) {
						Logger.debug(AddEvent.class, ex.toString());						
					}
				}
			}
			
			ActionForward af = mapping.findForward("viewCalendar");
			HibernateUtil.commitTransaction();
			if(!APILocator.getContentletAPI().isInodeIndexed(event.getInode())){
				Logger.error(this, "Timed out while waiting for index to return");
			}
			return af;
		} catch(Exception ex) {
			HibernateUtil.rollbackTransaction();
			Logger.debug(AddEvent.class, ex.toString());
			throw ex;
		}

	}
    
    
    
    private FileAsset saveFile(User user, Host host, java.io.File uploadedFile, Folder folder, String filename) throws Exception {

    	byte[] bytes = FileUtil.getBytes(uploadedFile);

    	if (bytes!=null) {

            String name = UtilMethods.getFileName(filename);
            String ext = UtilMethods.getFileExtension(filename);
            String tempName = filename;
            int counter = 1;
    		while(APILocator.getFileAPI().fileNameExists(folder, tempName)) {
    			tempName = name + counter + "." + ext;
    			counter++;
    		}
    		while(APILocator.getFileAssetAPI().fileNameExists(host, folder, tempName, "")) {
    			tempName = name + counter + "." + ext;
    			counter++;
    		}
    		name = UtilMethods.getFileName(tempName);
    		
    		Contentlet cont = new Contentlet();
			cont.setStructureInode(folder.getDefaultFileType());
			cont.setStringProperty(FileAssetAPI.TITLE_FIELD, name);
			cont.setFolder(folder.getInode());
			cont.setHost(host.getIdentifier());
			cont.setBinary(FileAssetAPI.BINARY_FIELD, uploadedFile);
			APILocator.getContentletAPI().checkin(cont,user,false);
   			APILocator.getVersionableAPI().setLive(cont);
   			return APILocator.getFileAssetAPI().fromContentlet(cont);

    	}
    	
    	return null;
    	
    }


    
}
