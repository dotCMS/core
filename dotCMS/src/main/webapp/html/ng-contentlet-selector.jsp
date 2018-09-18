<%@page import="com.dotcms.contenttype.model.type.ContentType"%>
<%@page import="com.dotcms.contenttype.transform.contenttype.StructureTransformer"%>
<%@page import="com.dotcms.contenttype.model.type.BaseContentType"%>

<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.dotmarketing.portlets.containers.model.Container"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="java.util.List"%>

<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@page import="java.util.*" %>

<%@ page import="com.dotcms.contenttype.model.type.BaseContentType" %>
<%@ page import="com.dotcms.contenttype.model.type.ContentType" %>
<%@ page import="com.dotcms.contenttype.transform.contenttype.StructureTransformer" %>
<%@ page import="com.dotmarketing.business.Layout" %>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>

<%
    String containerIdentifier = request.getParameter("container_id");
    User user = PortalUtil.getUser(request);
    Container container = (Container) APILocator.getVersionableAPI().findWorkingVersion(containerIdentifier,
            APILocator.getUserAPI().getSystemUser(), false);

    List<ContentType> contentTypes = null;
    String baseTypeToAdd = request.getParameter("add");

    if (BaseContentType.WIDGET.name().equalsIgnoreCase(baseTypeToAdd)) {
        contentTypes = APILocator.getContentTypeAPI(user).findByBaseType(BaseContentType.WIDGET, "name", 250,0);
    } else if (BaseContentType.FORM.name().equalsIgnoreCase(baseTypeToAdd)) {
        contentTypes = APILocator.getContentTypeAPI(user).findByBaseType(BaseContentType.FORM, "name", 250,0);
    } else {
        contentTypes = APILocator.getContainerAPI().getContentTypesInContainer(user, container);
    }

    Layout contentLayout = APILocator.getLayoutAPI().findLayoutByName("Content");

    String containerStructures = "[";
    Integer count = 1;
        containerStructures = containerStructures + "{";
        containerStructures = containerStructures + "\"inode\": \"catchall\",";
        containerStructures = containerStructures + "\"name\":" + "\"" + LanguageUtil.get(pageContext, "All") + "\",";
        containerStructures = containerStructures + "\"variable\": \"catchall\"";
        containerStructures = containerStructures + "},";

    for (ContentType contentType: contentTypes) {
        containerStructures = containerStructures + "{";
        containerStructures = containerStructures + "\"inode\":" + "\"" + contentType.id() + "\",";
        containerStructures = containerStructures + "\"name\":" + "\"" + contentType.name() + "\",";
        containerStructures = containerStructures + "\"baseType\":" + "\"" + contentType.baseType() + "\",";
        containerStructures = containerStructures + "\"variable\":" + "\"" + contentType.variable() + "\"";
        containerStructures = containerStructures + "}";

        if (count < contentTypes.size()) {
            containerStructures = containerStructures + ',';
        }
        count++;
    }

    containerStructures = containerStructures + "]";
%>


<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Contentlet Search</title>

    <style type="text/css">
        @import "/html/js/dojo/custom-build/dijit/themes/dijit.css";
        @import "/html/css/dijit-dotcms/dotcms.css";

        body {
            background-color: #fff;
        }

        body,
        .contentlet-search,
        .related-content-form {
            width: 100%;
            height: 100%;
        }

        .portlet-sidebar-wrapper {
            width: 200px;
        }
    </style>

    <script type="text/javascript">
    
    var _dotSelectedStructure = '<%=contentTypes.get(0).id()%>';
        function addNewContentlet(iNode) {
            var href = "/c/portal/layout?p_l_id=<%=contentLayout.getId()%>&p_p_id=content&p_p_action=1&p_p_state=maximized&p_p_mode=view";
            href += "&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_content_cmd=new";
            href += "&selectedStructure=" + (iNode || _dotSelectedStructure) + "&lang=1";
            window.location = href;
        }

        function contentSelected(content) {
            if (ngEditContentletEvents) {
                ngEditContentletEvents.next({
                    name: "select",
                    data: {
                        inode: content.inode,
                        identifier: content.identifier,
                        type: content.typeVariable,
                        baseType: content.baseType
                    }
                })
            }
        }

        function displayStructure(structureInode) {
            contentSelector.displayStructureFields(structureInode);
            _dotSelectedStructure = structureInode;
        }

        function getSelectedLanguageId () {
            var obj = dijit.byId("langcombo+1");
            return obj && obj.value;
        }

        function isInodeSet(x) {
            return (x && x != undefined && x != "" && x.length > 15);
        }

        function loadAddContentTypePrimaryMenu() {
            var addContentDropdown = '<div data-dojo-type="dijit/form/DropDownButton" data-dojo-props=\'iconClass:"fa-plus", class:"dijitDropDownActionButton"\'><span></span>';
            addContentDropdown+= '<ul data-dojo-type="dijit/Menu" >';
            var addContentTypePrimaryMenu =  document.getElementById('addContentTypeDropdown');
            for ( var i = 1; contentSelector.containerStructures.length > i; i++) {
                addContentDropdown+= '<li data-dojo-type="dijit/MenuItem" onClick="addNewContentlet(\'' + contentSelector.containerStructures[i].inode + '\')">' + contentSelector.containerStructures[i].name + '</li>';
            }
            addContentDropdown+='</ul></div>';
            addContentTypePrimaryMenu.innerHTML= addContentDropdown;
            dojo.parser.parse(addContentTypePrimaryMenu);
        }


        djConfig = {
            parseOnLoad: true,
            i18n: "/html/js/dojo/custom-build/custom-build/build/",
            useXDomain: false,
            isDebug: false,
            locale: "en-us",
            modulePaths: { dotcms: "/html/js/dotcms" }
        };
    </script>

    <script type="text/javascript" src="/html/js/log4js/log4javascript.js"></script>
    <script type="text/javascript" src="/html/js/log4js/dotcms-log4js.js"></script>
    <script type="text/javascript" src="/html/js/dojo/custom-build/dojo/dojo.js"></script>
    <script type="text/javascript" src="/html/js/dojo/custom-build/build/build.js"></script>
    <script type="text/javascript" src="/html/common/javascript.jsp"></script>
    <script type="text/javascript" src="/dwr/engine.js"></script>
    <script type="text/javascript" src="/dwr/util.js"></script>
    <script type="text/javascript" src="/dwr/interface/LanguageAjax.js"></script>
    <script type="text/javascript" src="/dwr/interface/StructureAjax.js"></script>
    <script type="text/javascript" src="/dwr/interface/ContentletAjax.js"></script>
    <script type="text/javascript" src="/dwr/interface/BrowserAjax.js"></script>
    <script type="text/javascript" src="/dwr/interface/CategoryAjax.js"></script>


    <script type="text/javascript">
        dojo.require("dotcms.dijit.form.ContentSelector");

        dojo.addOnLoad(function () {
            contentSelector.show();
            loadAddContentTypePrimaryMenu();
        });
    </script>
</head>
<body>
<div jsId="contentSelector" containerStructures='<%=containerStructures%>' onContentSelected="contentSelected" dojoType="dotcms.dijit.form.ContentSelector"></div>
</body>
</html>