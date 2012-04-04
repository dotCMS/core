package com.dotmarketing.rest;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This class acts like an invoker for classes that extend
 * AjaxAction. It is intended to allow developers a quick, safe and
 * easy way to write AJAX servlets in dotCMS without having to wire web.xml
 * 
 * @author will
 * 
 */
public class RestDirectorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) throws ServletException {

	}

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {
			String clazz = request.getRequestURI().split("/")[1];
			clazz = RestDirectorServlet.class.getPackage().getName() + "." + UtilMethods.capitalize(clazz) + "Action";
			RestAction rest = (RestAction) Class.forName(clazz).newInstance();
			if (!(rest instanceof RestAction)) {
				throw new ServletException("Class must implement RestAction");
			}
			
			
			rest.init(request, response);
			if("POST".equals(request.getMethod())){
				rest.doPost(request, response);
			}
			else if("GET".equals(request.getMethod())){
				rest.doGet(request, response);
			}
			else if("PUT".equals(request.getMethod())){
				rest.doPut(request, response);
			}
			else if("DELETE".equals(request.getMethod())){
				rest.doDelete(request, response);
			}
			else{
				rest.service(request, response);
			}
			return;
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			response.sendError(500, e.getMessage());

		}

	}

}
