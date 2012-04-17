package com.dotmarketing.portlets.osgi.AJAX;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class OSGIAJAX extends OSGIBaseAJAX {

	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
	
	public void undeploy(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String jar = request.getParameter("jar");
		File from = new File(Config.CONTEXT.getRealPath("/WEB-INF/felix/load/" + jar));
		File to = new File(Config.CONTEXT.getRealPath("/WEB-INF/felix/undeployed/" + jar));
		from.renameTo(to);
		writeSuccess(response, "OSGI Bundle Undeployed");
	}

	public void deploy(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String jar = request.getParameter("jar");
		File from = new File(Config.CONTEXT.getRealPath("/WEB-INF/felix/undeployed/" + jar));
		File to = new File(Config.CONTEXT.getRealPath("/WEB-INF/felix/load/" + jar));
		from.renameTo(to);
		writeSuccess(response, "OSGI Bundle Loaded");
	}
	
	public void add(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        try {
			List<FileItem> items = (List<FileItem>) upload.parseRequest(request);
		} catch (FileUploadException e) {
			Logger.error(OSGIAJAX.class,e.getMessage(),e);
		}
	}
}
