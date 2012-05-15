package com.dotmarketing.portlets.containers.ajax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContextFactory;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.ajax.util.ContainerAjaxUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

/**
 * Class used by new dwr interface created for Design Template/ Add Metadata to template.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * May 9, 2012 - 11:26:32 AM
 */
public class MetadataContainerAjax extends ContainerAjax {
	
	/**
	 * 
	 * 
	 * 
	 * @param query
	 * @param queryOptions
	 * @param start
	 * @param count
	 * @param sort
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Map<String, Object> fetchMetadataContainers (Map<String, String> query, Map<String, String> queryOptions, int start, int count,
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
					contMap.put("fullTitle", parentHost.getHostname() + ": METADATA - " + contMap.get("title"));
				} else {
					contMap.put("fullTitle", "METADATA - " + contMap.get("title"));
				}
				
				StringBuffer containerCode = new StringBuffer(cont.getCode());
				if(cont.isForMetadata())
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
}
