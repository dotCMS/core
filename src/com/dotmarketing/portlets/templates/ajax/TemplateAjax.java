package com.dotmarketing.portlets.templates.ajax;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContextFactory;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.factories.TemplateFactory;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.templates.model.TemplateWrapper;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;


/**
 * @author David
 */
public class TemplateAjax {

	UserWebAPI userWebAPI;
	TemplateAPI templateAPI;
	HostAPI hostAPI;

	public TemplateAjax () {
		templateAPI = APILocator.getTemplateAPI();
		userWebAPI = WebAPILocator.getUserWebAPI();
		hostAPI = APILocator.getHostAPI();
	}

	public Map<String, Object> fetchTemplates (Map<String, String> query, Map<String, String> queryOptions, int start, int count,
			List<String> sort) throws PortalException, SystemException, DotDataException, DotSecurityException {

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);
		if(count<=0)count=10;

		List<Template> fullListTemplates = new ArrayList<Template>();
		Host host = hostAPI.find(query.get("hostId"), user, respectFrontendRoles);

		try{
			String filter = query.get("fullTitle");
			if(UtilMethods.isSet(filter)){
				filter = filter.replaceAll("\\*", "");
				filter = filter.replaceAll("\\?", "");
			}


			if(UtilMethods.isSet(query.get("hostId"))) {
				fullListTemplates.addAll(templateAPI.findTemplatesUserCanUse(user, host.getHostname(), filter, true, start, count));
			}

			//doesn't currently respect archived
			if(fullListTemplates.size() ==0){

				fullListTemplates.addAll(templateAPI.findTemplatesUserCanUse(user,"", filter,true, start, start>0?count:count+1));

			}


		}catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
		//Collections.sort(fullListTemplates, new TemplateComparator(baseHostId));
		Map<String, Object> results = new HashMap<String, Object>();
		List<Map<String, Object>> list = new LinkedList<Map<String, Object>> ();

		boolean shouldIncludeTemplate = true;
		String toInclude = queryOptions.get("includeTemplate");
		for(Template template : fullListTemplates) {
			Map<String, Object> contMap = buildTemplateMap(template);
			list.add(contMap);
		}
		if(toInclude != null && shouldIncludeTemplate) {
			Template template = templateAPI.findWorkingTemplate(toInclude, APILocator.getUserAPI().getSystemUser(), false);
			if(template != null) {
				list.add(buildTemplateMap(template));
			}
		}


//		if(start >= list.size()) start =  list.size() - 1;
//		if(start < 0)  start  = 0;
//		if(start + count >= list.size()) count = list.size() - start;
//		List<Map<String, Object>> templates = list.subList(start, start + count);

		results.put("totalResults", list.size());
		if(UtilMethods.isSet(query.get("hostId"))) {
			results.put("totalResults", list.size());
		}
		results.put("list", list);

		return results;
	}

	public Map<String, Object> fetchByIdentity(String id) throws DotDataException, DotSecurityException {
		return buildTemplateMap(templateAPI.findWorkingTemplate(id, userWebAPI.getSystemUser(), false));
	}

	private Map<String, Object> buildTemplateMap(Template template) throws DotDataException, DotStateException, DotSecurityException {
		if(template == null) return null;
		Host parentHost = null;
		if(template instanceof TemplateWrapper){
			parentHost = ((TemplateWrapper) template).getHost();
		}else{
			try{
			  parentHost = templateAPI.getTemplateHost(template);
			}catch(DotDataException e){
				Logger.warn(this, "Could not find host for template = " + template.getIdentifier());
			}
		}
		Map<String, Object> templateMap = template.getMap();
		if(parentHost != null) {
			templateMap.put("hostName", parentHost.getHostname());
			templateMap.put("hostId", parentHost.getIdentifier());
			templateMap.put("fullTitle", parentHost.getHostname() + " " + templateMap.get("title"));
		} else {
			templateMap.put("fullTitle", templateMap.get("title"));
		}
		return templateMap;
	}



	class TemplateComparator implements Comparator<Template> {

		private String baseHostId;

		public TemplateComparator(String baseHostId) {
			this.baseHostId = baseHostId;
		}

		public int compare(Template o1, Template o2) {
			try {
				Identifier id1 = APILocator.getIdentifierAPI().find(o1.getIdentifier());
				if(id1.getHostId() == null) id1 = null;
				Identifier id2 = APILocator.getIdentifierAPI().find(o2.getIdentifier());
				if(id2.getHostId() == null) id2 = null;
				if(id1 != null && id2 != null && id1.getHostId().equals(baseHostId) && id2.getHostId().equals(baseHostId)) {
					return o1.getTitle().compareTo(o2.getTitle());
				}
				if(id1 != null && id1.getHostId().equals(baseHostId)) {
					return -1;
				}
				if(id2 != null && id2.getHostId().equals(baseHostId)) {
					return 1;
				}
				return id1 == null || id2 == null || id1.getHostId().equals(id2.getHostId())?o1.getTitle().compareTo(o2.getTitle()):id1.getHostId().compareTo(id2.getHostId());

			} catch (DotDataException e) {

			}
			return 0;
		}

	}



	private boolean passFilter(Map<String, Object> item, Map<String, String> query) {
		for(String key : item.keySet()) {
			if(query.containsKey(key)) {
				String filter = query.get(key);
				filter = "^" + filter.replaceAll("\\*", ".*");
				filter = filter.replaceAll("\\?", ".?");
				String value = item.get(key).toString();
				if(!RegEX.contains(value.toLowerCase(), filter.toLowerCase()))
					return false;
			}
		}
		return true;
	}

	public Map<String, Object> fetchTemplateImage(String id) throws DotDataException, DotSecurityException {
		Map<String, Object> toReturn =  new HashMap<String, Object>();
		Template template = null;
		try{
		   template = templateAPI.findWorkingTemplate(id, APILocator.getUserAPI().getSystemUser(), false);
		}catch(DotSecurityException e){
			Logger.error(this, e.getMessage());
		}
		if(template!=null){
			File imgFile = TemplateFactory.getImageFile(template);
			if(imgFile!=null){
				toReturn.put("inode", imgFile.getInode());
				toReturn.put("name", imgFile.getFileName());
				toReturn.put("identifier", imgFile.getIdentifier());
				toReturn.put("extension", com.dotmarketing.util.UtilMethods.getFileExtension(imgFile.getFileName()));
			}
		}
		return toReturn;
	}

	public String checkDependencies(String templateInode) throws DotDataException, DotRuntimeException, DotSecurityException, PortalException, SystemException{
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);
		return templateAPI.checkDependencies(templateInode, user, respectFrontendRoles);
	}

}


