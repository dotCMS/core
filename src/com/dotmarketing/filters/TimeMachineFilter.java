package com.dotmarketing.filters;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;

public class TimeMachineFilter implements Filter {

    ServletContext ctx;

    public static final String TM_DATE_VAR="tm_date";
    public static final String TM_LANG_VAR="tm_lang";
    public static final String TM_HOST_VAR="tm_host";
    
    
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		String uri=req.getRequestURI();

		if(uri != null && uri.startsWith("/admin") && req.getSession().getAttribute("tm_date")!=null){
			req.getSession().removeAttribute(TM_DATE_VAR);
			req.getSession().removeAttribute(TM_LANG_VAR);
			req.getSession().removeAttribute(TM_HOST_VAR);
		}
		if(req.getSession().getAttribute("tm_date")!=null && !CMSFilter.excludeURI(uri)) {
		    String datestr=(String)req.getSession().getAttribute("tm_date");

		    Date date;
		    try {
		        date=new Date(Long.parseLong(datestr));
		    }
		    catch(Exception ex) {
		        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		        return;
		    }
		    
		    // future date. Lets handle in other places
		    if(date.after(new Date())) {
		        chain.doFilter(request, response);
		        return;
		    }

		    Host host=(Host) req.getSession().getAttribute("tm_host");
		    String langid=(String) req.getSession().getAttribute("tm_lang");

		    if(uri.equals("/"))
		        uri="/home/index."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
		    if(uri.endsWith("/"))
		        uri+="index."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");

		    java.io.File file=new java.io.File(ConfigUtils.getBundlePath()+java.io.File.separator+
		            "tm_"+date.getTime()+java.io.File.separator+
		            "live"+java.io.File.separator+
		            host.getHostname()+java.io.File.separator+langid+
		            uri);
		    if(file.isDirectory()) {
		        file=new java.io.File(file,"index."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));
		    }
		    if(file.exists()) {
		        resp.setContentType(ctx.getMimeType(uri));
		        resp.setContentLength((int)file.length());
		        FileInputStream fis=new FileInputStream(file);
		        IOUtils.copy(fis, resp.getOutputStream());
		        fis.close();
		    }
		    else {
		        resp.sendError(400);
		    }
		}
		else {
		    chain.doFilter(request, response);
		}
	}

    @Override
    public void destroy() {}

    @Override
    public void init(FilterConfig fc) throws ServletException {
        ctx=fc.getServletContext();
    }
}
