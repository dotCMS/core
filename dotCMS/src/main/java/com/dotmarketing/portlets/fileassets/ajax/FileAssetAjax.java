package com.dotmarketing.portlets.fileassets.ajax;

import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;

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
		try (InputStream fios = Files.newInputStream(fileIO.toPath())){
            byte[] data = new byte[fios.available()];
            fios.read(data);
            String text = new String(data);

            map.put("text", text);

            return map;
		}
	}


	public void saveFileText(final String contentletInode, final String newText, final String binField)
			throws PortalException, SystemException, DotDataException, DotSecurityException, IOException {

		final WebContext webContext        = WebContextFactory.get();
		final HttpServletRequest request   = webContext.getHttpServletRequest();
		final User user 			       = userAPI.getLoggedInUser(request);
		final boolean respectFrontendRoles = userAPI.isLoggedToFrontend(request);
		final Contentlet contentlet        = APILocator.getContentletAPI().find(contentletInode, user, respectFrontendRoles);
		String incomingFileName            = null;
		String binaryFieldName             = binField;

		if (contentlet.isFileAsset()) {

			final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
			incomingFileName = fileAsset.getFileAsset().getName();
			binaryFieldName  = FileAssetAPI.BINARY_FIELD;
		} else if (contentlet.isDotAsset()) {

			incomingFileName = contentlet.getBinary(DotAssetContentType.ASSET_FIELD_VAR).getName();
			binaryFieldName  = DotAssetContentType.ASSET_FIELD_VAR;
		} else {

			incomingFileName = contentlet.getBinary(binField).getName();
		}

		final File tempDir =  new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + File.separator + contentletInode.charAt(0)
				+ File.separator + contentletInode.charAt(1) + File.separator + contentletInode
				+ File.separator + binaryFieldName);

		if(!tempDir.exists()) {
			tempDir.mkdirs();
		}

		final File fileData = new File(tempDir.getAbsoluteFile() + File.separator + WebKeys.TEMP_FILE_PREFIX + incomingFileName);

		fileData.deleteOnExit();

		try (OutputStream os = Files.newOutputStream(fileData.toPath())) {

			os.write(newText.getBytes());
		} catch(Exception e) {

			Logger.error(getClass(), "Error writing to file", e);
		}
	}

}
