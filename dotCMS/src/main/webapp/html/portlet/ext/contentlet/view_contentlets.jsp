<%@page import="com.dotcms.contenttype.transform.contenttype.StructureTransformer"%>
<%@page import="com.dotcms.contenttype.model.type.ContentType"%>
<%@page import="com.dotcms.content.elasticsearch.constants.ESMappingConstants"%>
<%@page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@ include file="/html/portlet/ext/contentlet/init.jsp" %>
<%@ include file="/html/portlet/ext/remotepublish/init.jsp" %>

<%@ page import="com.dotmarketing.business.CacheLocator" %>
<%@ page import="com.dotmarketing.cache.FieldsCache" %>
<%@ page import="com.dotmarketing.portlets.languagesmanager.model.Language" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Field" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>
<%@ page import="com.dotmarketing.util.Logger" %>
<%@ page import="com.dotmarketing.util.PortletID" %>
<%@ page import="com.dotcms.contenttype.exception.NotFoundInDbException" %>

<iframe id="AjaxActionJackson" name="AjaxActionJackson" style="border:0; width:0; height:0;"></iframe>
<%








    List<ContentType> contentTypes = (List<ContentType>)request.getAttribute ("contentSearchContentTypes");

    List<Structure> structures = new StructureTransformer(contentTypes).asStructureList();


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
    String orderBy = "score,modDate desc";
    Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
    String languageId = String.valueOf(defaultLang.getId());
    if(session.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED)!= null){
        languageId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED);
    }

    String structureSelected = null;
    final String variableName = (String) request.getParameter("filter");

    if(UtilMethods.isSet(variableName)){
        if (com.dotmarketing.beans.Host.HOST_VELOCITY_VAR_NAME.equals(variableName)){
            structureSelected = null;
        }else{
            try {
                ContentType filterContentType = APILocator.getContentTypeAPI(user).find(variableName);
                structureSelected = filterContentType != null ? filterContentType.id() : null;

            } catch (NotFoundInDbException e) {
                structureSelected = null;
            }
        }
    }else{
        structureSelected = (String) request.getAttribute("selectedStructure");
        if (structureSelected != null){
            try {
                ContentType contentType = APILocator.getContentTypeAPI(user).find(structureSelected);


                if (contentType != null && com.dotmarketing.beans.Host.HOST_VELOCITY_VAR_NAME.equals(contentType.variable()) ){
                    structureSelected = null;
                }
            } catch (NotFoundInDbException e) {
                structureSelected = null;
            }
        }
    }

    String schemeSelected = "catchall";
    if(UtilMethods.isSet(session.getAttribute(ESMappingConstants.WORKFLOW_SCHEME))){
        schemeSelected = (String)session.getAttribute(ESMappingConstants.WORKFLOW_SCHEME);
    }

    String stepsSelected = "catchall";
    if(UtilMethods.isSet(session.getAttribute(ESMappingConstants.WORKFLOW_STEP))){
        stepsSelected = (String)session.getAttribute(ESMappingConstants.WORKFLOW_STEP);
    }


    if (lastSearch != null && !UtilMethods.isSet(structureSelected)) {
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


    if (!InodeUtils.isSet(structureSelected)) {
        structureSelected = "catchall";
    }

    String devices = (String) request.getParameter("devices");

    if (devices != null) {
        structureSelected = devices;
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



    String _allValue = (UtilMethods.webifyString(fieldsSearch.get("catchall")).endsWith("*")) ? UtilMethods.webifyString(fieldsSearch.get("catchall")).substring(0,UtilMethods.webifyString(fieldsSearch.get("catchall")).length()-1) : UtilMethods.webifyString(fieldsSearch.get("catchall"));

    String[] strTypeNames = new String[]{"",LanguageUtil.get(pageContext, "Content"),
            LanguageUtil.get(pageContext, "Widget"),
            LanguageUtil.get(pageContext, "Form"),
            LanguageUtil.get(pageContext, "File"),
            LanguageUtil.get(pageContext, "HTMLPage"),
            LanguageUtil.get(pageContext, "Persona"),
            LanguageUtil.get(pageContext, "VanityURL"),
            LanguageUtil.get(pageContext, "KeyValue"),
            LanguageUtil.get(pageContext, "DotAsset")
            ,
    };

    final boolean enterprise = (LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level);
    final PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
    final List<PublishingEndPoint> sendingEndpointsList = pepAPI.getReceivingEndPoints();
    final boolean sendingEndpoints = UtilMethods.isSet(sendingEndpointsList) && !sendingEndpointsList.isEmpty();
    final boolean canReindexContentlets = APILocator.getRoleAPI().doesUserHaveRole(user,APILocator.getRoleAPI().loadRoleByKey(Role.CMS_POWER_USER))|| com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());

    Map<String, String> initParams = (Map<String, String>) request.getAttribute("initParams");
    final String dataViewMode = initParams.getOrDefault("dataViewMode", "");

%>

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
            <%if(request.getAttribute("DONT_SHOW_ALL") == null){%>
            {
                name: "catchall",
                label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All" )) %>",
                textLabel: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All" )) %>"
            },
            <%}%>

            <%	boolean started = false;
                int baseType=0;
                for(final Structure contentType : structures){

                    //Ignore the Host structure in the content search
                    if (contentType.isHost()) {
                        continue;
                    }

                    String labelAndIcon = "<i class='material-icons'>" + contentType.getIcon() +"</i>";

                    String contentTypeName = UtilMethods.javaScriptify(contentType.getName());
                    labelAndIcon+= contentTypeName;
                    labelAndIcon = "<div class='label'>" + labelAndIcon + "<div>";
                    if(contentType.getStructureType() != baseType){
                        labelAndIcon = "<div class='separator'></div>" + labelAndIcon;
                      baseType = contentType.getStructureType();
                    }
            %>
            <%=(started) ? "," :""%>
                {
                    name: "<%=contentType.getInode()%>",
                    label: "<%=labelAndIcon %>",
                    textLabel: "<%=contentTypeName %>"
                }
            <%started = true;%>
            <%}%>

        ]
    };

    var dojoStore = new dojo.data.ItemFileReadStore({
        data: dataItems
    });


    var  dojoRelationshipsStore = new dojo.data.ItemFileReadStore({
        data:  {
            identifier  : "id",
            label: "label",
            items: []
        }
    });


    function reloadRelationshipBox(box, relatedType){
    	var search = box.attr("displayedValue");

        //whitespaces are escaped
        var boxValue = search == "" ? "*" : "*" +search.trim().replace(/\s/g, "\\\\\\\\ ") + "*";
        var limit=box.pageSize;
        if (relatedType.indexOf(".") != -1){
            relatedType = relatedType.split('.')[0];
        }

    	var tmpl = `
    		{ "query" :
	    	    {
	    	        "query_string" :
	    	        {
	    	            "query" : "+contentType:${relatedType}  +(inode:${boxValue} title:${boxValue} identifier:${boxValue})"
	    	        }
	    	    },
	    	    "sort" : {"moddate":"desc"},
	    	    "size":${limit},
	    	    "from":0
	    	}`;

         var url = "/api/es/search";

         var xhrArgs = {
             url: url,
             postData: tmpl,
             headers: {
                 "Accept" : "application/json",
                 "Content-Type" : "application/json"
              },
             handleAs : "json",
             load: function(data) {

                 let dataItems = {
                     identifier  : "id",
                     label: "label",
                     items: []
                 };

                 for (let i=0; i<data.contentlets.length;++i) {
                     let entity = data.contentlets[i];
                     dataItems.items[i] = { label: entity.title, id: (entity.identifier + " " + entity.inode), searchMe : entity.title + " " + entity.identifier + " " + entity.inode };
                 }

                 dojoRelationshipsStore = new dojo.data.ItemFileReadStore({
                     data: dataItems
                 });

                 box.store=dojoRelationshipsStore;
                 box.set( 'store',dojoRelationshipsStore);
                 box.startup();
             }
         }
         dojo.xhrPost(xhrArgs);
     }



    // Workflow Schemes
    var dojoSchemeStore = null;

    function reloadSchemeStore (aFilteringSelect, structureInode) {

        var xhrArgs = {
            url: "/api/v1/workflow/schemes?showArchive=false" + ((null != structureInode && structureInode && "catchall"!=structureInode)?"&contentTypeId="+structureInode:""),
            handleAs: "json",
            load: function(data) {

                let dataItems = {
                    identifier: "name",
                    label: "label",
                    items: [ { name: "catchall", label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All" )) %>",
                        textLabel: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All" )) %>" }]
                };

                for (let i=0; i<data.entity.length;++i) {

                    let schemeEntity = data.entity[i];
                    dataItems.items[dataItems.items.length] = { name: schemeEntity.id, label: schemeEntity.name, textLabel: schemeEntity.name };
                }

                dojoSchemeStore = new dojo.data.ItemFileReadStore({
                    data: dataItems
                });

                // Update widget - either the provided one or find by ID
                var widget = aFilteringSelect || dijit.byId('scheme_id');
                if (null != widget) {
                    widget.set('store', dojoSchemeStore);
                }
            }
        }

        dojo.xhrGet(xhrArgs);
    }

    function reloadSchemeStoreFromStructureInode (aFilteringSelect) {

        reloadSchemeStore(aFilteringSelect, dijit.byId("structure_inode")?dijit.byId("structure_inode").getValue():null);
    }

    // Workflow Steps
    var dojoStepsStore = null;

    function reloadStepStore (aFilteringSelect, schemeId) {

        if ("catchall" == schemeId) {

            dojoStepsStore = new dojo.data.ItemFileReadStore({
                data: {
                    identifier: "name",
                    label: "label",
                    items: [ { name: "catchall", label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All" )) %>",
                        textLabel: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All" )) %>" }]
                }
            });

            if (null != aFilteringSelect) {

                aFilteringSelect.set('store', dojoStepsStore);
            }
        } else {

            var xhrArgs = {
                url: "/api/v1/workflow/schemes/" + schemeId + "/steps",
                handleAs: "json",
                load: function(data) {

                    let dataItems = {
                        identifier: "name",
                        label: "label",
                        items: [ { name: "catchall", label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All" )) %>",
                            textLabel: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All" )) %>" }]
                    };

                    dataItems.items[dataItems.items.length] = {
                            name: "<%=ESMappingConstants.WORKFLOW_CURRENT_STEP_NOT_ASSIGNED_VALUE%>",
                            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "workflow.notassigned" )) %>",
                            textLabel: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "workflow.notassigned" )) %>"
                        };

                    for (let i=0; i<data.entity.length;++i) {

                        let stepEntity = data.entity[i];
                        dataItems.items[dataItems.items.length] = { name: stepEntity.id, label: stepEntity.name, textLabel: stepEntity.name };
                    }

                    dojoStepsStore = new dojo.data.ItemFileReadStore({
                        data: dataItems
                    });

                    if (null != aFilteringSelect) {

                        aFilteringSelect.set('store', dojoStepsStore);
                    }
                }
            }

            dojo.xhrGet(xhrArgs);
        }

    }

    function reloadStepStoreFromSchemeId (aFilteringSelect) {

        reloadStepStore(aFilteringSelect, dijit.byId("scheme_id")?dijit.byId("scheme_id").getValue():null);
    }

    reloadStepStore(null, "<%=schemeSelected%>");

    dojo.addOnLoad(function() {


        function myLabelFunc(item) {
            return item.textLabel + "";
        }
        // content type select box
        var fs = new dijit.form.FilteringSelect({
                id: "structure_inode",
                name: "structure_inode_select",
                value: "<%=structureSelected%>",
                store: dojoStore,
                searchAttr: "textLabel",
                labelAttr: "label",
                labelType: "html",


                onChange: function(){

                    fsSchemes.set('value', 'catchall');
                    fsSteps.set('value', 'catchall');
                    reloadSchemeStoreFromStructureInode(fsSchemes);
                    structureChanged(true);
                    doSearch(1, "<%=orderBy%>");
                }
            },
            dojo.byId("structSelectBox"));

        // scheme select box
        var fsSchemes = new dijit.form.FilteringSelect({
                id: "scheme_id",
                name: "scheme_id_select",
                value: "<%=schemeSelected%>",
                searchAttr: "textLabel",
                labelAttr: "label",
                labelType: "html",

                onChange: function(){

                    fsSteps.set('value', 'catchall');
                    reloadStepStoreFromSchemeId (fsSteps)
                    structureChanged(true);
                    doSearch(1, "<%=orderBy%>");
                }
            },
            dojo.byId("schemeSelectBox"));
        
        // Load the scheme data after widget is created
        reloadSchemeStore(fsSchemes, "<%=structureSelected%>");

        // step select box
        var fsSteps = new dijit.form.FilteringSelect({
                id: "step_id",
                name: "step_id_select",
                value: "<%=stepsSelected%>",
                store: dojoStepsStore,
                searchAttr: "textLabel",
                labelAttr: "label",
                labelType: "html",

                onChange: function(){

                    // todo: recargar con step_id
                    structureChanged(true);
                    doSearch(1, "<%=orderBy%>");
                }
            },
            dojo.byId("stepSelectBox"));


    })

    function initialLoad() {
        var urlParams = new URLSearchParams(window.location.href);
        portletId = urlParams.get('angularCurrentPortlet');

        var viewDisplayMode = '<%=dataViewMode%>';
        if (viewDisplayMode !== '') {
            doSearch(<%= currpage %>, "<%=orderBy%>", viewDisplayMode);
        } else {
            doSearch(<%= currpage %>, "<%=orderBy%>")
        }

        dijit.byId("searchButton").attr("disabled", false);
        dijit.byId("clearButton").setAttribute("disabled", false);

        togglePublish();
    }

</script>



<%@ include file="/html/portlet/ext/contentlet/view_contentlets_js_inc.jsp" %>


<script language="Javascript">

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
                addNewContentlet(null);
            }
        });
        menu.addChild(menuItem1);

        var menuItem2 = new dijit.MenuItem({
            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Import-Content" )) %>",
            iconClass: "uploadIcon",
            onClick: function() {
                window.location='/c/portal/layout?p_l_id=<%= layout.getId() %>&dm_rlout=1&p_p_id=<%=PortletID.CONTENT%>&p_p_action=1&p_p_state=maximized&_<%=PortletID.CONTENT%>_struts_action=/ext/contentlet/import_contentlets&selectedStructure=' + document.getElementById("structureInode").value + '&angularCurrentPortlet=' + portletId;
            }
        });
        menu.addChild(menuItem2);


    });
</script>


<%@ include file="/html/portlet/ext/contentlet/view_bulk_actions_inc.jsp" %>

<dot-asset-drop-zone>
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
    <input type="hidden" name="currentSortBy" id="currentSortBy" value="score,modDate desc">
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
                        <div class="clear"></div>
                       <%if(contentTypes!=null && contentTypes.size()==1){ %>
                           <input type="hidden" name="structure_inode_select" value="<%=structures.get(0).getInode()%>"/>
                       <%} else {%>
                           <dt><label><%=LanguageUtil.get(pageContext, "Type") %>:</label></dt>
                           <dd><span id="structSelectBox"></span></dd>
                           <div class="clear"></div>
                       <%} %>
                    </dl>

                    <div id="advancedSearchOptions" style="height:0px;overflow: hidden">


                        <dl class="vertical">
                            <dt><label><%= LanguageUtil.get(pageContext, "Workflow-Schemes") %>:</label></dt>
                            <dd><span id="schemeSelectBox"></span></dd>
                            <div class="clear"></div>
                        </dl>

                        <dl class="vertical">
                            <dt><label><%= LanguageUtil.get(pageContext, "Step") %>:</label></dt>
                            <dd><span id="stepSelectBox"></span></dd>
                            <div class="clear"></div>
                        </dl>

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
                                <!-- site_folder_field fields  --->
                                <div id="site_folder_field"></div>


                                <div class="clear"></div>
                                <!-- Ajax built search fields  --->
                                <div id="search_fields_table"></div>

                                <!-- Ajax built Categories   --->
                                <dl class="vertical" id="search_categories_list"></dl>
                                <div class="clear"></div>
                                <!-- /Ajax built Categories   --->

                                <dl class="vertical">
                                    <dt><label><%= LanguageUtil.get(pageContext, "Show") %>:</label></dt>
                                    <dd>
                                        <select name="showingSelect" onchange='doSearch(1);togglePublish()'  id="showingSelect" dojoType="dijit.form.FilteringSelect">
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
                            <button dojoType="dijit.form.Button" id="clearButton" onClick="clearSearch();" iconClass="resetIcon" class="dijitButtonFlat">
                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear")) %>
                            </button>
                        </dd>
                    </dl>

                </div>

                <a href="javascript:toggleAdvancedSearchDiv()" class="advanced-search-button">
                    <div id="toggleDivText">
                        <%= LanguageUtil.get(pageContext, "advanced") %>
                    </div>
                </a>

            </div>

        </div>
        <!-- END Left Column -->

        <!-- START Right Column -->

        <div dojoType="dijit.layout.ContentPane" splitter="true" region="center" class="portlet-content-search" id="contentWrapper" style="overflow-y:auto; overflow-x:auto;">
            <div class="portlet-main">
                <div id="metaMatchingResultsDiv" style="display:none;">
                    <!-- START Listing Results -->
                    <input type="hidden" name="referer" value="<%=referer%>">
                    <input type="hidden" name="cmd" value="prepublish">
                    <div class="portlet-toolbar" style="min-height: 48px; margin-top: 16px; flex-direction: column; margin-bottom: 0.5rem;">
                        <div style="display: flex; align-self: flex-start; justify-content: space-between; width: 100%;">
                            <div class="portlet-toolbar__actions-secondary" style="display: flex; align-items: center; padding-left: 0.5rem;">
                                <div class="portlet-toolbar__actions-search" style="width: 270px;">
                                    <input type="text" dojoType="dijit.form.TextBox" tabindex="1" placeholder="<%= LanguageUtil.get(pageContext, "Type-To-Search").replace("\"", "'") %>" onKeyUp='doSearch()' name="allFieldTB" id="allFieldTB" value="<%=_allValue %>">
                                </div>
                                <div id="matchingResultsDiv" style="display: none" class="portlet-toolbar__info"></div>

                            </div>
                            <div class="portlet-toolbar__actions-primary">
                                <button id="bulkAvailableActions" dojoType="dijit.form.Button" data-dojo-props="onClick: doShowAvailableActions" iconClass="actionIcon" >
                                    <%= LanguageUtil.get(pageContext, "Available-actions")%>
                                </button>
                                <div data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"fa-plus", class:"dijitDropDownActionButton"'>
                                    <span></span>
                                    <script type="text/javascript">
                                        function importContent() {
                                            window.location = '/c/portal/layout?p_l_id=<%= layout.getId() %>&dm_rlout=1&p_p_id=<%=PortletID.CONTENT%>&p_p_action=1&p_p_state=maximized&_<%=PortletID.CONTENT%>_struts_action=/ext/contentlet/import_contentlets&selectedStructure=' + document.getElementById('structureInode').value + '&angularCurrentPortlet=' + portletId;
                                        }
                                    </script>
                                    <ul data-dojo-type="dijit/Menu" id="actionPrimaryMenu" style="display: none;">
                                        <li data-dojo-type="dijit/MenuItem" data-dojo-props="onClick:function() {addNewContentlet('')}"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Content" )) %></li>
                                        <li data-dojo-type="dijit/MenuItem" data-dojo-props="onClick:importContent">
                                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Import-Content" )) %>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                        <div id="tablemessage" class="contentlet-selection" style="align-self: flex-start; margin: 0.5rem;"></div>
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
        <div id="queryDiv" dojoType="dijit.Dialog" title='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Query" )) %>' class="content-search__show-query-dialog" style="display: none;padding-top:15px\9;">
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
</dot-asset-drop-zone>
    <script>
        var dotAssetDropZone = document.querySelector('dot-asset-drop-zone');
        dotAssetDropZone.addEventListener('uploadComplete', function() {
            doSearch();
        });
    </script>


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
                <div class="sTypeItem" id="sType<%=struc.getInode() %>"><a href="javascript:addNewContentlet('<%=struc.getInode() %>','<%=struc.getVelocityVarName() %>');"><%=struc.getName() %></a></div>
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
