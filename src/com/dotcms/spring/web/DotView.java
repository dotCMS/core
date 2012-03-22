package com.dotcms.spring.web;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.springframework.web.servlet.View;

import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.velocity.VelocityServlet;

public class DotView implements View {

	String pagePath;
	
	
	public DotView(String pagePath) {
		super();
		this.pagePath = pagePath;
	}




	public String getContentType() {
		return null;
	}

	public void render(Map<String, ?> map, HttpServletRequest request, HttpServletResponse response) throws Exception {
		

		// get the VelocityContext
		VelocityContext ctx = VelocityUtil.getWebContext(request, response);

		
		// add the Spring map to the context
		for(String x : map.keySet()){
			ctx.put(x, map.get(x));
		}
		
		// add the context to the request.attr
		// where it will be picked up and used by the VelocityServlet
		request.setAttribute(VelocityServlet.VELOCITY_CONTEXT, ctx);
		
		
		
		request.getRequestDispatcher(pagePath).forward(request, response);
		
		
		
		

	}

}
