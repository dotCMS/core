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

package org.apache.commons.fileupload;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * <a href="LiferayDiskFileUpload.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Kim
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class LiferayDiskFileUpload extends ServletFileUpload {

	public static final String FILE_NAME =
		LiferayDiskFileUpload.class.getName() + "_FILE_NAME";

	public static final String PERCENT =
		LiferayDiskFileUpload.class.getName() + "_PERCENT";

	public LiferayDiskFileUpload(DiskFileItemFactory fileItemFactory,
								 HttpServletRequest req) {
		super(fileItemFactory);
		fileItemFactory.setSizeThreshold(51200);

		_req = req;
		_ses = req.getSession();
	}

    public List parseRequest(HttpServletRequest req)
		throws FileUploadException {

		_ses.removeAttribute(LiferayDiskFileUpload.FILE_NAME);
		_ses.removeAttribute(LiferayDiskFileUpload.PERCENT);

		return super.parseRequest(new LiferayServletRequest(req));
	}

	protected FileItem createItem(Map headers, boolean formField)
		throws FileUploadException {

		LiferayFileItem item =
			(LiferayFileItem)super.createItem(headers, formField);

		String fileName = item.getFileName();

		if (fileName != null) {
			_ses.setAttribute(LiferayDiskFileUpload.FILE_NAME, fileName);
		}

		return item;
	}

	private HttpServletRequest _req;
	private HttpSession _ses;

}