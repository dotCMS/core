package com.dotcms.business;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

/**
 * This is a lazy initialization wrapper for the {@link FileAPI}. Because of the
 * addition of class constructors meant for unit tests, there's a high chance of
 * a cyclic dependency causing the server to crash. This wrapper class avoids
 * that situation, so that the constructors are initialized correctly without
 * interfering with each other.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jan 5, 2017
 *
 */
public class LazyFileAPIWrapper implements FileAPI {

	/**
	 * Singleton holder using initialization on demand
	 */
	private static class SingletonHolder {
		private static final FileAPI INSTANCE = APILocator.getFileAPI();
	}

	/**
	 * Returns a unique instance of the {@link FileAPI} class.
	 * 
	 * @return The {@link FileAPI} class.
	 */
	private FileAPI getInstance() {
		return LazyFileAPIWrapper.SingletonHolder.INSTANCE;
	}

	@Override
	public File copy(File source, Folder destination, boolean forceOverwrite, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return getInstance().copy(source, destination, forceOverwrite, user, respectFrontendRoles);
	}

	@Override
	public File find(String inode, User user, boolean respectFrontendRoles)
			throws DotStateException, DotDataException, DotSecurityException {
		return getInstance().find(inode, user, respectFrontendRoles);
	}

	@Override
	public String getRelativeAssetPath(Inode inode) {
		return getInstance().getRealAssetPath(inode);
	}

	@Override
	public File getWorkingFileByFileName(String fileName, Folder folder, User user, boolean respectFrontendRoles)
			throws DotStateException, DotDataException, DotSecurityException {
		return getInstance().getWorkingFileByFileName(fileName, folder, user, respectFrontendRoles);
	}

	@Override
	public File saveFile(File file, java.io.File fileData, Folder folder, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return getInstance().saveFile(file, fileData, folder, user, respectFrontendRoles);
	}

	@Override
	public String getRealAssetsRootPath() {
		return getInstance().getRealAssetsRootPath();
	}

	@Override
	public String getRelativeAssetsRootPath() {
		return getInstance().getRelativeAssetsRootPath();
	}

	@Override
	public boolean delete(File file, User user, boolean respectFrontendRoles) throws DotSecurityException, Exception {
		return getInstance().delete(file, user, respectFrontendRoles);
	}

	@Override
	public java.io.File getAssetIOFile(File file) throws IOException {
		return getInstance().getAssetIOFile(file);
	}

	@Override
	public List<File> getAllHostFiles(Host parentHost, boolean live, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return getInstance().getAllHostFiles(parentHost, live, user, respectFrontendRoles);
	}

	@Override
	public List<File> getFolderFiles(Folder parentFolder, boolean live, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return getInstance().getFolderFiles(parentFolder, live, user, respectFrontendRoles);
	}

	@Override
	public List<Map<String, Serializable>> DBSearch(Query query, User user, boolean respectFrontendRoles)
			throws ValidationException, DotDataException {
		return getInstance().DBSearch(query, user, respectFrontendRoles);
	}

	@Override
	public File getWorkingFileById(String fileId, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return getInstance().getWorkingFileById(fileId, user, respectFrontendRoles);
	}

	@Override
	public File get(String inode, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, DotDataException {
		return getInstance().get(inode, user, respectFrontendRoles);
	}

	@Override
	public Folder getFileFolder(File file, Host host, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return getInstance().getFileFolder(file, host, user, respectFrontendRoles);
	}

	@Override
	public List<File> findFiles(User user, boolean includeArchived, Map<String, Object> params, String hostId, String inode,
			String identifier, String parent, int offset, int limit, String orderBy)
			throws DotSecurityException, DotDataException {
		return getInstance().findFiles(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit,
				orderBy);
	}

	@Override
	public String getMimeType(String filename) {
		return getInstance().getMimeType(filename);
	}

	@Override
	public File copyFile(File file, Folder parent, User user, boolean respectFrontEndRoles)
			throws IOException, DotSecurityException, DotDataException {
		return getInstance().copyFile(file, parent, user, respectFrontEndRoles);
	}

	@Override
	public File copyFile(File file, Host host, User user, boolean respectFrontEndRoles)
			throws IOException, DotSecurityException, DotDataException {
		return getInstance().copyFile(file, host, user, respectFrontEndRoles);
	}

	@Override
	public boolean renameFile(File file, String newName, User user, boolean respectFrontEndRoles)
			throws DotStateException, DotDataException, DotSecurityException {
		return getInstance().renameFile(file, newName, user, respectFrontEndRoles);
	}

	@Override
	public boolean moveFile(File file, Folder parent, User user, boolean respectFrontEndRoles)
			throws DotStateException, DotDataException, DotSecurityException {
		return getInstance().moveFile(file, parent, user, respectFrontEndRoles);
	}

	@Override
	public boolean moveFile(File file, Host host, User user, boolean respectFrontEndRoles)
			throws DotStateException, DotDataException, DotSecurityException {
		return getInstance().moveFile(file, host, user, respectFrontEndRoles);
	}

	@Override
	public void publishFile(File file, User user, boolean respectFrontendRoles)
			throws WebAssetException, DotSecurityException, DotDataException {
		getInstance().publishFile(file, user, respectFrontendRoles);
	}

	@Override
	public File getFileByURI(String uri, Host host, boolean live, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getFileByURI(String uri, String hostId, boolean live, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return getInstance().getFileByURI(uri, hostId, live, user, respectFrontendRoles);
	}

	@Override
	public void invalidateCache(File file) throws DotDataException, DotSecurityException {
		getInstance().invalidateCache(file);
	}

	@Override
	public boolean fileNameExists(Folder folder, String fileName)
			throws DotStateException, DotDataException, DotSecurityException {
		return getInstance().fileNameExists(folder, fileName);
	}

	@Override
	public String getRealAssetPath() {
		return getInstance().getRealAssetPath();
	}

	@Override
	public String getRealAssetPath(Inode inode) {
		return getInstance().getRealAssetPath(inode);
	}

	@Override
	public String getRealAssetPath(String inode, String ext) {
		return getInstance().getRealAssetPath(inode, ext);
	}

	@Override
	public String getRealAssetPathTmpBinary() {
		return getInstance().getRealAssetPathTmpBinary();
	}

	@Override
	public boolean isLegacyFilesSupported() {
		return getInstance().isLegacyFilesSupported();
	}

	@Override
	public int deleteOldVersions(Date assetsOlderThan) throws DotDataException, DotHibernateException {
		return getInstance().deleteOldVersions(assetsOlderThan);
	}

	@Override
	public List<String> findUpdatedLegacyFileIds(Host h, Date startDate, Date endDate) {
		return getInstance().findUpdatedLegacyFileIds(h, startDate, endDate);
	}

	@Override
	public List<String> findUpdatedLegacyFileIds(Host host, String pattern, boolean include, Date startDate, Date endDate) {
		return getInstance().findUpdatedLegacyFileIds(host, pattern, include, startDate, endDate);
	}

	@Override
	public void updateUserReferences(String userId, String replacementUserId) throws DotDataException, DotSecurityException {
		getInstance().updateUserReferences(userId, replacementUserId);
	}

}
