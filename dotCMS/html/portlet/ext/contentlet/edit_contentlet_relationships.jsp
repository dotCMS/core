<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships"%>
<%@page import="com.dotmarketing.portlets.structure.model.Relationship"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.business.IdentifierFactory"%>
<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.business.*"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@ page import="com.dotmarketing.util.Config" %>
<%@page import="com.dotmarketing.business.IdentifierCache"%>
<%@page import="com.dotmarketing.portlets.structure.factories.RelationshipFactory"%>
<%@page import="java.util.HashMap"%>

<%
	LanguageAPI langAPI = APILocator.getLanguageAPI();
	List<Language> langs = langAPI.getLanguages();
	List<Language> languages = (List<Language>)request.getAttribute (com.dotmarketing.util.WebKeys.LANGUAGES);
	boolean canUserPublishContentlet = (request.getAttribute("canUserPublishContentlet") != null) ? ((Boolean)request.getAttribute("canUserPublishContentlet")).booleanValue() : false;

	java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
	params.put("struts_action",new String[] {"/ext/contentlet/view_contentlets_popup"});
	String viewContentsPopupURL = PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);

	DateFormat modDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
	modDateFormat.setTimeZone(timeZone);

	Contentlet contentlet = (Contentlet) request.getAttribute("contentlet");
	String contentletInode = String.valueOf(contentlet.getInode());

	Language defaultLang = langAPI.getDefaultLanguage();
    String languageId = String.valueOf(defaultLang.getId());

    PermissionAPI conPerAPI = APILocator.getPermissionAPI();
	boolean canUserWriteToContentlet = true;
	if(InodeUtils.isSet(contentlet.getInode()))
		canUserWriteToContentlet = conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_WRITE,user);

	//Variable used to return after the work is done with the contentlet
	String referer = "";
	if (request.getParameter("referer") != null) {
		referer = request.getParameter("referer");
	} else {
		params = new HashMap();
		params.put("struts_action",new String[] {"/ext/contentlet/edit_contentlet"});
		params.put("inode",new String[] { contentletInode + "" });
		params.put("cmd",new String[] { Constants.EDIT });
		referer = PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
	}

	/* for managing automatic relationships coming form the add content on the relationships tab */
	String relwith = request.getParameter("relwith");/*for the relationship*/
	String relisparent = request.getParameter("relisparent");
	String reltype = request.getParameter("reltype");

	if (relwith!=null) {
%>

<%@page import="com.dotmarketing.business.PermissionAPI"%><input type="hidden" name="relwith"	value="<%=relwith%>" id="relwith"/>
	<input type="hidden" name="relisparent"	value="<%=relisparent%>" id="relisparent"/>
	<input type="hidden" name="reltype"	value="<%=reltype%>" id="reltype"/>

<%
	}

	List<ContentletRelationships.ContentletRelationshipRecords> relationshipRecords =
		(List<ContentletRelationships.ContentletRelationshipRecords>) request.getAttribute("relationshipRecords");
	if (relationshipRecords.size() > 0) {

		List<ContentletRelationships.ContentletRelationshipRecords> sortList = new ArrayList<ContentletRelationships.ContentletRelationshipRecords>();
		for (ContentletRelationshipRecords records : relationshipRecords) {
			if(records.isHasParent()){
				sortList.add( sortList.size(), records);
			}
			else{
				sortList.add(0,records);
			}
		}
		int counter=100;
		int searchCounter=1;
		int dialogCounter=1;
		for (ContentletRelationshipRecords records : sortList) {
			Relationship rel = records.getRelationship();
			List<Contentlet> contentletsList = records.getRecords();
			Structure targetStructure = null;
			String relationType= null;
			String relationName = "";
			String parentInode="";
			String isParent="";
			String relationTypeValue = rel.getRelationTypeValue();
			String relationJsName = "rel_" + UtilMethods.javaScriptifyVariable(relationTypeValue) + "_" + (records.isHasParent()?"P":"C");
			relationType= rel.getRelationTypeValue();
			if (records.isHasParent()) {
				targetStructure = rel.getChildStructure();
				relationName = rel.getChildRelationName();
				isParent="yes";

			} else {
				targetStructure = rel.getParentStructure();
				relationName = rel.getParentRelationName();
				isParent="no";
			}


%>
			<%
			    //if coming from an automatic relationship addind action will display relationships tab
				if (request.getParameter("relend")!=null) {
			%>

				<script>
				displayProperties('relationships');
				</script>
			<%
				}
			%>

	<%
		if ((rel.isChildRequired() && records.isHasParent()) || (rel.isParentRequired() && !records.isHasParent())) {
	%>
			<script type="text/javascript">
				dojo.addOnLoad(
					  function(){
						  try{
							  dijit.byId('relationships').attr('title','<span class="required"></span>&nbsp;<%= LanguageUtil.get(pageContext, "Relationships") %>');
						  }catch(ex){}
					  }
					);
			</script>
	<%
		}
	%>

	<div class="yui-g portlet-toolbar" style="margin-top:25px;">
		<div class="yui-u first">
			<span class="formIcon"></span>
			<b><%= (sortList.size()>1 && rel.isParentRequired() && !records.isHasParent()) ? "<span class=\"required\"></span>"  :
				   (sortList.size()>1 && rel.isChildRequired() && records.isHasParent()) ? "<span class=\"required\"></span>" : ""  %>
			   <%= targetStructure.getName() %>: </b><%=("yes".equals(isParent)) ? LanguageUtil.get(pageContext, "Child"): LanguageUtil.get(pageContext, "Parent") %>
			(<%= rel.getRelationTypeValue() %>)
		</div>
		<div class="yui-u" style="text-align:right;">
			<div id="<%= relationJsName %>relateMenu"></div>
		</div>
	</div>
	
		<table border="0" class="listingTable"  style="margin-bottom:30px;">
				<thead>
					<tr class="beta">
					<th width="20"><B><font class="beta" size="2"></font></B></th>
	<%
			int nFields = 3;
			boolean indexed = false;
			boolean hasListedFields = false;
			List<Field> targetFields = targetStructure.getFields();
			for (Field f : targetFields) {
				if (f.isListed()) {
				    	hasListedFields = true;
					indexed = true;
					nFields++;
	%>
						<th><B><font class="beta" size="2"><%= f.getFieldName() %> </font></B>
						</th>
	<%
				}
			}

			if (!indexed) {
	%>
						<th><B><font class="beta" size="2"> <%= LanguageUtil.get(pageContext, "No-Searchable-Fields-Found-Showing-the-Identity-Number") %> </font></B></th>
	<%
			}
	%>

					<%
						if(langs.size() > 1) {
					%>
						<th width="<%= langs.size() < 6 ? (langs.size() * 40) : 200 %>px;"><B><font class="beta" size="2"></font></B></th>
					<%
						}else{
					%>
						<th width="20"><B><font class="beta" size="2"></font></B></th>
					<%
						}
					%>
					</tr>
				</thead>
				<tbody id="<%=relationJsName%>Table">
				</tbody>
			</table>

			<!-- Hidden relationship fields -->

			<input type="hidden" name="<%= relationJsName %>Inode" id="<%= relationJsName %>Inode">
			<input type="hidden" name="selected<%= relationJsName %>Inode" id="selected<%= relationJsName %>Inode">
			<input type="hidden" name="<%= relationJsName %>_inodes" id="<%= relationJsName %>_inodes">

			<!--  Javascripts -->
			<script	language="javascript">
				//Initializing the relationship table data array

				var hasListedFields = <%= hasListedFields %>;
				var <%= relationJsName %>_Contents = new Array ();
		<%String lastIdentifier = "";




				boolean parent = false;
				for (Contentlet cont : contentletsList) {

				    try{
				        parent = cont.getBoolProperty("dotCMSParentOnTree") ;
				    }
				    catch(Exception e){

				    }
				   	if(parent && RelationshipFactory.isSameStructureRelationship(rel, targetStructure)){
			        	//continue;
			        }
				   	
				   	
				   	
				   	
					/**********   GIT 1057     *******/
					
					ContentletAPI contentletAPI = APILocator.getContentletAPI();
					Contentlet languageContentlet = null;
				    %>
				    var cont<%=UtilMethods.javaScriptifyVariable(cont.getInode())%>Siblings = new Array();
				    <%
					for(Language lang : langs){
						try{
							languageContentlet = null;
							languageContentlet = contentletAPI.findContentletByIdentifier(cont.getIdentifier(), true, lang.getId(), user, false);
						}catch (Exception e) {
							try{
							languageContentlet = contentletAPI.findContentletByIdentifier(cont.getIdentifier(), false, lang.getId(), user, false);
							}catch (Exception e1) {	}
						}
					    %>
					    var cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling = new Array();
					    <%
					    if((languageContentlet == null) || (!UtilMethods.isSet(languageContentlet.getInode()))){
					    	%>
					    	cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['langCode'] = '<%=langAPI.getLanguageCodeAndCountry(lang.getId(),null)%>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['langName'] = '<%=lang.getLanguage() %>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['langId'] = '<%=lang.getId() %>';
					    	cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['inode'] = '';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['parent'] = '<%=parent %>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['working'] = 'false';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['live'] = 'false';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['deleted'] = 'true';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['locked'] = 'false';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['siblingInode'] = '<%=cont.getInode()%>';
							
						    <%
					    }else{
					    	%>
					    	cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['langCode'] = '<%=langAPI.getLanguageCodeAndCountry(lang.getId(),null)%>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['langName'] = '<%=lang.getLanguage() %>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['langId'] = '<%=lang.getId() %>';
					    	cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['inode'] = '<%=languageContentlet.getInode()%>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['parent'] = '<%=parent %>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['working'] = '<%=String.valueOf(languageContentlet.isWorking())%>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['live'] = '<%=String.valueOf(languageContentlet.isLive())%>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['deleted'] = '<%=String.valueOf(languageContentlet.isArchived())%>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['locked'] = '<%=Boolean.valueOf(languageContentlet.isLocked() && ! languageContentlet.getModUser().equals(user.getUserId()))%>';
							cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling['siblingInode'] = '<%=cont.getInode()%>';
						    <%
					    }
					    %>
					    	cont<%=UtilMethods.javaScriptifyVariable(cont.getInode())%>Siblings[cont<%=UtilMethods.javaScriptifyVariable(cont.getInode())%>Siblings.length] = cont<%=UtilMethods.javaScriptifyVariable(cont.getInode()+lang.getId())%>Sibling;
					    <%
					    
					}
					
				/**********   GIT 1057     *******/							 				   	

					String languageCode;
					String languageName;
					Language language = langAPI.getLanguage(cont.getLanguageId());%>
				var cont = new Array();
				cont['inode'] = '<%=cont.getInode()%>';
		<%languageCode = langAPI.getLanguageCodeAndCountry(cont.getLanguageId(),null);
				languageName =  language.getLanguage();%>
				cont['langCode'] = '<%=languageCode%>';
				cont['langName'] = '<%=languageName%>';
				cont['parent'] = '<%=parent%>';
				cont['working'] = '<%=String.valueOf(cont.isWorking())%>';
				cont['live'] = '<%=String.valueOf(cont.isLive())%>';
				cont['deleted'] = '<%=String.valueOf(cont.isArchived())%>';
				cont['locked'] = '<%=Boolean.valueOf(cont.isLocked() && ! cont.getModUser().equals(user.getUserId()))%>';
				cont['siblings'] = cont<%=UtilMethods.javaScriptifyVariable(cont.getInode())%>Siblings;

		<%
					for (Field f : targetFields) {
						if (f.isListed()) {
						    String fieldName = f.getFieldName();
						    	ContentletAPI rconAPI = APILocator.getContentletAPI();
							Object fieldValueObj = rconAPI.getFieldValue(cont, f);
							String fieldValue = "";
							if (fieldValueObj != null) {
								if (fieldValueObj instanceof java.util.Date) {
								    fieldValue = modDateFormat.format(fieldValueObj);
								} else if (fieldValueObj instanceof java.sql.Timestamp){
								    java.util.Date fieldDate = new java.util.Date(((java.sql.Timestamp)fieldValueObj).getTime());
								    fieldValue = modDateFormat.format(fieldDate);
								} else {
								    fieldValue = fieldValueObj.toString();
								}
							}
		 %>
		 		cont['<%=fieldName%>'] = '<%=fieldValue.replaceAll("'","\\\\'").replaceAll("\n","").replaceAll("\r","").trim()%>';
		 <%
						}
					}
					if (!hasListedFields) {
					    Identifier contid = APILocator.getIdentifierAPI().find(cont);
		%>
				 		cont['identifier'] = '<%=contid.getInode()%>';
		<%
					}

		%>
					cont['id'] = '<%=cont.getIdentifier()%>';
		<%
					if(!lastIdentifier.equalsIgnoreCase(cont.getIdentifier()) )
					{
		%>
					cont['groupStart'] = true;
		<%
					  lastIdentifier = cont.getIdentifier();
					}//end if( lastIdentifier != cont.getIdentifier() )
		%>

				<%= relationJsName %>_Contents[<%= relationJsName %>_Contents.length] = cont;
		<%

				}
		%>

				//Function used to render language id
				function <%= relationJsName %>_lang(o) {
					var contentletLangCode = '<%= langAPI.getLanguageCodeAndCountry(contentlet.getLanguageId(),null)%>';
					var lang = '';
					var result = '';
					var anchorValue = "";
		
					if (o != null) {
						result = result + "<table width=\"100%\" class=\"relationLanguageFlag\"><tbody><tr>"
						
						for(var sibIndex = 0; sibIndex < o['siblings'].length ; sibIndex++){
																								
							result = result + '<td  class=\"relationLanguageFlag\">';
							langImg = o['siblings'][sibIndex]['langCode'];
							langName = o['siblings'][sibIndex]['langName'];
							
							if(o['siblings'][sibIndex]['live'] == 'true'){
																								
								anchorValue = "";
								if (o != null){
									anchorValue = "<a href=\"javascript:<%= relationJsName %>editRelatedContent('" + o['siblings'][sibIndex]['inode'] + "', '"+ o['siblings'][sibIndex]['siblingInode'] +"', '"+ o['siblings'][sibIndex]['langId'] +"');\"" + ">" ;
					 			}
								
								result = result +'&nbsp;&nbsp;' + anchorValue + '<img style="vertical-align: middle; border: solid 2px #33FF33; padding:2px; border-radius:5px;" src="/html/images/languages/' + langImg + '.gif" alt="'+langName+'">' + '</a>';
								
							}else if(o['siblings'][sibIndex]['deleted'] == 'true'){
								
								anchorValue = "";
								if (o != null){
									anchorValue = "<a href=\"javascript:<%= relationJsName %>editRelatedContent('" + o['siblings'][sibIndex]['inode'] + "', '"+ o['siblings'][sibIndex]['siblingInode'] +"', '"+ o['siblings'][sibIndex]['langId'] +"');\"" + ">" ;
					 			}

								result = result + '&nbsp;&nbsp;'  + anchorValue + '<img style="vertical-align: middle; border: solid 2px #66664D; padding:2px; border-radius:5px;" src="/html/images/languages/' + langImg + '_gray.gif" alt="'+langName+'">' + '</a>';
								
								
							}else{
								
								anchorValue = "";
								if (o != null){
									anchorValue = "<a href=\"javascript:<%= relationJsName %>editRelatedContent('" + o['siblings'][sibIndex]['inode'] + "', '"+ o['siblings'][sibIndex]['siblingInode'] +"', '"+ o['siblings'][sibIndex]['langId'] +"');\"" + ">" ;
					 			}
								
								result = result + '&nbsp;&nbsp;'  + anchorValue + '<img style="vertical-align: middle; border: solid 2px #FFCC11; padding:2px; border-radius:5px;" src="/html/images/languages/' + langImg + '.gif" alt="'+langName+'">' + '</a>';
								
							}
							
							result = result + "</td>";
							if((sibIndex+1)%6 == 0){
								result = result + "</tr><tr>";
							}
						}
						result = result + "</tr></tbody></table>";
					}
					return result;
				}

				//Function used to render the publish/unpublish info
			   function <%= relationJsName %>_status(o) {

				var strHTML = '&nbsp;';

			    if (o != null) {

				 live = o['live'];
				 deleted = o['deleted'];
				 working = o['working'];
                 var strHTML = new String();

			        if (live && live != "false") {
			            strHTML = strHTML + "<span class=\"liveIcon\"></span>";
			        } else if (deleted && deleted != "false") {
			            strHTML = strHTML + "<span class=\"archivedIcon\"></span>";
			        } else if (working && working != "false") {
			            strHTML = strHTML + "<span class=\"workingIcon\"></span>";
			        }
			    }

			    return strHTML;
			}

				//Function used to render relationship order number
				var <%= relationJsName %>_current_order = 1;
				var <%= relationJsName %>_identifier_current_order = 1;
				function <%= relationJsName %>_order_tf() {
					return <%= relationJsName %>_current_order++;
				}

				//Returns the given object identifier
		 		function identifier_func (o) {
					var value = "";
					if (o != null)
						value = o['identifier'];
		 			return value;
		 		}

				//Adding the rendering functions based on the relationship
				//listed fields
		<%
				int fieldsCount = 3;
				for (Field f : targetFields) {
					if (f.isListed()) {
					    fieldsCount++;
					    String fieldName = f.getFieldName();
					    String functionName = relationJsName + "_" + UtilMethods.javaScriptifyVariable(fieldName) + "_func";
		 %>
	 		function <%= functionName %> (o) {
				var value = "";
				if (o != null){
					value = "<a class=\"beta\" href=\"javascript:<%= relationJsName %>editRelatedContent('" + o['inode'] + "', '"+ o['siblingInode'] +"', '"+ o['langId'] +"');\"" + ">" + o['<%=fieldName%>'] + "</a>";
	 			}
	 			return value;
			}
		 <%
					}
				}
		%>
				//Removes the given inode from the relationship table
				function <%= relationJsName %>_removeContentFromRelationship (identifier) {
					var relationList = <%= relationJsName %>_Contents;
					var found = false;
					var size = relationList.length;
					var firstIdentIndex = 0;
					var lastIdentIndex = 0;
					for (firstIdentIndex = 0; firstIdentIndex < size; firstIdentIndex++) {
					    var content = relationList[firstIdentIndex];
					    if (content['id'] == identifier) {
					    	found = true;
						break;
					    }
					}

					if( found )
					{
					    for (lastIdentIndex = firstIdentIndex+1; lastIdentIndex < size; lastIdentIndex++) {
					       var content = relationList[lastIdentIndex];
					       if (content['id'] != identifier) {
						break;
					       }
					   }
					   relationList.splice(firstIdentIndex, lastIdentIndex-firstIdentIndex);
					}

					<%= relationJsName %>_Contents = relationList;
					<%= relationJsName %>_saveRelations ();
				}

				//Invoked to open the select contentlet popup
				function <%= relationJsName %>_addRelationship(){
					dijit.byId("<%= relationJsName %>Dialog").show();
				}

				//Callback received from the relate content
				function callback<%= relationJsName %>(content){
				//identifier refers to contentlet family to add...
					  setTimeout("ContentletAjax.getContentletData ('" + content.inode + "', <%= relationJsName %>_addRelationshipCallback)", 50);
				}

				//Invoked when a contentlet is selected to fill the contentlet data in the table
				function <%= relationJsName %>_addRelationshipCallback(selectedData){
					var data = new Array();
					var dataToRelate = new Array();
					
					// Eliminating existing relations
					for (var indexJ = 0; indexJ < selectedData.length; indexJ++) {
						var relationExists = false;
						for (var indexI = 0; indexI < <%= relationJsName %>_Contents.length; indexI++) {
							if(selectedData[indexJ]['id'] == <%= relationJsName %>_Contents[indexI]['id']){
								relationExists = true;
							}
						}
						if(!relationExists){
							dataToRelate[dataToRelate.length] = selectedData[indexJ];
						}
					}
					
					// Eliminating mulitple contentlets for same identifier
					for (var indexK = 0; indexK < dataToRelate.length; indexK++) {
						var doesIdentifierExists = false;
						for (var indexL = 0; indexL < data.length; indexL++) {
							if(dataToRelate[indexK]['id'] == data[indexL]['id'])
								doesIdentifierExists = true;
						}
						if(!doesIdentifierExists)
							data[data.length] = dataToRelate[indexK];
					}				
					
					
					if( data == null || (data != null && data.length == 0) ) {
					  return;
					}

					var dataNoRep = new Array();

					if(<%= relationJsName %>_Contents.length == 0) {
						 dataNoRep = data;
					}

					for (var i = 0; i < <%= relationJsName %>_Contents.length; i++) {
						var cont = <%= relationJsName %>_Contents[i];
						var identifier = "";
						if (cont != null) {
							identifier = cont['id'];
						}

						var k = 0;
						for(var j = 0; j < data.length; j++ ) {
							if (identifier == data[j]['id']) {
								data.splice(j,1);
							} else {
								dataNoRep[k] = data[j];
								k++;
							}
						}

					}

					for(var i=0; i < dataNoRep.length; i++) {
						dataNoRep[i]['groupStart'] = true;
					}

// 					data[0]['groupStart'] = true;
					<%= relationJsName %>RelatedCons.insertNodes(false,dataNoRep);
					<%= relationJsName %>_Contents = <%= relationJsName %>_Contents.concat(dataNoRep);
					renumberRecolorAndReorder<%= relationJsName %>();
				}



				function <%= relationJsName %>_reorder () {
					var newOrder = new Array();
					var newContent = new Array();
					var start = -1;
					var lastOrder = -1;
					var i;
					var size = <%= relationJsName %>_Contents.length;

					for (i = 0; i < size; i++) {
					  var content = <%= relationJsName %>_Contents[i];
					  if( content['groupStart'] != null ) {
					    if( start > -1 ) {
					      newOrder.push({"_start": start, "_end": i, "order": lastOrder});
					    }
					    start = i;
					    lastOrder = document.getElementById('<%= relationJsName %>_order_' + content['id']).value;
					  }
					}

					if( start > -1 ) {
					    newOrder.push({"_start": start, "_end": i, "order": lastOrder});
					}

					newOrder.sort(function(a,b) {
					  return a.order - b.order;
					});

					for(i =0; i < newOrder.length; i++) {
					  newContent = newContent.concat( <%= relationJsName %>_Contents.slice(newOrder[i]._start, newOrder[i]._end) );
					}

					<%= relationJsName %>_Contents = newContent;
					<%= relationJsName %>_saveRelations ();
				}

				//Build the hidden fields required to save the relationships
				function <%= relationJsName %>_saveRelations () {
					var hiddenField = document.getElementById ('<%= relationJsName %>_inodes');
					hiddenField.value = "<%= rel.getInode() %>,";
					for (var i = 0; i < <%= relationJsName %>_Contents.length; i++) {
						if (<%= relationJsName %>_Contents[i]['inode'] != null)
							hiddenField.value = hiddenField.value + <%= relationJsName %>_Contents[i]['inode'] + ",";
					}
				}

				//Add new content
			    function <%= relationJsName %>_addContentlet(structureInode) {

					var referer = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
					referer += 		"<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
					referer += 		"<portlet:param name='cmd' value='edit' />";
					referer += 		"<portlet:param name='inode' value='<%=contentletInode%>' />";
					referer += "</portlet:actionURL>";
					referer += "&lang=" + '<%= contentlet.getLanguageId() %>';
					referer += "&relend=true";
					referer += "&referer=" + '<%=java.net.URLDecoder.decode(referer, "UTF-8")%>';
					<%if( request.getAttribute("isRelationsihpAField") != null && !(Boolean)request.getAttribute("isRelationsihpAField")){ //DOTCMS-6893 %>
						referer += "&is_rel_tab=true";
					<%}%>


					var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
					href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
					href += "<portlet:param name='cmd' value='new' />";					
					href += "</portlet:actionURL>";

					//href += "&_EXT_11_selectedStructure=" + structureInode ; 
					href += "&inode" + "";
					href += "&selectedStructure=" + structureInode ;
					href += "&lang=" + '<%= languageId %>';
					href += "&relwith=" +'<%=contentletInode%>';
					href += "&relisparent=" + '<%= isParent %>';
					href += "&reltype=" + '<%= relationType.toString() %>';
					href += "&relname=" + '<%= relationJsName %>';
					href += "&relname_inodes=" + '<%= rel.getInode()%>';
					href += "&referer=" + escape(referer);

					if (!confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.lose.unsaved.changes")) %>'))
						return;

					window.location=href;
				}

				dojo.addOnLoad(
						function(){
							<%= relationJsName %>_saveRelations ();
						}
				);
				// DOTCMS-6097

				dojo.require("dojo.dnd.Container");
				dojo.require("dojo.dnd.Manager");
				dojo.require("dojo.dnd.Source");

				var <%= relationJsName %>RelatedCons;

				function <%= relationJsName %>buildListing(nodeId,data){
					var srcNode = document.getElementById(nodeId);
					<%if(isParent.equals("no")){//DOTCMS-6878%>
						<%= relationJsName %>RelatedCons = new dojo.dnd.Source(srcNode,{creator: <%= relationJsName %>CreateRow,isSource:false});
					<%}else{%>
						<%= relationJsName %>RelatedCons = new dojo.dnd.Source(srcNode,{creator: <%= relationJsName %>CreateRow});
					<%}%>
					<%= relationJsName %>RelatedCons.insertNodes(false,data);
					//http://jira.dotmarketing.net/browse/DOTCMS-6465
					<%= relationJsName %>RelatedCons.isDragging = false;

				}

				function <%= relationJsName %>CreateRow(item,hint){

					var tr = document.createElement("tr");

					// displays edit(pencil icon) and delete(X).
					var actionTD = document.createElement("td");
					actionTD.innerHTML = <%= relationJsName %>unrelateAction(item);
					tr.appendChild(actionTD);

					// to hold contentInode to reorder
					var span = document.createElement("span");
					dojo.addClass(span,"<%= relationJsName %>hiddenInodeField");
					dojo.style(span,"display","none");
					span.innerHTML = item['inode'];
					tr.appendChild(span);

					// to hold order sequence number to reorder
					var order = document.createElement("input");
					order.type="hidden";
					order.id = "<%= relationJsName %>_order_" + item['id'];
					dojo.addClass(order,"<%= relationJsName %>orderBox");
					order.value = <%= relationJsName %>_order_tf();
					tr.appendChild(order);

					// displays each listed field
					<%
					for (Field f : targetFields) {
						if (f.isListed()) {
						    String fieldName = f.getFieldName();
						    String functionName = relationJsName + "_" + UtilMethods.javaScriptifyVariable(fieldName) + "_func";
			 		%>
						 	var field<%= functionName %>TD = document.createElement("td");
						 	var div = document.createElement("div");
						 	div.style.width = '100%';
						 	div.style.overflow = 'hidden';
						 	div.style.height = '20px';
						 	div.innerHTML = <%= functionName %>(item);
							field<%= functionName %>TD.appendChild(div);
							tr.appendChild(field<%= functionName %>TD);
			 		<%
						}
					}


					if (!indexed) {
					%>	// displays content's identifier, if no listed fields exist.
					 	var identifierTD = document.createElement("td");
					 	identifierTD.innerHTML = identifier_func(item);
						tr.appendChild(identifierTD);
					<%
					}
					%>

					<%
					if(langs.size() > 1) {
					%>	// displays the publish/unpublish/archive status of the content and language flag, if multiple languages exists.
						var langTD = document.createElement("td");					
						langTD.innerHTML = <%= relationJsName %>_lang(item);
						tr.appendChild(langTD);
					<%
					}else{
					%>

					// displays the publish/unpublish/archive status of the content only.
					var statusTD = document.createElement("td");
					statusTD.innerHTML = <%= relationJsName %>_status(item);
					tr.appendChild(statusTD);

					<%
					}
					%>

					return {node: tr, data: item, type: "text"};
				}


				function <%= relationJsName %>init(){

					// Initializing related contents table.
					<%= relationJsName %>buildListing('<%= relationJsName %>Table',<%= relationJsName %>_Contents);

					// connectin drag and drop to reorder functionality
					dojo.subscribe("/dnd/drop", function(source){
					  	renumberRecolorAndReorder<%= relationJsName %>(source);
					});

					renumberRecolorAndReorder<%= relationJsName %>();
				}

				dojo.addOnLoad(<%= relationJsName %>init);


				// to edit a related content, with proper referer
				function <%= relationJsName %>editRelatedContent(inode, siblingInode, langId){
					
					if (!confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.lose.unsaved.changes")) %>'))
						return;

					var referer = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
					referer += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
					referer += "<portlet:param name='cmd' value='edit' />";					
					referer += "</portlet:actionURL>";
					referer += "&inode="+'<%=contentletInode%>';
					referer += "&lang=" + '<%= contentlet.getLanguageId() %>';
					referer += "&relend=true";
					referer += "&referer=" + '<%=java.net.URLDecoder.decode(referer, "UTF-8")%>';
					<%if( request.getAttribute("isRelationsihpAField") != null && !(Boolean)request.getAttribute("isRelationsihpAField")){ //DOTCMS-6893 %>
						referer += "&is_rel_tab=true";
					<%}%>

					var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
					href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
					href += "<portlet:param name='cmd' value='edit' />";
					href += "</portlet:actionURL>";
					href += "&inode="+inode;
					if(inode == ''){
						href += "&sibbling=" + siblingInode;
						href += "&lang=" + langId;
					}			
					href += "&referer=" + escape(referer);
					document.location.href = href;
				}

				// displays edit icon and link to edit content
		 		function <%= relationJsName %>editAction (o) {
					var value = "";
					if (o != null){
						value = "<a class=\"beta\" href=\"javascript:<%= relationJsName %>editRelatedContent('" + o['inode'] + "', '"+ o['siblingInode'] +"', '"+ o['langId'] +"');\"" + "><span class=\"editIcon\"></span></a>";
		 			}
		 			return value;
		 		}

		 		// displays 'X' to unrelate content
		 		function <%= relationJsName %>unrelateAction (o) {
		 			var value = "";
					if(<%= canUserWriteToContentlet %>){
						value = "<a class=\"beta\" href=\"javascript:<%= relationJsName %>unrelateContent('<%=contentletInode%>','" + o['inode'] + "','"+ o['id'] +"');\"" + "><span class=\"deleteIcon\"></span></a>";
					}else{
						value = "<a class=\"beta\" href=\"javascript:alert('<%= LanguageUtil.get(pageContext, "dont-have-permissions-msg") %>');" + "><span class=\"deleteIcon\"></span></a>";
					}
					return value;
		 		}

		 		function <%= relationJsName %>unrelateContent(contentletInode,inodeToUnrelate,identifierToUnrelate){
		 			<%= relationJsName %>RelatedCons.deleteSelectedNodes();
		 			<%= relationJsName %>_removeContentFromRelationship (identifierToUnrelate);
		 			ContentletAjax.unrelateContent(contentletInode,inodeToUnrelate,'<%=rel.getInode()%>',<%= relationJsName %>unrelateCallback);
		 			renumberRecolorAndReorder<%= relationJsName %>();
		 		}

		 		function <%= relationJsName %>unrelateCallback (data) {
		 			showDotCMSSystemMessage(data);
		 		}

				function renumberRecolorAndReorder<%= relationJsName %>(source){
					recolor<%= relationJsName %>Rows();
					eles = dojo.query(".<%= relationJsName %>orderBox");
					for(i = 0;i<eles.length;i++){
						eles[i].value=i+1;
					}
					<%= relationJsName %>_reorder();
					<%= relationJsName %>setButtonState();
				}

				function recolor<%= relationJsName %>Rows() {
					var eles = dojo.query("table#<%= relationJsName %>Table .dojoDndItem");
					for(i = 0;i<eles.length;i++){
						if(i % 2 ==0){
							dojo.style(eles[i], "background", "#fff");
						}
						else{
							dojo.style(eles[i], "background", "#eee");
						}
					}
					var eles = dojo.query(".dojoDndItem .item_cell");
					var bg = "#eee";
					for(i = 0;i<eles.length;i++){
						if(i % 8 == 0)
							bg = bg == "#fff"?"#eee":"#fff";
						dojo.style(eles[i], "background", bg);
					}
				}


				// parent/child/permissions regarding relate new/existing content.
				var <%= relationJsName %>canRelateNew;
				var <%= relationJsName %>canRelateExisting;

				function <%= relationJsName %>setButtonState(){

					<%= relationJsName %>canRelateNew = true;//always true
					<%= relationJsName %>canRelateExisting = true;
					var prefix = '<%= relationJsName %>';
					var isParentString = '<%= isParent.toLowerCase().trim() %>';
					var isParent = false;
					if(isParentString == "yes")
						isParent = true;
					var entries = eval(prefix + "_Contents").length;

					if(isNewContentlet){// for both parent/child
						var <%= relationJsName %>canRelateNew = false;
					}
					if(!isNewContentlet && !isParent){
						if(entries == 0)
							var <%= relationJsName %>canRelateNew = true;
						else
							var <%= relationJsName %>canRelateNew = false;
					}

					<% if (!records.isHasParent() && rel.getCardinality() == com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()) { %>
					if (<%= relationJsName %>_Contents.length > 0)
						<%= relationJsName %>canRelateExisting = false;
					else
						<%= relationJsName %>canRelateExisting = true;
					<% } %>

					var canUserWriteToContentlet = <%= canUserWriteToContentlet %>;

					if(!canUserWriteToContentlet){
						<%= relationJsName %>canRelateExisting = false;
						<%= relationJsName %>canRelateNew = false;
					}

					addRelateButtons<%= relationJsName %>(<%= relationJsName %>canRelateExisting,<%= relationJsName %>canRelateNew);
				}

				function addRelateButtons<%= relationJsName %>(canRelateExisting,canRelateNew) {

					dojo.byId("<%= relationJsName %>relateMenu").innerHTML = "";

					var canUserWriteToContentlet = <%=canUserWriteToContentlet%>;
					if(!canUserWriteToContentlet)
						return;

			        var menu = new dijit.Menu({
			            style: "display: none;"
			        });

			        var menuItem1 = new dijit.MenuItem({
			            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Relate")) %>",
						iconClass: "searchIcon",
						onClick: function() {
							<%= relationJsName %>_addRelationship();
			            }
			        });
			        if(!canRelateExisting)
			        	menuItem1.attr('disabled','disabled');
			        menu.addChild(menuItem1);

			        var menuItem2 = new dijit.MenuItem({
			            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Relate-New-Content")) %>",
						iconClass: "plusIcon",
						onClick: function() {
							<%= relationJsName %>_addContentlet('<%= targetStructure.getInode() %>');
			            }
			        });
			        if(!canRelateNew)
			        	menuItem2.attr('disabled','disabled');
			        menu.addChild(menuItem2);

			        var button = new dijit.form.ComboButton({
			            label: "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Relate")) %>",
						iconClass: "searchIcon",
						dropDown: menu,
						onClick: function() {
							<%= relationJsName %>_addRelationship();
			            }
			        });
			        if(!canRelateExisting)
			        	button.attr('disabled','disabled');

			        dojo.byId("<%= relationJsName %>relateMenu").appendChild(button.domNode);
			    }

			</script>

			<div id="<%= relationJsName %>Dialog" dojoType="dotcms.dijit.form.ContentSelector" structureInode="<%= targetStructure.getInode() %>" relationJsName="<%= relationJsName %>" multiple="true" onContentSelected="callback<%= relationJsName %>" title="" counter_radio="<%= counter %>" searchCounter="<%= searchCounter %>" dialogCounter="<%= dialogCounter %>"></div>
<%
            counter=counter+100;
            searchCounter+=10000;
            dialogCounter++;
		}
%>
			<script type="text/javascript">
			dojo.require("dotcms.dijit.form.ContentSelector");
			var isNewContentlet = false;
			<%if(!InodeUtils.isSet(contentletInode)){%>
				isNewContentlet = true;
 			<%}%>
			<%if(UtilMethods.isSet(request.getAttribute("is_rel_tab")) && (request.getAttribute("is_rel_tab").toString().equalsIgnoreCase("true"))){%>
				dojo.addOnLoad(
				  function(){
						  dijit.byId('mainTabContainer').selectChild('relationships');

				  }
				);
			<%}%>
			function toggleCheckbox(id){				
				var chk = document.getElementById(id);
				var elems = chk.getElementsByTagName ("input");				
				var len = elems.length;	
				
				for ( var i = 0; i < len; i++ ){
					if (elems[i].checked){
						dijit.byId(elems[i].id).set("checked",false);
				    }else{
				    	dijit.byId(elems[i].id).set("checked",true);
				    }				
				}
			}
			</script>
<%
	} else {
%>
	<b><%= LanguageUtil.get(pageContext, "No-Relationships-Found") %></b>
<%
	}
%>


