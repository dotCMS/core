package com.dotmarketing.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public class UtilHTML {

	private static CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	private static LanguageAPI langAPI = APILocator.getLanguageAPI();
	private static IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
	private static HostAPI hostAPI = APILocator.getHostAPI();

	public static String getStatusIcons(Versionable v) throws DotStateException, DotDataException, DotSecurityException{
		StringBuffer buf = new StringBuffer();

		if(v.isWorking() && !v.isArchived() && !v.isLive()) {
		    buf.append("<span class='workingIcon'></span>");
		}

		if(v.isArchived()){
		    buf.append("<span class='greyDotIcon' style='opacity:.4'></span>");
		    buf.append("<span class='archivedIcon'></span>");
		} else {
		    if(v.isLive() && v.isWorking()) {
		        buf.append("<span class='greyDotIcon' style='opacity:.4'></span>");
		        buf.append("<span class='liveIcon'></span>");
		    } else if(APILocator.getVersionableAPI().hasLiveVersion(v)) {
		        buf.append("<span class='liveIcon'></span>");
		    } else {
		        buf.append("<span class='greyDotIcon' style='opacity:.4'></span>");
		    }
		}

		if(v.isLocked()){
			buf.append("<span class='lockIcon'></span>");
		}
		return buf.toString();
	}
	
	public static String getVersionStatusIcons(Versionable v) throws DotStateException, DotDataException, DotSecurityException{
		StringBuffer buf = new StringBuffer();
		if(v.isWorking()){
			buf.append("<span class='workingIcon'></span>");
		}
		else{
			buf.append("<span class='greyDotIcon' style='opacity:.2'></span>");
		}
		if(v.isLive()){
			buf.append("<span class='liveIcon'></span>");
		}
		else if(v.isArchived()){
	            buf.append("<span class='archivedIcon'></span>");
	    }
		else{
			buf.append("<span class='greyDotIcon' style='opacity:.2'></span>");
		}


		return buf.toString();
	}
	

	public static String getSelectCategories(Inode parent, int level, Inode current, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		level++;
		Logger.debug(UtilHTML.class, "getSelectCategories!!!");

		List<Category> categories = (parent == null) ? categoryAPI.findTopLevelCategories(user, respectFrontendRoles) : categoryAPI
				.getChildren(parent, user, respectFrontendRoles);

		StringBuffer strHTML = new StringBuffer();
		Iterator<Category> m = categories.iterator();

		while (m.hasNext()) {
			Category cat = (Category) m.next();
			
			boolean canUse = categoryAPI.canUseCategory(cat, user, respectFrontendRoles);
			if(canUse){
				if (level == 1) {
					strHTML.append("<option value=\"" + cat.getInode() + "\" class='topCat' ");
				} else {
					strHTML.append("<option value=\"" + cat.getInode() + "\"");
				}
	
				if (current != null) {
					Iterator<Category> l = categoryAPI.getParents(current, user, respectFrontendRoles).iterator();
	
					while (l.hasNext()) {
						Category disCat = (Category) l.next();
	
						if (cat.getInode().equalsIgnoreCase(disCat.getInode())) {
							strHTML.append(" selected");
						}
					}
				}
	
				strHTML.append(">");
	
				for (int k = 0; k < (level - 1); k++) {
					strHTML.append(" &nbsp; &nbsp;");
				}
	
				strHTML.append("+&nbsp;");
				strHTML.append(cat.getCategoryName() + "</option>\n");
	
				if (categoryAPI.getChildren(cat, user, respectFrontendRoles).size() > 0) {
					strHTML.append(getSelectCategories(cat, level, current, user, respectFrontendRoles));
				}
			}
		}
		return strHTML.toString();
	}



	public static String getSelectLanguages(Contentlet contentlet) {
		List<Language> languages = langAPI.getLanguages();

		StringBuffer strHTML = new StringBuffer();
		Iterator<Language> m = languages.iterator();

		while (m.hasNext()) {
			Language language = (Language) m.next();

			strHTML.append("<option value=\"" + language.getId() + "\"");

			if (language.getId() == contentlet.getLanguageId()) {
				strHTML.append(" selected");
			}

			strHTML.append(">");

			strHTML.append(language.getLanguage() + " (" + language.getCountry() + ")</option>\n");
		}

		return strHTML.toString();
	}

	public static String getSelectCategories(Inode parent, int level, String currentCats, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		CategoryAPI catAPI = APILocator.getCategoryAPI();
		
		level++;
		Logger.debug(UtilHTML.class, "getSelectCategories!!!");

		List<Category> categories = (parent == null) ? categoryAPI.findTopLevelCategories(user, respectFrontendRoles) : categoryAPI
				.getChildren(parent, user, respectFrontendRoles);

		StringBuffer strHTML = new StringBuffer();
		Iterator<Category> m = categories.iterator();

		while (m.hasNext()) {
			Category cat = (Category) m.next();

			boolean canUse = catAPI.canUseCategory(cat, user, respectFrontendRoles);
			if(canUse){				
				if (level == 1) {
					strHTML.append("<option value=\"" + cat.getInode() + "\" class='topCat' ");
				} else {
					strHTML.append("<option value=\"" + cat.getInode() + "\"");
				}
	
				if (currentCats.indexOf(cat.getInode() + ",") != -1) {
					Logger.debug(UtilHTML.class, "found the same objects!!!!");
					strHTML.append(" selected");
				}
	
				strHTML.append(">");
	
				for (int k = 0; k < (level - 1); k++) {
					strHTML.append(" &nbsp; &nbsp;");
				}
	
				if (level != 0)
					strHTML.append("+&nbsp;");
				strHTML.append(cat.getCategoryName() + "</option>\n");
	
				if (categoryAPI.getChildren(cat, user, respectFrontendRoles).size() > 0) {
					strHTML.append(getSelectCategories(cat, level, currentCats, user, respectFrontendRoles));
				}
			}
		}

		return strHTML.toString();
	}

	public static String getAssetIcon(WebAsset webasset) {
		String imgSrc = "";

		if (webasset instanceof Container) {
			imgSrc = "/html/images/icons/layout-select-content.png";
		} else if (webasset instanceof Template) {
			imgSrc = "/html/images/icons/layout-header-3-mix.png";
		} else if (webasset instanceof Link) {
			imgSrc = "/html/images/icons/chain.png";
		}

		return "<img src=\"" + imgSrc + "\" width=16 height=16 />";
	}

	public static String getAssetIcon(Contentlet con){
		return "<img src=\"/html/images/icons/document-text-image.png\" width=16 height=16 />";
	}
	
	
	
	
	public static String getSelectCategoriesTextMode(Inode parent, int level, String currentCats,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		
		CategoryAPI catAPI = APILocator.getCategoryAPI();
		
		level++;
		Logger.debug(UtilHTML.class, "getSelectCategories!!!");

		List<Category> categories = (parent == null) ? categoryAPI.findTopLevelCategories(user, respectFrontendRoles) : 
			categoryAPI.getChildren(parent, user, respectFrontendRoles);

		StringBuffer strHTML = new StringBuffer();
		Iterator<Category> m = categories.iterator();

		while (m.hasNext()) {
			Category cat = (Category) m.next();
			boolean canUse = catAPI.canUseCategory(cat, user, respectFrontendRoles);
			if(canUse){
				if (currentCats.indexOf(cat.getInode() + ",") != -1) {
					Logger.debug(UtilHTML.class, "found the same objects!!!!");
					strHTML.append("<tr><td>");
					strHTML.append(cat.getCategoryName());
					strHTML.append("</td></tr>");
				}
				if (categoryAPI.getChildren(cat, user, respectFrontendRoles).size() > 0) {
					strHTML.append(getSelectCategoriesTextMode(cat, level, currentCats, user, respectFrontendRoles));
				}
			}
		}
		return strHTML.toString();
	}

	public static String getSelectCategories(Inode parent, int level, String[] currentCats, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		StringBuffer buffer = new StringBuffer();
		if (currentCats != null) {
			for (int i = 0; i < currentCats.length; i++) {
				buffer.append(currentCats[i] + ",");
			}
		}
		return getSelectCategories(parent, level, buffer.toString(), user, respectFrontendRoles);
	}

	public static String getSelectCategoriesTextMode(Inode parent, int level, String[] currentCats,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		StringBuffer buffer = new StringBuffer();
		if (currentCats != null) {
			for (int i = 0; i < currentCats.length; i++) {
				buffer.append(currentCats[i] + ",");
			}
		}
		return getSelectCategoriesTextMode(parent, level, buffer.toString(), user, respectFrontendRoles);
	}

	public static List<Template> getSelectTemplates(Host host, User user) {
		List<Template> result = new ArrayList<Template>();
		String condition = "working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " and deleted=" + com.dotmarketing.db.DbConnectionFactory.getDBFalse();

		// Gets roles
		try {
			List<WebAsset> templates = WebAssetFactory.getAssetsWorkingWithPermission(Template.class, 100000, 0, "title", null, user);
			
			Iterator<WebAsset> templatesIter = templates.iterator();
			Template template;
			
			if (host == null) {
				while (templatesIter.hasNext()) {
					template = (Template) templatesIter.next();
					result.add(template);
				}
			} else {
				Identifier identifier;
				Host systemHost = hostAPI.findSystemHost(user, false);
				
				while (templatesIter.hasNext()) {
					template = (Template) templatesIter.next();
					identifier = identifierAPI.findFromInode(template.getIdentifier());
					if (identifier.getHostId().equals(host.getIdentifier()) || !UtilMethods.isSet(identifier.getHostId()) || ((systemHost != null) && identifier.getHostId().equals(systemHost.getIdentifier()))) {
						result.add(template);
					}
				}
			}
		} catch (Exception e) {
		}

		return result;
	}

	public static String getSelectCategories(Inode current, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getSelectCategories(null, 0, current, user, respectFrontendRoles);
	}

	public static String getSelectCategories(String x, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Category current = categoryAPI.find(x, user, respectFrontendRoles);

		return getSelectCategories(null, 0, current, user, respectFrontendRoles);
	}

	public static String getSelectCategoriesByParent(Inode parent, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Inode current = new Inode();
		Category parentCat = (Category) parent;
		StringBuffer strHTML = new StringBuffer();
		strHTML.append("<option value=\"" + parent.getInode() + "\" class='topCat'>");
		strHTML.append(parentCat.getCategoryName() + "</option>\n");
		strHTML.append(getSelectCategories(parent, 1, current, user, respectFrontendRoles));

		return strHTML.toString();
	}

	public static String getSelectCategories(Inode parent, int level, Inode current, int maxlevel, 
			User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		ArrayList idList = new ArrayList();
		if (current instanceof Category) {
			idList.add(current.getInode());
		} else {
			List cats = InodeFactory.getParentsOfClass(current, Category.class);
			Iterator it = cats.iterator();
			while (it.hasNext()) {
				Category cat = (Category) it.next();
				idList.add(cat.getInode());
			}
		}
		String[] catsArr = (String[]) idList.toArray(new String[0]);
		return getSelectCategories(parent, level, catsArr, maxlevel, user, respectFrontendRoles);
	}

	public static String getSelectCategories(Inode parent, int level, String[] selectedCategoriesIds, int maxlevel, 
			User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		level++;

		List categories = (parent == null) ? categoryAPI.findTopLevelCategories(user, respectFrontendRoles) : 
			categoryAPI.getChildren(parent, user, respectFrontendRoles);

		StringBuffer strHTML = new StringBuffer();
		Iterator m = categories.iterator();

		while (m.hasNext()) {
			Category cat = (Category) m.next();

			if (level == 1) {
				strHTML.append("<option value=\"" + cat.getInode() + "\" class=\"topCat\" ");
			} else {
				strHTML.append("<option value=\"" + cat.getInode() + "\"");
			}

			if (selectedCategoriesIds != null) {
				for (int i = 0; i < selectedCategoriesIds.length; i++) {
					String id = selectedCategoriesIds[i];
					if (cat.getInode().equalsIgnoreCase(id)) {
						strHTML.append(" selected");

					}
				}
			}

			strHTML.append(">");

			for (int k = 0; k < (level - 1); k++) {
				strHTML.append(" &nbsp; &nbsp;");
			}

			strHTML.append("+&nbsp;");
			strHTML.append(cat.getCategoryName() + "</option>");

			if (level <= maxlevel && categoryAPI.getChildren(cat, user, respectFrontendRoles).size() > 0) {
				strHTML.append(getSelectCategories(cat, level, selectedCategoriesIds, maxlevel, user, respectFrontendRoles));
			}
		}

		return strHTML.toString();
	}
	
	public static String getSelectCategories(Inode parent, int level, Identifier current, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		level++;
		Logger.debug(UtilHTML.class, "getSelectCategories!!!");

		List<Category> categories = (parent == null) ? categoryAPI.findTopLevelCategories(user, respectFrontendRoles) : categoryAPI
				.getChildren(parent, user, respectFrontendRoles);

		StringBuffer strHTML = new StringBuffer();
		Iterator<Category> m = categories.iterator();

		while (m.hasNext()) {
			Category cat = (Category) m.next();
			
			boolean canUse = categoryAPI.canUseCategory(cat, user, respectFrontendRoles);
			if(canUse){
				if (level == 1) {
					strHTML.append("<option value=\"" + cat.getInode() + "\" class='topCat' ");
				} else {
					strHTML.append("<option value=\"" + cat.getInode() + "\"");
				}
	
				if (current != null) {
					Iterator<Category> l = categoryAPI.getParents(current, user, respectFrontendRoles).iterator();
	
					while (l.hasNext()) {
						Category disCat = (Category) l.next();
	
						if (cat.getInode().equalsIgnoreCase(disCat.getInode())) {
							strHTML.append(" selected");
						}
					}
				}
	
				strHTML.append(">");
	
				for (int k = 0; k < (level - 1); k++) {
					strHTML.append(" &nbsp; &nbsp;");
				}
	
				strHTML.append("+&nbsp;");
				strHTML.append(cat.getCategoryName() + "</option>\n");
	
				if (categoryAPI.getChildren(cat, user, respectFrontendRoles).size() > 0) {
					strHTML.append(getSelectCategories(cat, level, current, user, respectFrontendRoles));
				}
			}
		}
		return strHTML.toString();
	}
	
	public static String htmlEncode(String data)
	{
		final StringBuffer buf = new StringBuffer();
		if(UtilMethods.isSet(data))
		{				
			final char[] chars = data.toCharArray();
			for (int i = 0; i < chars.length; i++) 
			{ 
				if (chars[i] == ';')
					buf.append(";");
				else
					buf.append("&#" + (int) chars[i]+";");
			}  
		}
		return buf.toString();
	}
	
	public static String escapeHTMLSpecialChars (String data){
		return UtilMethods.escapeHTMLSpecialChars (data);
	}
	
	/**
	 * This method escape the double quotes into html representacion
	 * @param fixme The text where the double quates exists
	 * @return String
	 * @author Oswaldo Gallango
	 * @version 1.0
	 */
	public static String escapeDoubleQuotesToHTMLEspecialChars(String fixme) {
		fixme = fixme.replaceAll("\"", "&quot;");
		return fixme;
	}

	public static CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public static void setCategoryAPI(CategoryAPI categoryAPI) {
		UtilHTML.categoryAPI = categoryAPI;
	}
}
