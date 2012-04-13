package com.dotmarketing.portlets.osgi.AJAX;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Config;

public class OSGIAJAX extends OSGIBaseAJAX {

	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
	
	public void undeploy(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String jar = request.getParameter("jar");
		File from = new File(Config.CONTEXT.getRealPath("/WEB-INF/felix/load/" + jar));
		File to = new File(Config.CONTEXT.getRealPath("/WEB-INF/felix/undeployed" + jar));
		from.renameTo(to);
		
	}

}
