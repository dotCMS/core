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

package com.liferay.portal.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dotmarketing.util.Logger;
import com.liferay.portal.ejb.ImageLocalUtil;
import com.liferay.portal.model.Image;
import com.liferay.util.ParamUtil;
import com.liferay.util.StringPool;
import com.liferay.util.Validator;

/**
 * <a href="ImageServlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Brett Randall
 * @version $Revision: 1.9 $
 *
 */
public class ImageServlet extends HttpServlet {

	public void init(ServletConfig sc) throws ServletException {
		synchronized (ImageServlet.class) {
			super.init(sc);

			ServletContext ctx = getServletContext();

			_companyId = ctx.getInitParameter("company_id");
		}
	}

	public void service(HttpServletRequest req, HttpServletResponse res)
		throws IOException, ServletException {

		String path = req.getPathInfo();

		// The image id may be passed in as image_id, img_id, or i_id

		String imageId = ParamUtil.getString(req, "image_id");

		if (Validator.isNull(imageId)) {
			imageId = ParamUtil.getString(req, "img_id");

			if (Validator.isNull(imageId)) {
				imageId = ParamUtil.getString(req, "i_id");
			}
		}

		// Company Logo

		if (path.startsWith("/company_logo")) {
			if (ParamUtil.get(req, "png", false)) {
				imageId += ".png";

				res.setContentType("image/png");
			}
			else if (ParamUtil.get(req, "wbmp", false)) {
				imageId += ".wbmp";

				res.setContentType("image/vnd.wap.wbmp");
			}
		}

		// Image Gallery

		if (path.startsWith("/image_gallery")) {
			if (!imageId.equals(StringPool.BLANK) &&
				!imageId.startsWith(_companyId + ".image_gallery.")) {

				imageId = _companyId + ".image_gallery." + imageId;

				if (ParamUtil.get(req, "small", false)) {
					imageId += ".small";
				}
				else {
					imageId += ".large";
				}
			}
		}

		// Journal Article

		if (path.startsWith("/journal/article")) {
			if (!imageId.equals(StringPool.BLANK) &&
				!imageId.startsWith(_companyId + ".journal.article.")) {

				imageId = _companyId + ".journal.article." + imageId;

				String version = req.getParameter("version");

				if (Validator.isNotNull(version)) {
					imageId += "." + version;
				}
			}
		}

		// Journal Template

		if (path.startsWith("/journal/template")) {
			if (!imageId.equals(StringPool.BLANK) &&
				!imageId.startsWith(_companyId + ".journal.template.")) {

				imageId = _companyId + ".journal.template." + imageId;
				imageId += ".small";
			}
		}

		// Shopping Item

		else if (path.startsWith("/shopping/item")) {
			if (!imageId.equals(StringPool.BLANK) &&
				!imageId.startsWith(_companyId + ".shopping.item.")) {

				imageId = _companyId + ".shopping.item." + imageId;

				if (ParamUtil.get(req, "small", false)) {
					imageId += ".small";
				}
				else if (ParamUtil.get(req, "medium", false)) {
					imageId += ".medium";
				}
				else {
					imageId += ".large";
				}
			}
		}

		Image image = ImageLocalUtil.get(imageId);

		if (Validator.isNull(imageId)) {
			_log.error(
				"Image id should never be null or empty, path is " +
					req.getPathInfo());
		}

		if (image == null) {
			_log.error("Image should never be null");
		}
		else {
			if (Validator.isNotNull(image.getType())) {
				res.setContentType("image/" + image.getType());
			}

			/*res.setHeader(
				"Cache-control", "max-age=" + Long.toString(Time.WEEK));*/

			try {
				if (!res.isCommitted()) {
					ServletOutputStream out = res.getOutputStream();

					out.write(image.getTextObj());
					out.flush();
				}
			}
			catch (Exception e) {
				Logger.error(this,e.getMessage(),e);
			}
		}
	}

	private static final Log _log = LogFactory.getLog(ImageServlet.class);

	private String _companyId;

}