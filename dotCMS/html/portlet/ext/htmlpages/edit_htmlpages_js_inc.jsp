<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<% String view = request.getParameter("view");%>
<% String content = request.getParameter("content");%>
<% String popup = request.getParameter("popup"); %>
<% String thumbs = request.getParameter("thumbs"); %>

	dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
	dojo.require("dotcms.dojo.data.TemplateReadStore");
	var view = <%=(view!=null)?view:"null"%>;
        var content = <%=(content!=null)?content:"null"%>;
        var popup = <%=(popup!=null)?popup:"null"%>;
        var thumbs = <%=(thumbs!=null)?thumbs:"null"%>;
        var referer = '<%=(referer!=null)?referer:"null" %>';	
	
	function getTemplateCallBack(data){
	
		var imageInode = data.identifier;
		var imageExtension = data.extension;
		
		if (isInodeSet(imageInode)) {
			document.getElementById("templateImage").src = "/thumbnail?id=" + imageInode + "&w=250&h=250";
		}
		else {
			document.getElementById("templateImage").src  = "/html/images/shim.gif";
		}
 
	}
	
	function showTemplate(){
		var ele = dijit.byId("template").attr('value');
		
		if(ele =="0"){
			var pickerStore=window.top._dotTemplateStore;
			pickerStore.hostId="";
			dijit.byId("template").attr('value', '');
			dijit.byId("template").filter();
		}
		else if(ele){
		  TemplateAjax.fetchTemplateImage(ele, dojo.hitch(getTemplateCallBack));	
		}		
	}

	var myForm = document.getElementById('fm');
	function setDate(id, month, day, year) {
			//myForm = document.getElementById("fm");
			if (id == "calendar_0") {
				myForm.calendar_0_month.selectedIndex = getIndex(myForm.calendar_0_month, month);
				myForm.calendar_0_day.selectedIndex = getIndex(myForm.calendar_0_day, day);
				myForm.calendar_0_year.selectedIndex = getIndex(myForm.calendar_0_year, year);
			}
			else if (id == "calendar_1") {
				myForm.calendar_1_month.selectedIndex = getIndex(myForm.calendar_1_month, month);
				myForm.calendar_1_day.selectedIndex = getIndex(myForm.calendar_1_day, day);
				myForm.calendar_1_year.selectedIndex = getIndex(myForm.calendar_1_year, year);
			}
	}

	var form;
	var subcmd;
	function submitfm(form, subcmd) {

		if (form.parent.value=='') {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.select.Folder")) %>');
			return false;
		}
		if (form.template[0].value=='') {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.select.Template")) %>');
			return false;
		}
		if (form.titleField.value == '' || trimString(form.titleField.value).length == 0) {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.set.Title")) %>');
			form.titleField.focus();
			return false;
		}
			
		if (form.pageUrl.value == '' || trimString(form.pageUrl.value).length == 0) {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.set.URL")) %>');
			form.pageUrl.focus();
			return false;
		}
		
		this.form = form;
		this.subcmd = subcmd;

		var form = this.form;
		var subcmd = this.subcmd;

		
		if (form.admin_l2) {
			for (var i = 0; i < form.admin_l2.length; i++) {
				form.admin_l2.options[i].selected = true;
			}
		}
		var sdMonth = parseFloat(document.getElementById('calendar_0_month').value) + 1;
		var sdDay = document.getElementById('calendar_0_day').value;
		var sdYear = document.getElementById('calendar_0_year').value;

		var edMonth = parseFloat(document.getElementById('calendar_1_month').value) + 1;
		var edDay = document.getElementById('calendar_1_day').value;
		var edYear = document.getElementById('calendar_1_year').value;
		
		form.webStartDate.value = sdMonth + "/" + sdDay + "/" + sdYear;
		form.webEndDate.value = edMonth + "/" + edDay + "/" + edYear;

		form.<portlet:namespace />cmd.value = '<%=Constants.ADD%>';
		form.<portlet:namespace />subcmd.value = subcmd;
		document.getElementById("sortOrder").disabled = false;
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>';
		submitForm(form);
	}
	
	var copyAsset = false;

	function submitParent() {
		if (copyAsset) {
			disableButtons(myForm);
			var parent = document.getElementById("parent").value;
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="copy" /><portlet:param name="inode" value="<%=String.valueOf(htmlpage.getInode())%>" /></portlet:actionURL>&parent=' + parent + '&referer=' + referer;
		}
	}
	function cancelEdit() {
		self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="unlock" /><portlet:param name="inode" value="<%=String.valueOf(htmlpage.getInode())%>" /></portlet:actionURL>&referer=' + escape(referer);
	}
	function previewHTMLPage() {
	    previewwin = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/preview_htmlpage" /><portlet:param name="inode" value="<%=String.valueOf(htmlpage.getInode())%>" /></portlet:actionURL>&view=' + view + '&referer=' + referer + '&content='+content+'&popup='+popup+'&child=true', "previewwin", 'width=1000,height=800,scrollbars=yes');
	}
	
	function popupEditLink(inode) {
	   editlinkwin = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&popup=1&inode=' + inode + '&child=true&page_width=650', "editlinkwin", 'width=700,height=400,resizable=1');
	}
	
	function setLink(inode,link,target,identifier){
		var myForm = document.getElementById('fm');
	    myForm.redirect.value = link;
	}
	
	function beLazy()
	{
		ele = document.getElementById("pageUrl");
		if(ele.value.length ==0 ){
			title = document.getElementById("titleField").value.toLowerCase();
			title = title.replace(/\s/g, "-");
			<%-- The tag in the next line just outputs a $ to get arround a tomcat 5.5 documented issued 
			when compiling jsps. DOTCMS-2116 --%>
			var arg=/[\+\%\&\!\"\'\#\<%= "$" %>\/\\\=\?\¡\¿}\:\;\*\<\>\`\´\|]/g ;
			title = title.replace(arg,"");
			ele.value = title;
		}	
		val = document.getElementById("friendlyNameField").value;
		if(val.length == 0){
			document.getElementById("friendlyNameField").value = document.getElementById("titleField").value;
		}
	}
	
    function deleteVersion(objId){
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.confirm.delete.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=deleteversion&inode=' + objId + '&referer=' + referer;
        }
    }
	function selectVersion(objId) {
        if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.confirm.replace.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=getversionback&inode=' + objId + '&inode_version=' + objId + '&referer=' + referer;
	    }
	}
	function editVersion(objId) {
		window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=edit&inode=' + objId + '&referer=' + referer;
	}
	
	<liferay:include page="/html/js/calendar/calendar_js.jsp" flush="true">
		<liferay:param name="calendar_num" value="2" />
	</liferay:include>
	
	function displayProperties(id) {
		if (id == "properties") {
			//display basic properties
			document.getElementById("properties").style.display = "";
			document.getElementById("advanced").style.display = "none";
			document.getElementById("permissions").style.display = "none";
			document.getElementById("versions").style.display = "none";
			//changing class for the tabs
			document.getElementById("properties_tab").className ="alpha";
			document.getElementById("advanced_tab").className ="beta";
			document.getElementById("permissions_tab").className ="beta";
			document.getElementById("versions_tab").className ="beta";
		} else if (id == "advanced") {
			//display advanced properties
			document.getElementById("properties").style.display = "none";
			document.getElementById("advanced").style.display = "";
			document.getElementById("permissions").style.display = "none";
			document.getElementById("versions").style.display = "none";
			//changing class for the tabs
			document.getElementById("properties_tab").className ="beta";
			document.getElementById("advanced_tab").className ="alpha";
			document.getElementById("permissions_tab").className ="beta";
			document.getElementById("versions_tab").className ="beta";
		} else if (id == "versions") {
			//display advanced properties
			document.getElementById("properties").style.display = "none";
			document.getElementById("advanced").style.display = "none";
			document.getElementById("permissions").style.display = "none";
			document.getElementById("versions").style.display = "";
			//changing class for the tabs
			document.getElementById("properties_tab").className ="beta";
			document.getElementById("advanced_tab").className ="beta";
			document.getElementById("permissions_tab").className ="beta";
			document.getElementById("versions_tab").className ="alpha";
		} else {
			//display permissions
			document.getElementById("properties").style.display = "none";
			document.getElementById("advanced").style.display = "none";
			document.getElementById("permissions").style.display = "";
			document.getElementById("versions").style.display = "none";
			//changing class for the tabs
			document.getElementById("properties_tab").className ="beta";
			document.getElementById("advanced_tab").className ="beta";
			document.getElementById("permissions_tab").className ="alpha";
			document.getElementById("versions_tab").className ="beta";
		}
	}
	function disableOrder(showMenu) {
		if (showMenu.checked) {
			document.getElementById("sortOrder").disabled = false;
		}
		else {
			document.getElementById("sortOrder").disabled = true;
		}
	}

	function selectHTMLPageVersion(parentId,objId,referer) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.confirm.replace.version")) %>')){
			window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>&cmd=getversionback&inode=' + parentId + '&inode_version=' + objId + '&referer=' + referer;
		}
	}

	function submitfmDelete() {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.htmlpage.confirm.delete")) %>'))
		{
			self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /><portlet:param name="cmd" value="full_delete" /><portlet:param name="inode" value="<%=String.valueOf(htmlpage.getInode())%>" /></portlet:actionURL>&referer=' + referer;
		}
	}
	
	function dateSelected(id) {
		var date = dijit.byId(id).attr('value');
		document.getElementById(id + '_month').value = date.getMonth();
		document.getElementById(id + '_day').value = date.getDate();
		document.getElementById(id + '_year').value = date.getFullYear();
	}
	
	function hideEditButtonsRow() {
	
	dojo.style('editHtmlPageButtonRow', { display: 'none' });
	}

	function showEditButtonsRow() {
	if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
	dojo.style('editHtmlPageButtonRow', { display: '' });
	changesMadeToPermissions = false;
	}
	
function showCacheTime(){
	var ttl = dijit.byId("cacheTTL").getValue();
	
	var m = 60 * 60 * 24 * 30;
	var w = 60*60*24*7;
	var d = 60*60*24;
	var h = 60*60;
	var mm = 60;
	var message = "";
	var x = 0;
	while(ttl>0){
		if(x>0){
		message+=", ";
		}
		
		if(ttl>=m){
			x = Math.floor(ttl / m);
			message+= x;
			message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Month") %>" 
				: " <%= LanguageUtil.get(pageContext, "Months") %>";
			ttl = Math.floor(ttl % m);
		}
		else if(ttl >= w){
			x = Math.floor(ttl / w);
			message+= x;
			message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Week") %>" 
				: " <%= LanguageUtil.get(pageContext, "Weeks") %>";
			ttl = Math.floor(ttl % w);
		}
		else if(ttl >= d){
			x = Math.floor(ttl / d);
			message+= x;
			message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Day") %>" 
				: " <%= LanguageUtil.get(pageContext, "Days") %>";
			ttl = Math.floor(ttl % d);
		}
		else if(ttl >= h){
			x = Math.floor(ttl / h);
			message+= x;
			message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Hour") %>" 
				: " <%= LanguageUtil.get(pageContext, "Hours") %>";
			ttl = Math.floor(ttl % h);
		}
		else if(ttl >= mm){
			x = Math.floor(ttl / mm);
			message+= x;
			message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Minute") %>" 
				: " <%= LanguageUtil.get(pageContext, "Minutes") %>";
			ttl = Math.floor(ttl % mm);
		}
		else if(ttl > 0){
			x =ttl;
			message+= x;
			message+=(x==1) ? " <%= LanguageUtil.get(pageContext, "Second") %>" 
				: " <%= LanguageUtil.get(pageContext, "Seconds") %>";
			ttl=0;
				
		}
	}
	
	dojo.byId("showCacheTime").innerHTML = message;
}
dojo.addOnLoad(function(){
	showCacheTime();
});