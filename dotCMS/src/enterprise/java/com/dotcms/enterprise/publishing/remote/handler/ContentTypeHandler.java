package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipFieldBuilder;
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
import com.dotcms.workflow.helper.SystemActionMappingsHandlerMerger;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
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
import java.util.stream.Collectors;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Content Type-related information inside a
 * bundle and saves it in the receiving instance. This class will read and process only the {@link ContentType} data
 * files.
 * <p>
 * In dotCMS, content is defined by “Content Types“. When building a site in dotCMS, CMS Administrators design Content
 * Types that fit their specific use case or content needs. The easiest way to think about a Content Type is as a
 * content database that you get to design, manage, permission, query and finally display.
 *
 * @author Anibal Gomez
 * @since Dec 20, 2016
 */
public class ContentTypeHandler implements IHandler {

	private final PublisherConfig config;
	private final ContentTypeAPI typeAPI;
	private final FieldAPI       fieldAPI;
	private final UserAPI        userAPI;
	private final PermissionAPI  permissionAPI;
	private final FolderAPI      folderAPI;
	private final WorkflowAPI    workflowAPI;
	private final HostAPI siteAPI;

	public ContentTypeHandler(final PublisherConfig config) {

		this.config = config;
		this.typeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
		this.fieldAPI = APILocator.getContentTypeFieldAPI();
		this.userAPI = APILocator.getUserAPI();
		this.permissionAPI = APILocator.getPermissionAPI();
		this.folderAPI = APILocator.getFolderAPI();
		this.workflowAPI = APILocator.getWorkflowAPI();
		this.siteAPI = APILocator.getHostAPI();
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@WrapInTransaction
	@Override
	public void handle(final File bundleFolder) throws Exception {

	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this");
        }

		final Collection<File> contentTypes = FileUtil.listFilesRecursively
				(bundleFolder, new ContentTypeBundler().getFileFilter());

        handleContentTypes(contentTypes);
	}

	private void handleContentTypes(final Collection<File> contentTypes) throws DotPublishingException, DotDataException {

	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

	        throw new RuntimeException("need an enterprise pro license to run this");
        }
		File workingOn = null;
        ContentType contentType = null;
		try {
	        //Handle folders
	        for(final File contentTypeFile: contentTypes) {
				workingOn = contentTypeFile;
	        	if(contentTypeFile.isDirectory()) {

	        		continue;
				}

	        	final ContentTypeWrapper contentTypeWrapper = BundlerUtil
						.jsonToObject(contentTypeFile, ContentTypeWrapper.class);

	        	contentType            = contentTypeWrapper.getContentType();
	        	final List<Field> fields                 = contentTypeWrapper.getFields();
				final List<FieldVariable> fieldVariables = contentTypeWrapper.getFieldVariables();

	        	ContentType localContentType = null;	        	
	        	try {
	        		localContentType = typeAPI.find(contentType.inode());
	        	} catch (final NotFoundInDbException e) {
	        		// The Content Type doesn't exist in the receiving instance. Just move on
	        		localContentType = null;
	        	}

	        	final boolean localExists = localContentType != null && UtilMethods.isSet(localContentType.inode());

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

						this.deleteContentType(localContentType);
					}
	        	} else {

	        		// create/update the structure
					localContentType = this.saveOrUpdateContentType(contentTypeWrapper, contentType, fields,
							 fieldVariables, localContentType, localExists);
				}

                //And finally verify it this content type was the default type of an already added folder
				this.verifyPendingFolders(localContentType);
			}
    	} catch (final Exception e) {
			final String errorMsg = String.format("An error occurred when processing Content Type in '%s' with ID '%s': %s",
					workingOn, (null == contentType ? "(empty)" : contentType.name()), (null == contentType ? "(empty)" :
                            contentType.id()), e.getMessage());
			Logger.error(this.getClass(), errorMsg, e);
			throw new DotPublishingException(errorMsg, e);
    	}
    }

	private void verifyPendingFolders(final ContentType localContentType) throws DotDataException, DotSecurityException {

		if(localContentType != null) {

			final ArrayList<Folder> pendingFolders = config.getPendingFoldersForDefaultType( localContentType.inode() );
			if ( pendingFolders != null && !pendingFolders.isEmpty() ) {

				final User systemUser = userAPI.getSystemUser();

				for (final Folder pendingFolder : pendingFolders ) {

					User userToUse;
					try {
						userToUse = userAPI.loadUserById( pendingFolder.getOwner() );
						if (!permissionAPI.doesUserHavePermission( pendingFolder, PermissionAPI.PERMISSION_PUBLISH, userToUse )) {

							userToUse = systemUser;
						}
					} catch ( Exception e ) {
						userToUse = systemUser;
						Logger.error( this.getClass(), "User " + pendingFolder.getOwner() + " does not have permissions to save :" + e.getMessage() );
					}

					//Find the folder
					final Folder folder = folderAPI.find( pendingFolder.getInode(), userToUse, false );
					//Set the proper structure type
					folder.setDefaultFileType( localContentType.inode() );
					folderAPI.save( folder, userToUse, false );
				}
			}
		}
	}

	private void deleteContentType(final ContentType localContentType) throws DotSecurityException, DotDataException {
		try {

			typeAPI.delete(localContentType);
			PushPublishLogger.log(getClass(), PushPublishHandler.CONTENT_TYPE, PushPublishAction.UNPUBLISH,
					localContentType.id(), localContentType.inode(), localContentType.name(), config.getId());
		} catch(DotStateException ex) {
			PushPublishLogger.error(getClass(), PushPublishHandler.CONTENT_TYPE, PushPublishAction.UNPUBLISH,
					localContentType.id(), localContentType.inode(), localContentType.name(), config.getId(),
					null, ex);
		}
	}

    /**
     * Saves or updates the Content Type coming from the bundle.
     *
     * @param contentTypeWrapper The {@link ContentTypeWrapper} object containing the Content Type's data in the
     *                           bundle.
     * @param contentType        The actual {@link ContentType} object from the bundle.
     * @param fields             The list of {@link Field} objects that make up the Content Type.
     * @param fieldVariables     The list of {@link FieldVariable} objects for each field in the Content Type.
     * @param localContentType   The existing local version of the Content Type, if any.
     * @param localExists        If there's a local existing version of the Content Type, set it to {@code true}.
     *                           Otherwise, set it to {@code false}.
     *
     * @return The new or updated Content Type object.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The dotCMS user accessing the APIs doesn't have the required permissions to do so.
     */
	private ContentType saveOrUpdateContentType(final ContentTypeWrapper contentTypeWrapper,
												final ContentType contentType,
												final List<Field> fields,
												final List<FieldVariable> fieldVariables,
												ContentType localContentType,
												final boolean localExists) throws DotDataException, DotSecurityException {
        final Host site = this.siteAPI.find(contentType.host(), APILocator.getUserAPI().getSystemUser(), false);
        if (null == site || !UtilMethods.isSet(site.getIdentifier())) {
            throw new NotFoundInDbException(String.format("Content Type '%s' [%s] is pointing to an invalid Site ID: " +
                    "'%s'. Make sure that the ID of the Site in the sender is the same as the receiver.", contentType
                    .name(), contentType.id(), contentType.host()));
        }
	    final List<Field> deferredFields = fields.stream()
                .map(field -> {
                    if (field instanceof RelationshipField) {
                        return RelationshipFieldBuilder.builder(field).skipRelationshipCreation(true).build();
                    }
                    return field;
                }).collect(Collectors.toList());
		contentType.constructWithFields(deferredFields);

		if(localContentType == null) {

			typeAPI.save(contentType);

			for (final FieldVariable fieldVariable : fieldVariables) {

				fieldAPI.save(fieldVariable, APILocator.systemUser());
			}
		} else {

			typeAPI.save(contentType, deferredFields, fieldVariables);
		}

		PushPublishLogger.log(getClass(), PushPublishHandler.CONTENT_TYPE, PushPublishAction.PUBLISH,
				contentType.id(), contentType.inode(), contentType.name(), config.getId());

		localContentType = typeAPI.find(contentType.inode());

		// set the workflow scheme
		setWorkflowScheme(contentTypeWrapper, contentType, localContentType, localExists);
		return localContentType;
	}

	private void setWorkflowScheme(final ContentTypeWrapper contentTypeWrapper,
								   final ContentType contentType,
								   final ContentType localContentType,
								   final boolean localExists) throws DotDataException, DotSecurityException {

		if (hasAnyWorkflowScheme(contentTypeWrapper)) {

			this.saveWorkflowSchemes(contentTypeWrapper, contentType, localContentType, localExists);
		} else {
			//if no workflow scheme is set. We need to reset the content type.
			workflowAPI.saveSchemeIdsForContentType(contentType, Collections.emptySet());
		}

		// save system action
        final List<SystemActionWorkflowActionMapping> remoteSystemActionMappings =
                contentTypeWrapper.getSystemActionMappings();
		new SystemActionMappingsHandlerMerger(workflowAPI).mergeSystemActions(localExists? localContentType : contentType,
                remoteSystemActionMappings);
	}

	private void saveWorkflowSchemes(final ContentTypeWrapper contentTypeWrapper,
									 final ContentType contentType,
									 final ContentType localContentType,
									 final boolean localExists) {

		try {

			final ImmutableList.Builder<WorkflowScheme> schemes = new ImmutableList.Builder<>();
			WorkflowScheme scheme = null;

			for (int i = 0; i < contentTypeWrapper.getWorkflowSchemaIds().size(); i++) {

				final String workflowId = contentTypeWrapper.getWorkflowSchemaIds()
						.get(i);
				try {
					scheme = workflowAPI
							.findScheme(workflowId);
				} catch (Exception ex) {
					Logger.error(ContentTypeHandler.class,
							String.format("Error retrieving Workflow Scheme ID [%s]: %s",
									workflowId, ex.getMessage()), ex);
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
		} catch (final Exception ex) {
			Logger.warn(ContentTypeHandler.class, String.format("Some of the target Schema IDs [ %s ] for Content Type" +
					" '%s' [%s] don't exist: %s", contentTypeWrapper.getWorkflowSchemaIds().toString(), contentType
					.name(), contentType.id(), ex.getMessage()));
		}
	}


	private boolean hasAnyWorkflowScheme(final ContentTypeWrapper contentTypeWrapper) {

		return !(contentTypeWrapper.getWorkflowSchemaIds().isEmpty() && contentTypeWrapper.getWorkflowSchemaNames().isEmpty());
	}
}
