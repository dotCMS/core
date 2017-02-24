<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<div id="${id}">
	<div dojoAttachPoint="dialog" dojoType="dijit.Dialog" dojoAttachEvent='onHide:_clearSearch' class="related-content-dialog">
		<form dojoAttachPoint="search_form" onsubmit="return false;" id="searchForm" class="related-content-form">
			<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" dojoAttachPoint="borderContainer" class="related-content-container">

					<!-- START Left Column -->
					<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" class="portlet-sidebar-wrapper">
						<div class="portlet-sidebar">
							<input type="hidden" name="hostField" dojoAttachPoint="hostField" value=""/>
							<input type="hidden" name="folderField" dojoAttachPoint="folderField" value=""/>
							<div class="sideMenuWrapper">
								<input type="hidden" name="structure_inode" dojoAttachPoint="structure_inode" value="strInode">
								<div dojoAttachPoint="structures_select"></div>
								<div dojoAttachPoint="search_languages_table"></div>
								<div dojoAttachPoint="search_fields_table"></div>
								<div dojoAttachPoint="search_categories_table">
								<dl class="vertical" dojoAttachPoint="search_categories_list"></dl>
								</div>
								<div class="clear"></div>
								<div class="buttonRow">
									<button dojoType="dijit.form.Button" dojoAttachEvent='onClick:_doSearchPage1' iconClass="searchIcon"><%= LanguageUtil.get(pageContext, "Search") %></button>
									<button dojoType="dijit.form.Button" dojoAttachEvent='onClick:_clearSearch' iconClass="cancelIcon" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Clear") %></button>
								</div>
							</div>
						</div>
					 </div>

					<!-- START Right Column -->
					<div dojoType="dijit.layout.ContentPane" splitter="true" region="center" class="portlet-main-wrapper">
						<div class="portlet-main">
							<div dojoAttachPoint="contentWrapper">
								<div class="portlet-toolbar">
									<div dojoAttachPoint="matchingResultsDiv" style="display: none"><%= LanguageUtil.get(pageContext, "Results") %></div>
									<div dojoAttachPoint="relateDiv">
										<button dojoType="dijit.form.Button" dojoAttachEvent='onClick:_doRelateContent' iconClass="searchIcon"><%= LanguageUtil.get(pageContext, "Relate") %></button>
									</div>
								</div>
								<table dojoAttachPoint="results_table" class="listingTable"></table>
							</div>
							<div class="portlet-pagination">
								<div dojoAttachPoint="previousDiv" style="display: none;">
									<button dojoType="dijit.form.Button" class="bg" dojoAttachEvent='onClick:_previousPage' iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "Previous") %></button>
								</div>
								<div></div>
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
