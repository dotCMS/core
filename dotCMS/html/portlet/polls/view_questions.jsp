<%@page import="com.dotmarketing.util.UtilMethods"%>
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

<%@ include file="/html/portlet/polls/init.jsp" %>

<%
int curValue = ParamUtil.get(request, "cur", 1);
int delta = 20;

int questionsStart = (curValue - 1) * delta;
int questionsEnd = questionsStart + delta;

List questions = PollsQuestionManagerUtil.getQuestions(portletConfig.getPortletName(), company.getCompanyId(), questionsStart, questionsEnd);
int questionsSize = PollsQuestionManagerUtil.getQuestionsSize(portletConfig.getPortletName(), company.getCompanyId());
%>

<div class="yui-g portlet-toolbar">
	<div class="yui-u" style="text-align:right;">	
		<button onClick="location.href='<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/polls/edit_question" /></portlet:actionURL>'" iconClass="plusIcon"  dojoType="dijit.form.Button" id="contentAddButton">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "add-question" )) %>
		</button>
	</div>
</div>

<table class="listingTable">
	<tr>
	   	<th><%= LanguageUtil.get(pageContext, "Id") %></th>
		<th><%= LanguageUtil.get(pageContext, "Title") %></th>
		<th><%= LanguageUtil.get(pageContext, "question") %></th>
		<th><%= LanguageUtil.get(pageContext, "num-of-votes") %></th>
		<th><%= LanguageUtil.get(pageContext, "last-vote-date") %></th>
		<th><%= LanguageUtil.get(pageContext, "expiration-date") %></th>
	</tr>

	<c:if test="<%= (questions == null) || (questions.size() == 0) %>">
		<tr class="alternate_1">
			<td colspan="5">
				<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "there-are-no-questions") %></div>
			</td>
		</tr>
	</c:if>

	<%
	for (int i = 0; i < questions.size(); i++) {
		PollsQuestion question = (PollsQuestion)questions.get(i);

		String className = "alternate_1";
		if (MathUtil.isEven(i)) {
			className = "alternate_2";
		}
	%>

		<tr class="<%= className %>">
		   <td nowrap>
                <a href="<portlet:renderURL><portlet:param name="struts_action" value="/polls/view_question" /><portlet:param name="question_id" value="<%= question.getQuestionId() %>" /></portlet:renderURL>"><%= question.getQuestionId() %></a>			
             </td>
			<td nowrap>
				<a href="<portlet:renderURL><portlet:param name="struts_action" value="/polls/view_question" /><portlet:param name="question_id" value="<%= question.getQuestionId() %>" /></portlet:renderURL>"><%= question.getTitle() %></a>
			</td>
			<td nowrap>
				<%= UtilMethods.truncatify(question.getDescription(), 255) %>
			</td>
			<td nowrap>
				<%= PollsVoteManagerUtil.getVotesSize(question.getQuestionId()) %>
			</td>
			<td nowrap>
				<%= question.getLastVoteDate() != null ? df.format(question.getLastVoteDate()) : LanguageUtil.get(pageContext, "never") %>
			</td>
			<td nowrap>
				<%= question.getExpirationDate() != null ? df.format(question.getExpirationDate()) : LanguageUtil.get(pageContext, "never") %>
			</td>
		</tr>

	<%
	}
	%>

	</table>

	<c:if test="<%= questionsSize > delta %>">
		<br>

		<table border="0" cellpadding="0" cellspacing="0" width="95%">
		<tr>
			<td>

				<%
				PortletURL portletURL = renderResponse.createRenderURL();

				portletURL.setParameter("struts_action", "/polls/view_questions");
				%>

				<liferay:page-iterator className="bg" curParam="<%= renderResponse.getNamespace() + \"cur\" %>" curValue="<%= curValue %>" delta="<%= delta %>" fontSize="2" maxPages="10" total="<%= questionsSize %>" url="<%= Http.decodeURL(portletURL.toString()) %>" />
			</td>
		</tr>
		</table>
	</c:if>