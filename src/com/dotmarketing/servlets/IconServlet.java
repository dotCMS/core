package com.dotmarketing.servlets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.FastDateFormat;

import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

public class IconServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
    FastDateFormat df = FastDateFormat.getInstance(Constants.RFC2822_FORMAT, TimeZone.getTimeZone("GMT"), Locale.US);
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String i = request.getParameter("i");
		
		
    	if(i !=null && i.length() > 0 && i.indexOf('.') < 0){
    		i="." + i;
    	}
		
		String icon = com.dotmarketing.util.UtilMethods.getFileExtension(i);



		
		
    	java.text.SimpleDateFormat httpDate = new java.text.SimpleDateFormat(Constants.RFC2822_FORMAT, Locale.US);
    	httpDate.setTimeZone(TimeZone.getDefault());
        //  -------- HTTP HEADER/ MODIFIED SINCE CODE -----------//
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(new Date(0));
        Date _lastModified = c.getTime();
        
		String _eTag = "dot:icon-" + icon + "-" + _lastModified.getTime() ;
        String ifModifiedSince = request.getHeader("If-Modified-Since");
        String ifNoneMatch = request.getHeader("If-None-Match");                
        /*
         * If the etag matches then the file is the same
         *
        */
        
        if(ifNoneMatch != null){
            if(_eTag.equals(ifNoneMatch) || ifNoneMatch.equals("*")){
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
                return;
            }
        }

    	
        /* Using the If-Modified-Since Header */
        if(ifModifiedSince != null){
		    try{

		        Date ifModifiedSinceDate = httpDate.parse(ifModifiedSince);
		        if(_lastModified.getTime() <= ifModifiedSinceDate.getTime()){
		            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
		            return;
		        }
		    }
		    catch(Exception e){}
		}
		

        response.setHeader("Last-Modified", df.format(_lastModified));
        response.setHeader("ETag", "\"" + _eTag +"\"");
		
		
		
		ServletOutputStream out = response.getOutputStream();
		response.setContentType("image/png");
        java.util.GregorianCalendar expiration = new java.util.GregorianCalendar();
        expiration.add(java.util.Calendar.YEAR, 1);

        response.setHeader("Expires",  httpDate.format(expiration.getTime()));
        response.setHeader("Cache-Control", "max-age=" +(60*60*24*30*12));

		
		File f = new File(FileUtil.getRealPath("/html/images/icons/" + icon + ".png"));
		if(!f.exists()){
			f = new File(FileUtil.getRealPath("/html/images/icons/ukn.png"));
		}
        response.setHeader("Content-Length", String.valueOf(f.length()));       
        BufferedInputStream fis = null;
		try {
			
			fis =
				new BufferedInputStream(new FileInputStream(f));
			int n;
			while ((n = fis.available()) > 0) {
				byte[] b = new byte[n];
				int result = fis.read(b);
				if (result == -1)
					break;
				out.write(b);
			} // end while
		}
		
		catch (Exception e) {
			 Logger.error(this.getClass(), "cannot read:" + f.toString());
		}
		finally {
			
			if (fis != null)
				fis.close();
			f=null;
		}
		out.close();

	}
}
