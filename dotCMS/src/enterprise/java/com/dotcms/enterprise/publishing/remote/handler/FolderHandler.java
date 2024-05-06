/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.FolderBundler;
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
                        "(null)" : folder.getPath()), (null == folder ? "(null)" : folder.getInode()), e.getMessage());
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
				if (folder.isSystemFolder()) {
					continue;
				}

	        	folderName = Try.of(()-> folder.getPath()).getOrNull();
	        	folderId = folderWrapper.getFolderId();
	        	host = folderWrapper.getHost();


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

                	BeanUtils.copyProperties(temp, folder);

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
                    e.getMessage());
            Logger.error(this.getClass(), errorMsg);
			Logger.error(this, "-- Local Folder: " + (UtilMethods.isSet(temp) ? temp.toString() : "- object is null -"));
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
