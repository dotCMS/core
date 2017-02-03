<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.liferay.util.Validator"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESSearchResults"%>
<%@ page import="org.elasticsearch.search.suggest.term.TermSuggestion.*"%>
<%@page import="org.elasticsearch.search.suggest.term.TermSuggestion"%>
<%@page import="org.elasticsearch.search.suggest.Suggest.Suggestion"%>
<%@page import="org.elasticsearch.search.facet.terms.TermsFacet"%>
<%@page import="org.elasticsearch.search.facet.Facet"%>

<%@page import="com.dotcms.content.elasticsearch.business.ESContentletAPIImpl"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.common.model.ContentletSearch"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>
<%@ page import="com.dotmarketing.portlets.contentlet.util.ContentletUtil" %>

<%if( LicenseUtil.getLevel() < 200){ %>
	<div class="portlet-wrapper">
		<div class="subNavCrumbTrail">
			<ul id="subNavCrumbUl">
				<li class="lastCrumb">
					<a href="#" ><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.ES_SEARCH_PORTLET")%></a>
				</li>

			</ul>
			<div class="clear"></div>
		</div>
		<jsp:include page="/WEB-INF/jsp/es_search_portlet/not_licensed.jsp"></jsp:include>

	</div>
<%return;}%>

<%
User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);

String query = request.getParameter("query");
if(!UtilMethods.isSet(query)){
	query = "";
}

boolean live = request.getParameter("live")!= null && request.getParameter("live").equals("true");


String userToPullID = request.getParameter("userid");

int counter = 1;
boolean userIsAdmin = false;
User userForPull = user;


String nastyError = null;



if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
	userIsAdmin = true;
	if(UtilMethods.isSet(userToPullID)){
		try{
			if(Validator.isEmailAddress(userToPullID)){
				userForPull = APILocator.getUserAPI().loadByUserByEmail(userToPullID,APILocator.getUserAPI().getSystemUser(),true);
			}
			else{
				userForPull = APILocator.getUserAPI().loadUserById(userToPullID,APILocator.getUserAPI().getSystemUser(),true);

			}
		}
		catch(Exception e){
			userForPull = APILocator.getUserAPI().getAnonymousUser();
		}
	}
}
else{
	userForPull= user;
}
ESSearchResults cons =null;

if(query == null){
	query = "";
}else{




	try{


		ContentletAPI es = APILocator.getContentletAPI();
		if (UtilMethods.isSet(query)) {


				cons = es.esSearch(query, live,userForPull,true);


		}




	}
	catch(Exception pe){

		nastyError = pe.toString();
	}
}



%>

<script src="/html/js/ace-builds-1.2.3/src-noconflict/ace.js" type="text/javascript"></script>
<script>
    var editor;
    function aceArea() {
        ace.config.set('basePath', '/html/js/ace-builds-1.2.3/src-noconflict/');
        editor = ace.edit('esEditor');
        editor.setTheme("ace/theme/textmate");
        editor.getSession().setMode("ace/mode/json");
    }

    function refreshPane(){
        var data = {
            "query": editor.getValue(),
            "userid": dijit.byId("userid").getValue(),
            "live": dijit.byId("live").getValue()
        };

        dojo.xhrPost({
            url: "/api/portlet/ES_SEARCH_PORTLET/render",
            handleAs: "text",
            postData: data,
            load: function(code) {
                dotAjaxNav.refreshHTML(code);
            }
        });
    }

    function showEsHelpDialog() {
        var esSearch = dijit.byId("esSearchHelpDia");
        esSearch.show();
    }

    function handleWrapMode(e) {
        editor.getSession().setUseWrapMode(e);
    }

    dojo.addOnLoad(aceArea);
</script>
<style type="text/css" media="screen">
    #esEditor {
        position: absolute;
        top: 0;
        right: 0;
        bottom: 0;
        left: 0;
    }
    .esEditorWrapper {
        position: relative;
        height: 400px;
        border: solid 1px #C0C0C0
    }

    .dmundra .helpButton {
        margin: 5px 0 0 0;
    }
    .wrap-editor {
        margin-top: 5px;
    }
</style>
<div class="portlet-wrapper">

	<table class="listingTable" style="width:70%">
        <tr>
            <th width="110" valign="top">
                <strong><%= LanguageUtil.get(pageContext, "ES Query") %>:</strong>
                <button type="button" class="helpButton" iconClass="helpIcon" onClick="showEsHelpDialog()" dojoType="dijit.form.Button" value="Help"><%= LanguageUtil.get(pageContext, "Help") %></button>
            </th>
            <td>
                <div class="esEditorWrapper">
                    <div id="esEditor"><%=UtilMethods.htmlifyString(query)%></div>
                </div>
                <div class="wrap-editor">
                    <input id="wrapEditor" name="wrapEditor" data-dojo-type="dijit/form/CheckBox" value="true" onChange="handleWrapMode" /> <label for="wrapEditor"><%= LanguageUtil.get(pageContext, "Wrap-Code") %></label>
                </div>
            </td>
        </tr>
		<tr>
			<th><strong><%= LanguageUtil.get(pageContext, "Live") %>:</strong></th>
			<td nowrap="nowrap">
				<input dojoType="dijit.form.CheckBox" name="live"  id="live" type="checkbox" value="true" <%=live ? "checked=true" : ""%>
			</td>
		</tr>

		<tr>
			<th><strong><%= LanguageUtil.get(pageContext, "User ID or Email") %>:</strong></th>
			<td nowrap="nowrap">

				<input name="userid" id="userid"  type="text" value="<%=UtilMethods.webifyString(userToPullID)%>" size="40"   dojoType="dijit.form.TextBox"  />


			</td>
		</tr>
	</table>


	<div style='text-align:center;padding:20px;'>
		<button type="button" id="submitButton"  iconClass="queryIcon" onClick="refreshPane()" dojoType="dijit.form.Button" value="Submit"><%= LanguageUtil.get(pageContext, "Query") %></button>
	</div>

	<%if(UtilMethods.isSet(cons)){ %>
		<table class="listingTable" style="width:70%;padding-bottom:20px">
			<tr>
				<th nowrap="nowrap"><strong><%= LanguageUtil.get(pageContext, "Took") %> :</strong></th>
				<td style="width:100%">
					<%=cons.getQueryTook()%> ms <%= LanguageUtil.get(pageContext, "query") %><br>
					<%=cons.getPopulationTook()%> ms <%= LanguageUtil.get(pageContext, "population") %><br>

				</td>
			</tr>

			<tr>
				<th nowrap="nowrap"><strong><%= LanguageUtil.get(pageContext, "Query-is") %> :</strong></th>
				<td>
					<%=cons.getQuery()%>


				</td>
			</tr>
			<tr>
				<th nowrap="nowrap"><strong><%= LanguageUtil.get(pageContext, "Showing Hits") %> :</strong></th>
				<td>
				<%=cons.getCount() %> of <%=cons.getTotalResults()%>


				</td>
			</tr>
		</table>
		<div style='text-align:center;padding:20px;'>

		</div>
	<%} %>

	<%if(cons!= null && cons.getFacets() !=null){ %>
		<table class="listingTable" style="width:70%;">
			<tr><th colspan="3">


					<h2>Facets</h2>
				</th></tr>
				<tr>
					<td>
					<%for(Facet f : cons.getFacets()){ %>
					<%TermsFacet terms = (TermsFacet) f; %>
					<%int ii=1; %>
						<%for (TermsFacet.Entry entry : terms) {%>

							<%=ii++%>. <%=entry.getTerm()%> =
							<%= entry.getCount()%><br>

						<% } %>
					<%} %>
				</td>
			</tr>
		</table>
		<div style='text-align:center;padding:20px;'>

		</div>
	<%} %>
	<%if(cons != null && cons.getSuggestions() !=null){ %>
		<table class="listingTable" style="width:70%;">
			<tr>
				<th colspan="3">
					<h3>Suggestions</h3>
				</th>
			</tr>
			<tr>
				<td><%int ii=1; %>
					<%for(Suggestion s : cons.getSuggestions() ){ %>

						<%for (Object entry : s.getEntries()) {%>
							<%=ii++%>. <%=((org.elasticsearch.search.suggest.term.TermSuggestion.Entry) entry).getText() %> |
							<%for(TermSuggestion.Entry.Option opt : ((org.elasticsearch.search.suggest.term.TermSuggestion.Entry) entry).getOptions() ){ %>
								<%=opt.getText()%>
							<%} %>
							<br>
						<%} %>
					<%} %>


				</td>
			</tr>
		</table>
		<div style='text-align:center;padding:20px;'>

		</div>
	<%} %>
	<%if(cons!=null && cons.size() >0){ %>

			<table class="listingTable" style="width:70%;">
				<tr><th colspan="3">


					<h2>Results</h2>
				</th></tr>

				<% for (Object x : cons){%>
					<%
						Contentlet c =(Contentlet) x;
						String mapStr = ContentletUtil.getContentPrintableMap(user, c).toString();
					%>

						<tr>
							<td><strong><%= counter %>.</td>
							<td width="100"><strong><%= LanguageUtil.get(pageContext, "Title") %>:</strong></td>




							<td>
									<%=c.getTitle() %>

							</td>
						</tr>
						<tr>
							<td>&nbsp;</td>
							<td><strong><%= LanguageUtil.get(pageContext, "Inode") %>:</strong></td>
							<td width="90%"><%=c.getInode() %></td>
						</tr>
						<tr>
							<td>&nbsp;</td>
							<td><strong><%= LanguageUtil.get(pageContext, "Identifier") %>:</strong></td>
							<td><%= c.getIdentifier() %></td>
						</tr>
						<tr >
							<td>&nbsp;</td>
							<td colspan=2>
								<div style="padding:20px;"><%= UtilMethods.makeHtmlSafe(mapStr) %></div>
							</td>
						</tr>
						<%	counter++;%>
					<%}%>
			</table>
		<div style='text-align:center;padding:20px;'>

		</div>

	<%} %>



		<%if(cons != null){ %>
			<table 	class="listingTable" style="width:70%;">
				<tr>
					<th colspan="3">
						<h3>Raw</h3>
					</th>
				</tr>
				<tr>
					<td colspan="3">
						<pre><%if(cons.getResponse() !=null){ %><%=cons.getResponse()%><%} %>
						</pre>
					</td>
				</tr>
			</table>
			<div style='text-align:center;padding:20px;'>

			</div>
		<%} %>
	<%if(UtilMethods.isSet(nastyError)){%>

		<div style='color:red;width:70%;margin:auto;padding:20px;'>
			<%=nastyError %>
		</div>

	<%}%>


</div>


<div id="esSearchHelpDia" title="ElasticSearch Help" dojoType="dijit.Dialog">
	<jsp:include page="/WEB-INF/jsp/es_search_portlet/es-search-help.jsp"></jsp:include>
</div>
