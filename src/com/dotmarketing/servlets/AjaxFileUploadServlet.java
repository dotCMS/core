package com.dotmarketing.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.missiondata.fileupload.MonitoredDiskFileItemFactory;

public class AjaxFileUploadServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	boolean isEmptyFile ;
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession();

		if("get".equals(request.getParameter("cmd"))) {
			doFileRetrieve(session, request, response);
		} else {
			doFileUpload(session, request, response);
		}
	}

	private void doFileRetrieve(HttpSession session, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		
		String userId = session.getAttribute("USER_ID").toString();
		String fieldName = request.getParameter("fieldName");
		String fileName = request.getParameter("fileName");

		File tempUserFolder = new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + userId + File.separator + fieldName);
		File file = new File(tempUserFolder.getAbsolutePath() + File.separator + fileName);
		if(file.exists()) {
			FileInputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[1000];
			int count = 0;
			String mimeType = this.getServletContext().getMimeType(file.getName());
			response.setContentType(mimeType);
			ServletOutputStream outStream = response.getOutputStream();
			while((count = fis.read(buffer)) > 0) {
				outStream.write(buffer, 0, count);
			}
			outStream.flush();
			outStream.close();
		}

	}
	
	private void doFileUpload(HttpSession session, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String fieldName =null;
		AjaxFileUploadListener listener = null;
		try {
			
			String fileName = "";

			listener = new AjaxFileUploadListener(request.getContentLength());
			FileItemFactory factory = new MonitoredDiskFileItemFactory(listener);
			fieldName = request.getParameter("fieldName");
			Enumeration params = request.getParameterNames(); 
			session.setAttribute("FILE_UPLOAD_STATS_" + fieldName, listener.getFileUploadStats());
			ServletFileUpload upload = new ServletFileUpload(factory);
			
			List items = upload.parseRequest(request);
			boolean hasError = false;
			isEmptyFile = false;
			
			
			String userId = session.getAttribute("USER_ID").toString();
			
			for (Iterator i = items.iterator(); i.hasNext();) {
				FileItem fileItem = (FileItem) i.next();

				if (!fileItem.isFormField()) {
					

					// *************************************************
					// This is where you would process the uploaded file
					// *************************************************
					
					if(fileItem.getSize() == 0)
						isEmptyFile = true;

					if (fileItem.getName().contains(File.separator)) {
						fileName = fileItem.getName().substring(
								fileItem.getName().lastIndexOf(File.separator) + 1);
					} else {
						fileName = fileItem.getName();
					}
					fileName = ContentletUtil.sanitizeFileName(fileName);

					File tempUserFolder = new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + userId + 
							File.separator + fieldName);
					if (!tempUserFolder.exists())
						tempUserFolder.mkdirs();
					
					fileItem.write(new File(tempUserFolder.getAbsolutePath() + File.separator + fileName));
					fileItem.delete();
				}
			}
			
			if(isEmptyFile)
				fileName = "";

			if (!hasError) {
				sendCompleteResponse(response, null);
			} else {
				sendCompleteResponse(response, "Could not process uploaded file. Please see log for details.");
			}
		} catch (Exception e) {
			listener.error("error");
			session.setAttribute("FILE_UPLOAD_STATS_" + fieldName, listener.getFileUploadStats());
			sendCompleteResponse(response, e.getMessage());
			e.printStackTrace();
		}
	}

	private void sendCompleteResponse(HttpServletResponse response, String errorMessage) throws IOException {
		if (errorMessage == null) {			
			response.getOutputStream().print("");
		} else {
			response.getOutputStream().print("");
		}
	}
	
}
