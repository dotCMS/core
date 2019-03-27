package com.dotmarketing.servlets.ajax;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Logger;

/**
 * This class acts like an invoker for classes that extend
 * AjaxAction. It is intended to allow developers a quick, safe and
 * easy way to write AJAX servlets in dotCMS without having to wire web.xml
 * 
 * @author will
 * 
 */
public class AjaxDirectorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) throws ServletException {

	}

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {
			String clazz = request.getRequestURI().split("/")[2];

			AjaxAction aj = (AjaxAction) Class.forName(clazz).newInstance();
			if (!(aj instanceof AjaxAction)) {
				throw new ServletException("Class must implement AjaxServletInterface");
			}
			
			
			aj.init(request, response);
			if("POST".equals(request.getMethod())){
				aj.doPost(request, response);
			}
			else if("GET".equals(request.getMethod())){
				aj.doGet(request, response);
			}
			else if("PUT".equals(request.getMethod())){
				aj.doPut(request, response);
			}
			else if("DELETE".equals(request.getMethod())){
				aj.doDelete(request, response);
			}
			else{
				aj.service(request, response);
			}
			return;
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			response.sendError(500, e.getMessage());

		}

	}

}
