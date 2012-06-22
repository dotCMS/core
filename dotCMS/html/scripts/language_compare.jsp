<%-- 
To use pass in the  path to languages
ie.. http://localhost:8080/html/scripts/language_compare.jsp?path=/Users/jasontesser/dev/git/dotcms/dotCMS/WEB-INF/messages/
--%>
<%@page import="java.io.InputStream"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="java.io.StringReader"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="org.apache.commons.io.FileUtils"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Iterator"%>
<%@page import="org.apache.commons.configuration.PropertiesConfiguration"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.io.File"%>
<%
String path = request.getParameter("path");
if(path.endsWith("/")){
	path = path.substring(0, path.length()-1);
}
String langToDeleteRemovedKeys = request.getParameter("lang_to_delete");
String findUnusedKeys = request.getParameter("find_unused");

File dir = new File(path);
List<String> currentKeys = new ArrayList<String>();
List<String> files = new ArrayList<String>();
Map<String,PropertiesConfiguration> pcs = new HashMap<String,PropertiesConfiguration>();
Map<String,List<String>> missingKeys = new HashMap<String,List<String>>();
Map<String,List<String>> removedKeys = new HashMap<String,List<String>>();
List<String> unusedKeys = new ArrayList<String>();


PropertiesConfiguration pc = new PropertiesConfiguration();
pc.load(new File(path + File.separator + "Language.properties"));
Iterator i1 = pc.getKeys();
while(i1.hasNext()){
	String k = i1.next().toString().trim();
	currentKeys.add(k);
	if(UtilMethods.isSet(findUnusedKeys) && findUnusedKeys.equalsIgnoreCase("true")){
		String x= Config.CONTEXT_PATH;
		String cmd = "find " + x  + "html -name *.jsp | xargs grep -l " + k + "";
		System.out.println("The CMD is " + cmd);
		Process p = Runtime.getRuntime().exec(cmd);
		ProcessBuilder pb = new ProcessBuilder();
		//pb.
		InputStream is = p.getInputStream();  
		int c;
	    while ((c = is.read()) != -1) {
	    	System.out.println("Line Out is " + (char)c);
	    }
	    is.close();  
	}
}

for(File f : dir.listFiles()){
	if(f.getName().contains("-ext")){
		continue;
	}
	if(f.getName().equals("Language.properties")){
		continue;
	}
	if(f.isDirectory()){
		continue;
	}
	
	files.add(f.getName());
	
	List<String> keys = new ArrayList<String>();
	List<String> aKeys = new ArrayList<String>();
	List<String> rKeys = new ArrayList<String>();
	PropertiesConfiguration pc1 = new PropertiesConfiguration();
	pc1.load(f);
	Iterator i2 = pc1.getKeys();
	while(i2.hasNext()){
		String k = i2.next().toString().trim();
		keys.add(k);
		if(!currentKeys.contains(k)){
			if(UtilMethods.isSet(langToDeleteRemovedKeys) && (langToDeleteRemovedKeys.equals(f.getName()) || langToDeleteRemovedKeys.equals("all"))){
					String s = FileUtils.readFileToString(f, "UTF-8");
					FileUtils.writeStringToFile(f,Pattern.compile("^" + k + ".*\\=.*$", Pattern.MULTILINE).matcher(s).replaceAll(""), "UTF-8");
					if(!langToDeleteRemovedKeys.equals("all")){
						response.sendRedirect("/html/scripts/language_compare.jsp?path=" + path);
					}
			}else{
				rKeys.add(k);
			}
		}
	}
	for(String ck : currentKeys){
		if(!keys.contains(ck)){
			aKeys.add(ck);
		}
	}
	missingKeys.put(f.getName(), aKeys);
	removedKeys.put(f.getName(), rKeys);
	pcs.put(f.getName(), pc1);
}
if(UtilMethods.isSet(langToDeleteRemovedKeys) && langToDeleteRemovedKeys.equals("all")){
	response.sendRedirect("/html/scripts/language_compare.jsp?path=" + path);
}
%>
<html>
	<head>
		<title>dotCMS Language File Release Helper</title>
	</head>
	<body>
		<h1><a href="/html/scripts/language_compare.jsp?path=<%= path %>&lang_to_delete=all">Remove All Old Keys</a></h1>
		<h1><a href="/html/scripts/language_compare.jsp?path=<%= path %>&find_unused=true">Find Unused Keys</a></h1>
		<% for(String language :files){ %>
			<h1><%= language %></h1>
			<div>
				<h2><%= removedKeys.get(language).size() %> Removed Keys <a href="/html/scripts/language_compare.jsp?path=<%= path %>&lang_to_delete=<%= language %>">Remove Keys</a></h2>
				<table border="1">
					<tr><td>Key</td><td>Value</td></tr>
						<% for(String rk : removedKeys.get(language)){ %>
							<tr>
								<td><%= rk %></td><td><%= pcs.get(language).getString(rk) %></td>
							</tr>
						<% } %>
				</table>
			</div>
			<div>
				<h2><%= missingKeys.get(language).size() %> Missing Keys</h2>
				<table border="1">
					<tr><td>Key</td><td>Value</td></tr>
						<% for(String mk : missingKeys.get(language)){ %>
							<tr>
								<td><%= mk %></td><td><%= UtilMethods.escapeHTMLSpecialChars(pc.getString(mk)) %></td>
							</tr>
						<% } %>
					</table>
			</div>
		<% } %>
	</body>
</html>