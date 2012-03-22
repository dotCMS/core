<% response.setContentType("text/xml"); %>
<%@ page import="com.dotmarketing.util.UtilMethods,com.dotmarketing.portlets.files.model.File,com.dotmarketing.util.MP3Utils,java.util.Iterator,java.net.URLEncoder" %>
<database>
<% 
	Iterator songIter = MP3Utils.retrieveSongs(request.getParameter("station")).iterator();
	int i = 0;
	int j = 1;
	while(songIter.hasNext()){
		Object myObject = songIter.next();
		if(myObject!=null){
			File cursong = (File) myObject;
		
%>	<Index mpListNumber="<%=i%>" mylabel="<%=j%>. <%= UtilMethods.xmlifyString(cursong.getTitle()) %>" mydata="<%= UtilMethods.getRelativeAssetPath(cursong) %>" />
<% 
i++;
j++;
} }
%></database>
