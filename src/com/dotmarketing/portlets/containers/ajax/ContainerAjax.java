package com.dotmarketing.portlets.containers.ajax;

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
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.ajax.util.ContainerAjaxUtil;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import edu.emory.mathcs.backport.java.util.Collections;


/**
 * @author David
 */
public class ContainerAjax {

	UserWebAPI userWebAPI;
	ContainerAPI containerAPI;
	HostAPI hostAPI;

	public ContainerAjax () {
		containerAPI = APILocator.getContainerAPI();
		userWebAPI = WebAPILocator.getUserWebAPI();
		hostAPI = APILocator.getHostAPI();
	}

	public Map<String, Object> fetchContainers (Map<String, String> query, Map<String, String> queryOptions, int start, int count,
			List<String> sort) throws PortalException, SystemException, DotDataException, DotSecurityException {
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);
	
		List<Container> fullListContainers = new ArrayList<Container>();
		try{
			if(UtilMethods.isSet(query.get("hostId"))) {
				Host host = hostAPI.find(query.get("hostId"), user, respectFrontendRoles);
				fullListContainers = containerAPI.findContainersUnder(host);
			} else {
				fullListContainers = containerAPI.findAllContainers(user, respectFrontendRoles);
			}
		}catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
		String baseHostId = (String) req.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
		Collections.sort(fullListContainers, new ContainerComparator(baseHostId));
		Map<String, Object> results = new HashMap<String, Object>();
		List<Map<String, Object>> list = new LinkedList<Map<String, Object>> ();
	
		for(Container cont : fullListContainers) {
			Map<String, Object> contMap = cont.getMap();
			if(passFilter(contMap, query)) {
				Host parentHost = containerAPI.getParentHost(cont, user, respectFrontendRoles);
				if(parentHost != null) {
					contMap.put("hostName", parentHost.getHostname());
					contMap.put("hostId", parentHost.getIdentifier());
					contMap.put("fullTitle", parentHost.getHostname() + ": " + contMap.get("title"));
				} else {
					contMap.put("fullTitle", contMap.get("title"));
				}
	
				list.add(contMap);
			}
		}
	
		if(start >= list.size()) start =  list.size() - 1;
		if(start < 0)  start  = 0;
		if(start + count >= list.size()) count = list.size() - start;
		List<Map<String, Object>> containers = list.subList(start, start + count);
	
		results.put("totalResults", list.size());
		results.put("list", containers);
	
		return results;
	}
	
	class ContainerComparator implements Comparator<Container> {

		private String baseHostId;

		public ContainerComparator(String baseHostId) {
			this.baseHostId = baseHostId;
		}

		public int compare(Container o1, Container o2) {
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

	protected boolean passFilter(Map<String, Object> item, Map<String, String> query) {
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

	public java.util.Map<String, String> getContainerStructure(String containerInode){
		Container cont = (Container) InodeFactory.getInode(containerInode, Container.class);
		Structure st = (Structure)InodeFactory.getInode(cont.getStructureInode(), Structure.class);
		Map<String, String> result = new HashMap<String, String>();
		result.put("inode", st.getInode());
		return result;
	}

	public String checkDependencies(String containerInode) throws DotDataException, DotRuntimeException, DotSecurityException, PortalException, SystemException{
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);
		Container cont = (Container) InodeFactory.getInode(containerInode, Container.class);
		TemplateAPI templateAPI = APILocator.getTemplateAPI();
		List<Template> templates = templateAPI.findTemplates(user, true, null, null, null, null, null, 0, -1, null);

		StringBuilder names=new StringBuilder();
		for (Template template : templates) {
			List<Container> containers = templateAPI.getContainersInTemplate(template, user, respectFrontendRoles);
			if(containers.contains(cont)) {
				names.append(template.getFriendlyName()).append(", ");
			}
		}

		return names.length()>0?names.toString():null;

	}
}
