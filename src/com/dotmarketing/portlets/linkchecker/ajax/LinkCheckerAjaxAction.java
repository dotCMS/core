package com.dotmarketing.portlets.linkchecker.ajax;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import com.dotcms.enterprise.linkchecker.LinkCheckerJob;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

public class LinkCheckerAjaxAction extends AjaxAction {

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String cmd = getURIParams().get("cmd");
        java.lang.reflect.Method meth = null;
        Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
        Object arglist[] = new Object[] { request, response };
        try {
            if (getUser() == null ) {
                response.sendError(401);
                return;
            }
            meth = this.getClass().getMethod(cmd, partypes);
            meth.invoke(this, arglist);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Trying to run method:" + cmd);
            Logger.error(this.getClass(), e.getMessage(), e.getCause());
            throw new RuntimeException(e.getMessage(),e);
        }
    }
    
    @SuppressWarnings("rawtypes")
    public void getBrokenLinks(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String,String> pmap=getURIParams();
        int offset=Integer.parseInt(pmap.get("offset"));
        int pageSize=Integer.parseInt(pmap.get("pageSize"));
        String structureInode = pmap.get("structInode");
        
        Map<String,Object> result=new HashMap<String,Object>();
        List<Map> list=new ArrayList<Map>();
        SimpleDateFormat df=new SimpleDateFormat("MM/dd/yyyy hh:mm a");
        try {
            for(InvalidLink link : APILocator.getLinkCheckerAPI().findAllByStructure(structureInode, offset, pageSize)) {
                Contentlet con = APILocator.getContentletAPI().find(link.getInode(), APILocator.getUserAPI().getSystemUser(), false);
                User modUser=APILocator.getUserAPI().loadUserById(con.getModUser());
                Field field=FieldsCache.getField(link.getField()); 
                Structure st=CacheLocator.getContentTypeCache().getStructureByInode(field.getStructureInode());
                
                Map<String,String> mm=new HashMap<String,String>();
                mm.put("inode", link.getInode());
                mm.put("con_title", con.getTitle());
                if(con.isArchived()) {
                	mm.put("status", "archived");
                }
                else if(con.isLive()) {
                	mm.put("status", "live");
                }
                else if(con.isWorking()) {
                	mm.put("status", "working");
                }
                else {
                	mm.put("status", "");
                }
                mm.put("field", field.getFieldName());
                mm.put("structure", st.getName());
                mm.put("date", df.format(con.getModDate()));
                mm.put("user", modUser.getFullName()+"<"+modUser.getEmailAddress()+">");
                mm.put("url_title", link.getTitle());
                mm.put("url", link.getUrl());
                list.add(mm);
            }
                
            
            result.put("list", list);
            result.put("total", APILocator.getLinkCheckerAPI().findAllByStructureCount(structureInode));
            
            response.setContentType("application/json");
            new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValue(response.getOutputStream(), result);
            
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
    
    public void runCheckNow(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String randomID=UUIDGenerator.generateUuid();
            JobDetail jd = new JobDetail("linkCheckerJob-" + randomID, "dotcms_jobs", LinkCheckerJob.class);
            jd.setJobDataMap(new JobDataMap());
            jd.setDurability(false);
            jd.setVolatility(false);
            jd.setRequestsRecovery(true);
            
            Trigger trigger=new SimpleTrigger("linkCheckerTrigger-"+randomID, "group20", new Date());
            
            Scheduler sched = QuartzUtils.getStandardScheduler();
            sched.scheduleJob(jd, trigger);
        } catch (SchedulerException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void action(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {}
    
}
