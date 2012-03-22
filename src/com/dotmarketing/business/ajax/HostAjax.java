package com.dotmarketing.business.ajax;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.quartz.SchedulerException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import edu.emory.mathcs.backport.java.util.Collections;

public class HostAjax {

	private HostAPI hostAPI = APILocator.getHostAPI();
	private UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();

	public Map<String, Object> findHostsForDataStore(String filter, boolean showArchived, int offset, int count) throws PortalException, SystemException, DotDataException, DotSecurityException {

		if(filter.endsWith("*"))
			filter = filter.substring(0, filter.length() - 1);
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);

		HostAPI hostAPI = APILocator.getHostAPI();
		List<Host> hosts = hostAPI.findAll(user, userWebAPI.isLoggedToFrontend(req));
		List<Map<String, Object>> hostResults = new ArrayList<Map<String, Object>>();
		Collections.sort(hosts, new HostNameComparator());

		for(Host host : hosts) {
			if(host.isSystemHost() || (!showArchived && host.isArchived()))
				continue;
			if(host.getHostname().toLowerCase().startsWith(filter.toLowerCase()))
				hostResults.add(host.getMap());
		}

		Map<String, Object> hostMapToReturn =new HashMap<String, Object>();
		hostMapToReturn.put("total",hostResults.size());
		hostMapToReturn.put("list", hostResults);
		return hostMapToReturn;

	}


	public Map<String, Object> findHostsPaginated(String filter, boolean showArchived, int offset, int count) throws DotDataException, DotSecurityException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		PermissionAPI permissionAPI = APILocator.getPermissionAPI();

		boolean respectFrontend = !userWebAPI.isLoggedToBackend(req);
		List<Host> hosts = hostAPI.findAllFromDB(user, respectFrontend);

		long totalResults;
		List<Map<String, Object>> listOfHosts = new ArrayList<Map<String,Object>>(hosts.size());

		Structure hostStructure = StructureCache.getStructureByVelocityVarName("Host");
		List<Field> fields = FieldsCache.getFieldsByStructureVariableName("Host");

		List<Field> searchableFields = new ArrayList<Field>(fields.size());
		for(Field field : fields) {
			if(field.isListed() && field.getFieldType().startsWith("text"))
				searchableFields.add(field);
		}

		Collections.sort(hosts, new HostNameComparator());

		for(Host h : hosts) {
			if(h.isSystemHost())
				continue;
			boolean addToList = false;

			if (showArchived) {
				if(!UtilMethods.isSet(filter))
					addToList = true;
				else {
					for(Field searchableField : searchableFields) {
						String value = h.getStringProperty(searchableField.getVelocityVarName());
						if(value != null && value.toLowerCase().contains(filter.toLowerCase()))
							addToList = true;
					}
				}
			} else if (!h.isArchived()) {
				if(!UtilMethods.isSet(filter))
					addToList = true;
				else {
					for(Field searchableField : searchableFields) {
						String value = h.getStringProperty(searchableField.getVelocityVarName());
						if(value != null && value.toLowerCase().contains(filter.toLowerCase()))
							addToList = true;
					}
				}
			}


			if(addToList) {

				boolean hostInSetup = false;
				try {
					hostInSetup = QuartzUtils.isJobSequentiallyScheduled("setup-host-" + h.getIdentifier(), "setup-host-group");
				} catch (SchedulerException e) {
					Logger.error(HostAjax.class, e.getMessage(), e);
				}

				Map<String, Object> hostMap = h.getMap();
				hostMap.put("userPermissions", permissionAPI.getPermissionIdsFromUser(h, user));
				hostMap.put("hostInSetup", hostInSetup);
				hostMap.put("archived", h.isArchived());
				hostMap.put("live", h.isLive());
				listOfHosts.add(hostMap);
			}
		}

		totalResults = listOfHosts.size();

		if(totalResults > 0 && count > 0) {
			offset = offset >= listOfHosts.size()?listOfHosts.size() - 1:offset;
			count = offset + count > listOfHosts.size()?listOfHosts.size():offset + count;
			listOfHosts = listOfHosts.subList(offset, count);
		}

		List<Map<String, Object>> fieldMaps = new ArrayList<Map<String,Object>>();
		for(Field f: fields) {
			Map<String, Object> fieldMap = f.getMap();
			fieldMaps.add(fieldMap);
		}

		Map<String, Object> toReturn = new HashMap<String, Object>();
		toReturn.put("total", totalResults);
		toReturn.put("list", listOfHosts);
		toReturn.put("structure", hostStructure.getMap());
		toReturn.put("fields", fieldMaps);

		return toReturn;

	}

	public List<Map<String, Object>> findAllHostThumbnails() throws PortalException, SystemException, DotDataException, DotSecurityException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		ContentletAPI contentAPI = APILocator.getContentletAPI();

		boolean respectFrontend = !userWebAPI.isLoggedToBackend(req);
		List<Host> hosts = hostAPI.findAll(user, respectFrontend);
		Collections.sort(hosts, new HostNameComparator());

		List<Map<String, Object>> listOfHosts = new ArrayList<Map<String,Object>>(hosts.size());

		for(Host host : hosts) {
			if(host.isSystemHost())
				continue;
			Map<String, Object> thumbInfo = new HashMap<String, Object>();
			thumbInfo.put("hostId", host.getIdentifier());
			thumbInfo.put("hostInode", host.getInode());
			thumbInfo.put("hostName", host.getHostname());
			File hostThumbnail = contentAPI.getBinaryFile(host.getInode(), Host.HOST_THUMB_KEY, user);
			boolean hasThumbnail = hostThumbnail != null;
			thumbInfo.put("hasThumbnail", hasThumbnail);
			thumbInfo.put("tagStorage", host.getMap().get("tagStorage"));

			listOfHosts.add(thumbInfo);
		}

		return listOfHosts;

	}

	public void publishHost(String id) throws DotDataException, DotSecurityException, PortalException, SystemException {
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(req);
		Host host = hostAPI.find(id, user, respectFrontendRoles);
		hostAPI.publish(host, user, respectFrontendRoles);
	}

	public void unpublishHost(String id) throws DotDataException, DotSecurityException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(req);
		Host host = hostAPI.find(id, user, respectFrontendRoles);
		hostAPI.unpublish(host, user, respectFrontendRoles);
	}

	public void archiveHost(String id) throws DotDataException, DotSecurityException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(req);
		Host host = hostAPI.find(id, user, respectFrontendRoles);
		if(host.isDefault())
			throw new DotStateException("the default host can't be archived");
		hostAPI.archive(host, user, respectFrontendRoles);
	}

	public void unarchiveHost(String id) throws DotDataException, DotSecurityException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(req);
		Host host = hostAPI.find(id, user, respectFrontendRoles);
		hostAPI.unarchive(host, user, respectFrontendRoles);
	}

	public void deleteHost(String id) throws DotDataException, DotSecurityException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(req);
		Host host = hostAPI.find(id, user, respectFrontendRoles);
		if(host.isDefault())
			throw new DotStateException("the default host can't be deleted");
		hostAPI.delete(host, user, respectFrontendRoles);
	}


	public void makeDefault(String id) throws DotDataException, DotSecurityException, PortalException, SystemException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(req);
		Host host = hostAPI.find(id, user, respectFrontendRoles);
		hostAPI.makeDefault(host, user, respectFrontendRoles);
	}

	public int getHostSetupProgress(String hostId) {
		return QuartzUtils.getTaskProgress("setup-host-" + hostId, "setup-host-group");
	}

	public Map<String, Object> fetchByIdentity(String id) throws DotDataException, DotSecurityException {
		Host host = hostAPI.find(id, userWebAPI.getSystemUser(), false);
		return host.getMap();
	}
}
