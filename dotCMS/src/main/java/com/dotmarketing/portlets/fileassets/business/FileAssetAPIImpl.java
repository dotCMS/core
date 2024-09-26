package com.dotmarketing.portlets.fileassets.business;

import com.dotcms.api.tree.Parentable;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import com.dotmarketing.portlets.contentlet.transform.strategy.FileViewStrategy;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.FolderAPIImpl;
import com.dotmarketing.portlets.structure.model.Field.DataType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.business.*;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UUIDUtil;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.rendering.velocity.viewtools.content.FileAssetMap;
import org.apache.commons.io.IOUtils;
import com.dotcms.tika.TikaUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.LOAD_META;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.USE_ALIAS;
import static com.dotmarketing.util.UtilHTML.getIconClass;
import static com.dotmarketing.util.UtilHTML.getStatusIcons;
import static com.dotmarketing.util.UtilMethods.*;
import static com.liferay.util.StringPool.BLANK;

/**
 * This class is a bridge impl that will support the older
 * com.dotmarketing.portlets.file.model.File as well as the new Contentlet based
 * files
 *
 * @author will
 *
 */
public class FileAssetAPIImpl implements FileAssetAPI {

    private final static String DEFAULT_RELATIVE_ASSET_PATH = "/assets";
	private final SystemEventsAPI systemEventsAPI;
	final ContentletAPI contAPI;
	final PermissionAPI perAPI;
	private final IdentifierAPI identifierAPI;
	private final FileAssetFactory fileAssetFactory;
	private final ContentletCache contentletCache;

	private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

	public FileAssetAPIImpl() {
	    this(
	    		APILocator.getContentletAPI(),
				APILocator.getPermissionAPI(),
				APILocator.getSystemEventsAPI(),
				APILocator.getIdentifierAPI(),
				FactoryLocator.getFileAssetFactory(),
				CacheLocator.getContentletCache()
		);
	}

	@VisibleForTesting
    public FileAssetAPIImpl(
			final ContentletAPI contAPI,
			final PermissionAPI perAPI,
			final SystemEventsAPI systemEventsAPI,
			final IdentifierAPI identifierAPI,
			final FileAssetFactory fileAssetFactory,
			final ContentletCache contentletCache) {

        this.contAPI = contAPI;
        this.perAPI = perAPI;
        this.systemEventsAPI = systemEventsAPI;
        this.identifierAPI   = identifierAPI;
        this.fileAssetFactory = fileAssetFactory;
        this.contentletCache = contentletCache;
    }

	/**
	 * Gets all the files,that the user has permission, under the specified parent.
	 *
	 * @param parent Parent where the files lives could be a folder or a Host
	 * @param sortBy sort
	 * @param working boolean include working files if true
	 * @param archived boolean include archived files if true
	 * @param user user
	 * @param respectFrontendRoles respect frontend roles
	 * @return list of fileAssets
	 */
	@Override
	@CloseDBIfOpened
	public List<FileAsset> findFileAssetsByParentable(final Parentable parent,
			final String sortBy, final boolean working, final boolean archived,
			final User user, final boolean respectFrontendRoles){

		if(parent==null) {
			throw new DotRuntimeException("parent is null :" + parent);
		}

		final BrowserQuery query = BrowserQuery.builder()
				.withHostOrFolderId(parent instanceof Folder ? ((Folder) parent).getIdentifier() : ((Host) parent).getIdentifier())
				.withUser(user)
				.showFiles(true)
				.showWorking(working)
				.showArchived(archived)
				.hostIdSystemFolder(parent instanceof Folder ? ((Folder) parent).getHostId() : ((Host) parent).getIdentifier())
				.sortBy(sortBy)
				.build();

		final List<Contentlet> contentlets = APILocator.getBrowserAPI().getContentUnderParentFromDB(query);

		return contentlets.stream().map(c -> fromContentlet(c)).collect(Collectors.toList());
	}

	@CloseDBIfOpened
	public List<FileAsset> findFileAssetsByFolder(
			final Folder parentFolder,
			final User user,
			final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		return findFileAssetsByParentable(parentFolder,null,true,false,user,respectFrontendRoles);
	}

    @Override
    @CloseDBIfOpened
    public List<FileAsset> findFileAssetsByDB(FileAssetSearcher searcher) {

        return Try.of(() -> fromContentlets(fileAssetFactory.findByDB(searcher)))
                        .getOrElseThrow(e -> e instanceof RuntimeException ? (RuntimeException) e : new DotRuntimeException(e));

    }

	@CloseDBIfOpened
	public List<FileAsset> findFileAssetsByHost(final Host parentHost, final User user, final boolean respectFrontendRoles) throws DotDataException,
	DotSecurityException {
		return findFileAssetsByParentable(parentHost,null,true,false,user,respectFrontendRoles);
	}

	@CloseDBIfOpened
	public List<FileAsset> findFileAssetsByHost(final Host parentHost, final User user, final boolean live,
												final boolean working, final boolean archived,
												final boolean respectFrontendRoles)
										throws DotDataException, DotSecurityException {
		return findFileAssetsByParentable(parentHost,null,working,archived,user,respectFrontendRoles);
	} // findFileAssetsByHost.

	@WrapInTransaction
	public void createBaseFileAssetFields(Structure structure) throws DotDataException, DotStateException {
		if (structure == null || !InodeUtils.isSet(structure.getInode())) {
			throw new DotStateException("Cannot create base fileasset fields on a Content Type that doesn't exist");
		}
		if (structure.getStructureType() != Structure.STRUCTURE_TYPE_FILEASSET) {
			throw new DotStateException("Cannot create base fileasset fields on a Content Type that is not a file asset");
		}
		Field field = new Field(HOST_FOLDER_FIELD_NAME, Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, structure, true, false, true, 1,
				"", "", "", true, false, true);

		field.setVelocityVarName(HOST_FOLDER_FIELD);
		FieldFactory.saveField(field);

		field = new Field(BINARY_FIELD_NAME, Field.FieldType.BINARY, Field.DataType.BINARY, structure, true, false, false, 2, "", "", "", true,
				false, false);
		field.setVelocityVarName(BINARY_FIELD);
		FieldFactory.saveField(field);


		field = new Field(TITLE_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, true, true, 3, "", "", "", true, false,
				true);
		field.setVelocityVarName(TITLE_FIELD);
		field.setListed(false);
		FieldFactory.saveField(field);


		field = new Field(FILE_NAME_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, true, true, 4, "", "", "", true, true,
				true);
		field.setVelocityVarName(FILE_NAME_FIELD);
		FieldFactory.saveField(field);


		field = new Field(META_DATA_TAB_NAME, Field.FieldType.TAB_DIVIDER, Field.DataType.SECTION_DIVIDER, structure, false, false, false, 5, "", "", "", false,
				false, false);
		field.setVelocityVarName("MetadataTab");
		FieldFactory.saveField(field);


		field = new Field(META_DATA_FIELD_NAME, Field.FieldType.KEY_VALUE, DataType.SYSTEM, structure, false, false, false, 6, "", "", "", true,
				true, true);
		field.setVelocityVarName(META_DATA_FIELD);
		FieldFactory.saveField(field);


		field = new Field(SHOW_ON_MENU_NAME, Field.FieldType.CHECKBOX, Field.DataType.TEXT, structure, false, false, true, 7, "|true", "false", "", true, false,
				false);
		field.setVelocityVarName(SHOW_ON_MENU);
		FieldFactory.saveField(field);


		field = new Field(SORT_ORDER_NAME, Field.FieldType.TEXT, Field.DataType.INTEGER, structure, false, false, true, 8, "", "0", "", true, false,
				false);
		field.setVelocityVarName(SORT_ORDER);
		FieldFactory.saveField(field);



		field = new Field(DESCRIPTION_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, true, true, 9, "", "", "", true, false,
				true);
		field.setVelocityVarName(DESCRIPTION);
		field.setListed(false);
		field.setSearchable(false);
		FieldFactory.saveField(field);

		FieldsCache.clearCache();
	}

	@CloseDBIfOpened
	public FileAsset fromContentlet(final Contentlet con) throws DotStateException {
		if (con == null || con.getInode() == null) {
			throw new DotStateException("Contentlet is null");
		}

		if (!con.isFileAsset()) {
			throw new DotStateException("Contentlet : " + con.getInode() + " is not a FileAsset");
		}

		if(con instanceof FileAsset) {
			return (FileAsset) con;
		}

		final FileAsset fileAsset = new FileAsset();
		fileAsset.setContentTypeId(con.getContentTypeId());
		try {
			contAPI.copyProperties(fileAsset, con.getMap());
		} catch (Exception e) {
			throw new DotStateException("Content -> FileAsset Copy Failed :" + e.getMessage(), e);
		}
		fileAsset.setHost(con.getHost());
		Contentlet originalContentlet = null;
		if(UtilMethods.isSet(con.getFolder())){
			try{
				final Identifier ident = APILocator.getIdentifierAPI().find(con);
				final Host host = APILocator.getHostAPI().find(con.getHost(), APILocator.systemUser() , false);
				final Folder folder = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), host, APILocator.systemUser(), false);
				originalContentlet = APILocator.getContentletAPI().find(con.getInode(), APILocator.systemUser(), false);
				fileAsset.setFolder(folder.getInode());
			}catch(Exception e){
				try{
					final Folder folder = APILocator.getFolderAPI().find(con.getFolder(), APILocator.systemUser(), false);
					fileAsset.setFolder(folder.getInode());
				}catch(Exception e1){
					Logger.warn(this, "Unable to convert contentlet to file asset " + con, e1);
				}
			}
		}

		fileAsset.setVariantId(con.getVariantId());
		if (null != originalContentlet && !originalContentlet.isDotAsset()){
			this.contentletCache.add(fileAsset);
		}
		return fileAsset;
	}
	
	
	public List<FileAsset> fromContentlets(final List<Contentlet> contentlets) {
		final List<FileAsset> fileAssets = new ArrayList<>();
		for (Contentlet con : contentlets) {
			fileAssets.add(fromContentlet(con));
		}
		return fileAssets;

	}

	public List<IFileAsset> fromContentletsI(final List<Contentlet> contentlets) {
		final List<IFileAsset> fileAssets = new ArrayList<>();
		for (Contentlet con : contentlets) {
			if (con.isDotAsset()) {

				fileAssets.add(transformDotAsset(con));
			} else {
				fileAssets.add(fromContentlet(con));
			}
		}
		return fileAssets;

	}

	private FileAsset transformDotAsset(Contentlet con) {
		try {
			con.setProperty(FileAssetAPI.BINARY_FIELD, Try.of(()->con.getBinary("asset")).getOrNull());

			FileAsset fileAsset = FileViewStrategy.convertToFileAsset(con, this);

			final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
			if	(request != null) {
				final String fileLink = new ResourceLink.ResourceLinkBuilder().build(request, APILocator.systemUser(), fileAsset, FileAssetAPI.BINARY_FIELD).getConfiguredImageURL();


				fileAsset.getMap().put("fileLink", fileLink);
			}
			return fileAsset;
		} catch (DotDataException | DotSecurityException e) {
			throw new DotRuntimeException(e);
		}
    }

	@CloseDBIfOpened
	public FileAssetMap fromFileAsset(final FileAsset fileAsset) throws DotStateException {
		try {
			final FileAssetMap fileAssetMap = new FileAssetMap(fileAsset);
			CacheLocator.getContentletCache().add(fileAsset);
			// We cache the original contentlet that was forced to pre-load its values. That's the state we want to maintain.
			return fileAssetMap;
		} catch (final Exception e) {
		    final String filePath = null != fileAsset ? fileAsset.getPath() + fileAsset.getFileName() : "- null -";
		    final String fileId = null != fileAsset ? fileAsset.getIdentifier() : "- null -";
            final String errorMsg = String.format("An error occurred when retrieving map for File Asset '%s' (%s): " +
                    "%s", filePath, fileId, e.getMessage());
            throw new DotStateException(errorMsg, e);
		}
	}

	public boolean isFileAsset(Contentlet con)  {
		return (con != null && con.getStructure() != null && con.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) ;
	}

    @Deprecated
	public Map<String, String> getMetaDataMap(Contentlet contentlet, final File binFile)
			throws DotDataException {
		return new TikaUtils().getMetaDataMap(contentlet.getInode(), binFile);
	}

	@CloseDBIfOpened
	public boolean fileNameExists(final Host host, final Folder folder, final String fileName, final String identifier)
			throws DotDataException {
		if (!UtilMethods.isSet(fileName)) {
			return true;
		}

		if (folder == null || host == null) {
			return false;
		}

		final Identifier folderId = APILocator.getIdentifierAPI().find(folder.getIdentifier());
		final String path =
				folder.getInode().equals(FolderAPI.SYSTEM_FOLDER) ? StringPool.FORWARD_SLASH
						+ fileName : folderId.getPath() + fileName;
		final Identifier fileAssetIdentifier = APILocator.getIdentifierAPI().find(host, path);
		if (null == fileAssetIdentifier || !InodeUtils.isSet(fileAssetIdentifier.getId())
				|| "folder".equals(fileAssetIdentifier.getAssetType())) {
			// if we're looking at a folder or the fileAssetIdentifier wasn't found. It doesn't exist for sure.
			return false;
		}
		//Beyond this point we know something matches the path for that host.
		if (!UtilMethods.isSet(identifier)) {
			//it's a brand new contentlet we're dealing with
			//At this point we know it DOES exist, and since we're dealing with a fresh contentlet that hasn't even been inserted yet (We don't need to worry about lang).
			return true;
		} else {
			// Now we have an identifier and a lang.
			// if the file-asset identifier is different from the contentlet identifier we're looking at. Then it does exist already.
			return !identifier.equals(fileAssetIdentifier.getId());
		}
	}

	@CloseDBIfOpened
	@Override
	public boolean fileNameExists(final Host host, final Folder folder, final String fileName) throws  DotDataException {

		if(!UtilMethods.isSet(fileName) || folder == null || host == null ) {
			return false;
		}
		final Identifier folderId  = this.identifierAPI.find(folder.getIdentifier());
		final String path          = folder.getInode().equals(FolderAPI.SYSTEM_FOLDER)?
				new StringBuilder(StringPool.FORWARD_SLASH).append(fileName).toString():
				new StringBuilder(folderId.getPath()).append(fileName).toString();
		final Identifier fileAsset = this.identifierAPI.find(host, path);

		return (fileAsset!=null && InodeUtils.isSet(fileAsset.getId())  && !fileAsset.getAssetType().equals(Contentlet.FOLDER_KEY));
	} // fileNameExists.

	@CloseDBIfOpened
	public boolean fileNameExists(Host host, Folder folder, String fileName, String identifier, long languageId) throws  DotDataException{
		if( !UtilMethods.isSet(fileName) ){
			return true;
		}

		if( folder == null || host == null ) {
			return false;
		}

		boolean exist = false;

		Identifier folderId = APILocator.getIdentifierAPI().find(folder.getIdentifier());
		String path = folder.getInode().equals(FolderAPI.SYSTEM_FOLDER)?"/"+fileName:folderId.getPath()+fileName;
		Identifier fileAsset = APILocator.getIdentifierAPI().find(host, path);

		if(fileAsset!=null && InodeUtils.isSet(fileAsset.getId()) && !identifier.equals(fileAsset.getId()) && !fileAsset.getAssetType().equals("folder")){
			// Let's not break old logic. ie calling fileNameExists method without languageId parameter.
			if (languageId == -1){
				exist = true;
			} else { // New logic.
				//We need to make sure that the contentlets for this identifier have the same language.
				try {
					contAPI.findContentletByIdentifier(fileAsset.getId(), false, languageId,
						APILocator.getUserAPI().getSystemUser(), false);
					exist = true;
				} catch (DotSecurityException dse) {
					// Something could failed, lets log and assume true to not break anything.
					Logger.error(FileAssetAPIImpl.class,
						"Error trying to find contentlet from identifier:" + fileAsset.getId(), dse);
					exist = true;
				} catch (DotContentletStateException dcse){
					// DotContentletStateException is thrown when content is not found.
					exist = false;
				}
			}
		}
		return exist;
    }

	public String getRelativeAssetPath(FileAsset fa) {
		String _inode = fa.getInode();
		return getRelativeAssetPath(_inode, fa.getUnderlyingFileName());
	}

	private  String getRelativeAssetPath(String inode, String fileName) {
		String _inode = inode;
		String path = "";

		path = java.io.File.separator + _inode.charAt(0)
				+ java.io.File.separator + _inode.charAt(1) + java.io.File.separator + _inode + java.io.File.separator + FileAssetAPI.BINARY_FIELD + java.io.File.separator+ fileName;

		return path;

	}

	@CloseDBIfOpened
	public  boolean renameFile (Contentlet fileAssetCont, String newName, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException, IOException {
		boolean isfileAssetContLive = false;
		Identifier id = APILocator.getIdentifierAPI().find(fileAssetCont);
		if(id!=null && InodeUtils.isSet(id.getId())){
			Host host = APILocator.getHostAPI().find(id.getHostId(), user, respectFrontendRoles);
			Folder folder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(), host, user, respectFrontendRoles);
			FileAsset fa = fromContentlet(fileAssetCont);
			String ext = fa.getExtension();
			if(!fileNameExists(host, folder, newName+ "." +ext, id.getId())){
			    if(fa.isLive()) {
					isfileAssetContLive = true;
			    }
				try {
					fileAssetCont.setInode(null);
					fileAssetCont.setFolder(folder.getInode());
					final String newFileName = newName + "." + ext;
					fileAssetCont.setStringProperty(FileAssetAPI.TITLE_FIELD, newFileName);
					fileAssetCont.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, newFileName);
					fileAssetCont= APILocator.getContentletAPI().checkin(fileAssetCont, user, respectFrontendRoles);
					if(isfileAssetContLive) {
						 APILocator.getVersionableAPI().setLive(fileAssetCont);
					}

					CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
					CacheLocator.getIdentifierCache().removeFromCacheByVersionable(fileAssetCont);
				} catch (Exception e) {
					Logger.error(this, "Unable to rename file asset to "
							+ newName + " for asset " + id.getId(), e);
					throw e;
				}
				return true;
			}
		}
		return false;
	}


	@WrapInTransaction
    public boolean moveFile ( Contentlet fileAssetCont, Host host, User user, boolean respectFrontendRoles ) throws DotStateException, DotDataException, DotSecurityException {
        return moveFile( fileAssetCont, null, host, user, respectFrontendRoles );
    }

	@WrapInTransaction
    public  boolean moveFile (Contentlet fileAssetCont, Folder parent, User user, boolean respectFrontendRoles) throws DotStateException, DotDataException, DotSecurityException  {
        return moveFile( fileAssetCont, parent, null, user, respectFrontendRoles );
    }

    private boolean moveFile (Contentlet fileAssetCont, Folder parent, Host host, User user, boolean respectFrontendRoles ) throws DotStateException, DotDataException, DotSecurityException {

        //Getting the contentlet identifier
        Identifier id = APILocator.getIdentifierAPI().find( fileAssetCont );
        if ( id != null && InodeUtils.isSet( id.getId() ) ) {

            final FileAsset fa = fromContentlet( fileAssetCont );

            if ( host == null ) {
                host = APILocator.getHostAPI().find( id.getHostId(), user, respectFrontendRoles );
            }

            //Verify if the file already exist
            Boolean fileNameExists;
            if ( parent != null ) {
                fileNameExists = fileNameExists( host, parent, fa.getFileName(), id.getId() );
            } else {
                fileNameExists = fileNameExists( host, APILocator.getFolderAPI().findSystemFolder(), fa.getFileName(), id.getId() );
            }

            if ( !fileNameExists ) {

				APILocator.getContentletAPI().move(fileAssetCont, user, host, parent, respectFrontendRoles);

				final Folder oldParent = APILocator.getFolderAPI().findFolderByPath( id.getParentPath(), host, user, respectFrontendRoles );

				if ( parent != null ) {
                    CacheLocator.getNavToolCache().removeNav(parent.getHostId(), parent.getInode());
                }

                CacheLocator.getNavToolCache().removeNav(oldParent.getHostId(), oldParent.getInode());

				this.systemEventsAPI.pushAsync(SystemEventType.MOVE_FILE_ASSET, new Payload(fileAssetCont.getMap(), Visibility.EXCLUDE_OWNER,
					new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

                return true;
            }
        }

        return false;
    }

    @CloseDBIfOpened
    public List<FileAsset> findFileAssetsByFolder(Folder parentFolder,
			String sortBy, boolean live, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return findFileAssetsByParentable(parentFolder,sortBy,!live,false,user,respectFrontendRoles);
	}

	@CloseDBIfOpened
	public List<FileAsset> findFileAssetsByFolder(Folder parentFolder,
			String sortBy, boolean live, boolean working, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return findFileAssetsByParentable(parentFolder,sortBy,working,false,user,respectFrontendRoles);
	}

  @Override
  @CloseDBIfOpened
  public FileAsset find(final String inode, final User user, final boolean respectFrontendRoles)
      throws DotDataException, DotSecurityException {

    return fromContentlet(contAPI.find(inode, user, respectFrontendRoles));

  }
	
    /**
     * Returns the absolute path under the {@code /assets/} folder for a given File Asset, based on its Inode, file
     * name, and file extension.
     *
     * @param inode    The File Asset's Inode.
     * @param fileName The File Asset's name.
     * @param ext      The File Asset's extension.
     *
     * @return The absolute path of the File Asset.
     */
    public String getRealAssetPath(final String inode, final String fileName, final String ext) {
        String realPath = Config.getStringProperty("ASSET_REAL_PATH");
        if (UtilMethods.isSet(realPath) && !realPath.endsWith(java.io.File.separator)) {
            realPath += java.io.File.separator;
        }
        String assetPath = Config.getStringProperty("ASSET_PATH", DEFAULT_RELATIVE_ASSET_PATH);
        if (UtilMethods.isSet(assetPath) && !assetPath.endsWith(java.io.File.separator)) {
            assetPath += java.io.File.separator;
        }
        final String fullFileName = UtilMethods.isSet(ext) ? fileName + "." + ext : fileName;
        final String path = ((!UtilMethods.isSet(realPath)) ? assetPath : realPath)
                + inode.charAt(0) + java.io.File.separator + inode.charAt(1)
                + java.io.File.separator + inode+ java.io.File.separator + FileAssetAPI.BINARY_FIELD + java.io.File.separator + fullFileName;

        if (!UtilMethods.isSet(realPath)) {
            return FileUtil.getRealPath(path);
        } else {
            return path;
        }
    }

	/**
	 * Returns the file on the filesystem that backup the fileAsset
	 * @param inode
	 * @param fileName generally speaking this method is expected to be called using the Underlying File Name property
	 * e.g.   getRealAssetPath(inode, fileAsset.getUnderlyingFileName())
	 * @return
	 */
	@Override
	public String getRealAssetPath(String inode, String fileName) {

		String extension = UtilMethods.getFileExtension(fileName);
		String fileNameWOExtenstion  =  UtilMethods.getFileName(fileName);
        return getRealAssetPath(inode, fileNameWOExtenstion, extension);

    }

	/**
	 * This method returns the relative path for assets
     *
     * @return the relative folder of where assets are stored
	 */
	public String getRelativeAssetsRootPath() {
        String path = "";
        path = Try.of(() -> Config.getStringProperty("ASSET_PATH", DEFAULT_RELATIVE_ASSET_PATH))
                .getOrElse(DEFAULT_RELATIVE_ASSET_PATH);
        return path;
    }

    /**
     * This method returns the root path for assets
     * @deprecated Use ConfigUtils.getAbsoluteAssetsRootPath();
     * @return the root folder of where assets are stored
     */
    public String getRealAssetsRootPath() {
        return ConfigUtils.getAbsoluteAssetsRootPath();
    }

	public String getRealAssetPath(String inode) {
        String _inode = inode;
        String path = "";

        String realPath = Config.getStringProperty("ASSET_REAL_PATH", null);
        if (UtilMethods.isSet(realPath) && !realPath.endsWith(java.io.File.separator))
            realPath = realPath + java.io.File.separator;

        String assetPath = Config.getStringProperty("ASSET_PATH", DEFAULT_RELATIVE_ASSET_PATH);
        if (UtilMethods.isSet(assetPath) && !assetPath.endsWith(java.io.File.separator))
            assetPath = assetPath + java.io.File.separator;

        path = ((!UtilMethods.isSet(realPath)) ? assetPath : realPath)
                + _inode.charAt(0) + java.io.File.separator + _inode.charAt(1)
                + java.io.File.separator + _inode+ java.io.File.separator + FileAssetAPI.BINARY_FIELD + java.io.File.separator;

        if (!UtilMethods.isSet(realPath))
            return FileUtil.getRealPath(path);
        else
            return path;

    }

    @Override
    public File getContentMetadataFile(final String inode) {
        return new File(getRealAssetsRootPath()+File.separator+
                inode.charAt(0)+File.separator+inode.charAt(1)+File.separator+inode+File.separator+
                "metaData"+File.separator+"content");
    }

	@Override
	public File getContentMetadataFile(final String inode, final String fileName) {

		return null == fileName?
				this.getContentMetadataFile(inode):
				new File(getRealAssetsRootPath()+File.separator+
				inode.charAt(0)+File.separator+inode.charAt(1)+File.separator+inode+File.separator+
				fileName);
	}

    @Override
    public String getContentMetadataAsString(File metadataFile) throws Exception {
        Logger.debug(this.getClass(), "DEBUG --> Parsing Metadata from file: " + metadataFile.getPath() );

        //Check if Metadata Max Size is set (in Bytes)
        int metadataLimitInBytes = Config.getIntProperty("META_DATA_MAX_SIZE", 5) * 1024 * 1024;

        //If Max Size limit is greater than what Java allows for Int values
        if(metadataLimitInBytes > Integer.MAX_VALUE){
            metadataLimitInBytes = Integer.MAX_VALUE;
        }

        //Subtracting 1024 Bytes (buffer size)
        metadataLimitInBytes = metadataLimitInBytes - 1024;

		String type = new TikaUtils().detect(metadataFile);

        InputStream input= Files.newInputStream(metadataFile.toPath());

        if(type.equals("application/x-gzip")) {
            // gzip compression was used
            input = new GZIPInputStream(input);
        }
        else if(type.equals("application/x-bzip2")) {
            // bzip2 compression was used
            input = new BZip2CompressorInputStream(input);
        }

        //Depending on the max limit of the metadata file size,
        //we'll get as many bytes as we can so we can parse it
        //and then they'll be added to the ContentMap
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[1024];
        int bytesRead = 0;
        int copied = 0;

        while (bytesRead < metadataLimitInBytes && (copied = input.read(buf,0,buf.length)) > -1 ) {
            baos.write(buf,0,copied);
            bytesRead = bytesRead + copied;

        }

        InputStream limitedInput = new ByteArrayInputStream(baos.toByteArray());

        //let's close the original input since it's no longer necessary to keep it open
        if (input != null) {
            try {
                input.close(); // todo: the file resource close handling for io should be on try catch
            } catch (IOException e) {
                 Logger.error(this.getClass(), "There was a problem with parsing a file Metadata: " + e.getMessage(), e);
            }
       }

        return IOUtils.toString(limitedInput);
    }

    /**
     * Cleans up thumbnails folder from a contentlet file asset, it uses the
     * identifier to remove the generated folder.
     *
     * <p>
     * Note: the thumbnails are generated once, so when the image is updated
     * then we need to clean the old thumbnails; that way it will generate a new
     * one.
     * </p>
     *
     * @param contentlet
     */
    public void cleanThumbnailsFromContentlet(Contentlet contentlet) {
        if (contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) {
            this.cleanThumbnailsFromFileAsset(APILocator.getFileAssetAPI().fromContentlet(
                    contentlet));
            return;
        }

        Logger.warn(this, "Contentlet parameter is NOT a fileasset.");
    }

    /**
     * Cleans up thumbnails folder for an specific asset, it uses the identifier
     * to remove the generated folder.
     *
     * <p>
     * Note: the thumbnails are generated once, so when the image is updated
     * then we need to clean the old thumbnails; that way it will generate a new
     * one.
     * </p>
     *
     * @param fileAsset
     */
    public void cleanThumbnailsFromFileAsset(IFileAsset fileAsset) {
        // Wiping out the thumbnails and resized versions
        // http://jira.dotmarketing.net/browse/DOTCMS-5911
        final String inode = fileAsset.getInode();
        if (UtilMethods.isSet(inode)) {
            final String realAssetPath = getRealAssetsRootPath();
            java.io.File tumbnailDir = new java.io.File(ConfigUtils.getDotGeneratedPath() + java.io.File.separator + inode.charAt(0)
                    + java.io.File.separator + inode.charAt(1));
            if (tumbnailDir != null) {
                java.io.File[] files = tumbnailDir.listFiles();
                if (files != null) {
                    for (java.io.File iofile : files) {
                        try {
                            if (iofile.getName().startsWith("dotGenerated_")) {
                                iofile.delete();
                            }
                        } catch (SecurityException e) {
                            Logger.error(
                                    this,
                                    "EditFileAction._saveWorkingFileData(): "
                                            + iofile.getName()
                                            + " cannot be erased. Please check the file permissions.");
                        } catch (Exception e) {
                            Logger.error(this,
                                    "EditFileAction._saveWorkingFileData(): " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    @Override
	public String getMimeType(String filename) {
		if (filename != null) {
			filename = filename.toLowerCase();
		}

		String mimeType;

		try {
			mimeType = Config.CONTEXT.getMimeType(filename);
			if(!UtilMethods.isSet(mimeType))
				mimeType = FileAsset.UNKNOWN_MIME_TYPE;
		}
		catch(Exception ex) {
			mimeType = FileAsset.UNKNOWN_MIME_TYPE;
			Logger.warn(this,"Error looking for mimetype on file: "+filename,ex);
		}

		return mimeType;
	}

	@Override
	public String getMimeType(final File binary) {

    	return MimeTypeUtils.getMimeType(binary);
	}

	/**
	 * @deprecated Use ConfigUtils.getAssetTempPath()
	 * @return String
	 */
	public String getRealAssetPathTmpBinary() {
		return ConfigUtils.getAssetTempPath();
	}

    /**
     * Utility method that generates an error message for specific methods.
     *
     * @param folder The folder whose child files could not be retrieved.
     * @param e      The exception being thrown.
     *
     * @return The formatted error message.
     */
    private String getFilesByFolderErrorMsg(final Folder folder, final Exception e) {
        final String folderPath = null != folder ? folder.getPath() : "- null -";
        final String folderId = null != folder ? folder.getIdentifier() : "- null -";
        return String.format("An error occurred when finding files by folder under '%s' (%s): %s", folderPath,
                folderId, e.getMessage());
    }

	@Override
	public void subscribeFileListener(final FileListener fileListener, final String fileNamePattern) {

		this.subscribeFileListener(fileListener, fileAsset -> RegEX.containsCaseInsensitive(fileAsset.getFileName(), fileNamePattern.trim()));
	}

	@Override
	public void subscribeFileListener(final FileListener fileListener, final Predicate<FileAsset> fileAssetFilter) {
		this.localSystemEventsAPI.subscribe(ContentletCheckinEvent.class, new EventSubscriber<ContentletCheckinEvent>() {

			@Override
			public String getId() {

				return fileListener.getId() + StringPool.FORWARD_SLASH + ContentletCheckinEvent.class.getName();
			}

			@Override
			public void notify(final ContentletCheckinEvent event) {

				FileAssetAPIImpl.this.triggerModifiedEvent(event, fileListener, fileAssetFilter);
			}
		});
	}

	private void triggerModifiedEvent(ContentletCheckinEvent event, FileListener fileListener, Predicate<FileAsset> fileAssetFilter) {

		try {

			final Contentlet contentletEvent = event.getContentlet();
			if (null != contentletEvent && isFileAsset(contentletEvent)) {

				final FileAsset fileAsset = fromContentlet(contentletEvent);
				if (fileAssetFilter.test(fileAsset)) {

					fileListener.fileModify(new FileEvent(UUIDUtil.uuid(), event.getUser(), fileAsset, event.getDate()));
				}
			}
		} catch (Throwable e) {
			Logger.error(this, e.getMessage());
			Logger.debug(this, e.getMessage(), e);
		}
	}

	@Override
	public FileAsset getFileByPath(final String uri, final Host site,
								   final long languageId, final boolean live) {

		FileAsset fileAsset = null;

		if (Objects.nonNull(site)) {

			Logger.debug(this, ()-> "Getting the file by path: " + uri + " for host: " + site.getHostname());
			try {

				final Identifier identifier = APILocator.getIdentifierAPI().find(site, uri);
				final Optional<ContentletVersionInfo> cinfo = APILocator.getVersionableAPI()
						.getContentletVersionInfo(identifier.getId(), languageId);

				if (cinfo.isPresent()) {

					final ContentletVersionInfo versionInfo = cinfo.get();
					final Contentlet contentlet = APILocator.getContentletAPI()
							.find(live ? versionInfo.getLiveInode() : versionInfo.getWorkingInode(),
									APILocator.systemUser(), false);
					if (contentlet.getContentType().baseType() == BaseContentType.FILEASSET) {

						fileAsset = fromContentlet(contentlet);
					}
				}
			} catch (DotDataException | DotSecurityException e) {

				Logger.error(this, "Error getting the fileasset for the path: "
						+ uri + " for host: " + site.getHostname() + ", msg: " + e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
        }

		return fileAsset;
	}
}
