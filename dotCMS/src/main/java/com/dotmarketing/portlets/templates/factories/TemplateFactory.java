package com.dotmarketing.portlets.templates.factories;

import java.util.List;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

/**
 *
 * @author will, david (2005)
 */
@Deprecated
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

	public static Contentlet getImageContentlet(Template template) throws DotStateException, DotDataException, DotSecurityException {
		String imageIdentifierInode = template.getImage();
		Identifier identifier = new Identifier();
		try {
			identifier = APILocator.getIdentifierAPI().find(imageIdentifierInode);
		} catch (DotHibernateException e) {
			Logger.error(TemplateFactory.class,e.getMessage(),e);
		}
		Contentlet imageContentlet = new Contentlet();
		if(InodeUtils.isSet(identifier.getInode())){
			imageContentlet = APILocator.getContentletAPI().findContentletByIdentifier(identifier.getId(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);
		}
		return imageContentlet;
	}

	

	static List<Container> getContainersInTemplate(Template t) throws DotDataException, DotSecurityException{
		return APILocator.getTemplateAPI().getContainersInTemplate(t, APILocator.getUserAPI().getSystemUser(), false);
	}

}
