package com.dotmarketing.scripting.util.php;

/*
 * This file was originally taken from the Quercus File Path. 
 * The Servlet was modified for use within the Scripting Plugin
 * for dotCMS.  In accordance with the GPL this class remains with a 
 * GPL 2 License
 * 
 * Below is the original licene/notice from Resin.
 *   
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 * @author Jason Tesser
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.regex.Pattern;

import com.caucho.util.CharBuffer;
import com.caucho.vfs.FileRandomAccessStream;
import com.caucho.vfs.FileReadStream;
import com.caucho.vfs.FileWriteStream;
import com.caucho.vfs.FilesystemPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.RandomAccessStream;
import com.caucho.vfs.StreamImpl;
import com.caucho.vfs.VfsStream;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * DotCMS VFS for use within the dotCMS Scripting Plugin.
 * 
 * @author Jason Tesser
 */
public class DotCMSPHPCauchoVFS extends FilesystemPath {
	private static final FileAPI fileAPI = APILocator.getFileAPI();

	private static final UserAPI userAPI = APILocator.getUserAPI();

	// The underlying Java File object.
	private static byte []NEWLINE = getNewlineString().getBytes();

	private static FilesystemPath PWD;

	private File _file;
	private Folder _folder = null;
	protected boolean _isWindows;

	private Host host = null;

	private static String realPath = null;
	private static String assetPath = "/assets";

	/**
	 * @param path canonical path
	 */
	protected DotCMSPHPCauchoVFS(Host host, FilesystemPath root, String userPath, String path)
	{
		super(root, userPath, path);

		_separatorChar = getFileSeparatorChar();
		_isWindows = _separatorChar == '\\';
		
		this.host = host;
		try {
			realPath = Config.getStringProperty("ASSET_REAL_PATH");
		} catch (Exception e) { }
		try {
			assetPath = Config.getStringProperty("ASSET_PATH");
		} catch (Exception e) { }
	}

	public DotCMSPHPCauchoVFS(Host host){
		this(host, "/");
	}
	
	public DotCMSPHPCauchoVFS(Host host, String path)
	{
		this(host, null,  //PWD != null ? PWD._root : null,
				path, normalizePath("/", initialPath(path),
						0, getFileSeparatorChar()));

		if (_root == null) {
			_root = new DotCMSPHPCauchoVFS(host, null, "/", "/");
			super._root = _root;

			if (PWD == null)
				PWD = _root;
		}

		_separatorChar = _root._separatorChar;
		_isWindows = ((DotCMSPHPCauchoVFS) _root)._isWindows;
		this.host = host;
		try {
			realPath = Config.getStringProperty("ASSET_REAL_PATH");
		} catch (Exception e) { }
		try {
			assetPath = Config.getStringProperty("ASSET_PATH");
		} catch (Exception e) { }
	}

	protected static String initialPath(String path)
	{
		if (path == null)
			return getPwd();
		else if (path.length() > 0 && path.charAt(0) == '/')
			return path;
		else if (path.length() > 1 && path.charAt(1) == ':' && isWindows())
			//return convertFromWindowsPath(path);
			return path;
		else {
			String dir = getPwd();

			if (dir.length() > 0 && dir.charAt(dir.length() - 1) == '/') 
				return dir + path;
			else
				return dir + "/" + path;
		}
	}

	/**
	 * Gets the system's user dir (pwd) and convert it to the Resin format.
	 */
	public static String getPwd()
	{
		String path = getUserDir();

		path = path.replace(getFileSeparatorChar(), '/');

		if (isWindows())
			path = convertFromWindowsPath(path);

		return path;
	}

	/**
	 * a:xxx -> /a:xxx
	 * ///a:xxx -> /a:xxx
	 * //xxx -> /:/xxx
	 * 
	 */
	private static String convertFromWindowsPath(String path)
	{
		int colon = path.indexOf(':');
		int length = path.length();
		char ch;

		if (colon == 1 && (ch = path.charAt(0)) != '/' && ch != '\\')
			return "/" + path.charAt(0) + ":/" + path.substring(2);
		else if (length > 1
				&& ((ch = path.charAt(0)) == '/' || ch == '\\')
				&& ((ch = path.charAt(1)) == '/' || ch == '\\')) {
			if (colon < 0)
				return "/:" + path;

			for (int i = colon - 2; i > 1; i--) {
				if ((ch = path.charAt(i)) != '/' && ch != '\\')
					return "/:" + path;
			}

			ch = path.charAt(colon - 1);

			if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z')
				return path.substring(colon - 2);
			else
				return "/:" + path;
		}
		else
			return path;
	}

	//  @Override
	//  public long getDiskSpaceFree()
	//  {
	//    try {
	//      // JDK 1.6+ only
	//      return _file.getFreeSpace();
	//    } catch (Exception e) {
	//      return 0;
	//    }
	//  }

	//  @Override
	//  public long getDiskSpaceTotal()
	//  {
	//    try {
	//      // JDK 1.6+ only
	//      return _file.getTotalSpace();
	//    } catch (Exception e) {
	//      return 0;
	//    }
	//  }

	/**
	 * Lookup the path, handling windows weirdness
	 */
	protected Path schemeWalk(String userPath,
			Map<String,Object> attributes,
			String filePath,
			int offset)
	{
		if (! isWindows())
			return super.schemeWalk(userPath, attributes, filePath, offset);

		String canonicalPath;

		if (filePath.length() < offset + 2)
			return super.schemeWalk(userPath, attributes, filePath, offset);

		char ch1 = filePath.charAt(offset + 1);
		char ch2 = filePath.charAt(offset);

		if ((ch2 == '/' || ch2 == _separatorChar)
				&& (ch1 == '/' || ch1 == _separatorChar))
			return super.schemeWalk(userPath, attributes,
					convertFromWindowsPath(filePath.substring(offset)), 0);
		else
			return super.schemeWalk(userPath, attributes, filePath, offset);
	}

	/**
	 * Lookup the actual path relative to the filesystem root.
	 *
	 * @param userPath the user's path to lookup()
	 * @param attributes the user's attributes to lookup()
	 * @param path the normalized path
	 *
	 * @return the selected path
	 */
	public Path fsWalk(String userPath,
			Map<String,Object> attributes,
			String path)
	{
		return new DotCMSPHPCauchoVFS(host,_root, userPath, path);
	}

	/**
	 * Returns true if the path itself is cacheable
	 */
	@Override
	protected boolean isPathCacheable()
	{
		return true;
	}

	public String getScheme()
	{
		return "dotcms";
	}

	@Override
	public String getFullPath() {
		return _pathname;
	}
	
	/**
	 * Returns the full url for the given path.
	 */
	public String getURL()
	{
		if (! isWindows())
			return escapeURL("dotcms:" + getFullPath());

		String path = getFullPath();
		int length = path.length();
		CharBuffer cb = new CharBuffer();

		// #2725, server/1495
		cb.append("dotcms:");

		char ch;
		int offset = 0;
		// For windows, convert /c: to c:
		if (length >= 3
				&& path.charAt(0) == '/'
					&& path.charAt(2) == ':'
						&& ('a' <= (ch = path.charAt(1)) && ch <= 'z'
							|| 'A' <= ch && ch <= 'Z')) {
			// offset = 1;
		}
		else if (length >= 3
				&& path.charAt(0) == '/'
					&& path.charAt(1) == ':'
						&& path.charAt(2) == '/') {
			cb.append('/');
			cb.append('/');
			cb.append('/');
			cb.append('/');
			offset = 3;
		}

		for (; offset < length; offset++) {
			ch = path.charAt(offset);

			if (ch == '\\')
				cb.append('/');
			else
				cb.append(ch);
		}

		return escapeURL(cb.toString());

	}

	/**
	 * Returns the native path.
	 */
	public String getNativePath()
	{
		if (! isWindows())
			return getFullPath();

		String path = getFullPath();
		int length = path.length();
		CharBuffer cb = new CharBuffer();
		char ch;
		int offset = 0;
		// For windows, convert /c: to c:
		if (isWindows()) {
			if (length >= 3
					&& path.charAt(0) == '/'
						&& path.charAt(2) == ':'
							&& ('a' <= (ch = path.charAt(1)) && ch <= 'z'
								|| 'A' <= ch && ch <= 'Z')) {
				offset = 1;
			}
			else if (length >= 3
					&& path.charAt(0) == '/'
						&& path.charAt(1) == ':'
							&& path.charAt(2) == '/') {
				cb.append('\\');
				cb.append('\\');
				offset = 3;
			}
		}

		for (; offset < length; offset++) {
			ch = path.charAt(offset);
			if (ch == '/')
				cb.append(_separatorChar);
			else
				cb.append(ch);
		}

		return cb.close();
	}

	public boolean exists()
	{
		File f = getFile();
		if(UtilMethods.isSet(f)){
			return true;
		}else{
			if(getPath().endsWith(".php")){
				return false;
			}
		}
		try {
			_folder = APILocator.getFolderAPI().findFolderByPath(getPath(), host,userAPI.getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		} 
		if(_folder != null && InodeUtils.isSet(_folder.getInode())){
			return true;
		}
		return false;
	}

	public int getMode()
	{
		int perms = 0;

		if (isDirectory()) {
			perms += 01000;
			perms += 0111;
		}

		if (canRead())
			perms += 0444;

		if (canWrite())
			perms += 0220;

		return perms;
	}

	public boolean isDirectory()
	{
		File f = getFile();
		if(UtilMethods.isSet(f)){
			return false;
		}else{
			try {
				_folder = APILocator.getFolderAPI().findFolderByPath(getPath(), host,userAPI.getSystemUser(),false);
			} catch (Exception e) {
				Logger.error(this,e.getMessage(),e);
			}
			if(_folder != null && InodeUtils.isSet(_folder.getInode())){
				return true;
			}
		}
		return true;
	}

	public boolean isFile()
	{
		File f = getFile();
		if(UtilMethods.isSet(f)){
			return f.isFile();
		}else{
			return false;
		}
	}

	public long getLength()
	{
		File f = getFile();
		if(UtilMethods.isSet(f)){
			return f.length();
		}else{
			return 0;
		}
	}

	public long getLastModified()
	{
		File f = getFile();
		if(UtilMethods.isSet(f)){
			return f.lastModified();
		}else{
			return 0;
		}
	}

	// This exists in JDK 1.2
	public void setLastModified(long time)
	{
		File f = getFile();
		if(UtilMethods.isSet(f)){
			f.setLastModified(time);
		}
	}

	public boolean canRead()
	{
		return true;
	}

	public boolean canWrite()
	{
		return false;
	}

//	/**
//	 * Returns a list of files in the directory.
//	 */
//	public String []list() throws IOException
//	{
//		String []list = getFile().list();
//
//		if (list != null)
//			return list;
//
//		return new String[0];
//	}

//	public boolean mkdir()
//	throws IOException
//	{
//		boolean value = getFile().mkdir();
//		if (! value && ! getFile().isDirectory())
//			throw new IOException("cannot create directory");
//
//		return value;
//	}

//	public boolean mkdirs()
//	throws IOException
//	{
//		File file = getFile();
//
//		boolean value;
//
//		synchronized (file) {
//			value = file.mkdirs();
//		}
//
//		if (! value && ! file.isDirectory())
//			throw new IOException("Cannot create directory: " + getFile());
//
//		return value;
//	}

//	public boolean remove()
//	{
//		if (getFile().delete())
//			return true;
//
//		if (getPath().endsWith(".jar")) {
//			// Jar.create(this).clearCache();
//			return getFile().delete();
//		}
//
//		return false;
//	}

	@Override
	public boolean truncate(long length)
	throws IOException
	{
		File file = getFile();

		FileOutputStream fos = new FileOutputStream(file);

		try {
			fos.getChannel().truncate(length);

			return true;
		} finally {
			fos.close();
		}
	}

//	public boolean renameTo(Path path)
//	{
//		if (! (path instanceof DotCMSPHPCauchoVFS))
//			return false;
//
//		DotCMSPHPCauchoVFS file = (DotCMSPHPCauchoVFS) path;
//
//		return this.getFile().renameTo(file.getFile());
//	}

	/**
	 * Returns the stream implementation for a read stream.
	 */
	public StreamImpl openReadImpl() throws IOException
	{
		return new FileReadStream(new FileInputStream(getFile()), this);
	}

	public StreamImpl openWriteImpl() throws IOException
	{
		File f = new File(_userPath);
		if(!f.exists()){
			f.createNewFile();
		}
		FileWriteStream fws = new FileWriteStream(
				new FileOutputStream(f),
				this);

		fws.setNewline(NEWLINE);

		return fws;
	}

	public StreamImpl openAppendImpl() throws IOException
	{
		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(getFile().toString(), true);
		} catch (IOException e) {
			// MacOS hack
			fos = new FileOutputStream(getFile().toString());
		}

		FileWriteStream fws = new FileWriteStream(fos);

		fws.setNewline(NEWLINE);

		return fws;
	}

	public StreamImpl openReadWriteImpl() throws IOException
	{
		VfsStream os;

		os = new VfsStream(new FileInputStream(getFile()),
				new FileOutputStream(getFile()),
				this);

		os.setNewline(NEWLINE);

		return os;
	}

	/**
	 * Returns the stream implementation for a random-access stream.
	 */
	public RandomAccessStream openRandomAccess() throws IOException
	{
		return new FileRandomAccessStream(new RandomAccessFile(getFile(), "rw"));
	}

	@Override
	protected Path copy()
	{
		return new DotCMSPHPCauchoVFS(host, getRoot(), getUserPath(), getPath());
	}

	public int hashCode()
	{
		return getPath().hashCode();
	}

	public boolean equals(Object b)
	{
		if (this == b)
			return true;

		if (! (b instanceof DotCMSPHPCauchoVFS))
			return false;

		DotCMSPHPCauchoVFS file = (DotCMSPHPCauchoVFS) b;

		return getPath().equals(file.getPath());
	}

	public File getFile() {
//		if (_file != null){
//			return _file;
//		}
		String uri;
		Identifier ident = new Identifier();
		try {
			ident = APILocator.getIdentifierAPI().find(host, getPath());
		} catch (Exception e1) {
			Logger.error(this, e1.getMessage(), e1);
		} 
		try{
			 uri = LiveCache.getPathFromCache(ident.getURI(), host);
		}catch (Exception e) {
			return null;
		}

		if(!UtilMethods.isSet(uri)){
			return null;
		}
		String inode = UtilMethods.getFileName(new File(FileUtil.getRealPath(assetPath + uri)).getName());
		User mu = null; 

		try{
			IFileAsset file;
			try{
				file = fileAPI.find(inode, userAPI.getSystemUser(), false);
			}catch(DotHibernateException e){
				inode = uri.split(Pattern.quote(File.separator))[3];
				file = APILocator.getFileAssetAPI().fromContentlet(APILocator.getContentletAPI().find(inode, userAPI.getSystemUser(), false));
			}
			mu = userAPI.loadUserById(file.getModUser(), userAPI.getSystemUser(), true);
		}catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			Logger.error(this, "User doesn't have permissions to get PHP filesystem.");
			throw new DotRuntimeException(e.getMessage(),e);
		}
		try {
			if(!Config.getBooleanProperty("ENABLE_SCRIPTING", false)){
				Logger.error(this, "User doesn't have permissions to get PHP filesystem.");
				throw new DotRuntimeException("Last Mod User does not have Scripting Developer role");
			}
			if(!APILocator.getRoleAPI().doesUserHaveRole(mu, APILocator.getRoleAPI().loadRoleByKey("Scripting Developer"))){
				Logger.error(this, "User doesn't have permissions to get PHP filesystem.");
				throw new DotRuntimeException("Last Mod User does not have Scripting Developer role");
			}
		} catch (DotDataException e) {
			Logger.error(this, "User doesn't have permissions to get PHP filesystem.");
			throw new DotRuntimeException("Last Mod User does not have Scripting Developer role");
		}

		if(!UtilMethods.isSet(realPath)){
			_file = new File(FileUtil.getRealPath(assetPath + uri));
		}else{
			_file = new File(realPath + uri);
		}
		return _file;
	}
}
