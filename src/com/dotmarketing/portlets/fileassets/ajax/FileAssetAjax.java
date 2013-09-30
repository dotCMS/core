package com.dotmarketing.portlets.fileassets.ajax;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class FileAssetAjax {
	
	protected FileAPI fileAPI;
	protected UserWebAPI userAPI;

	public FileAssetAjax() {
		fileAPI = APILocator.getFileAPI();
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
		
		java.io.File fileData =  new java.io.File(APILocator.getFileAPI().getRealAssetPath() + java.io.File.separator + contentletInode.charAt(0)
					+ java.io.File.separator + contentletInode.charAt(1) + java.io.File.separator + contentletInode
					+ java.io.File.separator + APILocator.getFileAssetAPI().BINARY_FIELD + java.io.File.separator + WebKeys.TEMP_FILE_PREFIX + fa.getFileAsset().getName());
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
	
	public void removeTempFile(String contInode)throws PortalException, SystemException,
	DotDataException, DotSecurityException, IOException{
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userAPI.isLoggedToFrontend(req);
		Contentlet cont  = APILocator.getContentletAPI().find(contInode, user, respectFrontendRoles);
		FileAsset fa = APILocator.getFileAssetAPI().fromContentlet(cont);
		java.io.File tempFile = new java.io.File(APILocator.getFileAPI().getRealAssetPath() + java.io.File.separator + contInode.charAt(0)
				+ java.io.File.separator + contInode.charAt(1) + java.io.File.separator + contInode
				+ java.io.File.separator + APILocator.getFileAssetAPI().BINARY_FIELD + java.io.File.separator + WebKeys.TEMP_FILE_PREFIX + fa.getFileAsset().getName());
		if(tempFile.exists()){
			tempFile.delete();
		}
	}

}
