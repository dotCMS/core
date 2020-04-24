<%@ page import="com.liferay.portal.language.LanguageUtil" %>


<div id="${id}" style="display: none;">
    <style>
        .file-selector-tree__card-view {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
            grid-template-rows: repeat(auto-fill, minmax(220px, 1fr));
            grid-gap: 0.75rem;
            margin: 2rem;
            width: 100%;
        }

        .file-selector-tree__card {
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24);
            cursor: pointer;
            display: flex;
            flex-direction: column;
            height: 100%;
            transition: box-shadow 100ms;
            min-height: 220px;
            margin: 0.25rem;
        }

        .file-selector-tree__card:hover {
            box-shadow: 0 3px 6px rgba(0, 0, 0, 0.16), 0 3px 6px rgba(0, 0, 0, 0.23);
        }

        .file-selector-tree .file-selector-tree__card .thumbnail {
            position: relative;
            background-size: cover;
            background-position: center center;
            background-repeat: no-repeat;
            width: 100%;
            height: 100%;
        }

        .file-selector-tree .file-selector-tree__card .icon {
            position: relative;
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .file-selector-tree .file-selector-tree__card div[class$="Icon"] {
            font-size: 72px;
            height: 72px;
            line-height: 1;
            width: 72px;
        }

        .file-selector-tree .file-selector-tree__card .thumbnail img {
            width: 0px;
            height: 0px;
            position: absolute;
        }

        .file-selector-tree .file-selector-tree__card .label {
            font-size: 16px;
            padding: 1.5rem 1rem;
            text-overflow: ellipsis;
            white-space: nowrap;
            overflow: hidden;
            flex-shrink: 0;
        }

        .file-selector-tree .selectableFile {
            align-items: center;
            cursor: pointer;
        }

        .file-selector-tree .selectableFile .thumbnail {
            width: 48px;
            margin-right: 8px;
        }

        .file-selector-tree .thumbnail img {
            max-width: 100%;
            margin: 0;
        }

        .file-selector-tree__views-menu {
            align-items: center;
        }

        .file-selector-tree__table {
            margin-bottom: 80px;
            overflow-y: scroll;

        }

        .file-selector-tree .portlet-main {
            overflow-y: inherit;
        }

        .file-selector-tree__main {
            overflow: inherit;
        }

    </style>
    <div dojoAttachPoint="dialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "Select-a-file")%>" class="file-selector-tree">
        <form dojoAttachPoint="search_form" onsubmit="return false;">
            <div dojotype="dijit.layout.BorderContainer" design="sidebar" gutters="false" livesplitters="false" class="file-selector-tree__container">
                <div dojotype="dijit.layout.ContentPane" dojoAttachPoint="foldersContentPane" splitter="false" region="leading" title="<%= LanguageUtil.get(pageContext, "Folders")%>" class="portlet-sidebar-wrapper">
                    <div dojoAttachPoint="foldersTreeWrapper" class="file-selector-tree__sidebar">
                        <div dojoAttachPoint="foldersTree"></div>
                    </div>
                </div>
                <div dojotype="dijit.layout.ContentPane" splitter="true" region="center" title="Files" dojoAttachPoint="tablesContentPane" class="file-selector-tree__main">
                    <div class="portlet-main">
                        <div class="portlet-toolbar file-selector-tree__toolbar">
                            <div class="portlet-toolbar__actions-primary">
                                <div class="inline-form">
                                    <label for="filterTextBox"><%= LanguageUtil.get(pageContext, "Filter")%>:</label>
                                    <input dojoType="dijit.form.TextBox" dojoAttachEvent="onKeyUp: _filter" trim="true" dojoAttachPoint="filterTextBox" />
                                    <button dojoType="dijit.form.Button" dojoAttachEvent="onClick: _clearFilter" type="button" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Clear")%></button>

                                    <span style="display: none;" dojoAttachPoint="uploadFileButton">
										<button dojoType="dijit.form.Button" dojoAttachEvent="onClick: _addNewFile" type="button"><%= LanguageUtil.get(pageContext, "Upload-New-File")%></button>
									</span>
                                </div>
                            </div>
                            <div class="portlet-toolbar__actions-secondary">
                                <div class="file-selector-tree__views-menu">
                                    <div style="margin:10px;"><%= LanguageUtil.get(pageContext, "View")%>:</div>
                                    <div dojoAttachPoint="detailViewIcon" style="margin:10px;cursor:pointer;">
                                        <i  class="material-icons">format_list_bulleted</i>
                                    </div>
                                    <div dojoAttachPoint="thumbnailViewIcon" style="margin:10px;cursor:pointer;">
                                        <i  class="material-icons">grid_on</i>
                                    </div>

                                    
                                </div>
                            </div>
                        </div>
                        <div dojoAttachPoint="tablesWrapper" style="display: none" class="file-selector-tree__table">
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

                            <div dojoAttachPoint="thumbnailsTable" style="display: none" class="file-selector-tree__card-view">
                            </div>
                        </div>
                        <div dojoAttachPoint="tablesSummary" style="display:none" class="file-selector-tree__pagination portlet-pagination">
                            <button dojoType="dijit.form.Button" dojoAttachPoint="previousButton" dojoAttachEvent="onClick:_previousButtonClick"><%= LanguageUtil.get(pageContext, "Previous")%></button>
                            <div class="portlet-pagination__results" dojoAttachPoint="resultsSummary"><%= LanguageUtil.get(pageContext, "Viewing-Results-1-of-10")%></div>
                            <button dojoType="dijit.form.Button" dojoAttachPoint="nextButton" dojoAttachEvent="onClick:_nextButtonClick"><%= LanguageUtil.get(pageContext, "Next")%></button>
                        </div>
                        <div dojoAttachPoint="loadingContentWrapper" style="display: none;" class="file-selector-tree__loading">
							<span>
								<%= LanguageUtil.get(pageContext, "Loading")%>
								<br>
								<img src="/html/images/icons/processing.gif">
							</span>
                        </div>
                        <div dojoAttachPoint="noContent" class="file-selector-tree__select-folder">
                            <%= LanguageUtil.get(pageContext, "Please-select-a-folder")%>
                        </div>
                        <div dojoAttachPoint="noResults" style="display: none;" class="file-selector-tree__no-results">
                            <%= LanguageUtil.get(pageContext, "No-files-found")%>
                        </div>
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
