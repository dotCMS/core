package com.dotmarketing.portlets.contentlet.business.web;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.util.Map;
/*
 * //http://jira.dotmarketing.net/browse/DOTCMS-2273
 * To save content via ajax.
 */
public interface ContentletWebAPI {



	/**
	 * Saves the formData info, in addition sends an event
	 * @param formData
	 * @param isAutoSave
	 * @param isCheckin
	 * @param user
	 * @return
     * @throws Exception
     */
	String saveContent(Map<String, Object> formData, boolean isAutoSave, boolean isCheckin, User user) throws Exception;

	/**
	 * Saves the formData info, in addition sends an event
	 *
	 * @param formData
	 * @param isAutoSave
	 * @param isCheckin
	 * @param user
	 * @param generateSystemEvent true in order to generate a system event for this save content operation
	 * @return
	 * @throws Exception
	 */
	String saveContent(Map<String, Object> formData, boolean isAutoSave, boolean isCheckin, User user,
					   boolean generateSystemEvent) throws Exception;


    /**
     * Validates the new/modified page taken into account the following
     * criteria:
     * <ol>
     * <li>The URL does not exist for another page.</li>
     * <li>The URL does not match an existing folder.</li>
     * <li>The URL does not match an asset file.</li>
     * </ol>
     *
     * @param contentPage
     *            - The content page as a {@link Contentlet} object.
     * @return If the page is valid, returns <code>null</code>. Otherwise,
     *         returns the message key containing the description of the error.
     * @throws DotRuntimeException
     *             If the user does not have permissions to perform the required
     *             action, or if a problem occurred when interacting with the
     *             database.
     */
	String validateNewContentPage(Contentlet contentPage);

	void cancelContentEdit(String workingContentletInode,
								  String currentContentletInode, User user) throws Exception;

}


