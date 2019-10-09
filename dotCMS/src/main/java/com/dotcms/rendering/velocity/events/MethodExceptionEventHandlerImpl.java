package com.dotcms.rendering.velocity.events;

import static com.dotcms.exception.ExceptionUtil.exceptionAsString;
import static com.liferay.util.StringPool.BLANK;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rendering.velocity.viewtools.exception.DotToolException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.util.Html;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.exception.ParseErrorException;


public class MethodExceptionEventHandlerImpl implements org.apache.velocity.app.event.MethodExceptionEventHandler {

	private static final String NEW_LINE = Html.br() + Html.space(2);
	private static final int EXCEPTION_MAX_LINES = 2;

	public Object methodException(final Class clazz, final String message, final Exception e) throws Exception {

		if(e instanceof DotToolException) {
			throw e;
		}

		if (e instanceof PreviewEditParseErrorException) {
			// this exception is already handled by System Messages.
			throw e;
		}

		if (e instanceof DotStateException) {
			// this exception is already handled by System Messages.
			this.notifyStateException(e);
			throw e;
		}

		if (!isEditMode(e)) {
			Logger.velocityWarn(clazz, message + " " + e.getMessage());
			Logger.velocityDebug(clazz, e.getMessage(),e);
			return null;
		}

		String x = e.toString();
		if (e instanceof ParseErrorException) {
			x = ExceptionHandlerParseErrorException.handleParseErrorException((ParseErrorException)e);
		} else if(e instanceof org.apache.lucene.queryparser.classic.ParseException){
			x = handleParseErrorException((org.apache.lucene.queryparser.classic.ParseException)e);
		} else {
			x= this.notifyStateException(e);
		}
		return x;
	}

	private String notifyStateException(final Exception exception) {
		String messageAsString = BLANK;
		final String userId = PrincipalThreadLocal.getName();
		if (UtilMethods.isSet(userId)) {

			final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
			final StringBuilder message = new StringBuilder();

			message.append(Html.h3("Velocity Error")).append(NEW_LINE)
					.append(Html.pre(exceptionAsString(exception, EXCEPTION_MAX_LINES)));

			messageAsString = message.toString();
			systemMessageBuilder.setMessage(messageAsString)
					.setLife(DateUtil.FIVE_SECOND_MILLIS)
					.setType(MessageType.SIMPLE_MESSAGE)
					.setSeverity(MessageSeverity.ERROR);

			SystemMessageEventUtil.getInstance().
					pushMessage(exception.getMessage(), systemMessageBuilder.create(),
							Collections.singletonList(userId));
		}
		return messageAsString;
	}

	private String handleParseErrorException(final org.apache.lucene.queryparser.classic.ParseException exception){
		final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
		final StringBuilder message = new StringBuilder();
		final String errorMessage = WordUtils.wrap
				( UtilMethods.isSet(exception.getMessage()) ? exception.getMessage() : exception.toString()  , 15, Html.br(), false);

		if(null != exception.currentToken) {
			message.append(Html.h3("Lucene Query Parsing Error"))
					.append(Html.b("Current Token")).append(NEW_LINE)
					.append(exception.currentToken).append(Html.br())
					.append(Html.pre(errorMessage));
		} else {
			message.append(Html.h3("Lucene Query Parsing Error."))
			       .append(Html.pre(errorMessage));
		}
		final String messageAsString = message.toString();
		systemMessageBuilder.setMessage(messageAsString)
				.setLife(DateUtil.FIVE_SECOND_MILLIS)
				.setType(MessageType.SIMPLE_MESSAGE)
				.setSeverity(MessageSeverity.ERROR);

		final String userId = PrincipalThreadLocal.getName();
		if (UtilMethods.isSet(userId)) {
			SystemMessageEventUtil.getInstance().
					pushMessage("",
							systemMessageBuilder.create(), Collections.singletonList(userId));
		}
		return messageAsString;
	}

	private boolean isEditMode(final Exception exception) {

		boolean ret = false;

		final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

		if (null != request) {

			final PageMode pageMode = PageMode.get(request);
			ret = PageMode.EDIT_MODE == pageMode;
		} else {
			for (StackTraceElement ste : exception.getStackTrace()) {
				if (ste.getMethodName().contains("EditMode")) {
					ret = true;
					break;
				}
			}
		}

		return ret;
	}
}
