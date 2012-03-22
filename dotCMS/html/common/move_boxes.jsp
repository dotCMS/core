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

<%@ include file="/html/common/init.jsp" %>

<%
String formName = ParamUtil.get(request, "form_name", "fm");

String leftTitle = ParamUtil.getString(request, "left_title");
String rightTitle = ParamUtil.getString(request, "right_title");

String leftBoxName = ParamUtil.getString(request, "left_box_name");
String rightBoxName = ParamUtil.getString(request, "right_box_name");

String leftOnChange = ParamUtil.getString(request, "left_on_change");
String rightOnChange = ParamUtil.getString(request, "right_on_change");

boolean leftReorder = ParamUtil.get(request, "left_reorder", false);
boolean rightReorder = ParamUtil.get(request, "right_reorder", false);

List leftKVPs = (List)request.getAttribute(WebKeys.MOVE_BOXES_LEFT_LIST);
List rightKVPs = (List)request.getAttribute(WebKeys.MOVE_BOXES_RIGHT_LIST);
%>

<table border="0" cellpadding="0" cellspacing="0" width="100%">
<tr>
	<td class="alpha">
		<table border="0" cellpadding="2" cellspacing="1" width="100%">
		<tr>
			<td align="center" class="gamma" valign="top" width="48%">
				<table border="0" cellpadding="2" cellspacing="0" width="100%" class="listingTable">
				<tr>
					<td align="center" class="header">
						<%= leftTitle %>
					</td>
				</tr>
				<tr>
					<td align="center">
						<table border="0" cellpadding="0" cellspacing="2">
						<tr>
							<td>
								<select multiple name="<%= leftBoxName %>" size="10" <%= Validator.isNotNull(leftOnChange) ? "onChange=\"" + leftOnChange + "\"" : "" %> onFocus="document.<%= formName %>.<%= rightBoxName %>.selectedIndex = '-1';">

								<%
								for (int i = 0; i < leftKVPs.size(); i++) {
									KeyValuePair kvp = (KeyValuePair)leftKVPs.get(i);
								%>

									<option value="<%= kvp.getKey() %>"><%= kvp.getValue() %></option>

								<%
								}
								%>

								</select>
							</td>

							<c:if test="<%= leftReorder %>">
								<td valign="top">
									<a class="gamma" href="javascript: reorder(document.<%= formName %>.<%= leftBoxName %>, 0);"><img border="0" height="16" hspace="0" src="<%= SKIN_COMMON_IMG %>/03_up.gif" vspace="2" width="16"></a><br>
									<a class="gamma" href="javascript: reorder(document.<%= formName %>.<%= leftBoxName %>, 1);"><img border="0" height="16" hspace="0" src="<%= SKIN_COMMON_IMG %>/03_down.gif" vspace="2" width="16"></a><br>
								</td>
							</c:if>

						</tr>
						</table>
					</td>
				</tr>
				</table>
			</td>
			<td align="center" class="gamma" valign="middle" width="4%">
				<br>
				<a class="bg" href="javascript: moveItem(document.<%= formName %>.<%= leftBoxName %>, document.<%= formName %>.<%= rightBoxName %>, <%= !rightReorder %>);"><img border="0" height="16" hspace="0" src="<%= SKIN_COMMON_IMG %>/03_right.gif" vspace="2" width="16" onClick="self.focus();"></a><br>
				<br>
				<a class="bg" href="javascript: moveItem(document.<%= formName %>.<%= rightBoxName %>, document.<%= formName %>.<%= leftBoxName %>, <%= !leftReorder %>);"><img border="0" height="16" hspace="0" src="<%= SKIN_COMMON_IMG %>/03_left.gif" vspace="2" width="16" onClick="self.focus();"></a><br>
				<br>
			</td>
			<td align="center" class="bg" valign="top" width="48%">
				<table border="0" cellpadding="2" cellspacing="0" width="100%" class="listingTable">
				<tr>
					<td align="center" class="header">
						<%= rightTitle %>
					</td>
				</tr>
				<tr>
					<td align="center">
						<table border="0" cellpadding="0" cellspacing="2">
						<tr>
							<td>
								<select multiple name="<%= rightBoxName %>" size="10" <%= Validator.isNotNull(rightOnChange) ? "onChange=\"" + rightOnChange + "\"" : "" %> onFocus="document.<%= formName %>.<%= leftBoxName %>.selectedIndex = '-1';">

								<%
								for (int i = 0; i < rightKVPs.size(); i++) {
									KeyValuePair kvp = (KeyValuePair)rightKVPs.get(i);
								%>

									<option value="<%= kvp.getKey() %>"><%= kvp.getValue() %></option>

								<%
								}
								%>

								</select>
							</td>

							<c:if test="<%= rightReorder %>">
								<td valign="top">
									<a class="gamma" href="javascript: reorder(document.<%= formName %>.<%= rightBoxName %>, 0);"><img border="0" height="16" hspace="0" src="<%= SKIN_COMMON_IMG %>/03_up.gif" vspace="2" width="16"></a><br>
									<a class="gamma" href="javascript: reorder(document.<%= formName %>.<%= rightBoxName %>, 1);"><img border="0" height="16" hspace="0" src="<%= SKIN_COMMON_IMG %>/03_down.gif" vspace="2" width="16"></a><br>
								</td>
							</c:if>

						</tr>
						</table>
					</td>
				</tr>
				</table>
			</td>
		</tr>
		</table>
	</td>
</tr>
</table>