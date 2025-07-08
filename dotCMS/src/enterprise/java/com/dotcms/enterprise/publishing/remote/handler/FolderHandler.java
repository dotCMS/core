/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.FolderBundler;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.FolderWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.xstream.XStreamHandler;
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
import io.vavr.control.Try;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.beanutils.BeanUtils;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Folder-related information inside a
 * bundle and saves it in the receiving instance. This class will read and process only the {@link Folder} data files.
 * <p>
 * Folders may be managed only from the Site Browser or WebDAV. Unlike files, folders are not published, unpublished, or
 * archived.
 *
 * @author root
 * @since Mar 7, 2013
 */
public class FolderHandler implements IHandler {

	private final FolderAPI fAPI = APILocator.getFolderAPI();
	private final IdentifierAPI iAPI = APILocator.getIdentifierAPI();
	private final UserAPI uAPI = APILocator.getUserAPI();
	private final PublisherConfig config;

	public FolderHandler(PublisherConfig config) {
		this.config = config;
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(File bundleFolder) throws Exception {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
		//For each content take the wrapper and save it on DB
        List<File> folders = new ArrayList<>();
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
                final String errorMsg = String.format("Folder '%s' [%s] could not be deleted: %s", (null == folder ?
                        "(null)" : folder.getPath()), (null == folder ? "(null)" : folder.getInode()), ExceptionUtil.getErrorMessage(e));
                Logger.error(this.getClass(), errorMsg, e);
                throw new DotPublishingException(errorMsg, e);
			}
		}
	}

	private void handleFolders(Collection<File> folders) throws DotPublishingException, DotDataException{
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
		User systemUser = uAPI.getSystemUser();
		Folder temp = null;
		
		String folderName=null;
		Identifier folderId=null;
		Host host = null;
        File workingOn = null;
		try{
	        XStream xstream = XStreamHandler.newXStreamInstance();
	        //Handle folders
	        for(File folderFile: folders) {
	            workingOn = folderFile;
	        	if(folderFile.isDirectory()) continue;

                FolderWrapper folderWrapper;
	        	try (final InputStream input = Files.newInputStream(folderFile.toPath())){
                     folderWrapper = (FolderWrapper) xstream.fromXML(input);
                }


	      final Folder folder = folderWrapper.getFolder();
				if (folder.isSystemFolder() || FolderAPI.OLD_SYSTEM_FOLDER_ID.equalsIgnoreCase( folder.getIdentifier()) ) {
					continue;
				}

	        	folderName = Try.of(folder::getPath).getOrNull();
	        	folderId = folderWrapper.getFolderId();
	        	host = folderWrapper.getHost();



	        	if(folder.getOwner() == null){
					folder.setOwner(systemUser.getUserId());
				}

	        	//Check Host if exists otherwise create
	        	Host localHost = APILocator.getHostAPI().find(host.getIdentifier(), systemUser, false);
				if(UtilMethods.isEmpty(localHost::getIdentifier)){
					Logger.warn(FolderHandler.class, "Unable to publish folder: " + folderName + ". Unable to find referenced Site: " + folder.getHostId());
					Logger.warn(FolderHandler.class, "Make sure the Site exists with the id: " + folder.getHostId() + " before pushing the folder or run the integrity checker before pushing.");
					continue;

				}
	        	temp = fAPI.findFolderByPath(folderId.getPath(), localHost, systemUser, false);
	        	if(folderWrapper.getOperation().equals(PushPublisherConfig.Operation.UNPUBLISH)) {
	        		String folderIden = temp.getIdentifier();
	        		deleteFolder(temp);

					PushPublishLogger.log(getClass(), PushPublishHandler.FOLDER, PushPublishAction.UNPUBLISH,
							folderIden, temp.getInode(), temp.getName(), config.getId());

                } else if ( temp == null || !UtilMethods.isSet( temp.getInode() ) ) { //Create identifier for new folder

                    Identifier id = iAPI.find(folder.getIdentifier());
        			if(id ==null || !UtilMethods.isSet(id.getId())){
						Identifier folderIdNew;
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
                    boolean defaultStructureExist = true;
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
        				} else{
							BeanUtils.copyProperties(localFolder, folder);
							fAPI.save(localFolder, systemUser, false);
						}
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
					if(temp.isSystemFolder()) {
                	  Logger.warn(this.getClass(), "The Bundle is asking to update the SYSTEM_FOLDER identifier - sender system does not match receiver system - trying to continue");
                	  continue;
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

					temp.setOwner(folder.getOwner());
					temp.setModDate(folder.getModDate());
					temp.setDefaultFileType( folder.getDefaultFileType());
					temp.setName(folder.getName());
					temp.setSortOrder(folder.getSortOrder());
					temp.setIDate(folder.getIDate());
					temp.setFilesMasks(folder.getFilesMasks());
					temp.setTitle(folder.getTitle());


                	fAPI.save(temp, systemUser, false);
                	
                    if ( currentDefaultType != null && !defaultStructureExist ) {
                        config.addPendingFolderForDefaultType( currentDefaultType, temp );
                    }
                    
					PushPublishLogger.log(getClass(), PushPublishHandler.FOLDER, PushPublishAction.PUBLISH_UPDATE,
							folder.getIdentifier(), folder.getInode(), folder.getName(), config.getId());
    			}
	        }
    	} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when processing Folder in '%s': %s", workingOn,
                    ExceptionUtil.getErrorMessage(e));
            Logger.error(this.getClass(), errorMsg);
			Logger.error(this, "-- Local Folder: " + (UtilMethods.isSet(temp) ? temp : "- object is null -"));
			if(UtilMethods.isSet(temp) && UtilMethods.isSet(folderName)) {
			    Logger.error(this, "-- folderName:" + folderName);
			}
	        if(UtilMethods.isSet(temp) && UtilMethods.isSet(folderId)) {
                Logger.error(this, "-- folderId  :" + folderId.getId());
            }
	        if(UtilMethods.isSet(temp) && UtilMethods.isSet(host)) {
	            Logger.error(this, "-- host      :" + host.getHostname());
	        }
            Logger.error(this.getClass(), e);
			throw new DotPublishingException(errorMsg, e);
    	}
		finally {
		    HibernateUtil.getSession().clear();
		}
	}

}
