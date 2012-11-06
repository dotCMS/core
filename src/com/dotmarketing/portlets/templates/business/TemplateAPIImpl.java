package com.dotmarketing.portlets.templates.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BaseWebAssetAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI.TemplateContainersReMap.ContainerRemapTuple;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.StringUtil;

public class TemplateAPIImpl extends BaseWebAssetAPI implements TemplateAPI {

	static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	static IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
	static TemplateFactory templateFactory = FactoryLocator.getTemplateFactory();
	static String containerTag = "#parseContainer('";
	static ContainerAPI containerAPI = APILocator.getContainerAPI();
	static HostAPI hostAPI = APILocator.getHostAPI();
	static UserAPI userAPI = APILocator.getUserAPI();

	private static ThreadLocal<Perl5Matcher> localP5Matcher = new ThreadLocal<Perl5Matcher>(){
		protected Perl5Matcher initialValue() {
			return new Perl5Matcher();
		}
	};

	private static org.apache.oro.text.regex.Pattern parseContainerPattern;
	private static org.apache.oro.text.regex.Pattern oldContainerPattern;

	static {
		Perl5Compiler c = new Perl5Compiler();
    	try{
	    	parseContainerPattern = c.compile("#parse\\( \\$container.* \\)",Perl5Compiler.READ_ONLY_MASK);
	    	oldContainerPattern = c.compile("[0-9]+",Perl5Compiler.READ_ONLY_MASK);
    	}catch (MalformedPatternException mfe) {
    		Logger.fatal(TemplateAPIImpl.class, "Unable to instaniate dotCMS Velocity Cache", mfe);
			Logger.error(TemplateAPIImpl.class, mfe.getMessage(), mfe);
		}
	}

	public List<Template> findTemplatesUnder(Folder parentFolder) throws DotDataException {
		return FactoryLocator.getTemplateFactory().findTemplatesUnder(parentFolder);
	}

	public List<Template> findTemplatesAssignedTo(Host parentHost) throws DotDataException {
		return FactoryLocator.getTemplateFactory().findTemplatesAssignedTo(parentHost, false);
	}

	public List<Template> findTemplatesAssignedTo(Host parentHost, boolean includeArchived) throws DotDataException {
		return FactoryLocator.getTemplateFactory().findTemplatesAssignedTo(parentHost, includeArchived);
	}

	public List<Template> findTemplatesUserCanUse(User user, String hostName, String query, boolean searchHost,int offset, int limit) throws DotDataException, DotSecurityException {
		return FactoryLocator.getTemplateFactory().findTemplatesUserCanUse(user, hostName, query, searchHost, offset, limit);
	}

	public void delete(Template template) throws DotDataException {
		FactoryLocator.getTemplateFactory().delete(template);
	}


	public Template copy(Template sourceTemplate, Host destination, boolean forceOverwrite, List<ContainerRemapTuple> containerMappings, User user,
			boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(sourceTemplate, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source file.");
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write in the destination folder.");
		}

		boolean isNew = false;
		Template newTemplate;
		if (forceOverwrite) {
			newTemplate = FactoryLocator.getTemplateFactory().findWorkingTemplateByName(sourceTemplate.getTitle(), destination);
			if (newTemplate == null) {
				isNew = true;
			}
		} else {
			isNew = true;
		}

		newTemplate = new Template();

		newTemplate.copy(sourceTemplate);
		newTemplate.setImage(sourceTemplate.getImage());

		if (!forceOverwrite) {
			newTemplate.setTitle(getCopyTemplateName(sourceTemplate.getTitle(), destination));

			if (!newTemplate.getTitle().equals(sourceTemplate.getTitle()))
				newTemplate.setFriendlyName(sourceTemplate.getFriendlyName() + " (COPY)");
		}


		updateParseContainerSyntax(newTemplate);
		newTemplate.setBody(replaceWithNewContainerIds(newTemplate.getBody(), containerMappings));

		if (isNew) {
			// creates new identifier for this webasset and persists it
			Identifier newIdentifier = com.dotmarketing.business.APILocator.getIdentifierAPI().createNew(newTemplate, destination);
			Logger.debug(TemplateFactory.class, "Parent newIdentifier=" + newIdentifier.getInode());

			newTemplate.setIdentifier(newIdentifier.getInode());
			// persists the webasset
			save(newTemplate);

			List<Container> destinationContainers = getContainersInTemplate(newTemplate, user, respectFrontendRoles);
			associateContainers(destinationContainers, newTemplate);

			//Copy the host again
			newIdentifier.setHostId(destination.getIdentifier());
		} else {
			saveTemplate(newTemplate, destination, user, respectFrontendRoles);
		}

		APILocator.getVersionableAPI().setWorking(newTemplate);
		if(sourceTemplate.isLive())
		    APILocator.getVersionableAPI().setLive(newTemplate);

		// Copy permissions
		permissionAPI.copyPermissions(sourceTemplate, newTemplate);

		save(newTemplate);

		return newTemplate;
	}


	public List<HTMLPage> getPagesUsingTemplate(Template template, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(template, PermissionAPI.PERMISSION_READ, user,
				respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the template file.");
		}

		return templateFactory.getPagesUsingTemplate(template);
	}

	public Template copy(Template sourceTemplate, Host destination, boolean forceOverwrite,
			boolean copySourceContainers, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(sourceTemplate, PermissionAPI.PERMISSION_READ, user,
				respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source file.");
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user,
				respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write in the destination folder.");
		}

		List<ContainerRemapTuple> remap = new LinkedList<ContainerRemapTuple>();
		if (copySourceContainers) {
			List<Container> sourceContainers = getContainersInTemplate(sourceTemplate, user, respectFrontendRoles);
			Container newContainer;
			for (Container container : sourceContainers) {
				newContainer = containerAPI.copy(container, destination, user, respectFrontendRoles);
				remap.add(new ContainerRemapTuple(container, newContainer));
			}
		}

		return copy(sourceTemplate, destination, forceOverwrite, remap, user, respectFrontendRoles);
	}

	private void save(Template template) throws DotDataException {
		templateFactory.save(template);
	}

	protected void save(WebAsset webAsset) throws DotDataException {
		save((Template) webAsset);
	}



	public Template saveTemplate(Template template, Host destination, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Template oldTemplate = UtilMethods.isSet(template.getIdentifier())
				?findWorkingTemplate(template.getIdentifier(), user, respectFrontendRoles)
						:null;


		if ((oldTemplate != null) && InodeUtils.isSet(oldTemplate.getInode())) {
			if (!permissionAPI.doesUserHavePermission(oldTemplate, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
				throw new DotSecurityException("You don't have permission to read the source file.");
			}
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to wirte in the destination folder.");
		}

		if (!permissionAPI.doesUserHavePermissions(PermissionableType.TEMPLATES, PermissionAPI.PERMISSION_EDIT, user)) {
			throw new DotSecurityException("You don't have permission to edit templates.");
		}


		//gets identifier from the current asset
		Identifier identifier = null;
		if (oldTemplate != null) {
			templateFactory.deleteFromCache(oldTemplate);
			identifier = identifierAPI.findFromInode(oldTemplate.getIdentifier());
		}
		else{
			identifier = APILocator.getIdentifierAPI().createNew(template, destination);
			template.setIdentifier(identifier.getId());
		}
		template.setModDate(new Date());
		template.setModUser(user.getUserId());

		//we need to replace older container parse syntax with updated syntax
		updateParseContainerSyntax(template);

		//it saves or updates the asset
		save(template);
		APILocator.getVersionableAPI().setWorking(template);


		///parses the body tag to get all identifier ids and saves them as children
		List<Container> containers = getContainersInTemplate(template, user, respectFrontendRoles);
		associateContainers(containers, template);



		return template;
	}

	public List<Container> getContainersInTemplate(Template template, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {


		if (!permissionAPI.doesUserHavePermission(template, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source file.");
		}

		List<Container> result = new ArrayList<Container>();
		List<String> ids = getContainerIds(template.getBody());
		for(String containerId : ids) {
			Container container = containerAPI.getWorkingContainerById(containerId, user, respectFrontendRoles);
			if(container != null) {
				result.add(container);
			} else {
				Logger.warn(this,"ERROR The Container Id: '" + containerId + "' doesn't exist and its reference by template " + template.getIdentifier());
			}
		}
		return result;

	}

	private List<String> getContainerIds(String templateBody) {
		Pattern oldContainerReferencesRegex = Pattern.compile("#parse\\s*\\(\\s*\\$container([^\\s)]+)\\s*\\)");
		Pattern newContainerReferencesRegex = Pattern.compile("#parseContainer\\s*\\(\\s*['\"]*([^'\")]+)['\"]*\\s*\\)");
		Matcher matcher = oldContainerReferencesRegex.matcher(templateBody);
		List<String> ids = new LinkedList<String>();
		while(matcher.find()) {
			String containerId = matcher.group(1).trim();
			if(!ids.contains(containerId))
			ids.add(containerId);
		}
		matcher = newContainerReferencesRegex.matcher(templateBody);
		while(matcher.find()) {
			String containerId = matcher.group(1).trim();
			if(!ids.contains(containerId))
			ids.add(containerId);
		}
		return ids;
	}

	private String replaceWithNewContainerIds(String body, List<ContainerRemapTuple> containerMappings) {

		Pattern oldContainerReferencesRegex = Pattern.compile("#parse\\s*\\(\\s*\\$container([^\\s]+)\\s*\\)");
		Pattern newContainerReferencesRegex = Pattern.compile("#parseContainer\\s*\\(\\s*['\"]*([^'\")]+)['\"]*\\s*\\)");

		StringBuffer newBody = new StringBuffer();
		Matcher matcher = oldContainerReferencesRegex.matcher(body);
		while(matcher.find()) {
			String containerId = matcher.group(1).trim();
			for(ContainerRemapTuple tuple : containerMappings) {
				if(tuple.getSourceContainer().getIdentifier().equals(containerId)) {
					matcher.appendReplacement(newBody, "#parseContainer('" + tuple.getDestinationContainer().getIdentifier() +"')");
				}
			}
		}
		matcher.appendTail(newBody);

		body = newBody.toString();
		newBody = new StringBuffer();
		matcher = newContainerReferencesRegex.matcher(body);
		while(matcher.find()) {
			String containerId = matcher.group(1).trim();
			for(ContainerRemapTuple tuple : containerMappings) {
				if(tuple.getSourceContainer().getIdentifier().equals(containerId)) {
					matcher.appendReplacement(newBody, "#parseContainer('" + tuple.getDestinationContainer().getIdentifier() +"')");
					break;
				}
			}
		}
		matcher.appendTail(newBody);

		return newBody.toString();
	}

	public void updateParseContainerSyntax(Template template) {
		String tb = template.getBody();
		Perl5Matcher matcher = (Perl5Matcher) localP5Matcher.get();
		String oldParse;
		String newParse;
    	while(matcher.contains(tb, parseContainerPattern)){
     		MatchResult match = matcher.getMatch();
    		int groups = match.groups();
     		for(int g=0;g<groups;g++){
     			oldParse = match.group(g);
     			if(matcher.contains(oldParse, oldContainerPattern)){
     				MatchResult matchOld = matcher.getMatch();
     				newParse = matchOld.group(0).trim();
     				newParse = containerTag + newParse + "')";
     				tb = StringUtil.replace(tb,oldParse,newParse);
     			}
     		}
     		template.setBody(tb);
    	}
	}

	@SuppressWarnings("unchecked")
	private String getCopyTemplateName(String templateName, Host host) throws DotDataException {
		String result = new String(templateName);

		List<Template> templates = findTemplatesAssignedTo(host);

		boolean isValidTemplateName = false;

		for (; !isValidTemplateName;) {
			isValidTemplateName = true;

			for (Template template: templates) {
				if (template.getTitle().equals(result)) {
					isValidTemplateName = false;

					break;
				}
			}

			if (!isValidTemplateName)
				result += " (COPY)";
		}

		return result;
	}





	public Host getTemplateHost(Template template) throws DotDataException {

		try {
			Host host = APILocator.getHostAPI().findParentHost(template, APILocator.getUserAPI().getSystemUser(), false);
			return host;
		} catch (DotSecurityException e1) {
			Logger.error(TemplateAPIImpl.class, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}

	}

	public boolean delete(Template template, User user, boolean respectFrontendRoles) throws DotSecurityException,
			Exception {
		if(permissionAPI.doesUserHavePermission(template, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			return deleteAsset(template);
		} else {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
	}

	public Template findWorkingTemplate(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Template t = (Template) APILocator.getVersionableAPI().findWorkingVersion(id, user, false);
		if(t!=null && InodeUtils.isSet(t.getInode())){
			if (!permissionAPI.doesUserHavePermission(t, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
				throw new DotSecurityException("You don't have permission to read the source file.");
			}
		}
		return t;
	}

	public List<Template> findTemplates(User user, boolean includeArchived,
			Map<String, Object> params, String hostId, String inode, String identifier, String parent,
			int offset, int limit, String orderBy) throws DotSecurityException,
			DotDataException {
		return templateFactory.findTemplates(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit, orderBy);
	}

	public void associateContainers(List<Container> containerIdentifiers,Template template) throws DotHibernateException {
		templateFactory.associateContainers(containerIdentifiers, template);
	}


	public Template findLiveTemplate(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Template t = (Template) APILocator.getVersionableAPI().findLiveVersion(id, user, false);
		if(t!=null && InodeUtils.isSet(t.getInode())){
			if (!permissionAPI.doesUserHavePermission(t, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
				throw new DotSecurityException("You don't have permission to read the source file.");
			}
		}
		return t;
	}


	public String checkDependencies(String templateInode, User user, Boolean respectFrontendRoles) throws PortalException, SystemException, DotDataException, DotSecurityException {
		String result = null;
		Template template = (Template) InodeFactory.getInode(templateInode, Template.class);
		// checking if there are pages using this template
		List<HTMLPage> pages=APILocator.getTemplateAPI().getPagesUsingTemplate(template, user, respectFrontendRoles);

		if(pages.size()>0) {
			StringBuilder names=new StringBuilder();
			for(int i=0; i<pages.size(); i++) {
				names.append(pages.get(i).getURI()).append(", ");
			}
			result =  names.toString();
		}
		return result;
	}

    @Override
    public int deleteOldVersions(Date assetsOlderThan) throws DotStateException, DotDataException {
        return deleteOldVersions(assetsOlderThan,"template");
    }
}
