package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.rendering.velocity.viewtools.exception.DotToolException;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.RELATIONSHIP_KEY;
import static com.dotmarketing.portlets.workflows.util.WorkflowActionletUtil.getParameterValue;


/**
 * This class allows on velocity to send message by the web socker plaform
 * @author jsanca
 *
 */
public class MessageTool implements ViewTool {

	protected final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();
	private User user;

	public void init(final Object initData) {
		final HttpServletRequest request = ((ViewContext) initData).getRequest();
		this.user = getUser(request);
	}

	public void sendInfo(final String message) throws DotDataException {

		this.sendInfo(message, 5);
	}

	public void sendInfo(final String message, final int timeToLifeInSeconds) throws DotDataException {

		final User  currentUser           = this.user;
		final List<String> users          = Arrays.asList(currentUser.getUserId());
		this.sendInfo(message, timeToLifeInSeconds, users);
	}

	public void sendInfo(final String message, final int timeToLifeInSeconds,
						 final List<String> users) throws DotDataException {

		this.send(message, timeToLifeInSeconds, users, MessageSeverity.INFO);
	}

	/////
	public void sendSuccess(final String message) throws DotDataException {

		this.sendSuccess(message, 5);
	}

	public void sendSuccess(final String message, final int timeToLifeInSeconds) throws DotDataException {

		final User  currentUser           = this.user;
		final List<String> users          = Arrays.asList(currentUser.getUserId());
		this.sendSuccess(message, timeToLifeInSeconds, users);
	}

	public void sendSuccess(final String message, final int timeToLifeInSeconds,
						 final List<String> users) throws DotDataException {

		this.send(message, timeToLifeInSeconds, users, MessageSeverity.SUCCESS);
	}

	/////
	public void sendWarning(final String message) throws DotDataException {

		this.sendWarning(message, 5);
	}

	public void sendWarning(final String message, final int timeToLifeInSeconds) throws DotDataException {

		final User  currentUser           = this.user;
		final List<String> users          = Arrays.asList(currentUser.getUserId());
		this.sendWarning(message, timeToLifeInSeconds, users);
	}

	public void sendWarning(final String message, final int timeToLifeInSeconds,
							final List<String> users) throws DotDataException {

		this.send(message, timeToLifeInSeconds, users, MessageSeverity.WARNING);
	}

	/////
	public void sendError(final String message) throws DotDataException {

		this.sendError(message, 5);
	}

	public void sendError(final String message, final int timeToLifeInSeconds) throws DotDataException {

		final User  currentUser           = this.user;
		final List<String> users          = Arrays.asList(currentUser.getUserId());
		this.sendError(message, timeToLifeInSeconds, users);
	}

	public void sendError(final String message, final int timeToLifeInSeconds,
							final List<String> users) throws DotDataException {

		this.send(message, timeToLifeInSeconds, users, MessageSeverity.ERROR);
	}

	public void send(final String message, final int timeToLifeInSeconds,
						 final List<String> users, final MessageSeverity severity) throws DotDataException {

		final long lifeMillis      		  = TimeUnit.MILLISECONDS.convert(timeToLifeInSeconds, TimeUnit.SECONDS);
		final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
		final SystemMessage systemMessage = systemMessageBuilder.setMessage(message)
				.setLife(lifeMillis)
				.setSeverity(severity).create();

		this.systemMessageEventUtil.pushMessage(systemMessage, users);
	}

}
