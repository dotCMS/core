package com.dotmarketing.servlets;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;

public class FileServlet extends HttpServlet {

	// the number of cached objects

	private static final long serialVersionUID = 1L;

	protected void service(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		if (Config.CONTEXT == null) {
			response.sendError(404, "Asset not Found");
			return;
		}

		boolean preview = (request.getAttribute(WebKeys.PREVIEW_MODE_COOKIE) != null && request.getAttribute(WebKeys.ADMIN_MODE_COOKIE) != null);

		//try to get the file from the cache using the URI
		com.dotmarketing.portlets.files.model.File _file = null ; //FileFactory.getCachedFile(request.getRequestURI(), live);

		if (_file != null && (InodeUtils.isSet(_file.getInode()))) {
			response.setContentType(_file.getMimeType());
			ServletOutputStream out = response.getOutputStream();

			try {
				FileInputStream fis = new FileInputStream(APILocator.getFileAPI().getRealAssetPath(_file));
				BufferedInputStream bis = new BufferedInputStream(fis);
				byte[] buf = new byte[1024];
				int i = 0;
				while ((i = bis.read(buf)) != -1) {
					out.write(buf, 0, i);
				}
				bis.close();
				fis.close();
				out.close();
			}
			catch (Exception e) {
    		    Logger.warn(this, e.toString(), e);
			}
		}
		else {
			if (!preview) {
				response.sendError(404, "Live Asset not Found");
			}
			else {
				response.sendError(404, "Working Asset not Found");
			}
		}
		return;
	}

}
