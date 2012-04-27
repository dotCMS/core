/**
 * 
 */
package com.dotmarketing.viewtools;

import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.velocity.VelocityServlet;

/**
 * @author Jason Tesser
 * @since 1.6.5
 *
 */
public class VelocityWebUtil implements ViewTool {

	static final String _logVariable = "LOG_VELOCITY_TEMPLATES";
	private Context ctx;
	private HttpServletRequest req;
	boolean debug =false;
	
	// private HttpServletRequest request;
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.req = context.getRequest();
		this.ctx = context.getVelocityContext();
		this.debug = Config.getBooleanProperty(_logVariable, false);
	
		if(!this.debug){
			try{
				if(ctx.get("request") != null){
					if(((HttpServletRequest) ctx.get("request")).getParameter(_logVariable) != null || 
							req.getSession().getAttribute(_logVariable) != null 
					){
						this.debug = true;
						
						
						if("false".equals(req.getParameter(_logVariable))){
							this.debug = true;
							req.getSession().removeAttribute(_logVariable);
						}else{
							req.getSession().setAttribute(_logVariable, "true");
						}
						
						
						
					}
				}
			}
			catch(Exception e){
				Logger.debug(VelocityWebUtil.class,e.getMessage(),e);
			}
		}
		
	}

	public String mergeTemplate(String templatePath) throws ResourceNotFoundException, ParseErrorException, Exception{
		VelocityServlet.velocityCtx.set(ctx);
		VelocityEngine ve = VelocityUtil.getEngine();
		Template template = null;
		StringWriter sw = new StringWriter();
		
		if(this.debug){
			Logger.info(VelocityWebUtil.class, _logVariable + ": " + templatePath);			
		}
		template = ve.getTemplate(templatePath);
		
		template.merge(ctx, sw);

		return sw.toString();
		
	}
	
	
	public void mergeTemplate(String templatePath, HttpServletResponse response) throws ResourceNotFoundException, ParseErrorException, Exception{
		VelocityServlet.velocityCtx.set(ctx);
		VelocityEngine ve = VelocityUtil.getEngine();
		Template template = null;
		
		if(this.debug){
			Logger.info(VelocityWebUtil.class, _logVariable + ": " + templatePath);			
		}
		
		template = ve.getTemplate(templatePath);
		template.merge(ctx, response.getWriter());

	}
	
	
	
	
	
	public boolean doesVelocityResourceExist(String templatePath){
		VelocityEngine ve = VelocityUtil.getEngine();
		try {
			ve.getTemplate(templatePath);
		} catch (ResourceNotFoundException e) {
			return false;
		}catch (Exception e) {
			Logger.debug(VelocityWebUtil.class,e.getMessage(),e);
		}
		return true;
	}
	
	public boolean isVelocityFile(String path){
		if(!UtilMethods.isSet(path)){
			return false;
		}
		path = path.toLowerCase();
		return (path.endsWith(".vtl") 
			||		path.endsWith(".vm") 
			||		path.endsWith(".html") 
			||		path.endsWith(".htm") 
		);
	}
}
