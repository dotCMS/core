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
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

/**
 * This class handles the communication between the UI and the back-end service that returns information to the user
 * regarding Containers in dotCMS. The information provided by this service is accessed via DWR.
 * <p>
 * For example, the <b>Container</b> portlet uses this class to display Container information to the users, which can
 * be filtered by specific search criteria.
 *
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
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

		List<Container> fullListContainers;

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

		List<Map<String,String>> resultList = new ArrayList<>();
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

    /**
     * Verifies if the specified comma-separated list of Container Inodes are present in any Template under any Site in
     * dotCMS.
     *
     * @param containerInode The list of Container Inodes that will be verified.
     *
     * @return If the Containers are in at least one Template, a formatted message with Container and Template
     * information will be returned. Otherwise, a {@code null} will be returned.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The logged-in user does not have the required permissions to perform this action.
     * @throws PortalException      Failed to check the currently logged-in user.
     * @throws SystemException      Failed to check the currently logged-in user.
     */
    public String checkDependencies(final String containerInode) throws DotDataException, DotSecurityException, PortalException, SystemException {
		final HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		final User user = userWebAPI.getLoggedInUser(req);
		final boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);
		final String[] inodesArray = containerInode.split(",");
		String templateInfo = null;
        final TemplateAPI templateAPI = APILocator.getTemplateAPI();
		for (final String contInode : inodesArray) {
			final Container container = (Container) InodeFactory.getInode(contInode, Container.class);
			final List<Template> templates = templateAPI.findTemplates(user, true, null, null, null, null, null, 0, -1, null);
			templateInfo = checkTemplatesUsedByContainer(templates, container, user, respectFrontendRoles);
			if (UtilMethods.isSet(templateInfo)) {
				final String containerDependencyResults = formatContainerDependencyMsg(container, templateInfo, user);
				return UtilMethods.isSet(containerDependencyResults) ? containerDependencyResults : null;
			}
		}
		return UtilMethods.isSet(templateInfo) ? templateInfo : null;
		}

    /**
     * Utility method that puts together the information message displayed to the user when a Container is being used by
     * at least one Template in the system.
     *
     * @param container    The {@link Container} that is being verified.
     * @param templateInfo Basic Template information for the user to determine what Templates are still using the
     *                     Container.
     *
     * @return The formatted message that will be displayed to the user.
     */
	private String formatContainerDependencyMsg(final Container container, final String templateInfo, final User user)
			throws LanguageException {
		final StringBuilder msg = new StringBuilder();
		msg.append(LanguageUtil.get(user, "container-used-templates"));
		msg.append("<br/><br/>");
		msg.append(LanguageUtil.get(user, "Container")).append(":<br/>");
		msg.append("<ul><li>").append("<b>").append(container.getTitle()).append("</b>").append("</li></ul>").append
				("<br/>");
		msg.append(LanguageUtil.get(user, "templates")).append(":<br/>");
		msg.append("<ul>");
		msg.append(templateInfo);
		msg.append("</ul>");
		return msg.toString();
	}
	
    /**
     * Verifies if the given Container exists in any of the list of provided Templates. This is commonly used by the
     * Container deletion operation in order to prevent users from deleting Containers that are currently in use.
     *
     * @param templates            The List of {@link Template} objects that need to be checked.
     * @param container            The {@link Container} that will be verified.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If front-end roles will be take in count for this operation (which means this is
     *                             being called from the front-end), set to {@code true}. Otherwise, set to {@code
     *                             false}.
     *
     * @return The list of Templates, if any, that have a reference to the specified Container.
     *
     * @throws DotSecurityException The specified user does not have the correct permissions to perform this action.
     * @throws DotDataException     An error occurred when interacting with the data source.
     */
    private String checkTemplatesUsedByContainer(final List<Template> templates, final Container container, final User user, final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		final TemplateAPI templateAPI = APILocator.getTemplateAPI();
		final StringBuilder names = new StringBuilder();
		String result;
		for (final Template template : templates) {
			final List<Container> containers = templateAPI.getContainersInTemplate(template, user, respectFrontendRoles);
			if (containers.contains(container)) {
				names.append(getTemplateInfo(template, user, respectFrontendRoles));
			}
		}
		result = names.toString();
		return result;
	}
	
    /**
     * Utility method that puts together useful Template information when a Container is being used in such a Template.
     *
     * @param template             The {@link Template} whose basic information will be retrieved.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If front-end roles will be take in count for this operation (which means this is
     *                             being called from the front-end), set to {@code true}. Otherwise, set to {@code
     *                             false}.
     *
     * @return The basic information for the provided Template.
     */
    private String getTemplateInfo(final Template template, final User user, final boolean respectFrontendRoles) {
        final String siteIdError = "- Site not available -";
        final String templateIdError = "- Identifier not found -";
        String siteName;
        final String templateTitle = template.getTitle();
        final Identifier templateId = Try.of(() -> APILocator.getIdentifierAPI().find(template.getIdentifier()))
                .getOrNull();
        if (null == templateId || !UtilMethods.isSet(templateId.getId())) {
            siteName = templateIdError;
        } else {
            final Host site = Try.of(() -> APILocator.getHostAPI().find(templateId.getHostId(), user,
                    respectFrontendRoles)).getOrNull();
            siteName = null == site || !UtilMethods.isSet(site.getIdentifier()) ? siteIdError : site.getHostname();
        }
        final String templateInfo = String.format("<li><b>%s ( %s ) under Site %s</b></li>", templateTitle, template
                .getIdentifier(), siteName);
        return templateInfo;
    }

}
