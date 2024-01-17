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
import com.dotcms.enterprise.publishing.remote.bundler.StructureBundler;
import com.dotcms.publisher.pusher.wrapper.StructureWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.beanutils.BeanUtils;

/**
 * @deprecated This Handler class has been deprecated in favor of {@link ContentTypeHandler}
 * <p>
 * This class handles {@link Structure} objects, which are the legacy version of the new {@link
 * com.dotcms.contenttype.model.type.ContentType} objects.
 */
public class StructureHandler implements IHandler {

	private PublisherConfig config;

	public StructureHandler(PublisherConfig config) {
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
		Collection<File> structures = FileUtil.listFilesRecursively(bundleFolder, new StructureBundler().getFileFilter());

        handleStructures(structures);
	}

	private void handleStructures(Collection<File> structures) throws DotPublishingException, DotDataException{
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this");
        }

		try{
	        XStream xstream = XStreamHandler.newXStreamInstance();
	        //Handle folders
	        for(File structureFile: structures) {
	        	if(structureFile.isDirectory()) continue;

	        	StructureWrapper structureWrapper;
				try(final InputStream input = Files.newInputStream(structureFile.toPath())){
					structureWrapper = (StructureWrapper) xstream.fromXML(input);
				}

	        	Structure structure = structureWrapper.getStructure();

	        	Structure localSt=CacheLocator.getContentTypeCache().getStructureByInode(structure.getInode());
	        	boolean localExists = localSt!=null && UtilMethods.isSet(localSt.getInode());

	        	if(structureWrapper.getOperation().equals(Operation.UNPUBLISH)) {
	        	    // delete operation
	        	    if(localExists) {
	        	    	String structureInode = localSt.getInode();
	        	    	if(structure.isFixed()){
	        	    		PushPublishLogger.log(getClass(), "Structure is fixed and it can't be unplublished. Id: " +structureInode, config.getId());
	        	    		continue;
	        	    	}
	        	    	try {
	        	    	    APILocator.getStructureAPI().delete(localSt, APILocator.getUserAPI().getSystemUser());
	        	    	    PushPublishLogger.log(getClass(), "Structure unplublished. Id: " +structureInode, config.getId());
	        	    	}
	        	    	catch(DotStateException ex) {
	        	    	    PushPublishLogger.log(getClass(), "Not deleting Structure Id: "+structureInode+". Reason: "+ex.getMessage(),config.getId());
	        	    	}
	        	    }
	        	}
	        	else {
	        		// create/update the structure

	        	    if(!localExists) {

	        	    	Structure sameVelVarSt = StructureFactory.getStructureByVelocityVarName(structure.getVelocityVarName());
	        	    	if(sameVelVarSt!=null && UtilMethods.isSet(sameVelVarSt.getInode())) {
	        	    		throw new DotDataException("Conflicts between Structures. Structure with velocityVarName : '"+sameVelVarSt.getVelocityVarName()+"' "
	        	    				+ "has different Inodes at sender and receiver. Please run the Integrity Checker at sender.");
	        	    	}

	        	        StructureFactory.saveStructure(structure, structure.getInode());

	        	        /*************************************************************/
	        	        /* GITHUB ISSUE: https://github.com/dotCMS/dotCMS/issues/2210
	        	         *
	        	         * We need to fill this property because, even if it was created, later into the code we use
	        	         * this one for retrieve a Field List.
	        	         *
	        	         */
	        	        /*************************************************************/
	        	        localSt = CacheLocator.getContentTypeCache().getStructureByInode(structure.getInode());
	        	    }
	        	    else {
	        	        // lets update the attributes
	        	        BeanUtils.copyProperties(localSt, structure);
	        	        StructureFactory.saveStructure(localSt);
	        	    }

	        	    // set the workflow schemes
                    try {
						ImmutableList.Builder<WorkflowScheme> schemes = new ImmutableList.Builder<>();
						WorkflowScheme scheme = null;

	        	    	for(int i =0; i < structureWrapper.getWorkflowSchemaIds().size();i++) {
							try {
								scheme = APILocator.getWorkflowAPI()
										.findScheme(structureWrapper.getWorkflowSchemaIds().get(i));
							} catch (Exception ex) {
								scheme = APILocator.getWorkflowAPI().findSchemeByName(
										structureWrapper.getWorkflowSchemaNames().get(i));
							}
							if (scheme != null) {
								schemes.add(scheme);
							}
						}
	        	        if(schemes!=null) {
                            APILocator.getWorkflowAPI().saveSchemesForStruct(
	        	                localExists ? localSt : structure, schemes.build());
	        	        }
	        	    }
	        	    catch(Exception ex) {
	        	        // well we don't have that schema here. What a shame
	        	        Logger.warn(StructureHandler.class,
	        	                "some of target schema ids ("+structureWrapper.getWorkflowSchemaIds().toString()+
	        	                ") for structure "+structure.getName()+" doesn't exists");
	        	    }

                    List<Field> fields = structureWrapper.getFields();
                    List<Field> localFields = FieldsCache.getFieldsByStructureInode( localSt.getInode() );
                    //Create a copy in order to avoid a possible concurrent modification error
                    List<String> localFieldsVarNames = new ArrayList<>();

                    for (Field localField : localFields) {
                    	localFieldsVarNames.add(localField.getVelocityVarName());
					}

                    //for each field in the pushed structure lets create it if doesn't exists and update its properties if it does
                    HibernateUtil.getSession().clear();
                    for ( Field field : fields ) {

                    	Field localField = FieldFactory.getFieldByVariableName(field.getStructureInode(), field.getVelocityVarName());

                        if ( localField == null || !UtilMethods.isSet( localField.getInode() ) ) {
                            FieldFactory.saveField( field, field.getInode() );
                        } else {
                            FieldFactory.deleteField(localField);
                            FieldFactory.saveField(field, field.getInode());
                        }
                        localFieldsVarNames.remove( field.getVelocityVarName() );

                        for(FieldVariable var : structureWrapper.getFieldVariables()) {
                        	if(var.getFieldId().equals(field.getInode())) {
                        		var.setId(null);
                        		FieldFactory.saveFieldVariable(var);
                        	}
                        }


                    }

                    if ( localFieldsVarNames.size() > 0 ) {
                        // we have local fields that didn't came
                        // in the pushed structure. lets remove them
                        for ( String localFieldVarName : localFieldsVarNames ) {
                            FieldFactory.deleteField( FieldFactory.getFieldByVariableName(structure.getInode(), localFieldVarName) );
                        }
                    }

	                FieldsCache.removeFields(structure);

	                PushPublishLogger.log(getClass(), "Structure published. Id: " +structure.getInode(), config.getId());
	        	}

                //And finally verify it this structure was the default type of an already added folder
	        	if(localSt!=null) {
	        		ArrayList<Folder> pendingFolders = config.getPendingFoldersForDefaultType( localSt.getInode() );
	        		if ( pendingFolders != null && !pendingFolders.isEmpty() ) {

	        			User systemUser = APILocator.getUserAPI().getSystemUser();

	        			for ( Folder pendingFolder : pendingFolders ) {

	        				User userToUse;
	        				try {
	        					userToUse = APILocator.getUserAPI().loadUserById( pendingFolder.getOwner() );
	        					if ( !APILocator.getPermissionAPI().doesUserHavePermission( pendingFolder, PermissionAPI.PERMISSION_PUBLISH, userToUse ) ) {
	        						userToUse = systemUser;
	        					}
	        				} catch ( Exception e ) {
	        					userToUse = systemUser;
	        					Logger.error( this.getClass(), "User " + pendingFolder.getOwner() + " does not have permissions to save :" + e.getMessage() );
	        				}

	        				//Find the folder
	        				Folder folder = APILocator.getFolderAPI().find( pendingFolder.getInode(), userToUse, false );
	        				//Set the proper structure type
	        				folder.setDefaultFileType( localSt.getInode() );
	        				APILocator.getFolderAPI().save( folder, userToUse, false );
	        			}
	        		}
	        	}

	        }

    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}
    }
}
