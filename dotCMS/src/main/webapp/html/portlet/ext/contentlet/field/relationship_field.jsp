<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships"%>
<%@page import="com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords"%>
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
<%@page import="com.dotmarketing.util.Config" %>
<%@page import="com.dotmarketing.util.StringUtils" %>
<%@page import="com.dotmarketing.business.IdentifierCache"%>
<%@page import="com.dotmarketing.business.FactoryLocator"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.util.UtilHTML"%>
<%@page import="java.util.Optional" %>
<%@page import="java.util.stream.Collectors" %>
<%@page import="java.util.function.Function" %>
<%@page import="java.util.Objects" %>
<%@page import="com.google.common.collect.ImmutableMap" %>
<%@page import="io.vavr.Tuple2" %>
<%@ page import="com.dotmarketing.exception.DotDataException" %>
<%@ page import="com.dotmarketing.exception.DotSecurityException" %>
<%
	LanguageAPI langAPI = APILocator.getLanguageAPI();
	PermissionAPI conPerAPI = APILocator.getPermissionAPI();
	List<Language> langs = langAPI.getLanguages();

    List<ContentletRelationships.ContentletRelationshipRecords> recordList = (List<ContentletRelationships.ContentletRelationshipRecords>)request.getAttribute("relationshipRecords");

    // if we don't a relationship in our attributes
    if(recordList==null || recordList.isEmpty()){
        return;
    }
    
    Contentlet contentlet = (Contentlet) request.getAttribute("contentlet");
    String contentTitle = (contentlet !=null && contentlet.getTitle() !=null) ? contentlet.getTitle() : LanguageUtil.get(pageContext, "modes.New-Content");
    contentTitle = UtilMethods.truncatify(contentTitle, 150);
    ContentletRelationships.ContentletRelationshipRecords  records = recordList.get(0);
    Relationship relationship = records.getRelationship();
    List<Contentlet> relatedContents = records.getRecords();
    
	boolean canUserPublishContentlet = (request.getAttribute("canUserPublishContentlet") != null) ? ((Boolean)request.getAttribute("canUserPublishContentlet")).booleanValue() : false;
    boolean canUserWriteToContentlet = (InodeUtils.isSet(contentlet.getInode())) ?  conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_WRITE,user) : true;

	java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
	params.put("struts_action",new String[] {"/ext/contentlet/view_contentlets_popup"});
	String viewContentsPopupURL = PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);

	DateFormat modDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
	modDateFormat.setTimeZone(timeZone);


	String contentletInode = String.valueOf(contentlet.getInode());

    final List<String> listOfRelatedInodes = new ArrayList<>();
    for(Contentlet relatedContent : relatedContents ){
        listOfRelatedInodes.add(relatedContent.getInode());
    }


	//Variable used to return after the work is done with the contentlet
	String referer = "";
	if (request.getParameter("referer") != null) {
		referer = request.getParameter("referer");
	} else {
		params = new HashMap<>();
		params.put("struts_action",new String[] {"/ext/contentlet/edit_contentlet"});
		params.put("inode",new String[] { contentletInode + "" });
		params.put("cmd",new String[] { Constants.EDIT });
		referer = PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
	}


	boolean thereCanBeOnlyOne = records.doesAllowOnlyOne();

	Structure targetStructure = null;
	String relationType= relationship.getRelationTypeValue();
	String relationName = "";
	String isParent="";

	String relationJsName = "rel_" + UtilMethods.javaScriptifyVariable(relationType) + "_" + (records.isHasParent()?"P":"C");

	if (records.isHasParent()) {
		targetStructure = relationship.getChildStructure();
		relationName = relationship.getChildRelationName();
        isParent="yes";
	} else {
		targetStructure = relationship.getParentStructure();
		relationName = relationship.getParentRelationName();
        isParent="no";
	}

	// issue-19204
	double randomNumber = Math.random();

    final Map<String, String> specialFields = ImmutableMap.of(
            "title", "Title",
            "titleImage", "Title-Image",
            "languageId", "Language");
    final com.dotcms.contenttype.model.field.Field relatedField =
            (com.dotcms.contenttype.model.field.Field) request.getAttribute("relatedField");
    final List<String> showFieldNames = relatedField
            .fieldVariables()
            .stream()
            .filter(fv -> fv.key().equals("showFields"))
            .findFirst()
            .map(fieldVariable -> Arrays
                    .stream(fieldVariable.value().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    final Optional<Structure> structure = recordList
            .stream()
            .map(record -> record.getRelationship().getParentStructureInode().equals(contentlet.getContentTypeId())
                    ? record.getRelationship().getChildStructure()
                    : record.getRelationship().getParentStructure())
            .findFirst();
    final Map<String, Field> relatedFields = structure
            .map(st -> st
                    .getFields()
                    .stream()
                    .collect(Collectors.toMap(Field::getVelocityVarName, Function.identity())))
            .orElse(Collections.emptyMap());
    final List<Tuple2<String, String>> showFields = showFieldNames
            .stream()
            .map(fieldName -> {
                final Field field = relatedFields.get(fieldName);
                final String fieldTitle  = (field != null)
                        ? field.getFieldName()
                        : specialFields.getOrDefault(fieldName, null);
                return fieldTitle != null ? new Tuple2<>(fieldName, fieldTitle) : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    final boolean overrideRelated = !showFields.isEmpty() && showFields.size() == showFieldNames.size();
    final String jsFieldNames = overrideRelated
            ? showFieldNames
                    .stream()
                    .map(fieldName -> String.format("'%s'", fieldName.trim()))
                    .collect(Collectors.joining(","))
            : "";
    final List<Contentlet> relatedContentlets = recordList
            .stream()
            .findFirst()
            .map(ContentletRelationshipRecords::getRecords)
            .orElse(Collections.emptyList());
    final Optional<Contentlet> relatedContentlet = relatedContentlets.stream().findFirst();
    final String jsSpecialFields = specialFields
           .keySet()
           .stream()
           .map(key -> {
               String fieldValue;
               if (key.equals("title")) {
                   fieldValue = relatedContentlet.map(Contentlet::getTitle).orElse(null);
                   if (fieldValue == null) {
                       final Field field = relatedFields.get(key);
                       fieldValue = field != null ? field.getTitle() : "";
                   }
               } else if (key.equals("titleImage")) {
                   final Optional <com.dotcms.contenttype.model.field.Field> titleImage =
                           relatedContentlet.flatMap(Contentlet::getTitleImage);
                   fieldValue = titleImage
                           .map(field -> relatedContentlet.get().getInode() + "/" + field.variable())
                           .orElse("");
               } else {
                   fieldValue = "";
               }
			   fieldValue = UtilMethods.makeHtmlSafe(fieldValue);
			   return String.format("%s: \"%s\"", key, fieldValue);
           })
           .collect(Collectors.joining(","));
%>
    <style type="text/css" media="all">
        @import url(/html/portlet/ext/contentlet/field/relationship_field.css);
		.loader-spinner {
			border-radius: 50%;
			width: 40px;
			height: 40px;
			display: inline-block;
			vertical-align: middle;
			font-size: 10px;
			position: relative;
			text-indent: -9999em;
			border: 4px solid rgba(107, 77, 226, 0.2);
			border-left-color: #6b4de2;
			transform: translateZ(0);
			animation: load8 1.1s infinite linear;
			overflow: hidden;
		}
		@-webkit-keyframes load8 {
			0% {
				-webkit-transform: rotate(0deg);
				transform: rotate(0deg);
			}
			100% {
				-webkit-transform: rotate(360deg);
				transform: rotate(360deg);
			}
		}
		@keyframes load8 {
			0% {
				-webkit-transform: rotate(0deg);
				transform: rotate(0deg);
			}
			100% {
				-webkit-transform: rotate(360deg);
				transform: rotate(360deg);
			}
}
    </style>

	<table border="0" class="listingTable" style="margin-bottom: 30px;">
		<thead>
			<tr>
				<th>
					<div class="portlet-toolbar__actions-secondary">
						<div id="<%=relationJsName%>relateMenu" style="background: white"></div>
					</div>
				</th>
<%
			if (!overrideRelated) {
%>
                <th style="min-width: 100px"></th>
                <th width="100%"><%=LanguageUtil.get(pageContext, "Title")%></th>
<%
			} else {
                for (final Tuple2<String, String> field : showFields) {
%>
                    <th style="min-width: 80px"><%=LanguageUtil.get(pageContext, field._2())%></th>
<%
                }
			}
%>
				<th style="min-width: 60px"></th>
				<th style="min-width: 60px"></th>
			</tr>
		</thead>
		<tbody id="<%=relationJsName%>Table"></tbody>
	</table>
	
    <!-- Hidden relationship fields -->

	<input type="hidden" name="<%= relationJsName %>Inode" id="<%= relationJsName %>Inode">
	<input type="hidden" name="selected<%= relationJsName %>Inode" id="selected<%= relationJsName %>Inode">
	<input type="hidden"  name="<%= relationJsName %>_inodes" id="<%= relationJsName %>_inodes">

	<!--  Javascripts -->
	<script	language="javascript">
        dojo.require("dojo.dnd.Container");
        dojo.require("dojo.dnd.Manager");
        dojo.require("dojo.dnd.Source");
        dojo.require("dotcms.dijit.form.ContentSelector");
	
		//Initializing the relationship table data array
		var <%= relationJsName %>_Contents = new Array();
		var <%= relationJsName %>_jsOverrideRelated = <%= overrideRelated %>;
		var <%= relationJsName %>_specialFields = { <%= jsSpecialFields %> };
		var <%= relationJsName %>_showFields = [ <%= jsFieldNames %> ];

		// Add the relation to the map, and set it to false.
		relationsLoadedMap['<%=relationJsName%>'] = false;

        function getCurrentLanguageIndex(o) {
            var index = 0;

            for (var sibIndex = 0; sibIndex < o['siblings'].length ; sibIndex++) {
                if (o['langCode'].toLowerCase() === o['siblings'][sibIndex]['langCode'].toLowerCase()) {
                    index = sibIndex;
                    break;
                }
            }
             
            return index;
        }

		//Function used to render language id
		function <%= relationJsName %>_lang(o, preId) {
			if (o !== null && dijit.byId("langcombo")) {
			    var contentletLangCode = '<%= langAPI.getLanguageCodeAndCountry(contentlet.getLanguageId(),null)%>';
                var currentLanguageIndex = getCurrentLanguageIndex(o);
                var lang = '';
                var result = '';
                var anchorValue = "";
                var imgLangName = '';

                result = '<div class="relationLanguageFlag" data-id="' + preId + '_' + o.id +'" id="' + o.id + '"><div value="' + currentLanguageIndex + '" data-dojo-type="dijit/form/Select">';

				for(var sibIndex = 0; sibIndex < o['siblings'].length ; sibIndex++){
					langImg = o['siblings'][sibIndex]['langCode'];
	                langName = o['siblings'][sibIndex]['langName'];
	                var siblingExists=true;
	                if (o['siblings'][sibIndex]['deleted'] == 'true') {
	                    imgLangName = langImg + '_gray';
	                    siblingExists=false;
	                } else {
	                    imgLangName = langImg;
	                }
	
	                var dataTags = 'data-inode="' + o['siblings'][sibIndex]['inode'] + '" data-siblingInode="' + o['siblings'][sibIndex]['siblingInode'] + '" data-langId="' + o['siblings'][sibIndex]['langId'] + '"';
	                var imgTag = '<img style="vertical-align: middle; padding:2px 8px 2px 2px;" src="/html/images/languages/' + imgLangName + '.gif" alt="' + langName +'">';
	                result = result + '<span value="' + sibIndex + '"><span ' + ((siblingExists) ? '' : 'style="text-decoration:line-through "') + ' onclick="openContentletPage(this)" ' + dataTags + '>' + imgTag + '(' + langImg + ')</span></span>';
				}

                result = result + "</div></div>";
			}
			return result;
         }

         function openContentletPage(dropdownElem) {
             var inode = getDataTagValue(dropdownElem.outerHTML, 'inode');
             var siblingInode = getDataTagValue(dropdownElem.outerHTML, 'siblinginode');
             var langId = getDataTagValue(dropdownElem.outerHTML, 'langid');
             <%= relationJsName %>editRelatedContent( inode, siblingInode, langId);
         }

         function getDataTagValue(tag, attr) {
             var startIndex = tag.indexOf(attr + '="');
             var endIndex = tag.indexOf('"', startIndex + attr.length + 2);
             return tag.substring(startIndex, endIndex).replace(attr, '').replace(/="/, '');
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
				value = "<a class=\"beta\" href=\"javascript:<%= relationJsName %>editRelatedContent('" + o['inode'] + "', '"+ o['siblingInode'] +"', '"+ o['langId'] +"');\"" + ">" + o['identifier'] + "</a>";
 			return value;
 		}

		//Adding the rendering functions based on the relationship
		//listed fields

		function <%= relationJsName%>_func (o) {
			var value = "";
			if (o != null){
				value = "<a class=\"beta\" href=\"javascript:<%= relationJsName %>editRelatedContent('" + o['inode'] + "', '"+ o['siblingInode'] +"', '"+ o['langId'] +"');\"" + ">" + o['<%= relationJsName %>'] + "</a>";
			}
			return value;
	    }

	    function <%= relationJsName%>EditRelatedContentWrap(o, content) {
	    var siblingInode = o['siblingInode'] || '';
            return o != null
                ? "<a href=\"javascript:<%= relationJsName %>editRelatedContent('" + o['inode'] + "', '"+ siblingInode +"', '"+ o['langId'] +"');\"" + ">" + content + "</a>"
                : ""
        }

        function <%= relationJsName%>WriteLinkTitle (o) {
            return <%= relationJsName%>EditRelatedContentWrap(o, o['title']);
        }
		
        function numberOfRows<%= relationJsName%> () {
	    	var dataRows = document.querySelectorAll('.dataRow<%=relationJsName%>');
	    	return dataRows.length;
        }
		
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

			if( found ){
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
			dijit.byId("<%= relationJsName %>Dialog").show(true);
                  dijit.byId("<%= relationJsName %>Dialog")._doSearchPage1();
		}

		//Map to determine if a content is multiligual and if there is a version in the selected language
		function mapToCheckCurrentLangExists(listRelationships){
			const idExists = new Map();
			for (var indexK = 0; indexK < listRelationships.length; indexK++) {
				idExists.set(listRelationships[indexK]['identifier'], false);
				for (var indexL = 0; indexL < listRelationships.length; indexL++) {
					if(listRelationships[indexK]['identifier'] == listRelationships[indexL]['identifier'] &&
							listRelationships[indexL]['langId'] == <%= contentlet.getLanguageId() %>) {
						idExists.set(listRelationships[indexK]['identifier'], true);
						break;
					}
				}
			}
			return idExists;
		}


		//Invoked when a contentlet is selected to fill the contentlet data in the table
		function <%= relationJsName %>_addRelationshipCallback(selectedData){

			//A new list will be created with all the relationships but will remove multilingual ones, and in that
			//case will add the one of the selected language.
			const mapIdCurrentLangExist = mapToCheckCurrentLangExists(selectedData);
			const newList = [];
			for (var indexL = 0; indexL < selectedData.length; indexL++) {
				var currentContent = selectedData[indexL];
				var currentContentId = currentContent['identifier'];
				var mapValue = mapIdCurrentLangExist.get(currentContentId);
				if(mapValue && currentContent['langId'] == <%= contentlet.getLanguageId() %>){
					newList.push(currentContent);
				}
				if(!mapValue){
					newList.push(currentContent);
				}
			}

			var data = new Array();
			var dataToRelate = new Array();
            var entries = numberOfRows<%= relationJsName%>();
			// Eliminating existing relations
			for (var indexJ = 0; indexJ < newList.length; indexJ++) {
				var relationExists = (<%=thereCanBeOnlyOne%> && (entries > 0 || dataToRelate.length>0)) ? true : false;
				for (var indexI = 0; indexI < <%= relationJsName %>_Contents.length; indexI++) {
					if(newList[indexJ]['id'] == <%= relationJsName %>_Contents[indexI]['id']){
						relationExists = true;
					}
				}
				if(!relationExists){
					dataToRelate[dataToRelate.length] = newList[indexJ];
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

			// Remove the loading
			document.querySelector('#relationship-loading<%=relationJsName%>')?.remove();

			if( data == null || (data != null && data.length == 0) ) {
				// If it's empty, set the value to true as well.
				relationsLoadedMap['<%=relationJsName%>'] = true;
				renumberAndReorder<%= relationJsName %>();
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

            if ('<%= isParent %>' === 'yes') {
                <%= relationJsName %>RelatedCons.insertNodes(false,dataNoRep);
            } else {
                for(var j = 0; j < dataNoRep.length; j++ ) {
                    var { node, data, type } = CreateRow<%= relationJsName %>(dataNoRep[j])
                    <%= relationJsName %>RelatedCons.appendChild(node);
                }
            }

			<%= relationJsName %>_Contents = <%= relationJsName %>_Contents.concat(dataNoRep);
			// This function is called when relations are loaded or every time we add a new relation.
			// So we set the map value to true.
			relationsLoadedMap['<%=relationJsName%>'] = true;
			renumberAndReorder<%= relationJsName %>();
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
			hiddenField.value = "<%= relationship.getInode() %>,";
			for (var i = 0; i < <%= relationJsName %>_Contents.length; i++) {
				if (<%= relationJsName %>_Contents[i]['inode'] != null)
					hiddenField.value = hiddenField.value + <%= relationJsName %>_Contents[i]['inode'] + ",";
			}
		}
		

		//Add new content
	    function <%= relationJsName %>_addContentlet(structureInode) {

	    	var myNode = (currentContentletInode==null || currentContentletInode=='') ? workingContentletInode : currentContentletInode;
	    	var relationshipReturnValue = { inode: myNode, title: "<%=UtilMethods.escapeDoubleQuotes(contentTitle)%>" };
	    	localStorage.setItem("dotcms.relationships.relationshipReturnValue",  JSON.stringify(relationshipReturnValue));
	    	
			var referer = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
			referer += 	"<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
			referer += "<portlet:param name='cmd' value='edit' />";
			referer += "</portlet:actionURL>";
			referer += "&inode="+myNode;
			referer += "&lang=" + '<%= contentlet.getLanguageId() %>';
			referer += "&relend=true";

			var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
			href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
			href += "<portlet:param name='cmd' value='new' />";
			href += "</portlet:actionURL>";

			//href += "&_content_selectedStructure=" + structureInode ;
			href += "&inode" + "";
			href += "&selectedStructure=" + structureInode ;
			href += "&lang=" + '<%= langAPI.getDefaultLanguage().getId() %>';
			href += "&relwith=" +'<%=contentletInode%>';
			href += "&relisparent=" + '<%= isParent %>';
			href += "&reltype=" + '<%= relationType.toString() %>';
			href += "&relname=" + '<%= relationJsName %>';
			href += "&relname_inodes=" + '<%= relationship.getInode()%>';
			href += "&referer=" + escape(referer);

			if (!confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.lose.unsaved.changes")) %>'))
                      return;

			window.location=href;
		}
	    
		// DOTCMS-6097

		var <%= relationJsName %>RelatedCons;


		function <%= relationJsName %>buildListing(nodeId,data){

			var srcNode = document.getElementById(nodeId);

            if ('<%= isParent %>' === 'yes') {
                <%= relationJsName %>RelatedCons = new dojo.dnd.Source(srcNode,{creator: CreateRow<%= relationJsName %>});
                <%= relationJsName %>RelatedCons.insertNodes(false,data);
                // http://jira.dotmarketing.net/browse/DOTCMS-6465
                <%= relationJsName %>RelatedCons.isDragging = false;
            } else {
                <%= relationJsName %>RelatedCons = srcNode;
            }

		}

        function createLangTd(row, item, preId) {
            var langTD = document.createElement("td");
            row.appendChild(langTD);
            // displays the publish/unpublish/archive status of the content and language flag, if multiple languages exists.
            <%if(langs.size() > 1) {%>
                if(dijit.byId("langcombo")){
                    langTD.style.whiteSpace="nowrap";
                    langTD.style.textAlign = 'right';
                    langTD.innerHTML = <%= relationJsName %>_lang(item, preId);
                    setTimeout(function () { dojo.parser.parse(document.querySelector('[data-id="' + preId + '_'+ item.id +'"]')); }, 0);
                }
            <%}%>
        }

        function createImageCell(row, item) {
            var imgCell = row.insertCell (row.cells.length);
            imgCell.style.whiteSpace="nowrap";
            imgCell.style.textAlign = 'center';
            var imageValue;
			imageValue = item.hasTitleImage === 'true'
				? '<img class="listingTitleImg" src="/dA/' + item.inode + '/titleImage/500w/50q">'
				: '<span class="'+item.iconClass+'" style="font-size:24px;width:auto;"></span>';

            imgCell.innerHTML = <%= relationJsName%>EditRelatedContentWrap(item, imageValue);
        }

        function CreateRow<%= relationJsName %>(item, hint) {
            var row = document.createElement("tr");
            row.className="dataRow<%= relationJsName %>";

            // displays edit(pencil icon) and delete(X).
            var actionTD = document.createElement("td");
            dojo.style(actionTD, "text-align", "center");
            actionTD.innerHTML = <%= relationJsName %>unrelateAction(item);
            row.appendChild(actionTD);

            if (<%= relationJsName%>_jsOverrideRelated === false) {
                createImageCell(row, item);

                var titleCell = row.insertCell (row.cells.length);
                titleCell.innerHTML = <%= relationJsName%>WriteLinkTitle (item);

                createLangTd(row, item, '<%= relationJsName %>');
            } else {
                <%= relationJsName %>_showFields.forEach(function(fieldName) {
                    var fieldValue = '';
                    if (fieldName === 'languageId') {
                        createLangTd(row, item, '<%= relationJsName %>');

                    } else if ((item[fieldName] && (fieldName === 'titleImage' || item[fieldName].includes("assets")))
							|| (!item[fieldName] && item['hasImageFields'] === 'true') ) {
                        createImageCell(row, item);
                    } else{
						var fieldCell = row.insertCell(row.cells.length);
						if (fieldName === "id"){
							fieldCell.innerHTML = <%= relationJsName%>EditRelatedContentWrap(
									item,
									item["siblings"][0][fieldName] || item["siblings"][0][fieldName.toUpperCase()] || item[fieldName]);
						} else {
							fieldCell.innerHTML = <%= relationJsName%>EditRelatedContentWrap(
									item,
									item[fieldName] || "");
						}

                    }
                });
            }

			// displays the publish/unpublish/archive status of the content only.
			var statusTD = document.createElement("td");
			statusTD.style.whiteSpace="nowrap";
			statusTD.innerHTML = <%= relationJsName%>EditRelatedContentWrap(item, item.statusIcons);
			row.appendChild(statusTD);

			// to hold contentInode to reorder
			var span = document.createElement("span");
			dojo.addClass(span,"<%= relationJsName %>hiddenInodeField");
			dojo.style(span,"display","none");
			span.innerHTML = item['inode'];
			row.appendChild(span);

			// to hold order sequence number to reorder
			var order = document.createElement("input");
			order.type="hidden";
			order.id = "<%= relationJsName %>_order_" + item['id'];
			dojo.addClass(order,"<%= relationJsName %>orderBox");
			order.value = <%= relationJsName %>_order_tf();
			row.appendChild(order);

			return {node: row, data: item, type: "text"};
		}

		function <%= relationJsName %>init(){
			add<%= relationJsName %>Loading();

			// Initializing related contents table.
			<%= relationJsName %>buildListing('<%= relationJsName %>Table',<%= relationJsName %>_Contents);

			// connecting drag and drop to reorder functionality
			dojo.subscribe("/dnd/drop", function(source){
			  	renumberAndReorder<%= relationJsName %>(source);
			});
		}

		dojo.addOnLoad(<%= relationJsName %>init);


		// to edit a related content, with proper referer
		function <%= relationJsName %>editRelatedContent(inode, siblingInode, langId){
			if (!confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.contentlet.lose.unsaved.changes")) %>')){
                return;
			}
			
            var myNode = (currentContentletInode==null || currentContentletInode=='') ? workingContentletInode : currentContentletInode;
            var relationshipReturnValue = { inode: myNode, title: "<%=UtilMethods.escapeDoubleQuotes(contentTitle)%>" };
            localStorage.setItem("dotcms.relationships.relationshipReturnValue",  JSON.stringify(relationshipReturnValue));
            
			var referer = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
			referer += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
			referer += "<portlet:param name='cmd' value='edit' />";
			referer += "</portlet:actionURL>";
			referer += "&inode="+'<%=contentletInode%>';
			referer += "&lang=" + '<%= contentlet.getLanguageId() %>';
			referer += "&relend=true";
			<%if( request.getAttribute("isRelationsihpAField") != null && !(Boolean)request.getAttribute("isRelationsihpAField")){ //DOTCMS-6893 %>
				referer += "&is_rel_tab=true";
			<%}%>
			referer += "&referer=" + '<%=java.net.URLDecoder.decode(referer, "UTF-8")%>';

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
            if ('<%= isParent %>' === 'yes') {
                <%= relationJsName %>RelatedCons.deleteSelectedNodes();
            } else {
                var row = document.getElementById('<%= relationJsName %>_order_' + identifierToUnrelate).closest('tr');
                row.remove();
            }
 			<%= relationJsName %>_removeContentFromRelationship (identifierToUnrelate);
 			renumberAndReorder<%= relationJsName %>();
 		}

 		function <%= relationJsName %>unrelateCallback (data) {
 			showDotCMSSystemMessage(data);
 		}

		function renumberAndReorder<%= relationJsName %>(source){

			eles = dojo.query(".<%= relationJsName %>orderBox");
			for(i = 0;i<eles.length;i++){
				eles[i].value=i+1;
			}
			<%= relationJsName %>_reorder();
			<%= relationJsName %>setButtonState();
			var cons = numberOfRows<%= relationJsName%>();
			
			dojo.destroy("<%=relationJsName%>TableMessage")

            if(cons== undefined || cons==0 ){
                 var srcNode = document.getElementById("<%=relationJsName%>Table");
                 var row = document.createElement("tr");
                 row.id="<%=relationJsName%>TableMessage"
                 var cell = row.insertCell (0);
                 cell.setAttribute("colspan", "100");
                 cell.setAttribute("style","text-align:center");
                 cell.innerHTML = <%=thereCanBeOnlyOne%> ? "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.relationship.selectOne")) %>" : "<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.relationship.selectMulti")) %>";
                 srcNode.appendChild(row);
             } 
		}

		function <%= relationJsName %>setButtonState(){

			canRelateNew       = true; //always true
			canRelateExisting  = true;
            var entries        = numberOfRows<%= relationJsName%>();
			if(<%=!InodeUtils.isSet(contentletInode)%>){// for both parent/child
				canRelateNew = false;
			}
			else if (<%=thereCanBeOnlyOne%> && entries > 0){
				canRelateNew = false;
			}
			else{
				canRelateNew = true;
			}
					

			if (<%=thereCanBeOnlyOne%> && entries > 0){
				canRelateExisting = false;
			}
			else{
				canRelateExisting = true;
			}

			var canUserWriteToContentlet = <%= canUserWriteToContentlet %>;

			if(!canUserWriteToContentlet){
				canRelateExisting = false;
				canRelateNew = false;
			}

			addRelateButtons<%= relationJsName %>(canRelateExisting,canRelateNew);
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
	        if(!canRelateExisting){
	        	button.attr('disabled','disabled');
	        }
	        dojo.byId("<%= relationJsName %>relateMenu").appendChild(button.domNode);
	    }

		function add<%= relationJsName %>Loading() {
			// Create row
			const row = document.createElement("tr");
			row.setAttribute("id", 'relationship-loading<%=relationJsName%>');
			// Create column
			const col = document.createElement("td");
			col.setAttribute("style", "text-align:center");
			col.setAttribute("colspan", "1000");

			// Append loading to the table
			col.innerHTML = '<div class="loader-spinner"></div>';
			row.appendChild(col);
			document.getElementById('<%= relationJsName %>Table').appendChild(row);
		}

        dojo.addOnLoad(
         function(){
			var doRelateContainer = document.getElementById('doRelateContainer');
			doRelateContainer.style.display = '<%= relationship.getCardinality() == 2 ? "none" : "block"%>';
			// Load initial relationships
			ContentletAjax.getContentletsData ('<%=String.join(",", listOfRelatedInodes)%>', <%= relationJsName %>_addRelationshipCallback);
        });
	</script>
	

	<div jsId="contentSelector" id="<%= relationJsName %>Dialog" dojoType="dotcms.dijit.form.ContentSelector"
	     structureInode="<%= targetStructure.getInode() %>"
	     relationJsName="<%= relationJsName %>"
		 multiple="<%= relationship.getCardinality() != 2%>"
         useRelateContentOnSelect="true"
		 selectButtonLabel='<%= LanguageUtil.get(pageContext, "Relate")%>'
	     title="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "search")) %>"
	     counter_radio="<%= randomNumber %>"
	     searchCounter="<%= randomNumber %>"
	     contentletLanguageId="<%=contentlet.getLanguageId() %>"
	     dialogCounter="<%= randomNumber %>">
	 </div>
