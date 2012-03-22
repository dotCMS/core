package com.dotmarketing.velocity.events;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.velocity.DotResourceLoader;

public class MethodExceptionEventHandlerImpl implements org.apache.velocity.app.event.MethodExceptionEventHandler {

	String errorTemplate = "/static/preview_mode/error_template.vtl";

	public Object methodException(Class clazz, String message, Exception e) throws Exception {

		if (!isEditMode(e)) {
			Logger.error(clazz, message);
			return null;
		}

		String x = e.toString();
		if (e instanceof ParseErrorException) {
			x = getParseErrorMessage((ParseErrorException) e);
		}
		else if(e instanceof org.apache.lucene.queryParser.ParseException){
			x = getLuceneParseErrorMessage((org.apache.lucene.queryParser.ParseException) e);
		}
		else {
			x = getErrorMessage( e);
		}
		
		return x;
	}
	
	
	public String getErrorMessage(Exception e) {
		java.io.StringWriter sw = new StringWriter();
		VelocityEngine ve = VelocityUtil.getEngine();
		VelocityContext context = new VelocityContext();

		context.put("veloError", e);

		context.put("prettyError", UtilMethods.htmlifyString(e.toString()));
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
		
	private String getLuceneParseErrorMessage(org.apache.lucene.queryParser.ParseException pee) {
		String msg = pee.toString();
		msg = msg.replaceAll("org.apache.lucene.queryParser.ParseException", "Lucene Parse Error\n");
		java.io.StringWriter sw = new StringWriter();
		VelocityEngine ve = VelocityUtil.getEngine();
		VelocityContext context = new VelocityContext();

		context.put("veloError", UtilMethods.htmlifyString(msg));

		//context.put("prettyError", UtilMethods.htmlifyString(msg));
		org.apache.velocity.Template template;
		try {
			template = ve.getTemplate(errorTemplate);
			template.merge(context, sw);
		} catch (Exception ex) {
			Logger.error(this.getClass(), "Unable to show velocityError", ex);
		}
		return sw.toString();
	}
	
	private String getParseErrorMessage(ParseErrorException pee) {
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

	boolean isEditMode(Exception e) {

		boolean ret = false;

		for (StackTraceElement ste : e.getStackTrace()) {
			if (ste.getMethodName().indexOf("EditMode") > -1) {
				
				ret = true;
				break;
			}
		}

		return ret;

	}
	


}
