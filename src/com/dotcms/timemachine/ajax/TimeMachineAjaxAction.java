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
    
    private static final DateFormat fmtId=new SimpleDateFormat("yyyyMMdd");
    private static final DateFormat fmtPretty=new SimpleDateFormat("yyyy-MM-dd");
    private static final ObjectWriter jsonWritter=new ObjectMapper().writerWithDefaultPrettyPrinter();
    
    public void getAvailableTimeMachineForSite(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> map = getURIParams();
        Host host=null;
        
        String hostname=map.get("hostname");
        String hostid=map.get("hostid");
        if(UtilMethods.isSet(hostname))
            host=APILocator.getHostAPI().findByName(hostname, getUser(), false);
        else if(UtilMethods.isSet(hostid))
            host=APILocator.getHostAPI().find(hostid, getUser(), false);
        else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }   
        
        List<Date> dates=APILocator.getTimeMachineAPI().getAvailableTimeMachineForSite(host);
        
        List<Map<String,String>> list=new ArrayList<Map<String,String>>(dates.size()); 
        for(Date dd : dates) {
            Map<String,String> m=new HashMap<String,String>();
            m.put("id", fmtId.format(dd));
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

    @Override
    public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
