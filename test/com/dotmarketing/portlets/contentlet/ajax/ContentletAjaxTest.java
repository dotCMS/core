package com.dotmarketing.portlets.contentlet.ajax;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dotcms.repackage.junit_4_8_1.junit.framework.Assert;
import com.dotcms.repackage.junit_4_8_1.org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 *
 * @author oswaldogallango
 *
 */
public class ContentletAjaxTest {

	/**
	 * Test problem on "Content Search" when switching on one language 
	 * show all the contentlets 
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 */
	@Test
	public void issue5330() throws DotDataException, DotSecurityException{
		/*
		 * Creating language
		 */
		Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
		Language lan = APILocator.getLanguageAPI().getLanguage(102);
		if(!UtilMethods.isSet(lan) || lan.getId() == 0){
			lan = new Language();
			lan.setCountry("Italy");
			lan.setCountryCode("IT");
			lan.setLanguageCode("it");
			lan.setLanguage("Italian");
			APILocator.getLanguageAPI().saveLanguage(lan);
			/*
			 * changin id to recreate possible match between languages
			 */
			DotConnect dc = new DotConnect();
			dc.setSQL("update language set id="+defaultLang.getId()+"02 where language_code='it'");
			dc.loadResult();
			
			lan = APILocator.getLanguageAPI().getLanguage("it", "IT");
		}

		/*
		 * Creating multilanguage contententlet
		 */
		User systemUser = APILocator.getUserAPI().getSystemUser();
		Host host = APILocator.getHostAPI().findDefaultHost(systemUser, false);
		
		Structure structure = StructureCache.getStructureByVelocityVarName("webPageContent");
		Contentlet contentlet1 = new Contentlet();
		contentlet1.setStructureInode(structure.getInode());
		contentlet1.setHost(host.getIdentifier());
		contentlet1.setLanguageId(defaultLang.getId());
		String title = "testIssue5330"+UtilMethods.dateToHTMLDate(new Date(),"MMddyyyyHHmmss");
		contentlet1.setStringProperty("title", title);
		contentlet1.setStringProperty("body", "testIssue5330");
		contentlet1.setHost(host.getIdentifier());

		contentlet1 = APILocator.getContentletAPI().checkin(contentlet1, systemUser,false);
		if(APILocator.getPermissionAPI().doesUserHavePermission(contentlet1, PermissionAPI.PERMISSION_PUBLISH, systemUser))
			APILocator.getVersionableAPI().setLive(contentlet1);

		String ident = contentlet1.getIdentifier();
		contentlet1 = APILocator.getContentletAPI().findContentletByIdentifier(ident, true, defaultLang.getId(), systemUser, false);
		contentlet1.setLanguageId(lan.getId());
		contentlet1.setStringProperty("body", "italianTestIssue5330");
		contentlet1.setInode("");

		contentlet1 = APILocator.getContentletAPI().checkin(contentlet1, systemUser,false);
		if(APILocator.getPermissionAPI().doesUserHavePermission(contentlet1, PermissionAPI.PERMISSION_PUBLISH, systemUser))
			APILocator.getVersionableAPI().setLive(contentlet1);

		/*
		 * Validate that there are two contentlets associated to the same identifier wit different languages
		 */
		List<Contentlet> contList = APILocator.getContentletAPI().getSiblings(ident);
		Assert.assertTrue(contList.size()==2);

		List<String> fieldsValues = new ArrayList<String>();
		fieldsValues.add("conHost");
		fieldsValues.add(host.getIdentifier());
		fieldsValues.add("webPageContent.title");
		fieldsValues.add(title);
		fieldsValues.add("languageId");
		fieldsValues.add(String.valueOf(defaultLang.getId()));

		List<Object> results=new ContentletAjax().searchContentlets (structure.getInode(), fieldsValues, new ArrayList<String>(), false, false, false, false, 1, "modDate Desc", null, null);
		Assert.assertTrue(contList.size()==2);
		Assert.assertTrue(contList.size()==2);

		fieldsValues = new ArrayList<String>();
		fieldsValues.add("conHost");
		fieldsValues.add(host.getIdentifier());
		fieldsValues.add("webPageContent.title");
		fieldsValues.add(title);
		fieldsValues.add("languageId");
		fieldsValues.add(String.valueOf(lan.getId()));

		results=new ContentletAjax().searchContentlets (structure.getInode(), fieldsValues, new ArrayList<String>(), false, false, false, false, 1, "modDate Desc", null, null);
		Assert.assertTrue(contList.size()==2);
		Assert.assertTrue(contList.size()==2);
	}

}
