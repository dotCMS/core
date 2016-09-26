<%
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
%>

<%@ page import="com.liferay.portal.util.WebKeys" %>

<%
String CTX_PATH = (String)application.getAttribute(WebKeys.CTX_PATH);

String forwardURL = null;

String forwardParam = request.getParameter(WebKeys.FORWARD_URL);
String forwardRequest = (String)request.getAttribute(WebKeys.FORWARD_URL);
String forwardSession = (String)session.getAttribute(WebKeys.FORWARD_URL);

if ((forwardParam != null) && (!forwardParam.equals("null")) && (!forwardParam.equals(""))) {
	forwardURL = forwardParam;
}
else if ((forwardRequest != null) && (!forwardRequest.equals("null")) && (!forwardRequest.equals(""))) {
	forwardURL = forwardRequest;
}
else if ((forwardSession != null) && (!forwardSession.equals("null")) && (!forwardSession.equals(""))) {
	forwardURL = forwardSession;
}
else {
	forwardURL = CTX_PATH;
}
%>