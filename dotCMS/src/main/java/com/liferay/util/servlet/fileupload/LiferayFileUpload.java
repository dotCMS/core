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

package com.liferay.util.servlet.fileupload;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.liferay.util.Validator;

/**
 * <a href="LiferayFileUpload.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Myunghun Kim
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class LiferayFileUpload extends ServletFileUpload {

	public static final String FILE_NAME =
		LiferayFileUpload.class.getName() + "_FILE_NAME";

	public static final String PERCENT =
		LiferayFileUpload.class.getName() + "_PERCENT";

	public LiferayFileUpload(FileItemFactory fileItemFactory,
								 HttpServletRequest req) {

		super(fileItemFactory);

		_req = req;
		_ses = req.getSession();
	}

    public List parseRequest(HttpServletRequest req)
		throws FileUploadException {

		_ses.removeAttribute(LiferayFileUpload.FILE_NAME);
		_ses.removeAttribute(LiferayFileUpload.PERCENT);

		return super.parseRequest(req);
	}

	protected FileItem createItem(Map headers, boolean formField)
		throws FileUploadException {

		LiferayFileItem item =
			(LiferayFileItem)super.createItem(headers, formField);

		String fileName = item.getFileName();

		if (Validator.isNotNull(fileName)) {
			_ses.setAttribute(LiferayFileUpload.FILE_NAME, fileName);
		}

		return item;
	}

	private HttpServletRequest _req;
	private HttpSession _ses;

}