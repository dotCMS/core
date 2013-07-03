<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@page import="com.dotmarketing.cms.factories.PublicEncryptionFactory"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publisher.environment.business.EnvironmentAPI"%>
<%@page import="com.dotcms.publisher.environment.bean.Environment"%>
<%@page import="com.dotcms.publisher.bundle.bean.Bundle"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.Role"%>
<%
	String identifier = request.getParameter("id");
	Bundle bundle = APILocator.getBundleAPI().getBundleById(identifier);

%>

<script type="text/javascript">
	require(["dojo/parser", "dijit/form/SimpleTextarea", "dotcms/dojo/data/RoleReadStore",  "dijit/form/FilteringSelect"]);

	var myRoleReadStore = new dotcms.dojo.data.RoleReadStore({nodeId: "whoCanUseSelect"});


	function updateBundle(){

		var form = dijit.byId("formSaveEnvironment");

		dijit.byId("bundleName").setAttribute('required',true);



		if (form.validate()) {

			var name = dijit.byId("bundleName").value;

			var xhrArgs = {
				url: "/api/bundle/updatebundle/bundleid/<%=bundle.getId()%>/bundlename/"+name,
				handleAs: "text",
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
		<table class="myTable" border=0 style="margin: auto" align="center">
			<tr>
				<td align="right" width="40%">
					<%= LanguageUtil.get(pageContext, "publisher_dialog_bundle_name") %>:
				</td>
				<td>
					<input type="text" dojoType="dijit.form.ValidationTextBox"
							  name="bundleName"
							  id="bundleName"
							  style="width:200px;"
							  value="<%=bundle.getName() %>"
							  />
				</td>
			</tr>
		</table>

		<table align="center">
			<tr>
				<td colspan="2" class="buttonRow" style="text-align: center;white-space: nowrap;">
					<button dojoType="dijit.form.Button" type="submit" id="save" iconClass="saveIcon"  onclick="updateBundle()"><%= LanguageUtil.get(pageContext, "Save") %></button>
					&nbsp;
					<button dojoType="dijit.form.Button" onClick="backToBundleList()" id="closeSave" iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "Cancel") %></button>

			    </td>
		    </tr>
	   </table>
	</div>
</div>