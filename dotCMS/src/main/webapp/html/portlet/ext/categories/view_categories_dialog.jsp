<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@include file="/html/portlet/ext/categories/init.jsp"%>

<%
	String dojoPath = Config.getStringProperty("path.to.dojo");
	String counter = (String) request.getAttribute("counter");
%>

<style type="text/css">
@import "<%=dojoPath%>/dojox/grid/enhanced/resources/claro/EnhancedGrid.css?b=<%= ReleaseInfo.getVersion() %>";
@import "<%=dojoPath%>/dojox/grid/resources/tundraGrid.css?b=<%= ReleaseInfo.getVersion() %>";

/*Grid need a explicit width/height by default*/
#addedGrid<%=counter%> { width: 43em; height:
    20em; }

#container<%=counter%> {
	margin: 0px;
	padding: 0px;
	width: 100%;
}

#catHolder<%=counter%> {
	margin: 0px;
	padding: 0px;
	width: 420px;
	background-color: #ECECEC;
	float: left;
}
#addedHolder<%=counter%> {
	margin: 0px;
	padding: 0px;
	width: 180px;
	background-color: #ECECEC;
	float: right;
	height: 100%;
}

</style>
<script type="text/javascript" src="/dwr/interface/CategoryAjax.js"></script>

<script type="text/javascript">
	dojo.require("dojox.grid.enhanced.plugins.Pagination");
	dojo.require('dijit.layout.ContentPane');
	dojo.require("dojo.data.ItemFileWriteStore");
	dojo.require("dojox.data.QueryReadStore");

	var grid<%=counter%>;
	var myStore<%=counter%>;
	var currentInodeOrIdentifier<%=counter%>;  // inode of the category
	var currentCatName<%=counter%>;  // inode of the category
	var lastTabSelected<%=counter%>;
	var addedGrid<%=counter%>;
	var addedStore<%=counter%>;
	var delCount<%=counter%> = 0;
	var inodesArray<%=counter%> = [];
	var deleteStore<%=counter%> = true;
	var baseCat<%=counter%> = null;

	/*  ADDED CATEGORIES GRID FUNCTIONS */

	function createAddedStore<%=counter%>() {
		var data =  { identifier: 'id',
				   label: 'name',
				   items: [
				 ]};

		 addedStore<%=counter%> = new dojo.data.ItemFileWriteStore({
	         data: data
	     });

	}

	function createAddedGrid<%=counter%>() {

		var deleteFormatter = function(value, index) {
			var inode = addedGrid<%=counter%>.store.getValue(addedGrid<%=counter%>.getItem(index), 'id');
			return "<div style='width:100%;height:20px;cursor:pointer;' onclick=\"javascript:delCat<%=counter%>('"+inode+"');\">&nbsp;<span class='deleteIcon'></span>&nbsp" + value  + "</div>";
		}

		var addedlayout = [
		{
			field : 'id',
			name : 'id',
			width: '0px',
			hidden : true
		},

		{
			field : 'name',
			name : '<%= LanguageUtil.get(pageContext, "Added") %>',
			width: 'auto',
			formatter : deleteFormatter
		}
		];

		addedGrid<%=counter%> = new dojox.grid.DataGrid({
			jsId : "addedGrid<%=counter%>",
	        store: addedStore<%=counter%>,
			autoWidth : true,
			initialWidth : '25%',
			escapeHTMLInData : false,
	        structure: addedlayout
	    }, dojo.byId('addedHolder<%=counter%>'));

		dojo.addClass(dojo.byId('addedHolder<%=counter%>'), "blank");

	}

	function addCat<%=counter%>(index) {
		    var inode = grid<%=counter%>.store.getValue(grid<%=counter%>.getItem(index), 'inode');
		    var name = grid<%=counter%>.store.getValue(grid<%=counter%>.getItem(index), 'category_name');
		    addSelectedCat<%=counter%>(inode,name);
	}

	function addSelectedCat<%=counter%>(inode,name) {
		addedStore<%=counter%>.fetch({query: {id:inode}, onComplete:function(items) {
			if(items.length > 0){
				showDotCMSErrorMessage( items[0].name.toString() + " : " + "<%= LanguageUtil.get(pageContext, "category-already-added") %>");
					}else{
						addedStore<%=counter%>.newItem({id : inode, name: name});
				        addedGrid<%=counter%>.render();
				        addPreview<%=counter%>(name, inode);
			}}});
	}

	function delCat<%=counter%>(inode) {
		addedStore<%=counter%>.fetch({query: {id:inode}, onComplete:function(items) {
			 addedStore<%=counter%>.deleteItem(items[0]);
			 addedStore<%=counter%>.save();
			 addedGrid<%=counter%>.render();
			 dojo.destroy(dojo.byId("preview"+inode));
		}});
	}

	function delAll<%=counter%>() {
		addedStore<%=counter%>.fetch({onComplete:function(items) {
			for (var i = 0; i < items.length; i++){
				addedStore<%=counter%>.deleteItem(items[i]);
				 dojo.destroy(dojo.byId("preview"+items[i].id));
			   }
			addedStore<%=counter%>.save();
		}});
	}

	function fixAddedGrid<%=counter%>() {
		addedGrid<%=counter%>.destroy(true);
		createAddedGrid<%=counter%>();
		addedGrid<%=counter%>.startup();
	}

	var onSave = function() {

	};

	/*  CATEGORIES GRID FUNCTIONS */

	var addFormatter<%=counter%> = function(value, index) {
		return "<div style='text-align:center;width:100%;cursor:pointer;' onclick=\"javascript:drillDown<%=counter%>("+index+");\"><span class='toggleOpenIcon'></span></div>";
	};


	/* format the name column of the grid to be an <a> element */
	var formatHref<%=counter%> = function(value, index) {
		if(value == undefined || value==null || value=="null"){
			value="&nbsp;";
		}
		
		return "<div style='width:100%;cursor:pointer;' onclick=\"addCat<%=counter%>("+index+")\" >"+value+"</div>";
	};

	function createStore<%=counter%>(params) {
		if(params==null) params = '';

		myStore<%=counter%> = new dojox.data.QueryReadStore({
			url : '/categoriesServlet'+params
		});
	}

	function createGrid<%=counter%>() {
			var layout = [
			{
				field : '',
				name : ' ',
				width : '34px',
				formatter : addFormatter<%=counter%>
			},
			{
				field : 'category_name',
				name : '<%= LanguageUtil.get(pageContext, "Name") %>',
				width : '50%',
				formatter : formatHref<%=counter%>
			}, {
				field : 'category_key',
				name : '<%= LanguageUtil.get(pageContext, "Key") %>',
				width : '25%',
				formatter : formatHref<%=counter%>
			}, {
				field : 'category_velocity_var_name',
				name : '<%= LanguageUtil.get(pageContext, "Variable") %>',
				width : '25%',
				formatter : formatHref<%=counter%>
			}
			];

			grid<%=counter%> = new dojox.grid.EnhancedGrid({
				rowsPerPage : 10,
				jsId : "grid<%=counter%>",
				store : myStore<%=counter%>,
				autoWidth : true,
				initialWidth : '70%',
				height: "90%",
				autoHeight : true,
				escapeHTMLInData : false,
				structure : layout,
				plugins : {
					pagination : {
						pageSizes : [  ],
						description : "45%",
						sizeSwitch : "260px",
						pageStepper : "30em",
						gotoButton : true,
						maxPageStep : 7,
						position : "bottom"
					}
				}
			}, dojo.byId('catHolder<%=counter%>'));

			dojo.addClass(dojo.byId('catHolder<%=counter%>'), "tundra");
	};

	function initDialog<%=counter%>() {

<%-- 		dijit.byId("catFilter<%=counter%>").focus();  --%>

		createStore<%=counter%>();
		createGrid<%=counter%>();
		grid<%=counter%>.startup();

		createAddedStore<%=counter%>();
		createAddedGrid<%=counter%>();
		addedGrid<%=counter%>.startup();
		doSearch<%=counter%>();

	}

	/* search handling */
	function doSearch<%=counter%>() {

		if(currentInodeOrIdentifier<%=counter%>) {
			var params = dojo.byId("catFilter<%=counter%>").value;
			params = "?inode="+currentInodeOrIdentifier<%=counter%>+"&q="+params;

			grid<%=counter%>.destroy(true);
			createStore<%=counter%>(params);
			createGrid<%=counter%>();
			grid<%=counter%>.startup();
		}

<%-- 		dijit.byId('catFilter<%=counter%>').focus(); --%>


    };

	/* clear the search field */
	function clearCatFilter<%=counter%>() {
		dijit.byId('catFilter<%=counter%>').attr('value', '');
		doSearch<%=counter%>();
	}

	function manageBreadCrumbs<%=counter%>(inode, name) {
		dojo.place(" <a style=\"\" id=\"a_"+inode+"\" href=\"javascript:prepareCrumbs<%=counter%>('"+inode+"', '"+name+"');  \"> &#62; "+name+"</a>", "nav<%=counter%>", "last");
	}

	function prepareCrumbs<%=counter%>(inode, name) {

		currentInodeOrIdentifier<%=counter%>=inode;
		currentCatName<%=counter%> = name;
		var destroy = false;
		dojo.forEach(dojo.query("#nav<%=counter%> >"), function(node, i) {
				if(node.id == "a_"+inode) {
					destroy = true;
					return;
				}
				if(destroy) {
					console.log("destroy: " + node);
					dojo.destroy(node);
				}
			}) ;

		dijit.byId('catFilter<%=counter%>').attr('value', '');
		doSearch<%=counter%>();
	}

	/* drill down of a category, load the children, properties */
	function drillDown<%=counter%>(index) {
		var inode = grid<%=counter%>.store.getValue(grid<%=counter%>.getItem(index), 'inode');
		var name = grid<%=counter%>.store.getValue(grid<%=counter%>.getItem(index), 'category_name');
		var velVar = grid<%=counter%>.store.getValue(grid<%=counter%>.getItem(index), 'category_velocity_var_name');
		var key = grid<%=counter%>.store.getValue(grid<%=counter%>.getItem(index), 'category_key');
		var keywords = grid<%=counter%>.store.getValue(grid<%=counter%>.getItem(index), 'keywords');

		manageBreadCrumbs<%=counter%>(inode, name);
		currentCatName<%=counter%> = name;
		currentInodeOrIdentifier<%=counter%> = inode;
		dijit.byId('catFilter<%=counter%>').attr('value', '');
		doSearch<%=counter%>();
	}

	function alterFocus<%=counter%>(toBlur, toFocus) {

		 	if(dijit.byId("categoriesDialog<%=counter%>").open) {
				toBlur.blur();
				toFocus.focus();
		 	}
	}

	function addPreview<%=counter%>(catName, inode) {
		dojo.place("<li id='preview"+inode+"'  ><a href=\"javascript:delCat<%=counter%>('"+inode+"');\"><img src='/html/images/icons/cross.png' /></a>"+catName+"</li>", "previewCats<%=counter%>", "last");
	}
</script>



<div id="categoriesDialog<%=counter%>" dojoType="dijit.Dialog" style="display:none;max-width:900px;max-height:540px;vertical-align: middle; " draggable="true"
	title="<%= LanguageUtil.get(pageContext, "categories") %>" >
		<div class="categories-selector__breadcrumbs" id="breadCrumbs<%=counter%>">
			<ul id="nav<%=counter%>" style="margin-left:0px;">
				<a id="a_null<%=counter%>" style="" onfocus="return false;"  href="javascript:prepareCrumbs<%=counter%>(baseCat<%=counter%>, '<%= LanguageUtil.get(pageContext, "Top-Level") %>');"  \><%= LanguageUtil.get(pageContext, "Top-Level") %></a>
			</ul>
		</div>
		<div class="categories-selector__toolbar">
			<div class="categories-selector__toolbar-actions-primary">
				<div class="inline-form">
					<input name="catFilter" id="catFilter<%=counter%>" dojoType="dijit.form.TextBox" placeholder="<%= LanguageUtil.get(pageContext, "message.filter.categories") %>" style="width: 265px;" />
					<button dojoType="dijit.form.Button" onclick="doSearch<%=counter%>();" type="button"><%= LanguageUtil.get(pageContext, "Search") %></button>
					<button dojoType="dijit.form.Button" onclick="clearCatFilter<%=counter%>()" type="button" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Clear") %></button>
				</div>
			</div>
			<div class="categories-selector-toolbar-actions-secondary">
				<div id="all<%=counter%>">
					<button dojoType="dijit.form.Button" class="dijitButtonDanger" onclick="delAll<%=counter%>();" type="button"><%= LanguageUtil.get(pageContext, "delete-all") %></button>
				</div>
			</div>
		</div>

		<div id="container<%=counter%>" style="height: 340px; overflow: auto;">

			<div id="scroll<%=counter%>" style="height: 300px; margin-top: 0px">
				<div id="catHolder<%=counter%>" ></div>
				<div id="addedHolder<%=counter%>" ></div>
			</div>
		</div>
</div>