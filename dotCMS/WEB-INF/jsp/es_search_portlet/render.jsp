
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.liferay.util.Validator"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESSearchResults"%>
<%@ page import="com.dotcms.repackage.org.elasticsearch.search.suggest.term.TermSuggestion.*"%>
<%@page import="com.dotcms.repackage.org.elasticsearch.search.suggest.term.TermSuggestion"%>
<%@page import="com.dotcms.repackage.org.elasticsearch.search.suggest.Suggest.Suggestion"%>
<%@page import="com.dotcms.repackage.org.elasticsearch.search.facet.terms.TermsFacet"%>
<%@page import="com.dotcms.repackage.org.elasticsearch.search.facet.Facet"%>

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


<script>
function refreshPane(){
	

	
    	var data = {
    			"query":dijit.byId("query").getValue(),
    			"userid":dijit.byId("userid").getValue(),
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
</script>
<div class="portlet-wrapper">
				<div style='text-align:center;padding:20px;'>
	
				</div>
			<table class="listingTable" style="width:70%">	
				<tr>
					<th valign="top"><strong><%= LanguageUtil.get(pageContext, "ES Query") %> :</strong></th>
					<td>
					
						<textarea dojoType="dijit.form.Textarea" name="query" style="width:500px;min-height: 150px;font-family:monospace;" id="query" type="text"><%=UtilMethods.htmlifyString(query)%></textarea>
					
					</td>
				</tr>
				<tr>
					<th><strong><%= LanguageUtil.get(pageContext, "Live") %> :</strong></th>
					<td nowrap="nowrap">
					
						<input dojoType="dijit.form.CheckBox" name="live"  id="live" type="checkbox" value="true" <%=live ? "checked=true" : ""%>
				
					</td>
				</tr>

				<tr>
					<th><strong><%= LanguageUtil.get(pageContext, "User ID or Email") %> :</strong></th>
					<td nowrap="nowrap">
					
						<input name="userid" id="userid"  type="text" value="<%=UtilMethods.webifyString(userToPullID)%>" size="40"   dojoType="dijit.form.TextBox"  />


					</td>
				</tr>
			</table>

			
			<div style='text-align:center;padding:20px;'>
				<button type="button" id="submitButton" onClick="refreshPane()" dojoType="dijit.form.Button" value="Submit"><%= LanguageUtil.get(pageContext, "Submit") %></button>
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
			
			<%if(cons!=null && cons.size() >0){ %>

					<table class="listingTable" style="width:70%;"">	
						<tr><th colspan="3">
						

							<h2>Results</h2>
						</th></tr>
	
						<% for (Object x : cons){%>
							<%Contentlet c =(Contentlet) x;%>
					
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
										<div style="padding:20px;"><%= UtilMethods.makeHtmlSafe(c.getMap().toString()) %></div>
									</td>
								</tr>
								<%	counter++;%>
							<%}%>
					</table>
				<div style='text-align:center;padding:20px;'>
	
				</div>
				
			<%} %>

	<%if(cons!= null && cons.getFacets() !=null){ %>
		<table class="listingTable" style="width:70%;"">	
			<tr><th colspan="3">
				

					<h2>Facets</h2>
				</th></tr>
				<tr>
					<td>
					<%for(Facet f : cons.getFacets()){ %>
					<%TermsFacet terms = (TermsFacet) f; %>
						<%for (TermsFacet.Entry entry : terms) {%>
							<%=entry.getTerm()%> =
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
			<table class="listingTable" style="width:70%;"">	
				<tr>
					<th colspan="3">
						<h3>Suggestions</h3>
					</th>
				</tr>
				<tr>
					<td>
						<%for(Suggestion s : cons.getSuggestions() ){ %>
				
							<%for (Object entry : s.getEntries()) {%>
								<%=((com.dotcms.repackage.org.elasticsearch.search.suggest.term.TermSuggestion.Entry) entry).getText() %> | 
								<%for(TermSuggestion.Entry.Option opt : ((com.dotcms.repackage.org.elasticsearch.search.suggest.term.TermSuggestion.Entry) entry).getOptions() ){ %>
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
	
	
	<%if(cons == null){ %>
	<table 	class="listingTable" style="width:70%;">	
	<tr>
		<th >
			<h2>Examples</h2>
		</th>
	</tr>
	<tr>
		<td>
		
			<pre style="font-family: monospace;font-size: 10pt">
//Match All
{
    "query" : {
        "match_all" : {}
    }
}


// Match gas
{
    "query": {
        "bool": {
            "must": {
                "term": {
                    "_all": "gas"
                }
            }
        }
    }
}

// curl for the same results
curl -XGET http://localhost:8080/api/es/search -d '{
    "query": {
        "bool": {
            "must": {
                "term": {
                    "_all": "gas"
                }
            }
        }
    }
}'



// facet on the news.tags field
{
    "query" : { "query_string" : {"query" : "gas*"} },
    "facets" : {
        "tags" : { "terms" : {"field" : "news.tags"} }
    }
}


// curl for the same results
curl -XGET http://localhost:8080/api/es/raw -d '
	{
	    "query" : { "query_string" : {"query" : "gas*"} },
	    "facets" : {
	        "tags" : { "terms" : {"field" : "news.tags"} }
	    }
	}
'





// suggest based on title
{
  "suggest" : {
    "title-suggestions" : {
      "text" : "gs pric rollrcoater",
      "term" : {
        "size" : 3,
        "field" : "title"
      }
    }
  }
}

// curl for the same results
curl -XGET http://localhost:8080/api/es/raw -d '
	{
	  "suggest" : {
	    "title-suggestions" : {
	      "text" : "gs pric rollrcoater",
	      "term" : {
	        "size" : 3,
	        "field" : "title"
	      }
	    }
	  }
	}
'



// filter news by distance away
// (For this example to work you need to add a field to the news structure 
// that uses latlon as its velocity variable name.
// it can be a text field with a value of ""42.648899,-71.165497)
{
    "query": {
        "filtered": {
            "query": {
                "match_all": {}
            },
            "filter": {
                "geo_distance": {
                    "distance": "20km",
                    "news.latlon": {
                        "lat": 37.776,
                        "lon": -122.41
                    }
                }
            }
        }
    }
}


// filter news by distance away part 2
// (For this example to work you need to add a field to the news structure 
// that uses latlon as its velocity variable name.
// it can be a text field with a value of ""42.648899,-71.165497)
{
    "query": {
        "filtered": {
            "query": {
                "match_all": {}
            },
            "filter": {
                "geo_distance": {
                    "distance": "20km",
                    "news.latlon": {
                        "lat": 42.648899,
                        "lon": -71.165497
                    }
                }
            }
        }
    }
}


// sort news by distance away
// (For this example to work you need to add a field to the news structure 
// that uses latlon as its velocity variable name.
// it can be a text field with a value of ""42.648899,-71.165497)
{
    "sort" : [
        {
            "_geo_distance" : {
                "news.latlon" : {
                    "lat" : 42,
                    "lon" : -71
                },
                "order" : "asc",
                "unit" : "km"
            }
        }
    ],
    "query" : {
        "term" : { "title" : "gas" }
    }
}




// query using a range
{
    "query": {
        "bool": {
            "must": {
                "term": {
                    "title": "gas"
                }
            },
            "must_not": {
                "range": {
                    "languageid": {
                        "from": 2,
                        "to": 20
                    }
                }
            }
        }
    }
}
		</pre>
</td>
</tr>
</table>

<%} %>
</div>