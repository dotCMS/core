<%@ include file="/html/scripts/auth.jsp" %><%
Properties prop = new Properties();       

/**

Build a text file of lines that are using the keys


rm /tmp/keys
grep -rh "<bean:message" * | grep -v "Binary file"  >> /tmp/keys
grep -rh "LanguageUtil.get(" *  | grep -v "Binary file"  >> /tmp/keys
grep -rh "LanguageUtil.format(" *  | grep -v "Binary file"  >> /tmp/keys
grep -rh "SessionMessages.add(" * | grep -v "Binary file" >> /tmp/keys 
grep -rh "new ActionMessage(" * | grep -v "Binary file" >> /tmp/keys 
grep -rh "\$text.get" * | grep -v "Binary file" >> /tmp/keys 
grep -rh "\$languagewebapi" * | grep -v "Binary file" >> /tmp/keys 

grep -rh "<bean:message" ../enterprise-2.x/* | grep -v "Binary file"  >> /tmp/keys
grep -rh "LanguageUtil.get(" ../enterprise-2.x/*  | grep -v "Binary file"  >> /tmp/keys
grep -rh "LanguageUtil.format(" ../enterprise-2.x/*  | grep -v "Binary file"  >> /tmp/keys
grep -rh "SessionMessages.add(" ../enterprise-2.x/* | grep -v "Binary file" >> /tmp/keys 
grep -rh "new ActionMessage(" ../enterprise-2.x/* | grep -v "Binary file" >> /tmp/keys 
grep -rh "\$text.get" ../enterprise-2.x/* | grep -v "Binary file" >> /tmp/keys 
grep -rh "\$languagewebapi" ../enterprise-2.x/* | grep -v "Binary file" >> /tmp/keys 



**/




FileChannel fc = new FileInputStream("/tmp/keys").getChannel();
MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
String myFile = Charset.defaultCharset().decode(bb).toString();


File oldfiles = new File("/Users/will/git/dotCMS/dotCMS/WEB-INF/messages");
File newFiles = new File("/Users/will/git/dotCMS/dotCMS/WEB-INF/messages/new");
newFiles.mkdir();

OutputStream	fot;
InputStream    	fis;
BufferedReader 	br;
BufferedWriter 	bo;
String         	line;
File 			newFile;


for(File x : oldfiles.listFiles() ){
	if(x.isDirectory())continue;
	if(x.getName().startsWith("."))continue;
	if(x.getName().contains("-ext"))continue;
	
	fis = new FileInputStream(x);
	br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
	
	newFile = new File(newFiles.getAbsolutePath() + "/" + x.getName());
	fot = new FileOutputStream(newFile);
	bo = new BufferedWriter(new OutputStreamWriter(fot, Charset.forName("UTF-8")));
	
	response.setContentType("text/plain");
	out.println("file:\t" + x);
	
	boolean lastLineBreak = false;
	Set<String> keys = new HashSet<String>();
	int dupes = 0;
	int written = 0;
	while ((line = br.readLine()) != null) {
		
		if(line.startsWith("#")){
			bo.write(line + "\n");
			lastLineBreak=false;
			continue;
		}
		
		if(line==null || line.indexOf("=")<0){
			if(!lastLineBreak){
				lastLineBreak=true;
				bo.write("\n");
			}
			continue;
		}

		String key = line.split("=")[0];
		key=key.trim();
		if(keys.contains(key)){
			dupes++;
			continue;	
		}
		keys.add(key);
		
		
		if(key.startsWith("javax.portlet.title") 
				|| key.startsWith("message.") 
				|| key.startsWith("errors.") 
				|| key.startsWith("prompt.") 
				|| key.startsWith("com.dotmarketing.portlets.workflows.actionlet.") 
				|| key.startsWith("publisher_status_")
				||  myFile.contains(key )  ){
			lastLineBreak=false;
			written++;
			bo.write(line + "\n");
		}
	}
	out.println("wrote:\t" + written );
	out.println("dupes:\t" + dupes );
	out.println("" );
	bo.close();
	br.close();
	
}



%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.Map"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.io.OutputStreamWriter"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.io.BufferedWriter"%>
<%@page import="java.io.OutputStream"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.util.Set"%>
<%@page import="java.nio.charset.Charset"%>
<%@page import="java.nio.MappedByteBuffer"%>
<%@page import="java.nio.channels.FileChannel"%>
<%@page import="java.io.StringWriter"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.io.InputStream"%>
<%@page import="java.util.Properties"%>
