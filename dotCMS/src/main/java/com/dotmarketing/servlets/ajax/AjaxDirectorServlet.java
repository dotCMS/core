package com.dotmarketing.servlets.ajax;

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

    public void init(ServletConfig config) throws ServletException {

    }

    protected void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

        final HttpServletRequest request = loadJson(req);
        
        
        try {
            String clazz = request.getRequestURI().split("/")[2];

            AjaxAction aj = (AjaxAction) Class.forName(clazz).newInstance();
            if (!(aj instanceof AjaxAction)) {
                throw new ServletException("Class must implement AjaxServletInterface");
            }

            aj.init(request, response);
            if ("POST".equals(request.getMethod())) {
                aj.doPost(request, response);
            } else if ("GET".equals(request.getMethod())) {
                aj.doGet(request, response);
            } else if ("PUT".equals(request.getMethod())) {
                aj.doPut(request, response);
            } else if ("DELETE".equals(request.getMethod())) {
                aj.doDelete(request, response);
            } else {
                aj.service(request, response);
            }
            return;
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            response.sendError(500, e.getMessage());

        }

    }

    private HttpServletRequest loadJson(HttpServletRequest req) throws IOException {

        String jsonStr = null;
        try (BufferedReader reader = req.getReader()) {
            jsonStr = CharStreams.toString(reader);
        }
        if(!UtilMethods.isSet(jsonStr)) {
            return req;
        }

        try {
            JSONObject json = new JSONObject(jsonStr);
            Map<String, String[]> map = new HashMap<>();
            for (Iterator<String> i = json.keys(); i.hasNext();) {
                String key = i.next();
                if (json.optJSONArray(key) != null) {
                    List<String> strArray = new ArrayList<>();
                    JSONArray arr = json.optJSONArray(key);
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
