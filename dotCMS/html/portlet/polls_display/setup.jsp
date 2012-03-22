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

<%@ include file="/html/portlet/polls_display/init.jsp" %>

<c:if test="<%= !pollsDisplayAdmin %>">
	<liferay:include page="/html/portal/portlet_not_setup.jsp" />
</c:if>

<c:if test="<%= pollsDisplayAdmin %>">

	<%
	PollsDisplay pollsDisplay = (PollsDisplay)request.getAttribute(WebKeys.POLLS_DISPLAY);
	%>

	<table border="0" cellpadding="4" cellspacing="0" width="100%">

	<form action="<portlet:renderURL><portlet:param name="struts_action" value="/polls_display/setup" /></portlet:renderURL>" method="post" name="<portlet:namespace />fm" onSubmit="submitForm(this); return false;">
	<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="<%= Constants.UPDATE %>">

	<tr>
		<td align="center">
			<table border="0" cellpadding="0" cellspacing="0">

			<c:if test="<%= SessionErrors.contains(renderRequest, NoSuchQuestionException.class.getName()) %>">
				<tr>
					<td>
						<font class="bg" size="1"><span class="bg-neg-alert"><%= LanguageUtil.get(pageContext, "the-question-could-not-be-found") %></span></font>
					</td>
				</tr>
				<tr>
					<td><img border="0" height="8" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
				</tr>
			</c:if>

			<tr>
				<td>
					<table border="0" cellpadding="0" cellspacing="0">
					<tr>
						<td>
							<font class="gamma" size="2">
							<%= LanguageUtil.get(pageContext, "question") %>
							</font>
						</td>
						<td width="10">&nbsp;
							
						</td>
						<td>
							<select name="<portlet:namespace />question_id">
								<option value=""></option>

								<%
								List questions = PollsQuestionManagerUtil.getQuestions(PortletKeys.POLLS, company.getCompanyId());

								for (int i = 0; i < questions.size(); i++) {
									PollsQuestion question = (PollsQuestion)questions.get(i);
								%>

									<option <%= ((pollsDisplay != null) && (pollsDisplay.getQuestionId().equals(question.getQuestionId()))) ? "selected" : "" %> value="<%= question.getQuestionId() %>"><%= question.getTitle() %></option>

								<%
								}
								%>

							</select>
						</td>
					</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td><img border="0" height="8" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			</tr>
			<tr>
				<td align="center">
                    <button dojoType="dijit.form.Button" type="submit" id="submitButton"><bean:message key="update" /></button>
                    
                    <button dojoType="dijit.form.Button" 
                    onClick="self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/polls_display/view" /></portlet:renderURL>';">
                       <bean:message key="cancel" />
                    </button>
				</td>
			</tr>
			</table>
		</td>
	</tr>

	</form>

	</table>
</c:if>