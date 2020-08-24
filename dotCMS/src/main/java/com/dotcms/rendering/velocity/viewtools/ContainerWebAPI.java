package com.dotcms.rendering.velocity.viewtools;

import static com.dotcms.util.CollectionsUtils.list;

import java.util.*;
import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.util.PageMode;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

public class ContainerWebAPI implements ViewTool {

	private HttpServletRequest request;
    private Context ctx;
    private ViewContext viewContext;
	private User backuser;
	private final PermissionAPI permissionAPI;
	private final ContainerAPI containerAPI;
	private final UserAPI userAPI;
	private UserWebAPI userWebAPI;

	public ContainerWebAPI(){
		this(APILocator.getPermissionAPI(), APILocator.getContainerAPI(), APILocator.getUserAPI(), WebAPILocator.getUserWebAPI());
	}

	@VisibleForTesting
	ContainerWebAPI(final PermissionAPI permissionAPI, final ContainerAPI containerAPI, final UserAPI userAPI,
					final UserWebAPI userWebAPI){

		this.permissionAPI = permissionAPI;
		this.containerAPI = containerAPI;
		this.userAPI = userAPI;
		this.userWebAPI = userWebAPI;
	}

	public void init(Object initData) {
		viewContext = (ViewContext) initData;
		request = viewContext.getRequest();
        ctx = viewContext.getVelocityContext();

		try {
			backuser = userWebAPI.getUser(request);
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}
	}

	public String getStructureCode(String containerIdentifier, String structureId) throws Exception {

		try {
			Container c = null;
			User sysUser = null;
			try {
				sysUser = APILocator.getUserAPI().getSystemUser();
			} catch (DotDataException e) {
				Logger.error(DotTemplateTool.class,e.getMessage(),e);
			}
			c = APILocator.getContainerAPI().getWorkingContainerById(containerIdentifier, sysUser, false);

			List<ContainerStructure> csList = APILocator.getContainerAPI().getContainerStructures(c);

			for (ContainerStructure cs : csList) {
				if(cs.getStructureId().equals(structureId)) {

					VelocityUtil vu = new VelocityUtil();
					String parsedCode = vu.parseVelocity(cs.getCode(), ctx);
					return parsedCode;
				}
			}

		} catch (Exception e) {
			Logger.error(getClass(), e.getMessage(), e);
			throw e;
		}

		return "";

	}
	/**
	 * This method returns the personalized list of content ids that match
	 * the persona of the visitor
	 * @param pageId
	 * @param containerId
	 * @param uuid
	 * @return
	 */
    public List<String> getPersonalizedContentList(String pageId, String containerId, String uuid) {
        Set<String> availablePersonalizations = UtilMethods.isSet(pageId)
                        ? Try.of(() -> APILocator.getMultiTreeAPI().getPersonalizationsForPage(pageId))
                                        .getOrElse(ImmutableSet.of(MultiTree.DOT_PERSONALIZATION_DEFAULT))
                        : ImmutableSet.of(MultiTree.DOT_PERSONALIZATION_DEFAULT);

        String pTag = WebAPILocator.getPersonalizationWebAPI().getContainerPersonalization(request);
        final PageMode pageMode = PageMode.get(request);

        pTag = (!availablePersonalizations.contains(pTag)) ? MultiTree.DOT_PERSONALIZATION_DEFAULT : pTag;

		List<String> contentlets = new ArrayList<>();
		if (ContainerUUID.UUID_LEGACY_VALUE.equals(uuid)) {
			contentlets.addAll(
					Optional.ofNullable(
								getContentsIdByUUID(containerId, pTag, ContainerUUID.UUID_START_VALUE, pageMode)
							).orElse(Collections.EMPTY_LIST)
			);

			contentlets.addAll(
					Optional.ofNullable(
							getContentsIdByUUID(containerId, pTag, ContainerUUID.UUID_LEGACY_VALUE, pageMode)
					).orElse(Collections.EMPTY_LIST)
			);
		} else {
			contentlets.addAll(
					Optional.ofNullable(
							getContentsIdByUUID(containerId, pTag, uuid, pageMode)
					).orElse(Collections.EMPTY_LIST));
		}

		if (!contentlets.isEmpty()) return contentlets;

		// if called through the ContainerResource, the content list will appear under the default UUID,
        // as the content is just being rendered in the container and is not associated with any page
        contentlets = (List<String>) ctx.get("contentletList" + containerId + ContainerUUID.UUID_LEGACY_VALUE);
        if(contentlets !=null ) {
            return contentlets;
        }
        
        return new ArrayList<>();
    }

	private List<String> getContentsIdByUUID(final String containerId, final String pTag, final String uuid, final PageMode pageMode) {

		List<String> contentlets = null;

		if (pageMode != PageMode.EDIT_MODE && pageMode != PageMode.PREVIEW_MODE) {
			// if live mode, the content list will not have a colon in the key- as it was a velocity variable
			contentlets = (List<String>) ctx.get("contentletList" + containerId + uuid + pTag.replace(":", ""));
			if (contentlets != null) {
				return contentlets;
			}
		}

		// if edit or preview mode, the content list WILL have a colon in the key, as this is
		// a map in memory
		contentlets = (List<String>) ctx.get("contentletList" + containerId + uuid + pTag);
		if(contentlets !=null ) {
			return contentlets;
		}
		return null;
	}

	/**
	 * This method checks if the logged in user (frontend) has the required permission over
	 * the passed container id
	 */
	public boolean doesUserHasPermission (String containerInode, int permission, boolean respectFrontendRoles) throws DotDataException {
		try {
			if(!InodeUtils.isSet(containerInode)) {
				return false;
			} else {
				final User systemUser = APILocator.getUserAPI().getSystemUser();
				final Container container = APILocator.getContainerAPI().find(containerInode, systemUser, respectFrontendRoles);
				return permissionAPI.doesUserHavePermission(container, permission, backuser, respectFrontendRoles);
			}
		} catch (DotSecurityException e) {
			return false;
		}
	}


	/**
	 * This method checks if the logged in user has the required permission to ADD any content into the
	 * the passed container id
	 */
	public boolean doesUserHasPermissionToAddContent (final String containerInode) throws DotDataException {

		if(!InodeUtils.isSet(containerInode)) {
			return false;
		} else {
			final User systemUser = userAPI.getSystemUser();
			Container container = null;

			try {
				container = containerAPI.find(containerInode, systemUser, false);
			} catch (DotSecurityException e) {
				//This exception should never happend
				Logger.debug(this.getClass(),
						"Exception on doesUserHasPermissionToAddContent exception message: " + e.getMessage(), e);
				throw new DotRuntimeException(e);
			}

			final List<ContentType> contentTypesInContainer = containerAPI.getContentTypesInContainer(backuser, container);
			return contentTypesInContainer != null && !contentTypesInContainer.isEmpty();
		}
	}

	/**
	 * This method checks if the logged in user has the required permission to ADD any widget into the
	 * the passed container id
	 */
	public boolean doesUserHasPermissionToAddWidget (final String containerInode) throws DotDataException {

		if(!InodeUtils.isSet(containerInode)) {
			return false;
		} else {
			try {
				final List<ContentType> contentTypesInContainer = APILocator.getContentTypeAPI(backuser).findByType(BaseContentType.WIDGET);
				return contentTypesInContainer != null && !contentTypesInContainer.isEmpty();
			} catch (DotSecurityException e) {
				//This exception should never happend
				Logger.debug(this.getClass(),
						"Exception on doesUserHasPermissionToAddWidget exception message: " + e.getMessage(), e);
				throw new DotRuntimeException(e);
			}
		}
	}

	/**
	 * This method checks if the logged in user has the required permission to ADD any form into the
	 * the passed container id
	 */
	public boolean doesUserHasPermissionToAddForm (final String containerInode) throws DotDataException {

		if(!InodeUtils.isSet(containerInode)) {
			return false;
		} else {
			try {
				final List<ContentType> contentTypesInContainer = APILocator.getContentTypeAPI(backuser).findByType(BaseContentType.FORM);
				return contentTypesInContainer != null && !contentTypesInContainer.isEmpty();
			} catch (DotSecurityException e) {
				//This exception should never happend
				Logger.debug(this.getClass(),
						"Exception on doesUserHasPermissionToAddForm exception message: " + e.getMessage(), e);
				throw new DotRuntimeException(e);
			}
		}
	}

	public String getBaseContentTypeUserHasPermissionToAdd(final String containerInode) throws DotDataException {
		final Collection<String> baseContentTypesNames = list();

		if(this.doesUserHasPermissionToAddContent(containerInode)) {
			baseContentTypesNames.add(BaseContentType.CONTENT.toString());
		}

		if(this.doesUserHasPermissionToAddWidget(containerInode)) {
			baseContentTypesNames.add(BaseContentType.WIDGET.toString());
		}

		if(this.doesUserHasPermissionToAddForm(containerInode)) {
			baseContentTypesNames.add(BaseContentType.FORM.toString());
		}

		return String.join(",", baseContentTypesNames);
	}
}
