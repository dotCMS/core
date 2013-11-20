package com.dotmarketing.portlets.contentlet.action;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.csvreader.CsvReader;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil.ImportAuditResults;
import com.dotmarketing.portlets.contentlet.struts.ImportContentletsForm;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.FileUtil;
import com.liferay.util.servlet.SessionMessages;
import com.liferay.util.servlet.UploadPortletRequest;

/**
 * This action class import content from csv/text files. 
 * The csv file should contains as first line the required headers that match the structure fields
 *  
 * @author david
 * 
 */
public class ImportContentletsAction extends DotPortletAction {


	
	private final static String languageCodeHeader = "languageCode";
	private final static String countryCodeHeader = "countryCode";

	
	/**
	 * @param permissionAPI the permissionAPI to set
	 */


	public void processAction(ActionMapping mapping, final ActionForm form, final PortletConfig config, final ActionRequest req, final ActionResponse res) throws Exception {
		Logger.debug(this, "Import Contentlets Action");
		
		
		
		HttpSession session = ((ActionRequestImpl)req).getHttpServletRequest().getSession();
		String importSes =Long.toString(System.currentTimeMillis());
		
		if(UtilMethods.isSet(session.getAttribute("importSession"))){
			importSes = (String) session.getAttribute("importSession");
		}
		final String importSession=importSes;

		
		
		
		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");
		
		Logger.debug(this, "ImportContentletsAction cmd=" + cmd);
		
		User user = _getUser(req);
		
		/*
		 * We are submiting the file to process
		 */
		if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.PREVIEW)) {
			
			try {
				Logger.debug(this, "Calling Preview Upload Method");

				//Validation
				UploadPortletRequest uploadReq = PortalUtil.getUploadPortletRequest(req);
				byte[] bytes = FileUtil.getBytes(uploadReq.getFile("file"));

				if (bytes == null || bytes.length == 0) {
					SessionMessages.add(req, "error", "message.contentlet.file.required");
					setForward(req, "portlet.ext.contentlet.import_contentlets");
				} else {
					try {
						Reader reader = null;
						CsvReader csvreader = null;
						String[] csvHeaders = null;
						int languageCodeHeaderColumn = -1;
						int countryCodeHeaderColumn = -1;
						
						ImportContentletsForm importContentletsForm = (ImportContentletsForm) form;
						if (importContentletsForm.getLanguage() == -1)
							reader = new InputStreamReader(new ByteArrayInputStream(bytes), Charset.forName("UTF-8"));
						else
							reader = new InputStreamReader(new ByteArrayInputStream(bytes));
						
						csvreader = new CsvReader(reader);
						csvreader.setSafetySwitch(false);
						
						switch ((int) importContentletsForm.getLanguage()) {
						case -1:
							if (0 < importContentletsForm.getFields().length) {
								if (csvreader.readHeaders()) {
									csvHeaders = csvreader.getHeaders();
									for (int column = 0; column < csvHeaders.length; ++column) {
										if (csvHeaders[column].equals(languageCodeHeader))
											languageCodeHeaderColumn = column;
										if (csvHeaders[column].equals(countryCodeHeader))
											countryCodeHeaderColumn = column;
										
										if ((-1 < languageCodeHeaderColumn) && (-1 < countryCodeHeaderColumn))
											break;
									}
									
									if ((-1 == languageCodeHeaderColumn) || (-1 == countryCodeHeaderColumn)) {
										SessionMessages.add(req, "error", "message.import.contentlet.csv_headers.required");
										setForward(req, "portlet.ext.contentlet.import_contentlets");
									} else {
										_generatePreview(0,req, res, config, form, user, bytes, csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader);
										setForward(req, "portlet.ext.contentlet.import_contentlets_preview");
									}
								} else {
									SessionMessages.add(req, "error", "message.import.contentlet.csv_headers.error");
									setForward(req, "portlet.ext.contentlet.import_contentlets");
								}
							} else {
								SessionMessages.add(req, "error", "message.import.contentlet.key_field.required");
								setForward(req, "portlet.ext.contentlet.import_contentlets");
							}
							break;
						case 0:
							SessionMessages.add(req, "error", "message.import.contentlet.language.required");
							setForward(req, "portlet.ext.contentlet.import_contentlets");
							break;
						default:
							_generatePreview(0, req, res, config, form, user, bytes, csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader);
							setForward(req, "portlet.ext.contentlet.import_contentlets_preview");
							break;
						}
						
						csvreader.close();
					} catch (Exception e) {
						_handleException(e, req);
						return;
					}
				}

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		} else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.PUBLISH)) {
			AdminLogger.log(ImportContentletsAction.class, "processAction", "Importing Contentlets", user);
			Long importIdComplete=(Long)session.getAttribute("importId");
			String subcmd = req.getParameter("subcmd");
			 if(subcmd != null && subcmd.equals("importContentletsResults")){
				HashMap<String, List<String>>results=ImportAuditUtil.loadImportResults(importIdComplete);
				req.setAttribute("importResults", results);
				setForward(req, "portlet.ext.contentlet.import_contentlets_results");
			} else {
				final ActionRequestImpl reqImpl = (ActionRequestImpl) req;
				final HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
				final long importId = ImportAuditUtil.createAuditRecord(user.getUserId(), (String)httpReq.getSession().getAttribute("fileName"));
				Thread t=new Thread() {
					public void run() {
						try {
							Logger.debug(this, "Calling Process File Method");
							
							Reader reader = null;
							CsvReader csvreader = null;
							String[] csvHeaders = null;
							int languageCodeHeaderColumn = -1;
							int countryCodeHeaderColumn = -1;
							
							byte[] bytes = (byte[]) httpReq.getSession().getAttribute("file_to_import");
							File file= (File)httpReq.getSession().getAttribute("csvFile");
							ImportContentletsForm importContentletsForm = (ImportContentletsForm) form;
							if (importContentletsForm.getLanguage() == -1)
								reader = new InputStreamReader(new ByteArrayInputStream(bytes), Charset.forName("UTF-8"));
							else
								reader = new InputStreamReader(new ByteArrayInputStream(bytes));
							csvreader = new CsvReader(reader);
							csvreader.setSafetySwitch(false);
								
							if (importContentletsForm.getLanguage() == -1) {
								if (csvreader.readHeaders()) {
									csvHeaders = csvreader.getHeaders();
									for (int column = 0; column < csvHeaders.length; ++column) {
										if (csvHeaders[column].equals(languageCodeHeader))
											languageCodeHeaderColumn = column;
										if (csvHeaders[column].equals(countryCodeHeader))
											countryCodeHeaderColumn = column;
											
										if ((-1 < languageCodeHeaderColumn) && (-1 < countryCodeHeaderColumn))
											break;
									}
								}
							}
							HttpSession session = ((ActionRequestImpl)req).getHttpServletRequest().getSession();
							User user = _getUser(req);
							//long importId = ImportAuditUtil.createAuditRecord(user.getUserId(), (String)httpReq.getSession().getAttribute("fileName"));
							
			
							HashMap<String, List<String>> importresults=null;
							if(importSession.equals((String) session.getAttribute("importSession") )){
								session.removeAttribute("importSession");
								importresults=_processFile(importId, req, res, config, form, user, csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader);
								
							}
											
							List<String> counters= importresults .get("counters");
							int contentsToImport=0;
							for(String counter: counters ){
								String counterArray[]=counter.split("=");
								if(counterArray[0].equals("newContent") || counterArray[0].equals("contentToUpdate"))
									contentsToImport=contentsToImport + Integer.parseInt(counterArray[1]);		
							}
							
							List<String> inodes= importresults.get("lastInode");
							if(!inodes.isEmpty()){
								ImportAuditUtil.updateAuditRecord(inodes.get(0), contentsToImport, importId,importresults);
							}
											
							csvreader.close();
							
			
						} catch (Exception ae) {
							_handleException(ae, req);
							return;
						} finally{
							if(!ImportAuditUtil.cancelledImports.containsKey(importId)){
								ImportAuditUtil.setAuditRecordCompleted(importId);
							}else{
								ImportAuditUtil.cancelledImports.remove(importId);
							}
						}
					}
				};
				//t.setName("ImportContentletThread");
				t.start();
				req.setAttribute("previewResults", (HashMap<String, List<String>>) session.getAttribute("previewResults"));
				req.setAttribute("importId", importId);
				setForward(req, "portlet.ext.contentlet.import_contentlets_preview");
			}		
		}else  if(cmd != null && cmd.equals("downloadCSVTemplate")){
			_downloadCSVTemplate(req, res,config,form);
		} else {
			
			if(UtilMethods.isSet(req.getParameter("selectedStructure")) && UtilMethods.isSet(req.getAttribute("ImportContentletsForm"))){
				((ImportContentletsForm)req.getAttribute("ImportContentletsForm")).setStructure(req.getParameter("selectedStructure"));
			}
			
			ImportAuditResults audits = ImportAuditUtil.loadAuditResults(user.getUserId());
			req.setAttribute("audits", audits);			
			session.setAttribute("importSession", importSession);
			setForward(req, "portlet.ext.contentlet.import_contentlets");
		}

	}

	// /// ************** ALL METHODS HERE *************************** ////////
	private void _downloadCSVTemplate(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {

		ActionResponseImpl resImpl = (ActionResponseImpl)res;
		HttpServletResponse httpRes = resImpl.getHttpServletResponse();

		httpRes.setContentType("application/octet-stream");
		httpRes.setHeader("Content-Disposition", "attachment; filename=\"CSV_Template.csv\"");

		ServletOutputStream out = httpRes.getOutputStream();
		ImportContentletsForm importForm = (ImportContentletsForm) form;

		List<Field> fields = FieldsCache.getFieldsByStructureInode(importForm.getStructure());
		for(int i = 0; i < fields.size(); i++) {
			Field field = fields.get(i);
			if (ImportUtil.isImportableField(field)) {
				String fieldName = field.getFieldName();
				if(fieldName.contains(","))
					out.print("\"" + fieldName + "\"");
				else
					out.print(fieldName);
				if(i < fields.size() - 1) 
					out.print(",");
			}
		}
		out.print("\n");

		for(int i = 0; i < fields.size(); i++) {
			Field field = fields.get(i);
			if (ImportUtil.isImportableField(field)) {
				if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
					out.print("MM/dd/yyyy");
				}        	
				else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
					out.print("MM/dd/yyyy hh:mm aa");
				}         	
				else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
					out.print("hh:mm aa");
				}else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
						out.print("Host/Folder Identifier");
				}else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {
					out.print("Category Unique Key");
				} else {
					out.print("XXX");
				}
				if(i < fields.size() - 1) 
					out.print(",");
			}
		}

		out.flush();
		out.close();
		HibernateUtil.closeSession();
	}

	private void _generatePreview(long importId, ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, byte[] bytes, String[] csvHeaders, CsvReader csvreader, int languageCodeHeaderColumn, int countryCodeHeaderColumn, Reader reader) throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		HttpSession session = httpReq.getSession();

		httpReq.getSession().setAttribute("file_to_import", bytes);
		httpReq.getSession().setAttribute("form_to_import", form);

		ImportContentletsForm importForm = (ImportContentletsForm) form;
		httpReq.getSession().setAttribute("fileName", importForm.getFileName());
		String currentHostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
		HashMap<String, List<String>> results = ImportUtil.importFile(importId, currentHostId, importForm.getStructure(), importForm.getFields(), true, (importForm.getLanguage() == -1), user, importForm.getLanguage(), csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader);

		req.setAttribute("previewResults", results);
	}

	private HashMap<String, List<String>> _processFile(long importId,ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String[] csvHeaders, CsvReader csvreader, int languageCodeHeaderColumn, int countryCodeHeaderColumn, Reader reader)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		HttpSession session = httpReq.getSession();
		ImportContentletsForm importForm = (ImportContentletsForm) httpReq.getSession().getAttribute("form_to_import");
		String currentHostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	 	HashMap<String, List<String>> results = ImportUtil.importFile(importId, currentHostId, importForm.getStructure(), importForm.getFields(), false, (importForm.getLanguage() == -1), user, importForm.getLanguage(), csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader);
	 	//req.setAttribute("importResults", results);
	 	return results;
		
	}

}