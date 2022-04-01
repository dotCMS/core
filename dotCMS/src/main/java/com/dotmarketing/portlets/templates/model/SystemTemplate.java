package com.dotmarketing.portlets.templates.model;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Theme;
import com.dotmarketing.util.Logger;

import java.util.Date;

/**
 *
 */
public class SystemTemplate extends Template {

	private static final String SYSTEM_TEMPLATE_NAME = "System Template";

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

	@Override
	public void setIdentifier(String identifier) {
		Logger.debug(this, () -> "");
	}

	@Override
	public void setOwner(String owner) {
		Logger.debug(this, () -> "");
	}

	@Override
	public void setModDate(Date modDate) {
		Logger.debug(this, () -> "");
	}

	@Override
	public void setTitle(String title) {
		Logger.debug(this, () -> "");
	}

	@Override
	public void setInode(String inode) {
		Logger.debug(this, () -> "");
	}

	@Override
	public void setDrawed(Boolean drawed) {
		Logger.debug(this, () -> "");
	}

	@Override
	public void setTheme(String theme) {
		Logger.debug(this, () -> "");
	}

}
