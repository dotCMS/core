<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<div id="${id}">
	<div dojoAttachPoint="dialog" dojoType="dijit.Dialog" style="height: 500px;width: 1000px;">
		<form dojoAttachPoint="search_form" onsubmit="return false;">
				<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" style="height:450px;" dojoAttachPoint="borderContainer" class="shadowBox headerBox">
					
					<!-- START Left Column -->
					<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width:350px;" class="lineRight">
						<input type="hidden" name="hostField" dojoAttachPoint="hostField" value=""/>
						<input type="hidden" name="folderField" dojoAttachPoint="folderField" value=""/>
						<div style="margin:10px 20px;">
							<b><%= LanguageUtil.get(pageContext, "Search") %>:</b> <span dojoAttachPoint='structureName'></span>							
						</div>
						<div class="sideMenuWrapper" style="height: 400px;overflow: auto;">
							<input type="hidden" name="structure_inode" dojoAttachPoint="structure_inode" value="strInode">
							<div dojoAttachPoint="search_fields_table"></div>
							<div dojoAttachPoint="search_categories_table">
								<dl dojoAttachPoint="search_categories_list"></dl>
							</div>
							<div class="clear"></div>
							<div class="buttonRow">
								<button dojoType="dijit.form.Button" dojoAttachEvent='onClick:_doSearchPage1' iconClass="searchIcon"><%= LanguageUtil.get(pageContext, "Search") %></button>
								<button dojoType="dijit.form.Button" dojoAttachEvent='onClick:_clearSearch' iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "Clear-Search") %></button>
							</div>
						</div>					
					 </div>
					 
					     <!-- START Right Column -->
					<div dojoType="dijit.layout.ContentPane" splitter="true" region="center">
						<div dojoAttachPoint="contentWrapper" style="overflow:auto;margin-top:36px;">
				        	<div dojoAttachPoint="matchingResultsDiv" style="display: none"><%= LanguageUtil.get(pageContext, "Results") %></div>
							<table dojoAttachPoint="results_table" class="listingTable"></table>
						</div>
						<div class="yui-g buttonRow">
							<div class="yui-u first" style="text-align:left;">
						        <div dojoAttachPoint="previousDiv" style="display: none;">
						             <button dojoType="dijit.form.Button" class="bg" dojoAttachEvent='onClick:_previousPage' iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "Previous") %></button>
						        </div>
							</div>
							<div class="yui-u" style="text-align:right;">
						        <div dojoAttachPoint="nextDiv" style="display: none;">
						             <button dojoType="dijit.form.Button" class="bg" dojoAttachEvent='onClick:_nextPage' iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "Next") %></button>
						        </div>
							</div>
						</div>
					</div>
					 
			    </div>
		</form>
		<!-- Dynamic Variables/Language specific content from request/session/context -->
		<div style="display: none;">
			<input type="hidden" dojoAttachPoint="htmlPageLanguage" value="<%=session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE) %>">
			<input type="hidden" dojoAttachPoint="tagText" value="<%= LanguageUtil.get(pageContext, "Type-your-tag-You-can-enter-multiple-comma-separated-tags") %>">
			<input type="hidden" dojoAttachPoint="suggestedTagsText" value="<%= LanguageUtil.get(pageContext, "Suggested-Tags") %>">
			<input type="hidden" dojoAttachPoint="noResultsText" value="<%= LanguageUtil.get(pageContext, "No-Results-Found") %>">
			<input type="hidden" dojoAttachPoint="matchResultsText" value="<%= LanguageUtil.get(pageContext, "Matching-Results") %>">
		</div>
	</div>
</div>
