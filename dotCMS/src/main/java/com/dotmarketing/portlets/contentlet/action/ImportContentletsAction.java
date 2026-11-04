package com.dotmarketing.portlets.contentlet.action;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil.ImportAuditResults;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.struts.ImportContentletsForm;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.*;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.servlet.SessionMessages;
import com.liferay.util.servlet.UploadPortletRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This action class import content from csv/text files. The csv file should
 * contains as first line the required headers that match the structure fields
 * 
 * @author david
 * 
 */
public class ImportContentletsAction extends DotPortletAction {

	private final static String languageCodeHeader = "languageCode";
	private final static String countryCodeHeader = "countryCode";
	public static final String ENCODE_TYPE = "encodeType";
	public static final String ERROR = "error";
	public static final String PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS = "portlet.ext.contentlet.import_contentlets";

	/**
	 * Handles all the actions associated to importing contentlets.
	 * 
	 * @param mapping
	 *            -
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
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
		
		Logger.debug(this, "ImportContentletsAction cmd=" + cmd);
		
		User user = _getUser(req);
		
		/*
		 * We are submiting the file to process
		 */
		if (com.dotmarketing.util.Constants.PREVIEW.equals(cmd)) {
			
			try {
				Logger.debug(this, "Calling Preview Upload Method");

				//Validation
				UploadPortletRequest uploadReq = PortalUtil.getUploadPortletRequest(req);

				
				// The upload is only copied into the shared asset store once the request is known
				// to be one we are going to act on: a rejected request leaves nothing behind.
				File file = uploadReq.getFile("file");

				final ImportContentletsForm importContentletsForm = (ImportContentletsForm) form;
				if(importContentletsForm.getStructure().isEmpty()){
					SessionMessages.add(req, ERROR, "structure-type-is-required");
					setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
				}
				else if(importContentletsForm.getWorkflowActionId().isEmpty()){
					SessionMessages.add(req, ERROR, "Workflow-action-type-required");
					setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
				}
				else if (file == null || file.length() < 100) {
					SessionMessages.add(req, ERROR, "message.contentlet.file.required");
					setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
				}
				else if (!_checkImportAccess(req, importContentletsForm.getStructure(), user)) {
					setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
				} else {
					final Structure hostStrucuture = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(Host.HOST_VELOCITY_VAR_NAME);
					final boolean isHost = (hostStrucuture.getInode().equals( importContentletsForm.getStructure()));
					if(isHost){
						//Security measure to prevent invalid attempts to import a host.
						SessionMessages.add(req, ERROR, "message.import.host.invalid");
						setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
						return;
					}

					Charset charset = importContentletsForm.getLanguage() == -1 ? Charset.defaultCharset() : FileUtil.detectEncodeType(file);
					try(Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))){

						String[] csvHeaders = null;
						int languageCodeHeaderColumn = -1;
						int countryCodeHeaderColumn = -1;

						CsvReader csvreader = new CsvReader(reader);
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
										SessionMessages.add(req, ERROR, "message.import.contentlet.csv_headers.required");
										setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
									} else {
										_generatePreview(0,req, res, config, form, user, _copyToAssetTempPath(file), csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader);
										setForward(req, "portlet.ext.contentlet.import_contentlets_preview");
									}
								} else {
									SessionMessages.add(req, ERROR, "message.import.contentlet.csv_headers.error");
									setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
								}
							} else {
								SessionMessages.add(req, ERROR, "message.import.contentlet.key_field.required");
								setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
							}
							break;
						case 0:
							SessionMessages.add(req, ERROR, "message.import.contentlet.language.required");
							setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
							break;
						default:
							_generatePreview(0, req, res, config, form, user, _copyToAssetTempPath(file), csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader);
							setForward(req, "portlet.ext.contentlet.import_contentlets_preview");
							break;
						}
						csvreader.close();
					}
				}

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		} else if (com.dotmarketing.util.Constants.PUBLISH.equals(cmd)) {
			AdminLogger.log(ImportContentletsAction.class, "processAction", "Importing Contentlets", user);
			Long importIdComplete=(Long)session.getAttribute("importId");
			String subcmd = req.getParameter("subcmd");
			 if(subcmd != null && subcmd.equals("importContentletsResults")){
				HashMap<String, List<String>>results=ImportAuditUtil.loadImportResults(importIdComplete);
				req.setAttribute("importResults", results);
				setForward(req, "portlet.ext.contentlet.import_contentlets_results");
			} else if (!_checkImportAccess(req, _structureToImport(session, form), user)) {
				setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
			} else {
				final ActionRequestImpl reqImpl = (ActionRequestImpl) req;
				final HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
				final HttpSession httpSession = httpReq.getSession();
				final long importId = ImportAuditUtil.createAuditRecord(user.getUserId(), (String)httpSession.getAttribute("fileName"));
				// Snapshot the request attributes/headers/session now, while the request is still alive.
				// The Tomcat RequestFacade will be recycled before the background thread finishes for large imports.
				final HttpServletRequest safeHttpReq = new MockHeaderRequest(
						new MockSessionRequest(new MockAttributeRequest(httpReq)));


				Runnable runnable = new Runnable() {

					@Override
					@CloseDBIfOpened
					public void run() {

						boolean importFailed = false;
						String importErrorMessage = null;
						ImportContentletsForm importContentletsForm = (ImportContentletsForm) form;
						File fileToImport = (File) httpSession.getAttribute("file_to_import");
						Charset charset = importContentletsForm.getLanguage() == -1
								? Charset.defaultCharset()
								: FileUtil.detectEncodeType(fileToImport);
						try(Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToImport), charset))){

							Logger.debug(this, "Calling Process File Method");


							String[] csvHeaders = null;
							int languageCodeHeaderColumn = -1;
							int countryCodeHeaderColumn = -1;


							CsvReader csvreader = new CsvReader(reader);
							csvreader.setSafetySwitch(false);

							if (importContentletsForm.getLanguage() == -1) {
								if (csvreader.readHeaders()) {
									csvHeaders = csvreader.getHeaders();
									for (int column = 0; column < csvHeaders.length; ++column) {
										if (csvHeaders[column].equals(languageCodeHeader)) {
											languageCodeHeaderColumn = column;
										}
										if (csvHeaders[column].equals(countryCodeHeader)) {
											countryCodeHeaderColumn = column;
										}
										if ((-1 < languageCodeHeaderColumn) && (-1 < countryCodeHeaderColumn)) {
											break;
										}
									}
								}
							}



							HashMap<String, List<String>> importresults= new HashMap<>();
							if(importSession.equals(httpSession.getAttribute("importSession"))){
								httpSession.removeAttribute("importSession");
								importresults = _processFile(importId, httpSession, user,
										csvHeaders, csvreader, languageCodeHeaderColumn,
										countryCodeHeaderColumn, reader, safeHttpReq);
							}

							final List<String> counters= importresults.get("counters");
							int contentsToImport=0;
							for(String counter: counters){
								String counterArray[]=counter.split("=");
								if(counterArray[0].equals("newContent") || counterArray[0].equals("contentToUpdate")) {
									contentsToImport =
											contentsToImport + Integer.parseInt(counterArray[1]);
								}
							}

							final List<String> inodes= importresults.get("lastInode");
							if(!inodes.isEmpty()){
								ImportAuditUtil.updateAuditRecord(inodes.get(0), contentsToImport, importId,importresults);
							}

							csvreader.close();


						} catch (Exception ae) {
							Logger.error(this, "Error importing contentlets for import ID: " + importId, ae);
							try {
								HibernateUtil.rollbackTransaction();
							} catch (DotHibernateException e1) {
								Logger.error(this, "Error rolling back transaction for import ID: " + importId, e1);
							}
							importFailed = true;
							importErrorMessage = ae.getMessage() != null
									? ae.getMessage() : ae.getClass().getSimpleName();

						} finally{

							try {
								ImportAuditUtil.cancelledImports.remove(importId);
								if(importFailed){
									ImportAuditUtil.setAuditRecordAsFailed(importId, importErrorMessage);
								} else {
									ImportAuditUtil.setAuditRecordCompleted(importId);
								}
							} catch (Exception e) {
								Logger.error(this, "Error updating audit record for import ID: " + importId, e);
							}
						}
					}
				};


				if(Config.getBooleanProperty("IMPORT_CONTENTLETS_ASYNC", true)) {
					DotConcurrentFactory.getInstance().getSubmitter("importContentlets").submit(runnable);
				}else{
					runnable.run();
				}


				 req.setAttribute("previewResults", session.getAttribute("previewResults"));
				 session.removeAttribute("previewResults");
				 req.setAttribute("importId", importId);
				 setForward(req, "portlet.ext.contentlet.import_contentlets_preview");
			}		
		}else  if(cmd != null && cmd.equals("downloadCSVTemplate")){
			_downloadCSVTemplate(req, res,config,form);
		} else {
			if(UtilMethods.isSet(req.getParameter("selectedStructure")) && UtilMethods.isSet(req.getAttribute("ImportContentletsForm"))){
				((ImportContentletsForm)req.getAttribute("ImportContentletsForm")).setStructure(req.getParameter("selectedStructure"));
			}
			if (session.getAttribute("previewResults")!= null){
				HashMap<String, List<String>>
					results =
					(HashMap<String, List<String>>) session.getAttribute("previewResults");
				ContentletCache contentletCache = CacheLocator.getContentletCache();
				if (results.get("updatedInodes") != null) {
					for (String inode : results.get("updatedInodes")) {
						contentletCache.remove(inode);
					}
					session.removeAttribute("previewResults");
				}
			}
			
			ImportAuditResults audits = ImportAuditUtil.loadAuditResults(user.getUserId());
			req.setAttribute("audits", audits);			
			session.setAttribute("importSession", importSession);
			setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
		}

	}



	/**
	 * Provides the user a CSV template based on the selected Content Type. This
	 * assists users in the process of adding new content to dotCMS as it
	 * already provides the correct column headers required by the content
	 * import process.
	 * 
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @throws Exception
	 *             The CSV template could not be generated.
	 */
	private void _downloadCSVTemplate(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
		ActionResponseImpl resImpl = (ActionResponseImpl)res;
		HttpServletResponse httpRes = resImpl.getHttpServletResponse();

		ImportContentletsForm importForm = (ImportContentletsForm) form;
		if (!UtilMethods.isSet(importForm.getStructure())) {
			SessionMessages.add(req, ERROR, "structure-type-is-required");
			setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
			return;
		}

		// The fields must be resolved before any download header is committed, otherwise a
		// failed lookup leaves the response flagged as an attachment with an empty error body
		final List<Field> fields;
		try {
			fields = FieldsCache.getFieldsByStructureInode(importForm.getStructure());
		} catch (final DotStateException e) {
			Logger.warn(this, "Unable to generate CSV template: selected Content Type does not exist", e);
			SessionMessages.add(req, ERROR, "Workflow-does-not-exists-content-type");
			setForward(req, PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS);
			return;
		}

		httpRes.setContentType("application/octet-stream");
		httpRes.setHeader("Content-Disposition", "attachment; filename=\"CSV_Template.csv\"");

		ServletOutputStream out = httpRes.getOutputStream();
		for(int i = 0; i < fields.size(); i++) {
			Field field = fields.get(i);
			if (ImportUtil.isImportableField(field)) {
				String fieldVariableName = field.getVelocityVarName();
				if(fieldVariableName.contains(",")) {
					out.print("\"" + fieldVariableName + "\"");
				} else {
					out.print(fieldVariableName);
				}
				if(i < fields.size() - 1) { 
					out.print(",");
				}
			}
		}
		out.print("\n");

		for(int i = 0; i < fields.size(); i++) {
			Field field = fields.get(i);
			if (ImportUtil.isImportableField(field)) {
				if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
					out.print("\"MM/dd/yyyy\"");
				} else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
					out.print("\"MM/dd/yyyy hh:mm aa\"");
				} else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
					out.print("\"hh:mm aa\"");
				}else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
					out.print("\"Host/Folder Identifier\"");
				}else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {
					out.print("\"Category Unique Key\"");
				}else if (field.getFieldType().equals(Field.FieldType.KEY_VALUE.toString())){
				    out.print("\"{'key1':'value','key2':'value2'}\"");
				}    else {
					out.print("\"XXX\"");
				}
				if(i < fields.size() - 1) {
					out.print(",");
				}
			}
		}
		out.flush();
		out.close();
		HibernateUtil.closeSession();
	}


	/**
	 * Checks that the user may run the import, and puts a message on the request naming the
	 * Content Type when they may not.
	 *
	 * <p>The type is named in the message on purpose: the user picked it from the screen, so
	 * telling them which one they are missing access to is the difference between a message they
	 * can act on and one that only says no. The same bar is applied before the preview and before
	 * the import itself, so a user cannot pass the first screen and be refused on the second.</p>
	 *
	 * @param req               the Struts wrapper for the HTTP request, where the message is left
	 * @param contentTypeInode  the inode of the Content Type the import will run against
	 * @param user              the {@link User} making the request
	 * @return {@code true} when the import may go ahead
	 */
	private boolean _checkImportAccess(final ActionRequest req, final String contentTypeInode,
			final User user) {
		final ContentType contentType = _findContentType(contentTypeInode);
		final Host site = _selectedSite(req, user);
		if (ContentImportAccess.canImportInto(contentType, site, user)) {
			return true;
		}
		if (null == contentType) {
			SecurityLogger.logInfo(ImportContentletsAction.class, String.format(
					"Refusing an import for user '%s': Content Type '%s' could not be read",
					user.getUserId(), contentTypeInode));
		}
		SessionMessages.add(req, ERROR, _importAccessMessage(contentType, contentTypeInode, user));
		return false;
	}

	/**
	 * Reads the Content Type the import will run against, returning {@code null} rather than
	 * throwing when it cannot be read, so the caller refuses instead of failing.
	 *
	 * @param contentTypeInode the inode to look up
	 * @return the {@link ContentType}, or {@code null}
	 */
	private ContentType _findContentType(final String contentTypeInode) {
		try {
			return APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentTypeInode);
		} catch (final Exception e) {
			Logger.warn(this, String.format("Content Type '%s' could not be read for an import",
					contentTypeInode), e);
			return null;
		}
	}

	/**
	 * Reads the site the import is running against, the one the user has selected in the back end.
	 *
	 * @param req  the Struts wrapper for the HTTP request
	 * @param user the {@link User} making the request
	 * @return the selected {@link Host}, or {@code null} when none is selected or it cannot be
	 *         read
	 */
	private Host _selectedSite(final ActionRequest req, final User user) {
		final HttpSession httpSession = ((ActionRequestImpl) req).getHttpServletRequest()
				.getSession();
		final String siteId = (String) httpSession
				.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
		if (!UtilMethods.isSet(siteId)) {
			return null;
		}
		try {
			return APILocator.getHostAPI().find(siteId, APILocator.systemUser(), false);
		} catch (final Exception e) {
			Logger.warn(this, String.format("Site '%s' could not be read for an import", siteId),
					e);
			return null;
		}
	}

	/**
	 * Returns the inode of the Content Type the import is actually going to run against.
	 *
	 * <p>The background import thread reads the form stored in the session by the preview step,
	 * not the form bound to this request, so that is the value the permission check has to be made
	 * against. Falling back to the request form only matters when the session has no stored form,
	 * in which case the import has nothing to run and the check refuses on an unresolvable type.
	 * </p>
	 *
	 * @param session the HTTP session holding the form the preview step stored
	 * @param form    the form bound to this request, used only as a fallback
	 * @return the Content Type inode the import will use, possibly {@code null}
	 */
	private String _structureToImport(final HttpSession session, final ActionForm form) {
		final ImportContentletsForm storedForm = (ImportContentletsForm) session
				.getAttribute("form_to_import");
		if (null != storedForm) {
			return storedForm.getStructure();
		}
		return form instanceof ImportContentletsForm
				? ((ImportContentletsForm) form).getStructure() : null;
	}

	/**
	 * Builds the message shown when an import is refused, naming the Content Type involved.
	 *
	 * <p>The message is resolved here rather than left as a key because it carries the type's
	 * name. {@code messages_inc.jsp} runs whatever it is given through the message resources and
	 * returns it unchanged when there is no matching key, so an already-translated sentence
	 * reaches the screen as it is.</p>
	 *
	 * @param contentType      the {@link ContentType} the import was for, or {@code null} when it
	 *                         could not be read
	 * @param contentTypeInode the inode that was asked for, used when the type cannot be named
	 * @param user             the {@link User} whose locale the message is built in
	 * @return the message to show
	 */
	private String _importAccessMessage(final ContentType contentType,
			final String contentTypeInode, final User user) {
		final String typeName = null != contentType ? contentType.name() : contentTypeInode;
		try {
			return LanguageUtil.get(user, "message.import.contentlet.no-permission-on-type",
					typeName);
		} catch (final Exception e) {
			Logger.warn(this, "Could not build the import permission message", e);
			return "You do not have permission to import content of type \"" + typeName + "\".";
		}
	}

	/**
	 * Copies the uploaded file into the shared asset temp path and returns the copy, which is what
	 * the rest of the import reads from.
	 *
	 * <p>Called only once the request has passed every check, so a request that is going to be
	 * rejected never writes to shared storage.</p>
	 *
	 * @param uploaded the file as the upload request parsed it
	 * @return the copy inside the asset temp path
	 * @throws IOException if the file cannot be copied
	 */
	private File _copyToAssetTempPath(final File uploaded) throws IOException {
		final java.nio.file.Path destination = Paths.get(ConfigUtils.getAssetTempPath()
				+ java.io.File.separator + FileUtil.sanitizeFileName(uploaded.getName()));
		Files.copy(uploaded.toPath(), destination,
				java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		return destination.toFile();
	}

	/**
	 * Reads and analyzes the content of the CSV import file in order to
	 * determine potential errors, inconsistencies or warnings, and provide the
	 * user with useful information regarding the contents of the file.
	 * 
	 * @param importId
	 *            - The ID of this data import.
	 * @param req
	 *            - The Struts wrapper for the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper for the HTTP Response object.
	 * @param config
	 *            - The configuration parameters for this portlet.
	 * @param form
	 *            - The form containing the information selected by the user in
	 *            the UI.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param file
	 *            - The file representation of the CSV file.
	 * @param csvHeaders
	 *            - The headers that make up the CSV file.
	 * @param csvreader
	 *            - The actual data contained in the CSV file.
	 * @param languageCodeHeaderColumn
	 *            - The column name containing the language code.
	 * @param countryCodeHeaderColumn
	 *            - The column name containing the country code.
	 * @param reader
	 *            - The character streams reader.
	 * @throws Exception
	 *             An error occurred when analyzing the CSV file.
	 */
	private void _generatePreview(long importId, ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, File file, String[] csvHeaders, CsvReader csvreader, int languageCodeHeaderColumn, int countryCodeHeaderColumn, Reader reader) throws Exception {
		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		HttpSession session = httpReq.getSession();
		httpReq.getSession().setAttribute("file_to_import", file);
		httpReq.getSession().setAttribute("form_to_import", form);
		ImportContentletsForm importForm = (ImportContentletsForm) form;
		httpReq.getSession().setAttribute("fileName", importForm.getFileName());
		String currentSiteId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
		Logger.info(this, "-------- Starting Content Import Preview -------- ");
        Logger.info(this, String.format("-> File: %s", importForm.getFileName()));
        Logger.info(this, String.format("-> Content Type ID: %s", importForm.getStructure()));
		HashMap<String, List<String>> results = ImportUtil.importFile(importId, currentSiteId, importForm.getStructure(), importForm.getFields(), true, (importForm.getLanguage() == -1), user, importForm.getLanguage(), csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader, importForm.getWorkflowActionId(),httpReq);
		req.setAttribute("previewResults", results);
	}

	/**
	 * Executes the content import process after the review process has been run
	 * and displayed to the user.
	 * 
	 * @param importId
	 *            - The ID of this data import.
	 * @param session
	 *            - HTTP Session object.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param csvHeaders
	 *            - The headers that make up the CSV file.
	 * @param csvreader
	 *            - The actual data contained in the CSV file.
	 * @param languageCodeHeaderColumn
	 *            - The column name containing the language code.
	 * @param countryCodeHeaderColumn
	 *            - The column name containing the country code.
	 * @param reader
	 *            - The character streams reader.
	 * @return The status of the content import performed by dotCMS. This
	 *         provides information regarding inconsistencies, errors, warnings
	 *         and/or precautions to the user.
	 * @throws Exception
	 *             An error occurred when adding/updating data to the content
	 *             repository.
	 */
	private HashMap<String, List<String>> _processFile(final long importId, final HttpSession session,
			final User user, final String[] csvHeaders, final CsvReader csvreader,
			final int languageCodeHeaderColumn, final int countryCodeHeaderColumn, final Reader reader,
			final HttpServletRequest httpReq)
			throws Exception {
		final ImportContentletsForm importForm = (ImportContentletsForm) session
				.getAttribute("form_to_import");
		final String currentSiteId = (String) session
				.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
        Logger.info(this, "-------- Starting Content Import Process -------- ");
        Logger.info(this, String.format("-> File: %s", importForm.getFileName()));
        Logger.info(this, String.format("-> Content Type ID: %s", importForm.getStructure()));
		final HashMap<String, List<String>> results = ImportUtil
				.importFile(importId, currentSiteId, importForm.getStructure(),
						importForm.getFields(), false, (importForm.getLanguage() == -1), user,
						importForm.getLanguage(), csvHeaders, csvreader, languageCodeHeaderColumn,
						countryCodeHeaderColumn, reader, importForm.getWorkflowActionId(), httpReq);
		return results;
	}

}
