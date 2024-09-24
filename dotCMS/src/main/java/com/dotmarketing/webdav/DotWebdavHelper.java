package com.dotmarketing.webdav;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.rendering.velocity.services.DotResourceCache;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.cache.FolderCache;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetSearcher;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.*;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import org.apache.commons.lang.StringUtils;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.velocity.runtime.resource.ResourceManager;

import javax.servlet.http.HttpServletRequest;

/**
 * This Helper Class provides all the utility methods needed for the interaction between dotCMS and WebDAV.
 * Web-based Distributed Authoring and Versioning, or WebDAV, is an extension of the HTTP protocol that allows you to
 * create a connection between your local computer and a server to easily transfer files between machines.
 * <p>
 * This helper has direct communication with the Workflow API used for saving, moving, and deleting pieces of content
 * in the system. </p>
 *
 * @author root
 * @since Mar 22, 2012
 */
public class DotWebdavHelper {

	private static final String PRE_AUTHENTICATOR = PropsUtil.get("auth.pipeline.pre");
	private static ThreadLocal<Perl5Matcher> localP5Matcher = new ThreadLocal<Perl5Matcher>(){
		protected Perl5Matcher initialValue() {
			return new Perl5Matcher();
		}
	};
	private  org.apache.oro.text.regex.Pattern tempResourcePattern;
	private String tempFolderPath = "dotwebdav";

	private HostAPI hostAPI = APILocator.getHostAPI();
	private FolderAPI folderAPI = APILocator.getFolderAPI();
	private IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
	private FolderCache fc = CacheLocator.getFolderCache();
	private PermissionAPI perAPI = APILocator.getPermissionAPI();
	private LanguageAPI languageAPI = APILocator.getLanguageAPI();
	private static FileResourceCache fileResourceCache = new FileResourceCache();
	private long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
	private boolean legacyPath = Config.getBooleanProperty("WEBDAV_LEGACY_PATHING", false);
	private static final String emptyFileData = "~DOTEMPTY";
	private ContentletAPI conAPI = APILocator.getContentletAPI();

	/**
	 * MD5 message digest provider.
	 */
	private static MessageDigest md5Helper;


	private Hashtable<String, com.bradmcevoy.http.LockInfo> resourceLocks = new Hashtable<>();

	static {
		new Timer().schedule(new FileResourceCacheCleaner(), 1000  * 60 * Config.getIntProperty("WEBDAV_CLEAR_RESOURCE_CACHE_FRECUENCY", 10), 1000  * 60 * Config.getIntProperty("WEBDAV_CLEAR_RESOURCE_CACHE_FRECUENCY", 10));
	}

	public DotWebdavHelper() {
		Perl5Compiler c = new Perl5Compiler();
		try{
			tempResourcePattern = c.compile("/\\(.*\\)|/._\\(.*\\)|/\\.|^\\.|^\\(.*\\)",Perl5Compiler.READ_ONLY_MASK);
		}catch (MalformedPatternException mfe) {
			Logger.fatal(this,"Unable to instaniate webdav servlet : " + mfe.getMessage(),mfe);
			Logger.error(this,mfe.getMessage(),mfe);
		}


		// Load the MD5 helper used to calculate signatures.
		try {
			md5Helper = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException("No MD5", e);
		}

	}

	//Check if the contentlets to upload are going to be live or working
	public boolean isAutoPub(String path){
		if(legacyPath){
			if(path.startsWith("/webdav/autopub")){
				return true;
			}
			return false;
		}else{
			if(path.startsWith("/webdav/live")){
				return true;
			}
			return false;
		}
	}

	public User authorizePrincipal(final String username, final String passwd) throws DotSecurityException, NoSuchUserException, DotDataException {


		final Company company         = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
		final boolean useEmailAsLogin = !company.getAuthType().equals(Company.AUTH_TYPE_ID);

		try {

			if (PRE_AUTHENTICATOR != null && !PRE_AUTHENTICATOR.equals("")) {

				final Authenticator authenticator = (Authenticator) new com.dotcms.repackage.bsh.
						Interpreter().eval("new " + PRE_AUTHENTICATOR + "()");
				if (useEmailAsLogin) {

					authenticator.authenticateByEmailAddress(company.getCompanyId(), username, passwd);
				} else {

					authenticator.authenticateByUserId      (company.getCompanyId(), username, passwd);
				}
			}
		} catch (AuthException ae) {

			Logger.debug(this, "Username : " + username + " failed to login", ae);
			throw new DotSecurityException(ae.getMessage(),ae);
		} catch (Exception e) {

			Logger.error(this, e.getMessage(), e);
			throw new DotSecurityException(e.getMessage(),e);
		}

		final UserAPI userAPI = APILocator.getUserAPI();
		final User user       = company.getAuthType().equals(Company.AUTH_TYPE_ID)?
				userAPI.loadUserById     (username,userAPI.getSystemUser(),false):
				userAPI.loadByUserByEmail(username, userAPI.getSystemUser(), false);

		if (user == null) {

			throw new DotSecurityException("The user was returned NULL");
		}

		// Validate password and rehash when is needed
		if (!LoginFactory.passwordMatch(passwd, user) && !this.tryMatchJsonWebToken(passwd, user)) {

			Logger.debug(this, ()-> "The user's passwords didn't match");
			throw new DotSecurityException("The user's passwords didn't match");
		}

		return user;
	}

	private boolean tryMatchJsonWebToken (final String jsonWebToken, final User user) {

		final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final String ipAddress           = null != request? request.getRemoteAddr(): null;
		final Optional<JWToken> tokenOpt = APILocator.getApiTokenAPI().fromJwt(jsonWebToken.trim(), ipAddress);

		return tokenOpt.isPresent() && tokenOpt.get().getUserId().equals(user.getUserId());
	}

	public boolean isFolder(String uriAux, User user) throws IOException {
		Logger.debug(this, "isFolder");
		boolean returnValue = false;
		Logger.debug(this, "Method isFolder: the uri is " + uriAux);
		if (uriAux.equals("") || uriAux.equals("/")) {
			returnValue = true;
		} else {
			uriAux = stripMapping(uriAux);
			String hostName = getHostname(uriAux);
			Logger.debug(this, "Method isFolder: the hostname is " + hostName);
			Host host;
			try {
				host = hostAPI.findByName(hostName, user, false);
			} catch (DotDataException e) {
				Logger.error(DotWebdavHelper.class, e.getMessage(), e);
				throw new IOException(e.getMessage(),e);
			} catch (DotSecurityException e) {
				Logger.error(DotWebdavHelper.class, e.getMessage(), e);
				throw new IOException(e.getMessage(),e);
			}
			if (host != null && InodeUtils.isSet(host.getInode())) {
				String path = getPath(uriAux);
				Logger.debug(this, "Method isFolder: the path is " + path);
				if (path.equals("") || path.equals("/")) {
					returnValue = true;
				} else {
					if(!path.endsWith("/"))
						path += "/";
					if(path.contains("mvdest2")){
						Logger.debug(this, "is mvdest2 a folder");
					}
					Folder folder = new Folder();
					try {
						folder = folderAPI.findFolderByPath(path, host,user,false);
					} catch (Exception e) {
						Logger.debug(this, "unable to find folder " + path );
						//throw new IOException(e.getMessage(),e);
					}
					if (InodeUtils.isSet(folder.getInode())) {
						returnValue = true;
					}
				}
			}
		}
		return returnValue;
	}

	public boolean isResource(String uri, User user) throws IOException {
		uri = stripMapping(uri);
		Logger.debug(this.getClass(), "In the Method isResource");
		if (uri.endsWith("/")) {
			return false;
		}
		boolean returnValue = false;
		// Host
		String hostName = getHostname(uri);

		Host host;
		try {
			host = hostAPI.findByName(hostName, user, false);
		} catch (DotDataException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		}

		if(host == null){
			Logger.debug(this, "isResource Method: Host is NULL");
		}else{
			Logger.debug(this, "isResource Method: host id is " + host.getIdentifier() + " and the host name is " + host.getHostname());
		}
		// Folder
		String path = getPath(uri);
		String folderName = getFolderName(path);
		Folder folder;
		try {
			folder = folderAPI.findFolderByPath(folderName, host, user,false);
		} catch (Exception e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		}
		if(folder!=null && InodeUtils.isSet(folder.getInode())) {
			// FileName
			String fileName = getFileName(path);
			fileName = deleteSpecialCharacter(fileName);

			if (InodeUtils.isSet(host.getInode())) {
				try {
					returnValue = APILocator.getFileAssetAPI().fileNameExists(host, folder, fileName, "");

				} catch (Exception ex) {
					Logger.debug(this, "Error verifying if file already exists",ex);
				}
			}
		}
		return returnValue;
	}

	public IFileAsset loadFile(String url, User user) throws IOException, DotDataException, DotSecurityException{
		url = stripMapping(url);
		String hostName = getHostname(url);
		url = getPath(url);
		Contentlet cont = null;
		Identifier id = null;

		Host host;
		try {
			host = hostAPI.findByName(hostName, user, false);
		} catch (DotDataException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		}

		IFileAsset f =null;
		try {
			id = APILocator.getIdentifierAPI().find(host, url);
			if(id!=null && InodeUtils.isSet(id.getId())) {
				if(id.getAssetType().equals("contentlet")){
					cont = conAPI.findContentletByIdentifier(id.getId(), false, defaultLang, user, false);
					if(cont!=null && InodeUtils.isSet(cont.getIdentifier()) && !APILocator.getVersionableAPI().isDeleted(cont)){
						f = APILocator.getFileAssetAPI().fromContentlet(cont);
					}
				}
			}
		}catch (Exception ex) {
			f = null;
		}

		return f;
	}

	public String getAssetName(final IFileAsset fileAsset){
		try{
			final Identifier identifier = APILocator.getIdentifierAPI().find(fileAsset.getIdentifier());
			return identifier.getAssetName();
		}catch (Exception e){
			Logger.error( DotWebdavHelper.class," Failed to obtain file-asset name ", e);
		}
		return fileAsset.getFileName();
	}

	public Folder loadFolder(String url,User user) throws IOException{
		url = stripMapping(url);
		String hostName = getHostname(url);
		url = getPath(url);
		Host host;
		Folder folder;
		try {
			host = hostAPI.findByName(hostName, user, false);
			folder = folderAPI.findFolderByPath(url, host,user, false);
		} catch (DotDataException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		}

		return folder;
	}

	public File loadTempFile(String url){
		try {
			url = stripMapping(url);
		} catch (IOException e) {
			Logger.error( this, "Error happened with uri: [" + url + "]", e);
		}
		Logger.debug(this, "Getting temp file from path " + url);
		return new File(getTempDir().getPath() + url);
	}

	/**
	 * Returns a collection of child Resources for a given folder
	 *
	 * @param parentFolder Parent folder
	 * @param user         Authenticated user
	 * @param isAutoPub
	 * @return
	 * @throws IOException
	 */
	public List<Resource> getChildrenOfFolder ( Folder parentFolder, User user, boolean isAutoPub, long lang ) throws IOException {

		String prePath = "/webdav/";
		if(legacyPath){
			if ( isAutoPub ) {
				prePath += "autopub/";
			} else {
				prePath += "nonpub/";
			}
		}else{
			if ( isAutoPub ) {
				prePath += "live/";
			} else {
				prePath += "working/";
			}
			defaultLang = lang;
			prePath += defaultLang;
			prePath += "/";
		}

		Host folderHost;
		try {
			folderHost = hostAPI.find( parentFolder.getHostId(), user, false );
		} catch ( DotDataException e ) {
			Logger.error( DotWebdavHelper.class, e.getMessage(), e );
			throw new IOException( e.getMessage() );
		} catch ( DotSecurityException e ) {
			Logger.error( DotWebdavHelper.class, e.getMessage(), e );
			throw new IOException( e.getMessage() );
		}

		List<Resource> result = new ArrayList<>();
		try {

			//Search for child folders
			List<Folder> folderListSubChildren = folderAPI.findSubFolders( parentFolder, user, false );
			//Search for child files
			List<Versionable> filesListSubChildren = new ArrayList<>();
			try {
				filesListSubChildren.addAll( APILocator.getFileAssetAPI().findFileAssetsByDB(FileAssetSearcher.builder().folder(parentFolder).user(user).respectFrontendRoles(false).build()) );
			} catch ( Exception e2 ) {
				Logger.error( this, "Could not load files : ", e2 );
			}

			for ( Versionable file : filesListSubChildren ) {
				if ( !file.isArchived() ) {
					IFileAsset fileAsset = (IFileAsset) file;
					if(fileAsset.getLanguageId()==defaultLang){
						FileResourceImpl resource = new FileResourceImpl( fileAsset, prePath + folderHost.getHostname() + "/" + fileAsset.getPath() );
						result.add( resource );
					}
				}
			}
			for ( Folder folder : folderListSubChildren ) {
				String path = identifierAPI.find(folder.getIdentifier()).getPath();

				FolderResourceImpl resource = new FolderResourceImpl( folder, prePath + folderHost.getHostname() + "/" + (path.startsWith( "/" ) ? path.substring( 1 ) : path) );
				result.add( resource );
			}

			String p = APILocator.getIdentifierAPI().find(parentFolder.getIdentifier()).getPath();
			if ( p.contains( "/" ) )
				p.replace( "/", File.separator );
			File tempDir = new File( getTempDir().getPath() + File.separator + folderHost.getHostname() + p );
			p = identifierAPI.find(parentFolder.getIdentifier()).getPath();
			if ( !p.endsWith( "/" ) )
				p = p + "/";
			if ( !p.startsWith( "/" ) )
				p = "/" + p;
			if ( tempDir.exists() && tempDir.isDirectory() ) {
				File[] files = tempDir.listFiles();
				for ( File file : files ) {
					String tp = prePath + folderHost.getHostname() + p + file.getName();
					if ( !isTempResource( tp ) ) {
						continue;
					}
					if ( file.isDirectory() ) {
						TempFolderResourceImpl tr = new TempFolderResourceImpl( tp, file, isAutoPub );
						result.add( tr );
					} else {
						TempFileResourceImpl tr = new TempFileResourceImpl( file, tp, isAutoPub );
						result.add( tr );
					}
				}
			}
		} catch ( Exception e ) {
			Logger.error( DotWebdavHelper.class, e.getMessage(), e );
			throw new IOException( e.getMessage() );
		}

		return result;
	}

	public File getTempDir () {
		return new File(ConfigUtils.getAssetTempPath());
	}

	public String getHostName ( String uri ) {
		try {
			return getHostname(stripMapping(uri));
		} catch (IOException e) {
			Logger.error( this, "Error happened with uri: [" + uri + "]", e);
			return null;
		}
	}

	public boolean isTempResource(String path){
		Perl5Matcher matcher = (Perl5Matcher) localP5Matcher.get();
		if(matcher.contains(path, tempResourcePattern))
			return true;
		return false;
	}

	public File createTempFolder(String path){
		try {
			path = stripMapping(path);
		} catch (IOException e) {
			Logger.error( this, "Error happened with uri: [" + path + "]", e);
		}
		if(path.startsWith(getTempDir().getPath()))
			path = path.substring(getTempDir().getPath().length(), path.length());
		if(path.startsWith("/") || path.startsWith("\\")){
			path = path.substring(1, path.length());
		}
		path = path.replace("/", File.separator);
		File f = new File(getTempDir().getPath() + File.separator + path);
		f.mkdirs();
		return f;
	}

	public void copyFolderToTemp(Folder folder, File tempFolder, User user, String name,boolean isAutoPub, long lang) throws IOException{
		String p = "";
		try {
			p = identifierAPI.find(folder.getIdentifier()).getPath();
		} catch (Exception e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		if(p.endsWith("/"))
			p = p + "/";
		String path = p.replace("/", File.separator);
		path = tempFolder.getPath() + File.separator + name;
		File tf = createTempFolder(path);
		List<Resource> children = getChildrenOfFolder(folder, user, isAutoPub, lang);
		for (Resource resource : children) {
			if(resource instanceof CollectionResource){
				FolderResourceImpl fr = (FolderResourceImpl)resource;
				copyFolderToTemp(fr.getFolder(), tf, user, fr.getFolder().getName(),isAutoPub, lang);
			}else{
				FileResourceImpl fr = (FileResourceImpl)resource;
				copyFileToTemp(fr.getFile(), tf);
			}
		}
	}

	public File copyFileToTemp(IFileAsset file, File tempFolder) throws IOException{
		File f = null;

		f = ((Contentlet)file).getBinary(FileAssetAPI.BINARY_FIELD);

		File nf = new File(tempFolder.getPath() + File.separator + f.getName());
		FileUtil.copyFile(f, nf);
		return nf;
	}

	public File createTempFile(String path) throws IOException{
		File file = new File(getTempDir().getPath() + path);
		String p = file.getPath().substring(0,file.getPath().lastIndexOf(File.separator));
		File f = new File(p);
		f.mkdirs();
		file.createNewFile();
		return file;
	}

	public void copyTempDirToStorage(File fromFileFolder, String destPath, User user,boolean autoPublish) throws Exception{
		if(fromFileFolder == null || !fromFileFolder.isDirectory()){
			throw new IOException("The temp source file must be a directory");
		}
		destPath = stripMapping(destPath);
		if(destPath.endsWith("/"))
			destPath = destPath + "/";
		createFolder(destPath, user);
		File[] files = fromFileFolder.listFiles();
		for (File file : files) {
			if(file.isDirectory()){
				copyTempDirToStorage(file, destPath + file.getName(), user, autoPublish);
			}else{
				copyTempFileToStorage(file, destPath + file.getName(), user,autoPublish);
			}
		}
	}

	public void copyTempFileToStorage(File fromFile, String destPath,User user,boolean autoPublish) throws Exception{
		destPath = stripMapping(destPath);
		if(fromFile == null){
			throw new IOException("The temp source file must exist");
		}
		InputStream in = Files.newInputStream(fromFile.toPath());
		setResourceContent(destPath, in, null, null, new Date(fromFile.lastModified()),user, autoPublish);
	}

	public void copyResource(String fromPath, String toPath, User user, boolean autoPublish) throws Exception {
		setResourceContent(toPath, getResourceContent(fromPath,user), null, null, user);
	}

	public void copyFolder(String sourcePath, String destinationPath, User user, boolean autoPublish) throws IOException, DotDataException {
		try{
			destinationPath=stripMapping(destinationPath);
			sourcePath=stripMapping(sourcePath);
			PermissionAPI perAPI = APILocator.getPermissionAPI();
			createFolder(destinationPath, user);

			Summary[] children = getChildrenData(sourcePath, user);

			for (int i = children.length - 1; i >= 0; i--) {
				// children[i] = "/" + children[i];

				if (!children[i].isFolder()) {

					setResourceContent(destinationPath + "/" + children[i].getName(), getResourceContent(sourcePath + "/" + children[i].getName(),user), null, null, user);

					// ### Copy the permission ###
					// Source
					boolean live = false;

					Identifier identifier  = APILocator.getIdentifierAPI().find(children[i].getHost(), destinationPath + "/" + children[i].getName());
					Permissionable destinationFile = null;
					if(identifier!=null && identifier.getAssetType().equals("contentlet")){
						destinationFile = conAPI.findContentletByIdentifier(identifier.getId(), live, defaultLang, user, false);
					}

					// Delete the new permissions
					perAPI.removePermissions(destinationFile);

					// Copy the new permissions
					perAPI.copyPermissions((Permissionable)children[i].getFile(), destinationFile);

					// ### END Copy the permission ###
					// }
				} else {
					copyFolder(sourcePath + "/" + children[i].getName(), destinationPath + "/" + children[i].getName(), user, autoPublish);
				}

			}

			// ### Copy the permission ###
			// Source
			String sourceHostName = getHostname(sourcePath);
			String sourceFolderName = getPath(sourcePath);
			// String sourceFolderName = DotCMSStorage.getFolderName(sourcePath);
			Host sourceHost;

			sourceHost = hostAPI.findByName(sourceHostName, user, false);



			Folder sourceFolder = folderAPI.findFolderByPath(sourceFolderName + "/", sourceHost,user,false);
			// Destination
			String destinationHostName = getHostname(destinationPath);
			String destinationFolderName = getPath(destinationPath);
			// String destinationFolderName =
			// DotCMSStorage.getFolderName(destinationPath);
			Host destinationHost;

			destinationHost = hostAPI.findByName(destinationHostName, user, false);


			Folder destinationFolder = folderAPI.findFolderByPath(destinationFolderName + "/", destinationHost,user,false);

			// Delete the new permissions
			perAPI.removePermissions(destinationFolder);

			// Copy the new permissions
			perAPI.copyPermissions(sourceFolder, destinationFolder);
		}catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
		return;
	}

	private File writeDataIfEmptyFile(Folder folder, String fileName, File fileData) throws IOException{
		if(fileData.length() == 0 && !Config.getBooleanProperty("CONTENT_ALLOW_ZERO_LENGTH_FILES", true)){
			Logger.warn(this, "The file " + folder.getPath() + fileName + " that is trying to be uploaded is empty. A byte will be written to the file because empty files are not allowed in the system");
			FileUtil.write(fileData, emptyFileData);
		}
		return fileData;
	}

	public void setResourceContent(String resourceUri,
								   InputStream content,	String contentType, String characterEncoding, Date modifiedDate, User user, boolean isAutoPub) throws Exception {
		resourceUri = stripMapping(resourceUri);
		Logger.debug(this.getClass(), "setResourceContent");
		String hostName = getHostname(resourceUri);
		String path = getPath(resourceUri);
		String folderName = getFolderName(path);
		String fileName = getFileName(path);
		fileName = deleteSpecialCharacter(fileName);
		final boolean disableWorkflow = Config.getBooleanProperty("dotcms.webdav.disableworkflow", false);

		Host host;
		try {
			host = hostAPI.findByName(hostName, user, false);
		} catch (DotDataException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		}

		Folder folder = new Folder();
		try {
			folder = folderAPI.findFolderByPath(folderName, host,user,false);
		} catch (Exception e2) {
			Logger.error(this, e2.getMessage(), e2);
		}
		if (host != null && InodeUtils.isSet(host.getInode()) && InodeUtils.isSet(folder.getInode())) {
			IFileAsset destinationFile = null;
			File workingFile = null;
			Folder parent = null;
			Contentlet fileAssetCont = null;
			Identifier identifier  = APILocator.getIdentifierAPI().find(host, path);
			if(identifier!=null && InodeUtils.isSet(identifier.getId()) && identifier.getAssetType().equals("contentlet")){
				List<Contentlet> list = conAPI.findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false);
				long langContentlet = list.get(0).getLanguageId();
				if(langContentlet != defaultLang){
					for(Contentlet c : list){
						if(c.getLanguageId() == defaultLang){
							langContentlet = defaultLang;
							break;
						}
					}
				}
				fileAssetCont = conAPI.findContentletByIdentifier(identifier.getId(), false, langContentlet, user, false);
				workingFile = fileAssetCont.getBinary(FileAssetAPI.BINARY_FIELD);
				destinationFile = APILocator.getFileAssetAPI().fromContentlet(fileAssetCont);
				parent = APILocator.getFolderAPI().findFolderByPath(identifier.getParentPath(), host, user, false);

				if(fileAssetCont.isArchived()) {
					conAPI.unarchive(fileAssetCont, user, false);
				}
			}

			//http://jira.dotmarketing.net/browse/DOTCMS-1873
			//To clear velocity cache
			if(workingFile!=null){
				DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
				vc.remove(ResourceManager.RESOURCE_TEMPLATE + workingFile.getPath());
			}

			if(destinationFile==null){
				Contentlet fileAsset = new Contentlet();
				Structure faStructure = CacheLocator.getContentTypeCache().getStructureByInode(folder.getDefaultFileType());
				Field fieldVar = faStructure.getFieldVar(FileAssetAPI.BINARY_FIELD);
				fileAsset.setStructureInode(folder.getDefaultFileType());
				fileAsset.setFolder(folder.getInode());

				// Create user temp folder and create file inside of it
				File fileData = createFileInTemporalFolder(fieldVar, user.getUserId(), fileName);

				// Saving the new working data
				try (final ReadableByteChannel inputChannel = Channels.newChannel(content);
					 final WritableByteChannel outputChannel = Channels.newChannel(Files.newOutputStream(fileData.toPath()))){

					FileUtil.fastCopyUsingNio(inputChannel, outputChannel);
					Logger.debug(this, "WEBDAV fileName:" + fileName + " : File size:" + fileData.length() + " : " + fileData.getAbsolutePath());
				}

				//Avoid uploading an empty file
				if(HttpManager.request().getUserAgentHeader().contains("Cyberduck")){
					fileData = writeDataIfEmptyFile(folder, fileName, fileData);
				}

				fileAsset.setStringProperty(FileAssetAPI.TITLE_FIELD, fileName);
				fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName);
				fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, fileData);
				fileAsset.setHost(host.getIdentifier());
				fileAsset.setLanguageId(defaultLang);
				fileAsset = this.runWorkflowIfPossible(resourceUri, user, isAutoPub, disableWorkflow, fileAsset);

				//Validate if the user have the right permission before
				this.validatePermissions(user, isAutoPub, disableWorkflow, fileAsset);
			} else {
				File fileData;

				Structure faStructure = CacheLocator.getContentTypeCache().getStructureByInode(folder.getDefaultFileType());
				Field fieldVar = faStructure.getFieldVar(FileAssetAPI.BINARY_FIELD);

				// Create user temp folder and create file inside of it
				fileData = createFileInTemporalFolder(fieldVar, user.getUserId(), fileName);


				// Saving the new working data
				try (final ReadableByteChannel inputChannel = Channels.newChannel(content);
					 final WritableByteChannel outputChannel = Channels.newChannel(Files.newOutputStream(fileData.toPath()))){

					FileUtil.fastCopyUsingNio(inputChannel, outputChannel);
					Logger.debug(this, "WEBDAV fileName:" + fileName + " : File size:" + fileData.length() + " : " + fileData.getAbsolutePath());
				}

				//Avoid uploading an empty file
				fileData = writeDataIfEmptyFile(folder, fileName, fileData);

				fileAssetCont.setInode(null);
				fileAssetCont.setFolder(parent.getInode());
				fileAssetCont.setBinary(FileAssetAPI.BINARY_FIELD, fileData);
				fileAssetCont.setLanguageId(defaultLang);
				fileAssetCont = this.runWorkflowIfPossible(resourceUri, user, isAutoPub, disableWorkflow, fileAssetCont);

				//Wiping out the thumbnails and resized versions
				//http://jira.dotmarketing.net/browse/DOTCMS-5911
				APILocator.getFileAssetAPI().cleanThumbnailsFromFileAsset(destinationFile);

				//Wipe out empty versions that Finder creates
				final List<Contentlet> versions = conAPI.findAllVersions(identifier, user, false);
				for(final Contentlet contentlet : versions){

					// Make sure we are not trying to delete the current version
					if (!contentlet.isLive() && !contentlet.isWorking()) {

						final File binary = contentlet.getBinary(FileAssetAPI.BINARY_FIELD);

						Logger.debug(this,
								() -> "inode " + contentlet.getInode() + " size: " + (null != binary
										? binary.length() : 0));
						if(null == binary || binary.length() == 0){
							Logger.debug(this, "deleting version " + contentlet.getInode());
							conAPI.deleteVersion(contentlet, user, false);
							break;
						}
					}
				}
			}
		}
	} // setResourceContent.

	private void validatePermissions(User user, boolean isAutoPub, boolean disableWorkflow,
									 Contentlet fileAsset) throws DotDataException, DotSecurityException {
		if (isAutoPub && !perAPI.doesUserHavePermission(fileAsset, PermissionAPI.PERMISSION_PUBLISH, user)) {

			if (disableWorkflow) {
				fileAsset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, disableWorkflow);
			}
			conAPI.archive(fileAsset, APILocator.getUserAPI().getSystemUser(), false);

			if (disableWorkflow) {
				fileAsset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, disableWorkflow);
			}
			conAPI.delete(fileAsset, APILocator.getUserAPI().getSystemUser(), false);
			throw new DotSecurityException(
					"User does not have permission to publish contentlets");
		} else if (!isAutoPub && !perAPI.doesUserHavePermission(fileAsset, PermissionAPI.PERMISSION_EDIT, user)) {

			if (disableWorkflow) {
				fileAsset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, disableWorkflow);
			}
			conAPI.archive(fileAsset, APILocator.getUserAPI().getSystemUser(), false);

			if (disableWorkflow) {
				fileAsset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, disableWorkflow);
			}
			conAPI.delete(fileAsset, APILocator.getUserAPI().getSystemUser(), false);
			throw new DotSecurityException("User does not have permission to edit contentlets");
		}
	}


	/**
	 * Saves a File Asset that is being uploaded into dotCMS. Based on the Contentlet's data, it will be determined
	 * whether it will be processed by a Workflow or not.
	 *
	 * @param resourceUri     The location where the Resource -- i.e., File Asset -- is being saved.
	 * @param user            The user performing this action.
	 * @param isAutoPub       If {@code true}, the Resource will be published automatically. Otherwise, set to {@code
	 *                        false}.
	 * @param disableWorkflow If {@code true}, no Workflow will be executed on the specified Resource. Otherwise, set to
	 *                        {@code false}.
	 * @param fileAsset       The Resource as File Asset that is being saved.
	 *
	 * @return The {@link Contentlet} that has just been saved.
	 *
	 * @throws DotDataException     An error occurred when accessing the data source.
	 * @throws DotSecurityException The specified user doesn't have the required permissions to permiform this action.
	 */
	private Contentlet runWorkflowIfPossible(final String resourceUri, final User user, final boolean isAutoPub,
											 final boolean disableWorkflow, final Contentlet fileAsset)
			throws DotDataException, DotSecurityException {

	    // We can use defer because we use the DB to list files / folders
		fileAsset.setIndexPolicy(IndexPolicy.DEFER);
		fileAsset.setIndexPolicyDependencies(IndexPolicy.DEFER);
		fileAsset.getMap().put(Contentlet.VALIDATE_EMPTY_FILE, true);

		return disableWorkflow?
				this.runCheckinPublishNoWorkflow(resourceUri, user, isAutoPub, disableWorkflow, fileAsset):
				this.runWorkflow(resourceUri, user, isAutoPub, disableWorkflow, fileAsset);
	}

	private boolean hasPermissionPublish (final Contentlet fileAsset, final User user) throws DotDataException, DotSecurityException {

		return UtilMethods.isSet(fileAsset.getPermissionId())?
				this.perAPI.doesUserHavePermission(fileAsset, PermissionAPI.PERMISSION_PUBLISH, user):
				this.perAPI.doesUserHavePermission(fileAsset.getContentType(), PermissionAPI.PERMISSION_PUBLISH, user);
	}

	private Contentlet runWorkflow(final String resourceUri, final User user, final boolean isAutoPub,
								   final boolean disableWorkflow, Contentlet fileAsset)
			throws DotDataException, DotSecurityException {

		if (!isAutoPub) { // if it is just save

			final Optional<WorkflowAction> saveActionOpt = fileAsset.isNew()?
					APILocator.getWorkflowAPI().findActionMappedBySystemActionContentlet(fileAsset, SystemAction.NEW, user):
					APILocator.getWorkflowAPI().findActionMappedBySystemActionContentlet(fileAsset, SystemAction.EDIT, user);
			if (saveActionOpt.isPresent() && saveActionOpt.get().hasSaveActionlet()) {

				fileAsset = APILocator.getWorkflowAPI().fireContentWorkflow(fileAsset,
						new ContentletDependencies.Builder()
								.workflowActionId(saveActionOpt.get().getId())
								.modUser(user).build());

				fileResourceCache.add(resourceUri + "|" + user.getUserId(), new Date().getTime());
				return fileAsset;
			}
		} else if (isAutoPub && this.hasPermissionPublish(fileAsset, user)) {

			final Optional<WorkflowAction> publishActionOpt = APILocator.getWorkflowAPI().
					findActionMappedBySystemActionContentlet(fileAsset, SystemAction.PUBLISH, user);
			if (publishActionOpt.isPresent()) {

				final boolean hasSave    = publishActionOpt.get().hasSaveActionlet();
				final boolean hasPublish = publishActionOpt.get().hasPublishActionlet();

				if (hasPublish) {
					if (!hasSave) {
						// do checkin without workflow
						fileAsset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, disableWorkflow);
						fileAsset = conAPI.checkin(fileAsset, user, false);
						if (fileAsset.getMap().containsKey(Contentlet.DISABLE_WORKFLOW)) {
							fileAsset.getMap().remove(Contentlet.DISABLE_WORKFLOW);
						}
					}

					fileAsset = APILocator.getWorkflowAPI().fireContentWorkflow(fileAsset,
							new ContentletDependencies.Builder()
									.workflowActionId(publishActionOpt.get().getId())
									.modUser(user).build());

					fileResourceCache.add(resourceUri + "|" + user.getUserId(), new Date().getTime());
					return fileAsset;
				}
			}
		}

		fileAsset = conAPI.checkin(fileAsset, user, false);
		fileAsset.getMap().put(Contentlet.VALIDATE_EMPTY_FILE, false);

		if (isAutoPub && perAPI
				.doesUserHavePermission(fileAsset, PermissionAPI.PERMISSION_PUBLISH, user)) {

			conAPI.publish(fileAsset, user,false);

			fileResourceCache.add(resourceUri + "|" + user.getUserId(), new Date().getTime());
		}

		return fileAsset;
	}

	private Contentlet runCheckinPublishNoWorkflow(final String resourceUri, final User user, final boolean isAutoPub,
												   final boolean disableWorkflow, Contentlet fileAsset)
			throws DotDataException, DotSecurityException {

		fileAsset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, disableWorkflow);
		fileAsset = conAPI.checkin(fileAsset, user, false);
		fileAsset.getMap().put(Contentlet.VALIDATE_EMPTY_FILE, false);

		if (isAutoPub && perAPI.doesUserHavePermission(fileAsset, PermissionAPI.PERMISSION_PUBLISH, user)) {

			fileAsset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, disableWorkflow);
			conAPI.publish(fileAsset, user,false);

			final Date currentDate = new Date();
			fileResourceCache.add(resourceUri + "|" + user.getUserId(), currentDate.getTime());
		}

		return fileAsset;
	}

	/**
	 * Create temporal user folder and create a file inside of it
	 *
	 * @param fieldVar
	 * @param userId
	 * @param fileName
	 * @return created file
	 */
	private File createFileInTemporalFolder(Field fieldVar, final String userId, final String fileName) {
		final String folderPath = new StringBuilder()
				.append(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary())
				.append(File.separator).append(userId).append(File.separator)
				.append(fieldVar.getFieldContentlet()).toString();

		File tempUserFolder = new File(folderPath);
		if (!tempUserFolder.exists())
			tempUserFolder.mkdirs();

		final String filePath = new StringBuilder()
				.append(tempUserFolder.getAbsolutePath())
				.append(File.separator).append(fileName).toString();

		File fileData = new File(filePath);
		if(fileData.exists())
			fileData.delete();

		return fileData;
	}

	public Folder createFolder(String folderUri, User user) throws IOException, DotDataException {
		Folder folder = null;
		folderUri = stripMapping(folderUri);
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		Logger.debug(this.getClass(), "createFolder");
		String hostName = getHostname(folderUri);
		String path = getPath(folderUri);

		Host host;
		try {
			host = hostAPI.findByName(hostName, user, false);
		} catch (DotDataException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		}

		// CheckPermission
		List<Permission> parentPermissions = new ArrayList<>();
		boolean hasPermission = false;
		boolean validName = true;
		String parentPath = getFolderName(path);
		if (UtilMethods.isSet(parentPath) && !parentPath.equals("/")) {
			Folder parentFolder;
			try {
				parentFolder = folderAPI.findFolderByPath(parentPath,host,user,false);
				hasPermission = perAPI.doesUserHavePermission(parentFolder,	PERMISSION_CAN_ADD_CHILDREN, user, false);
			} catch (Exception e) {
				Logger.error(DotWebdavHelper.class,"Error creating folder with URI: " + folderUri + ". Error: " + e.getMessage(),e);
				throw new IOException(e.getMessage(),e);
			}
		} else {
			try {
				hasPermission = perAPI.doesUserHavePermission(host, PERMISSION_CAN_ADD_CHILDREN, user, false);
			} catch (DotDataException e) {
				Logger.error(DotWebdavHelper.class,e.getMessage(),e);
				throw new IOException(e.getMessage(),e);
			}
		}

		// Create the new folders with it parent permissions
		if ((hasPermission) && (validName)) {
			if (InodeUtils.isSet(host.getInode())) {
				path = deleteSpecialCharacter(path);
				try {
					folder = folderAPI.createFolders(path, host,user,false);
				} catch (Exception e) {
					throw new DotDataException(e.getMessage(), e);
				}
			}
		}
		return folder;
	}

	public void move(String fromPath, String toPath, User user,boolean autoPublish)throws IOException, DotDataException {
		String resourceFromPath = fromPath;
		final String fromPathStripped = stripMapping(fromPath);
		toPath = stripMapping(toPath);
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		String hostName = getHostname(fromPathStripped);
		String toParentPath = getFolderName(getPath(toPath));

		Host host;
		Folder toParentFolder;
		try {
			host = hostAPI.findByName(hostName, user, false);
			toParentFolder = folderAPI.findFolderByPath(toParentPath,host,user,false);
		} catch (DotDataException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		}
		if (isResource(resourceFromPath,user)) {
			try {
				if (!perAPI.doesUserHavePermission(toParentFolder,
						PermissionAPI.PERMISSION_READ, user, false)) {
					throw new IOException("User doesn't have permissions to move file to folder");
				}
			} catch (DotDataException e1) {
				Logger.error(DotWebdavHelper.class,e1.getMessage(),e1);
				throw new IOException(e1.getMessage());
			}
			if (toParentFolder == null || !InodeUtils.isSet(toParentFolder.getInode())) {
				throw new IOException("Cannot move a file to the root of the host.");
			}

			try{
				final Identifier identifier  = APILocator.getIdentifierAPI().find(host, getPath(fromPathStripped));
				final Identifier identTo = APILocator.getIdentifierAPI().find(host, getPath(toPath));
				final boolean destinationExists = identTo != null && InodeUtils.isSet(identTo.getId());

				if(identifier!=null && identifier.getAssetType().equals("contentlet")){
					Contentlet fileAssetCont = conAPI.findContentletByIdentifier(identifier.getId(), false, defaultLang, user, false);
					if(!destinationExists) {
						if (getFolderName(fromPathStripped).equals(getFolderName(toPath))) {
							String fileName = getFileName(toPath);
							if(fileName.contains(".")){
								fileName = fileName.substring(0, fileName.lastIndexOf("."));
							}
							APILocator.getFileAssetAPI().renameFile(fileAssetCont, fileName, user, false);
						} else {
							APILocator.getFileAssetAPI().moveFile(fileAssetCont, toParentFolder, user, false);
						}
					} else {
						// if the destination exists lets just create a new version and delete the original file
						final Contentlet origin = conAPI.findContentletByIdentifier(identifier.getId(), false, defaultLang, user, false);
						final Contentlet toContentlet = conAPI.findContentletByIdentifier(identTo.getId(), false, defaultLang, user, false);
						Contentlet newVersion = conAPI.checkout(toContentlet.getInode(), user, false);

						final boolean sameSourceAndTarget = (origin.getIdentifier().equals(newVersion.getIdentifier()));

						// get a copy in a tmp folder to avoid filename change
						final File tmpDir = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()
								+ File.separator+UUIDGenerator.generateUuid());
						final File tmp = new File(tmpDir, toContentlet.getBinary(FileAssetAPI.BINARY_FIELD).getName());
						FileUtil.copyFile(origin.getBinary(FileAssetAPI.BINARY_FIELD), tmp);

						newVersion.setBinary(FileAssetAPI.BINARY_FIELD, tmp);
						newVersion.setLanguageId(defaultLang);
						newVersion = conAPI.checkin(newVersion, user, false);
						if(autoPublish) {
							conAPI.publish(newVersion, user, false);
						}
						if(sameSourceAndTarget){
						   //If source and target are the same this could be a rename attempt
						   identTo.setAssetName(getFileName(toPath));
						   identifierAPI.save(identTo);
						}

						conAPI.unlock(newVersion, user, false);
						// if we don't validate source and destination are the same we will end-up loosing the file.
						if(!sameSourceAndTarget){
						  final User sysUser = APILocator.systemUser();
						  conAPI.archive(origin, sysUser, false);
						  conAPI.delete(origin, sysUser, false);
						}
					}
				}

			}catch (Exception e) {
				throw new DotDataException(e.getMessage(),e);
			}
		} else {
			if (UtilMethods.isSet(toParentPath) && !toParentPath.equals("/")) {
				try {
					if (!perAPI.doesUserHavePermission(toParentFolder,	PermissionAPI.PERMISSION_READ, user, false)) {
						throw new IOException("User doesn't have permissions to move file to folder");
					}
				} catch (DotDataException e1) {
					Logger.error(DotWebdavHelper.class,e1.getMessage(),e1);
					throw new IOException(e1.getMessage());
				}
				final boolean sourceAndDestinationAreTheSame = isSameResourcePath(fromPathStripped, toPath, user);
				if (getFolderName(fromPathStripped).equals(getFolderName(toPath))) { //This line verifies the parent folder is the same.
					Logger.debug(this, "Calling FolderFactory to rename " + fromPathStripped + " to " + toPath);

					//need to verify the source and destination are not the same because we could be renaming the folder to be the same but with different casing.
					if (!sourceAndDestinationAreTheSame) {
						try {
							// Folder must end with "/", otherwise we get the parent folder
							String folderToPath = getPath(toPath);
							if (!folderToPath.endsWith("/")) {
								folderToPath = folderToPath + "/";
							}

							final Folder folder = folderAPI
									.findFolderByPath(folderToPath, host, user, false);
							removeObject(toPath, user);
							fc.removeFolder(folder, identifierAPI.find(folder.getIdentifier()));

						} catch (Exception e) {
							Logger.debug(this, "Unable to delete toPath " + toPath);
						}
					}

					final boolean renamed;
					try{
						final Folder folder = folderAPI.findFolderByPath(getPath(fromPathStripped), host,user,false);
						renamed = folderAPI.renameFolder(folder, getFileName(toPath),user,false);
						if (!sourceAndDestinationAreTheSame) {
						    fc.removeFolder(folder, identifierAPI.find(folder.getIdentifier()));
						}
					}catch (Exception e) {
						throw new DotDataException(e.getMessage(), e);
					}
					if(!renamed){
						Logger.error(this, "Unable to rename folder");
						throw new IOException("Unable to rename folder");
					}

				} else {
					Logger.debug(this, "Calling folder factory to move from " + fromPathStripped + " to " + toParentPath);
					Folder fromFolder;
					try {
						fromFolder = folderAPI.findFolderByPath(getPath(fromPathStripped), host,user,false);
					} catch (Exception e1) {
						Logger.error(DotWebdavHelper.class, e1.getMessage(), e1);
						throw new DotRuntimeException(e1.getMessage(), e1);
					}
					if(fromFolder != null){
						Logger.debug(this,
								"Calling folder factory to move from " + identifierAPI.find(
										fromFolder.getIdentifier()).getPath() + " to "
										+ toParentPath);
						Logger.debug(this, "the from folder inode is " + fromFolder.getInode());
					}else{
						Logger.debug(this, "The from folder is null");
					}
					try {
						folderAPI.move(fromFolder, toParentFolder,user,false);
						fc.removeFolder(fromFolder, identifierAPI.find(fromFolder.getIdentifier()));
						fc.removeFolder(toParentFolder, identifierAPI.find(toParentFolder.getIdentifier()));
						//folderAPI.updateMovedFolderAssets(fromFolder);
					} catch (Exception e) {
						Logger.error(DotWebdavHelper.class, e.getMessage(), e);
						throw new DotDataException(e.getMessage(), e);
					}
				}
			} else {
				try {
					if (!perAPI.doesUserHavePermission(host,PermissionAPI.PERMISSION_READ, user, false)) {
						throw new IOException("User doesn't have permissions to move file to host");
					}
				} catch (DotDataException e) {
					Logger.error(DotWebdavHelper.class,e.getMessage(),e);
					throw new IOException(e.getMessage(),e);
				}
				if (getFolderName(fromPathStripped).equals(getFolderName(toPath))) {
					final Folder fromFolder = Try.of(()->folderAPI.findFolderByPath(getPath(fromPathStripped), host, user,false)).get();
					try{
						folderAPI.renameFolder(fromFolder, getFileName(toPath),user,false);
						fc.removeFolder(fromFolder, identifierAPI.find(fromFolder.getIdentifier()));
					}catch (Exception e) {
						if( UtilMethods.isSet(fromFolder.getName()) && fromFolder.getName().toLowerCase().contains("untitled folder")){
							try {
								folderAPI.delete(fromFolder,user,false);
							} catch (DotSecurityException ex) {
								throw new DotDataException(ex.getMessage(), ex);
							}
						}
						throw new DotDataException(e.getMessage(), e);
					}
				} else {
					final Folder fromFolder;
					try {
						fromFolder = folderAPI.findFolderByPath(getPath(fromPathStripped), host,user,false);
						folderAPI.move(fromFolder, host,user,false);
						fc.removeFolder(fromFolder, identifierAPI.find(fromFolder.getIdentifier()));
					} catch (Exception e) {
						Logger.error(DotWebdavHelper.class, e.getMessage(), e);
						throw new DotDataException(e.getMessage(), e);
					}
				}
			}
		}

	}

	public void removeObject(String uri, User user) throws IOException, DotDataException, DotSecurityException {
		String resourceUri = uri;
		uri = stripMapping(uri);
		Logger.debug(this.getClass(), "In the removeObject Method");
		String hostName = getHostname(uri);
		String path = getPath(uri);
		String folderName = getFolderName(path);
		Host host;
		WebAsset webAsset=null;
		try {
			host = hostAPI.findByName(hostName, user, false);
		} catch (DotDataException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		}
		Folder folder = folderAPI.findFolderByPath(folderName, host,user,false);
		if (isResource(resourceUri,user)) {
			Identifier identifier  = APILocator.getIdentifierAPI().find(host, path);

			Long timeOfPublishing = fileResourceCache.get(uri + "|" + user.getUserId());
			Date currentDate = new Date();
			long diff = -1;
			long minTimeAllowed = Config.getIntProperty("WEBDAV_MIN_TIME_AFTER_PUBLISH_TO_ALLOW_DELETING_OF_FILES", 10);
			boolean canDelete= true;

			if(UtilMethods.isSet(timeOfPublishing)) {
				diff = (currentDate.getTime()-timeOfPublishing)/1000;
				canDelete = diff >= minTimeAllowed;
			}

			if(identifier!=null && identifier.getAssetType().equals("contentlet")){
				Contentlet fileAssetCont = conAPI
						.findContentletByIdentifier(identifier.getId(), false, defaultLang, user, false);

				//Webdav calls the delete method when is creating a new file. But it creates the file with 0 content length.
				//No need to wait 10 seconds with files with 0 length.
				if(canDelete
						|| (fileAssetCont.getBinary(FileAssetAPI.BINARY_FIELD) != null
						&& fileAssetCont.getBinary(FileAssetAPI.BINARY_FIELD).length() <= 0)){

					try{
						conAPI.archive(fileAssetCont, user, false);
						if (UtilMethods.isSet(fileAssetCont.getActionId())) {

							fileAssetCont.getMap().remove(Contentlet.WORKFLOW_ACTION_KEY);
						}
					}catch (Exception e) {
						Logger.error(DotWebdavHelper.class, e.getMessage(), e);
						throw new DotDataException(e.getMessage(), e);
					}


					fileResourceCache.remove(uri + "|" + user.getUserId());
				}
			}

		} else if (isFolder(resourceUri,user)) {
			if(!path.endsWith("/"))
				path += "/";
			folder = folderAPI.findFolderByPath(path, host,user,false);
			if (folder.isShowOnMenu()) {
				// RefreshMenus.deleteMenus();
				RefreshMenus.deleteMenu(folder);
				CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
				if(!path.equals("/")) {
					Identifier ii=APILocator.getIdentifierAPI().find(folder.getIdentifier());
					CacheLocator.getNavToolCache().removeNavByPath(ii.getHostId(), ii.getParentPath());
				}
			}

			folderAPI.delete(folder, user,false);

		}

	}

	/**
	 * This will validate that the operation triggered is a Method `MOVE` call
	 * And extracts the source and target to compare if the source and destination
	 * @param resourceValidationName
	 * @return
	 */
	boolean isSameTargetAndDestinationResourceOnMove(final String resourceValidationName) {
		final Request request = HttpManager.request();
		if (null != request) {
			final Request.Method method = request.getMethod();
			if (Method.MOVE.equals(method)) {
				if (UtilMethods.isSet(request.getAbsoluteUrl()) && UtilMethods
						.isSet(request.getDestinationHeader())) {
					final boolean sameResource = isSameResourceURL(request.getAbsoluteUrl(),request.getDestinationHeader(), resourceValidationName);
					if (sameResource) {
						Logger.warn(DotWebdavHelper.class,
								() -> " Attempt to perform a `MOVE` operation over the same source and target resource.");
					}
					return sameResource;
				}
			}
		}
		return false;
	}

	/**
	 *
	 * @param sourcePath
	 * @param targetPath
	 * @return
	 */
	boolean isSameResourcePath(final String sourcePath, final String targetPath, final User user) {
		try {
			final Folder sourceFolder = getFolder(sourcePath, user);
			final Folder targetFolder = getFolder(targetPath, user);
			if (null != sourceFolder && UtilMethods.isSet(sourceFolder.getIdentifier())
					&& null != targetFolder && UtilMethods.isSet(targetFolder.getIdentifier())) {
				return sourceFolder.getIdentifier().equals(targetFolder.getIdentifier());
			}
		} catch (DotDataException | DotSecurityException e) {
			Logger.error(DotWebdavHelper.class,
					String.format("Error trying to determine if these 2 uris (`%s`,`%s`) are the same folder.", sourcePath, targetPath),
					e);
		}
		return false;
	}

	private Folder getFolder(final String uri, final User user)
			throws DotSecurityException, DotDataException {
	   final String hostName = getHostName(uri);
	   final String path = getPath(uri);
	   final Host host = hostAPI.findByName(hostName, user, false);
	   return folderAPI.findFolderByPath(path, host, user, false);
	}

	/**
 	 * This takes care of situations like case sensitivity and and backslash at the end etc.
	 * Example  http:/demo.dotcms.com/blah/products vs http:/demo.dotcms.com/blah/Products/
	 * @param sourceUrl basically url#1
	 * @param targetUrl basically url#2
	 * @param resourceName this ia an extra param to perform an additional validation on the resourceName
	 * @return same resource returns true otherwise false.
	 */
    @VisibleForTesting
	boolean isSameResourceURL(String sourceUrl, String targetUrl, final String resourceName) {

		try {
			final Resource source = getResourceFromURL(sourceUrl);
			final Resource target = getResourceFromURL(targetUrl);

			if (source != null && target != null) {
					return source.getUniqueId().equals(target.getUniqueId()) && UtilMethods
							.isSet(source.getName()) && source.getName()
							.equalsIgnoreCase(resourceName);
			}
		} catch (Exception e) {
			Logger.error(DotWebdavHelper.class,
					String.format("Error trying to determine if these 2 urls (`%s`,`%s`) are the same resource.", sourceUrl, targetUrl),
			  e);
		}
		return false;
	}

    @VisibleForTesting
	Resource getResourceFromURL(String url) {
		String host = null;
		String path = null;
		final URLUtils.ParsedURL sourceParts = URLUtils.parseURL(url);
		if (null != sourceParts) {
			if (UtilMethods.isSet(sourceParts.getHost())) {
				host = sourceParts.getHost();
			}

			if (UtilMethods.isSet(sourceParts.getPath())) {
				path = sourceParts.getPath();
			}

			if (UtilMethods.isSet(sourceParts.getResource())) {
				if (UtilMethods.isSet(path)) {
					if (!path.endsWith("/")) {
						path = path + "/" + sourceParts.getResource();
					} else {
						path = path + sourceParts.getResource();
					}
				}
			}
		}
		//This a fallback..
		if (host == null) {
			host = getHostName(url);
		}
		if (path == null) {
			path = getPath(url);
		}

		return ResourceFactoryImpl
				.getResource(host, path, this, hostAPI);
	}

//  Previously this was was used to store a reference to the Lock token.
//  Though the wrong inode was sent  cause the number of entries on this map to grow indefinitely. The token clean up method was broken.
//	private static Map<String, LockToken> locks = new HashMap<String, LockToken>();
//	private static LockToken currentLock;

	public final LockResult lock(LockTimeout lockTimeout, LockInfo lockInfo, String uid)
	{
		//Logger.debug("Lock : " + lockTimeout + " info : " + lockInfo + " on resource : " + getName() + " in : " + parent);
		LockToken token = new LockToken();
		token.info = lockInfo;
		token.timeout = LockTimeout.parseTimeout("30");
		token.tokenId = uid;
		// no need to save a reference
		//locks.put(uid, token);
		// But we need to return a LockResult different from null. Or it'll break.
		return LockResult.success(token);
	}

	public final LockResult refreshLock(String uid)
	{
		// log.trace("RefreshLock : " + tokenId + " on resource : " + getName() + " in : " + parent);
		LockToken token = new LockToken();
		token.info = null;
		token.timeout = LockTimeout.parseTimeout("30");
		token.tokenId = uid;
		// locks.put(uid, token);
		// Again we need to return a LockResult different from null. Or it'll break.
		return LockResult.success(token);
	}

	public void unlock(String uid)
	{
		// log.trace("UnLock : " + arg0 + " on resource : " + getName() + " in : " + parent);
		// No need to perform any clean up since we're not saving anything.
		// locks.remove(uid);
	}

	public final LockToken getCurrentLock(String uid)
	{
		// log.trace("GetCurrentLock");
		// return locks.get(uid);
		// In order to disable the lock-unlock mechanism. all we need to do is return a null instead of an existing token
		// That should trick the upper HandlerHelper.isLockedOut to believe there is no lock already installed. Therefore nothing will ever be considered to be locked again.
		return null;
	}

	private String getFileName(String uri) {
		int begin = uri.lastIndexOf("/") + 1;
		int end = uri.length();
		String fileName = uri.substring(begin, end);
		return fileName;
	}

	private String getFolderName(String uri) {
		if (uri.endsWith("/")) {
			uri = uri.substring(0, uri.length() - 1);
		}
		int begin = 0;
		int end = uri.lastIndexOf("/") + 1;
		String folderName = uri.substring(begin, end);
		return folderName;
	}

	private String getHostname(String uri) {
		if (uri == null || uri.equals("")) {
			return "/";
		}
		int begin = 1;
		int end = (uri.indexOf("/", 1) != -1 ? uri.indexOf("/", 1) : uri.length());
		uri = uri.substring(begin, end);
		return uri;
	}

	private String getPath(String uri) {
		int begin = (uri.indexOf("/", 1) != -1 ? uri.indexOf("/", 1) : uri.length());
		int end = uri.length();
		uri = uri.substring(begin, end);
		return uri;
	}

	public long getLanguage(){
		return defaultLang;
	}

	/**
	 * This method takes the path and strips all strings that are related to the endpoint.
	 * Also, if the new pathing is used when it's stripping it, set it as defaultLang, so it can be used by the other methods.
	 *
	 * e.g: uri = /webdav/live/2/demo.dotcms.com/home -> defaultLang set to 2 and returns /demo.dotcms.com/home (after stripping)
	 *
	 * @param uri Full URL of the connection
	 * @return the URL without the endpoint
	 * @throws IOException when the language passed in the path doesn't exist the IOException will be thrown.
	 *
	 */
	public String stripMapping(final String uri) throws IOException {
		String r = uri;

		if(legacyPath){
			if (r.startsWith("/webdav")) {
				r = r.substring(7, r.length());
			}
			if (r.startsWith("/nonpub")) {
				r = r.substring(7, r.length());
			}
			if (r.startsWith("/autopub")) {
				r = r.substring(8, r.length());
			}
		} else {
			String[] splitUri = uri.split("/");

			//""
			//"webdav"
			//[working or live]
			//[languageId]
			// etc ie "demo.dotcms.com/..."
			if( splitUri.length >= 4 &&
					"webdav".equals(splitUri[1]) &&
					("working".equals(splitUri[2]) || "live".equals(splitUri[2])) &&
					StringUtils.isNumeric(splitUri[3])) {

				// Validate that the language exists.
				long uriLangId = Long.parseLong(splitUri[3]);
				if(languageAPI.getLanguages().contains(languageAPI.getLanguage(uriLangId))){
					defaultLang = uriLangId;
				} else {
					Logger.error(DotWebdavHelper.class,
							"The language id specified in the path does not exists: " + uriLangId);
					throw new IOException("The language id specified in the path does not exists");
				}

				// For example uri = /webdav/live/1/demo.dotcms.com
				// r is going to be /demo.dotcms.com
				r = uri.substring(uri.indexOf(splitUri[3])+splitUri[3].length(), uri.length());

			} else {
				Logger.warn(DotWebdavHelper.class, "URI already stripped: " + uri);
				r = uri;
			}
		}

		return r;
	}

	public String deleteSpecialCharacter(String fileName) throws IOException {
		if (UtilMethods.isSet(fileName)) {
			fileName = fileName.replace("\\", "");
			fileName = fileName.replace(":", "");
			fileName = fileName.replace("*", "");
			fileName = fileName.replace("?", "");
			fileName = fileName.replace("\"", "");
			fileName = fileName.replace("<", "");
			fileName = fileName.replace(">", "");
			fileName = fileName.replace("|", "");
			fileName = fileName.replace("+", " ");
			if (!UtilMethods.isSet(fileName)) {
				throw new IOException(
						"Please specify a name without special characters \\/:*?\"<>|");
			}
		}
		return fileName;

	}

	private boolean checkFolderFilter(Folder folder, String fileName) {
		boolean returnValue = false;
		returnValue = folderAPI.matchFilter(folder, fileName);
		return returnValue;
	}

	private Summary[] getChildrenData(String folderUriAux, User user) throws IOException {
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		Logger.debug(this.getClass(), "getChildrenNames");
		folderUriAux=stripMapping(folderUriAux);
		ArrayList<Summary> returnValue = new ArrayList<>();
		try {
			// ### GET THE HOST ###
			if (folderUriAux.equals("") || folderUriAux.equals("/")) {
				List<Host> hosts = hostAPI.findAll(user, false);
				for (Host host : hosts) {
					Summary s = new Summary();
					s.setName(host.getHostname());
					s.setPath("/" + s.getName());
					s.setFolder(true);
					s.setCreateDate(host.getModDate());
					s.setModifyDate(host.getModDate());
					s.setHost(host);
					returnValue.add(s);
				}
			} else {
				// ### GET THE FOLDERS AT THE FIRST LEVEL ###
				String hostName = getHostname(folderUriAux);
				Host host;
				try {
					host = hostAPI.findByName(hostName, user, false);
				} catch (DotDataException e) {
					Logger.error(DotWebdavHelper.class, e.getMessage(), e);
					throw new IOException(e.getMessage(),e);
				} catch (DotSecurityException e) {
					Logger.error(DotWebdavHelper.class, e.getMessage(), e);
					throw new IOException(e.getMessage(),e);
				}
				String path = getPath(folderUriAux);
				if (path.equals("") || path.equals("/")) {
					List<Folder> folders = folderAPI.findSubFolders(host,user,false);
					for (Folder folderAux : folders) {
						if (perAPI.doesUserHavePermission(folderAux, PERMISSION_READ, user, false)) {
							Summary s = new Summary();
							s.setName(folderAux.getName());
							s.setPath("/" + host.getHostname()
									+ identifierAPI.find(folderAux.getIdentifier()).getPath());
							s.setPath(s.getPath().substring(0,
									s.getPath().length() - 1));
							s.setFolder(true);
							s.setCreateDate(folderAux.getIDate());
							s.setCreateDate(folderAux.getModDate());
							s.setHost(host);
							returnValue.add(s);
						}
					}
				} else {
					// ### GET THE FOLDERS, HTMLPAHES AND FILES AT SECOND LEVEL
					// AND LOWERS ###
					path += "/";
					Folder folder = folderAPI.findFolderByPath(path, host, user, false);
					if (InodeUtils.isSet(folder.getInode())) {
						List<Folder> folders = new ArrayList<>();
						List<Versionable> files = new ArrayList<>();


						try {
							folders = APILocator.getFolderAPI().findSubFolders(folder, user, false);

							final FileAssetSearcher searcher = folder.isSystemFolder() 
							                ? FileAssetSearcher.builder().host(host).user(user).respectFrontendRoles(false).build()
							                : FileAssetSearcher.builder().folder(folder).user(user).respectFrontendRoles(false).build();
							    
			
							files.addAll(APILocator.getFileAssetAPI().findFileAssetsByDB(searcher));
							
							
							
						} catch (Exception ex) {
							String message = ex.getMessage();
							Logger.debug(this, ex.toString());
						}

						for (Folder folderAux : folders) {
							if (perAPI.doesUserHavePermission(folderAux,
									PERMISSION_READ, user, false)) {
								Summary s = new Summary();
								s.setFolder(true);
								s.setCreateDate(folderAux.getIDate());
								s.setModifyDate(folderAux.getModDate());
								s.setName(folderAux.getName());
								s.setPath("/" + host.getHostname()
										+ identifierAPI.find(folderAux.getIdentifier()).getPath());
								s.setPath(s.getPath().substring(0,
										s.getPath().length() - 1));
								s.setHost(host);
								returnValue.add(s);
							}
						}

						for (Versionable file : files) {
							if (perAPI.doesUserHavePermission((Permissionable)file,
									PERMISSION_READ, user, false)) {
								IFileAsset fa = (IFileAsset)file;
								String fileUri = "";
								File workingFile = null;
								InputStream is = null;
								Date idate = null;

								Identifier identifier  = APILocator.getIdentifierAPI().find(file);
								if(identifier!=null && identifier.getAssetType().equals("contentlet")){
									fileUri = identifier.getPath();
									workingFile = ((Contentlet)file).getBinary(FileAssetAPI.BINARY_FIELD);
									is = Files.newInputStream(workingFile.toPath());
									idate = file.getModDate();
								}

								int begin = fileUri.lastIndexOf("/") + 1;
								int end = fileUri.length();
								fileUri = fileUri.substring(begin, end);
								Summary s = new Summary();
								s.setFolder(false);
								s.setName(fileUri);
								s.setPath(s.getName());
								s.setPath(folderUriAux + "/" + fileUri);
								s.setCreateDate(idate);
								s.setModifyDate(fa.getModDate());

								s.setLength(is.available());
								s.setHost(host);
								s.setFile(fa);
								returnValue.add(s);
							}
						}

					}
				}
			}
		} catch (Exception ex) {
			Logger.debug(this, ex.toString());
		}
		return returnValue.toArray(new Summary[returnValue.size()]);
	}
	private InputStream getResourceContent(String resourceUri, User user) throws Exception {
		resourceUri=stripMapping(resourceUri);
		Logger.debug(this.getClass(), "getResourceContent");
		InputStream returnValue = null;
		String hostName = getHostname(resourceUri);
		String path = getPath(resourceUri);
		String folderName = getFolderName(path);
		Host host;
		Folder folder;
		try {
			host = hostAPI.findByName(hostName, user, false);
			folder = folderAPI.findFolderByPath(folderName, host,user,false);
		} catch (DotDataException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage(),e);
		}
		if (host != null && InodeUtils.isSet(host.getInode()) && InodeUtils.isSet(folder.getInode())) {
			InputStream is = null;
			Identifier identifier  = APILocator.getIdentifierAPI().find(host, path);
			if(identifier!=null && identifier.getAssetType().equals("contentlet")){
				Contentlet cont  = conAPI.findContentletByIdentifier(identifier.getId(), false, defaultLang, user, false);
				File workingFile = cont.getBinary(FileAssetAPI.BINARY_FIELD);
				is = Files.newInputStream(workingFile.toPath());
			}
			returnValue = is;
		}
		return returnValue;
	}
	private void setResourceContent(String resourceUri, InputStream content, String contentType, String characterEncoding, User user) throws Exception {
		try {
			setResourceContent(resourceUri, content, contentType, characterEncoding, Calendar.getInstance().getTime(), user, false);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(),e);
		}
	}

	public static FileResourceCache getFileResourceCache() {
		return fileResourceCache;
	}

}
