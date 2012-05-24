package com.dotmarketing.portlets.workflows.ajax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class WfRoleStoreAjax extends WfBaseAction {

	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String searchName = request.getParameter("searchName");

		Map m = request.getParameterMap();
		
		
		if(searchName ==null) searchName ="";
		String roleId = request.getParameter("roleId");
		RoleAPI rapi = APILocator.getRoleAPI();
		
		Map<String, Object> mm= request.getParameterMap();
		for (String x : mm.keySet()) {
			//System.out.println(x + ":"+ ((String[])mm.get(x))[0]);
		}
		
		int start = 0 ;
		int count = 20;
		try{
			start = Integer.parseInt(request.getParameter("start"));
		}
		catch(Exception e){
			
		}
		try {
			count = Integer.parseInt(request.getParameter("count"));
		} catch (Exception e) {

		}
		
		try {
			Role cmsAnon = APILocator.getRoleAPI().loadCMSAnonymousRole();

			String cmsAnonName =LanguageUtil.get(getUser(), "current-user");
			boolean addSystemUser = false;
			if(searchName.length() > 0 && cmsAnonName.startsWith(searchName)){
				addSystemUser = true;
			}

	        List<Role> roleList = new ArrayList<Role>();
	        if(UtilMethods.isSet(roleId)){
	        	try{
	        		Role r = rapi.loadRoleById(roleId);
	        		if(r!= null){
	        			roleList.add(r);
	        			response.getWriter().write(rolesToJson(roleList));
	        			return;
	        		}	        		
	        	}
	        	catch(Exception e){
	        		
	        	}
	        	
	        }	        
	        
			while(roleList.size() < count){				
				List<Role> roles = rapi.findRolesByFilterLeftWildcard(searchName, start, count);
				if(roles.size() ==0){
					break;
				}
		        for(Role role : roles){
		        	if(role.isUser()){		        		
			        	try {		        		
			        		APILocator.getUserAPI().loadUserById(role.getRoleKey(), APILocator.getUserAPI().getSystemUser(), false);
						} catch (Exception e) {						
							//Logger.error(WfRoleStoreAjax.class,e.getMessage(),e);
							continue;
						}
		        	}
		        	if(role.getId().equals(cmsAnon.getId())){		        		
		        		Role rAnon = new Role();
		        		BeanUtils.copyProperties(rAnon, role);		        		
		        		role = rAnon;		        		
		        		role.setName(cmsAnonName);
		        		addSystemUser = false;
		        	}		        	
		        	if(role.isSystem() && ! role.isUser() && !role.getId().equals(cmsAnon.getId())){
		        		continue;
		        	}
		        	if(role.getName().equals(searchName)){
		        		roleList.add(0,role);
		        	}
		        	else{
		        		roleList.add(role);		        		
		        	}		        		        
		        }
		        start = start + count;
			}
			if(addSystemUser){
				cmsAnon.setName(cmsAnonName);
				roleList.add(0,cmsAnon);
			}

			
			//x = x.replaceAll("identifier", "x");
            response.getWriter().write(rolesToJson(roleList));

		} catch (Exception e) {
			Logger.error(WfRoleStoreAjax.class,e.getMessage(),e);
		}
		
	}
	
	public void assignable(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	
		String name = request.getParameter("name");

		try {
			String actionId = request.getParameter("actionId");
			WorkflowAction action = APILocator.getWorkflowAPI().findAction(actionId, getUser());
			Role role = APILocator.getRoleAPI().loadRoleById(action.getNextAssign());
	        List<Role> roleList = new ArrayList<Role>();
	        List<User> userList = new ArrayList<User>();
			if(!role.isUser()){
				if(action.isRoleHierarchyForAssign()){
			        userList = APILocator.getRoleAPI().findUsersForRole(role, true);
			        roleList.addAll(APILocator.getRoleAPI().findRoleHierarchy(role));	
				}
				else{
			        userList = APILocator.getRoleAPI().findUsersForRole(role, false);
			        roleList.add(role);
				}
			}
			else{
				userList.add(APILocator.getUserAPI().loadUserById(role.getRoleKey(), APILocator.getUserAPI().getSystemUser(), false));	
			
			}
			
			

	        

			for(User user :userList){
				Role r =APILocator.getRoleAPI().getUserRole(user);
				if(r !=null && UtilMethods.isSet(r.getId())){
					roleList.add(r);
				}
			}
			if(name != null){
				
				name = name.toLowerCase().replaceAll("\\*", "");
				if(UtilMethods.isSet(name)){
					List<Role> newRoleList = new ArrayList<Role>();
					for(Role r : roleList){
						if(r.getName().toLowerCase().startsWith(name)){
							newRoleList.add(r);
						}
					}
					roleList = newRoleList;
				}
				
			}
			
			
			
			
			
			
            response.getWriter().write(rolesToJson(roleList));
		} catch (Exception e) {
			Logger.error(WfRoleStoreAjax.class,e.getMessage(),e);
		}
		
		
	}
	
	private String rolesToJson(List<Role> roles) throws JsonGenerationException, JsonMappingException, IOException, DotDataException, LanguageException{
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Map<String,Object> m = new LinkedHashMap<String, Object>();
        
        List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
        Map<String,Object> map = new HashMap<String,Object>();
        Role cmsAnon = APILocator.getRoleAPI().loadCMSAnonymousRole();
        for(Role role : roles){

        	map = new HashMap<String,Object>();
        	if(role.getId().equals(cmsAnon.getId())){
        		map.put("name", role.getName()  );
        	}
        	else{
        		map.put("name", role.getName()  + ((role.isUser()) ? " (" + LanguageUtil.get(PublicCompanyFactory.getDefaultCompany(), "User") + ")" : "") );
        	}
        	map.put("id", role.getId());
    		list.add(map);
        }
        
        
        
        m.put("identifier", "id");
        m.put("label", "name");
        m.put("items", list);
		return mapper.defaultPrettyPrintingWriter().writeValueAsString(m);
	}
	
	
}
