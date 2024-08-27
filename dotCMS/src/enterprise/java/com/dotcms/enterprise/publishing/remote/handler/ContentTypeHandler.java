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

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipFieldBuilder;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContentTypeBundler;
import com.dotcms.publisher.pusher.wrapper.ContentTypeWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.workflow.helper.SystemActionMappingsHandlerMerger;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;

import java.io.File;
import java.util.*;
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
	        throw new DotRuntimeException("need an enterprise pro license to run this");
        }

		final Collection<File> contentTypes = FileUtil.listFilesRecursively
				(bundleFolder, new ContentTypeBundler().getFileFilter());

        handleContentTypes(contentTypes);
	}

	private void handleContentTypes(final Collection<File> contentTypes) throws DotPublishingException, DotDataException {

	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

	        throw new DotRuntimeException("need an enterprise pro license to run this");
        }
		File workingOn = null;
        ContentType contentType = null;
		try {
	        //Handle folders
	        for(final File contentTypeFile: contentTypes) {
				if(contentTypeFile.isDirectory()) {
					continue;
				}

				workingOn = contentTypeFile;

	        	final ContentTypeWrapper contentTypeWrapper = BundlerUtil
						.jsonToObject(contentTypeFile, ContentTypeWrapper.class);

				if(UtilMethods.isEmpty(()->contentTypeWrapper.getContentType().inode())){
					continue;
				}

	        	contentType = contentTypeWrapper.getContentType();

	        	final List<Field> fields                 = contentTypeWrapper.getFields();
				final List<FieldVariable> fieldVariables = contentTypeWrapper.getFieldVariables();


				final Optional<ContentType> inodeType = Try.of(()->typeAPI.find(contentTypeWrapper.getContentType().inode())).toJavaOptional();
				final Optional<ContentType> variableType = Try.of(()->typeAPI.find(contentTypeWrapper.getContentType().variable())).toJavaOptional();

				if(inodeType.isPresent() && variableType.isPresent() && ! inodeType.get().inode().equals(variableType.get().inode())){
					throw new DotPublishingException("ContentType:" + contentType.variable() + " exists but does not have the same inode. Expecting: " + contentType.inode() + " and got:" + variableType.get().inode() + "\n\t\tPlease delete one of the types or run the integrity checker before continuing.");
				}


	        	final ContentType localContentType = inodeType.orElse(variableType.orElse((null)));



	        	if(contentTypeWrapper.getOperation().equals(Operation.UNPUBLISH)) {
						this.deleteContentType(localContentType);
					continue;
	        	}

				// create/update the structure
				ContentType updatedContentType = this.saveOrUpdateContentType(contentTypeWrapper, contentType, fields,
						 fieldVariables, localContentType);


                //And finally verify it this content type was the default type of an already added folder
				this.verifyPendingFolders(updatedContentType);
			}
    	} catch (final Exception e) {
			Logger.error(this.getClass(), "Error on Content Type: "+ workingOn);
			Logger.error(this.getClass(), e.getMessage(), e);

			final String errorMsg = String.format("An error occurred when processing Content Type in '%s' with ID '%s': %s",
					(null == contentType ? "(empty)" : contentType.variable()),
					(null == contentType ? "(empty)" : contentType.id()),
					e.getMessage());

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

	private void deleteContentType(final ContentType localContentType)  {
		if(UtilMethods.isEmpty(()->localContentType.inode())){
			return;
		}
		try {
			if(localContentType.fixed()){
				PushPublishLogger.error(getClass(), PushPublishHandler.CONTENT_TYPE, PushPublishAction.UNPUBLISH,
						localContentType.id(), localContentType.inode(), localContentType.name(), config.getId(),
						"Type is Fixed", null);
				return;
			}
			typeAPI.delete(localContentType);
			PushPublishLogger.log(getClass(), PushPublishHandler.CONTENT_TYPE, PushPublishAction.UNPUBLISH,
					localContentType.id(), localContentType.inode(), localContentType.name(), config.getId());
		} catch(DotStateException | DotSecurityException | DotDataException ex) {
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
												final ContentType contentTypeIn,
												final List<Field> fields,
												final List<FieldVariable> fieldVariables,
												final ContentType localContentType) throws DotDataException, DotSecurityException {

		final String hostId = UtilMethods.isSet(()-> localContentType.inode())
				? localContentType.host()
				: Try.of(()->this.siteAPI.find(contentTypeIn.host(), APILocator.systemUser(), false).getIdentifier())
					.toJavaOptional()
					.orElse(Host.SYSTEM_HOST);

		final Host host = APILocator.getHostAPI().find(hostId, APILocator.systemUser(), false);

		// update host so that it works locally
		final ContentType typeToSave = hostId.equals(contentTypeIn.host())
				? contentTypeIn
				: ContentTypeBuilder.builder(contentTypeIn).from(contentTypeIn)
				.host(host.getIdentifier())
				.siteName(host.getHostname()).build();


		final List<Field> deferredFields = fields.stream()
                .map(field -> {
                    if (field instanceof RelationshipField) {
                        return RelationshipFieldBuilder.builder(field).skipRelationshipCreation(true).build();
                    }
                    return field;
                }).collect(Collectors.toList());


		typeToSave.constructWithFields(deferredFields);

		if(UtilMethods.isEmpty(()->localContentType.inode())) {

			typeAPI.save(typeToSave);

			for (final FieldVariable fieldVariable : fieldVariables) {

				fieldAPI.save(fieldVariable, APILocator.systemUser());
			}
		} else {

			typeAPI.save(typeToSave, deferredFields, fieldVariables);
		}

		PushPublishLogger.log(getClass(), PushPublishHandler.CONTENT_TYPE, PushPublishAction.PUBLISH,
				typeToSave.id(), typeToSave.inode(), typeToSave.name(), config.getId());





		ContentType returnType = typeAPI.find(typeToSave.inode());

		// set the workflow scheme
		this.setWorkflowScheme(contentTypeWrapper, typeToSave, returnType);



		if(!hostId.equals(contentTypeIn.host())){
			String siteName = Try.of(()->this.siteAPI.find(returnType.host(), APILocator.systemUser(), false).getHostname()).getOrElse(returnType.host());

			SystemMessage message = new SystemMessageBuilder()
					.setMessage("Content type: " + returnType.variable() + " imported successfully but moved from site id: " + contentTypeIn.host() + " to " + siteName)
					.setType(MessageType.SIMPLE_MESSAGE)
					.setLife(15)
					.setSeverity(MessageSeverity.INFO)
					.create();
			APILocator.getRoleAPI().findUserIdsForRole(APILocator.getRoleAPI().loadCMSAdminRole());


			SystemMessageEventUtil.getInstance().pushMessage(message, APILocator.getRoleAPI().findUserIdsForRole(APILocator.getRoleAPI().loadCMSAdminRole()));

		}

		return returnType;
	}


	private void setWorkflowScheme(final ContentTypeWrapper contentTypeWrapper,
								   final ContentType contentType,
								   final ContentType localContentType) throws DotDataException, DotSecurityException {
		boolean localExists = UtilMethods.isSet(()->localContentType.inode());

		if (hasAnyWorkflowScheme(contentTypeWrapper)) {
			this.saveWorkflowSchemes(contentTypeWrapper, contentType);
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
									 final ContentType contentType) {

		try {

			Set<String> schemes = new HashSet<>(contentTypeWrapper.getWorkflowSchemaIds().stream().map(id->
					Try.of(()->workflowAPI
							.findScheme(id).getId())
							.onFailure(e->Logger.warn(ContentTypeHandler.class,"Unable to find workflow scheme id:" + id))
							.getOrNull()
					)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet()));

			// always add a workflow if there were none sent
			if(schemes.isEmpty()){
				schemes.add(APILocator.getWorkflowAPI().findSystemWorkflowScheme().getId());
			}

			workflowAPI.saveSchemeIdsForContentType(
					contentType, schemes);



		} catch (final Exception ex) {
			Logger.warn(ContentTypeHandler.class,
					"Some of the target Schema IDs: " +
							contentTypeWrapper.getWorkflowSchemaIds().toString()
							+ " for Content Type"
							+ contentType.variable()
							+ "/"
							+ contentType.inode()
							+ " don't exist.", ex);
		}
	}


	private boolean hasAnyWorkflowScheme(final ContentTypeWrapper contentTypeWrapper) {

		return !(contentTypeWrapper.getWorkflowSchemaIds().isEmpty() && contentTypeWrapper.getWorkflowSchemaNames().isEmpty());
	}
}
