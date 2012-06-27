
<%@page import="com.liferay.util.jna.JNALibrary"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.Reader"%>
<%@page import="java.io.FileReader"%>
<%@page import="java.io.FileWriter"%>
<%@page import="java.io.File"%>
<%
String text="This is a simple text";

File file1=new File("/tmp/a");
File file2=new File("/tmp/b");

FileWriter fw=new FileWriter(file1);
fw.write(text);
fw.close();

JNALibrary.link(file1.getAbsolutePath(), file2.getAbsolutePath());

BufferedReader fr=new BufferedReader(new FileReader(file2));
String text2=fr.readLine();
fr.close();
%>
<html>
<body>
text: <%= text %> <br/>
text2: <%= text2 %> <br/>
equals?: <%= text.equals(text2) %>
</body>
</html>
