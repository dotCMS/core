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

package com.liferay.portal.util;

/**
 * <a href="Constants.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.99 $
 *
 */
public interface Constants {

	// Commands used in Action classes

	public static final String CMD = "cmd";

	public static final String ADD = "add";

	public static final String APPROVE = "approve";

	public static final String CANCEL = "cancel";

	public static final String DELETE = "delete";

	public static final String EDIT = "edit";
	
	// *********************** BEGIN GRAZIANO issue-12-dnd-template	
	public static final String DESIGN = "design";
	
	public static final String ADD_DESIGN = "add_design";
	// *********************** END GRAZIANO issue-12-dnd-template
	
	public static final String REJECT = "reject";

	public static final String SAVE = "save";

	public static final String SEARCH = "search";

	public static final String SEND = "send";

	public static final String UPDATE = "update";

	public static final String VIEW = "view";
	
	public static final String VIEW_HOST_REPORT = "view_host_report";
	
	public static final String VIEW_ACTIVITY_STREAM = "view_activity_stream";
	
	public static final String VIEW_BROWSER = "view_browser";
	
	public static final String VIEW_FILES = "view_files";

	public static final String VIEW_PAGES = "view_pages";
	

	// Rreturn values used in Action classes

	public static final String COMMON_ERROR = "/common/error.jsp";

	public static final String COMMON_FORWARD = "/common/forward_js.jsp";

	public static final String COMMON_FORWARD_JSP = "/common/forward_jsp.jsp";

	public static final String COMMON_NULL = "/common/null.jsp";

	public static final String COMMON_REFERER = "/common/referer_js.jsp";

	public static final String COMMON_REFERER_JSP = "/common/referer_jsp.jsp";

	public static final String PORTAL_ERROR = "/portal/error";

	// Content types

	public static final String TEXT_HTML = "text/html";

	public static final String TEXT_PLAIN = "text/plain";

	public static final String TEXT_WML = "text/wml";

	public static final String TEXT_XML = "text/xml";

	// Content directories

	public static final String TEXT_HTML_DIR = "/html";

	public static final String TEXT_WML_DIR = "/wml";

	// Data source

	public static final String DATA_SOURCE = "jdbc/dotCMSPool";

	// JAAS

	public static final String REALM_NAME = "PortalRealm";

	public static final String JBOSS_LOGIN_MODULE = "client-login";
	
	// Hostvar
	public static final String ADDHVAR = "savehostvar";
	
	public static final String DELETEHVAR = "deletehostvar";

}