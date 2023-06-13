package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.FolderBundler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.FolderWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPIImpl;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.BeanUtils;

public class FolderHandler implements IHandler {
	private FolderAPI fAPI = APILocator.getFolderAPI();
	private IdentifierAPI iAPI = APILocator.getIdentifierAPI();
	private UserAPI uAPI = APILocator.getUserAPI();
	private PublisherConfig config;

	public FolderHandler(PublisherConfig config) {
		this.config = config;
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(File bundleFolder) throws Exception {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this");
		//For each content take the wrapper and save it on DB
        List<File> folders = new ArrayList<File>();
        if(new File(bundleFolder + File.separator + "ROOT").exists()){
        	folders = FileUtil.listFilesRecursively(new File(bundleFolder + File.separator + "ROOT"), new FolderBundler().getFileFilter());
        	Collections.sort(folders, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					if(o1.getAbsolutePath().length() > o2.getAbsolutePath().length()){
						return 1;
					}
					if(o1.getAbsolutePath().length() < o2.getAbsolutePath().length()){
						return -1;
					}
					//Not greater nor less is equals
					return 0;
				}
			});
        }
    	handleFolders(folders);
	}

	private void deleteFolder(Folder folder) throws DotPublishingException, DotDataException{
		try {
			APILocator.getFolderAPI().delete(folder, APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotSecurityException e) {
			if(e.getMessage().equals("YOU CANNOT DELETE THE SYSTEM FOLDER")) {
				Logger.debug(getClass(), "SYSTEM FOLDER CANNOT BE DELETED");
			} else {
				Logger.error(FolderHandler.class,e.getMessage(),e);
				throw new DotPublishingException("Unable to delete with system user",e);
			}
		}
	}


	private void handleFolders(Collection<File> folders) throws DotPublishingException, DotDataException{
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this");
		User systemUser = uAPI.getSystemUser();
		Folder temp = null;
		try{
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(File folderFile: folders) {
	        	if(folderFile.isDirectory()) continue;

                FolderWrapper folderWrapper;
	        	try (final InputStream input = Files.newInputStream(folderFile.toPath())){
                     folderWrapper = (FolderWrapper) xstream.fromXML(input);
                }

	        	Folder folder = folderWrapper.getFolder();
	        	Identifier folderId = folderWrapper.getFolderId();
	        	Host host = folderWrapper.getHost();


	        	if(folder.getOwner() == null){
					folder.setOwner(systemUser.getUserId());
				}

	        	//Check Host if exists otherwise create
	        	Host localHost = APILocator.getHostAPI().find(host.getIdentifier(), systemUser, false);

	        	temp = fAPI.findFolderByPath(folderId.getPath(), localHost, systemUser, false);
	        	if(folderWrapper.getOperation().equals(PushPublisherConfig.Operation.UNPUBLISH)) {
	        		String folderIden = temp.getIdentifier();
	        		deleteFolder(temp);

					PushPublishLogger.log(getClass(), PushPublishHandler.FOLDER, PushPublishAction.UNPUBLISH,
							folderIden, temp.getInode(), temp.getName(), config.getId());

                } else if ( temp == null || !UtilMethods.isSet( temp.getInode() ) ) { //Create identifier for new folder

                    Identifier id = iAPI.find(folder.getIdentifier());
        			if(id ==null || !UtilMethods.isSet(id.getId())){
        				Identifier folderIdNew = null;
        				if(folderId.getParentPath().equals("/")) {
	            			folderIdNew = iAPI.createNew(folder,
	            					localHost,
	            					folderId.getId());
        				} else {
        					folderIdNew = iAPI.createNew(folder,
                					fAPI.findFolderByPath(folderId.getParentPath(), localHost, systemUser, false),
                					folderId.getId());
        				}
            			folder.setIdentifier(folderIdNew.getId());
               		}


        			// look for local folder by path
        			Folder localFolder = fAPI.findFolderByPath(folderId.getPath(), localHost, systemUser, false);
        			// look for local folder by inode
        			try {
        				if(localFolder == null || !UtilMethods.isSet(localFolder.getInode())) {
        					localFolder = fAPI.find(folder.getInode(), systemUser, false);
        				}
        			} catch(DotDataException e) {
        				Logger.debug(getClass(), e.getMessage());
        			}

                    //Default structure of the folder
                    Boolean defaultStructureExist = true;
                    String currentDefaultType = folder.getDefaultFileType();
                    if ( currentDefaultType != null ) {

                        //We need to verify if the default structure exist on this server
                        Structure currentDefaultStructure = CacheLocator.getContentTypeCache().getStructureByInode( currentDefaultType );
                        if ( currentDefaultStructure == null || !InodeUtils.isSet( currentDefaultStructure.getInode() ) ) {

                            /*
                             As the default structure does not exist yet lets set the default structure
                             of this server to the folder.
                             */
                            defaultStructureExist = false;
                            folder.setDefaultFileType( CacheLocator.getContentTypeCache().getStructureByVelocityVarName( FileAssetAPIImpl.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME ).getInode() );
                        }
                    }

        			if((localFolder == null || !UtilMethods.isSet(localFolder.getInode())) && !fAPI.exists(folder.getInode())) {
        				fAPI.save(folder, folder.getInode(), systemUser, false);
        			}
        			// different parentPaths, the folder was moved, so let's move it
        			else {

        				if(!folderId.getParentPath().equals(id.getParentPath())) {
            				// if was moved to HOST
            				if(folderId.getParentPath().equals("/")) {
            					fAPI.move(localFolder, localHost, systemUser, false);
            				} else { // if was moved to another FOLDER
            					Folder newParentFolder = fAPI.findFolderByPath(folderId.getParentPath(), localHost, systemUser, false);
            					fAPI.move(localFolder, newParentFolder, systemUser, false);
            				}
        				}
        				else if(!folderId.getAssetName().equals(id.getAssetName())) {
        				    fAPI.renameFolder(localFolder, folderId.getAssetName(), systemUser, false);
        				}

        				BeanUtils.copyProperties(folder, localFolder);
                        fAPI.save(localFolder, systemUser, false);

        				CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(id.getId());
        			}

                    /*
                     Add the folder to a list of pending folders to update after the real default structure is added
                     */
                    if ( currentDefaultType != null && !defaultStructureExist ) {
                        config.addPendingFolderForDefaultType( currentDefaultType, folder );
                    }

					PushPublishLogger.log(getClass(), PushPublishHandler.FOLDER, PushPublishAction.PUBLISH_CREATE,
							folder.getIdentifier(), folder.getInode(), folder.getName(), config.getId());

                } else { // Folder already exists, lets update its properties with remote folder's ones
                	if(!temp.getInode().equals(folder.getInode())) {
                		throw new DotDataException("Conflicts between Folders. Folder with path : '"+folderId.getPath()+"' "
                				+ "has different Inodes at sender and receiver. Please run the Integrity Checker at sender.");
                	}
                	// if we are trying to update the system folder, just move on.
                	if(Folder.SYSTEM_FOLDER.equals(temp.getInode())){
                	  Logger.warn(this.getClass(), "The Bundle is asking to update the SYSTEM_FOLDER identifier - sender system does not match receiver system - trying to continue");
                	  continue;
                	}
                	

                	BeanUtils.copyProperties(folder, temp);

                	fAPI.save(temp, systemUser, false);

					PushPublishLogger.log(getClass(), PushPublishHandler.FOLDER, PushPublishAction.PUBLISH_UPDATE,
							folder.getIdentifier(), folder.getInode(), folder.getName(), config.getId());
    			}
	        }
    	}
    	catch(Exception e){
			Logger.error(this, "An error occurred when saving local Folder: " + (UtilMethods.isSet(temp) ? temp.toString() : "- object is null -"));
    		throw new DotPublishingException(e.getMessage(),e);
    	}
		finally {
		    HibernateUtil.getSession().clear();
		}
	}

}
