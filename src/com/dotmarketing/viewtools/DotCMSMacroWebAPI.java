package com.dotmarketing.viewtools;

import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.queryParser.ParseException;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class DotCMSMacroWebAPI implements ViewTool {

	private static final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	private static final UserAPI userAPI = APILocator.getUserAPI();

	public void init(Object obj) {

	}
    
	public List getfileRepository(String folderPath, String searchFolder,
			HttpServletRequest request) throws DotDataException, PortalException, SystemException, DotSecurityException {

		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);

		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);

		Folder folder = CacheLocator.getFolderCache().getFolderByPathAndHost(folderPath, host);

		if (Boolean.parseBoolean(searchFolder)) {
			return APILocator.getFolderAPI().findSubFolders(folder, user, true);
		} 
		else 
		{		
			return APILocator.getFolderAPI().getLiveFilesSortTitle(folder, user, true);
		}

	}
	
	public List<Link> getLinkRepository(String folderPath,HttpServletRequest request) 
	{	
		List<Link> links = new ArrayList<Link>();
		try
		{		
			Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
			Role[] roles;
			User user = (User) request.getSession().getAttribute("CMS_USER");
			if (user != null) {
				List rolesList = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
				roles = new Role[rolesList.size() + 1];
				int i = 0;
				for (i = 0; i < rolesList.size(); ++i) {
					roles[i] = (Role) rolesList.get(i);
				}
				roles[i] = APILocator.getRoleAPI().loadCMSAnonymousRole();

			} else {
				roles = new Role[]{APILocator.getRoleAPI().loadCMSAnonymousRole()};
			}
			roles[0].getId();
			Folder folder = CacheLocator.getFolderCache().getFolderByPathAndHost(folderPath, host);
			if(InodeUtils.isSet(folder.getInode()))
			{
				links = APILocator.getFolderAPI().getLinks(folder, user, false); 
			}
			else
			{
				Logger.debug(DotCMSMacroWebAPI.class,"Folder:" + folderPath + " was not found");
			}
		}
		catch(Exception ex)
		{
			Logger.debug(DotCMSMacroWebAPI.class,ex.toString());
		}
		finally
		{
			return links;
		}
	}

	public String getFolderPath(String rootFolder, HttpServletRequest request) {

		String folderPath = request.getParameter("folderPath");

		if (folderPath == null || folderPath.indexOf(rootFolder) != 0
				|| folderPath.length() < rootFolder.length()) {
			return rootFolder;
		} else {
			return folderPath;
		}

	}

	//GET EVENTS
	/**
	 * Get a list of x number of events, from a category inode list in a range of dates.
	 * if startDayOffset is null or 0, the start date begin from today at 00:00:00.
	 * if daysToShow is null or cero, en end date is today at 00:00:00, if not the
	 * end date if from the start date + daysToShow
	 * @param categories a string list of comma separated categories inodes
	 * @param limit max number of events to get
	 * @param startDayOffset
	 * @param daysToShow
	 * @return a list of events
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public List getEvents(String categories, int limit, int startDayOffset,
			String daysToShow) throws DotDataException, DotSecurityException {

		int daysToShowInt = 0;
		if (UtilMethods.isSet(daysToShow)) {
			daysToShowInt = Integer.parseInt(daysToShow);
		}
		return getEvents(categories, limit, startDayOffset, daysToShowInt);
	}

	/**
	 * Get a list of x number of events, from a category inode list in a range of dates.
	 * if startDayOffset is null or 0, the start date begin from today at 00:00:00.
	 * if daysToShow is null or cero, en end date is today at 00:00:00, if not the
	 * end date if from the start date + daysToShow
	 * @param categories a string list of comma separated categories inodes
	 * @param limit max number of events to get
	 * @param startDayOffset
	 * @param daysToShow
	 * @return a list of events
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public List getEvents(String categories, int limit, String startDayOffset,
			int daysToShow) throws DotDataException, DotSecurityException {

		int startDayOffsetInt = 0;
		if (UtilMethods.isSet(startDayOffset)) {
			startDayOffsetInt = Integer.parseInt(startDayOffset);
		}

		return getEvents(categories, limit, startDayOffsetInt, daysToShow);
	}

	/**
	 * Get a list of x number of events, from a category inode list in a range of dates.
	 * if startDayOffset is null or 0, the start date begin from today at 00:00:00.
	 * if daysToShow is null or cero, en end date is today at 00:00:00, if not the
	 * end date if from the start date + daysToShow
	 * @param categories a string list of comma separated categories inodes
	 * @param limit max number of events to get
	 * @param startDayOffset
	 * @param daysToShow
	 * @return a list of events
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public List getEvents(String categories, int limit, String startDayOffset,
			String daysToShow) throws DotDataException, DotSecurityException {

		int startDayOffsetInt = 0;
		if (UtilMethods.isSet(startDayOffset)) {
			startDayOffsetInt = Integer.parseInt(startDayOffset);
		}
		int daysToShowInt = 0;
		if (UtilMethods.isSet(daysToShow)) {
			daysToShowInt = Integer.parseInt(daysToShow);
		}
		return getEvents(categories, limit, startDayOffsetInt, daysToShowInt);
	}

	/**
	 * Get a list of x number of events, from a category inode list in a range of dates.
	 * if startDayOffset is null or 0, the start date begin from today at 00:00:00.
	 * if daysToShow is null or cero, en end date is today at 00:00:00, if not the
	 * end date if from the start date + daysToShow
	 * @param categories a string list of comma separated categories inodes
	 * @param limit max number of events to get
	 * @param startDayOffset
	 * @param daysToShow
	 * @return a list of events
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public List getEvents(String categories, int limit, int startDayOffset,
			int daysToShow) throws DotDataException, DotSecurityException {

		GregorianCalendar fromCal = new GregorianCalendar();
		GregorianCalendar toCal = new GregorianCalendar();

		if (startDayOffset != 0) {
			fromCal.add(GregorianCalendar.DATE, startDayOffset);
			toCal.add(GregorianCalendar.DATE, startDayOffset);
		}

		if (daysToShow != 0 && daysToShow > 1) {
			toCal.add(GregorianCalendar.DATE, daysToShow - 1);
		}

		fromCal.set(GregorianCalendar.HOUR_OF_DAY, 0);
		fromCal.set(GregorianCalendar.MINUTE, 0);
		fromCal.set(GregorianCalendar.SECOND, 1);

		toCal.set(GregorianCalendar.HOUR_OF_DAY, 23);
		toCal.set(GregorianCalendar.MINUTE, 59);
		toCal.set(GregorianCalendar.SECOND, 58);
		
		String[] categorieInodes=categories.split(",");
		List<Category> categoriesList=new ArrayList<Category>(categorieInodes.length);
		for(String inode : categorieInodes)
			categoriesList.add(categoryAPI.find(inode, userAPI.getSystemUser(), false));
		
		return APILocator.getEventAPI().find(fromCal.getTime(), toCal.getTime(),
				null, null, categoriesList, true, false, 0, limit, userAPI.getSystemUser(), false);
	}

	public List getFileSystemFolder(String folder) {
		List list = new ArrayList();
		/*
		if(session.getAttribute(WebKeys.CMS_USER) == null){
			return list;
		}
		*/
		folder = UtilMethods.cleanFileSystemPathURI(folder);
		java.io.File file = new java.io.File(Config.CONTEXT.getRealPath(folder));
		if (!file.exists() || !file.isDirectory()) {
			return list;
		}
		String[] files = file.list(new SystemFileNameFilter());
		for (int i = 0; i < files.length; i++) {
			java.io.File myFile = new java.io.File(file.getAbsolutePath()
					+ java.io.File.separatorChar + files[i]);
			Map map = new HashMap();
			map.put("name", files[i]);
			long size = myFile.length() / 1024;
			if (size < 1)
				size = 1;
			long k = size % 1024;
			long mb = size / 1024;
			long percent = (k * 10) / 1024;
			Date d = new Date();
			d.setTime(myFile.lastModified());
			String prettySize = (mb > 1) ? mb + "." + percent + " mb" : k + " kb";
			map.put("size", String.valueOf(myFile.length() / 1024));
			map.put("prettySize", prettySize);
			map.put("link", folder + "/" + files[i]);
			map.put("date", d);
			map.put("prettyDate", UtilMethods.dateToPrettyHTMLDate(d));
			list.add(map);
		}

		return list;

	}

	private class SystemFileNameFilter implements FilenameFilter {

		public boolean accept(java.io.File directory, String fileName) {

			if (fileName.startsWith(".") || UtilMethods.cleanFileSystemPathURI(fileName) == null) {
				return false;
			}

			java.io.File file = new java.io.File(directory.getAbsolutePath()
					+ java.io.File.separatorChar + fileName);
			if (file.isDirectory()) {
				return false;
			}

			return true;

		}

	}

	   public ArrayList<HashMap<String, String>> getSQLResults(String dataSource, String sql, int startRow, int maxRow) {
	        ArrayList<HashMap<String, String>> errorResults = new ArrayList<HashMap<String, String>>();
	        if (!UtilMethods.isSet(sql)) {
	            return new ArrayList<HashMap<String, String>>();
	        }
	        if (sql.toLowerCase().indexOf("user_") > -1) {
	            Logger.error(this,"getSQLResults macro is trying to pull from the users table");
	            HashMap<String, String> map = new HashMap<String, String>();
	            map.put("hasDotConnectSQLError", "true");
	            map.put("dotConnectSQLError", "getSQLResults macro is trying to pull from the user_ table");
	            errorResults.add(map);
	            return new ArrayList<HashMap<String, String>>();
	        }
	        if (sql.toLowerCase().indexOf("role_") > -1) {
	            Logger.error(this,"getSQLResults macro is trying to pull from the role_ table");
	            HashMap<String, String> map = new HashMap<String, String>();
	            map.put("hasDotConnectSQLError", "true");
	            map.put("dotConnectSQLError", "getSQLResults macro is trying to pull from the role_ table");
	            errorResults.add(map);
	            return new ArrayList<HashMap<String, String>>();
	        }
	        try {
	            DotConnect dc = new DotConnect();

	            dc.setSQL(sql);
	            if (startRow > 0) {
	                dc.setStartRow(startRow);
	            }
	            if (maxRow > 0) {
	                dc.setMaxRows(maxRow);
	            }
	            if (dataSource.equals("default")) {
	                return dc.getResults();
	            } else {
	                return dc.getResults(dataSource);
	            }
	        } catch (Exception e) {
	            HashMap<String, String> map = new HashMap<String, String>();
	            map.put("hasDotConnectSQLError", "true");
	            map.put("dotConnectSQLError", "There was a sql error:" + e.getMessage());
	            errorResults.add(map);
	            return errorResults;
	        }
	    }


	/**
	 * returns a structure using the structure name
	 * @param structureName name of the structure to look for
	 * @return structure
	 */
	public static Structure getStructureByName(String structureName) {
		Structure st = StructureCache.getStructureByName(structureName);
		return st;
	}


	/**
	 * returns a map of the top most popular tags [numberOfTags] used by the given list of structures
	 * @param structures Name of the structures to query
	 * @param maxNumberOfTags maximum number of tag to be returned, if 0 is provided, then
	 * returns all tags
	 * @return map of the top most popular tags
	 */
	public static Map<String, Integer> getTagsCount(List<String> structures, int maxNumberOfTags, List<String> ignoredTags, HttpServletRequest request){

		HashMap<String, Integer> tagsCount = new HashMap<String, Integer>();
		for (String structure : structures) {

			Structure st = StructureCache.getStructureByName(structure);
			if(st == null)
				continue;
			
			String languageId = "";
			if(request.getParameter("language")!=null)
					languageId= " +languageId:"+request.getParameter("language");

			ArrayList<Field> tagFields = StructureFactory.getTagsFields(st.getInode());
			List<String> ignoredTagsList = new ArrayList<String>();
			if(ignoredTags == null) ignoredTags = new ArrayList<String>();
			for (String ignoredTag : ignoredTags)
				ignoredTagsList.add(ignoredTag.trim());

			String query = "+type:content +deleted:false +live:true "+languageId+" +structureName:" + st.getVelocityVarName();
			List<Contentlet> hits = new ArrayList <Contentlet>();
			ContentletAPI conAPI = APILocator.getContentletAPI();
			
			try {
				hits = conAPI.search(query,  -1, 0, null, userAPI.getSystemUser(), false);
			} catch (DotDataException e) {
				Logger.debug(DotCMSMacroWebAPI.class,"Error retriving data");
			} catch (DotSecurityException e) {
				Logger.debug(DotCMSMacroWebAPI.class,"Error retriving data, user does not have permissions");
			} catch (Exception e) {
				Logger.debug(DotCMSMacroWebAPI.class,"Error retriving data, parsing exception");
			}


			for (Contentlet contentlet: hits) {
				
				for (Field tagField : tagFields) {
					String tagsValue = (String) contentlet.getMap().get(tagField.getVelocityVarName());
					if(UtilMethods.isSet(tagsValue)) {
						String[] tags = tagsValue.split(",");
						for (String tag : tags) {
							tag = tag.trim();
							if(ignoredTagsList.contains(tag) || !UtilMethods.isSet(tag))
								continue;
							if(tagsCount.containsKey(tag)) {
								int count = tagsCount.get(tag);
								tagsCount.put(tag, count+1);
							} else {
								tagsCount.put(tag, 1);
							}
						}
					}
				}
			}
		}


		List<String> tempList = new ArrayList<String>(30);
		HashMap<String, Integer> result = new HashMap<String, Integer>();

		if (maxNumberOfTags == 0) {
			return tagsCount;
		}

		int i = 0;
		Set<String> keySet = tagsCount.keySet();
		for (String stringKey: keySet) {
			if (tempList.size() == 0) {
				tempList.add(stringKey);
			} else {
				for (i = 0; i < tempList.size(); ++i) {

					int tagCount = ((Integer) tagsCount.get(stringKey)).intValue();
					int tempTagCount = ((Integer) tagsCount.get(tempList.get(i))).intValue();

					if (tagCount > tempTagCount) {
						break;
					}
				}

				tempList.add(i, stringKey);
			}
		}

		i = 0;
		while (i < maxNumberOfTags && i < tempList.size()) {
			result.put(tempList.get(i), tagsCount.get(tempList.get(i)));
			i++;
		}

		return result;
	}


	/**
	 * returns a string set alphabetically ordered
	 * @param stringSet set to be ordered
	 * @return string set alphabetically ordered
	 */
	public static List<String> sortAlpha(Set<String> stringSet){
		List<String> result = new ArrayList<String>(30);

		int i;
		String tempStringKey;
		for (String stringKey: stringSet) {
			if (result.size() == 0) {
				result.add(stringKey);
			} else {
				for (i = 0; i < result.size(); ++i) {
					tempStringKey = result.get(i);
					if (0 < tempStringKey.compareToIgnoreCase(stringKey)) {
						break;
					}
				}

				result.add(i, stringKey);
			}
		}

		return result;
	}




	/**
	 * returns a map of the top most popular tags, this map has the tag name as a key and
	 * the tag count as value 
	 * @param tagHashMap map to be verified, in order to get the top most popular tags
	 * @param maxNumberOfTags maximum number of tag to be returned, if 0 is provided, then
	 * returns the original map, i.e. returns all tags
	 * @return map of the top most popular tags
	 */
	public static HashMap topMostPopularTags(HashMap tagHashMap, int maxNumberOfTags){
		List<String> tempList = new ArrayList<String>(30);
		HashMap result = new HashMap();
		
		int i;
		Set<String> keySet = tagHashMap.keySet();
		for (String stringKey: keySet) {
			if (tempList.size() == 0) {
				tempList.add(stringKey);
			} else {
				for (i = 0; i < tempList.size(); ++i) {

					int tagCount = ((Integer) tagHashMap.get(stringKey)).intValue();
					int tempTagCount = ((Integer) tagHashMap.get(tempList.get(i))).intValue();
					
					if (tagCount > tempTagCount) {
						break;
					}
				}
				
				tempList.add(i, stringKey);
			}
		}

		i = 0;
		while (i < maxNumberOfTags && i < tempList.size()) {
			result.put(tempList.get(i), tagHashMap.get(tempList.get(i)));
			i++;
		}
		
		return result;
	}
	
	/**
	 * Return a list of hashmap with the files to show in the media gallery 
	 * @param folderPath String with the path with the files to show.
	 * @param host Host where the folder path belongs.
	 * @return List of the hashmap with the files to show in the media gallery. Each hashmap can contain these keys: 'movie' (null if there is no movie file) or 'photo' (The photo file. If there is a movie file, this will be the photo associated to the movie).
	 */
	public List<HashMap<String, IFileAsset>> getMediaGalleryFolderFiles(String folderPath, Host host) {
		return getMediaGalleryFolderFiles(folderPath, host.getIdentifier());
	}
	
	
	public List<HashMap<String, IFileAsset>> getMediaGalleryFolderFiles(String folderPath, String hostId) {
        folderPath = (folderPath == null) ? "" : folderPath;
        folderPath = folderPath.trim().endsWith("/") ? folderPath.trim() : folderPath.trim() + "/";
        Folder folder = new Folder();
		try {
			folder = APILocator.getFolderAPI().findFolderByPath(folderPath, hostId,userAPI.getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(),e);
		}
        List<IFileAsset> fileList = new ArrayList<IFileAsset>();
		try {
			boolean live = true;
			fileList.addAll(APILocator.getFolderAPI().getLiveFilesSortTitle(folder, null, true));
			fileList.addAll(APILocator.getFileAssetAPI().findFileAssetsByFolder(folder, FileAssetAPI.FILE_NAME_FIELD + "asc", true, APILocator.getUserAPI().getSystemUser(), false));
		} catch (Exception e) {
			Logger.error(this, e.getMessage());
		} 
        List<HashMap<String, IFileAsset>> result = new ArrayList<HashMap<String, IFileAsset>>();
        HashMap<String, IFileAsset> resultFile;
        List<IFileAsset> noPhotoFile = new ArrayList<IFileAsset>();
        int pos;
        IFileAsset fileListFile;
        String videoPhotoFileName;
        
        for (IFileAsset file: fileList) {
        	resultFile = new HashMap<String, IFileAsset>();
			if (file.getExtension().toLowerCase().endsWith("flv")) {
				resultFile.put("movie", file);
				
				videoPhotoFileName = file.getURI().substring(0,file.getURI().length()-4) + ".jpg";
				for (pos = 0; pos < fileList.size(); ++pos) {
					fileListFile = fileList.get(pos);
					if (videoPhotoFileName.equals(fileListFile.getURI())) {
						resultFile.put("photo", fileListFile);
						noPhotoFile.add(fileListFile);
						break;
					}
				}
			} else {
				resultFile.put("photo", file);
			}
			result.add(resultFile);
        }
        
        for (IFileAsset file: noPhotoFile) {
        	for (pos = 0; pos < result.size(); ++pos) {
        		resultFile = result.get(pos);
        		if ((resultFile.get("movie") == null) && 
        			(resultFile.get("photo").equals(file))) {
        			result.remove(pos);
        			break;
        		}
        	}
        }
        
        return result;
    }
	
	/**
	 * Return a list of hashmap with the files to show in the media gallery 
	 * @param folderPath String with the path with the files to show.
	 * @param host long with the hostId of the host where the folder path belongs.
	 * @return List of the hashmap with the files to show in the media gallery. Each hashmap can contain these keys: 'movie' (null if there is no movie file) or 'photo' (The photo file. If there is a movie file, this will be the photo associated to the movie).
	 */
	@Deprecated
    public List<HashMap<String, IFileAsset>> getMediaGalleryFolderFiles(String folderPath, long hostId) {
        return getMediaGalleryFolderFiles(folderPath, String.valueOf(hostId));
    }
}