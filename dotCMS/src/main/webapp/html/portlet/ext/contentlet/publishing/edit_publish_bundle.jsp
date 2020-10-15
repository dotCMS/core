<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@page import="com.dotmarketing.cms.factories.PublicEncryptionFactory"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.environment.business.EnvironmentAPI"%>
<%@page import="com.dotcms.publisher.environment.bean.Environment"%>
<%@page import="com.dotcms.publisher.bundle.bean.Bundle"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%
	String identifier = request.getParameter("id");
	Bundle bundle = APILocator.getBundleAPI().getBundleById(identifier);

%>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<script type="text/javascript">
	require(["dojo/parser", "dijit/form/SimpleTextarea", "dotcms/dojo/data/RoleReadStore",  "dijit/form/FilteringSelect"]);

	var myRoleReadStore = new dotcms.dojo.data.RoleReadStore({nodeId: "whoCanUseSelect"});


	function updateBundle(){

		var form = dijit.byId("formSaveEnvironment");

		dijit.byId("bundleName").setAttribute('required',true);



		if (form.validate()) {

			var name = dijit.byId("bundleName").value;
			var xhrArgs = {
				url: "/api/bundle/updatebundle/bundleid/<%=bundle.getId()%>",
				content:{bundleName : encodeURIComponent(convertStringToUnicode(name))}, 
				handleAs: "json",
				load: function(data){
					if(data=="false"){

						alert('<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles_Error_Edit") %>');
					}
					else{
						backToBundleList();
					}
				},
				error: function(error){
					alert('<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles_Error_Edit") %>');

				}
			}

			var deferred = dojo.xhrGet(xhrArgs);
		}

	}

	function backToBundleList(){

		dijit.byId("editBundle").hide();
		loadUnpushedBundles();

	}
	
	function convertStringToUnicode(name) {
		  var unicodeString = '';
		   for (var i=0; i < name.length; i++) {
			  if(name.charCodeAt(i) > 128){
				 var str = name.charCodeAt(i).toString(16).toUpperCase();
			 	 while(str.length < 4)
			        str = "0" + str;
				  unicodeString += "\\u" + str;
			  }else{
		          unicodeString += name[i];
			  }
		   }
		   
		  return unicodeString;
	}


</script>

<style>
	.myTable {margin:20px;padding:10px;}
	.myTable tr td{padding:5px;vertical-align: top;}
	#addressRow {}
	#portRow {display:none;}

</style>

<div style="margin:auto;">
	<div dojoType="dijit.form.Form"  name="formSaveEnvironment"  id="formSaveEnvironment" onsubmit="return false;">
		<input type="hidden" name="identifier" value="<%=UtilMethods.webifyString(String.valueOf(bundle.getId())) %>">
		<table class="myTable" border=0 style="margin: 20px auto" align="center">
            <tr style="height:30px">
                <td align="right">
                    <%= LanguageUtil.get(pageContext, "id") %>:
                </td>
                <td style="white-space: nowrap;" >
                    <div style="padding-left:5px;padding-bottom:10px;"><%if(bundle.bundleTgzExists()){%><a href="/api/bundle/_download/<%=bundle.getId()%>" target="_blank"><%} %>
                    <%=bundle.getId() %></a></div> 
                    <div  style="padding-left:5px;padding-bottom:10px;">
                        <%if(bundle.bundleTgzExists()){%>

                                (<%= LanguageUtil.get(pageContext, "Download") %>)
                             </a>
                        <%} %>
                    </div>
                </td>
            </tr>

            
			<tr style="height:30px">
				<td align="right">
					<%= LanguageUtil.get(pageContext, "publisher_dialog_bundle_name") %>:
				</td>
				<td>
					<input type="text" dojoType="dijit.form.ValidationTextBox"
							  name="bundleName"
							  id="bundleName"
							  style="width:200px;"
							  value="<%=StringEscapeUtils.unescapeJava(bundle.getName()) %>"
							  />
				</td>
			</tr>

		</table>

		<table align="center">
			<tr>
				<td colspan="2" class="buttonRow" style="text-align: center;white-space: nowrap;">
					<button dojoType="dijit.form.Button" onClick="backToBundleList()" id="closeSave" iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
					&nbsp;
					<button dojoType="dijit.form.Button" type="submit" id="save" iconClass="saveIcon"  onclick="updateBundle()"><%= LanguageUtil.get(pageContext, "Save") %></button>

			    </td>
		    </tr>
	   </table>
	</div>
</div>