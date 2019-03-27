package com.dotmarketing.portlets.containers.ajax;

import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;


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

	public Map<String, Object> fetchContainers (final Map<String, String> query,
												final Map<String, String> queryOptions,
												int start, int count,
												final List<String> sort) throws PortalException, SystemException, DotDataException, DotSecurityException {

		final HttpServletRequest request   = WebContextFactory.get().getHttpServletRequest();
		final User user 				   = userWebAPI.getLoggedInUser(request);
		final boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(request);
		final String baseHostId             = (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

		List<Container> fullListContainers = new ArrayList<>();

		try {
			if(UtilMethods.isSet(query.get("hostId"))) {
				final Host host = hostAPI.find(query.get("hostId"), user, respectFrontendRoles);
				fullListContainers = containerAPI.findContainersUnder(host);
			} else {
				final Host host = hostAPI.find(baseHostId, user, respectFrontendRoles);
				fullListContainers = containerAPI.findAllContainers(host, user, respectFrontendRoles);
			}
		}catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}


		Collections.sort(fullListContainers, new ContainerComparator(baseHostId));
		final Map<String, Object> results    = new HashMap<>();
		final List<Map<String, Object>> list = new LinkedList<> ();

		for(final Container container : fullListContainers) {
			if(container != null && !container.isArchived()){
				final Map<String, Object> contMap = container.getMap();
				if(passFilter(contMap, query)) {
					final Host parentHost = containerAPI.getParentHost(container, user, respectFrontendRoles);
					if(parentHost != null) {
						contMap.put("hostName", parentHost.getHostname());
						contMap.put("hostId", parentHost.getIdentifier());
						contMap.put("fullTitle", parentHost.getHostname() + ": " + contMap.get("title"));
					} else {
						contMap.put("fullTitle", contMap.get("title"));
					}

					contMap.put("source", container.getSource().toString());
					if (container instanceof FileAssetContainer) {

						contMap.put("path", FileAssetContainer.class.cast(container).getPath());
					}

					list.add(contMap);
				}
			}
		}

		if(start >= list.size()) start =  list.size() - 1;
		if(start < 0)  start  = 0;
		if(start + count >= list.size()) count = list.size() - start;
		final List<Map<String, Object>> containers = list.subList(start, start + count);

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

    public List<Map<String, String>> getContainerStructures(String containerInode) throws Exception {
        return getContainerStructures(containerInode, false);
    }

    public List<Map<String, String>> getContainerStructuresWithAllOption(String containerInode) throws Exception {
        return getContainerStructures(containerInode, true);
    }

	private List<Map<String, String>> getContainerStructures(String containerInode, boolean withAllOption) throws Exception{
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
	    Container cont = (Container) InodeFactory.getInode(containerInode, Container.class);

		List<Map<String,String>> resultList = new ArrayList<Map<String,String>>();
		List<ContainerStructure> csList;

		try {
			csList = APILocator.getContainerAPI().getContainerStructures(cont);

            // Include ALL option
            if (withAllOption) {
                Map<String, String> result = new HashMap<String, String>();
                result.put("inode", Structure.STRUCTURE_TYPE_ALL);
                result.put("name", LanguageUtil.get(userWebAPI.getLoggedInUser(request), "all"));
                resultList.add(result);
            }

			for (ContainerStructure cs : csList) {
				Map<String, String> result = new HashMap<String, String>();
				Structure st = CacheLocator.getContentTypeCache().getStructureByInode(cs.getStructureId());
				result.put("inode", cs.getStructureId());
				result.put("name", st.getName());
				resultList.add(result);
			}

		} catch (Exception e) {
			Logger.error(getClass(), e.getMessage());
			throw e;
		}

		return resultList;
	}
	
	public List<Map<String, String>> getContainerStructuresForUser(String containerInode) throws Exception{
		Container cont = (Container) InodeFactory.getInode(containerInode, Container.class);

		List<Map<String,String>> resultList = new ArrayList<Map<String,String>>();
		List<ContainerStructure> csList;

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		PermissionAPI permissionAPI = APILocator.getPermissionAPI();


		try {
			csList = APILocator.getContainerAPI().getContainerStructures(cont);

			for (ContainerStructure cs : csList) {
				Map<String, String> result = new HashMap<String, String>();
				Structure st = CacheLocator.getContentTypeCache().getStructureByInode(cs.getStructureId());
				if(permissionAPI.doesUserHavePermission(st, PERMISSION_WRITE, user)){
					result.put("inode", cs.getStructureId());
					result.put("name", st.getName());
					resultList.add(result);
				}
			}

		} catch (Exception e) {
			Logger.error(getClass(), e.getMessage());
			throw e;
		}

		return resultList;
	}

	public String checkDependencies(String containerInode) throws DotDataException, DotRuntimeException, DotSecurityException, PortalException, SystemException{
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);
		String[] inodesArray = containerInode.split(",");
		String result= null;
		for(String contInode : inodesArray){
			Container cont = (Container) InodeFactory.getInode(contInode, Container.class);
			TemplateAPI templateAPI = APILocator.getTemplateAPI();
			List<Template> templates = templateAPI.findTemplates(user, true, null, null, null, null, null, 0, -1, null);
			result = checkTemplatesUsedByContainer(templates,cont,user, respectFrontendRoles);			
			if(result.length()>0){
				StringBuilder dialogMessage=new StringBuilder();
				dialogMessage.append(LanguageUtil.get(user,"container-used-templates")).append("<br> <br>");
				dialogMessage.append(LanguageUtil.get(user,"Container") + " : ").append(cont.getTitle()).append("<br>");
				dialogMessage.append(LanguageUtil.get(user,"templates") + " : ").append(result);
				return dialogMessage.length()>0?dialogMessage.toString():null;
			}
				
		}

		return result.length()>0?result.toString():null;

	}
	
	private String checkTemplatesUsedByContainer(List<Template> templates,Container cont, User user,boolean respectFrontendRoles ) throws DotSecurityException,	DotDataException {
		TemplateAPI templateAPI = APILocator.getTemplateAPI();
		StringBuilder names=new StringBuilder();
		String result = null;
		for (Template template : templates) {
			List<Container> containers = templateAPI.getContainersInTemplate(template, user, respectFrontendRoles);
			if(containers.contains(cont)) {
				names.append(template.getTitle()).append("</br> ");
			}
		}
		result = names.toString();
		return result;
	}
	
	
}
