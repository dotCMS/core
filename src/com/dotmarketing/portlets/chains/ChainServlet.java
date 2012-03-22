package com.dotmarketing.portlets.chains;

import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

/**
 * 
 * @author davidtorresv
 *
 */
public class ChainServlet extends HttpServlet {

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String servletPath = request.getServletPath();
		String uri = request.getRequestURI();
		
        try {
        	
        	String chainURI = uri.substring(servletPath.length(), uri.length());
        	if(chainURI.split("/").length < 2 && request.getParameter("chainId") == null && request.getParameter("chainKey") == null) {
        		throw new ServletException("You got to invoke the chains executor with a valid chain key name");
        	}
				
    		String chainKey = "";
    		if(chainURI.split("/").length >= 2)
    			chainKey = chainURI.split("/")[1];	
    		else if(request.getParameter("chainId") != null) 
    			chainKey = request.getParameter("chainId");	
   			else if(request.getParameter("chainKey") != null) 
    			chainKey = request.getParameter("chainKey");	

    		ChainsProcessor.executeChain(chainKey, request);
    		ChainControl result = ChainsProcessor.executeChain(chainKey, request);
    		String url = result.getExecutionResult();

    		request.setAttribute("chains_control", result);
    		request.setAttribute("chains_messages", result.getAllMessages());
    		request.setAttribute("chains_errors", result.getAllErrorMessages());
    		request.setAttribute("chains_messages_map", result.getMessages());
    		request.setAttribute("chains_errors_map", result.getErrorMessages());
    		
    		for (Entry<String, Object> entry : result.getChainProperties().entrySet()) {
    			request.setAttribute(entry.getKey(), entry.getValue());
    		}
    		
            if (url.startsWith("http://") || url.startsWith("https://"))
				response.sendRedirect(url);
			else
				request.getRequestDispatcher(url).forward(request, response);
			return;
                
        } catch (Exception e) {
            Logger.error(this, "Error ocurred during the executing of the chain url: " + uri, e);
            throw new ServletException(e);
        } finally {
        	try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this,e.getMessage(),e);
			}
        }

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3480753977537849772L;
	
	

}
