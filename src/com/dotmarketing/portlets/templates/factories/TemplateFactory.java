package com.dotmarketing.portlets.templates.factories;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * 
 * @author will, david (2005)
 */
public class TemplateFactory {

	public static File getImageFile(Template template) throws DotStateException, DotDataException, DotSecurityException {
		String imageIdentifierInode = template.getImage();
		Identifier identifier = new Identifier();
		try {
			identifier = APILocator.getIdentifierAPI().find(imageIdentifierInode);
		} catch (DotHibernateException e) {
			Logger.error(TemplateFactory.class,e.getMessage(),e);
		}
		File imageFile = new File();
		if(InodeUtils.isSet(identifier.getInode())){
			imageFile = (File) APILocator.getVersionableAPI().findWorkingVersion(identifier, APILocator.getUserAPI().getSystemUser(),false);
		}
		return imageFile;
	}

	@SuppressWarnings("unchecked")
	public static Template copyTemplate(Template currentTemplate) throws DotDataException, DotSecurityException {

		Template newTemplate = new Template();

		newTemplate.copy(currentTemplate);
		newTemplate.setImage(currentTemplate.getImage());
		newTemplate.setFriendlyName(currentTemplate.getFriendlyName()
				+ " (COPY) ");
		newTemplate.setTitle(currentTemplate.getTitle() + " (COPY) ");
		
		//Copy the host
		HostAPI hostAPI = APILocator.getHostAPI();
		Host h;
		try {
			h = hostAPI.findParentHost(currentTemplate, APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotSecurityException e) {
			Logger.error(TemplateFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} 
		// creates new identifier for this webasset and persists it
		Identifier newIdentifier = com.dotmarketing.business.APILocator.getIdentifierAPI().createNew(newTemplate, h);
		Logger.debug(TemplateFactory.class, "Parent newIdentifier="
				+ newIdentifier.getInode());
		
		newTemplate.setIdentifier(newIdentifier.getId());

		// persists the webasset
		HibernateUtil.saveOrUpdate(newTemplate);
		
		APILocator.getVersionableAPI().setWorking(newTemplate);

		// gets containers children (we attach identifier to templates instead
		// of the container inode)
		java.util.List<Container> children = getContainersInTemplate(currentTemplate);
		java.util.Set<Container> childrenSet = new java.util.HashSet<Container>();
	    childrenSet.addAll(children);
	    
	    APILocator.getTemplateAPI().associateContainers(children,newTemplate);
		
		/*for(Identifier id : children ){
			newTemplate.addChild(id);
		}*/
		
		//Copy the host again
		newIdentifier.setHostId(h.getIdentifier());

		// Copy permissions
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		perAPI.copyPermissions(currentTemplate, newTemplate);

		return newTemplate;
	}
	
	static List<Container> getContainersInTemplate(Template t) throws DotDataException, DotSecurityException{
		return APILocator.getTemplateAPI().getContainersInTemplate(t, APILocator.getUserAPI().getSystemUser(), false);
	}

}
