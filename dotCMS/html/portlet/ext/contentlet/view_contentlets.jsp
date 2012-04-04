<%@ include file="/html/portlet/ext/contentlet/init.jsp" %>

<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.portlets.languagesmanager.model.Language" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.dotmarketing.portlets.languagesmanager.business.*" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>
<%@page import="com.dotmarketing.cache.StructureCache"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>

<%
        List<Structure> structures = (List<Structure>)request.getAttribute (com.dotmarketing.util.WebKeys.Structure.STRUCTURES);
        List<Language> languages = (List<Language>)request.getAttribute (com.dotmarketing.util.WebKeys.LANGUAGES);



        java.util.Map params = new java.util.HashMap();
        params.put("struts_action",new String[] {"/ext/contentlet/view_contentlets"});

        String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);

        Map lastSearch = (Map)session.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_LAST_SEARCH);
        Structure structure = StructureFactory.getDefaultStructure();

    Map<String, String> fieldsSearch = new HashMap<String,String>();
    Language selectedLanguage = new Language();
    List<String> categories = new ArrayList();
    boolean showDeleted = false;
    boolean filterSystemHost = false;
    boolean filterLocked = false;
    int currpage = 1;
    String orderBy = "modDate desc";
    Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
    String languageId = String.valueOf(defaultLang.getId());
    if(request.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED)!= null){
        selectedLanguage = (Language)request.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED);
    }
    long selectedLanguageId = selectedLanguage.getId();

        String structureSelected = "";
        if(UtilMethods.isSet(request.getParameter("structure_id"))){
                structureSelected=request.getParameter("structure_id");
        }

        if (lastSearch != null && !UtilMethods.isSet(structureSelected)) {
                if(session.getAttribute("selectedStructure") != null){
                        structure = StructureCache.getStructureByInode((String)session.getAttribute("selectedStructure"));
                        if(structures.contains(structure)){
                                structureSelected = structure.getInode();
                        }else{
                                session.removeAttribute("selectedStructure");

                                structureSelected = null;;
                        }
                }
            fieldsSearch = (Map<String, String>) lastSearch.get("fieldsSearch");
            categories = (List<String>) lastSearch.get("categories");
            showDeleted = (Boolean) lastSearch.get("showDeleted");
            filterSystemHost = (Boolean) lastSearch.get("filterSystemHost");
            filterLocked = (Boolean) lastSearch.get("filterLocked");
            currpage = (Integer) lastSearch.get("page");
            orderBy = (String) lastSearch.get("orderBy");
        if (fieldsSearch.containsKey("languageId")) {
            languageId = ((String) fieldsSearch.get("languageId")).trim();
        }
        }

        if(!InodeUtils.isSet(structureSelected)){
                if(session.getAttribute("selectedStructure") != null){
                        String longSelectedStructure = (String) session.getAttribute("selectedStructure");
                        if(InodeUtils.isSet(longSelectedStructure)){
                                structureSelected = longSelectedStructure.toString();
                        }
                }
        }


        if (!InodeUtils.isSet(structureSelected) || !structures.contains(StructureCache.getStructureByInode(structureSelected))) {
            structure = (Structure)StructureFactory.getDefaultStructure();
            if(APILocator.getPermissionAPI().doesUserHavePermission(structure, PermissionAPI.PERMISSION_EDIT, user)){
                structureSelected = structure.getInode();
            }
        }

        if(!InodeUtils.isSet(structureSelected) || !structures.contains(StructureCache.getStructureByInode(structureSelected))){
                List<Structure> structs = structures;
                for(Structure struct : structs){
                        if(APILocator.getPermissionAPI().doesUserHavePermission(struct, PermissionAPI.PERMISSION_READ, user)){
                                structureSelected = struct.getInode();
                                break;
                        }
                }
        }

        if (InodeUtils.isSet(structureSelected)) {
            structure = (Structure)StructureFactory.getStructureByInode(structureSelected);
        }

        List<Field> fields = FieldsCache.getFieldsByStructureInode(structureSelected);

        boolean hasNoSearcheableHostFolderField = false;
        boolean hasHostFolderField = false;
        for (Field field: fields) {
                if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                        if(APILocator.getPermissionAPI().doesUserHavePermission(APILocator.getHostAPI().findSystemHost(), PermissionAPI.PERMISSION_READ, user, true)){
                           hasHostFolderField = true;
                        }
                        if(!field.isSearchable()){
                           hasNoSearcheableHostFolderField = true;
                        }
                        break;
                }
        }

        if (!UtilMethods.isSet(fieldsSearch.get("conHost")) || hasNoSearcheableHostFolderField) {
                fieldsSearch.put("conHost", (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID));
        }

        String crumbtrailSelectedHostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
        if ((crumbtrailSelectedHostId == null) || crumbtrailSelectedHostId.equals("allHosts"))
                crumbtrailSelectedHostId = "";

        String structureInodesList="";
        String structureVelocityVarNames="";

        for (Structure st : structures) {

                if(structureInodesList!=""){
                        structureInodesList+=";"+st.getInode();
                }
                else
                        structureInodesList+=st.getInode();

                if(structureVelocityVarNames!=""){
                        structureVelocityVarNames+=";"+st.getVelocityVarName();
                }
                else
                        structureVelocityVarNames+=st.getVelocityVarName();

        }
%>


<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.dotmarketing.business.RoleAPIImpl"%>
<%@page import="com.dotmarketing.portlets.folders.model.Folder"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.cache.FieldsCache"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>

<%@page import="com.dotmarketing.business.PermissionAPI"%>
<jsp:include page="/html/portlet/ext/folders/context_menus_js.jsp" />
<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/CategoryAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/ContentletAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>


<jsp:include page="/html/portlet/ext/folders/menu_actions_js.jsp" />

<script type="text/javascript">

    var dataItems = {
        identifier: "name",
        label: "label",
        items: [
                <%boolean started = false;%>
                <%for(Structure s : structures){
                	String spanClass = (s.getStructureType() ==1)
               			? "contentIcon"
           				:	(s.getStructureType() ==2)
               					? "gearIcon"
           						:	(s.getStructureType() ==3)
           						? "formIcon"
           								: "fileIcon";



                %>
                        <%=(started) ? "," :""%>
                        {
                            name: "<%=s.getInode()%>",
                            label: "<span class='<%=spanClass%>'></span> <%=UtilMethods.javaScriptify(s.getName())%>",
                            textLabel: "<%=s.getName()%>"
                        }
                        <%started = true;%>
                <%}%>

        ]
    };

    var dojoStore = new dojo.data.ItemFileReadStore({
        data: dataItems
    });


    dojo.addOnLoad(function() {


        function myLabelFunc(item) {
               return item.textLabel + "";
        }


        var fs = new dijit.form.FilteringSelect({
            id: "structure_inode",
            name: "structure_inode_select",
            value: "<%=structureSelected%>",
            store: dojoStore,
            searchAttr: "textLabel",
            labelAttr: "label",
            labelType: "html",

            onChange: function(){
                structureChanged(true);
                        doSearch();
            }
        },
        dojo.byId("structSelectBox"));



        })

        function initialLoad() {
                doSearch(<%= currpage %>, "<%=orderBy%>");
                dijit.byId("searchButton").attr("disabled", false);
                dijit.byId("clearButton").setAttribute("disabled", false);

                displayArchiveButton();
        }

</script>





<script language="Javascript">

        <%@ include file="/html/portlet/ext/contentlet/view_contentlets_js_inc.jsp" %>

        <liferay:include page="/html/js/calendar/calendar_js_box_ext.jsp" flush="true">
        <liferay:param name="calendar_num" value="2" />
        </liferay:include>

        function <portlet:namespace />setCalendarDate_0 (year, month, day) {
        var textbox = document.getElementById('lastModDateFrom');
        textbox.value = month + '/' + day + '/' + year;
        }

        function <portlet:namespace />setCalendarDate_1 (year, month, day) {
        var textbox = document.getElementById('lastModDateTo');
        textbox.value = month + '/' + day + '/' + year;
        }

        dojo.addOnLoad(function() {
        var menu = new dijit.Menu({
            style: "display: none;"
        });
        var menuItem1 = new dijit.MenuItem({
            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Content" )) %>",
                        iconClass: "plusIcon",
                        onClick: function() {
                addNewContentlet();
            }
        });
        menu.addChild(menuItem1);

        var menuItem2 = new dijit.MenuItem({
            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Import-Content" )) %>",
                        iconClass: "uploadIcon",
                        onClick: function() {
                window.location='/c/portal/layout?p_l_id=<%= layout.getId() %>&dm_rlout=1&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&_EXT_11_struts_action=/ext/contentlet/import_contentlets';
            }
        });
        menu.addChild(menuItem2);

        var button = new dijit.form.ComboButton({
            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Content" )) %>",
                        iconClass: "plusIcon",
                        dropDown: menu,
                        onClick: function() {
                addNewContentlet();
            }
        });
        dojo.byId("addNewMenu").appendChild(button.domNode);
    });
</script>


<!-- START Button Row -->
        <div class="buttonBoxLeft">
	        <b><%=LanguageUtil.get(pageContext, "Type") %>:</b>
	        <span id="structSelectBox"></span>
        </div>

        <div class="buttonBoxRight">
             <div id="addNewMenu"></div>
        </div>

<!-- END Button Row -->

<form method="Post" action="" id="search_form" onsubmit="doSearch();return false;">

<input type="hidden" name="fullCommand" id="fullCommand" value="">
<input type="hidden" name="structureInode" id="structureInode" value="">
<input type="hidden" name="fieldsValues" id="fieldsValues" value="">
<input type="hidden" name="categoriesValues" id="categoriesValues" value="">
<input type="hidden" name="showDeleted" id="showDeleted" value="">
<input type="hidden" name="filterSystemHost" id="filterSystemHost" value="">
<input type="hidden" name="filterLocked" id="filterLocked" value="">
<input type="hidden" name="currentPage" id="currentPage" value="">
<input type="hidden" name="currentSortBy" id="currentSortBy" value="">
<input type="hidden" value="" name="lastModDateFrom"  id="lastModDateFrom" size="10" maxlength="10" readonly="true"/>
<input type="hidden" value="" name="lastModDateTo"  id="lastModDateTo" size="10" maxlength="10" readonly="true"/>
<input type="hidden" name="structureVelocityVarNames" id="structureVelocityVarNames" value="<%= structureVelocityVarNames %>">
<input type="hidden" name="structureInodesList" id="structureInodesList" value="<%= structureInodesList %>">
<input type="hidden" name="hostField" id="hostField" value="<%= conHostValue %>"/>
<input type="hidden" name="folderField" id="folderField" value="<%= conFolderValue %>"/>

<!-- START Split Screen -->
<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" style="height:400px;" id="borderContainer" class="shadowBox headerBox">

<!-- START Left Column -->
        <div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width: 350px;" class="lineRight">

                <div id="filterWrapper" style="overflow-y:auto; overflow-x:hidden;margin:43px 0 0 5px;">

                        <% List<Structure> readStructs = StructureFactory.getStructuresWithReadPermissions(user, true);  %>
                        <% if((readStructs.size() == 0)){%>
                                <div align="center" style="text-align:center;">
                                        <dt><FONT COLOR="#FF0000"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-Structure-Read-Permissions" )) %></FONT></dt>
                                </div>
                        <%}%>

                        <!-- START Advanced Search-->
                        <div id="advancedSearch">
                                <dl>
                                        <%if (languages.size() > 1) { %>
                                                <dt><%= LanguageUtil.get(pageContext, "Language") %>:</dt>
                                                <dd>
                                                    <div id="combo_zone2" style="width:215px; height:20px;">
                                                        <input id="language_id"/>
                                                    </div>
                                                    <script>
														<%StringBuffer buff = new StringBuffer();
														  // http://jira.dotmarketing.net/browse/DOTCMS-6148
														  buff.append("{identifier:'id',imageurl:'imageurl',label:'label',items:[");

														  String imageURL="/html/images/languages/all.gif";
														  String style="background-image:url(URLHERE);width:16px;height:11px;display:inline-block;vertical-align:middle;margin:3px 5px 3px 2px;";
														  buff.append("{id:'0',value:'',lang:'All',imageurl:'"+imageURL+"',label:'<span style=\""+style.replaceAll("URLHERE",imageURL)+"\"></span>All'}");
														  for (Language lang : languages) {
															  imageURL="/html/images/languages/" + lang.getLanguageCode()  + "_" + lang.getCountryCode() +".gif";
															  final String display=lang.getLanguage() + " - " + lang.getCountry().trim();
															  buff.append(",{id:'"+lang.getId()+"',");
															  buff.append("value:'"+lang.getId()+"',");
															  buff.append("imageurl:'"+imageURL+"',");
															  buff.append("lang:'"+display+"',");
															  buff.append("label:'<span style=\""+style.replaceAll("URLHERE",imageURL)+"\"></span>"+display+"'}");
														  }
														  buff.append("]}");%>

														function updateSelectBoxImage(myselect) {
															var imagestyle = "url('" + myselect.item.imageurl + "')";
															var selField = dojo.query('#combo_zone2 div.dijitInputField')[0];
															dojo.style(selField, "backgroundImage", imagestyle);
															dojo.style(selField, "backgroundRepeat", "no-repeat");
															dojo.style(selField, "padding", "0px 0px 0px 25px");
															dojo.style(selField, "backgroundColor", "transparent");
															dojo.style(selField, "backgroundPosition", "3px 6px");
														}

															var storeData=<%=buff.toString()%>;
															var langStore = new dojo.data.ItemFileReadStore({data: storeData});
															var myselect = new dijit.form.FilteringSelect({
																	 id: "language_id",
																	 name: "language_id",
																	 value: '',
																	 required: true,
																	 store: langStore,
																	 searchAttr: "lang",
																	 labelAttr: "label",
																	 labelType: "html",
																	 onChange: function() {
																		 var el=dijit.byId('language_id');
																		 updateSelectBoxImage(el);
																	 },
																	 labelFunc: function(item, store) { return store.getValue(item, "label"); }
																},
																dojo.byId("language_id"));

																<%if(languageId.equals("0")) {%>
																	myselect.setValue('<%=languages.get(0).getId()%>');
																<%} else {%>
																	myselect.setValue('<%=languageId%>');
																<%}%>

													</script>
                                                </dd>
                                        <%} else { %>
                                                <% long langId = languages.get(0).getId(); %>
                                                <input type="hidden" name="language_id" id="language_id" value="<%= langId %>">
                                        <% } %>
                                </dl>
								<div class="clear"></div>

                                <!-- Ajax built search fields  --->
                                        <div id="search_fields_table"></div>
										<div class="clear"></div>
                                <!-- /Ajax built search fields  --->

                                <dl>
                                        <%--
                                                <dt><%= LanguageUtil.get(pageContext, "Identifier") %>:</dt>
                                                <dd><input type="hidden" value="" name="Identifier" dojoType="dijit.form.TextBox" id="Identifier" size="10"/></dd>
                                         --%>
                                         <input type="hidden" value="" name="Identifier" id="Identifier" size="10"/>
                                         <input type="hidden" value="" name="allSearchedContentsInodes" id="allSearchedContentsInodes" dojoType="dijit.form.TextBox"/>
                                         <input type="hidden" value="" name="allUncheckedContentsInodes" id="allUncheckedContentsInodes" dojoType="dijit.form.TextBox"/>
                                        <dt><%= LanguageUtil.get(pageContext, "Archived-only") %>:</dt>
                                        <dd><input type="checkbox" dojoType="dijit.form.CheckBox" id="showDeletedCB" onclick="displayArchiveButton();doSearch(1);" <%=showDeleted?"checked=\"checked\"":""%>></dd>
                                </dl>
								<div class="clear"></div>

                                <dl id="filterSystemHostTable" style="display: ">
                                    <dt><%= LanguageUtil.get(pageContext, "Exclude-system-host") %>:</dt>
                                    <dd>
                                       <input type="checkbox" dojoType="dijit.form.CheckBox" id="filterSystemHostCB" onclick="doSearch(1);" <%=filterSystemHost?"checked=\"checked\"":""%>>
                                   </dd>
                                </dl>
								<div class="clear"></div>

                                <dl>
                                    <dt><%= LanguageUtil.get(pageContext, "Locked-only") %>:</dt>
                                        <dd>
                                         <input type="checkbox" dojoType="dijit.form.CheckBox" id="filterLockedCB" onclick="doSearch(1);" <%=filterLocked?"checked=\"checked\"":""%>>
                                   </dd>
                                </dl>
								<div class="clear"></div>


                                <dl id="search_categories_list"></dl>
								<div class="clear"></div>
                        </div>
                        <!-- END Advanced Search-->



                        <div class="buttonRow">
                                <span id="searchButton"></span>

                                <button dojoType="dijit.form.ComboButton" id="searchButton" optionsTitle='createOptions' onClick="doSearch();return false;" iconClass="searchIcon" title="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "search")) %>">
                                        <span><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "search")) %></span>
                                        <div dojoType="dijit.Menu" style="display: none;" onClick="doSearch();return false;">
                                                <div dojoType="dijit.MenuItem"  iconClass="searchIcon" onClick="doSearch();return false;"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "search")) %></div>
                                                <div dojoType="dijit.MenuItem" iconClass="queryIcon" onClick="showHideQuery()"><%= LanguageUtil.get(pageContext, "Show-Query")%></div>
                                        </div>
                                </button>

                                <button dojoType="dijit.form.Button" id="clearButton" onClick="clearSearch();doSearch();" iconClass="resetIcon">
                                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear-Search")) %>
                                </button>
                        </div>

                </div>
        </div>
<!-- END Left Column -->


<!-- START Right Column -->
        <div dojoType="dijit.layout.ContentPane" splitter="true" region="center">

                <div id="contentWrapper" style="overflow-y:auto; overflow-x:auto;margin:35px 0 0 0;">
                        <div id="metaMatchingResultsDiv" style="display:none;padding-top:7px;">
                                <!-- START Listing Results -->
                                        <input type="hidden" name="referer" value="<%=referer%>">
                                        <input type="hidden" name="cmd" value="prepublish">
                                        <div id="matchingResultsDiv" style="display: none"></div>
                                        <table id="results_table" class="listingTable"></table>
                                        <div id="results_table_popup_menus"></div>
                                        <div class="clear"></div>
                                <!-- END Listing Results -->
                        </div>

                        <!-- Start Pagination -->
                                <div class="yui-gb buttonRow">
                                        <div class="yui-u first" style="text-align:left; width: 10%;">
                                                <div id="previousDiv" style="display: none;">
                                                        <button dojoType="dijit.form.Button" onClick="previousPage();return false;" iconClass="previousIcon" id="previousDivButton">
                                                                <%= LanguageUtil.get(pageContext, "Previous")%>
                                                        </button>
                                                </div>&nbsp;
                                        </div>
                                        <div id="pagesdiv" class="yui-u" style="width:75%;">
                                                <div id="matchingResultsBottomDiv"></div>
                                        </div>
                                        <div class="yui-u" style="text-align:right; width: 10%;">
                                                <div id="nextDiv" style="display: none;">
                                                        <button dojoType="dijit.form.Button" onClick="nextPage();return false;" iconClass="nextIcon" id="nextDivButton">
                                                                <%= LanguageUtil.get(pageContext, "Next")%>
                                                        </button>
                                                </div>&nbsp;
                                        </div>
                                </div>
                        <!-- END Pagination -->
                        <div class="clear"></div>
                </div>

                <%boolean canReindexContentlets = APILocator.getRoleAPI().doesUserHaveRole(user,APILocator.getRoleAPI().loadRoleByKey(Role.CMS_POWER_USER))|| com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());%>
                <div class="clear"></div>

                <!-- START Buton Row -->
                        <div class="buttonRow">
                                <div id="archiveButtonDiv" style="display:none">
                                        <button dojoType="dijit.form.Button" id="unArchiveButton" onClick="unArchiveSelectedContentlets()" iconClass="unarchiveIconDis" disabled="true" >
                                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Un-Archive")) %>
                                        </button>

                                        <button dojoType="dijit.form.Button" id="deleteButton" onClick="deleteSelectedContentlets()" iconClass="deleteIconDis" disabled="true">
                                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete"))%>
                                        </button>


                                         <button dojoType="dijit.form.Button" id="archiveUnlockButton" onClick="unlockSelectedContentlets()" iconClass="unlockIconDis" disabled="true">
                                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock"))%>
                                        </button>

                                        <% if(canReindexContentlets){ %>
                                                <button dojoType="dijit.form.Button" id="archiveReindexButton" onClick="reindexSelectedContentlets()" iconClass="reindexIconDis">
                                                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reindex")) %>
                                                </button>
                                        <% } %>
                                </div>

                                <div id="unArchiveButtonDiv">
                                        <button dojoType="dijit.form.Button" id="publishButton"  onClick="publishSelectedContentlets()" iconClass="publishIconDis" disabled="true">
                                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>
                                        </button>

                                        <button dojoType="dijit.form.Button"  id="unPublishButton" onClick="unPublishSelectedContentlets()" iconClass="unpublishIconDis" disabled="true">
                                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish")) %>
                                        </button>

                                        <button dojoType="dijit.form.Button" id="archiveButton" onClick="archiveSelectedContentlets()" iconClass="archiveIconDis" disabled="true">
                                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive"))%>
                                        </button>

                                         <button dojoType="dijit.form.Button" id="unlockButton" onClick="unlockSelectedContentlets()" iconClass="unlockIconDis" disabled="true">
                                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock"))%>
                                        </button>

                                        <% if(canReindexContentlets){ %>
                                                <button dojoType="dijit.form.Button" id="reindexButton" onClick="reindexSelectedContentlets()" iconClass="reindexIconDis" disabled="true">
                                                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reindex")) %>
                                                </button>
                                        <% } %>
                                </div>
                        </div>
                <!-- END Buton Row -->
        </div>
    </div>
<!-- END Right Column -->

</div>
<!-- END Right Column -->



<!-- START Show Query -->
        <div id="queryDiv" dojoType="dijit.Dialog" style="display: none;">
                <div id="queryResults"></div>
        </div>
<!-- END Show Query -->

<!-- START Search Hint -->
        <div id="hintsdiv" dojoType="dijit.Dialog" style="display: none">
                <b><%= LanguageUtil.get(pageContext, "Search-Hints") %></b>
                <ul style="list-style:none; margin:0px; padding:0px;">
                        <li><%= LanguageUtil.get(pageContext, "message.contentlet.hints.text1") %></li>
                        <li><%= LanguageUtil.get(pageContext, "message.contentlet.hints.text2") %></li>
                        <li><%= LanguageUtil.get(pageContext, "message.contentlet.hints.text3") %></li>
                        <li><%= LanguageUtil.get(pageContext, "message.contentlet.hints.text4") %></li>
                </ul>
        </div>
<!-- START Search Hint -->


<div id="popups"></div>

<%if(UtilMethods.isSet(structureSelected) && structure.getStructureType()==Structure.STRUCTURE_TYPE_FORM){ %>
        <input type="hidden" name="contype" value="3"/>
<% } %>

</form>

<div class="messageZone" id="messageZone" style="display: none;">
  <%= LanguageUtil.get(pageContext, "Loading")%>...
</div>

<script type="text/javascript">
resizeBrowser();
</script>
