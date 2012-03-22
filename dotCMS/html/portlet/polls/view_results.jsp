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

NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
NumberFormat percentFormat = NumberFormat.getPercentInstance(locale);
%>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"current-results\") %>" />
<div class="portlet-wrapper">


	<div style="min-height:400px;" id="borderContainer" class="shadowBox headerBox">				
	 	<div style="padding:7px;">
		 	<div>

				<h3><%= LanguageUtil.get(pageContext, "current-results") %></h3>
		  	</div>
				<br clear="all">
	  	</div>

	<table border="0" style="margin:auto;" cellpadding="0" cellspacing="0" width="85%">
	<tr>
		<td>
			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td>
					<font class="gamma" size="2">
					<b><%= question.getTitle() %></b> <c:if test="<%= PollsQuestionManagerUtil.hasAdmin(question.getQuestionId()) %>">&nbsp;<font class="gamma" size="1">[<a class="gamma" href="<portlet:actionURL><portlet:param name="struts_action" value="/polls/edit_question" /><portlet:param name="question_id" value="<%= question.getQuestionId() %>" /></portlet:actionURL>"><%= LanguageUtil.get(pageContext, "edit") %></a>]</font></c:if><br>
					<%= question.getDescription() %><br>
					</font>
				</td>
			</tr>
			</table>

			<br>

			<c:if test="<%= SessionMessages.contains(renderRequest, \"vote_added\") %>">
				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td>
						<font class="bg" size="1"><span class="bg-pos-alert"><%= LanguageUtil.get(pageContext, "thank-you-for-your-vote") %></span></font>
					</td>
				</tr>
				</table>

				<br>
			</c:if>

			<table border="0" cellpadding="4" cellspacing="0" width="100%">
			<tr>
				<td class="beta">
					<table border="0" cellpadding="4" cellspacing="0" width="100%">
					<tr class="beta">
						<td>
							<font class="beta" size="2"><b>
							%
							</b></font>
						</td>
						<td width="10">&nbsp;
							
						</td>
						<td colspan="6">
							<font class="beta" size="2"><b>
							<%= LanguageUtil.get(pageContext, "votes") %>
							</b></font>
						</td>
					</tr>

					<%
					int totalVotes = PollsVoteManagerUtil.getVotesSize(question.getQuestionId());

					for (int i = 0; i < choices.size(); i++) {
						PollsChoice choice = (PollsChoice)choices.get(i);

						int choiceVotes = PollsVoteManagerUtil.getVotesSize(question.getQuestionId(), choice.getChoiceId());

						String className = "gamma";
						if (MathUtil.isOdd(i)) {
							className = "bg";
						}

						double votesPercent = 0.0;
						if (totalVotes > 0) {
							votesPercent = (double)choiceVotes / totalVotes;
						}

						int votesPixelWidth = 100;
						int votesPercentWidth = (int)Math.floor(votesPercent * votesPixelWidth);
					%>

						<tr class="<%= className %>">
							<td valign="top">
								<font class="<%= className %>" size="2">
								<%= percentFormat.format(votesPercent) %>
								</font>
							</td>
							<td width="10">&nbsp;
								
							</td>
							<td colspan="<%= votesPercentWidth > 0 ? "1" : "3" %>" valign="top">
								<font class="<%= className %>" size="2">
								<%= numberFormat.format(choiceVotes) %>
								</font>
							</td>

							<c:if test="<%= votesPercentWidth > 0 %>">
								<td width="10">&nbsp;
									
								</td>
								<td valign="middle">
									<table border="0" cellpadding="0" cellspacing="0">
									<tr>
										<td class="alpha"><img border="0" height="5" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="<%= votesPercentWidth %>"></td>
										<td class="beta"><img border="0" height="5" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="<%= votesPixelWidth - votesPercentWidth %>"></td>
									</tr>
									</table>
								</td>
							</c:if>

							<td width="10">&nbsp;
								
							</td>
							<td valign="top">
								<font class="<%= className %>" size="2"><b>
								<%= choice.getChoiceId() %>.
								</b></font>
							</td>
							<td valign="top" width="99%">
								<font class="<%= className %>" size="2">
								<%= choice.getDescription() %>
								</font>
							</td>
						</tr>

					<%
					}
					%>

					</table>
				</td>
			</tr>
			</table>

			<br>

			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td>
					<font class="gamma" size="2">
					<b><%= LanguageUtil.get(pageContext, "total-votes") %>:</b>
					</font>
				</td>
				<td width="10">&nbsp;
					
				</td>
				<td>
					<font class="gamma" size="2">
					<%= numberFormat.format(totalVotes) %>
					</font>
				</td>
			</tr>
			</table>


			<%
			boolean hasVoted = PollsQuestionManagerUtil.hasVoted(question.getQuestionId());
			%>

			<c:if test="<%= !hasVoted %>">
				<br>

				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td>
                        <button dojoType="dijit.form.Button" onClick="self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/polls/view_question" /><portlet:param name="question_id" value="<%= question.getQuestionId() %>" /></portlet:actionURL>';">
                           <%= LanguageUtil.get(pageContext, "back-to-vote") %>
                        </button>
					</td>
				</tr>
				</table>
			</c:if>
		</td>
	</tr>
	</table>
	</div>
</div>
	
</liferay:box>