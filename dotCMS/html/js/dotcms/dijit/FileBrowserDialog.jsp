<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<div id="${id}" style="display: none;">
	<div dojoAttachPoint="dialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "Select-a-file")%>">
	<form dojoAttachPoint="search_form" onsubmit="return false;">
	    <div dojotype="dijit.layout.BorderContainer" design="sidebar" gutters="false" livesplitters="true" style="${style}" class="shadowBox headerBox">
	        <div dojotype="dijit.layout.ContentPane" dojoAttachPoint="foldersContentPane" splitter="true" region="leading" 
				style="top: 0px; bottom: 0px; left: 0px; width: 250px; overflow: auto;" title="Folders">
				<div class="filterBox">
	           		<%= LanguageUtil.get(pageContext, "Sites-and-Folders")%>
	            </div>
				<div class="clear"></div>
	            <div class="sideMenuWrapper" dojoAttachPoint="foldersTreeWrapper">
	            	<div dojoAttachPoint="foldersTree">
	            		
	            	</div>
           		</div>
	        </div>
	        <div dojotype="dijit.layout.ContentPane" splitter="true" region="center" title="Files" dojoAttachPoint="tablesContentPane">
				<div class="filterBox">
					<div class="viewSelectorBox" style="float: right;">
						<%= LanguageUtil.get(pageContext, "View")%>:
						<img dojoAttachPoint="listViewIcon" src="/html/images/icons/application-detail.png" alt='<%= LanguageUtil.get(pageContext, "list-view")%>' title='<%= LanguageUtil.get(pageContext, "list-view")%>'>
						<img dojoAttachPoint="detailViewIcon" src="/html/images/icons/application-list.png" alt='<%= LanguageUtil.get(pageContext, "details-view")%>' title='<%= LanguageUtil.get(pageContext, "details-view")%>'>            
						<img dojoAttachPoint="thumbnailViewIcon" src="/html/images/icons/application-icon.png" alt='<%= LanguageUtil.get(pageContext, "thumbnails-view")%>' title='<%= LanguageUtil.get(pageContext, "thumbnails-view")%>'>            
					</div>	
					<div style="float: left;">
						<%= LanguageUtil.get(pageContext, "Filter")%>:
						<input dojoType="dijit.form.TextBox" dojoAttachEvent="onKeyDown: _filter" trim="true" dojoAttachPoint="filterTextBox">
						<button dojoType="dijit.form.Button" dojoAttachEvent="onClick: _clearFilter" type="button" iconClass="resetIcon"><%= LanguageUtil.get(pageContext, "Clear")%></button>
					</div>
					<div style="float: left; display: none;" dojoAttachPoint="uploadFileButton">
						<button dojoType="dijit.form.Button" dojoAttachEvent="onClick: _addNewFile" type="button" 
							iconClass="uploadIcon"><%= LanguageUtil.get(pageContext, "Upload-New-File")%></button>
					</div>
				</div>
				<div class="clear"></div>
				<div  dojoAttachPoint="tablesWrapper" style="overflow: auto;">
		            <table dojoAttachPoint="detailsTable" style="display: none;" class="listingTable">
		                <thead>
		                    <tr>
		                        <th>
		                            <%= LanguageUtil.get(pageContext, "Name")%>
		                        </th>
		                        <th>
		                            <%= LanguageUtil.get(pageContext, "Description")%>
		                        </th>
		                        <th>
		                            <%= LanguageUtil.get(pageContext, "Mod-User")%>
		                        </th>
		                        <th>
		                            <%= LanguageUtil.get(pageContext, "Mod-Date")%>
		                        </th>
		                    </tr>
		                </thead>
		                <tbody dojoAttachPoint="detailsTableBody">
		                </tbody>
		            </table>
		            <table dojoAttachPoint="listTable" style="display: none;" class="listingTable">
		                <tbody dojoAttachPoint="listTableBody">
		                </tbody>
		            </table>
		            <table dojoAttachPoint="thumbnailsTable" style="display: none;" class="listingTable thumbnailTD">
		                <tbody dojoAttachPoint="thumbnailsTableBody">
		                </tbody>
		            </table>					
				</div>
				<div dojoAttachPoint="tablesSummary" style="display:none; text-align: center;overflow: hidden;">
					<div class="yui-gb buttonRow">
					 	<div class="yui-u first" style="text-align:left;">
							<button dojoType="dijit.form.Button" iconClass="previousIcon" dojoAttachPoint="previousButton" dojoAttachEvent="onClick:_previousButtonClick"><%= LanguageUtil.get(pageContext, "Previous")%></button>
						</div>
						<div class="yui-u" style="text-align:center;" dojoAttachPoint="resultsSummary"><%= LanguageUtil.get(pageContext, "Viewing-Results-1-of-10")%></div>
						<div class="yui-u" style="text-align:right;">
							<button dojoType="dijit.form.Button" iconClass="nextIcon" dojoAttachPoint="nextButton" dojoAttachEvent="onClick:_nextButtonClick"><%= LanguageUtil.get(pageContext, "Next")%></button>
						</div>
					</div>
				</div>
	            <div dojoAttachPoint="loadingContentWrapper" style="display: none; text-align: center;">
	                <br>
	                <br>
	                <b><%= LanguageUtil.get(pageContext, "Loading")%></b>
	                <br>
	                <img src="/html/images/icons/processing.gif">
	                <br>
	                <br>
	            </div>
	            <div dojoAttachPoint="noContent" style="text-align: center;">
	                <br>
	                <br>
	                <b><%= LanguageUtil.get(pageContext, "Please-select-a-folder")%></b>
	                <br>
	                <br>
	            </div>
	            <div dojoAttachPoint="noResults" style="display: none; text-align: center;">
	                <br>
	                <br>
	                <b><%= LanguageUtil.get(pageContext, "No-files-found")%></b>
	                <br>
	                <br>
	            </div>
	        </div>
	    </div>
	    </form>
	</div>
	
	<div dojoAttachPoint="addFileDialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "Upload-file(s)")%>" style="width: 700px; height: 450px;">
		<div dojoAttachPoint="addFileIFrameWrapper" style="width: 660px; height: 430px;">
			<iframe dojoAttachEvent="onload: _addFileIFrameLoaded" scrolling="no" style="border: 0; width: 680px; height: 400px;" dojoAttachPoint="addAFileIFrame"></iframe>
		</div>
	</div>	
	
</div>

