package com.dotmarketing.servlets;

import com.dotcms.repackage.com.missiondata.fileupload.MonitoredDiskFileItemFactory;
import com.dotcms.repackage.org.apache.commons.fileupload.FileItem;
import com.dotcms.repackage.org.apache.commons.fileupload.FileItemFactory;
import com.dotcms.repackage.org.apache.commons.fileupload.servlet.ServletFileUpload;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.dotcms.repackage.org.apache.commons.io.FilenameUtils;

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

        ServletOutputStream outStream = null;
	    try {

			String userId = null;
			// if we want front end access, this validation would need to be altered
			if(UtilMethods.isSet(session.getAttribute("USER_ID"))) {
				userId = (String) session.getAttribute("USER_ID");
				User user = UserLocalManagerUtil.getUserById(userId);

				if(!UtilMethods.isSet(user) || !UtilMethods.isSet(user.getUserId())) {
					throw new Exception("Could not download File. Invalid User");
				}

			} else {
				throw new Exception("Could not download File. Invalid User");
			}

			String fieldName = request.getParameter("fieldName");
			String fileName = request.getParameter("fileName");

			File tempUserFolder = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + File.separator + userId + File.separator + fieldName);

			File file = new File(tempUserFolder.getAbsolutePath() + File.separator + fileName);

			if(!isValidPath(file.getCanonicalPath())) {
				throw new Exception("Invalid fileName or Path");
			}

			if(file.exists()) {
				final InputStream is = Files.newInputStream(file.toPath());
				byte[] buffer = new byte[1000];
				int count = 0;
				String mimeType = this.getServletContext().getMimeType(file.getName());
				response.setContentType(mimeType);
				outStream = response.getOutputStream();
				while((count = is.read(buffer)) > 0) {
					outStream.write(buffer, 0, count);
				}
				outStream.flush();
			}

		} catch (Exception e) {
			sendCompleteResponse(response, e.getMessage());
			e.printStackTrace();
		} finally {
            CloseUtils.closeQuietly(outStream);
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

			String userId = null;
			// if we want front end access, this validation would need to be altered
			if(UtilMethods.isSet(session.getAttribute("USER_ID"))) {
				userId = (String) session.getAttribute("USER_ID");
				User user = UserLocalManagerUtil.getUserById(userId);

				if(!UtilMethods.isSet(user) || !UtilMethods.isSet(user.getUserId())) {
					throw new Exception("Could not upload File. Invalid User");
				}

			} else {
				throw new Exception("Could not upload File. Invalid User");
			}


			for (Iterator i = items.iterator(); i.hasNext();) {
				FileItem fileItem = (FileItem) i.next();

				if (!fileItem.isFormField()) {


					// *************************************************
					// This is where you would process the uploaded file
					// *************************************************

					if(fileItem.getSize() == 0)
						isEmptyFile = true;

					fileName = fileItem.getName();

					fileName = FilenameUtils.separatorsToSystem(fileName);

					if (fileName.contains(File.separator)) {
						fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
					}
					fileName = ContentletUtil.sanitizeFileName(fileName);
					fieldName = ContentletUtil.sanitizeFileName(fieldName);
					File tempUserFolder = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + File.separator + userId +
							File.separator + fieldName);

					if(!isValidPath(tempUserFolder.getCanonicalPath())) {
						throw new IOException("Invalid fileName or Path");
					}

					if (!tempUserFolder.exists())
						tempUserFolder.mkdirs();
					File dest=new File(tempUserFolder.getAbsolutePath() + File.separator + fileName);
					if(dest.exists())
						dest.delete();
					fileItem.write(dest);
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
			response.getOutputStream().print(errorMessage);
		}
	}

	private static boolean isValidPath(String path) throws IOException {
		Path child = Paths.get(path).toAbsolutePath();
		String tempBinaryPath = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()).getCanonicalPath();
		Path parent = Paths.get(tempBinaryPath).toAbsolutePath();

		return child.startsWith(parent);
	}

}
