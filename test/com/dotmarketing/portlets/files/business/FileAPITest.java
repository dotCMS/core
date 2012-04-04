package com.dotmarketing.portlets.files.business;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.factories.IdentifierFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.factories.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class FileAPITest extends ServletTestCase {
	
	private static UserAPI userAPI;
	private static HostAPI hostAPI;
	private static PermissionAPI permissionAPI;
	private static RoleAPI roleAPI;
	private static FolderAPI folderAPI;
	private static FileAPI fileAPI;
	private static IdentifierAPI identifierAPI;
	
	private static String fileName = "test.gif";
	
	private static String testFilePath = ".." + java.io.File.separator +
										 "test" + java.io.File.separator +
										 "com" + java.io.File.separator +
										 "dotmarketing" + java.io.File.separator +
										 "portlets" + java.io.File.separator +
										 "files" + java.io.File.separator +
										 "business" + java.io.File.separator +
										 "test_file" + java.io.File.separator +
										 fileName;
	
	private static String copyTestFilePath = ".." + java.io.File.separator +
											 "test" + java.io.File.separator +
											 "com" + java.io.File.separator +
											 "dotmarketing" + java.io.File.separator +
											 "portlets" + java.io.File.separator +
											 "files" + java.io.File.separator +
											 "business" + java.io.File.separator +
											 fileName;
	
	private static String author = "Test Author";
	private static boolean deleted = false;
	private static String friendlyName = "Test File Friendly Name";
	private static int height;
	private static Date iDate = new Date();
	private static boolean live = false;
	private static boolean locked = true;
	private static int maxHeight = 800;
	private static int maxSize = 1024;
	private static int maxWidth = 600;
	private static String mimeType = "image/gif";
	private static int minHeight = 1;
	private static Date modDate = new Date();
	private static String modUser;
	private static String owner;
	private static Date publishDate = null;
	private static boolean showOnMenu = true;
	private static int size;
	private static int sortOrder = 2;
	private static String title = "Test File";
	private static String type = "file_asset";
	private static int width;
	private static boolean working = true;
	
	private static File testFile;
	private static Host testHost;
	private static Folder testFolder1;
	private static Folder testFolder2;
	
	private static List<Permission> permissionList;
	
	protected void setUp() throws Exception {
		userAPI = APILocator.getUserAPI();
		hostAPI = APILocator.getHostAPI();
		permissionAPI = APILocator.getPermissionAPI();
		roleAPI = APILocator.getRoleAPI();
		folderAPI = APILocator.getFolderAPI();
		fileAPI = APILocator.getFileAPI();
		identifierAPI = APILocator.getIdentifierAPI();
		
		createJUnitTestFile();
	}
	
	protected void tearDown() throws Exception {
		deleteJUnitTestFile();
	}
	
	private static void copy(java.io.File source, java.io.File destination) throws Exception {
		InputStream in = new FileInputStream(source);
		OutputStream out = new FileOutputStream(destination);
			
			byte[] buf = new byte[1024];
			int len;
			while (0 < (len = in.read(buf))){
				out.write(buf, 0, len);
			}
	}
	
	private void createJUnitTestFile() throws Exception {
		String testFileFullPath = Config.CONTEXT.getRealPath(testFilePath);
		
		java.io.File tempTestFile = new java.io.File(testFileFullPath);
		if (!tempTestFile.exists()) {
			String message = "File does not exist: '" + testFileFullPath + "'";
			Logger.error(this, message);
			throw new Exception(message);
		}
		
		String copyTestFileFullPath = Config.CONTEXT.getRealPath(copyTestFilePath);
		java.io.File copyTempTestFile = new java.io.File(copyTestFileFullPath);
		if (!copyTempTestFile.exists()) {
			if (!copyTempTestFile.createNewFile()) {
				String message = "Cannot create copy of the test file: '" + copyTestFileFullPath + "'";
				Logger.error(this, message);
				throw new Exception(message);
			}
		}
		
		copy(tempTestFile, copyTempTestFile);
		
		BufferedImage img = javax.imageio.ImageIO.read(copyTempTestFile);
		
		testFile = new File();
		testFile.setAuthor(author);
		testFile.setDeleted(deleted);
		testFile.setFileName(fileName);
		testFile.setFriendlyName(friendlyName);
		
		height = img.getHeight();
		testFile.setHeight(height);
		
		testFile.setIDate(iDate);
		testFile.setLive(live);
		testFile.setLocked(locked);
		testFile.setMaxHeight(maxHeight);
		testFile.setMaxSize(maxSize);
		testFile.setMaxWidth(maxWidth);
		testFile.setMimeType(mimeType);
		testFile.setMinHeight(minHeight);
		testFile.setModDate(modDate);
		
		User user = userAPI.getSystemUser();
		modUser = user.getUserId();
		testFile.setModUser(modUser);
		
		owner = user.getUserId();
		testFile.setOwner(owner);
		
		testFile.setPublishDate(publishDate);
		testFile.setShowOnMenu(showOnMenu);
		
		size = (int) copyTempTestFile.length();
		testFile.setSize(size);
		
		testFile.setSortOrder(sortOrder);
		testFile.setTitle(title);
		testFile.setType(type);
		
		width = img.getWidth();
		testFile.setWidth(width);
		
		testFile.setWorking(working);
		
		testHost = new Host();
		testHost.setHostname("dotcms_junit_test_host");
		testHost.setModDate(new Date());
		testHost.setModUser(user.getUserId());
		testHost.setOwner(user.getUserId());
		testHost.setProperty("theme", "default");
		testHost = hostAPI.save(testHost, user, false);
		
		testFolder1 = (Folder) InodeFactory.getInode(null, Folder.class);
		testFolder1.setFilesMasks("");
		testFolder1.setIDate(new Date());
		testFolder1.setName("dotcms_junit_test_folder_1");
		testFolder1.setOwner(user.getUserId());
		//testFolder1.setPath("/dotcms_junit_test_folder_1/");
		testFolder1.setShowOnMenu(false);
		testFolder1.setSortOrder(0);
		testFolder1.setTitle("dotcms_junit_test_folder_1");
		testFolder1.setType("folder");
		testFolder1.setHostId(testHost.getIdentifier());
		
		permissionList = new ArrayList<Permission>();
		permissionList.add(new Permission("", roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ));
		
		folderAPI.save(testFolder1,user,false);
		Permission newPermission;
		for (Permission permission: permissionList) {
			newPermission = new Permission(testFolder1.getInode(), permission.getRoleId(), permission.getPermission());
			permissionAPI.save(newPermission, testFolder1, user, false);
		}
		
		testFolder2 = (Folder) InodeFactory.getInode(null, Folder.class);
		testFolder2.setFilesMasks("");
		testFolder2.setIDate(new Date());
		testFolder2.setName("dotcms_junit_test_folder_2");
		testFolder2.setOwner(user.getUserId());
		//testFolder2.setPath("/dotcms_junit_test_folder_2/");
		testFolder2.setShowOnMenu(false);
		testFolder2.setSortOrder(0);
		testFolder2.setTitle("dotcms_junit_test_folder_2");
		testFolder2.setType("folder");
		testFolder2.setHostId(testHost.getIdentifier());
		
		folderAPI.save(testFolder2,user,false);
		for (Permission permission: permissionList) {
			newPermission = new Permission(testFolder2.getInode(), permission.getRoleId(), permission.getPermission());
			permissionAPI.save(newPermission, testFolder2, user, false);
		}
		
		testFile = fileAPI.saveFile(testFile, copyTempTestFile, testFolder1, user, false);
		permissionAPI.copyPermissions(testFolder1, testFile);
		
		if (copyTempTestFile.exists())
			copyTempTestFile.delete();
	}
	
	private void deleteJUnitTestFile() throws Exception {
		User user = userAPI.getSystemUser();
		fileAPI.delete(testFile, user, false);
		
		testFolder1 = (Folder) InodeFactory.getInode(testFolder1.getInode(), Folder.class);
		InodeFactory.deleteInode(testFolder1);
		
		testFolder2 = (Folder) InodeFactory.getInode(testFolder2.getInode(), Folder.class);
		InodeFactory.deleteInode(testFolder2);
		
		hostAPI.delete(testHost, user, false);
	}
	
	private boolean checkPermission(WebAsset asset) throws Exception {
		List<Permission> permissions = permissionAPI.getPermissions(asset);
		
		if (permissions.size() != permissionList.size())
			return false;
		
		for (Permission permission1: permissions) {
			for (Permission permission2: permissionList) {
				if ((permission1.getPermission() != permission2.getPermission()) || !permission1.getRoleId().equals(permission2.getRoleId())) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public void testCopy() throws Exception {
		User user = userAPI.getSystemUser();
		File testFileCopy = fileAPI.copy(testFile, testFolder2, false, user, false);
		java.io.File file1 = fileAPI.getAssetIOFile(testFile);
		java.io.File file2 = fileAPI.getAssetIOFile(testFileCopy);
		
		try {
			assertFalse("Invalid \"identifier\" in copy file.", testFileCopy.getIdentifier().equals(testFile.getIdentifier()));
			assertFalse("Invalid \"inode\" in copy file.", testFileCopy.getInode().equals(testFile.getInode()));
			assertEquals("Invalid \"fileName\" in copy file.", fileName, testFileCopy.getFileName());
			assertFalse("Invalid \"friendlyName\" in copy file.", testFileCopy.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"height\" in copy file.", height, testFileCopy.getHeight());
			assertEquals("Invalid \"mimeType\" in copy file.", mimeType, testFileCopy.getMimeType());
			assertEquals("Invalid \"modUser\" in copy file.", modUser, testFileCopy.getModUser());
			assertEquals("Invalid \"showOnMenu\" in copy file.", showOnMenu, testFileCopy.isShowOnMenu());
			assertEquals("Invalid \"size\" in copy file.", size, testFileCopy.getSize());
			assertEquals("Invalid \"sortOrder\" in copy file.", sortOrder, testFileCopy.getSortOrder());
			assertEquals("Invalid \"title\" in copy file.", title, testFileCopy.getTitle());
			assertEquals("Invalid \"type\" in copy file.", type, testFileCopy.getType());
			assertEquals("Invalid \"width\" in copy file.", width, testFileCopy.getWidth());
			
			assertFalse("copy file did not copy the file in the file system: " + file2.getPath(), file1.getPath().equals(file2.getPath()));
			assertEquals("Invalid \"size\" in copy file in the file system.", size, file2.length());
			
			BufferedImage img = javax.imageio.ImageIO.read(file2);
			assertEquals("Invalid \"height\" in copy file in the file system.", height, img.getHeight());
			assertEquals("Invalid \"width\" in copy file in the file system.", width, img.getWidth());
			assertTrue("Invalid permissions assigned to copy file.", checkPermission(testFileCopy));
		} finally {
			fileAPI.delete(testFileCopy, user, false);
		}
	}
	
	public void testCopyOverwrite() throws Exception {
		User user = userAPI.getSystemUser();
		File testFileCopy = fileAPI.copy(testFile, testFolder2, false, user, false);
		java.io.File file1 = fileAPI.getAssetIOFile(testFile);
		java.io.File file2 = fileAPI.getAssetIOFile(testFileCopy);
		testFileCopy = fileAPI.copy(testFile, testFolder2, true, user, false);
		java.io.File file3 = fileAPI.getAssetIOFile(testFileCopy);
		
		try {
			assertFalse("Invalid \"identifier\" in copy file.", testFileCopy.getIdentifier().equals(testFile.getIdentifier()));
			assertFalse("Invalid \"inode\" in copy file.", testFileCopy.getInode().equals(testFile.getInode()));
			assertEquals("Invalid \"fileName\" in copy file.", fileName, testFileCopy.getFileName());
			assertFalse("Invalid \"friendlyName\" in copy file.", testFileCopy.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"height\" in copy file.", height, testFileCopy.getHeight());
			assertEquals("Invalid \"mimeType\" in copy file.", mimeType, testFileCopy.getMimeType());
			assertEquals("Invalid \"modUser\" in copy file.", modUser, testFileCopy.getModUser());
			assertEquals("Invalid \"showOnMenu\" in copy file.", showOnMenu, testFileCopy.isShowOnMenu());
			assertEquals("Invalid \"size\" in copy file.", size, testFileCopy.getSize());
			assertEquals("Invalid \"sortOrder\" in copy file.", sortOrder, testFileCopy.getSortOrder());
			assertEquals("Invalid \"title\" in copy file.", title, testFileCopy.getTitle());
			assertEquals("Invalid \"type\" in copy file.", type, testFileCopy.getType());
			assertEquals("Invalid \"width\" in copy file.", width, testFileCopy.getWidth());
			
			assertFalse("copy file did not copy the file in the file system: " + file2.getPath(), file1.getPath().equals(file2.getPath()));
			assertFalse("Overwritten file did not copy the file in the file system: " + file3.getPath(), file1.getPath().equals(file3.getPath()));
			assertTrue("Overwritten file has create a new file in the file system: " + file3.getPath(), file2.getPath().equals(file3.getPath()));
			assertEquals("Invalid \"size\" in copy file in the file system.", size, file2.length());
			
			BufferedImage img = javax.imageio.ImageIO.read(file2);
			assertEquals("Invalid \"height\" in copy file in the file system.", height, img.getHeight());
			assertEquals("Invalid \"width\" in copy file in the file system.", width, img.getWidth());
			
			Identifier identifier = identifierAPI.findFromInode(testFileCopy.getIdentifier());
			List<Versionable> allVersions = APILocator.getVersionableAPI().findAllVersions(identifier);
			assertTrue("Invalid number of version: " + allVersions.size(), (allVersions.size() == 2));
			
			List<File> files = IdentifierFactory.getChildrenClassByCondition(testFolder2, File.class, "working=" + DbConnectionFactory.getDBTrue());
			assertTrue("Invalid number of files created: " + files.size(), (files.size() == 1));
			assertTrue("Invalid permissions assigned to copy file.", checkPermission(testFileCopy));
		} finally {
			fileAPI.delete(testFileCopy, user, false);
		}
	}
	
	public void testCopyNoOverwrite() throws Exception {
		User user = userAPI.getSystemUser();
		File testFileCopy1 = fileAPI.copy(testFile, testFolder2, false, user, false);
		File testFileCopy2 = fileAPI.copy(testFile, testFolder2, false, user, false);
		java.io.File file1 = fileAPI.getAssetIOFile(testFile);
		java.io.File file2 = fileAPI.getAssetIOFile(testFileCopy1);
		java.io.File file3 = fileAPI.getAssetIOFile(testFileCopy2);
		
		try {
			assertFalse("Invalid \"identifier\" in first copy file.", testFileCopy1.getIdentifier().equals(testFile.getIdentifier()));
			assertFalse("Invalid \"inode\" in first copy file.", testFileCopy1.getInode().equals(testFile.getInode()));
			assertEquals("Invalid \"fileName\" in first copy file.", fileName, testFileCopy1.getFileName());
			assertFalse("Invalid \"friendlyName\" in first copy file.", testFileCopy1.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"height\" in first copy file.", height, testFileCopy1.getHeight());
			assertEquals("Invalid \"mimeType\" in first copy file.", mimeType, testFileCopy1.getMimeType());
			assertEquals("Invalid \"modUser\" in first copy file.", modUser, testFileCopy1.getModUser());
			assertEquals("Invalid \"showOnMenu\" in first copy file.", showOnMenu, testFileCopy1.isShowOnMenu());
			assertEquals("Invalid \"size\" in first copy file.", size, testFileCopy1.getSize());
			assertEquals("Invalid \"sortOrder\" in first copy file.", sortOrder, testFileCopy1.getSortOrder());
			assertEquals("Invalid \"title\" in first copy file.", title, testFileCopy1.getTitle());
			assertEquals("Invalid \"type\" in first copy file.", type, testFileCopy1.getType());
			assertEquals("Invalid \"width\" in first copy file.", width, testFileCopy1.getWidth());
			
			assertFalse("First copy file did not copy the file in the file system: " + file2.getPath(), file1.getPath().equals(file2.getPath()));
			assertEquals("Invalid \"size\" in first copy file in the file system.", size, file2.length());
			
			BufferedImage img = javax.imageio.ImageIO.read(file2);
			assertEquals("Invalid \"height\" in first copy file in the file system.", height, img.getHeight());
			assertEquals("Invalid \"width\" in first copy file in the file system.", width, img.getWidth());
			
			Identifier identifier = identifierAPI.findFromInode(testFileCopy1.getIdentifier());
			List<Versionable> allVersions =  APILocator.getVersionableAPI().findAllVersions(identifier);
			assertTrue("Invalid number of version for first copy file: " + allVersions.size(), (allVersions.size() == 1));
			
			assertFalse("Invalid \"identifier\" in second copy file.", testFileCopy2.getIdentifier().equals(testFile.getIdentifier()));
			assertFalse("Invalid \"inode\" in second copy file.", testFileCopy2.getInode().equals(testFile.getInode()));
			assertFalse("Invalid \"fileName\" in second copy file.", testFileCopy2.getFileName().equals(fileName));
			assertFalse("Invalid \"friendlyName\" in second copy file.", testFileCopy2.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"height\" in second copy file.", height, testFileCopy2.getHeight());
			assertEquals("Invalid \"mimeType\" in second copy file.", mimeType, testFileCopy2.getMimeType());
			assertEquals("Invalid \"modUser\" in second copy file.", modUser, testFileCopy2.getModUser());
			assertEquals("Invalid \"showOnMenu\" in second copy file.", showOnMenu, testFileCopy2.isShowOnMenu());
			assertEquals("Invalid \"size\" in second copy file.", size, testFileCopy2.getSize());
			assertEquals("Invalid \"sortOrder\" in second copy file.", sortOrder, testFileCopy2.getSortOrder());
			assertEquals("Invalid \"title\" in second copy file.", title, testFileCopy2.getTitle());
			assertEquals("Invalid \"type\" in second copy file.", type, testFileCopy2.getType());
			assertEquals("Invalid \"width\" in second copy file.", width, testFileCopy2.getWidth());
			
			assertFalse("Second copy file did not copy the file in the file system: " + file3.getPath(), file1.getPath().equals(file3.getPath()));
			assertFalse("Second copy file did not copy the file in the file system: " + file3.getPath(), file2.getPath().equals(file3.getPath()));
			assertEquals("Invalid \"size\" in second copy file in the file system.", size, file3.length());
			
			img = javax.imageio.ImageIO.read(file3);
			assertEquals("Invalid \"height\" in second copy file in the file system.", height, img.getHeight());
			assertEquals("Invalid \"width\" in second copy file in the file system.", width, img.getWidth());
			
			identifier = identifierAPI.findFromInode(testFileCopy2.getIdentifier());
			allVersions =  APILocator.getVersionableAPI().findAllVersions(identifier);
			assertTrue("Invalid number of version for second copy file: " + allVersions.size(), (allVersions.size() == 1));
			
			List<File> files = IdentifierFactory.getChildrenClassByCondition(testFolder2, File.class, "working=" + DbConnectionFactory.getDBTrue());
			assertTrue("Invalid number of files created: " + files.size(), (files.size() == 2));
			assertTrue("Invalid permissions assigned to first copy file.", checkPermission(testFileCopy1));
			assertTrue("Invalid permissions assigned to second copy file.", checkPermission(testFileCopy2));
		} finally {
			fileAPI.delete(testFileCopy1, user, false);
		}
	}
}