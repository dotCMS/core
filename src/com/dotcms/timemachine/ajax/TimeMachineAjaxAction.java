package com.dotcms.timemachine.ajax;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class TimeMachineAjaxAction extends AjaxAction {
    
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, String> map = getURIParams();
        String cmd = map.get("cmd");
        java.lang.reflect.Method meth = null;
        Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
        Object arglist[] = new Object[] { request, response };
        try {
            if (getUser() == null || 
                    !APILocator.getRoleAPI().doesUserHaveRole(getUser(), APILocator.getRoleAPI().loadCMSAdminRole())) {
                response.sendError(401);
                return;
            }
            meth = this.getClass().getMethod(cmd, partypes);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return;
        }
        try {
            meth.invoke(this, arglist);
        } catch (Exception e) {
            Logger.error(this, "Trying to run method:" + cmd);
            Logger.error(this, e.getMessage(), e);
        }
    }
    
    
    private static final DateFormat fmtPretty=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final ObjectWriter jsonWritter=new ObjectMapper().writerWithDefaultPrettyPrinter();
    
    public void getHostsWithTimeMachine(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Host> hosts=APILocator.getTimeMachineAPI().getHostsWithTimeMachine();
        List<Map<String,String>> list=new ArrayList<Map<String,String>>(hosts.size()); 
        for(Host hh : hosts) {
            Map<String,String> m=new HashMap<String,String>();
            m.put("id", hh.getIdentifier());
            m.put("hostname", hh.getHostname());
            list.add(m);
        }
        
        Map<String, Object> m=new HashMap<String,Object>();
        m.put("identifier", "id");
        m.put("label", "hostname");
        m.put("items", list);
        response.setContentType("application/json");
        jsonWritter.writeValue(response.getOutputStream(), m);
    }
    
    public void getAvailableTimeMachineForSite(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> map = getURIParams();
        
        String hostid=map.get("hostid");
        
        if(!validateParams(null, hostid, null)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        Host host=APILocator.getHostAPI().find(hostid, getUser(), false);
        
        List<Date> snaps=APILocator.getTimeMachineAPI().getAvailableTimeMachineForSite(host);
        
        List<Map<String,String>> list=new ArrayList<Map<String,String>>(snaps.size()); 
        for(Date dd : snaps) {
            Map<String,String> m=new HashMap<String,String>();
            m.put("id", Long.toString(dd.getTime()));
            m.put("pretty", fmtPretty.format(dd));
            list.add(m);
        }
        
        Map<String, Object> m=new HashMap<String,Object>();
        m.put("identifier", "id");
        m.put("label", "pretty");
        m.put("items", list);
        response.setContentType("application/json");
        jsonWritter.writeValue(response.getOutputStream(), m);
    }
    
    public void getAvailableLangForTimeMachine(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> map = getURIParams();
        String hostid=map.get("hostid");
        String datestr=map.get("date");
        
        if(!validateParams(datestr, hostid, null)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        Host host=APILocator.getHostAPI().find(hostid, getUser(), false);
        
        List<String> langs=APILocator.getTimeMachineAPI().getAvailableLangForTimeMachine(
                                 host, new Date(Long.parseLong(datestr)));
        
        List<Map<String,String>> list=new ArrayList<Map<String,String>>();
        
        for(String lid : langs) {
            Language lang=APILocator.getLanguageAPI().getLanguage(lid);
            Map<String,String> m=new HashMap<String,String>();
            m.put("id", lid);
            m.put("pretty", lang.getLanguage()+" - "+lang.getCountry());
            list.add(m);
        }
        
        Map<String, Object> m=new HashMap<String,Object>();
        m.put("identifier", "id");
        m.put("label", "pretty");
        m.put("items", list);
        resp.setContentType("application/json");
        jsonWritter.writeValue(resp.getOutputStream(), m);
    }
    
    public void startBrowsing(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> map = getURIParams();
        String datestr=map.get("date");
        String hostid=map.get("hostid");
        String langid=map.get("langid");
        
        if(!validateParams(datestr, hostid, langid))
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        else {
            req.getSession().setAttribute("tm_host",
                    APILocator.getHostAPI().find(hostid, getUser(), false));
            req.getSession().setAttribute("tm_date", datestr);
            req.getSession().setAttribute("tm_lang", langid);
        }
    }
    
    public void stopBrowsing(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        req.getSession().removeAttribute("tm_date");
        req.getSession().removeAttribute("tm_lang");
        req.getSession().removeAttribute("tm_host");
    }
    
    private boolean validateParams(String datestr, String hostid, String langid) {
        try {
            // validating
            if(datestr!=null)
                Long.parseLong(datestr);
            if(hostid!=null) {
                Host hh=APILocator.getHostAPI().find(hostid, getUser(), false);
                if(hh==null || !UtilMethods.isSet(hh.getIdentifier()))
                    throw new Exception();
            }
            if(langid!=null) {
                Language ll=APILocator.getLanguageAPI().getLanguage(langid);
                if(ll==null || !UtilMethods.isSet(ll.getId()))
                    throw new Exception();
            }
        }
        catch(Exception ex) {
            return false;
        }
        return true;
    }
    
    public void saveJobConfig(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String cronExp=req.getParameter("cronExp");
        String[] hostids=req.getParameterValues("snaphost");
        boolean allhost=req.getParameter("allhosts").equals("true");
        String[] langids=req.getParameterValues("lang");
        Map<String, String> map = getURIParams();
        boolean runnow=map.get("run")!=null;
    }

    @Override
    public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
