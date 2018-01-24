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

<%
    String containerIdentifier = request.getParameter("container_id");
    User user = PortalUtil.getUser(request);
    Container container = (Container) APILocator.getVersionableAPI().findWorkingVersion(containerIdentifier, user, false);

    List<ContentType> contentTypes = null;
    String baseTypeToAdd = request.getParameter("add");

    if (BaseContentType.WIDGET.name().equalsIgnoreCase(baseTypeToAdd)) {
        contentTypes = APILocator.getContentTypeAPI(user).findByType(BaseContentType.WIDGET);
    } else if (BaseContentType.FORM.name().equalsIgnoreCase(baseTypeToAdd)) {
        contentTypes = APILocator.getContentTypeAPI(user).findByType(BaseContentType.FORM);
    } else {
        contentTypes = APILocator.getContainerAPI().getContentTypesInContainer(container);

    }
    contentTypes = new ArrayList<>(contentTypes);

    contentTypes.addAll(APILocator.getContentTypeAPI(user).findByType(BaseContentType.WIDGET));
    Layout contentLayout = APILocator.getLayoutAPI().findLayoutByName("Content");
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
            margin-right: 16px;
        }

        .portlet-toolbar__add-contentlet {
            margin-left: auto;
        }
    </style>

    <script type="text/javascript">
    
    var _dotSelectedStructure = '<%=contentTypes.get(0).id()%>';
        function addNewContentlet() {


            var href = "/c/portal/layout?p_l_id=<%=contentLayout.getId()%>&p_p_id=content&p_p_action=1&p_p_state=maximized&p_p_mode=view";
            href += "&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_content_cmd=new";
            href += "&selectedStructure=" + _dotSelectedStructure + "&lang=1";
            window.location = href;
        }

        function contentSelected(content) {
            if (ngEditContentletEvents) {
                ngEditContentletEvents.next({
                    name: "select",
                    data: {
                        inode: content.inode,
                        identifier: content.identifier,
                        type: content.typeVariable
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


    <script type="text/javascript">
        dojo.require("dotcms.dijit.form.ContentSelector");

        dojo.addOnLoad(function () {
            contentSelector.show();

            contentSelector.containerStructures = [
                <%
                    for (ContentType contentType: contentTypes) {
                %>
                {
                    "inode": "<%=contentType.id()%>",
                    "name": "<%=contentType.name()%>",
                    "baseType": "<%=contentType.baseType()%>"
                },
                <%
                    }
                %>
            ];

            contentSelector._fillStructures();
        })

    </script>
</head>
<body>
<div jsId="contentSelector" onContentSelected="contentSelected" dojoType="dotcms.dijit.form.ContentSelector"></div>
</body>
</html>