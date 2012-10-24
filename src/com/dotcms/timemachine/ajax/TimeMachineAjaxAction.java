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

import com.dotcms.timemachine.business.TimeMachineAPI.SnapshotInfo;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

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
        
        List<SnapshotInfo> snaps=APILocator.getTimeMachineAPI().getAvailableTimeMachineForSite(host);
        
        List<Map<String,String>> list=new ArrayList<Map<String,String>>(snaps.size()); 
        for(SnapshotInfo dd : snaps) {
            Map<String,String> m=new HashMap<String,String>();
            m.put("id", dd.date.getTime()+"."+dd.langid);
            Language lang=APILocator.getLanguageAPI().getLanguage(dd.langid);
            m.put("pretty", fmtPretty.format(dd.date)+" "+lang.getLanguageCode()+"_"+lang.getCountryCode());
            list.add(m);
        }
        
        Map<String, Object> m=new HashMap<String,Object>();
        m.put("identifier", "id");
        m.put("label", "pretty");
        m.put("items", list);
        response.setContentType("application/json");
        jsonWritter.writeValue(response.getOutputStream(), m);
    }
    
    public void startBrowsing(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> map = getURIParams();
        String snap=map.get("snap");
        String hostid=map.get("hostid");
        String[] cc=snap.split("\\.");
        String datestr=cc[0];
        String langid=cc[1];
        
        req.getSession().setAttribute("tm_host",
                APILocator.getHostAPI().find(hostid, getUser(), false));
        req.getSession().setAttribute("tm_date", datestr);
        req.getSession().setAttribute("tm_lang", langid);
    }
    
    public void stopBrowsing(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        req.getSession().removeAttribute("tm_date");
        req.getSession().removeAttribute("tm_lang");
        req.getSession().removeAttribute("tm_host");
    }

    @Override
    public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
