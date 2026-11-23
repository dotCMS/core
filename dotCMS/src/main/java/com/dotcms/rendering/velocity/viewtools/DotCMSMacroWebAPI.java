package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.browser.BrowserAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DotCMSMacroWebAPI implements ViewTool {

	private static final UserAPI userAPI = APILocator.getUserAPI();
	private BrowserAPI browser = APILocator.getBrowserAPI();

	/** The request the tool was initialised with; it carries the author of the rendered code. */
	private HttpServletRequest request;

	/**
	 * Keeps the request the tool was created for.
	 *
	 * @param obj the {@link ViewContext} of the rendering
	 */
	@Override
	public void init(Object obj) {
		final ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
	}

	/**
	 * Returns the user making the current request, or the anonymous user when nobody is logged
	 * in. The request bound to the current thread wins over the one the tool was created for, so
	 * a tool instance shared between requests still answers for the current one.
	 *
	 * @return the user whose permissions folders, files and content are read with
	 */
	protected User getRequestUser() {
		final HttpServletRequest threadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final HttpServletRequest currentRequest = null != threadRequest ? threadRequest : this.request;
		User user = null;
		if (null != currentRequest) {
			user = WebAPILocator.getUserWebAPI().getUser(currentRequest);
		}
		return null != user ? user : APILocator.getUserAPI().getAnonymousUserNoThrow();
	}

	public List getfileRepository(String folderPath, String searchFolder,
			HttpServletRequest request) throws DotDataException, PortalException, SystemException, DotSecurityException {
		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		User user = com.liferay.portal.util.PortalUtil.getUser(request);
		Folder folder = (Folder) APILocator.getFolderAPI().findFolderByPath(folderPath, host, user, false);
		List returnList = null;

		if(user==null) {
			user = userAPI.getAnonymousUser();
		}

		if (Boolean.parseBoolean(searchFolder)) {
			returnList = APILocator.getFolderAPI().findSubFolders(folder, user, true);
		}
		else {

			Map<String, Object> resultsMap = browser.getFolderContent(user, folder.getInode(), 0, -1, "", null, null, false, false, true, true, null, true);
			returnList = (List<Map<String, Object>>) resultsMap.get("list");
		}
		return returnList;
	}

	public List<Link> getLinkRepository(String folderPath,HttpServletRequest request)
	{
		List<Link> links = new ArrayList<>();
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
			Folder folder = APILocator.getFolderAPI().findFolderByPath(folderPath, host, user, true);
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
		return links;
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

	/**
	 * Lists the files of a folder of the web application. Paths that leave the web application
	 * root or point to {@code WEB-INF} / {@code META-INF} return an empty list.
	 *
	 * @param folder Folder path relative to the web application root, e.g. {@code /html/images}.
	 *
	 * @return One map per file with its name, size, link and modification date.
	 */
	public List getFileSystemFolder(String folder) {
		List list = new ArrayList();
		folder = UtilMethods.cleanFileSystemPathURI(folder);
		if (folder == null) {
			return list;
		}
		final java.io.File file = resolveWebAppFolder(folder);
		if (file == null || !file.exists() || !file.isDirectory()) {
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

	/**
	 * Resolves a folder path against the web application root, returning {@code null} if the
	 * canonical location is outside of it or inside {@code WEB-INF} / {@code META-INF}.
	 */
	static java.io.File resolveWebAppFolder(final String folder) {
		try {
			final java.nio.file.Path root = java.nio.file.Paths.get(FileUtil.getRealPath("/"))
					.toFile().getCanonicalFile().toPath();
			final java.nio.file.Path target = java.nio.file.Paths.get(FileUtil.getRealPath(folder))
					.toFile().getCanonicalFile().toPath();
			if (!target.startsWith(root) || target.startsWith(root.resolve("WEB-INF"))
					|| target.startsWith(root.resolve("META-INF"))) {
				Logger.debug(DotCMSMacroWebAPI.class, "Folder outside of the allowed location: " + folder);
				return null;
			}
			return target.toFile();
		} catch (final Exception e) {
			Logger.debug(DotCMSMacroWebAPI.class, "Unable to resolve folder " + folder + ": " + e.getMessage());
			return null;
		}
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

	   /**
	    * Runs a SQL query on a data source. The data source must be {@code "default"} or a name in the local naming
	    * context; queries on the dotCMS database, under any name, need {@code ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB}.
	    *
	    * @param dataSource the data source name
	    * @param sql        the query
	    * @param startRow   the first row to return
	    * @param maxRow     the maximum number of rows to return
	    * @return the rows, or an error row
	    * @deprecated Use the {@link SQLResultsViewTool} instead, the {@link SQLResultsViewTool} view tool is mapped to the <strong>sql</strong> key.
	    */
	   @Deprecated
	   public ArrayList<HashMap<String, String>> getSQLResults(String dataSource, String sql, int startRow, int maxRow) {

		   ArrayList<HashMap<String, String>> errorResults = new ArrayList<>();

		   if(!canUserEvaluate()){
	            HashMap<String, String> map = new HashMap<>();
	            map.put("hasDotConnectSQLError", "true");
	            map.put("dotConnectSQLError", "External scripting is disabled in your dotcms instance.");
	            errorResults.add(map);
	            return errorResults;
		   }
		   else if (!DbConnectionFactory.isLocalJndiName(dataSource)) {
	            Logger.warn(this, "getSQLResults macro was called with an invalid data source name");
	            HashMap<String, String> map = new HashMap<>();
	            map.put("hasDotConnectSQLError", "true");
	            map.put("dotConnectSQLError", "getSQLResults macro was called with an invalid data source name");
	            errorResults.add(map);
	            return errorResults;
		   }
		   else{
			   if (!UtilMethods.isSet(sql)) {
		            return new ArrayList<>();
		        }
		        if (sql.toLowerCase().indexOf("user_") > -1) {
		            Logger.error(this,"getSQLResults macro is trying to pull from the users table");
		            HashMap<String, String> map = new HashMap<>();
		            map.put("hasDotConnectSQLError", "true");
		            map.put("dotConnectSQLError", "getSQLResults macro is trying to pull from the user_ table");
		            errorResults.add(map);
		            return new ArrayList<>();
		        }
		        if (sql.toLowerCase().indexOf("cms_role") > -1) {
		            Logger.error(this,"getSQLResults macro is trying to pull from the cms_role table");
		            HashMap<String, String> map = new HashMap<>();
		            map.put("hasDotConnectSQLError", "true");
		            map.put("dotConnectSQLError", "getSQLResults macro is trying to pull from the cms_role table");
		            errorResults.add(map);
		            return new ArrayList<>();
		        }
		        if (sql.toLowerCase().indexOf("delete from") > -1 || sql.toLowerCase().indexOf("drop") > -1
		        		|| sql.toLowerCase().indexOf("truncate") > -1) {
		            Logger.error(this,"getSQLResults macro is trying to run a DELETE/DROP/TRUNCATE query");
		            HashMap<String, String> map = new HashMap<>();
		            map.put("hasDotConnectSQLError", "true");
		            map.put("dotConnectSQLError", "getSQLResults macro is trying to run a DELETE/DROP/TRUNCATE query");
		            errorResults.add(map);
		            return new ArrayList<>();
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
		            if (!SQLResultsViewTool.DEFAULT_DATASOURCE.equals(dataSource)) {
		                try {
		                    DbConnectionFactory.getDataSource(dataSource);
		                } catch (final Exception e) {
		                    Logger.warn(this, "getSQLResults macro could not resolve its data source");
		                    HashMap<String, String> map = new HashMap<>();
		                    map.put("hasDotConnectSQLError", "true");
		                    map.put("dotConnectSQLError", "getSQLResults macro was called with an invalid data source name");
		                    errorResults.add(map);
		                    return errorResults;
		                }
		            }
		            // The data source only needs to be checked when queries on the dotCMS database are not allowed.
		            if (!Config.getBooleanProperty(SQLResultsViewTool.ALLOW_DOTCMS_DB_ACCESS, false)) {
		                final boolean dotcmsDatabase;
		                try {
		                    dotcmsDatabase = SQLResultsViewTool.isDotcmsDatabase(dataSource);
		                } catch (final Exception e) {
		                    Logger.warn(this, "getSQLResults macro could not resolve its data source");
		                    HashMap<String, String> map = new HashMap<>();
		                    map.put("hasDotConnectSQLError", "true");
		                    map.put("dotConnectSQLError", "getSQLResults macro was called with an invalid data source name");
		                    errorResults.add(map);
		                    return errorResults;
		                }
		                if (dotcmsDatabase) {
		                    Logger.error(this,"getSQLResults macro is trying to execute queries using the default connection pool. ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB is set to false");
		                    HashMap<String, String> map = new HashMap<>();
		                    map.put("hasDotConnectSQLError", "true");
		                    map.put("dotConnectSQLError", "getSQLResults macro is trying to execute queries using the default connection pool. ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB is set to false");
		                    errorResults.add(map);
		                    return errorResults;
		                }
		            }
		            // Only "default" names the internal pool; any other name is queried through the data source it names.
		            return SQLResultsViewTool.DEFAULT_DATASOURCE.equals(dataSource)
		                    ? dc.getResults()
		                    : dc.getResults(dataSource);
		        } catch (Exception e) {
		            HashMap<String, String> map = new HashMap<>();
		            map.put("hasDotConnectSQLError", "true");
		            map.put("dotConnectSQLError", "There was a sql error:" + e.getMessage());
		            errorResults.add(map);
		            return errorResults;
		        }
		   }
		   
	    }





	/**
	 * returns a string set alphabetically ordered
	 * @param stringSet set to be ordered
	 * @return string set alphabetically ordered
	 */
	public static List<String> sortAlpha(Set<String> stringSet){
		List<String> result = new ArrayList<>(30);

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
		List<String> tempList = new ArrayList<>(30);
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
	 * @throws DotDataException
	 */
	public List<HashMap<String, IFileAsset>> getMediaGalleryFolderFiles(String folderPath, Host host) throws DotDataException {
		return getMediaGalleryFolderFiles(folderPath, host.getIdentifier());
	}


	/**
	 * Returns the files of a folder to show in a media gallery. The site, the folder and its files
	 * are read with the permissions of the user making the request (the anonymous user when nobody
	 * is logged in), so files that user cannot read are left out.
	 *
	 * @param folderPath path of the folder whose files are listed
	 * @param hostId identifier of the site the folder belongs to
	 * @return one map per file, with the keys 'movie' (null when the file is not a movie) and
	 * 'photo' (the file, or the photo of the movie). Empty when the user cannot read the site or
	 * the folder.
	 * @throws DotDataException if the files cannot be loaded
	 */
	public List<HashMap<String, IFileAsset>> getMediaGalleryFolderFiles(String folderPath, String hostId) throws DotDataException {
        folderPath = (folderPath == null) ? "" : folderPath;
        folderPath = folderPath.trim().endsWith("/") ? folderPath.trim() : folderPath.trim() + "/";
        final User user = getRequestUser();
        Folder folder;
		try {
			// Resolve the site here: the hostId overload of findFolderByPath reads the site
			// without front-end roles.
			final Host site = APILocator.getHostAPI().find(hostId, user, true);
			folder = APILocator.getFolderAPI().findFolderByPath(folderPath, site, user, true);
		} catch (final DotSecurityException e) {
			Logger.debug(this, () -> String.format("User '%s' cannot read the requested gallery site or folder",
					user.getUserId()));
			return new ArrayList<>();
		} catch (Exception e) {
			Logger.error(this,e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(),e);
		}
        List<IFileAsset> fileList = new ArrayList<>();
		try {
			// The site and the folder were read as the viewer above. The listing itself runs as the
			// system user because it reads the folder again with the page mode's front-end role
			// setting (off in edit and preview mode); the files are then kept only if the viewer can
			// read them.
			final List<FileAsset> folderFiles = APILocator.getFileAssetAPI().findFileAssetsByFolder(
					folder, "", true, APILocator.getUserAPI().getSystemUser(), false);
			fileList.addAll(APILocator.getPermissionAPI().filterCollection(folderFiles,
					PermissionAPI.PERMISSION_READ, true, user));
		} catch (Exception e) {
			Logger.error(this, e.getMessage());
		}
        List<HashMap<String, IFileAsset>> result = new ArrayList<>();
        HashMap<String, IFileAsset> resultFile;
        List<IFileAsset> noPhotoFile = new ArrayList<>();
        int pos;
        IFileAsset fileListFile;
        String videoPhotoFileName;

        for (IFileAsset file: fileList) {
        	resultFile = new HashMap<>();
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
	 * @throws DotDataException
	 */
	@Deprecated
    public List<HashMap<String, IFileAsset>> getMediaGalleryFolderFiles(String folderPath, long hostId) throws DotDataException {
        return getMediaGalleryFolderFiles(folderPath, String.valueOf(hostId));
    }
	
	/**
	 * Decides whether the code that is calling this tool may run {@link #getSQLResults}.
	 *
	 * <p>Scripting has to be switched on, and the content whose code calls the tool has to have
	 * been last saved by someone holding the
	 * {@link com.dotmarketing.business.Role#SCRIPTING_DEVELOPER} role. That author comes from the
	 * request, where the render that knows what content it is rendering puts it, rather than from
	 * the current Velocity resource name: {@code #parse} lets the template author choose that
	 * name, and the names dotCMS really produces do not parse at all.
	 *
	 * @return {@code true} when this tool may run
	 */
	protected boolean canUserEvaluate(){
		if (!Config.getBooleanProperty("ENABLE_SCRIPTING", false)) {
			Logger.warn(this.getClass(), "Scripting called and ENABLE_SCRIPTING set to false");
			return false;
		}
		return RestrictedViewToolAccess.renderingAuthorHasAccess(this.request);
	}
}
