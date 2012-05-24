package com.dotmarketing.servlets;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class RestfulContentServlet extends HttpServlet implements Servlet {



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	@Override
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		
		
		
		HttpSession session = request.getSession();

		boolean eMode = (session
				.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null);


		boolean render = (request.getParameter("render") !=null);
		String q = request.getParameter("q");
		if(!UtilMethods.isSet(q)){
			
			String convertMe = request.getParameter("convertMe");
			if(UtilMethods.isSet(convertMe)){
				//convertMe = URLDecoder.decode(convertMe, "UTF-8");
				convertMe = URLEncoder.encode(convertMe, "UTF-8");

			}
			else{
				convertMe="";
			}
			
			String url = "/JSONContent/?type=xml&debug=true&q=%2Blive%3Atrue&orderBy=modDate+desc";
			response.setContentType("text/html");
			response.getWriter().println("<html><body><pre>");
			response.getWriter().println("dotCMS JSON/XML Content Engine");
			response.getWriter().println("-------------------------------------------------");
			response.getWriter().println(" Get lists of content as JSON objects or as XML by passing a query via a url." );
			response.getWriter().println(" Copy a query from the content search tool and use it to pull content");
			response.getWriter().println(" This engine will respect permissions and live/working.");
			response.getWriter().println("");
			response.getWriter().println("");
			response.getWriter().println("Test URL: (in debug mode)");
			response.getWriter().print("-------------------------------------------------</pre>");
			response.getWriter().println("&nbsp;&nbsp;<a href='" + url +"' style='font-family:monospace'>" + url +"</a>");
			response.getWriter().println("<pre>");
			response.getWriter().println("");
			response.getWriter().println("");
			response.getWriter().println("URL Parameters:");
			response.getWriter().println("-------------------------------------------------");
			response.getWriter().println(" q \t\t: URLEncoded lucene query, e.g. for query '+live:true' do q=" + URLEncoder.encode("+live:true", "UTF-8") );
			response.getWriter().println(" type \t\t: json or xml (defaults to xml)");
					
			response.getWriter().println(" limit \t\t: number of results, defaults to 10 max of 1000");
			
			response.getWriter().println(" offset \t: start at offset, defaults to 0");
			response.getWriter().println(" orderBy \t: field to orderby, defaults to modDate");
			response.getWriter().println(" debug \t\t: if set, will return JSON as text/plain");
			response.getWriter().println("</pre>");
			response.getWriter().println("&nbsp;<form method=post><textarea name='convertMe' rows=5 cols=40>" + convertMe+"</textarea><br><input type='submit' value='url encode a lucene query'></form>");
			response.getWriter().println("</body></html>");
			
			
			
			return;
		}
		//q = URLDecoder.decode(q, "UTF-8");
		
		if(eMode){
			q = q + " +working:true";
		}else{
			q = q + " +live:true";
		}
		
		int limit = 10;
		try{
			limit = Integer.parseInt(request.getParameter("limit"));
		}
		catch(Exception e){
			
		}
		if(limit > 1000){
			limit =1000;
		}
		int offset = 0;
		try{
			offset = Integer.parseInt(request.getParameter("offset"));
		}
		catch(Exception e){
			
		}

		String orderBy = request.getParameter("orderBy");
		if(!UtilMethods.isSet(orderBy)){
			orderBy="modDate";
	
		}
		
		// get the type
		String type = request.getParameter("type");
		if(!UtilMethods.isSet(type)){
			type="xml";
		}
		type = type.toLowerCase();
		if(!"xml".equals(type) && ! "json".equals(type)){
			type="xml";
		}
		
		
		
		boolean debug = (request.getParameter("debug") !=null);

        User user = null;
        try {
            if (session != null){
                user = (com.liferay.portal.model.User) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
            }
        } catch (Exception nsue) {
            Logger.warn(this, "Exception trying to getUser: " + nsue.getMessage(), nsue);
        }
		

        
        
        
        
        // build our objects


		List<Contentlet> cons = new ArrayList<Contentlet>();
		try {
			cons = APILocator.getContentletAPI().search(q,new Integer(limit),new Integer(offset),orderBy,user,true);
		} catch (DotDataException e1) {
			// TODO Auto-generated catch block
			Logger.error(this.getClass(), e1.getMessage(), e1);
		} catch (DotSecurityException e1) {
			// TODO Auto-generated catch block
			Logger.error(this.getClass(), e1.getMessage(), e1);
		} catch (Exception e1) {
			response.setContentType("text/plain");
			response.getWriter().println(
					"Bad Lucene  Query Parse " + q );
			response.getWriter().println(e1.getMessage()
					);
			return;
		}
		
		/**
		 * when the limit is 0, set the limit to the size of the lucene result
		 * search
		 */
		if(debug){
			response.setContentType("text/plain"); 
		}else if("json".equals(type)){
			response.setContentType("application/json");
		}else if("xml".equals(type)){
			response.setContentType("application/xml");
		}
		response.setCharacterEncoding("UTF-8");
		if("json".equals(type)){
			doJSON(cons, response);
		}
		else{
			doXML(cons, response, render);
		}

	}
	
	private void doXML(List<Contentlet> cons, HttpServletResponse response, boolean render) throws IOException{
		
		XStream xstream = new XStream(new DomDriver()); 
		xstream.alias("content", Map.class);

		response.getWriter().println("<?xml version=\"1.0\" encoding='UTF-8'?>");	
		response.getWriter().println("<contentlets>");	
		for(Contentlet c : cons){
				Map m = c.getMap();
				Structure s = c.getStructure();
				for(Field f : s.getFields()){
					if(f.getFieldType().equals(Field.FieldType.BINARY.toString())){

							m.put(f.getVelocityVarName(), "/contentAsset/raw-data/" +  c.getIdentifier() + "/" + f.getVelocityVarName()	);
						
							m.put(f.getVelocityVarName() + "ContentAsset", c.getIdentifier() + "/" +f.getVelocityVarName()	);
					}
					
				}
		       response.getWriter().println(xstream.toXML(m));	
		}
		response.getWriter().println("</contentlets>");
		response.getWriter().flush();  
		response.getWriter().close();  
	}
		
	private void doJSON(List<Contentlet> cons, HttpServletResponse response) throws IOException{
		
	
		
        // get our JSON Going
        JSONObject json = new JSONObject();  
		JSONArray jsonCons = new JSONArray();  

		for(Contentlet c : cons){
			
			
			try {
				
				jsonCons.put(contentletToJSON(c));
				
			} catch (Exception e) {
				Logger.error(this.getClass(), "unable JSON contentlet " + c.getIdentifier());
				Logger.debug(this.getClass(), "unable to find contentlet", e);
			}
		}


		try {
			json.put("contentlets", jsonCons);
		} catch (JSONException e) {
			Logger.error(this.getClass(), "unable to create JSONObject");
			Logger.debug(this.getClass(), "unable to create JSONObject", e);
		}

		
		 
        response.getWriter().println(json.toString());	
		response.getWriter().flush();  
		response.getWriter().close();  
		
		
	}
	
	
	
	
	
	
	
	private JSONObject contentletToJSON(Contentlet con) throws JSONException{
		JSONObject jo = new JSONObject();
		
		Structure s = con.getStructure();
		Map map = con.getMap();
		for (Iterator it = map.keySet().iterator(); it.hasNext(); ) {  
			String key = (String) it.next();  
			if(Arrays.binarySearch(ignoreFields, key) < 0){
				jo.put(key, map.get(key));
			}

		}  

		for(Field f : s.getFields()){
			if(f.getFieldType().equals(Field.FieldType.BINARY.toString())){

					jo.put(f.getVelocityVarName(), "/contentAsset/raw-data/" +  con.getIdentifier() + "/" + f.getVelocityVarName()	);
				
					jo.put(f.getVelocityVarName() + "ContentAsset", con.getIdentifier() + "/" +f.getVelocityVarName()	);
			}
			
		}
		

		   return jo;
		
		
		
	}
	
	final String[] ignoreFields = {"disabledWYSIWYG", "lowIndexPriority"};
	
	
	
	
	
}
