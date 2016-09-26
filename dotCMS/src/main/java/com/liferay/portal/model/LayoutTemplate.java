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

package com.liferay.portal.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.util.PropsUtil;
import com.liferay.util.StringPool;
import com.liferay.util.Validator;

/**
 * <a href="LayoutTemplate.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Ivica Cardic
 * @version $Revision: 1.0 $
 *
 */
public class LayoutTemplate implements Comparable, Serializable {

	public LayoutTemplate() {
	}

	public LayoutTemplate(String layoutTemplateId) {
		_layoutTemplateId = layoutTemplateId;
	}

	public LayoutTemplate(String layoutTemplateId, String name) {
		_layoutTemplateId = layoutTemplateId;
		_name = name;
	}

	public String getLayoutTemplateId() {
		return _layoutTemplateId;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public String getTemplatePath() {
		return _templatePath;
	}

	public void setTemplatePath(String templatePath) {
		_templatePath = templatePath;
	}

	public String getThumbnailPath() {
		return _thumbnailPath;
	}

	public void setThumbnailPath(String thumbnailPath) {
		_thumbnailPath = thumbnailPath;
	}

	public String getContent() {
		return _content;
	}

	public void setContent(String content) {
		_setContent = true;

		_content = content;
	}

	public boolean hasSetContent() {
		return _setContent;
	}

	public List getLayoutTemplateColumns() {
		return _layoutTemplateColumns;
	}

	public void setLayoutTemplateColumns(List layoutTemplateColumns) {
		_layoutTemplateColumns = layoutTemplateColumns;
	}

	public String getServletContextName() {
		return _servletContextName;
	}

	public void setServletContextName(String servletContextName) {
		_servletContextName = servletContextName;

		if (Validator.isNotNull(_servletContextName)) {
			_warFile = true;
		}
		else {
			_warFile = false;
		}
	}

	public boolean getWARFile() {
		return _warFile;
	}

	public boolean isWARFile() {
		return _warFile;
	}

	public String getContextPath() {
		if (isWARFile()) {
			return StringPool.SLASH + getServletContextName();
		}
		else {
			String contextPath = PropsUtil.get(PropsUtil.PORTAL_CTX);
			if (contextPath.equals(StringPool.SLASH)) {
				contextPath = StringPool.BLANK;
			}

			return contextPath;
		}
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		LayoutTemplate layoutTemplate = (LayoutTemplate) obj;

		return getName().compareTo(layoutTemplate.getName());
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		LayoutTemplate layoutTemplate = null;

		try {
			layoutTemplate = (LayoutTemplate)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		String layoutTemplateId = layoutTemplate.getLayoutTemplateId();

		if (getLayoutTemplateId().equals(layoutTemplateId)) {
			return true;
		}
		else {
			return false;
		}
	}

	private String _layoutTemplateId;
	private String _name;
	private String _templatePath;
	private String _thumbnailPath;
	private String _content;
	private boolean _setContent;
	private List _layoutTemplateColumns = new ArrayList();
	private String _servletContextName;
	private boolean _warFile;

}