/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.LiferayDiskFileUpload;
import org.apache.commons.fileupload.LiferayFileItem;
import org.apache.commons.fileupload.LiferayFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.dotmarketing.util.Logger;
import com.liferay.util.GetterUtil;
import com.liferay.util.SystemProperties;

/**
 * <a href="UploadServletRequest.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Zongliang Li
 * @version $Revision: 1.18 $
 *
 */
public class UploadServletRequest extends HttpServletRequestWrapper {

	public static final int DEFAULT_SIZE_MAX = GetterUtil.getInteger(
		SystemProperties.get(
			UploadServletRequest.class.getName() + ".max.size"));

	public static final File DEFAULT_TEMP_DIR = new File(
		GetterUtil.get(
			SystemProperties.get(
				UploadServletRequest.class.getName() + ".temp.dir"),
			(String)SystemProperties.get("java.io.tmpdir")));

	public UploadServletRequest(HttpServletRequest req)
		throws IOException {

		super(req);

		_params = new LinkedHashMap();

		try {
			//DiskFileUpload diskFileUpload = new DiskFileUpload(
			//	new LiferayFileItemFactory(DEFAULT_TEMP_DIR));

			ServletFileUpload diskFileUpload = new LiferayDiskFileUpload(
				new LiferayFileItemFactory(DEFAULT_TEMP_DIR), req);

			diskFileUpload.setSizeMax(DEFAULT_SIZE_MAX);

			List list = diskFileUpload.parseRequest(req);

			for (int i = 0; i < list.size(); i++) {
				LiferayFileItem fileItem = (LiferayFileItem)list.get(i);

				if (fileItem.isFormField()) {
					fileItem.setString(req.getCharacterEncoding());
	            }

				LiferayFileItem[] fileItems =
					(LiferayFileItem[])_params.get(fileItem.getFieldName());

				if (fileItems == null) {
					fileItems = new LiferayFileItem[] {fileItem};
				}
				else {
					LiferayFileItem[] newFileItems =
						new LiferayFileItem[fileItems.length + 1];

					System.arraycopy(
						fileItems, 0, newFileItems, 0, fileItems.length);

					newFileItems[newFileItems.length - 1] = fileItem;

					fileItems = newFileItems;
				}

				_params.put(fileItem.getFieldName(), fileItems);
				if(fileItem.getFileName() != null)
					_params.put(fileItem.getFileName(), new LiferayFileItem[] {fileItem});

			}
		}
		catch (FileUploadException fue) {
			Logger.error(this,fue.getMessage(),fue);
		}
	}

	public String getContentType() {

		// Workaround to prevent Struts from processing the multipart content
		// again

		return null;
	}

	public String getContentType(String name) {
		if (getFileName(name) == null) {
			return null;
        }
		LiferayFileItem[] fileItems = (LiferayFileItem[])_params.get(name);

		if ((fileItems != null) && (fileItems.length > 0)) {
			return fileItems[0].getContentType();
		}
		else {
			return null;
		}
	}

	public File getFile(String name) {
		if (getFileName(name) == null) {
			return null;
		}

		LiferayFileItem[] fileItems = (LiferayFileItem[])_params.get(name);

		if ((fileItems != null) && (fileItems.length > 0)) {
			return fileItems[0].getStoreLocation();
		}
		else {
			return null;
		}
	}

	public String getFileName(String name) {
		LiferayFileItem[] fileItems = (LiferayFileItem[])_params.get(name);
        if(fileItems == null || fileItems.length == 0)
			return null;
        else if ((fileItems != null) && (fileItems.length > 0)) {
			return fileItems[0].getFileName();
		}
		else {
			return null;
		}
	}

	public String getFullFileName(String name) {
		LiferayFileItem[] fileItems = (LiferayFileItem[])_params.get(name);

		if ((fileItems != null) && (fileItems.length > 0)) {
			return fileItems[0].getFullFileName();
		}
		else {
			return null;
		}
	}

	public String getParameter(String name) {
		LiferayFileItem[] fileItems = (LiferayFileItem[])_params.get(name);

		if ((fileItems != null) && (fileItems.length > 0)) {
			return fileItems[0].getString();
		}
		else {
			return super.getParameter(name);
		}
	}

	public Map getParameterMap() {
		Map map = new HashMap();

		Enumeration enu = getParameterNames();

		while (enu.hasMoreElements()) {
			String name = (String)enu.nextElement();

			map.put(name, getParameterValues(name));
		}

		return map;
	}

	public Enumeration getParameterNames() {
		List parameterNames = new ArrayList();

		Enumeration enu = super.getParameterNames();

		while (enu.hasMoreElements()) {
			String name = (String)enu.nextElement();

			if (!_params.containsKey(name)) {
				parameterNames.add(name);
			}
		}

		Iterator itr = _params.keySet().iterator();

		while (itr.hasNext()) {
			String name = (String)itr.next();

			parameterNames.add(name);
		}

		return Collections.enumeration(parameterNames);
	}

	public String[] getParameterValues(String name) {
		String[] parentValues = super.getParameterValues(name);

		LiferayFileItem[] fileItems = (LiferayFileItem[])_params.get(name);

		if ((fileItems == null) || (fileItems.length == 0)) {
			return parentValues;
		}
		else if ((parentValues == null) || (parentValues.length == 0)) {
			String[] values = new String[fileItems.length];

			for (int i = 0; i < values.length; i++) {
				values[i] = fileItems[i].getString();
			}

			return values;
		}
		else {
			String[] values = new String[
				parentValues.length + fileItems.length];

			System.arraycopy(
				parentValues, 0, values, 0, parentValues.length);

			for (int i = parentValues.length; i < values.length; i++) {
				values[i] = fileItems[i - parentValues.length].getString();
			}

			return values;
		}
	}

	private Map _params;

}
