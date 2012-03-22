package com.dotmarketing.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.PropertyConfigurator;
import org.springframework.core.io.ClassPathResource;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class Log4JInitializationServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1700686919872123657L;

	public void init() {
	    String file = getInitParameter("log4j-init-file");
	    String path = null;
		try {
			path = new ClassPathResource("log4j.xml").getFile().getPath();
		} catch (IOException e) {
			Logger.error(Log4JInitializationServlet.class,e.getMessage(),e);
		}
	    
	    // if the log4j-init-file is not set, then no point in trying
	    if(file != null && UtilMethods.isSet(path)) {
	      PropertyConfigurator.configure(path);
	    }
	  }

	  public void doGet(HttpServletRequest req, HttpServletResponse res) {
	  }
}
