package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContentTypeBundler;
import com.dotcms.publisher.pusher.wrapper.ContentTypeWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ContentTypeHandler implements IHandler {

	private final PublisherConfig config;

	private final ContentTypeAPI typeAPI;
	private final FieldAPI fAPI;

	private final UserAPI userAPI;
	private final PermissionAPI permissionAPI;
	private final FolderAPI folderAPI;
	private final WorkflowAPI workflowAPI;

	public ContentTypeHandler(PublisherConfig config) {
		this.config = config;

		this.typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
		this.fAPI = APILocator.getContentTypeFieldAPI();

		this.userAPI = APILocator.getUserAPI();
		this.permissionAPI = APILocator.getPermissionAPI();
		this.folderAPI = APILocator.getFolderAPI();
		this.workflowAPI = APILocator.getWorkflowAPI();
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
		Collection<File> structures = FileUtil.listFilesRecursively(bundleFolder, new ContentTypeBundler().getFileFilter());

        handleContentTypes(structures);
	}

	private void handleContentTypes(Collection<File> structures) throws DotPublishingException, DotDataException{
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this");
        }

		try{
	        //Handle folders
	        for(File structureFile: structures) {
	        	if(structureFile.isDirectory()) continue;

	        	ContentTypeWrapper contentTypeWrapper = BundlerUtil.jsonToObject(structureFile, ContentTypeWrapper.class);

	        	ContentType contentType = contentTypeWrapper.getContentType();
	        	List<Field> fields = contentTypeWrapper.getFields();
	        	List<FieldVariable> fieldVariables = contentTypeWrapper.getFieldVariables();

	        	ContentType localContentType = null;	        	
	        	try {
	        		localContentType = typeAPI.find(contentType.inode());
	        	} catch (NotFoundInDbException e) {
	        		localContentType = null;
	        	}
	        	boolean localExists = localContentType != null && UtilMethods.isSet(localContentType.inode());

	        	if(contentTypeWrapper.getOperation().equals(Operation.UNPUBLISH)) {

	        		// delete operation
	        	    if(localExists) {
	        	    	String contentTypeInode = localContentType.inode();
	        	    	if(contentType.fixed()){
							PushPublishLogger.error(getClass(), PushPublishHandler.CONTENT_TYPE, PushPublishAction.UNPUBLISH,
									localContentType.id(), localContentType.inode(), localContentType.name(), config.getId(),
									"Type is Fixed", null);
	        	    		continue;
	        	    	}
	        	    	try {
	        	    		typeAPI.delete(localContentType);
							PushPublishLogger.log(getClass(), PushPublishHandler.CONTENT_TYPE, PushPublishAction.UNPUBLISH,
									localContentType.id(), localContentType.inode(), localContentType.name(), config.getId());
	        	    	}
	        	    	catch(DotStateException ex) {
							PushPublishLogger.error(getClass(), PushPublishHandler.CONTENT_TYPE, PushPublishAction.UNPUBLISH,
									localContentType.id(), localContentType.inode(), localContentType.name(), config.getId(),
									null, ex);
	        	    	}
	        	    }

	        	} else {

	        		// create/update the structure
        	    	contentType.constructWithFields(fields);

        	    	if(localContentType == null){
        	    		typeAPI.save(contentType);
        	    		
        	    		for(FieldVariable fieldVariable : fieldVariables) {
                        	fAPI.save(fieldVariable, APILocator.systemUser());
                        }
        	    	}else{
        	    		typeAPI.save(contentType, fields, fieldVariables);
        	    	}
					PushPublishLogger.log(getClass(), PushPublishHandler.CONTENT_TYPE, PushPublishAction.PUBLISH,
							contentType.id(), contentType.inode(), contentType.name(), config.getId());

        	    	/*************************************************************/
        	        /* GITHUB ISSUE: https://github.com/dotCMS/dotCMS/issues/2210
        	         *
        	         * We need to fill this property because, even if it was created, later into the code we use
        	         * this one for retrieve a Field List.
        	         *
        	         */
        	        /*************************************************************/
        	        localContentType = typeAPI.find(contentType.inode());

					// set the workflow scheme
					if (hasAnyWorkflowScheme(contentTypeWrapper)) {
						try {
							ImmutableList.Builder<WorkflowScheme> schemes = new ImmutableList.Builder<>();
							WorkflowScheme scheme = null;
							for (int i = 0; i < contentTypeWrapper.getWorkflowSchemaIds().size(); i++) {

								final String workflowId = contentTypeWrapper.getWorkflowSchemaIds()
										.get(i);
								try {
									scheme = workflowAPI
											.findScheme(workflowId);
								} catch (Exception ex) {
									Logger.error(ContentTypeHandler.class,
											String.format("Error retrieving workflow Scheme [%s]",
													workflowId), ex);
								}
								if (scheme != null) {
									schemes.add(scheme);
								} else {
						    	/*
						    	Workflow Scheme should exist..., the workflow should be already
						    	be processed by the WorkflowHandler.
						    	 */
									Logger.error(ContentTypeHandler.class,
											String.format(
													"Relating Workflow Scheme to Content Type [%s]. Workflow Scheme with id not found [%s]",
													contentType.name(), workflowId));
								}
							}
							if (scheme != null) {
								workflowAPI.saveSchemesForStruct(
										new StructureTransformer(localExists ? localContentType : contentType).asStructure(), schemes.build());
							}
						} catch (Exception ex) {
							// well we don't have that schema here. What a shame
							Logger.warn(ContentTypeHandler.class,
									"Some of the target schema ids (" + contentTypeWrapper.getWorkflowSchemaIds().toString() +
											") for content type " + contentType.name() + " doesn't exists");
						}
					} else {
						//if no workflow scheme is set. We need to reset the content type.
						workflowAPI.saveSchemeIdsForContentType(contentType, Collections.emptySet());
					}
				}

                //And finally verify it this content type was the default type of an already added folder
	        	if(localContentType != null) {
	        		ArrayList<Folder> pendingFolders = config.getPendingFoldersForDefaultType( localContentType.inode() );
	        		if ( pendingFolders != null && !pendingFolders.isEmpty() ) {

	        			User systemUser = userAPI.getSystemUser();

	        			for ( Folder pendingFolder : pendingFolders ) {

	        				User userToUse;
	        				try {
	        					userToUse = userAPI.loadUserById( pendingFolder.getOwner() );
	        					if ( !permissionAPI.doesUserHavePermission( pendingFolder, PermissionAPI.PERMISSION_PUBLISH, userToUse ) ) {
	        						userToUse = systemUser;
	        					}
	        				} catch ( Exception e ) {
	        					userToUse = systemUser;
	        					Logger.error( this.getClass(), "User " + pendingFolder.getOwner() + " does not have permissions to save :" + e.getMessage() );
	        				}

	        				//Find the folder
	        				Folder folder = folderAPI.find( pendingFolder.getInode(), userToUse, false );
	        				//Set the proper structure type
	        				folder.setDefaultFileType( localContentType.inode() );
	        				folderAPI.save( folder, userToUse, false );
	        			}
	        		}
	        	}
	        }
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}
    }


    private boolean hasAnyWorkflowScheme(final ContentTypeWrapper contentTypeWrapper){
		return !(contentTypeWrapper.getWorkflowSchemaIds().isEmpty() && contentTypeWrapper.getWorkflowSchemaNames().isEmpty());
	}
}
