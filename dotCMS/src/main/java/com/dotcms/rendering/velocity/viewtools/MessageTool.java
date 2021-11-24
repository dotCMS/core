package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotmarketing.exception.DotDataException;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * This class allows on velocity to send message by the web socker plaform
 * @author jsanca
 *
 */
public class MessageTool implements ViewTool {

	protected final MessageDelegate messageDelegate;
	private User user;

	public MessageTool() {
		this((final SystemMessage message, final List<String> users)->
				SystemMessageEventUtil.getInstance().pushMessage(message, users));
	}

	@VisibleForTesting
	protected MessageTool(final MessageDelegate messageDelegate) {

		this.messageDelegate = messageDelegate;
	}


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

		this.messageDelegate.pushMessage(systemMessage, users);
	}

	@FunctionalInterface
	interface MessageDelegate {

		/**
		 * Simple method used to push the message
		 * @param message
		 * @param users
		 */
		void pushMessage (final SystemMessage message, final List<String> users);
	}

}
