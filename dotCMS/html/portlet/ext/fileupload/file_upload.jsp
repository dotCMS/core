<%@ include file="/html/portlet/ext/fileupload/init.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>
<%@ page import="com.dotmarketing.util.*" %>
<% 
String inode = request.getParameter("inode");
String parent = request.getParameter("parent");
String referer = request.getParameter("referer");
%>
<script>
    function doUpload(){
        x = document.getElementById("uploadedFile").value.split("\\");
        var fileName = x[x.length -1];


        while(fileName.indexOf(" ") > -1){
            fileName = fileName.replace(" ", "");
        }
        if(fileName.length ==0){
            alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Please-browse-for-a-file-to-upload")) %>');
            return false;
        }
        
        if(confirm("Upload file " + fileName + "?")){


        	dijit.byId('uploadButton').setAttribute('disabled',true);	
            document.getElementById("tableDiv").style.visibility = "hidden";
            document.getElementById("messageDiv").style.visibility = "";

          		

            form = document.getElementById("fm");
            form.fileName.value=fileName;
            form.action="";
            form.cmd.value="<%= Constants.ADD %>";
            submitForm(form);
        }
    }
    <%if(!com.dotmarketing.util.InodeUtils.isSet(inode)){%>
       window.close();
    <%}%>
</script>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"fileupload-upload\") %>" />
    <body class="Body" onLoad="this.focus()" leftmargin="0" topmargin="0" marginheight="0">
		<table class="adminTable" border="0" cellspacing="1" cellpadding="4" align="center">
			<tr>
				<td>
					 <div id="tableDiv" style="visibility: show; position:relative; z-index: 100">
						<table width="100%" cellspacing="1" cellpadding="0" align="center">
							 <html:form action='/ext/fileupload/upload_file' method="POST"  styleId="fm" enctype="multipart/form-data" onsubmit="return false;">
							 <html:hidden property="inode" />
							 <html:hidden property="parent" />
							 <input type="hidden" name="cmd" value="<%=Constants.ADD%>">
							 <input type="hidden" name="fileName" value="">
							 <html:hidden property="maxsize" />
							 <html:hidden property="maxwidth" />
							 <html:hidden property="maxheight" />
							 <html:hidden property="minheight" />
							 <input name="referer" type="hidden" value="<%= referer %>">
							 <TR>
								 <td><strong><font class="gamma" size="2"><%= LanguageUtil.get(pageContext, "File") %>:</font></strong></td>
								 <TD>
								 <input type="file" style="width:250" name="uploadedFile" id="uploadedFile">
								 <font class="gamma" size="2">
								 <% if (request.getParameter("maxwidth")!=null) { %>
								 	<%= LanguageUtil.get(pageContext, "Width") %>: <%= request.getParameter("maxwidth") %> (<%= LanguageUtil.get(pageContext, "max") %>)<BR>
								 <% } %>								 
								 <% if (request.getParameter("maxheight")!=null) { %>
								 	<%= LanguageUtil.get(pageContext, "File") %>: <%= request.getParameter("maxheight") %> (<%= LanguageUtil.get(pageContext, "max") %>)<BR>
								 <% } %>								 
								 </font>
								 </TD>
							 </TR>
						     <TR>
								 <td valign="top"><font class="gamma" size="2"><strong><%= LanguageUtil.get(pageContext, "Description") %>:</font></strong></td>
								 <TD><html:textarea property="caption" styleClass="textarea" /></textarea></TD>
							 </TR>
						     <TR>
								 <TD colspan=2 align=center>
                                   <button dojoType="dijit.form.Button" onClick="doUpload()" id="uploadButton">Upload Now</button> 
                                 </TD>
							 </TR>
							 </html:form>
						 </TABLE>
					</div>
					<div id="messageDiv" style="visibility: hidden;show; position:relative; z-index: 100">
						<font class="gamma" size="2">
						<%= LanguageUtil.get(pageContext, "File-Uploading") %>  . . .<BR><B>  <%= LanguageUtil.get(pageContext, "Note") %>:</B> <%= LanguageUtil.get(pageContext, "This-window-will-redirect-you-back-when-the-file-has-been-uploaded") %>
						</font>
					</DIV>
				</td>
			</tr>
		</table>
	</body>
</liferay:box>
