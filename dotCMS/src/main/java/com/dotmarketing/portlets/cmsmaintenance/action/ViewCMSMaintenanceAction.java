package com.dotmarketing.portlets.cmsmaintenance.action;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.contenttype.util.ContentTypeImportExportUtil;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Clickstream404;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.PermissionReference;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexQueueFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.calendar.model.CalendarReminder;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.portlets.cmsmaintenance.struts.CmsMaintenanceForm;
import com.dotmarketing.portlets.cmsmaintenance.util.AssetFileNameFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotReindexStateException;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary404;
import com.dotmarketing.portlets.dashboard.model.DashboardUserPreferences;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.rules.util.RulesImportExportUtil;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.ExportStarterUtil;
import com.dotmarketing.util.HibernateCollectionConverter;
import com.dotmarketing.util.HibernateMapConverter;
import com.dotmarketing.util.ImportStarterUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.Parameter;
import com.dotmarketing.util.TrashUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.ZipUtil;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.ejb.ImageLocalManagerUtil;
import com.liferay.portal.ejb.PortletPreferencesLocalManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.FileUtil;
import com.liferay.util.servlet.SessionMessages;
import com.liferay.util.servlet.UploadPortletRequest;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * This class group all the CMS Maintenance Task
 * (Cache controll, search and replace, import/export content )
 *
 * @author Oswaldo
 * @version 3.3
 *
 */
public class ViewCMSMaintenanceAction extends DotPortletAction {

	/**
	 * The path where backup files are stored
	 */
	String backupFilePath = ConfigUtils.getBackupPath();

	/**
	 * The path where tmp files are stored. This gets wiped alot
	 */
	String backupTempFilePath = ConfigUtils.getBackupPath()+File.separator+"temp";
	private static String assetRealPath = null;
	private static String assetPath = File.separator + "assets";

	private ContentletAPI conAPI = APILocator.getContentletAPI();

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
	throws Exception {
		Logger.debug(this, "Running ViewCMSMaintenanceAction!!!! = " + req.getWindowState());

		// Set the asset paths
        try {
        	assetRealPath = Config.getStringProperty("ASSET_REAL_PATH");
        } catch (Exception e) { }
        try {
            assetPath = Config.getStringProperty("ASSET_PATH");
        } catch (Exception e) { }

		try {
			//gets the user
			_initCacheValues(req);

			if (req.getWindowState().equals(WindowState.MAXIMIZED)) {
				Logger.debug(this, "Showing view action cms maintenance maximized");
				_initStructures(form,req,res);
				return mapping.findForward("portlet.ext.cmsmaintenance.view_cms_maintenance");
			} else {
				Logger.debug(this, "Showing view action cms maintenance minimized");
				return mapping.findForward("portlet.ext.cmsmaintenance.view");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(com.liferay.portal.util.Constants.COMMON_ERROR);
		}
	}

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
			throws Exception
	{
		String message = "";
		String errorMessage = "";
		CmsMaintenanceForm ccf = (CmsMaintenanceForm) form;
		String cmd = req.getParameter("cmd");
		String defaultStructure = req.getParameter("defaultStructure");
		Structure structure = new Structure();

		//Manage all the cache Task
		if(cmd.equals("cache")){

			String cacheName = ccf.getCacheName();
			if (cacheName.equals(com.dotmarketing.util.WebKeys.Cache.CACHE_CONTENTS_INDEX))
			{
				Logger.info(this, "Running Contents Index Cache");
				if(defaultStructure.equals("Rebuild Whole Index")){
					//structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(defaultStructure);
				}else
					structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(ccf.getStructure());
				if(!InodeUtils.isSet(structure.getInode()))
				{

						int shards = Config.getIntProperty("es.index.number_of_shards", 2);
						try{
							shards = Integer.parseInt(req.getParameter("shards"));
						}catch(Exception e){

						}
						System.setProperty("es.index.number_of_shards", String.valueOf(shards));
						Logger.info(this, "Running Contentlet Reindex");

						conAPI.refreshAllContent();

						message = "message.cmsmaintenance.cache.indexrebuilt";
						AdminLogger.log(ViewCMSMaintenanceAction.class, "processAction", "Running Contentlet Reindex");

				}
				else
				{
					try{
						Logger.info(this, "Running Contentlet Reindex on Structure: " + structure.getName());
						conAPI.reindex(structure);
						message = "message.cmsmaintenance.cache.indexrebuilt";
						AdminLogger.log(ViewCMSMaintenanceAction.class, "processAction", "Running Contentlet Reindex on structure : "+structure.getName());
					}catch(DotReindexStateException dre){
						Logger.warn(this, "Content Reindexation Failed caused by: "+ dre.getMessage());
						errorMessage = "message.cmsmaintenance.cache.failedtorebuild";
					}
				}
			} else if (cacheName.equals(com.dotmarketing.util.WebKeys.Cache.CACHE_MENU_FILES))
			{
				Logger.info(this, "Deleting Menu Files");
				_deleteMenusCache();
				message = "message.cmsmaintenance.cache.flushmenucaches";
			} else if (cacheName.equals("flushCache"))
			{
				final String cacheToFlush = req.getParameter("cName");
				boolean isAllCachesFlush = false;// this boolean is for the messages (logs and UI)
				try{
					CacheLocator.getCache(cacheToFlush);
				}catch (NullPointerException e) {
					isAllCachesFlush = true;//is a NPE is returned means it's cleaning all the caches
				}
				final String msgLogger = isAllCachesFlush ? "Flushing All Caches" : "Flushing " + cacheToFlush +" Cache";
				Logger.info(this, msgLogger);
				_flush(cacheToFlush);
				message = isAllCachesFlush ? "message.cmsmaintenance.cache.flushallcache" : "message.cmsmaintenance.cache.flushcache";
			} else {
				Logger.info(this, "Flushing Live and Working File Cache");
				_deleteFiles(com.dotmarketing.util.WebKeys.Cache.CACHE_LIVE_FILES);
				_deleteFiles(com.dotmarketing.util.WebKeys.Cache.CACHE_WORKING_FILES);

				message = "message.cmsmaintenance.cache.deletefiles";
			}
		} // Download the records that could not be re-indexed in a CSV file
		else if (cmd.equals("export-failed-as-csv")) {
			ActionResponseImpl resImpl = (ActionResponseImpl) res;
			HttpServletResponse response = resImpl.getHttpServletResponse();
			downloadRemainingRecordsAsCsv(response);
		} //Manage all the search and replace Task
		else if(cmd.equals("searchandreplace")){
			Logger.info(this, "Running Search & Replace");
			Map<String,String> result = searchAndReplace(ccf);
			if(result.get("type") != null){
				if(result.get("type").equals("message")){
					message = "Search and Replace complete <br>" + result.get("message");
				} else {
					errorMessage = "Search and Replace complete <br>" + result.get("message");
				}
				_deleteFiles(com.dotmarketing.util.WebKeys.Cache.CACHE_LIVE_FILES);
				_deleteFiles(com.dotmarketing.util.WebKeys.Cache.CACHE_WORKING_FILES);
				_flush(com.dotmarketing.util.WebKeys.Cache.CACHE_ALL_CACHES);
			}
		}
		//Not being used this is being called using ajax
		else if(cmd.equals("fixAssetsInconsistencies")){
           // this one NOT
		}
		//Not being used this is being called using ajax
		else if(cmd.equals("dropoldassets")){

			Logger.info(this, "Running Drop old Assets");
			if(_dropAssetOldVersions(ccf) >= 0){
				message = "message.cmsmaintenance.dropoldassets.sucessfully";
			}else{
				errorMessage = "message.cmsmaintenance.dropoldassets.failed";
			}

		}
		//Manage all the import/ export Task
		else {

			final boolean dataOnly = Parameter.getBooleanFromString(req.getParameter("dataOnly"), true);
			
            final File outputDir = dataOnly ? new ExportStarterUtil().createStarterData() : new ExportStarterUtil().createStarterWithAssets();
            final File zipFile = new File(backupFilePath + "/backup_" +  UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_') + ".zip");
            message +="Zipping up to file:" + zipFile.getAbsolutePath();
            try(final ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFile.toPath())))){
                ZipUtil.zipDirectory(outputDir.getAbsolutePath(), zout);
            }
            
            
			if(cmd.equals("downloadZip")) {

				ActionResponseImpl responseImpl = (ActionResponseImpl) res;
				HttpServletResponse httpResponse = responseImpl.getHttpServletResponse();
				httpResponse.setHeader("Content-type", "application/zip");
				httpResponse.setHeader("Content-Disposition", "attachment; filename=" + zipFile.getName());

				IOUtils.copy(Files.newInputStream(zipFile.toPath()), httpResponse.getOutputStream());
           
			}
			new TrashUtils().moveFileToTrash(outputDir, "starter");
			
		}

		if(UtilMethods.isSet(message)){
			SessionMessages.add(req, "message",message);
		}
		if(UtilMethods.isSet(errorMessage)){
			SessionMessages.add(req, "error",errorMessage);
		}

		String referer = req.getParameter("referer");
		setForward(req,referer);
	}

	private void _flush(String cacheName)throws Exception{
			try{
				CacheLocator.getCache(cacheName).clearCache();
			}catch (NullPointerException e) {
				MaintenanceUtil.flushCache();
			}
		APILocator.getPermissionAPI().resetAllPermissionReferences();
	}

	private void _deleteMenusCache()throws Exception{
		MaintenanceUtil.deleteMenuCache();
	}

	private void _deleteFiles(String cacheName)
	throws Exception
	{
		try
		{
			String realPath = "";

			String velocityRootPath =ConfigUtils.getDynamicVelocityPath();
			if (velocityRootPath.startsWith(File.separator + "WEB-INF")) {
				velocityRootPath = FileUtil.getRealPath(velocityRootPath);
			}

			if (cacheName.equals(com.dotmarketing.util.WebKeys.Cache.CACHE_LIVE_FILES))
			{
				realPath = velocityRootPath + File.separator + "live";
			}
			else
			{
				realPath = velocityRootPath + File.separator + "working";
			}
			File file = new File(realPath);
			if (file.isDirectory())
			{
				_deleteRoot(file);
				Logger.debug(ViewCMSMaintenanceAction.class,"The directory " + realPath + " has been deleted");
			}
		}
		catch(Exception ex)
		{
			Logger.error(ViewCMSMaintenanceAction.class,ex.toString());
			throw ex;
		}
	}

	private boolean _deleteRoot(File root) throws Exception
	{
		boolean returnValue = true;
		File[] childs = root.listFiles();
		for(int i = 0; i < childs.length; i++)
		{
			File child = childs[i];
			if (child.isFile())
			{
				returnValue = returnValue && child.delete();
			}
			if (child.isDirectory())
			{
				returnValue = returnValue && _deleteRoot(child);
			}
		}
		return returnValue;
	}

	private int _numberRoot(File root) throws Exception
	{
		int returnValue = 0;
		File[] childs = root.listFiles();
		for(int i = 0; i < childs.length; i++)
		{
			File child = childs[i];
			if (child.isFile())
			{
				returnValue++;
			}
			if (child.isDirectory())
			{
				returnValue += _numberRoot(child);
			}
		}
		return returnValue;
	}

	/**
	 * Initialice cache values
	 * @param req
	 */
	private void _initCacheValues(RenderRequest req)
	{
		int liveCount = 0;
		int workingCount = 0;
		try
		{
			Logger.debug(this, "Initializing Cache Values");
			String velocityRootPath =ConfigUtils.getDynamicVelocityPath();
			if (velocityRootPath.startsWith(File.separator + "WEB-INF")) {
				velocityRootPath = FileUtil.getRealPath(velocityRootPath);
			}
			String livePath = velocityRootPath + File.separator + "live";
			String workingPath = velocityRootPath + File.separator + "working";

			//Count the Live Files
			File file = new File(livePath);
			liveCount = _numberRoot(file);
			Logger.debug(this, "Found "+liveCount+" live Files");

			//Count the working files
			file = new File(workingPath);
			workingCount = _numberRoot(file);
			Logger.debug(this, "Found "+workingCount+" working Files");
		}
		catch(Exception ex)
		{
			Logger.error(ViewCMSMaintenanceAction.class,"Error calculating the number of files");
		}
		finally
		{
			req.setAttribute(WebKeys.Cache.CACHE_NUMBER_LIVE_FILES,new Integer(liveCount));
			req.setAttribute(WebKeys.Cache.CACHE_NUMBER_WORKING_FILES,new Integer(workingCount));
		}
	}


	private Map<String,String> searchAndReplace(CmsMaintenanceForm form) throws DotDataException{

		String message="";
		Map<String,String> messageResult = new HashMap<String,String>();
		boolean isAdmin = false;

		if (UtilMethods.isSet(form.getUserId())) {
			try {
				if (APILocator.getUserAPI().isCMSAdmin(APILocator.getUserAPI().loadUserById(form.getUserId(),APILocator.getUserAPI().getSystemUser(),false))) {
					isAdmin = true;
				}
			} catch (NoSuchUserException e) {
				Logger.error(ViewCMSMaintenanceAction.class,"USER DOESN'T EXIST" + e.getMessage(),e);
				message = "Cannot find user";
			} catch (DotSecurityException e) {
				message = "Failed to check for permissions";
				Logger.error(ViewCMSMaintenanceAction.class,e.getMessage(),e);
			}
		}else{
			message = "User not passed";
			Logger.error(ViewCMSMaintenanceAction.class,"No UserId Passed");
		}

		if (isAdmin) {
			String searchString = form.getSearchString();
			String replaceString = form.getReplaceString();
			boolean DBerrorFound = MaintenanceUtil.DBSearchAndReplace(searchString, replaceString);
			if(DBerrorFound){
				message = "Search/Replace Finished with Errors.  See Log for more info";
			}else{
				message = "Search/Replace Finished Successful";
			}
			messageResult.put("type", "message");
			messageResult.put("message", message);
		}
		return messageResult;
	}







	/**
	 * Will zip up all files in the tmp directory and send the result to the
	 * given OutputStream
	 *
	 * @param out
	 *            OutputStream to write the zip files to
	 * @throws IOException
	 * @author Will
	 */
	private void zipTempDirectoryToStream(OutputStream out) throws IOException {
		byte b[] = new byte[512];
		ZipOutputStream zout = new ZipOutputStream(out);
		ZipUtil.zipDirectory(backupTempFilePath, zout);
		zout.close();
		out.close();
	}



	public void _initStructures(ActionForm form,RenderRequest req, RenderResponse res)
	{
		CmsMaintenanceForm maintenanceForm = (CmsMaintenanceForm) form;
		List<Structure> structures = ImmutableList.of();
		//List<Structure> structures = StructureFactory.getStructures();
		//maintenanceForm.setStructures(structures);
	}

	@SuppressWarnings("unchecked")
	private int _dropAssetOldVersions(CmsMaintenanceForm ccf) throws ParseException	{

		Date assetsOlderThan = new SimpleDateFormat("MM/dd/yyyy").parse(ccf.getRemoveassetsdate());
		return CMSMaintenanceFactory.deleteOldAssetVersions(assetsOlderThan);

	}

	/**
	 * Retrieves the basic information of the contents in the
	 * {@code dist_reindex_journal} table that could not be re-indexed and sends
	 * it back to the user as a CSV file. This way users can keep track of them
	 * and check the logs to get more information about the failure.
	 *
	 * @param response
	 *            - The {@link HttpServletResponse} object that allows to send
	 *            the CSV file to the user.
	 */
	@CloseDBIfOpened
	private void downloadRemainingRecordsAsCsv(final HttpServletResponse response) {
		final String fileName = "failed_reindex_records" + new java.util.Date().getTime();
		final String[] fileColumns = new String[] { "ID", "Identifier To Index", "Priority", "Cause" };
		PrintWriter pr = null;
		try {
			response.setContentType("application/octet-stream; charset=UTF-8");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".csv\"");
			pr = response.getWriter();
			pr.print(StringUtils.join(fileColumns, ","));
			pr.print("\r\n");

            final List<ReindexEntry> failedRecords = APILocator.getReindexQueueAPI()
                    .getFailedReindexRecords();
			if (!failedRecords.isEmpty()) {
				for (final ReindexEntry row : failedRecords) {
					final StringBuilder entry = new StringBuilder();
					entry.append(row.getId()).append(", ");
					entry.append(row.getIdentToIndex()).append(", ");
					entry.append(row.getPriority()).append(",");
                    entry.append(row.getLastResult());
					pr.print(entry.toString());
					pr.print("\r\n");
				}
			} else {
				Logger.warn(this, "Re-index table contained zero failed records. The CSV file will not be created.");
			}
		} catch (final Exception e) {
			Logger.error(this, "Download of CSV file with remaining non-indexed records failed.", e);
		} finally {
		    if (null != pr) {
				pr.flush();
				pr.close();
			}
		}
	}

}
