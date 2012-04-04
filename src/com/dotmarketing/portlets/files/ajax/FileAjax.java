package com.dotmarketing.portlets.files.ajax;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.servlets.AjaxFileUploadListener;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class FileAjax {

	protected FileAPI fileAPI;
	protected UserWebAPI userAPI;

	public FileAjax() {
		fileAPI = APILocator.getFileAPI();
		userAPI = WebAPILocator.getUserWebAPI();
	}

	public Map<String, Object> getWorkingTextFile(String fileInode) throws DotDataException, DotSecurityException,
			PortalException, SystemException, IOException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userAPI.isLoggedToFrontend(req);

		File file = fileAPI.get(fileInode, user, respectFrontendRoles);
		Map<String, Object> map = file.getMap();

		java.io.File fileIO = fileAPI.getAssetIOFile(file);
		FileInputStream fios = new FileInputStream(fileIO);
		byte[] data = new byte[fios.available()];
		fios.read(data);
		String text = new String(data);

		map.put("text", text);
		
		return map;

	}

	public void saveFileText(String fileIdentifier, String newText) throws PortalException, SystemException,
			DotDataException, DotSecurityException, IOException {
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userAPI.getLoggedInUser(req);
		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		boolean respectFrontendRoles = userAPI.isLoggedToFrontend(req);
		File file = fileAPI.getWorkingFileById(fileIdentifier, user, respectFrontendRoles);
		java.io.File fileData = new java.io.File(APILocator.getFileAPI().getAssetIOFile(file).getPath()+"_text");
		fileData.deleteOnExit();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileData);
			fos.write(newText.getBytes());
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	public Map<String, Object> getFileUploadStatus(String fieldName) {
		WebContext ctx = WebContextFactory.get();
		AjaxFileUploadListener.FileUploadStats fileUploadStats = 
			(AjaxFileUploadListener.FileUploadStats) ctx.getSession().getAttribute("FILE_UPLOAD_STATS_" + fieldName);
		if (fileUploadStats != null) {
			long bytesProcessed = fileUploadStats.getBytesRead();
			long sizeTotal = fileUploadStats.getTotalSize();
			long percentComplete = (long) Math
					.floor(((double) bytesProcessed / (double) sizeTotal) * 100.0);
			long timeInSeconds = fileUploadStats.getElapsedTimeInSeconds();
			double uploadRate = bytesProcessed / (timeInSeconds + 0.00001);
			double estimatedRuntime = sizeTotal / (uploadRate + 0.00001);
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("bytesProcessed", bytesProcessed);
			result.put("sizeTotal", sizeTotal);
			result.put("percentComplete", percentComplete);
			result.put("timeInSeconds", timeInSeconds);
			result.put("uploadRate", uploadRate);
			result.put("estimatedRuntime", estimatedRuntime);
			return result;
		}
		return null;
	}
	
	public void clearFileUploadStatus(String fieldName) {
		WebContext ctx = WebContextFactory.get();
		ctx.getSession().removeAttribute("FILE_UPLOAD_STATS_" + fieldName);
	}
	
}
