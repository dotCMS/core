<%@page import="com.dotmarketing.util.PortletID"%>
<%@page import="com.dotcms.repackage.bsh.This"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@ include file="/html/portlet/ext/contentlet/init.jsp" %>
<%@ include file="/html/portlet/ext/remotepublish/init.jsp" %>

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
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>

<iframe id="AjaxActionJackson" name="AjaxActionJackson" style="border:0; width:0; height:0;"></iframe>
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
    boolean filterUnpublish = false;
    int currpage = 1;
    String orderBy = "modDate desc";
    Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
    String languageId = String.valueOf(defaultLang.getId());
    if(session.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED)!= null){
    	languageId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED);
    }



	String structureSelected = "";
	if(UtilMethods.isSet(request.getParameter("structure_id"))){
		structureSelected=request.getParameter("structure_id");
	}

	if (lastSearch != null && !UtilMethods.isSet(structureSelected)) {
		String ssstruc = (String)session.getAttribute("selectedStructure");
		if(session.getAttribute("selectedStructure") != null && CacheLocator.getContentTypeCache().getStructureByInode((String)session.getAttribute("selectedStructure")) !=null){
		        structure = CacheLocator.getContentTypeCache().getStructureByInode((String)session.getAttribute("selectedStructure"));
		        if(structures.contains(structure)){
		                structureSelected = structure.getInode();
		        }else{
		                session.removeAttribute("selectedStructure");

		                structureSelected = null;;
		        }
		}
		if(lastSearch.get("fieldsSearch") != null){
			fieldsSearch = (Map<String, String>) lastSearch.get("fieldsSearch");
		}
		if(lastSearch.get("categories") != null){
			categories = (List<String>) lastSearch.get("categories");
		}
		if(UtilMethods.isSet(lastSearch.get("showDeleted"))){
			showDeleted = (Boolean) lastSearch.get("showDeleted");
		}
		if(UtilMethods.isSet(lastSearch.get("filterSystemHost"))){
			filterSystemHost = (Boolean) lastSearch.get("filterSystemHost");
		}
		if(UtilMethods.isSet(lastSearch.get("filterLocked"))){
			filterLocked = (Boolean) lastSearch.get("filterLocked");
		}
		if(lastSearch.get("filterUnpublish")!=null)
		    filterUnpublish = (Boolean) lastSearch.get("filterUnpublish");
		if(UtilMethods.isSet(lastSearch.get("page"))){
			currpage = (Integer) lastSearch.get("page");
		}
		if(UtilMethods.isSet(lastSearch.get("orderBy"))){
			orderBy = (String) lastSearch.get("orderBy");
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

        if (!InodeUtils.isSet(structureSelected) || !structures.contains(CacheLocator.getContentTypeCache().getStructureByInode(structureSelected))) {

                structureSelected = "_all";

        }


        List<Field> fields = new ArrayList<Field>();
        try{
        	fields = FieldsCache.getFieldsByStructureInode(structureSelected);
        }
        catch(Exception e){
        	Logger.debug(this.getClass(), e.getMessage());
        }
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

        if (fieldsSearch == null ||  !UtilMethods.isSet(fieldsSearch.get("conHost")) || hasNoSearcheableHostFolderField) {
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



        String _allValue = (UtilMethods.webifyString(fieldsSearch.get("_all")).endsWith("*")) ? UtilMethods.webifyString(fieldsSearch.get("_all")).substring(0,UtilMethods.webifyString(fieldsSearch.get("_all")).length()-1) : UtilMethods.webifyString(fieldsSearch.get("_all"));

		String[] strTypeNames = new String[]{"",LanguageUtil.get(pageContext, "Content"),
				LanguageUtil.get(pageContext, "Widget"),
				LanguageUtil.get(pageContext, "Form"),
				LanguageUtil.get(pageContext, "File"),
				LanguageUtil.get(pageContext, "HTMLPage"),
				LanguageUtil.get(pageContext, "Persona"),
				LanguageUtil.get(pageContext, "VanityURL"),
				LanguageUtil.get(pageContext, "KeyValue"),
				};

		boolean enterprise = LicenseUtil.getLevel() > 199;

		PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
		List<PublishingEndPoint> sendingEndpointsList = pepAPI.getReceivingEndPoints();
		boolean sendingEndpoints = UtilMethods.isSet(sendingEndpointsList) && !sendingEndpointsList.isEmpty();

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
<script type='text/javascript' src='/html/js/scriptaculous/prototype.js'></script>
<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/CategoryAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/ContentletAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>


<jsp:include page="/html/portlet/ext/folders/menu_actions_js.jsp" />

<script type="text/javascript">

	var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');

    var dataItems = {
        identifier: "name",
        label: "label",
        items: [
				<%if(request.getAttribute("SHOW_FORMS_ONLY") == null){%>
                {
                    name: "_all",
                    label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All" )) %>",
                    textLabel: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All" )) %>"
                },
                <%}%>

                <%	boolean started = false;
					int baseType=0;
					for(Structure s : structures){
                	
                	String labelAndIcon = (s.getStructureType()==1) 
                  	    ? "<span class='contentIcon'></span>" 
                  	    	: (s.getStructureType()==2) 
                  	    		? "<span class='gearIcon'></span>" 
                          	    	: (s.getStructureType()==3) 
                      	    			? "<span class='fa-columns'></span>" 
                                  	    	: (s.getStructureType()==4) 
                          	    			? "<span class='fileIcon'></span>" 
                                      	    	: (s.getStructureType()==5) 
                              	    			? "<span class='pageIcon'></span> " 
                                          	    	: (s.getStructureType()==6) 
                                  	    			? "<span class='personaIcon'></span>" 
                                  	    				:"<span class='blankIcon'></span>";
                
                
                
					labelAndIcon+="&nbsp; &nbsp;" + UtilMethods.javaScriptify(s.getName());
					if(s.getStructureType() != baseType){
					  labelAndIcon = "<div style='height:1px;margin:-1px -10px 0px -10px;background:silver;'></div>" + labelAndIcon;
					  baseType = s.getStructureType();
					}
                %>
                        <%=(started) ? "," :""%>
                        {
                            name: "<%=s.getInode()%>",
                            label: "<%=labelAndIcon %>",
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
                        doSearch(null, "<%=orderBy%>");
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
               	window.location='/c/portal/layout?p_l_id=<%= layout.getId() %>&dm_rlout=1&p_p_id=<%=PortletID.CONTENT%>&p_p_action=1&p_p_state=maximized&_<%=PortletID.CONTENT%>_struts_action=/ext/contentlet/import_contentlets&selectedStructure=' + document.getElementById("structureInode").value;
            }
        });
        menu.addChild(menuItem2);


    });
</script>
<form method="Post" action="" id="search_form" onsubmit="doSearch();return false;">

    <input type="hidden" name="fullCommand" id="fullCommand" value="">
    <input type="hidden" name="expiredInodes" id="expiredInodes" value=""/>
    <input type="hidden" name="expireDateReset" id="expireDateReset" value=""/>
    <input type="hidden" name="luceneQuery" id="luceneQuery" value="">
    <input type="hidden" name="structureInode" id="structureInode" value="">
    <input type="hidden" name="fieldsValues" id="fieldsValues" value="">
    <input type="hidden" name="categoriesValues" id="categoriesValues" value="">
    <input type="hidden" name="showDeleted" id="showDeleted" value="<%= showDeleted %>">
    <input type="hidden" name="filterSystemHost" id="filterSystemHost" value="<%= filterSystemHost %>">
    <input type="hidden" name="filterLocked" id="filterLocked" value="<%= filterLocked %>">
    <input type="hidden" name="filterUnpublish" id="filterUnpublish" value="<%= filterUnpublish %>">
    <input type="hidden" name="currentPage" id="currentPage" value="">
    <input type="hidden" name="currentSortBy" id="currentSortBy" value="modDate desc">
    <input type="hidden" value="" name="lastModDateFrom"  id="lastModDateFrom" size="10" maxlength="10" readonly="true"/>
    <input type="hidden" value="" name="lastModDateTo"  id="lastModDateTo" size="10" maxlength="10" readonly="true"/>
    <input type="hidden" name="structureVelocityVarNames" id="structureVelocityVarNames" value="<%= structureVelocityVarNames %>">
    <input type="hidden" name="structureInodesList" id="structureInodesList" value="<%= structureInodesList %>">
    <input type="hidden" name="hostField" id="hostField" value="<%= conHostValue %>"/>
    <input type="hidden" name="folderField" id="folderField" value="<%= conFolderValue %>"/>
    <input type="hidden" value="" name="Identifier" id="Identifier" size="10"/>
    <input type="hidden" value="" name="allSearchedContentsInodes" id="allSearchedContentsInodes" dojoType="dijit.form.TextBox"/>
    <input type="hidden" value="" name="allUncheckedContentsInodes" id="allUncheckedContentsInodes" dojoType="dijit.form.TextBox"/>
    <!-- START Split Screen -->
    <div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer">

        <!-- START Left Column -->
        <div dojoType="dijit.layout.ContentPane" id="filterWrapper" splitter="false" region="leading" style="width: 200px;" class="portlet-sidebar-wrapper" >
            <div class="portlet-sidebar">
                <% List<Structure> readStructs = StructureFactory.getStructuresWithReadPermissions(user, true);  %>
                <% if((readStructs.size() == 0)){%>
                    <div align="center" style="text-align:center;">
                        <dt><FONT COLOR="#FF0000"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-Structure-Read-Permissions" )) %></FONT></dt>
                    </div>
                <%}%>

                <!-- START Advanced Search-->
                <div id="advancedSearch">
                    <dl class="vertical">
                        <dt><label><%=LanguageUtil.get(pageContext, "Type") %>:</label></dt>
                        <dd><span id="structSelectBox"></span></dd>
                        <div class="clear"></div>

                        <dt><label><%= LanguageUtil.get(pageContext, "Search") %>:</label></dt>
                        <dd><input type="text" dojoType="dijit.form.TextBox" tabindex="1" onKeyUp='doSearch()' name="allFieldTB" id="allFieldTB" value="<%=_allValue %>"></dd>
                    </dl>

                    <div id="advancedSearchOptions" style="height:0px;overflow: hidden">

                        <%if (languages.size() > 1) { %>
                            <dl class="vertical">
                                <!-- Language search fields  --->
                                <dt><label><%= LanguageUtil.get(pageContext, "Language") %>:</label></dt>
                                <dd>
                                    <div id="combo_zone2">
                                        <input id="language_id"/>
                                    </div>

                                    <%@include file="languages_select_inc.jsp" %>
                                </dd>
                            </dl>
                        <%} else { %>
                            <% long langId = languages.get(0).getId(); %>
                            <input type="hidden" name="language_id" id="language_id" value="<%= langId %>">
                        <% } %>


                        <!-- Ajax built search fields  --->
                        <div id="search_fields_table"></div>
                        <div class="clear"></div>
                        <!-- /Ajax built search fields  --->

                        <!-- Ajax built Categories   --->
                        <dl class="vertical" id="search_categories_list"></dl>
                        <div class="clear"></div>
                        <!-- /Ajax built Categories   --->

                        <dl class="vertical">
                            <dt><label><%= LanguageUtil.get(pageContext, "Show") %>:</label></dt>
                            <dd>
                                <select name="showingSelect" onchange='doSearch(1);displayArchiveButton()'  id="showingSelect" dojoType="dijit.form.FilteringSelect">
                                    <option value="all" <% if (!showDeleted && !filterLocked && !filterUnpublish) { %> selected <% } %>><%= LanguageUtil.get(pageContext, "All") %></option>
                                    <option value="locked" <% if (filterLocked) { %> selected <% } %>><%= LanguageUtil.get(pageContext, "Locked") %></option>
                                    <option value="unpublished" <% if (filterUnpublish) { %> selected <% } %>><%= LanguageUtil.get(pageContext, "Unpublished") %></option>
                                    <option value="archived" <% if (showDeleted) { %> selected <% } %>><%= LanguageUtil.get(pageContext, "Archived") %></option>
                                </select>
                            </dd>
                        </dl>

                        <div class="clear"></div>

                        <dl class="radio-check-one-line" id="filterSystemHostTable">
                            <dt><label for="filterSystemHostCB"><%= LanguageUtil.get(pageContext, "Exclude-system-host") %></label></dt>
                            <dd>
                                <div class="checkbox">
                                    <input type="checkbox" dojoType="dijit.form.CheckBox" id="filterSystemHostCB" onclick="doSearch(1);" <%=filterSystemHost?"checked=\"checked\"":""%>>
                                </div>
                            </dd>
                        </dl>

                        <div id="measureTheHeightOfSearchTools" class="clear"></div>
                    </div>

                </div>
                <!-- END Advanced Search-->

                <div class="buttonRow">
                    <dl class="vertical">
                        <dd>
                            <button dojoType="dijit.form.ComboButton" id="searchButton" optionsTitle='createOptions' onClick="doSearch();return false;" iconClass="searchIcon" title="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "search")) %>">
                                <span><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "search")) %></span>
                                <div dojoType="dijit.Menu" style="display: none;" onClick="doSearch();return false;">
                                    <div dojoType="dijit.MenuItem"  iconClass="searchIcon" onClick="doSearch();return false;"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "search")) %></div>
                                    <div dojoType="dijit.MenuItem" iconClass="queryIcon" onClick="showHideQuery()"><%= LanguageUtil.get(pageContext, "Show-Query")%></div>
                                </div>
                            </button>
                        </dd>
                    </dl>

                    <dl class="vertical">
                        <dd>
                            <button dojoType="dijit.form.Button" id="clearButton" onClick="clearSearch();doSearch();" iconClass="resetIcon" class="dijitButtonFlat">
                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear")) %>
                            </button>
                        </dd>
                    </dl>

                </div>

               <a href="javascript:toggleAdvancedSearchDiv()" class="advanced-search-button">
                    <div id="toggleDivText">
                        <%= LanguageUtil.get(pageContext, "Advanced") %>
                    </div>
               </a>

            </div>

        </div>
        <!-- END Left Column -->

        <!-- START Right Column -->
        <%boolean canReindexContentlets = APILocator.getRoleAPI().doesUserHaveRole(user,APILocator.getRoleAPI().loadRoleByKey(Role.CMS_POWER_USER))|| com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());%>

        <div dojoType="dijit.layout.ContentPane" splitter="true" region="center" class="portlet-content-search" id="contentWrapper" style="overflow-y:auto; overflow-x:auto;">
            <div class="portlet-main">
                <div id="metaMatchingResultsDiv" style="display:none;">
                    <!-- START Listing Results -->
                    <input type="hidden" name="referer" value="<%=referer%>">
                    <input type="hidden" name="cmd" value="prepublish">
                    <div class="portlet-toolbar">
                        <div class="portlet-toolbar__actions-primary">
                            <div data-dojo-type="dijit/form/DropDownButton">
                                <span><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Content" )) %></span>
                                <script type="text/javascript">
                                    function importContent() {
                                        window.location = '/c/portal/layout?p_l_id=<%= layout.getId() %>&dm_rlout=1&p_p_id=<%=PortletID.CONTENT%>&p_p_action=1&p_p_state=maximized&_<%=PortletID.CONTENT%>_struts_action=/ext/contentlet/import_contentlets&selectedStructure=' + document.getElementById('structureInode').value;
                                    }
                                </script>
                                <ul data-dojo-type="dijit/Menu" id="actionPrimaryMenu" style="display: none;">
                                    <li data-dojo-type="dijit/MenuItem" data-dojo-props="onClick:function() {addNewContentlet()}"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Content" )) %></li>
                                    <li data-dojo-type="dijit/MenuItem" data-dojo-props="onClick:importContent">
                                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Import-Content" )) %>
                                    </li>
                                </ul>
                            </div>
                        </div>
                        <div id="matchingResultsDiv" style="display: none" class="portlet-toolbar__info"></div>
                        <div class="portlet-toolbar__actions-secondary" id="portletActions">
                            <div id="archiveButtonDiv" style="display:none">
                                <div id="archiveDropDownButton" data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"actionIcon", class:"dijitDropDownActionButton"'>
                                    <span></span>

                                    <div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">
                                        <div id="unArchiveButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: unArchiveSelectedContentlets">
                                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Un-Archive")) %>
                                        </div>
                                        <div id="deleteButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: deleteSelectedContentlets">
                                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete"))%>
                                        </div>
                                        <div id="archiveUnlockButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: unlockSelectedContentlets">
                                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock"))%>
                                        </div>
                                        <% if(canReindexContentlets){ %>
                                            <div id="archiveReindexButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: reindexSelectedContentlets">
                                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reindex")) %>
                                            </div>
                                        <% } %>
                                    </div>
                                </div>
                            </div>
                            <div id="unArchiveButtonDiv">
                                <div id="unArchiveDropDownButton" data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"actionIcon", class:"dijitDropDownActionButton"'>
                                    <span></span>

                                    <div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">
                                        <div id="publishButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: publishSelectedContentlets">
                                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Publish")) %>
                                        </div>
                                        <div id="unPublishButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: unPublishSelectedContentlets">
                                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unpublish")) %>
                                        </div>
                                        <div id="archiveButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: archiveSelectedContentlets">
                                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Archive"))%>
                                        </div>
                                        <div data-dojo-type="dijit/MenuSeparator"></div>
                                        <% if ( enterprise ) { %>
                                            <% if ( sendingEndpoints ) { %>
                                                <div id="pushPublishButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: pushPublishSelectedContentlets">
                                                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remote-Publish")) %>
                                                </div>
                                            <% } %>
                                            <div id="addToBundleButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: addToBundleSelectedContentlets">
                                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-To-Bundle")) %>
                                            </div>
                                            <div data-dojo-type="dijit/MenuSeparator"></div>
                                        <% } %>
                                        <div id="unlockButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: unlockSelectedContentlets">
                                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unlock"))%>
                                        </div>
                                        <% if(canReindexContentlets){ %>
                                            <div id="reindexButton" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: reindexSelectedContentlets">
                                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Reindex")) %>
                                            </div>
                                        <% } %>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <table id="results_table" class="listingTable content-search__results-list"></table>
                    <div id="results_table_popup_menus"></div>
                    <!-- END Listing Results -->
                </div>

                <!-- Start Pagination -->
                <div class="portlet-pagination">
                    <div id="previousDiv" style="display: none;">
                        <button dojoType="dijit.form.Button" onClick="previousPage();return false;" iconClass="previousIcon" id="previousDivButton">
                            <%= LanguageUtil.get(pageContext, "Previous")%>
                        </button>
                    </div>&nbsp;
                    <div id="matchingResultsBottomDiv" class="portlet-pagination__results"></div>
                    <div id="nextDiv" style="display: none;">
                        <button dojoType="dijit.form.Button" onClick="nextPage();return false;" iconClass="nextIcon" id="nextDivButton">
                                <%= LanguageUtil.get(pageContext, "Next")%>
                        </button>
                    </div>&nbsp;
                </div>
            <!-- END Pagination -->
            </div>

        </div>
        <!-- END Right Column -->

        <!-- START Show Query -->
        <div id="queryDiv" dojoType="dijit.Dialog" class="content-search__show-query-dialog" style="display: none;padding-top:15px\9;">
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
            <input type="hidden" name="contentStructureType" value="3"/>
        <% } %>
    </div>

</form>

<div class="messageZone" id="messageZone" style="display: none;">
  <i class="loadingIcon"></i>
  <%= LanguageUtil.get(pageContext, "Loading")%>...
</div>

<div dojoType="dijit.Dialog" id="selectStructureDiv"  title='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Content" )) %>'>

	<table class="sTypeTable">
		<tr>
			<%int stType=0; %>
			<%int maxPerCol=Config.getIntProperty("EDIT_CONTENT_STRUCTURES_PER_COLUMN", 15); %>
			<td class="sTypeTd">
				<%int i=0; %>
				<%for( Structure struc : structures) {%>
					<%if(stType != struc.getStructureType()){ %>
						<% stType = struc.getStructureType(); %>
						<div class="sTypeHeader" id="sType<%=strTypeNames[stType] %>"><%=strTypeNames[stType] %></div>
					<%} %>
					<div class="sTypeItem" id="sType<%=struc.getInode() %>"><a href="javascript:addNewContentlet('<%=struc.getInode() %>');"><%=struc.getName() %></a></div>
					<%if(i++ == maxPerCol){ %>
						<%i=0; %>
						</td>
						<td valign="top" class="sTypeTd">
					<%} %>
				<%} %>
		    </td>
		</tr>
	</table>

</div>

<form id="remotePublishForm">
	<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="">
	<input name="remotePublishDate" id="remotePublishDate" type="hidden" value="">
	<input name="remotePublishTime" id="remotePublishTime" type="hidden" value="">
	<input name="remotePublishExpireDate" id="remotePublishExpireDate" type="hidden" value="">
	<input name="remotePublishExpireTime" id="remotePublishExpireTime" type="hidden" value="">
	<input name="iWantTo" id=iWantTo type="hidden" value="">
	<input name="whoToSend" id=whoToSend type="hidden" value="">
	<input name="bundleName" id=bundleName type="hidden" value="">
	<input name="bundleSelect" id=bundleSelect type="hidden" value="">
	<input name="forcePush" id=forcePush type="hidden" value="">
</form>
