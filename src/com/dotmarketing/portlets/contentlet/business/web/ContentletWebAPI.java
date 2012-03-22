package com.dotmarketing.portlets.contentlet.business.web;

import java.util.Map;

import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.liferay.portal.model.User;
/*
 * //http://jira.dotmarketing.net/browse/DOTCMS-2273
 * To save content via ajax.
 */
public interface ContentletWebAPI {

public String saveContent(Map<String,Object> formData, boolean isAutoSave,boolean isCheckin,User user)
			throws DotContentletValidationException, Exception;

public void cancelContentEdit(String workingContentletInode,
		String currentContentletInode,User user) throws Exception;

}


