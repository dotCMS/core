package com.dotmarketing.portlets.cmsmaintenance.factories;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.cmsmaintenance.action.ViewCMSMaintenanceAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This class is use to fix identifiers inconsistencies in db,
 * solving the problem of identifiers without a working or live asset
 * @author Oswaldo
 *
 */
public class CMSMaintenanceFactory {

	public static int deleteOldAssetVersions (Date assetsOlderThan) {
		int counter = 0;
		int auxCount = 0;

		try {
			HibernateUtil.startTransaction();
		} catch (DotHibernateException e) {
			Logger.error(CMSMaintenanceFactory.class, e.getMessage());
		}
		try	{

			String condition = "live <> "+DbConnectionFactory.getDBTrue()+" and working <> "+
			DbConnectionFactory.getDBTrue()+" and mod_date < '" + UtilMethods.dateToHTMLDate(assetsOlderThan,"dd-MMM-yyyy").toUpperCase() + "'";

			Logger.info(CMSMaintenanceFactory.class, "Starting deleteOldAssetVersions for date: "+ UtilMethods.dateToHTMLDate(assetsOlderThan,"yyyy-MM-dd")+" and condition: " + condition);

			ContentletAPI conAPI = APILocator.getContentletAPI();

			Logger.info(CMSMaintenanceFactory.class, "Removing Contentlets");
			auxCount = conAPI.deleteOldContent(assetsOlderThan, 500);
			counter  = auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Contentlets");

			Logger.info(CMSMaintenanceFactory.class, "Removing HTML Pages");
			auxCount = deleteOldAssets(assetsOlderThan, "htmlpage", 500);
			counter += auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " HTML Pages");

			Logger.info(CMSMaintenanceFactory.class, "Removing Containers");
			auxCount = deleteOldAssets(assetsOlderThan, "containers", 500);
			counter += auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Containers");

			Logger.info(CMSMaintenanceFactory.class, "Removing Templates");
			auxCount =  deleteOldAssets(assetsOlderThan, "template", 500);
			counter += auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Templates");

			Logger.info(CMSMaintenanceFactory.class, "Removing Links");
			auxCount = deleteOldAssets(assetsOlderThan, "links", 500);
			counter += auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Links");

			Logger.info(CMSMaintenanceFactory.class, "Removing File Assets");

			auxCount = deleteOldFiles(assetsOlderThan, 500);
			counter += auxCount;
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " File Assets");

			auxCount = deleteOldBinary();
			Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Binaries");

			Logger.info(CMSMaintenanceFactory.class, "Finished removing old asset versions, removed "+counter+" assets");


		}catch(Exception ex){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e) {
				Logger.error(CMSMaintenanceFactory.class, e.getMessage());
			}
			Logger.debug(CMSMaintenanceFactory.class,"There was a problem deleting old asset versions",ex);
			Logger.warn(CMSMaintenanceFactory.class,"There  was a problem deleting old asset versions",ex);
			Logger.error(ViewCMSMaintenanceAction.class,ex.toString(), ex);
			return -1;
		}

		try {
			HibernateUtil.commitTransaction();
		} catch (DotHibernateException e) {
			Logger.error(CMSMaintenanceFactory.class, e.getMessage());
		}
		return counter;

	}

	/**
	 * This method get the File date from a dotmarketing file object
	 * @param file dotmarketing File object
	 * @return Java io.File
	 * @throws IOException
	 */
	private static java.io.File getAssetIOFile (com.dotmarketing.portlets.files.model.File file) throws IOException {

		String fileName = file.getFileName();
		String suffix = UtilMethods.getFileExtension(fileName);

		String assetsPath = APILocator.getFileAPI().getRealAssetsRootPath();
		String fileInode = file.getInode();

		// creates the path where to save the working file based on the inode
		Logger.debug(CMSMaintenanceFactory.class, "Creating path to save the working file with inode: "+ fileInode);
		String fileFolderPath = String.valueOf(fileInode);
		if (fileFolderPath.length() == 1) {
			fileFolderPath = fileFolderPath + "0";
		}

		fileFolderPath = assetsPath + java.io.File.separator +
		fileFolderPath.substring(0, 1) + java.io.File.separator +
		fileFolderPath.substring(1, 2);

		new java.io.File(fileFolderPath).mkdirs();

		String filePath = fileFolderPath + java.io.File.separator +
		fileInode + "." + suffix;

		// creates the new file as
		// inode{1}/inode{2}/inode.file_extension
		java.io.File assetFile = new java.io.File(filePath);
		if (!assetFile.exists())
			Logger.debug(CMSMaintenanceFactory.class, "Creating new file in:  "+ filePath);
			assetFile.createNewFile();

		return assetFile;
	}


	/**
	 * Remove assets in DB older than the specified date.
	 *
	 * @param assetsOlderThan
	 * @param asset
	 * @param offset
	 * @return int
	 * @exception DotDataException
	 */
	@SuppressWarnings("unchecked")
	private static int deleteOldAssets(Date assetsOlderThan, String asset, int offset) throws DotDataException {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(assetsOlderThan);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date date = calendar.getTime();
		DotConnect dc = new DotConnect();

		String countSQL = ("select count(*) as count from "+ asset);
		dc.setSQL(countSQL);
		List<Map<String, String>> result = dc.loadResults();
		int before = Integer.parseInt(result.get(0).get("count"));
		String versionInfoTable = UtilMethods.getVersionInfoTableName(asset);

		String getInodesSQL = "select inode from inode where inode in (select inode from "+asset+" where  "+
         " mod_date < ? and not exists (select * from "+versionInfoTable+
         " where working_inode="+asset+".inode or live_inode="+asset+".inode))";

		dc.setSQL(getInodesSQL);
		dc.addParam(date);
		List<Map<String, Object>> results = dc.loadResults();
		int lenght = results.size();
		boolean first = true;

		String deleteContentletSQL = "delete from "+asset+" where  "+
		 " mod_date < ? and not exists (select * from "+versionInfoTable+
		 " where working_inode="+asset+".inode or live_inode="+asset+".inode)";

		dc.setSQL(deleteContentletSQL);
		dc.addParam(date);
		dc.loadResult();

		Logger.info(CMSMaintenanceFactory.class, "Deleting "+lenght+" assets");
		if(lenght>0){
			StringBuffer deleteInodeSQL = new StringBuffer("delete from inode where inode in(");
			first = true;
			List<String> inodesToDelete = new ArrayList<String>();

			for(int i = 0;i < lenght;i++)
			{
				Map<String, Object> hash = (Map<String, Object>) results.get(i);
				String inode = (String) hash.get("inode");
				inodesToDelete.add(inode);
				if(!first){
					deleteInodeSQL.append(",'" + inode + "'");
				}else{
					deleteInodeSQL.append("'" + inode + "'");

				}
				first = false;

				if((i % offset) == 0 && i != 0)
				{
					deleteInodeSQL.append(")");
					dc.setSQL(deleteInodeSQL.toString());
					MaintenanceUtil.cleanInodesFromTree(inodesToDelete, offset);
					dc.loadResult();
					deleteInodeSQL = new StringBuffer("delete from inode where inode in(");
					first = true;

				}

			}
			if(!(lenght % offset == 0) && inodesToDelete.size()>0)
			{
				deleteInodeSQL.append(")");
				dc.setSQL(deleteInodeSQL.toString());
				MaintenanceUtil.cleanInodesFromTree(inodesToDelete, offset);
				dc.loadResult();
			}

		}else{
			Logger.info(CMSMaintenanceFactory.class, "No assets to delete");
		}
		MaintenanceUtil.cleanMultiTreeTable();

		dc.setSQL(countSQL);
		result = dc.loadResults();
		int after = Integer.parseInt(result.get(0).get("count"));
		return before - after;

	}

	/**
	 * Remove assets in the file system older than the speficied date.
	 *
	 * @param assetsOlderThan
	 * @param offset
	 * @return int
	 * @exception DotDataException
	 */
	@SuppressWarnings("unchecked")
	private static int deleteOldFiles(Date assetsOlderThan, int offset) throws DotDataException {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(assetsOlderThan);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date date = calendar.getTime();
		DotConnect dc = new DotConnect();

		//List<com.dotmarketing.portlets.files.model.File> fileList = (List<com.dotmarketing.portlets.files.model.File>)InodeFactory.getInodesOfClassByCondition(com.dotmarketing.portlets.files.model.File.class, condition);
		StringBuffer getInodesSQL = new StringBuffer("select inode from inode where inode in (select inode from file_asset where mod_date < ? ");
		getInodesSQL.append(" and inode not in (select working_inode from fileasset_version_info) and inode not in (select live_inode from fileasset_version_info)) ");
		dc.setSQL(getInodesSQL.toString());
		dc.addParam(date);
		List<Map<String, String>> results = dc.loadResults();

		com.dotmarketing.portlets.files.model.File file;
		FileAPI fileAPI = APILocator.getFileAPI();

		User user = null;

		try {
			user = APILocator.getUserAPI().getSystemUser();
		} catch (Exception e) {
			Logger.warn(CMSMaintenanceFactory.class, e.toString());
		}

		for(Map<String, String> inode: results){
			try {
				file = fileAPI.get(inode.get("inode"), user, false);
				File dataFile = getAssetIOFile(file);
				dataFile.delete();

				//search for tumbnails and resized images
				Logger.info(CMSMaintenanceFactory.class, "Searching for thumbnails and resized images");
				if(UtilMethods.isImage(file.getFileName())){
					String assetsPath = APILocator.getFileAPI().getRealAssetsRootPath();
					String fileInode = file.getInode();

					// creates the path where to save the working file based on the inode
					String fileFolderPath = String.valueOf(fileInode);
					if (fileFolderPath.length() == 1) {
						fileFolderPath = fileFolderPath + "0";
					}

					fileFolderPath = assetsPath + java.io.File.separator +
					fileFolderPath.substring(0, 1) + java.io.File.separator +
					fileFolderPath.substring(1, 2);
					java.io.File[] fileArrays = new java.io.File(fileFolderPath).listFiles();
					int fileCount = 0;
					Logger.debug(CMSMaintenanceFactory.class, "Found "+ fileArrays.length + " thumbnails and resized images");
					for(java.io.File file2 : fileArrays){
						if(file2.getName().indexOf(file.getInode()+"_resized") != -1 ||
								file2.getName().indexOf(file.getInode()+"_thumb") != -1){
							Logger.debug(CMSMaintenanceFactory.class, "Deleting file: "+ file2.getName());
							file2.delete();
							fileCount+=1;
						}
					}
					Logger.info(CMSMaintenanceFactory.class, "Deleted "+fileCount+" thumbnails and resized images");
				}
			} catch (Exception e) {
				Logger.warn(CMSMaintenanceFactory.class, e.toString());
			}
		}

		HibernateUtil.getSession().clear();
		
		return deleteOldAssets(assetsOlderThan, "file_asset", 500);
	}

	private static int deleteOldBinary() {
        int result = 0;

		String assetsPath = APILocator.getFileAPI().getRealAssetsRootPath();
		File assetsRootFolder = new File(assetsPath);
		if (!assetsRootFolder.exists() || !assetsRootFolder.isDirectory())
			return result;

		String reportsPath = "";
		if (UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))) {
			reportsPath = Config.getStringProperty("ASSET_REAL_PATH") + File.separator + Config.getStringProperty("REPORT_PATH");
		} else {
			reportsPath = Config.CONTEXT.getRealPath(File.separator + Config.getStringProperty("ASSET_PATH") + File.separator + Config.getStringProperty("REPORT_PATH"));
		}
		File reportsFolder = new File(reportsPath);

		String messagesPath = "";
		if (UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))) {
			messagesPath = Config.getStringProperty("ASSET_REAL_PATH") + File.separator + "messages";
		} else {
			messagesPath = Config.CONTEXT.getRealPath(File.separator + Config.getStringProperty("ASSET_PATH") + "messages");
		}
		File messagesFolder = new File(messagesPath);

		ContentletAPI contentletAPI = APILocator.getContentletAPI();
		Contentlet contentlet;

		User user = null;
		try {
			user = APILocator.getUserAPI().getSystemUser();
		} catch (Exception e) {
			Logger.warn(CMSMaintenanceFactory.class, e.toString());
		}

		File[] filesLevel1 = assetsRootFolder.listFiles();
		for (File fileLevel1: filesLevel1) {
			if(!fileLevel1.getPath().equals(assetsRootFolder.getPath()+java.io.File.separator+"license")
					&& !fileLevel1.getPath().equals(reportsFolder.getPath())
					&& !fileLevel1.getPath().equals(messagesFolder.getPath())){
				if (fileLevel1.isDirectory()) {
					File[] filesLevel2 = fileLevel1.listFiles();
					for (File fileLevel2: filesLevel2) {
						if (fileLevel2.isDirectory()) {
							File[] filesLevel3 = fileLevel2.listFiles();
							for (File fileLevel3: filesLevel3) {
								if (fileLevel3.isDirectory()) {
									try {
										contentlet = contentletAPI.find(fileLevel3.getName(), user, false);
										if ((contentlet == null) || !InodeUtils.isSet(contentlet.getInode())) {
											deleteFolder(fileLevel3);

											++result;
										}
									} catch (Exception e) {
										Logger.warn(CMSMaintenanceFactory.class, e.toString());
									}
								}
							}
						}
					}
				}
			}
		}

		return result;
	}

	private static boolean deleteFolder(File folder) {
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			boolean result;
			for (File file: files) {
				result = deleteFolder(file);
				if (result == false)
					return result;
			}
		}

		return folder.delete();
	}
}
