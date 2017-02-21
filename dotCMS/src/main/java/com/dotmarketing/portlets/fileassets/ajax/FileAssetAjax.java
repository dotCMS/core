package com.dotmarketing.portlets.fileassets.ajax;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.servlets.AjaxFileUploadListener;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class FileAssetAjax {

	protected UserWebAPI userAPI;

	public FileAssetAjax() {
		userAPI = WebAPILocator.getUserWebAPI();
	}

	public Map<String, Object> getWorkingTextFile(String contentletInode) throws DotDataException, DotSecurityException,
	PortalException, SystemException, IOException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userAPI.isLoggedToFrontend(req);

		Contentlet cont  = APILocator.getContentletAPI().find(contentletInode, user, respectFrontendRoles);
		FileAsset fa = APILocator.getFileAssetAPI().fromContentlet(cont);
		Map<String, Object> map = fa.getMap();

		java.io.File fileIO = fa.getFileAsset();
		FileInputStream fios = new FileInputStream(fileIO);
		byte[] data = new byte[fios.available()];
		fios.read(data);
		String text = new String(data);

		map.put("text", text);

		return map;

	}


	public void saveFileText(String contentletInode, String newText) throws PortalException, SystemException,
	DotDataException, DotSecurityException, IOException {
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userAPI.isLoggedToFrontend(req);
		Contentlet cont  = APILocator.getContentletAPI().find(contentletInode, user, respectFrontendRoles);
		FileAsset fa = APILocator.getFileAssetAPI().fromContentlet(cont);

		java.io.File tempDir =  new java.io.File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + java.io.File.separator + contentletInode.charAt(0)
					+ java.io.File.separator + contentletInode.charAt(1) + java.io.File.separator + contentletInode
					+ java.io.File.separator + APILocator.getFileAssetAPI().BINARY_FIELD);

		if(!tempDir.exists())
			tempDir.mkdirs();

		java.io.File fileData = new java.io.File(tempDir.getAbsoluteFile() + java.io.File.separator + WebKeys.TEMP_FILE_PREFIX + fa.getFileAsset().getName());

		fileData.deleteOnExit();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileData);
			fos.write(newText.getBytes());
		} catch(Exception e) {
			Logger.error(getClass(), "Error writing to file", e);
		}finally {
			if (fos != null)
				fos.close();
		}
	}

	public Map<String, Object> getFileUploadStatus(String fieldName) {
		WebContext ctx = WebContextFactory.get();
		AjaxFileUploadListener.FileUploadStats fileUploadStats =
			(AjaxFileUploadListener.FileUploadStats) ctx.getSession().getAttribute("FILE_UPLOAD_STATS_" + fieldName);
		Map<String, Object> result = new HashMap<String, Object>();
		if (fileUploadStats != null) {
			if(fileUploadStats.getCurrentStatus().equalsIgnoreCase("error")){
				result.put("error", "Sorry! We Could not process this uploaded file.");
				return result;
			}
			long bytesProcessed = fileUploadStats.getBytesRead();
			long sizeTotal = fileUploadStats.getTotalSize();
			long percentComplete = (long) Math
				.floor(((double) bytesProcessed / (double) sizeTotal) * 100.0);
			long timeInSeconds = fileUploadStats.getElapsedTimeInSeconds();
			double uploadRate = bytesProcessed / (timeInSeconds + 0.00001);
			double estimatedRuntime = sizeTotal / (uploadRate + 0.00001);

			result.put("bytesProcessed", bytesProcessed);
			result.put("sizeTotal", sizeTotal);
			result.put("percentComplete", percentComplete);
			result.put("timeInSeconds", timeInSeconds);
			result.put("uploadRate", uploadRate);
			result.put("estimatedRuntime", estimatedRuntime);
			result.put("error", null);

			// dotcms 3022
			ctx.getSession().setAttribute("SIZE_FILE_UPLOAD_STATS_" + fieldName, sizeTotal);
			return result;
		}
		return null;
	}

	public long clearFileUploadStatus(String fieldName) {
		WebContext ctx = WebContextFactory.get();
		long size = ((AjaxFileUploadListener.FileUploadStats)ctx.getSession().getAttribute("FILE_UPLOAD_STATS_" + fieldName)).getTotalSize();
		ctx.getSession().removeAttribute("FILE_UPLOAD_STATS_" + fieldName);
		return size;
	}

}
