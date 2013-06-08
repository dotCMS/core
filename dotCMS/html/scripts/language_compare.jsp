<%@ include file="/html/scripts/auth.jsp" %><%-- 
To use pass in the  path to languages
ie.. http://localhost:8080/html/scripts/language_compare.jsp?path=/Users/jasontesser/dev/git/dotcms/dotCMS/WEB-INF/messages/
--%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.io.OutputStreamWriter"%>
<%@page import="java.io.Writer"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="java.io.BufferedWriter"%>
<%@page import="java.io.FileWriter"%>
<%@page import="org.apache.commons.cli.CommandLine"%>
<%@page import="java.io.DataInputStream"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="com.dotmarketing.util.Logger"%>
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
	if (path.endsWith("/")) {
		path = path.substring(0, path.length() - 1);
	}
	String langToDeleteRemovedKeys = request
			.getParameter("lang_to_delete");
	String genUnused = request.getParameter("gen_unused");
	
	String removeUnused = request.getParameter("remove_unused");

	File dir = new File(path);
	List<String> currentKeys = new ArrayList<String>();
	List<String> files = new ArrayList<String>();
	Map<String, PropertiesConfiguration> pcs = new HashMap<String, PropertiesConfiguration>();
	Map<String, List<String>> missingKeys = new HashMap<String, List<String>>();
	Map<String, List<String>> removedKeys = new HashMap<String, List<String>>();
	List<String> unusedKeys = new ArrayList<String>();

	PropertiesConfiguration pc = new PropertiesConfiguration();
	pc.load(new File(path + File.separator + "Language.properties"));
	String x = Config.CONTEXT_PATH;
	
	FileWriter fw = null;
	BufferedWriter bw = null;
	
	if (UtilMethods.isSet(genUnused) && genUnused.equalsIgnoreCase("true")) {
		File f = new File(x + "keys.txt");
		if(f.exists()){
			f.delete();
		}
		f = new File(x + "keys.txt");
		f.createNewFile();
		
		File f1 = new File(x + "unsedkeys.txt");
		if(f1.exists()){
			f1.delete();
		}
		f1 = new File(x + "unsedkeys.txt");
		f1.createNewFile();
		
		fw = new FileWriter(f1.getAbsoluteFile());
		bw = new BufferedWriter(fw);
	}
	
	
	Iterator i1 = pc.getKeys();
	while (i1.hasNext()) {
		String k = i1.next().toString().trim();
		currentKeys.add(k);
		if (UtilMethods.isSet(genUnused) && genUnused.equalsIgnoreCase("true")) {

			String[] cmd = {x + "html/scripts/language_compare_helper1.sh" , x , k};

			//Logger.info(this,"The CMD is " + cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			int exitVal = p.waitFor();
			
		}
		//break;
	}
		
	if (UtilMethods.isSet(genUnused) && genUnused.equalsIgnoreCase("true")) {
		try {
			FileInputStream fstream = new FileInputStream(x + "keys.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			String currentKey = "";
			boolean currentKeyUsed = false;
			while ((strLine = br.readLine()) != null) {
				if(currentKey.equals("")){
					currentKey = strLine.replaceAll("---", "");
					continue;
				}else if(!strLine.startsWith("---")){
					currentKeyUsed = true;
					continue;
				}else{
					if(!currentKeyUsed){
						bw.write(currentKey + System.getProperty("line.separator"));
					}
					currentKey = strLine.replaceAll("---", "");
					currentKeyUsed = false;
				}
			}
			in.close();
		} catch (Exception e) {//Catch exception if any
			Logger.error(this,"Error: " + e.getMessage(),e);
		}
	
		if(bw!=null){
			bw.flush();
			bw.close();
		}
	}
	
	
	if (UtilMethods.isSet(removeUnused) && removeUnused.equalsIgnoreCase("true")) {
		List<String> keys = new ArrayList<String>();
		File f1 = new File(x + "unsedkeys.txt");
		FileInputStream fs = new FileInputStream(f1);
		DataInputStream i = new DataInputStream(fs);
		BufferedReader b = new BufferedReader(new InputStreamReader(i));
		String sline = "";
		while ((sline = b.readLine()) != null) {
			keys.add(sline.trim());
		}
		
		for (File f : dir.listFiles()) {
			if (f.getName().contains("-ext")) {
				continue;
			}
			if (f.isDirectory()) {
				continue;
			}
			File t = new File(dir.getAbsolutePath() + File.separator + "temp_" + f.getName());
			t.createNewFile();
			Writer o = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(t), "UTF-8"));
			
			FileInputStream fstream = new FileInputStream(f);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine = "";
			while ((strLine = br.readLine()) != null) {
				String k = strLine.split("=")[0].trim();
				if(k.startsWith("javax.portlet") || !keys.contains(k)){
					o.write(strLine + System.getProperty("line.separator"));
				}
			}
			
			t.renameTo(f);
		}
	}
	
	for (File f : dir.listFiles()) {
		if (f.getName().contains("-ext")) {
			continue;
		}
		if (f.getName().equals("Language.properties")) {
			continue;
		}
		if (f.isDirectory()) {
			continue;
		}
		
		files.add(f.getName());

		List<String> keys = new ArrayList<String>();
		List<String> aKeys = new ArrayList<String>();
		List<String> rKeys = new ArrayList<String>();
		PropertiesConfiguration pc1 = new PropertiesConfiguration();
		pc1.load(f);
		Iterator i2 = pc1.getKeys();
		while (i2.hasNext()) {
			String k = i2.next().toString().trim();
			keys.add(k);
			if (!currentKeys.contains(k)) {
				if (UtilMethods.isSet(langToDeleteRemovedKeys)
						&& (langToDeleteRemovedKeys.equals(f.getName()) || langToDeleteRemovedKeys
								.equals("all"))) {
					String s = FileUtils.readFileToString(f, "UTF-8");
					FileUtils.writeStringToFile(
							f,
							Pattern.compile("^" + k + ".*\\=.*$",
									Pattern.MULTILINE).matcher(s)
									.replaceAll(""), "UTF-8");
					if (!langToDeleteRemovedKeys.equals("all")) {
						response.sendRedirect("/html/scripts/language_compare.jsp?path="
								+ path);
					}
				} else {
					rKeys.add(k);
				}
			}
		}
		for (String ck : currentKeys) {
			if (!keys.contains(ck)) {
				aKeys.add(ck);
			}
		}
		missingKeys.put(f.getName(), aKeys);
		removedKeys.put(f.getName(), rKeys);
		pcs.put(f.getName(), pc1);
	}
	if (UtilMethods.isSet(langToDeleteRemovedKeys)
			&& langToDeleteRemovedKeys.equals("all")) {
		response.sendRedirect("/html/scripts/language_compare.jsp?path="
				+ path);
	}
	
	
%>
<html>
	<head>
		<title>dotCMS Language File Release Helper</title>
	</head>
	<body>
		<h1><a href="/html/scripts/language_compare.jsp?path=<%=path%>&lang_to_delete=all">Remove All Old Keys</a></h1>
		<h1><a href="/html/scripts/language_compare.jsp?path=<%=path%>&gen_unused=true">Generate Unused Keys</a></h1>
		<h1><a href="/html/scripts/language_compare.jsp?path=<%=path%>&show_unsed=true&remove_unused=true">Remove Unused Keys</a></h1>
		<%
			for (String language : files) {
		%>
			<h1><%=language%></h1>
			<div>
				<h2><%=removedKeys.get(language).size()%> Removed Keys <a href="/html/scripts/language_compare.jsp?path=<%= path %>&lang_to_delete=<%=language%>">Remove Keys</a></h2>
				<table border="1">
					<tr><td>Key</td><td>Value</td></tr>
						<%
							for (String rk : removedKeys.get(language)) {
						%>
							<tr>
								<td><%=rk%></td><td><%=pcs.get(language).getString(rk)%></td>
							</tr>
						<%
							}
						%>
				</table>
			</div>
			<div>
				<h2><%=missingKeys.get(language).size()%> Missing Keys</h2>
				<table border="1">
					<tr><td>Key</td><td>Value</td></tr>
						<%
							for (String mk : missingKeys.get(language)) {
						%>
							<tr>
								<td><%=mk%></td><td><%=UtilMethods.escapeHTMLSpecialChars(pc
							.getString(mk))%></td>
							</tr>
						<%
							}
						%>
					</table>
			</div>
		<%
			}
		%>
	</body>
</html>