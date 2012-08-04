package com.dotmarketing.portlets.templates.design.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.design.bean.PreviewFileAsset;
import com.dotmarketing.portlets.templates.design.util.DesignTemplateUtil;
import com.dotmarketing.portlets.templates.design.util.PreviewTemplateUtil;

/**
 * 
 * Servlet implementation class PreviewDesignTemplate
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * May 7, 2012 - 12:22:31 PM
 */
public class PreviewDesignTemplateServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {				
		try {
			String bodyTemplate = request.getParameter("bodyTemplateHTML");			
			// get the imported files
			List<PreviewFileAsset> savedFiles = new ArrayList<PreviewFileAsset>();

			// get the preview body with the imported files (js and/or css)
			StringBuffer endBody = DesignTemplateUtil.getPreviewBody(bodyTemplate,savedFiles);
			
			// get the container's list
			List<Container> containers = PreviewTemplateUtil.getContainers(endBody);			
			
			//replace the text
			for(Container c : containers){
				String identifier = c.getIdentifier();
				// if the container isn't into the header or into the footer than we insert the mock content...
				if(c.getMaxContentlets()>0)
					endBody = new StringBuffer(endBody.toString().replace("#parseContainer('"+identifier+"')", PreviewTemplateUtil.getMockBodyContent()));
				else //...else the container's code
					endBody = new StringBuffer(endBody.toString().replace("#parseContainer('"+identifier+"')", c.getCode()));
			}
			PrintWriter out = response.getWriter();
			out.print(endBody);			
		} catch (DotDataException e) {
			e.printStackTrace();
		} catch (DotSecurityException e) {
			e.printStackTrace();
		}		
	}
	
}
