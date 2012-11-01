package com.dotmarketing.portlets.linkchecker.ajax;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
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
        
        Map<String,Object> result=new HashMap<String,Object>();
        List<Map> list=new ArrayList<Map>();
        SimpleDateFormat df=new SimpleDateFormat("yyyyMMdd hh:mm");
        try {
            for(InvalidLink link : APILocator.getLinkCheckerAPI().findAll(offset, pageSize)) {
                Contentlet con = APILocator.getContentletAPI().find(link.getInode(), getUser(), false);
                User modUser=APILocator.getUserAPI().loadUserById(con.getModUser());
                Field field=FieldsCache.getField(link.getField()); 
                Structure st=StructureCache.getStructureByInode(field.getStructureInode());
                
                Map<String,String> mm=new HashMap<String,String>();
                mm.put("inode", link.getInode());
                mm.put("con_title", con.getTitle());
                mm.put("field", field.getFieldName());
                mm.put("structure", st.getName());
                mm.put("date", df.format(con.getModDate()));
                mm.put("user", modUser.getFullName()+"<"+modUser.getEmailAddress()+">");
                mm.put("url_title", link.getTitle());
                mm.put("url", link.getUrl());
                list.add(mm);
            }
                
            
            result.put("list", list);
            result.put("total", APILocator.getLinkCheckerAPI().findAllCount());
            
            response.setContentType("application/json");
            new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValue(response.getOutputStream(), result);
            
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void action(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {}
    
}
