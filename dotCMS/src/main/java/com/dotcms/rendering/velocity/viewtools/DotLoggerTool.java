package com.dotcms.rendering.velocity.viewtools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;


/**
 * Simple viewtool to log messages to our standard logger infrastructure.  Will output the template name to make debugging easier.
 * @author andres
 *
 */
public class DotLoggerTool implements ViewTool {

	private InternalContextAdapterImpl ica;
	
	private static Logger logger = LogManager.getLogger(DotLoggerTool.class);
	
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		Context ctx=context.getVelocityContext();
		ica = new InternalContextAdapterImpl(ctx);
	}

	public void info(String s) {
		if (s!=null) {
			logger.info(ica.getCurrentTemplateName()+": " +s);
		}
	}
	
	public void error(String s) {
		if (s!=null) {
			logger.error(ica.getCurrentTemplateName()+": " +s);
		}
	}
	
	public void debug(String s) {
		if (s!=null) {
			logger.debug(ica.getCurrentTemplateName()+": " +s);
		}
	}
	
	public void warn(String s) {
		if (s!=null) {
			logger.warn(ica.getCurrentTemplateName()+": " +s);
		}
	}
	
	/**
	 * Outputs the stack trace of the templates to the info logger
	 */
	public void printTemplateStack() {
		Object[] stack=ica.getTemplateNameStack();

		int size=stack.length;
		for (int i=(size-1);i>=0;i--) {
			//We iterate backward to print things a bit nicer
			int count=(size-i);
			logger.info("[" + count+"/"+size+"] " + stack[i].toString());
		
		}
	}
}
