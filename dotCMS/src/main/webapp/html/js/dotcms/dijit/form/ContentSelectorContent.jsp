<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<style>
    .content-search-dialog__content-type-menu {
        display: block;
        max-height: 88vh;
        overflow-y: auto !important;
    }
    .related-content-form {
        width: 90vw;
        height: 90vh;
    }
</style>
<form
        dojoAttachPoint="search_form"
        onsubmit="return false;"
        id="searchForm"
        class="related-content-form"
>
    <!-- START Left Column -->
    <div dojoType="dijit.layout.ContentPane" class="portlet-sidebar-wrapper">
        <div class="portlet-sidebar">
            <input
                    type="hidden"
                    name="hostField"
                    dojoAttachPoint="hostField"
                    value=""
            />
            <input
                    type="hidden"
                    name="folderField"
                    dojoAttachPoint="folderField"
                    value=""
            />
            <input
                    type="hidden"
                    name="structure_inode"
                    dojoAttachPoint="structure_inode"
                    value="strInode"
            />
            <div class="sideMenuWrapper">
                <div dojoAttachPoint="content_type_select"></div>
                <div dojoAttachPoint="search_general">
                    <dl class="vertical">
                        <dt>
                            <label
                            ><%= LanguageUtil.get(pageContext, "Search")
                            %>:</label
                            >
                        </dt>
                        <dd>
                            <input
                                    type="text"
                                    dojoType="dijit.form.TextBox"
                                    data-dojo-props="intermediateChanges:true"
                                    dojoAttachEvent="onKeyUp:_doSearchPage1"
                                    dojoAttachPoint="generalSearch"
                            />
                        </dd>
                    </dl>
                </div>
                <div dojoAttachPoint="search_languages_table"></div>
                <div dojoAttachPoint="site_folder_field_pop"></div>
                <div dojoAttachPoint="search_fields_table"></div>
                <div dojoAttachPoint="search_categories_table">
                    <dl
                            class="vertical"
                            dojoAttachPoint="search_categories_list"
                    ></dl>
                </div>
                <div class="clear"></div>
                <div class="buttonRow">
                    <button
                            dojoType="dijit.form.Button"
                            dojoAttachEvent="onClick:_doSearchPage1"
                            iconClass="searchIcon"
                    >
                        <%= LanguageUtil.get(pageContext, "Search") %>
                    </button>
                    <button
                            dojoType="dijit.form.Button"
                            dojoAttachEvent="onClick:_clearSearch"
                            iconClass="cancelIcon"
                            class="dijitButtonFlat"
                            style="margin-top: 16px;"
                    >
                        <%= LanguageUtil.get(pageContext, "Clear") %>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- START Right Column -->
    <div dojoType="dijit.layout.ContentPane" class="portlet-main-wrapper">
        <div dojoAttachPoint="contentWrapper">
            <div class="portlet-toolbar">
                <div
                        dojoAttachPoint="matchingResultsDiv"
                        class="portlet-toolbar__matching-results"
                        style="visibility: hidden;"
                >
                    <%= LanguageUtil.get(pageContext, "Results") %>
                </div>
                <div id="addContentTypeDropdown"></div>
                <div
                        dojoAttachPoint="addContentletButton"
                        class="portlet-toolbar__add-contentlet"
                        style="display: none;"
                >
                    <button
                            dojoType="dijit.form.Button"
                            onClick="addNewContentlet()"
                    >
                        <%= LanguageUtil.get(pageContext, "Add-New-Content") %>
                    </button>
                </div>
                <div dojoAttachPoint="relateDiv" id="doRelateContainer">
                    <button
                            dojoType="dijit.form.Button"
                            dojoAttachEvent="onClick:_doRelateContent"
                            iconClass="searchIcon"
                    >
                        <%= LanguageUtil.get(pageContext, "Relate") %>
                    </button>
                </div>
            </div>
            <table
                    dojoAttachPoint="results_table"
                    class="listingTable relateContent"
            ></table>
        </div>
        <div class="portlet-pagination">
            <div dojoAttachPoint="previousDiv" style="display: none;">
                <button
                        dojoType="dijit.form.Button"
                        class="bg"
                        dojoAttachEvent="onClick:_previousPage"
                        iconClass="previousIcon"
                >
                    <%= LanguageUtil.get(pageContext, "Previous") %>
                </button>
            </div>
            <div></div>
            <div dojoAttachPoint="nextDiv" style="display: none;">
                <button
                        dojoType="dijit.form.Button"
                        class="bg"
                        dojoAttachEvent="onClick:_nextPage"
                        iconClass="nextIcon"
                >
                    <%= LanguageUtil.get(pageContext, "Next") %>
                </button>
            </div>
        </div>
    </div>
</form>

<!-- Dynamic Variables/Language specific content from request/session/context -->
<div style="display: none;">
    <input
        type="hidden"
        dojoAttachPoint="htmlPageLanguage"
        value="<%=session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE) %>"
    />
    <input type="hidden" dojoAttachPoint="tagText" value="<%=
    LanguageUtil.get(pageContext,
    "Type-your-tag-You-can-enter-multiple-comma-separated-tags") %>"> <input
        type="hidden" dojoAttachPoint="suggestedTagsText" value="<%=
    LanguageUtil.get(pageContext, "Suggested-Tags") %>"> <input type="hidden"
                                                                dojoAttachPoint="noResultsText" value="<%= LanguageUtil.get(pageContext,
    "No-Results-Found") %>"> <input type="hidden"
                                    dojoAttachPoint="matchResultsText" value="<%= LanguageUtil.get(pageContext,
    "Matching-Results") %>">
</div>
