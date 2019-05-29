package com.dotmarketing.servlets.ajax;

import javax.ws.rs.core.Response;
import com.dotcms.rest.WebResource;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.io.CharStreams;

import io.vavr.control.Try;

/**
 * This class acts like an invoker for classes that extend AjaxAction. It is intended to allow
 * developers a quick, safe and easy way to write AJAX servlets in dotCMS without having to wire
 * web.xml
 * 
 * @author will
 * 
 */
public class AjaxDirectorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private WebResource webResource;

    public AjaxDirectorServlet() {
        this(new WebResource());
    }

    public AjaxDirectorServlet(final WebResource webResource) {
        super();
        this.webResource = webResource;
    }

    public void init(ServletConfig config) throws ServletException {

    }

    protected void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

        final HttpServletRequest request = loadJsonAsParams(req);
        
        
        try {
            String clazz = request.getRequestURI().split("/")[2];

            AjaxAction ajaxAction = (AjaxAction) Class.forName(clazz).getConstructor().newInstance();

            ajaxAction.setWebResource(webResource);

            if (!(ajaxAction instanceof AjaxAction)) {
                throw new ServletException("Class must implement AjaxServletInterface");
            }

            ajaxAction.init(request, response);
            if ("POST".equals(request.getMethod())) {
                ajaxAction.doPost(request, response);
            } else if ("GET".equals(request.getMethod())) {
                ajaxAction.doGet(request, response);
            } else if ("PUT".equals(request.getMethod())) {
                ajaxAction.doPut(request, response);
            } else if ("DELETE".equals(request.getMethod())) {
                ajaxAction.doDelete(request, response);
            } else {
                ajaxAction.service(request, response);
            }
            return;
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            response.sendError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage());

        }

    }

  private HttpServletRequest loadJsonAsParams(HttpServletRequest req) throws IOException {

    if (req.getContentType() != null && req.getContentType().toLowerCase().indexOf("multipart/form-data") > -1) {
      return req;
    }
    String jsonStr = null;
    try (BufferedReader reader = req.getReader()) {
      jsonStr = CharStreams.toString(reader);
    }
    if (!UtilMethods.isSet(jsonStr)) {
      return req;
    }

    try {
      final JSONObject json = new JSONObject(jsonStr);
      final Map<String, String[]> map = new HashMap<>();
      for (Iterator<String> i = json.keys(); i.hasNext();) {
        String key = i.next();
        if (json.optJSONArray(key) != null) {
          final List<String> strArray = new ArrayList<>();
          final JSONArray arr = json.optJSONArray(key);
          for (int j = 0; j < arr.length(); j++) {
            if (UtilMethods.isSet(arr.opt(j))) {
              strArray.add(arr.opt(j).toString());
            }
          }
          map.put(key, strArray.toArray(new String[strArray.size()]));

        } else if (json.opt(key) != null) {
          map.put(key, new String[] {json.opt(key).toString()});
        }

      }

      return new JsonDataRequestWrapper(req, map);
    } catch (JSONException e) {
      // crash and burn
      throw new IOException("Error parsing JSON request string");
    }

  }

}
