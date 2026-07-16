package com.dotmarketing.portlets.templates.model;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Theme;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;

import java.util.Date;

/**
 * This class represents the default <b>System Template</b> in dotCMS.
 * <p>The idea behind this approach is to allow Users and Content Authors to add any sort of content to HTML Pages
 * without having to go through the usual configuration or setup process in both the Container and the Template that
 * references it. A pre-defined code template is used to render which in turn uses the System Container to hold
 * contentlets. Such a code can be located in
 * {@link com.dotmarketing.portlets.templates.business.TemplateAPIImpl#LAYOUT_FILE_NAME}.</p>
 *
 * @author Jose Castro
 * @since Apr 1st, 2022
 */
public class SystemTemplate extends Template {

	private static final String SYSTEM_TEMPLATE_NAME = "System Template";

	/**
	 * Creates an instance of the default <b>System Template</b>.
	 */
    public SystemTemplate() {
		final String userId = APILocator.systemUser().getUserId();
		super.setIdentifier(Template.SYSTEM_TEMPLATE);
		super.setInode(Template.SYSTEM_TEMPLATE);
		super.setOwner(userId);
		super.setModUser(userId);
		super.setModDate(new Date());
		super.setTheme(Theme.SYSTEM_THEME);
		super.setTitle(SYSTEM_TEMPLATE_NAME);
		super.setFriendlyName(SYSTEM_TEMPLATE_NAME);
		super.setDrawed(true);
    }

	// we override it, in order to do the permissionable behind a template object
	@Override
	public String getPermissionType() {
		return Template.class.getCanonicalName();
	}

	@Override
	public void setIdentifier(String identifier) {
		Logger.debug(this, () -> "System Template ID cannot be overridden.");
	}

	@Override
	public void setOwner(String owner) {
		Logger.debug(this, () -> "System Template owner cannot be overridden.");
	}

	@Override
	public void setModDate(Date modDate) {
		Logger.debug(this, () -> "System Template mod date cannot be overridden.");
	}

	@Override
	public void setTitle(String title) {
		Logger.debug(this, () -> "System Template title cannot be overridden.");
	}

	@Override
	public void setInode(String inode) {
		Logger.debug(this, () -> "System Template inode cannot be overridden.");
	}

	@Override
	public void setDrawed(Boolean drawed) {
		Logger.debug(this, () -> "System Template drawed body cannot be overridden.");
	}

	@Override
	public void setTheme(String theme) {
		Logger.debug(this, () -> "System Template theme cannot be overridden.");
	}

	@Override
	public ManifestInfo getManifestInfo() {
		return new ManifestInfoBuilder()
				.objectType(PusheableAsset.TEMPLATE.getType())
				.id(this.getIdentifier())
				.inode(this.inode)
				.title(this.getTitle())
				.siteId(Host.SYSTEM_HOST)
				.build();
	}

}
