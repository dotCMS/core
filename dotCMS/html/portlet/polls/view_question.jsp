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
PollsQuestion question = (PollsQuestion)request.getAttribute(WebKeys.POLLS_QUESTION);
List choices = (List)request.getAttribute(WebKeys.POLLS_CHOICES);
%>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"question\") %>" />
<div class="portlet-wrapper">


	<div style="min-height:400px;" id="borderContainer" class="shadowBox headerBox">				
	 	<div style="padding:7px;">
		 	<div>

				<h3><%= LanguageUtil.get(pageContext, "question") %></h3>
		  	</div>
				<br clear="all">
	  	</div>

	<table border="0" style="margin:auto;" cellpadding="0" cellspacing="0" width="85%">

	<form action="<portlet:actionURL><portlet:param name="struts_action" value="/polls/add_vote" /></portlet:actionURL>" method="post" name="<portlet:namespace />fm">
	<input name="<portlet:namespace />redirect" type="hidden" value="<portlet:renderURL><portlet:param name="struts_action" value="/polls/view_question" /><portlet:param name="question_id" value="<%= question.getQuestionId() %>" /></portlet:renderURL>">
	<input name="<portlet:namespace />question_id" type="hidden" value="<%= question.getQuestionId() %>">

	<tr>
		<td>
			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td>
					<font class="bg" size="2">
					<b><%= question.getTitle() %></b> <c:if test="<%= PollsQuestionManagerUtil.hasAdmin(question.getQuestionId()) %>">&nbsp;<font class="bg" size="1">[<a class="bg" href="<portlet:actionURL><portlet:param name="struts_action" value="/polls/edit_question" /><portlet:param name="question_id" value="<%= question.getQuestionId() %>" /></portlet:actionURL>"><%= LanguageUtil.get(pageContext, "edit") %></a>]</font></c:if><br>
					<%= question.getDescription() %><br>
					</font>
				</td>
			</tr>
			</table>

			<br>

			<c:if test="<%= SessionErrors.contains(renderRequest, NoSuchChoiceException.class.getName()) %>">
				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td>
						<font class="bg" size="1"><span class="bg-neg-alert"><%= LanguageUtil.get(pageContext, "please-select-an-option") %></span></font>
					</td>
				</tr>
				</table>

				<br>
			</c:if>

			<c:if test="<%= SessionErrors.contains(renderRequest, DuplicateVoteException.class.getName()) %>">
				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td>
						<font class="bg" size="1"><span class="bg-neg-alert"><%= LanguageUtil.get(pageContext, "you-may-only-vote-once") %></span></font>
					</td>
				</tr>
				</table>

				<br>
			</c:if>

			<table border="0" cellpadding="0" cellspacing="0">

			<%
			Iterator itr = choices.iterator();

			while (itr.hasNext()) {
				PollsChoice choice = (PollsChoice)itr.next();
			%>

				<tr>
					<td valign="top">
						<input name="<portlet:namespace />choice_id" type="radio" value="<%= choice.getChoiceId() %>">
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td valign="top">
						<font class="bg" size="2"><b>
						<%= choice.getChoiceId() %>.
						</b></font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td valign="top">
						<font class="bg" size="2">
						<%= choice.getDescription() %>
						</font>
					</td>
				</tr>

			<%
			}
			%>

			</table>

			<br>

			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td>
					<button dojoType="dijit.form.Button" onClick="submitForm(document.<portlet:namespace />fm);"><%= LanguageUtil.get(pageContext, "vote") %></button>
                    
                    <button dojoType="dijit.form.Button" onClick="self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/polls/view_results" /><portlet:param name="question_id" value="<%= question.getQuestionId() %>" /></portlet:actionURL>';">
					<%= LanguageUtil.get(pageContext, "see-current-results") %>
                    </button>
				</td>
			</tr>
			</table>
		</td>
	</tr>

	</form>

	</table>
	</div>
	</div>
</liferay:box>