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
List questions = PollsQuestionManagerUtil.getQuestions(portletConfig.getPortletName(), company.getCompanyId(), 0, 5);
%>

<liferay:include page="/html/portlet/polls/sub_nav.jsp" />

<table border="0" cellpadding="4" cellspacing="0" width="100%">
<tr>
	<td align="center">
		<c:if test="<%= (questions == null) || (questions.size() == 0) %>">
			<font class="gamma" size="2">
			<%= LanguageUtil.get(pageContext, "there-are-no-questions") %>
			</font>
		</c:if>

		<c:if test="<%= (questions != null) && (questions.size() > 0) %>">
			<table border="0" cellpadding="0" cellspacing="0" width="95%">
			<tr>
				<td>
					<font class="gamma" size="2"><b>
					<%= LanguageUtil.get(pageContext, "Question Id") %>
					</b></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td>
					<font class="gamma" size="2"><b>
					<%= LanguageUtil.get(pageContext, "question") %>
					</b></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td>
					<font class="gamma" size="2"><b>
					<%= LanguageUtil.get(pageContext, "num-of-votes") %>
					</b></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td>
					<font class="gamma" size="2"><b>
					<%= LanguageUtil.get(pageContext, "last-vote-date") %>
					</b></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td>
					<font class="beta" size="2"><b>
					<%= LanguageUtil.get(pageContext, "expiration-date") %>
					</b></font>
				</td>
			</tr>

			<%
			for (int i = 0; i < questions.size() && i < 5; i++) {
				PollsQuestion question = (PollsQuestion)questions.get(i);
			%>

				<tr>
				    <td>
						<font class="gamma" size="2">
						<a class="gamma" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/polls/view_question" /><portlet:param name="question_id" value="<%= question.getQuestionId() %>" /></portlet:renderURL>"><%= question.getQuestionId() %></a>
						</font>
					</td>
					<td></td>
					<td>
						<font class="gamma" size="2">
						<a class="gamma" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/polls/view_question" /><portlet:param name="question_id" value="<%= question.getQuestionId() %>" /></portlet:renderURL>"><%= question.getTitle() %></a>
						</font>
					</td>
					<td></td>
					<td>
						<font class="gamma" size="2">
						<%= PollsVoteManagerUtil.getVotesSize(question.getQuestionId()) %>
						</font>
					</td>
					<td></td>
					<td>
						<font class="gamma" size="2">
						<%= question.getLastVoteDate() != null ? df.format(question.getLastVoteDate()) : LanguageUtil.get(pageContext, "never") %>
						</font>
					</td>
					<td></td>
					<td>
						<font class="gamma" size="2">
						<%= question.getExpirationDate() != null ? df.format(question.getExpirationDate()) : LanguageUtil.get(pageContext, "never") %>
						</font>
					</td>
				</tr>

			<%
			}
			%>

			</table>
		</c:if>
	</td>
</tr>
</table>