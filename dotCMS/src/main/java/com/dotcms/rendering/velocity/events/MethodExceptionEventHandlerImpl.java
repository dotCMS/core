package com.dotcms.rendering.velocity.events;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rendering.velocity.services.DotResourceLoader;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.exception.DotToolException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.util.Html;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.*;


public class MethodExceptionEventHandlerImpl implements org.apache.velocity.app.event.MethodExceptionEventHandler {

	private static final String NEW_LINE = Html.br() + Html.space(2);
	String errorTemplate = "/static/preview_mode/error_template.vtl";

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
			this.notifyStateException(DotStateException.class.cast(e));
			throw e;
		}

		if (!isEditMode(e)) {
			Logger.velocityWarn(clazz, message + " " + e.getMessage());
			Logger.velocityDebug(clazz, e.getMessage(),e);
			return null;
		}

		String x = e.toString();
		if (e instanceof ParseErrorException) {
			x = getParseErrorMessage((ParseErrorException) e);
		} else if(e instanceof org.apache.lucene.queryparser.classic.ParseException){
			x = getLuceneParseErrorMessage((org.apache.lucene.queryparser.classic.ParseException) e);
		} else {
			x = getErrorMessage( e);
		}
		
		return x;
	}

	private void notifyStateException(final DotStateException e) {

		final String userId = PrincipalThreadLocal.getName();
		if (UtilMethods.isSet(userId)) {

			final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
			final StringBuilder        message 			    = new StringBuilder();

			message.append(Html.h3("Velocity Error")).append(NEW_LINE)
					.append(Html.pre(getErrorMessage(e)));

			systemMessageBuilder.setMessage(message.toString())
					.setLife(DateUtil.FIVE_SECOND_MILLIS)
					.setType(MessageType.SIMPLE_MESSAGE)
					.setSeverity(MessageSeverity.ERROR);

			SystemMessageEventUtil.getInstance().
					pushMessage(e.getMessage(), systemMessageBuilder.create(), Arrays.asList(userId));
		}
	}


	public String getErrorMessage(final Exception e) {

		final StringWriter    sw      = new StringWriter();
		final VelocityEngine  ve      = VelocityUtil.getEngine();
		final VelocityContext context = new VelocityContext();

		context.put("veloError", e);

		context.put("prettyError", UtilMethods.htmlifyString(e.toString()));

		final Template template;
		try {
			template = ve.getTemplate(errorTemplate);
			context.put("error", this);
			template.merge(context, sw);
		} catch (Exception ex) {
			Logger.error(this.getClass(), "Unable to show velocityError", ex);
		}

		return sw.toString();
	}
		
	private String getLuceneParseErrorMessage(final org.apache.lucene.queryparser.classic.ParseException pee) {

		String msg = pee.toString();
		msg = msg.replaceAll("org.apache.lucene.queryparser.classic.ParseException", "Lucene Parse Error\n");
		final StringWriter    sw      = new StringWriter();
		final VelocityEngine  ve      = VelocityUtil.getEngine();
		final VelocityContext context = new VelocityContext();

		context.put("veloError", UtilMethods.htmlifyString(msg));
		final Template template;

		try {
			template = ve.getTemplate(errorTemplate);
			template.merge(context, sw);
		} catch (Exception ex) {
			Logger.error(this.getClass(), "Unable to show velocityError", ex);
		}

		return sw.toString();
	}
	
	private String getParseErrorMessage(final ParseErrorException pee) {

		String msg = pee.toString().replaceAll("at " + pee.getTemplateName(), "");
		msg = msg.replaceAll("org.apache.velocity.exception.ParseErrorException:", "");
								msg = msg.replaceAll("\\.\\.\\.", ",");
		if(pee.getLineNumber() > -1){
			msg = msg.replaceAll("\\[.*\\]","");
		}
		else{
			msg = msg.replaceAll("\\[", "\n\\[");
		}

		msg = UtilMethods.replace(msg, "\"<EOF>\"", "end of file");
		msg = UtilMethods.htmlifyString(msg);
		
		msg = UtilMethods.htmlLineBreak(msg);
		while(msg.endsWith(",")){
			msg = msg.substring(0,msg.length()-1);
		}

		int showLines = 2;
		List<Map<String, String>> badCode = new ArrayList<Map<String, String>>();
		if (pee.getLineNumber() > -1) {
			BufferedReader buff = null;
			try {
				buff = new BufferedReader(new InputStreamReader(DotResourceLoader.getInstance().getResourceStream(
						pee.getTemplateName())));

				String x;
				int here = 1;
				while ((x = buff.readLine()) != null) {
					Map m = new HashMap<String, String>();
					if (pee.getLineNumber() == here) {
						m.put("code", UtilMethods.htmlifyString(x));
						m.put("lineNumber", here);
						m.put("badLine", "true");
						badCode.add(m);
					} else if (here >= pee.getLineNumber() - showLines && here <= pee.getLineNumber() + showLines) {
						if (UtilMethods.isSet(x)) {
							m.put("code", UtilMethods.htmlifyString(x));
							m.put("lineNumber", here);
							badCode.add(m);
						} else {
							m.put("code", "&nbsp;");
							m.put("lineNumber", here);
							badCode.add(m);
						}

					} else if (here - showLines > pee.getLineNumber()) {
						break;
					}

					here++;
				}

			} catch (IOException e1) {
				Logger.error(this.getClass(), "Unable to open buffy!" + e1);
			} finally {
				try {
					buff.close();
				} catch (Exception die) {
					Logger.error(this.getClass(), "Unable to close buffy!" + die);

				}
			}
			

		}

		java.io.StringWriter sw = new StringWriter();
		VelocityEngine ve = VelocityUtil.getEngine();
		VelocityContext context = new VelocityContext();

		context.put("veloError", pee);
		context.put("subErrorTemplate", "/static/preview_mode/error_template_parseexception.vtl");
		context.put("badCode", badCode);
		context.put("prettyError", msg);
		org.apache.velocity.Template template;
		try {
			template = ve.getTemplate(errorTemplate);
			context.put("error", this);
			template.merge(context, sw);
		} catch (Exception ex) {
			Logger.error(this.getClass(), "Unable to show velocityError", ex);
		}
		return sw.toString();
	}

	boolean isEditMode(final Exception e) {

		boolean ret = false;

		final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

		if (null != request) {

			final PageMode pageMode = PageMode.get(request);
			ret = PageMode.EDIT_MODE == pageMode;
		} else {
			for (StackTraceElement ste : e.getStackTrace()) {
				if (ste.getMethodName().indexOf("EditMode") > -1) {

					ret = true;
					break;
				}
			}
		}

		return ret;
	}
}
