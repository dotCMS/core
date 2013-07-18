package com.dotmarketing.portlets.cmsmaintenance.factories;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.cmsmaintenance.action.ViewCMSMaintenanceAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

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



		/*
		 * Run the drop tasks interatively, moving forward in time 
		 * DROP_OLD_ASSET_ITERATE_BY_SECONDS controls how many seconds to
		 * move forward in time for each iteration - default is to iterate by 30 days
		 */
		Calendar runDate = Calendar.getInstance();
		runDate.setTime(assetsOlderThan);
		runDate.add(Calendar.YEAR, -2);
		
        try{
    		DotConnect dc = new DotConnect();
            String minIdateSQL = "select idate from inode order by idate";
            
            dc.setSQL(minIdateSQL);
            dc.setMaxRows(1);
        	List<Map<String, Object>> map =  dc.loadObjectResults();
        	Date d = (Date) map.get(0).get("idate");
        	if(d !=null)
        		runDate.setTime(d);
        }
        catch(Exception e){
        	Logger.info(CMSMaintenanceFactory.class, "Can't get start date");
        }
		
		

		while(runDate.getTime().before(assetsOlderThan) || runDate.getTime().equals(assetsOlderThan)){
			try	{			
				HibernateUtil.startTransaction();
				Logger.info(CMSMaintenanceFactory.class, "Starting deleteOldAssetVersions for date: "+ UtilMethods.dateToHTMLDate(runDate.getTime(),"yyyy-MM-dd"));
	
				ContentletAPI conAPI = APILocator.getContentletAPI();
	
				Logger.info(CMSMaintenanceFactory.class, "Removing Contentlets");
				auxCount = conAPI.deleteOldContent(runDate.getTime());
				counter  = auxCount;
				Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Contentlets");
	
				Logger.info(CMSMaintenanceFactory.class, "Removing HTML Pages");
				auxCount = APILocator.getHTMLPageAPI().deleteOldVersions(runDate.getTime());
				counter += auxCount;
				Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " HTML Pages");
	
				Logger.info(CMSMaintenanceFactory.class, "Removing Containers");
				auxCount = APILocator.getContainerAPI().deleteOldVersions(runDate.getTime());
				counter += auxCount;
				Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Containers");
	
				Logger.info(CMSMaintenanceFactory.class, "Removing Templates");
				auxCount = APILocator.getTemplateAPI().deleteOldVersions(runDate.getTime());
				counter += auxCount;
				Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Templates");
	
				Logger.info(CMSMaintenanceFactory.class, "Removing Links");
				auxCount = APILocator.getMenuLinkAPI().deleteOldVersions(runDate.getTime());
				counter += auxCount;
				Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " Links");
	
				Logger.info(CMSMaintenanceFactory.class, "Removing File Assets");
	
				auxCount = APILocator.getFileAPI().deleteOldVersions(runDate.getTime());
				counter += auxCount;
				Logger.info(CMSMaintenanceFactory.class, "Removed "+ auxCount+ " File Assets");
	
				Logger.info(CMSMaintenanceFactory.class, "Finished removing old asset versions, removed "+counter+" assets");
				
				
				// This is the last run, break
				if(runDate.getTime().equals(assetsOlderThan)){
					break;
				}
				runDate.add(Calendar.SECOND, Config.getIntProperty("DROP_OLD_ASSET_ITERATE_BY_SECONDS", 60 * 60 * 24 *30));

				// we should never go past the date the user entered
				if(runDate.getTime().after(assetsOlderThan)){
					runDate.setTime(assetsOlderThan);	

				}
				
			}catch(Exception ex){
				try {
					HibernateUtil.rollbackTransaction();
				} catch (DotHibernateException e) {
					Logger.error(CMSMaintenanceFactory.class, e.getMessage());
				}
				Logger.debug(CMSMaintenanceFactory.class,"There was a problem deleting old asset versions",ex);
				Logger.warn(CMSMaintenanceFactory.class,"There  was a problem deleting old asset versions",ex);
				Logger.error(ViewCMSMaintenanceAction.class,ex.toString(), ex);
				if(counter>0){
				    CacheLocator.getCacheAdministrator().flushAll();
				}
				return -1;
			}
			finally {
				try {
					HibernateUtil.commitTransaction();
				} catch (DotHibernateException e) {
					Logger.error(CMSMaintenanceFactory.class, e.getMessage());
					try {
						HibernateUtil.rollbackTransaction();
					} catch (DotHibernateException ex) {
						Logger.error(CMSMaintenanceFactory.class, e.getMessage());
					}
					Logger.debug(CMSMaintenanceFactory.class,"There was a problem deleting old asset versions",e);
					Logger.warn(CMSMaintenanceFactory.class,"There  was a problem deleting old asset versions",e);
					Logger.error(ViewCMSMaintenanceAction.class,e.toString(), e);
					if(counter>0){
					    CacheLocator.getCacheAdministrator().flushAll();
					}
					return -1;
				}
				
			}
		}
		if(counter>0){
		    CacheLocator.getCacheAdministrator().flushAll();
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
	
}
