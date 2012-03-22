<%@page import="com.dotmarketing.util.Constants"%>
<%@ page contentType="text/javascript;charset=UTF-8" language="java" session="false" %>
<%@page import="java.util.Locale"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.TimeZone"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.GregorianCalendar"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="java.io.BufferedInputStream"%>
<%@page import="java.util.LinkedHashSet"%>
<%@page import="java.io.DataInputStream"%>
<%@page import="java.io.FileInputStream"%>
<%@page import="java.io.File"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Set"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%

	response.setContentType("text/css");
	String dojoPath = Config.getStringProperty("path.to.dojo");
	if(!UtilMethods.isSet(dojoPath)){
		// Change dojopath in dotmarketing-config.properties!
		response.sendError(500, "No dojo path variable (path.to.dojo) set in the property file");
	}
	Set<String> files = new LinkedHashSet<String>();

	/**
	* this will concat all the files added below into 1 big .js file
	* which will help both in inital loading and
	* in caching, and in subsequent isUpdated 304 type requests for the files.
	**/
	

	/** static css **/
	files.add("/html/css/reset-fonts-grids.css");
	files.add("/html/css/base.css");
	files.add("/html/css/dot_admin.css");
	files.add("/html/js/calendar/calendar-blue.css");

	/** 
	* Looks for plugins backend css to add
	**/
	String pluginsCSSPath = Config.getStringProperty("BACKEND_PLUGINS_CSS_PATHS");
	if(UtilMethods.isSet(pluginsCSSPath)){
		if(pluginsCSSPath.indexOf(",") != -1){
			for(String pluginCssPath : pluginsCSSPath.split(",")){
				files.add(pluginCssPath.trim());
			}
		}else{
			files.add(pluginsCSSPath.trim());
		}
	}
	
	StringBuilder buff = new StringBuilder();
	for(String x : files){
		buff.append(x);
	}
	Calendar _lastModified = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
	_lastModified.set(Calendar.DAY_OF_YEAR, 0);
	_lastModified.set(Calendar.MINUTE, 0);
	_lastModified.set(Calendar.SECOND, 0);
	_lastModified.set(Calendar.MILLISECOND, 0);
	_lastModified.set(Calendar.HOUR, 0);
	_lastModified.set(Calendar.YEAR, 2010);
	_lastModified.add(Calendar.DAY_OF_YEAR, -(buff.length() + files.size()));

	Date _lastModifiedDate = _lastModified.getTime();
	
	String eTag = "eTag" + buff.toString().hashCode();

	SimpleDateFormat httpDate = new SimpleDateFormat(Constants.RFC2822_FORMAT, Locale.US);
	httpDate.setTimeZone(TimeZone.getTimeZone("GMT"));
	int _daysCache = 365;
	GregorianCalendar expiration = new GregorianCalendar();
	expiration.add(java.util.Calendar.DAY_OF_MONTH, _daysCache);
	int seconds = (_daysCache * 24 * 60 * 60);

	response.setHeader("Expires", httpDate.format(expiration.getTime()));
	response.setHeader("Cache-Control", "public, max-age=" + seconds);
    response.setHeader("ETag", eTag);
    response.setHeader("Last-Modified", httpDate.format(_lastModifiedDate));

    String ifModifiedSince = request.getHeader("If-Modified-Since");
	String ifNoneMatch = request.getHeader("If-None-Match");

	
    if(ifNoneMatch != null){
        if(eTag.equals(ifNoneMatch) || ifNoneMatch.equals("*")){
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
            return;
        }
    }
    /* Using the If-Modified-Since Header */
    if(ifModifiedSince != null){
	    try{
	    	
	        Date ifModifiedSinceDate = httpDate.parse(ifModifiedSince);
	        
	        if(_lastModifiedDate.getTime() <= ifModifiedSinceDate.getTime()){

	            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
	            return;
	        }
	    }
	    catch(Exception e){}
	}

	for(String x : files){
		File f = new File(Config.CONTEXT.getRealPath(x));
		if (f.exists()) {
			FileInputStream fis = new FileInputStream(f);
			BufferedInputStream bis = new BufferedInputStream(fis);
			DataInputStream dis = new DataInputStream(bis);

			while (dis.available() != 0) {

				out.println(dis.readLine());

			}

			fis.close();
			bis.close();
			dis.close();

		} else {
			Logger.fatal(this.getClass(), "Cannot find " + f.getAbsolutePath());
			response.sendError(500, "id10t ERROR " + f.getAbsolutePath() + " not found");
			return;
		}

	}

	
	
%>