package com.dotmarketing.portlets.templates.ajax;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.factories.TemplateFactory;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.templates.model.TemplateWrapper;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

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
		List<Template> totalTemplates = new ArrayList<Template>();
		Host host = hostAPI.find(query.get("hostId"), user, respectFrontendRoles);

		try{
			String filter = query.get("fullTitle");
			if(UtilMethods.isSet(filter)){
				filter = filter.replaceAll("\\*", "");
				filter = filter.replaceAll("\\?", "");
			}

			if(UtilMethods.isSet(query.get("hostId"))) {
			    int startF=start;
			    int countF=count;
			    if(start==0){
                    Template t = new Template();
                    t.setOwner(user.getUserId());
                    t.setModUser(user.getUserId());
                    t.setInode("0");
                    t.setTitle("--- " + LanguageUtil.get(user, "All-Hosts") +" ---");
                    t.setIdentifier("0");
                    fullListTemplates.add(t);
                    totalTemplates.add(t);
                    countF=count-1;

					if (UtilMethods.isSet(query.get("templateSelected"))) {
						final Template templateSelected = templateAPI.findWorkingTemplate(query.get("templateSelected"),
								APILocator.getUserAPI().getSystemUser(), respectFrontendRoles);

						if (UtilMethods.isSet(templateSelected) && templateSelected.isAnonymous()) {
							fullListTemplates.add(templateSelected);
							totalTemplates.add(templateSelected);
						}
					}
				}
				else {
					startF=start-1;
				}

				fullListTemplates.addAll(templateAPI.findTemplatesUserCanUse(user, host.getIdentifier(), filter, true, startF, countF));
				totalTemplates.addAll(templateAPI.findTemplatesUserCanUse(user, host.getIdentifier(), filter, true, 0, 1000));

			}

			//doesn't currently respect archived
			if(fullListTemplates.size() == 0){
				fullListTemplates.addAll(templateAPI.findTemplatesUserCanUse(user,"", filter,true, start, start>0?count:count+1));
				totalTemplates.addAll(templateAPI.findTemplatesUserCanUse(user,"", filter,true, 0, 1000));
			}


		}catch (Exception e) {
			throw new DotRuntimeException(e.getMessage(), e);
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

		results.put("totalResults", totalTemplates.size());

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
		final Map<String, Object> templateMap = template.getMap();

		if (template.isAnonymous()){
			try {
				final String customLayoutTemplateLabel = LanguageUtil.get("editpage.properties.template.custom.label");
				templateMap.put("fullTitle", customLayoutTemplateLabel);
				templateMap.put("htmlTitle", customLayoutTemplateLabel);
			} catch (LanguageException e) {
				Logger.error(this.getClass(),
						"Exception on buildTemplateMap exception message: " + e.getMessage(), e);
			}
		} else if(parentHost != null) {
			templateMap.put("hostName", parentHost.getHostname());
			templateMap.put("hostId", parentHost.getIdentifier());
			templateMap.put("fullTitle",   templateMap.get("title") + " (" + parentHost.getHostname() + ")" );
			templateMap.put("htmlTitle",  "<div><div style='float:left'>" + templateMap.get("title") + "</div><div style='float:right'>" + parentHost.getHostname() + "</div></div>" );
		} else {
			templateMap.put("fullTitle", templateMap.get("title"));
			templateMap.put("htmlTitle", templateMap.get("title"));
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
			Identifier imageIdentifier = APILocator.getIdentifierAPI().find(template.getImage());
			if(UtilMethods.isSet(imageIdentifier.getAssetType()) && imageIdentifier.getAssetType().equals("contentlet")) {
				Contentlet imageContentlet = TemplateFactory.getImageContentlet(template);
				if(imageContentlet!=null){
					toReturn.put("inode", imageContentlet.getInode());
					toReturn.put("name", imageContentlet.getTitle());
					toReturn.put("identifier", imageContentlet.getIdentifier());
					toReturn.put("extension", com.dotmarketing.util.UtilMethods.getFileExtension(imageContentlet.getTitle()));
				}
			}
		}
		return toReturn;
	}

	public String checkDependencies(String templateInode) throws DotDataException, DotSecurityException, PortalException, SystemException{
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = userWebAPI.getLoggedInUser(req);
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(req);
		return templateAPI.checkDependencies(templateInode, user, respectFrontendRoles);
	}

    /**
     * Method that will verify if a given template title is already used by another template
     *
     * @param title          template title to verify
     * @param templateInode  template inode in case we are editing a template, null or empty in case of a new template
     * @param hostIdentifier current host identifier
     * @return
     * @throws DotDataException
     * @throws SystemException
     * @throws PortalException
     * @throws DotSecurityException
     */
    public boolean duplicatedTitle ( String title, String templateInode, String hostIdentifier ) throws DotDataException, SystemException, PortalException, DotSecurityException {

        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = userWebAPI.getLoggedInUser( req );
        boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend( req );

        //Getting the current host
        Host host = hostAPI.find( hostIdentifier, user, respectFrontendRoles );

        //The template name must be unique
        Template foundTemplate = FactoryLocator.getTemplateFactory().findWorkingTemplateByName( title, host );
        boolean duplicatedTitle = false;
        if ( foundTemplate != null && InodeUtils.isSet( foundTemplate.getInode() ) ) {
            if ( !UtilMethods.isSet( templateInode ) ) {
                duplicatedTitle = true;
            } else {
                if ( !foundTemplate.getInode().equals( templateInode ) ) {
                    duplicatedTitle = true;
                }
            }
        }

        return duplicatedTitle;
    }

}