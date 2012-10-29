package com.dotmarketing.viewtools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.StaticMenuBuilder;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.dotmarketing.util.AssetsComparator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class NavigationWebAPI implements ViewTool {

	private static String MENU_VTL_PATH;
	private static String SHORT_MENU_VTL_PATH;
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private HttpServletRequest request;
    private User user = null;
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

    public int formCount = 0;

	static {
		String velocityRootPath = ConfigUtils.getDynamicVelocityPath() + java.io.File.separator;
		MENU_VTL_PATH = velocityRootPath + "menus" + java.io.File.separator;
		SHORT_MENU_VTL_PATH = ConfigUtils.getDynamicContentPath()+java.io.File.separator+"velocity"+java.io.File.separator;
	}

	private String loadHomeTitle(HttpServletRequest request){
		boolean multilingual = request.getAttribute("dot_multilingual_navigation") != null;
		if(multilingual) {
			return "$languagewebapi.get('home')";
		} else {
			return "Home";
		}
	}

	/**
	  * Return the htmlcode with the crumbtrail
	  * @param		request HttpServletRequest.
	  * @param		imgPath String.
	  * @return		String.
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	  * @exception	JspException.
	  * @exception	DotSecurityException.
	  */
	public String crumbTrail(HttpServletRequest request, String imgPath) throws JspException, DotSecurityException, PortalException, SystemException, DotDataException {
		return crumbTrail(request, imgPath, null);
	}

	/**
	  * Return the htmlcode with the crumbtrail
	  * @param		request HttpServletRequest.
	  * @param		homePath String.
	  * @param		imgPath String.
	  * @return		String.
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	  * @exception	JspException.
	  * @exception	DotSecurityException.
	  */
	public String crumbTrail(HttpServletRequest request, String imgPath, String homePath) throws JspException, DotSecurityException, PortalException, SystemException, DotDataException {
		return crumbTrail(request, imgPath, homePath, null);
	}

	/**
	  * Return the htmlcode with the crumbtrail
	  * @param		request HttpServletRequest.
	  * @param		imgPath String.
	  * @param		homePath String.
	  * @param		crumbTitle String.
	  * @return		String.
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	  * @exception	JspException.
	  * @exception	DotSecurityException.
	  */
	public String crumbTrail(HttpServletRequest request, String imgPath, String homePath, String crumbTitle) throws JspException, DotSecurityException, PortalException, SystemException, DotDataException {

		String homeTitle = loadHomeTitle(request);

		StringBuffer stringbuf = new StringBuffer();

		HashSet<String> listItems = new HashSet<String>();

		String path = request.getRequestURI();

		Logger.debug(NavigationWebAPI.class, "\n\n");
		Logger.debug(NavigationWebAPI.class, "CrumbTrailBuilderTag pagePath=" + path);

		//Opening the crumbtrail ul/li code
		stringbuf.append("<ul>");

		if(UtilMethods.isSet(homePath)){
			//Setting the home page trail

			stringbuf.append("<li><a href=\"" + UtilMethods.encodeURIComponent(homePath) + "\">" + homeTitle + "</a>");
			if(UtilMethods.isSet(imgPath))
				stringbuf.append("<img src=\""+UtilMethods.encodeURIComponent(imgPath)+"\" alt=\""+UtilMethods.escapeHTMLSpecialChars("ct_img")+"\" />");
			stringbuf.append("</li>");
			listItems.add(homeTitle);
		}else if (!(path.startsWith("/home"))){
			//Setting the home page trail
			stringbuf.append("<li><a href=\"/\">" + homeTitle + "</a>");
			if(UtilMethods.isSet(imgPath))
				stringbuf.append("<img src=\""+UtilMethods.encodeURIComponent(imgPath)+"\" alt=\""+UtilMethods.escapeHTMLSpecialChars("ct_img")+"\" />");
			stringbuf.append("</li>");
			listItems.add(homeTitle);
		}
		String openPath = "";
		int idx1 = 0;

		//Iterating through the full path to build the folder trails and the page trail at the end
		boolean end = false;
		do {
			idx1 = path.indexOf("/", idx1 + 1);
			if (idx1 == -1) {
				idx1 = path.length() - 1;
				end = true;
			}
			openPath = path.substring(0, idx1 + 1);
			getTrail(stringbuf, openPath, path, imgPath, crumbTitle, request);
		} while (!end);

		stringbuf.append("</ul>");

		return stringbuf.toString();
	}

	/**
	  * Return the htmlcode with the trail
	  * @param		stringbuffer StringBuffer.
	  * @param		openPath String.
	  * @param		fullPath String.
	  * @param		imgPath String.
	  * @param		crumbTitle String.
	  * @param		request HttpServletRequest.
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	  * @exception	DotSecurityException.
	  */
	private void getTrail(StringBuffer stringbuffer, String openPath, String fullPath, String imgPath, String crumbTitle, HttpServletRequest request) throws DotSecurityException, PortalException, SystemException, DotDataException {

		boolean multilingual = request.getAttribute("dot_multilingual_navigation") != null;

		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);

		//Checking if it's the end of the url and we are requesting a page
		if (openPath.equals(fullPath) && openPath.endsWith("." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"))) {
			getPageTrail(stringbuffer, fullPath, crumbTitle, request);
		} else {
			Folder folder = APILocator.getFolderAPI().findFolderByPath(openPath, host, user, true);

			String tempPath = openPath + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION");

			if ((tempPath.equals(fullPath)) ||
				(!UtilMethods.isSet(LiveCache.getPathFromCache(tempPath, host.getIdentifier()))))
				return;

			stringbuffer.append("<li><a href=\"" + UtilMethods.encodeURIComponent(APILocator.getIdentifierAPI().find(folder).getPath()) + "\">");
				if(multilingual && (folder.getTitle().contains("glossary.get")||folder.getTitle().contains("text.get")))
					stringbuffer.append(UtilHTML.escapeHTMLSpecialChars(folder.getTitle())+ "</a>");
				else
					stringbuffer.append(UtilHTML.escapeHTMLSpecialChars(multilingual?"$languagewebapi.get('" + folder.getTitle() + "')":folder.getTitle()) + "</a>");
			//if it's not the last item we should include an image separator
			if (!openPath.equals(fullPath) && UtilMethods.isSet(imgPath))
					stringbuffer.append("<img src=\""+UtilMethods.encodeURIComponent(imgPath)+"\" alt=\""+UtilMethods.escapeHTMLSpecialChars("ct_img")+"\" />");
			stringbuffer.append("</li>");
		}

	}

	/**
	  * Return the htmlcode with the pagetrail
	  * @param		stringbuffer StringBuffer.
	  * @param		fullPath String.
	  * @param		crumbTitle String.
	  * @param		request HttpServletRequest.
	  * @return		StringBuffer
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	  */
	private StringBuffer getPageTrail(StringBuffer stringbuffer, String fullPath, String crumbTitle, HttpServletRequest request) throws PortalException, SystemException, DotDataException, DotSecurityException {
		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);

		boolean multilingual = request.getAttribute("dot_multilingual_navigation") != null;

		if (UtilMethods.isSet(crumbTitle)) {
			String title = multilingual?"$languagewebapi.get('" + crumbTitle + "')":crumbTitle;
			title = UtilHTML.escapeHTMLSpecialChars(title);
			stringbuffer.append("<li>" + title + "</li>");
		} else if (UtilMethods.isSet(request.getParameter("crumbTitle"))) {
			String title = multilingual?"$languagewebapi.get('" + request.getParameter("crumbTitle") + "')":request.getParameter("crumbTitle");
			title = UtilHTML.escapeHTMLSpecialChars(title);
			stringbuffer.append("<li>" + title + "</li>");
		} else if (UtilMethods.isSet(request.getParameter("inode")) || UtilMethods.isSet(request.getParameter("id"))) {

			Contentlet cont = new Contentlet();
			if(InodeUtils.isSet(request.getParameter("id"))) {
				try {
					Identifier id = APILocator.getIdentifierAPI().find(request.getParameter("id"));
					long languageId = 0;
					try{
						languageId = ((Language) request.getSession(false).getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE)).getId();
					}catch (Exception e) {
						languageId = langAPI.getDefaultLanguage().getId();
					}
					cont = conAPI.findContentletByIdentifier(id.getInode(), true,languageId , user, true);
				} catch (Exception e) { }
			} else {
				String inode = request.getParameter("inode");
		            try{
		            	cont = conAPI.find(inode, user, true);
		            }catch(Exception e){
		            	Logger.debug(this, "Unable to find Contentlet with inode " + inode, e);
		            }
			}
			String conTitle;
			try {
				conTitle = conAPI.getName(cont, APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.debug(this, "Unable to set contentlet title", e);
				conTitle = "";
			}
			if ((cont != null) && (conTitle != null)) {
				stringbuffer.append("<li>" + UtilHTML.escapeHTMLSpecialChars(conTitle) + "</li>");
			} else {
				String idInode = APILocator.getIdentifierAPI().find(host,fullPath).getInode();
				
				if (InodeUtils.isSet(idInode)) {
					stringbuffer.append("<li>$HTMLPAGE_TITLE</li>");
				}
			}

		} else {
			String idInode = APILocator.getIdentifierAPI().find(host,fullPath).getInode();
			String title = multilingual?"$languagewebapi.get($HTMLPAGE_TITLE)":"$HTMLPAGE_TITLE";
			if (InodeUtils.isSet(idInode)) {
				stringbuffer.append("<li>" + title + "</li>");
			}
		}
		return stringbuffer;
	}

	/**
	  * Return the htmlcode with the navigation menu
	  * @param		startFromPath String.
	  * @param		numberOfLevels int.
	  * @param		request HttpServletRequest.
	  * @return		String.
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	  * @exception	JspException.
	  */
	@SuppressWarnings("unchecked")
	public String createMenu(String startFromPath, int numberOfLevels, HttpServletRequest request) throws PortalException, SystemException, DotDataException, DotSecurityException
	{
		String currentPath = request.getRequestURI();
		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		String hostId = host.getIdentifier();
		StringBuffer stringbuf = new StringBuffer();

		//Variable used to discriminate the menu names based on the paramaters given to the macro
		String paramsValues = "";

		boolean addSpans = false;
		if(request.getAttribute("menu_spans") != null && (Boolean)request.getAttribute("menu_spans")){
			addSpans = true;
		}

		String firstItemClass = "";
		if(request.getAttribute("firstItemClass") != null){
			firstItemClass = " class=\""+(String)request.getAttribute("firstItemClass")+"_";
		}

		String lastItemClass = "";
		if(request.getAttribute("lastItemClass") != null ) {
			lastItemClass=" class=\""+(String)request.getAttribute("lastItemClass")+"_";
		}

		String menuIdPrefix = "";
		if(request.getAttribute("menuIdPrefix") != null ){
			menuIdPrefix=(String)request.getAttribute("menuIdPrefix")+"_";
		}


		paramsValues = ((Boolean)addSpans).toString() + firstItemClass.toString() + lastItemClass.toString() +
		menuIdPrefix.toString();

		try {

			Logger.debug(NavigationWebAPI.class, "\n\nNavigationWebAPI :: StaticMenuBuilder begins");
			Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder start path=" + startFromPath);
			Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder number of levels=" + numberOfLevels);

			if ((startFromPath == null) || (startFromPath.length() == 0)) {

				Logger.debug(NavigationWebAPI.class, "pagePath=" + currentPath);

				int idx1 = currentPath.indexOf("/");
				int idx2 = currentPath.indexOf("/", idx1 + 1);

				startFromPath = currentPath.substring(idx1, idx2 + 1);

				Logger.debug(NavigationWebAPI.class, "path=" + startFromPath);
			}

			Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder hostId=" + host.getIdentifier());

			java.util.List itemsList = new ArrayList();
			String folderPath = "";
			String fileName = "";
			boolean fileExists = true;

			java.io.File file = null;
			String menuId = "";
			if ("/".equals(startFromPath)) {
				fileName = hostId + "_levels_" + numberOfLevels + paramsValues.hashCode() + "_static.vtl";
				menuId = String.valueOf(hostId);
				file = new java.io.File(MENU_VTL_PATH + fileName);
				if (!file.exists() || file.length() == 0) {
					itemsList = APILocator.getFolderAPI().findSubFolders(host, true);
					folderPath = startFromPath;
					fileExists = false;
				}
			} else {
				Folder folder = APILocator.getFolderAPI().findFolderByPath(startFromPath, hostId, user, true);
				Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder folder=" + APILocator.getIdentifierAPI().find(folder).getPath());

				fileName = folder.getInode() + "_levels_" + numberOfLevels + paramsValues.hashCode() + "_static.vtl";
				menuId = String.valueOf(folder.getInode());
				file = new java.io.File(MENU_VTL_PATH + fileName);
				Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder file=" + MENU_VTL_PATH + fileName);

				if (!file.exists() || file.length() == 0) {
					file.createNewFile();
					itemsList = APILocator.getFolderAPI().findMenuItems(folder, user, true); 
					folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
					fileExists = false;
				}
			}

			Comparator comparator = new AssetsComparator(1);
			Collections.sort(itemsList, comparator);

			String filePath = "dynamic" + java.io.File.separator + "menus" + java.io.File.separator + fileName;
			if (fileExists) {
				return filePath;
			} else {

				if (itemsList.size() > 0) {

					stringbuf.append("#if($EDIT_MODE)");
					stringbuf.append("<form action=\"${directorURL}\" method=\"post\" name=\"form_menu_")
					                 .append(menuId).append("\" id=\"form_menu_").append(menuId).append("\">");
					stringbuf.append("<input type=\"hidden\" name=\"cmd\" value=\"orderMenu\">");
					stringbuf.append("<input type=\"hidden\" name=\"path\" value=\"").append(startFromPath).append("\">");
					stringbuf.append("<input type=\"hidden\" name=\"hostId\" value=\"").append(hostId).append("\">");
					stringbuf.append("<input type=\"hidden\" name=\"pagePath\" value=\"$VTLSERVLET_URI\">");
					stringbuf.append("<input type=\"hidden\" name=\"referer\" value=\"$VTLSERVLET_URI\">");

					stringbuf.append("<div class=\"dotMenuReorder\"><a href=\"javascript:parent.submitMenu('form_menu_").append(menuId).append("');\">Reorder Menu</a></div>");
					stringbuf.append("</form>");
					stringbuf.append("#end ");

					stringbuf.append("<ul>");

					// gets menu items for this folder
					Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder number of items=" + itemsList.size());

					// /FIRST LEVEL MENU ITEMS!!!!
					boolean isLastItem = false;
					boolean isFirstItem = true;
					int index = 0;
					for (Object itemChild : itemsList) {
						index++;
						if(index == itemsList.size()){
							isLastItem = true;
							isFirstItem = false;
						} else if(index > 1){
							isFirstItem = false;
						}

						String styleClass = " ";
						if(isFirstItem && !firstItemClass.equals("")){
							styleClass = firstItemClass + "1\"";
						} else if(isLastItem && !lastItemClass.equals("")){
							styleClass = lastItemClass + "1\"";
						}

						if (itemChild instanceof Folder) {

							Folder folderChild = (Folder) itemChild;

							// recursive method here

							stringbuf = buildSubFolderMenu(stringbuf, folderChild, numberOfLevels, 1, addSpans, isFirstItem,firstItemClass, isLastItem, lastItemClass, menuIdPrefix);

						} else if (itemChild instanceof Link) {
							Link link = (Link) itemChild;
							if(link.getLinkType().equals(LinkType.CODE.toString())) {
								stringbuf.append("$UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('").append(UtilMethods.espaceVariableForVelocity(link.getLinkCode())).append("'), $velocityContext)");
							} else {
								stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")");
								stringbuf.append("#if ($UtilMethods.inString($VTLSERVLET_DECODED_URI,\"").append(((Link) itemChild).getProtocal()).append(((Link) itemChild).getUrl()).append("\"))");
								stringbuf.append("<li class=\"active\"><a ").append(styleClass).append(" href=\"").append(((Link) itemChild).getProtocal())
								    .append(((Link) itemChild).getUrl()).append("\" target=\"").append(((Link) itemChild).getTarget()).append("\">");
								stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((Link) itemChild).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
								stringbuf.append(" #else ");
								stringbuf.append("<li><a ").append(styleClass).append(" href=\"").append(((Link) itemChild).getProtocal()).append(((Link) itemChild)
								    .getUrl()).append("\" target=\"").append(((Link) itemChild).getTarget()).append("\">");
								stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((Link) itemChild).getTitle()))
								    .append((addSpans?"</span>":"")).append("</a></li>");
								stringbuf.append("#end ");
							}
						} else if (itemChild instanceof HTMLPage) {
							/*if (((HTMLPage) itemChild).isWorking() && !((HTMLPage) itemChild).isDeleted()) {*/
							stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")");
							stringbuf.append("#if ($UtilMethods.inString($VTLSERVLET_DECODED_URI,\"").append(startFromPath).append(((HTMLPage) itemChild).getPageUrl()).append("\"))");
							stringbuf.append("<li class=\"active\"><a ").append(styleClass).append(" href=\"").append(UtilMethods.encodeURIComponent(folderPath + ((HTMLPage) itemChild).getPageUrl())).append("\">");
							stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((HTMLPage) itemChild).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
							stringbuf.append(" #else ");
							stringbuf.append("<li><a ").append(styleClass).append(" href=\"").append(UtilMethods.encodeURIComponent(folderPath + ((HTMLPage) itemChild).getPageUrl())).append("\"> ");
							stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((HTMLPage) itemChild).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
							stringbuf.append("#end ");
							/*}*/
						} else if (itemChild instanceof IFileAsset) {
							if (((IFileAsset) itemChild).isWorking() && !((IFileAsset) itemChild).isDeleted()) {
								stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")");
								stringbuf.append("#if ($UtilMethods.inString($VTLSERVLET_DECODED_URI,\"").append(startFromPath).append(((IFileAsset) itemChild).getFileName()).append("\"))");
								stringbuf.append("<li class=\"active\"><a ").append(styleClass).append(" href=\"").append(UtilMethods.encodeURIComponent(folderPath + ((IFileAsset) itemChild).getFileName())).append("\">");
								stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((IFileAsset) itemChild).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
								stringbuf.append(" #else ");
								stringbuf.append("<li><a ").append(styleClass).append(" href=\"").append(UtilMethods.encodeURIComponent(folderPath + ((IFileAsset) itemChild).getFileName())).append("\">");
								stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((IFileAsset) itemChild).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
								stringbuf.append(" #end ");
							}
						}
					}
					stringbuf.append("</ul>");

				}



				if (stringbuf.toString().getBytes().length > 0) {
					// Specifying explicitly a proper character set encoding
					FileOutputStream fo = new FileOutputStream(file);
					OutputStreamWriter out = new OutputStreamWriter(fo, UtilMethods.getCharsetConfiguration());
					out.write(stringbuf.toString());
					out.flush();
					out.close();
					fo.close();
				} else {
					Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: Error creating static menu!!!!!");
				}

				Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: End of StaticMenuBuilder" + filePath);

				return filePath;
			}
		} catch (Exception e) {
			// Clear the string buffer, and insert only the main hyperlink text
			// to it.
			// Ignore the embedded links.
			stringbuf.delete(0, stringbuf.length());
			Logger.error(NavigationWebAPI.class,e.getMessage(),e);

		}
		return "";
	}

	/**
	  * WebApi init method
	  * @param		obj Object.
	  */
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
        this.request = context.getRequest();
        HttpSession ses = request.getSession(false);
    	if (ses != null)
    		user = (User) ses.getAttribute(WebKeys.CMS_USER);

		java.io.File fileFolder = new java.io.File(MENU_VTL_PATH);
		if (!fileFolder.exists()) {
			fileFolder.mkdirs();
		}
	}

	/**
	  * Concatenate the submenu htmlcode to the menu htmlcode
	  * @param		stringbuf StringBuffer.
	  * @param		thisFolder Folder.
	  * @param		numberOfLevels int.
	  * @param		currentLevel int.
	  * @param		addSpans boolean.
	  * @param		isFirstItem boolean.
	  * @param		firstItemClass String.
	  * @param		isLastItem boolean.
	  * @param		lastItemClass String.
	  * @param		menuIdPrefix String.
	  * @return		StringBuffer
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotStateException 
	  */
	@SuppressWarnings("unchecked")
	private StringBuffer buildSubFolderMenu(StringBuffer stringbuf, Folder thisFolder, int numberOfLevels, int currentLevel, boolean addSpans, boolean isFirstItem, String firstItemClass, boolean isLastItem, String lastItemClass, String menuIdPrefix) throws DotStateException, DotDataException, DotSecurityException {
		String thisFolderPath = "";
		try {
			thisFolderPath = APILocator.getIdentifierAPI().find(thisFolder).getPath();
		} catch (Exception e1) {
			Logger.error(NavigationWebAPI.class,e1.getMessage(),e1);
		} 
		stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")");
		stringbuf.append("#if ($UtilMethods.inString($VTLSERVLET_DECODED_URI,\"").append(thisFolderPath).append("\") || ($UtilMethods.isSet($openAllLevels) && $openAllLevels == true))");
		stringbuf.append("<li class=\"active\" id=\"").append(menuIdPrefix+ thisFolder.getName()).append("\">");
		stringbuf.append(" #else ");
		stringbuf.append("<li id=\"").append(menuIdPrefix).append(thisFolder.getName()).append("\">");
		stringbuf.append(" #end ");
		// gets menu items for this folder
		java.util.List<Inode> itemsChildrenList2 = new ArrayList();
		try {
			itemsChildrenList2 = APILocator.getFolderAPI().findMenuItems(thisFolder, user, true);
		} catch (Exception e1) {
			Logger.error(NavigationWebAPI.class,e1.getMessage(),e1);
		} 

		// do we have any children?
		boolean nextLevelItems = (itemsChildrenList2.size() > 0 && currentLevel < numberOfLevels);

		String folderChildPath = thisFolderPath.substring(0, thisFolderPath.length() - 1);
		folderChildPath = folderChildPath.substring(0, folderChildPath.lastIndexOf("/"));

		stringbuf.append("<a ");
		if(isFirstItem && !firstItemClass.equals("")){
			stringbuf.append(firstItemClass).append(currentLevel).append("\"");
		}else if(isLastItem && !lastItemClass.equals("")){
		    stringbuf.append(lastItemClass).append(currentLevel).append("\"");
		}
		stringbuf.append(" href=\"").append(UtilMethods.encodeURIComponent(thisFolderPath)).append("\">");
		stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(thisFolder.getTitle())).append((addSpans?"</span>":""));
		stringbuf.append("</a>");

		if (currentLevel < numberOfLevels) {

			if (nextLevelItems) {
				stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")");
				stringbuf.append("#if ($UtilMethods.inString($VTLSERVLET_DECODED_URI,\"").append(thisFolderPath).append("\") || ($UtilMethods.isSet($openAllLevels) && $openAllLevels == true))");
				stringbuf.append("<ul>");
			}

			isLastItem = false;
			isFirstItem = true;
			int index = 0;

			for (Inode childChild2 : itemsChildrenList2) {

				index++;
				if(index == itemsChildrenList2.size()){
					isLastItem = true;
					isFirstItem = false;
				}else if(index > 1){
					isFirstItem = false;
				}

				String styleClass = " ";
				if(isFirstItem && !firstItemClass.equals("")){
					styleClass =firstItemClass+currentLevel+"\"";
				} else if(isLastItem && !lastItemClass.equals("")){
					styleClass = lastItemClass+currentLevel+"\"";
				}

				if (childChild2 instanceof Folder) {
					Folder folderChildChild2 = (Folder) childChild2;
					String path = "";
					try {
						path = APILocator.getIdentifierAPI().find(folderChildChild2).getPath();
					} catch (Exception e) {
						Logger.error(NavigationWebAPI.class,e.getMessage(),e);
					}

					Logger.debug(this, "folderChildChild2= " + folderChildChild2.getTitle() + " currentLevel=" + currentLevel + " numberOfLevels="
							+ numberOfLevels);
					if (currentLevel <= numberOfLevels) {
						stringbuf = buildSubFolderMenu(stringbuf, folderChildChild2, numberOfLevels, currentLevel + 1, addSpans,isFirstItem, firstItemClass, isLastItem, lastItemClass, menuIdPrefix);
					} else {

						stringbuf.append("<li><a href=\"").append(UtilMethods.encodeURIComponent(path)).append("index.").append(Config.getStringProperty("VELOCITY_PAGE_EXTENSION")).append("\">");
						stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(folderChildChild2.getTitle())).append((addSpans?"</span>":"")).append("</a></li>");

					}
				} else if (childChild2 instanceof Link) {
					if (((Link) childChild2).isWorking() && !((Link) childChild2).isDeleted()) {

						Link link = (Link) childChild2;
	                	if(link.getLinkType().equals(LinkType.CODE.toString())) {
		                    stringbuf.append("$UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('").append(UtilMethods.espaceVariableForVelocity(link.getLinkCode())).append("'), $velocityContext) ");
	                	} else {
	        				stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")");
							stringbuf.append("#if ($VTLSERVLET_DECODED_URI != '").append(((Link) childChild2).getProtocal()).append(((Link) childChild2).getUrl()).append("')");
							stringbuf.append("<li><a ").append(styleClass).append(" href=\"").append(((Link) childChild2).getProtocal()).append(((Link) childChild2).getUrl()).append("\" target=\"")
									.append(((Link) childChild2).getTarget()).append("\">");
							stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((Link) childChild2).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
							stringbuf.append(" #else ");
							stringbuf.append("<li class=\"active\"><a ").append(styleClass).append(" href=\"").append(((Link) childChild2).getProtocal()).append(((Link) childChild2).getUrl())
									.append("\" target=\"").append(((Link) childChild2).getTarget()).append("\">");
							stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((Link) childChild2).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
							stringbuf.append(" #end ");
	                	}

					}
				} else if (childChild2 instanceof HTMLPage) {
					if (((HTMLPage) childChild2).isWorking() && !((HTMLPage) childChild2).isDeleted()) {
        				stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")");
						stringbuf.append("#if ($VTLSERVLET_DECODED_URI != '" + thisFolderPath + ((HTMLPage) childChild2).getPageUrl() + "')");
						stringbuf.append("<li><a ").append(styleClass).append(" href=\"").append(UtilMethods.encodeURIComponent(thisFolderPath + ((HTMLPage) childChild2).getPageUrl())).append("\">");
						stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((HTMLPage) childChild2).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
						stringbuf.append(" #else ");
						stringbuf.append("<li class=\"active\"><a ").append(styleClass).append(" href=\"").append(UtilMethods.encodeURIComponent(thisFolderPath + ((HTMLPage) childChild2).getPageUrl())).append("\">");
						stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((HTMLPage) childChild2).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
						stringbuf.append(" #end ");
					}
				} else if (childChild2 instanceof IFileAsset) {
					if (((IFileAsset) childChild2).isWorking() && !((IFileAsset) childChild2).isDeleted()) {
        				stringbuf.append("#set ($VTLSERVLET_DECODED_URI=\"$UtilMethods.decodeURL($VTLSERVLET_URI)\")");
						stringbuf.append("#if ($VTLSERVLET_DECODED_URI != '").append(thisFolderPath).append(((IFileAsset) childChild2).getFileName()).append("')");
						stringbuf.append("<li><a ").append(styleClass).append(" href=\"").append(UtilMethods.encodeURIComponent(thisFolderPath + ((IFileAsset) childChild2).getFileName())).append("\">");
						stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((IFileAsset) childChild2).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
						stringbuf.append(" #else ");
						stringbuf.append("<li class=\"active\"><a ").append(styleClass).append(" href=\"").append(UtilMethods.encodeURIComponent(thisFolderPath + ((IFileAsset) childChild2).getFileName())).append("\">");
						stringbuf.append((addSpans?"<span>":"")).append(UtilHTML.escapeHTMLSpecialChars(((IFileAsset) childChild2).getTitle())).append((addSpans?"</span>":"")).append("</a></li>");
						stringbuf.append(" #end ");
					}
				}
			}
		}
		if (nextLevelItems) {
			stringbuf.append("</ul>");
			stringbuf.append(" #end ");
		}
		stringbuf.append("</li>");
		return stringbuf;
	}

	/**
	 * This code is to built the site map menu
	 * @param startFromLevel This is number of folders the map should start at from the path.
	 * @param numberOfLevels This is how many folders to drill inside of from the start depth.
	 * @param  path This is the path of the current folder
	 * @param request HttpServletRequest
	 * @return String
	 * @throws JspException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	 */
	public String createSiteMapMenu(int startFromLevel, int numberOfLevels, String path, HttpServletRequest request,boolean addHome) throws PortalException, SystemException, DotDataException, DotSecurityException
	{
		return createSiteMapMenu(startFromLevel, numberOfLevels, path, request, addHome, false);
	}

	/**
	 * This code is to built the site map menu
	 * @param startFromLevel This is number of folders the map should start at from the path.
	 * @param numberOfLevels This is how many folders to drill inside of from the start depth.
	 * @param  path This is the path of the current folder
	 * @param request HttpServletRequest
	 * @param reverseOrder return the list in reverse order
	 * @return String
	 * @throws JspException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	 */
	public String createSiteMapMenu(int startFromLevel, int numberOfLevels, String path, HttpServletRequest request,boolean addHome, boolean reverseOrder) throws PortalException, SystemException, DotDataException, DotSecurityException
	{
		String currentPath = path;
		String startFromPath = currentPath.trim();
		if(!startFromPath.endsWith("/"))
			startFromPath = startFromPath.trim()+"/";
		return createSiteMapMenu( startFromLevel,numberOfLevels, startFromPath,currentPath,addHome,request,reverseOrder);
	}

	/**
	 * This code is to built the site map menu
	 * @param startFromLevel This is number of folders the map should start at from the path.
	 * @param numberOfLevels This is how many folders to drill inside of from the start depth.
	 * @param request HttpServletRequest
	 * @return String
	 * @throws JspException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	 */
	public String createSiteMapMenu(int startFromLevel, int numberOfLevels, HttpServletRequest request) throws JspException, PortalException, SystemException, DotDataException, DotSecurityException

	{
		String currentPath = request.getRequestURI();
		String startFromPath = currentPath.trim();
		if(!startFromPath.endsWith("/"))
			startFromPath = startFromPath+"/";

		boolean addHome   = true;
		return createSiteMapMenu( startFromLevel,  numberOfLevels, startFromPath,currentPath,addHome,request,false);
	}

	/**
	 * This code is to built the site map menu
	 * @param startFromLevel This is number of folders the map should start at from the path.
	 * @param numberOfLevels This is how many folders to drill inside of from the start depth.
	 * @param startFromPath This is the path of the folder where the search start
	 * @param currentPath This is the path of the current folder
	 * @param addHome This said if include
	 * @param request HttpServletRequest
	 * @return String
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	 * @throws DotSecurityException 
	 */
	@SuppressWarnings("unchecked")
	private String createSiteMapMenu(int startFromLevel, int numberOfLevels,String startFromPath,String currentPath,boolean addHome,HttpServletRequest request,boolean reverseOrder) throws PortalException, SystemException, DotDataException, DotSecurityException
	{

		String siteMapIdPrefix = "";
		if(request.getAttribute("siteMapIdPrefix") != null ){
			siteMapIdPrefix=(String)request.getAttribute("siteMapIdPrefix")+"_";
		}

		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		String hostId = host.getIdentifier();
		StringBuffer stringbuf = new StringBuffer();
		FileOutputStream fo = null;

		int orderDirection = 1;
		if(reverseOrder){
			orderDirection = -1;
		}

		try {


			Logger.debug(StaticMenuBuilder.class, "\n\nStaticMenuBuilder begins");
			Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder start path=" + startFromPath);
			Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder number of levels=" + numberOfLevels);

			if ((startFromPath == null) || (startFromPath.length() == 0)) {

				Logger.debug(StaticMenuBuilder.class, "pagePath=" + currentPath);

				int idx1 = currentPath.indexOf("/");
				int idx2 = currentPath.indexOf("/", idx1 + 1);

				startFromPath = currentPath.substring(idx1, idx2 + 1);

				Logger.debug(StaticMenuBuilder.class, "path=" + startFromPath);
			}

			Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder hostId=" + host.getIdentifier());

			java.util.List itemsList = new ArrayList();
			String folderPath = "";
			String fileName = "";
			boolean fileExists = true;

			java.io.File file = null;
			String menuId = "";
			if ("/".equals(startFromPath)) {
				fileName = hostId + "_siteMapLevels_"+startFromLevel+"_" + numberOfLevels+"_"+reverseOrder+"_"+addHome + "_" + siteMapIdPrefix + "_static.vtl";
				menuId = String.valueOf(hostId);
				//file = new java.io.File(Config.CONTEXT.getRealPath(MENU_VTL_PATH + fileName));
				String vpath = MENU_VTL_PATH + fileName;
				file = new java.io.File(vpath);
				if (!file.exists() || file.length() == 0) {

					itemsList = APILocator.getFolderAPI().findSubFolders(host, true);
					Comparator comparator = new AssetsComparator(orderDirection);
					Collections.sort(itemsList, comparator );
					for(int i=1; i < startFromLevel;i++){
						java.util.List<Inode> itemsList2 = new ArrayList<Inode>();
						for(Object inode : itemsList){
							if (inode instanceof Folder) {
								itemsList2.addAll(APILocator.getFolderAPI().findMenuItems((Folder) inode,orderDirection));
							}
						}
						itemsList = itemsList2;
					}

					folderPath = startFromPath;
					fileExists = false;
				}

			} else {
				Folder folder = APILocator.getFolderAPI().findFolderByPath(startFromPath, hostId, user, true);
				try{
					Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder folder=" + APILocator.getIdentifierAPI().find(folder).getPath());
				}catch (Exception e) {/*do nothing*/}

				fileName = folder.getInode() + "_siteMapLevels_"+startFromLevel+"_" + numberOfLevels+"_"+reverseOrder+"_"+addHome + "_" + siteMapIdPrefix+ "_static.vtl";
				menuId = String.valueOf(folder.getInode());
				String vpath = MENU_VTL_PATH + fileName;
				file = new java.io.File(vpath);
				//file = new java.io.File(Config.CONTEXT.getRealPath(MENU_VTL_PATH + fileName));
				Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder file=" + MENU_VTL_PATH + fileName);

				if (!file.exists()) {
					itemsList = APILocator.getFolderAPI().findMenuItems(folder, orderDirection);
					for(int i=1; i < startFromLevel;i++){
						java.util.List<Inode> itemsList2 = new ArrayList<Inode>();
						for(Object inode : itemsList){
							if (inode instanceof Folder) {
								itemsList2.addAll(APILocator.getFolderAPI().findMenuItems((Folder)inode, orderDirection));
							}
						}
						itemsList = itemsList2;
					}
					folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
					fileExists = false;
				}
			}
			String filePath = "dynamic" + java.io.File.separator + "menus" + java.io.File.separator + fileName;

			if (fileExists) {
				return filePath;
			} else {

				if (itemsList.size() > 0) {

					stringbuf.append("#if($EDIT_MODE) ");
					stringbuf.append("<form action=\"${directorURL}\" method=\"post\" name=\"form_menu_").append(menuId).append("\" id=\"form_menu_").append(menuId)
					        .append("\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"cmd\" value=\"orderMenu\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"path\" value=\"").append(startFromPath).append("\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"hostId\" value=\"").append(hostId).append("\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"pagePath\" value=\"$VTLSERVLET_URI\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"referer\" value=\"$VTLSERVLET_URI\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"startLevel\" value=\"1\">\n");
					stringbuf.append("<input type=\"hidden\" name=\"depth\" value=\"1\">\n");
					stringbuf.append("<div class=\"dotMenuReorder\">\n");
					stringbuf.append("<a href=\"javascript:document.getElementById('form_menu_").append(menuId).append("').submit();\">");
					stringbuf.append("</a></div>\n");
					stringbuf.append("</form>");
					stringbuf.append("#end ");

					    stringbuf.append("#if($addParent && $addParent == true)");
						Folder parent = APILocator.getFolderAPI().findFolderByPath(currentPath, hostId,user,true);
						if(InodeUtils.isSet(parent.getInode())) {
							String encodedPath = UtilMethods.encodeURIComponent(APILocator.getIdentifierAPI().find(parent).getPath());
							stringbuf.append("#set($parentLink = '").append(encodedPath).append("')");
							stringbuf.append("#set($parentName = '").append(UtilMethods.encodeURIComponent(UtilHTML.escapeHTMLSpecialChars(parent.getTitle()))).append("')");
							stringbuf.append("<h2><a href=\"").append(encodedPath).append("\" class=\"parentFolder\">");
							stringbuf.append(UtilHTML.escapeHTMLSpecialChars(parent.getTitle())).append("</a></h2>\n");
						}
						stringbuf.append("#end");



					stringbuf.append("<ul>");

					//adding home folder
					if(addHome)
					{
						String homeTitle = loadHomeTitle(request);
						stringbuf.append("<li id=\"").append(siteMapIdPrefix).append("home\"><a href=\"/\">").append(homeTitle).append("</a></li>");
					}



					// gets menu items for this folder
					Logger.debug(StaticMenuBuilder.class, "StaticMenuBuilder number of items=" + itemsList.size());

					// /FIRST LEVEL MENU ITEMS!!!!
					for (Object itemChild : itemsList) {

						if (itemChild instanceof Folder) {

							Folder folderChild = (Folder) itemChild;

							// recursive method here


							stringbuf = buildSubFolderSiteMapMenu(stringbuf, folderChild, numberOfLevels, 1,orderDirection,siteMapIdPrefix);

						} else if (itemChild instanceof Link) {
							if (((Link) itemChild).isWorking() && !((Link) itemChild).isDeleted()) {
								Link link = (Link) itemChild;
								if(link.getLinkType().equals(LinkType.CODE.toString())) {
									stringbuf.append("$UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('").append(UtilMethods.espaceVariableForVelocity(link.getLinkCode())).append("'), $velocityContext) ");
								} else {
									stringbuf.append("<li><a href=\"").append(((Link) itemChild).getProtocal()).append(((Link) itemChild).getUrl()).append("\" target=\"")
									        .append(((Link) itemChild).getTarget()).append("\">\n");
									stringbuf.append(UtilHTML.escapeHTMLSpecialChars(((Link) itemChild).getTitle())).append("</a></li>");
								}
							}
						} else if (itemChild instanceof HTMLPage) {
							if (((HTMLPage) itemChild).isWorking() && !((HTMLPage) itemChild).isDeleted()) {
								stringbuf.append("<li><a href=\"").append(UtilMethods.encodeURIComponent(folderPath + ((HTMLPage) itemChild).getPageUrl())).append("\">");
								stringbuf.append(UtilHTML.escapeHTMLSpecialChars(((HTMLPage) itemChild).getTitle())).append("</a></li>\n");
							}
						} else if (itemChild instanceof IFileAsset) {
							if (((IFileAsset) itemChild).isWorking() && !((IFileAsset) itemChild).isDeleted()) {
								stringbuf.append("<li><a href=\"").append(UtilMethods.encodeURIComponent(folderPath + ((IFileAsset) itemChild).getFileName())).append("\">");
								stringbuf.append(UtilHTML.escapeHTMLSpecialChars(((IFileAsset) itemChild).getTitle())).append("</a></li>\n");

							}
						}
					}
					stringbuf.append("</ul>");

				}


				// Specifying explicitly a proper character set encoding
				fo = new FileOutputStream(file);
				OutputStreamWriter out = new OutputStreamWriter(fo, UtilMethods.getCharsetConfiguration());

				if (stringbuf.length() == 0) {
					stringbuf.append("#if($EDIT_MODE)No menu items found#{end}");
				}

				out.write(stringbuf.toString());
				out.flush();
				out.close();

				Logger.debug(StaticMenuBuilder.class, "End of StaticMenuBuilder" + filePath);

				return filePath;
			}

		} catch (Exception e) {
			// Clear the string buffer, and insert only the main hyperlink text
			// to it.
			// Ignore the embedded links.
			stringbuf.delete(0, stringbuf.length());
			Logger.error(NavigationWebAPI.class,e.getMessage(),e);
		} finally {
			if(fo != null)
				try {
					fo.close();
				} catch (IOException e) {
					Logger.error(NavigationWebAPI.class, e.getMessage(), e);
				}
		}
		return "";
	}

	/**
	  * Concatenate the subfolder site map htmlcode to the folder site map htmlcode
	  * @param		stringbuf StringBuffer.
	  * @param		thisFolder Folder.
	  * @param		numberOfLevels int.
	  * @param		currentLevel int.
	  * @param		orderDirection int.
	  * @param		menuIdPrefix String.
	  * @return		StringBuffer
	 * @throws DotSecurityException
	 * @throws DotDataException
	  */
	@SuppressWarnings("unchecked")
	private StringBuffer buildSubFolderSiteMapMenu(StringBuffer stringbuf, Folder thisFolder, int numberOfLevels, int currentLevel, int orderDirection, String menuIdPrefix) throws DotDataException, DotSecurityException {

        String thisFolderPath = APILocator.getIdentifierAPI().find(thisFolder).getPath();
		stringbuf.append("<li id=\"" + menuIdPrefix +  thisFolder.getName() + "\">");
		// gets menu items for this folder
		java.util.List itemsChildrenList2 = APILocator.getFolderAPI().findMenuItems(thisFolder,orderDirection);

		// do we have any children?
		boolean nextLevelItems = (itemsChildrenList2.size() > 0 && currentLevel < numberOfLevels);

		String folderChildPath = thisFolderPath.substring(0, thisFolderPath.length() - 1);
		folderChildPath = folderChildPath.substring(0, folderChildPath.lastIndexOf("/"));

		Host host = WebAPILocator.getHostWebAPI().findParentHost(thisFolder, user, true);//DOTCMS-4099
		Identifier id = APILocator.getIdentifierAPI().find(host,thisFolderPath + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));
		if(id != null && InodeUtils.isSet(id.getInode()))
			stringbuf.append("<a href=\"" + UtilMethods.encodeURIComponent(thisFolderPath) + "\">");
		stringbuf.append(UtilHTML.escapeHTMLSpecialChars(thisFolder.getTitle()));
		if(id != null && InodeUtils.isSet(id.getInode()))
			stringbuf.append("</a>");

		if (currentLevel < numberOfLevels) {

			if (nextLevelItems) {
				stringbuf.append("<ul>");
			}

			for (Object childChild2 : itemsChildrenList2) {
				if (childChild2 instanceof Folder) {
					Folder folderChildChild2 = (Folder) childChild2;

					Logger.debug(this, "folderChildChild2= " + folderChildChild2.getTitle() + " currentLevel=" + currentLevel + " numberOfLevels="
							+ numberOfLevels);
					if (currentLevel <= numberOfLevels) {
						stringbuf = buildSubFolderSiteMapMenu(stringbuf, folderChildChild2, numberOfLevels, currentLevel + 1,orderDirection,menuIdPrefix);
					} else {

						stringbuf.append("<li><a href=\"").append(UtilMethods.encodeURIComponent(APILocator.getIdentifierAPI().find(folderChildChild2).getPath())).append("index.")
						        .append(Config.getStringProperty("VELOCITY_PAGE_EXTENSION")).append("\">");
						stringbuf.append(UtilHTML.escapeHTMLSpecialChars(folderChildChild2.getTitle())).append("</a></li>");

					}
				} else if (childChild2 instanceof Link) {
					if (((Link) childChild2).isWorking() && !((Link) childChild2).isDeleted()) {
	                	Link link = (Link) childChild2;
	                	if(link.getLinkType().equals(LinkType.CODE.toString())) {
		                    stringbuf.append("$UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('").append(UtilMethods.espaceVariableForVelocity(link.getLinkCode())).append("'), $velocityContext) ");
	                	} else {
							stringbuf.append("<li><a href=\"").append(((Link) childChild2).getProtocal()).append(((Link) childChild2).getUrl()).append("\" target=\"")
									.append(((Link) childChild2).getTarget()).append("\">");
							stringbuf.append(UtilHTML.escapeHTMLSpecialChars(((Link) childChild2).getTitle())).append("</a></li>");
	                	}
					}
				} else if (childChild2 instanceof HTMLPage) {
					if (((HTMLPage) childChild2).isWorking() && !((HTMLPage) childChild2).isDeleted()) {
						stringbuf.append("<li><a href=\"").append(UtilMethods.encodeURIComponent(thisFolderPath+((HTMLPage) childChild2).getPageUrl())).append("\">");
						stringbuf.append(UtilHTML.escapeHTMLSpecialChars(((HTMLPage) childChild2).getTitle())).append("</a></li>");

					}
				} else if (childChild2 instanceof IFileAsset) {
					if (((IFileAsset) childChild2).isWorking() && !((IFileAsset) childChild2).isDeleted()) {
						stringbuf.append("<li><a href=\"").append(UtilMethods.encodeURIComponent(thisFolderPath+((IFileAsset) childChild2).getFileName())).append("\">");
						stringbuf.append(UtilHTML.escapeHTMLSpecialChars(((IFileAsset) childChild2).getTitle())).append("</a></li>\n");

					}
				}
			}
		}
		if (nextLevelItems) {
			stringbuf.append("</ul>");

		}
		stringbuf.append("</li>");
		return stringbuf;
	}

	/**
	 * Returns true if the menu is valid and contains items in it
	 * @param startFromLevel start level from the current request path
	 * @param maxDepth
	 * @param request
	 * @return
	 * @throws JspException
	 */
	public boolean menuItemsByDepth(int startFromLevel, int maxDepth, HttpServletRequest request) throws JspException
	{
		String currentPath = request.getRequestURI();
		StringTokenizer st = new StringTokenizer(currentPath, "/");
		int i = 1;
		StringBuffer myPath = new StringBuffer("/");
		boolean rightLevel = false;
		while (st.hasMoreTokens()) {
			if (i++ >= startFromLevel) {
				rightLevel = true;
				break;
			}
			String myToken = st.nextToken();
			if (!st.hasMoreTokens())
				break;
			myPath.append(myToken);
			myPath.append("/");
		}

		boolean returnValue = (rightLevel ? menuItems(myPath.toString(), maxDepth, request) : false);
		return returnValue;
	}

	/**
	 * Returns true if the menu is valid and contains items in it
	 * @param startFromPath
	 * @param maxDepth
	 * @param request
	 * @return
	 * @throws JspException
	 */
	public boolean menuItems(String startFromPath, int numberOfLevels, HttpServletRequest request) throws JspException
	{
 		boolean fileExists = false;
		try
		{
			//Create the Menu for this path and depthLevel
			createMenu(startFromPath,numberOfLevels,request);
			//Validate if the file has been created, if so the menu have items
			String currentPath = request.getRequestURI();
			Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
			String hostId = host.getIdentifier();

			if ((startFromPath == null) || (startFromPath.length() == 0)) {

				Logger.debug(NavigationWebAPI.class, "pagePath=" + currentPath);

				int idx1 = currentPath.indexOf("/");
				int idx2 = currentPath.indexOf("/", idx1 + 1);

				startFromPath = currentPath.substring(idx1, idx2 + 1);

				Logger.debug(NavigationWebAPI.class, "path=" + startFromPath);
			}

			boolean addSpans = false;
			if(request.getAttribute("menu_spans") != null && (Boolean)request.getAttribute("menu_spans")){
				addSpans = true;
			}

			String firstItemClass = "";
			if(request.getAttribute("firstItemClass") != null){
				firstItemClass = " class=\""+(String)request.getAttribute("firstItemClass")+"_";
			}

			String lastItemClass = "";
			if(request.getAttribute("lastItemClass") != null ) {
				lastItemClass=" class=\""+(String)request.getAttribute("lastItemClass")+"_";
			}

			String menuIdPrefix = "";
			if(request.getAttribute("menuIdPrefix") != null ){
				menuIdPrefix=(String)request.getAttribute("menuIdPrefix")+"_";
			}

			String paramsValues = ((Boolean)addSpans).toString() + firstItemClass.toString() + lastItemClass.toString() +
			menuIdPrefix.toString();

			String fileName = "";
			java.io.File file = null;
			if ("/".equals(startFromPath))
			{

				fileName = hostId + "_levels_" + numberOfLevels + paramsValues.hashCode() + "_static.vtl";
				file = new java.io.File(MENU_VTL_PATH + fileName);
				if (file.exists() && file.length() > 0)
				{
					fileExists = true;
				}
			}
			else
			{
				Folder folder = APILocator.getFolderAPI().findFolderByPath(startFromPath, hostId, user, true);
				fileName = folder.getInode() + "_levels_" + numberOfLevels + paramsValues.hashCode() +  "_static.vtl";
				file = new java.io.File(MENU_VTL_PATH + fileName);
				if (file.exists() && file.length() > 0)
				{
					fileExists = true;
				}
			}
		}
		catch(Exception ex)
		{
			Logger.error(this,ex.toString(),ex);
		}
		return fileExists;
	}

	/**
	  * Return array list of html page in a folder
	  * @param		folderPath String.
	  * @param		request HttpServletRequest.
	  * @return		List<HTMLPage>
	  * @exception	JspException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	  */
	@SuppressWarnings("unchecked")
	public List<HTMLPage> getPagesList(String folderPath, HttpServletRequest request) throws PortalException, SystemException, DotDataException, DotSecurityException {

		List<HTMLPage> pagesList = new ArrayList<HTMLPage>();
		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		Folder thisFolder = APILocator.getFolderAPI().findFolderByPath(folderPath, host,user,true);
		List<Inode> itemsChildrenList = (List<Inode>)APILocator.getFolderAPI().findMenuItems(thisFolder, user, true);
		for (Inode childChild : itemsChildrenList) {
			if(childChild instanceof HTMLPage){
				pagesList.add((HTMLPage) childChild);
			}
		}



		return pagesList;
	}

	//=========== Begin Navigation macro methods ==============

	/**
	  * Return path of the file with the menu items ordered
	  * @param		startFromLevel integer with the level where the navigation menu will start to show.
	  * @param		maxDepth integer with the number of level to show counting from the startFromLevel.
	  * @param		request HttpServletRequest.
	  * @return		String with the page of the file with the menu items ordered.
	  * @exception	JspException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	  */
	public String createMenuByDepth(int startFromLevel, int maxDepth, HttpServletRequest request) throws JspException, PortalException, SystemException, DotDataException, DotSecurityException {

		String currentPath = request.getRequestURI();
		StringTokenizer st = new StringTokenizer(currentPath, "/");
		int i = 1;
		StringBuffer myPath = new StringBuffer("/");
		boolean rightLevel = false;
		while (st.hasMoreTokens()) {
			if (i++ >= startFromLevel) {
				rightLevel = true;
				break;
			}
			String myToken = st.nextToken();
			if (!st.hasMoreTokens())
				break;
			myPath.append(myToken);
			myPath.append("/");

		}

		String menuString = (rightLevel ? buildMenuItems(myPath.toString(), maxDepth, request) : "");
		java.io.File file;
		file = new java.io.File(SHORT_MENU_VTL_PATH+menuString);
		if(!file.exists()){
			menuString = "";
		}
		return menuString;
	}

	/**
	  * Return start from path
	  * @param		startFromLevel integer with the level where the path will start to show.
	  * @param		request HttpServletRequest.
	  * @return		String with the path
	  * @exception	JspException
	  */
	public String getStartFromPath(int startFromLevel, HttpServletRequest request) throws JspException {

		String currentPath = request.getRequestURI();
		StringTokenizer st = new StringTokenizer(currentPath, "/");
		int i = 1;
		StringBuffer myPath = new StringBuffer("/");
		while (st.hasMoreTokens()) {
			if (i++ >= startFromLevel) {
				break;
			}
			String myToken = st.nextToken();
			if (!st.hasMoreTokens())
				break;
			myPath.append(myToken);
			myPath.append("/");

		}

		String startFromPath = myPath.toString();

		if ((startFromPath == null) || (startFromPath.length() == 0)) {
			int idx1 = currentPath.indexOf("/");
			int idx2 = currentPath.indexOf("/", idx1 + 1);

			startFromPath = currentPath.substring(idx1, idx2 + 1);
		}

		return startFromPath;
	}

	/**
	  * Return and/or create the path of the file with the menu items ordered. The file will contain a velocity array of hashmap. Each item of the array will be a item of the menu.
	  * Each item (hashmap) will have the following keys, depending the type of item:
	  *
	  * Type FOLDER
	  * key="type"			value="FOLDER"
	  * key="title"			String
	  * key="name"			String
	  * key="path"			String
	  * key="submenu"		ArrayList of the next level
	  * key="isFirstItem"	Boolean
	  * key="isLastItem"	Boolean
	  *
	  * Type CODED LINK
	  * key="type"			value="LINK"
	  * key="path"			String
	  * key="linkType"		value="CODE"
	  * key="isFirstItem"	Boolean
	  * key="isLastItem"	Boolean
	  *
	  * Type LINK
	  * key="type"			value="LINK"
	  * key="name"			String
	  * key="protocal"		String
	  * key="target"		String
	  * key="title"			String
	  * key="isFirstItem"	Boolean
	  * key="isLastItem"	Boolean
	  *
	  * Type HTMLPAGE
	  * key="type"			value="HTMLPAGE"
	  * key="name"			String
	  * key="path"			String
	  * key="title"			String
	  * key="isFirstItem"	Boolean
	  * key="isLastItem"	Boolean
	  *
	  * Type FILE
	  * key="type"			value="FILE"
	  * key="name"			String
	  * key="path"			String
	  * key="title"			String
	  * key="isFirstItem"	Boolean
	  * key="isLastItem"	Boolean
	  *
	  * @param		startFromPath String with the start path of the menu items.
	  * @param		numberOfLevels integer with the number of level of the menu items counting from startFromPath.
	  * @param		request HttpServletRequest.
	  * @return		String with the path
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws SystemException
	 * @throws PortalException
	  * @exception	JspException
	  */
	@SuppressWarnings("unchecked")
	public String buildMenuItems(String startFromPath, int numberOfLevels, HttpServletRequest request) throws PortalException, SystemException, DotDataException, DotSecurityException
	{
		String currentPath = request.getRequestURI();
		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		String hostId = host.getIdentifier();
		StringBuffer stringbuf = new StringBuffer();

		try {

			Logger.debug(NavigationWebAPI.class, "\n\nNavigationWebAPI :: StaticMenuBuilder begins");
			Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder start path=" + startFromPath);
			Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder number of levels=" + numberOfLevels);

			if ((startFromPath == null) || (startFromPath.length() == 0)) {
				Logger.debug(NavigationWebAPI.class, "pagePath=" + currentPath);

				int idx1 = currentPath.indexOf("/");
				int idx2 = currentPath.indexOf("/", idx1 + 1);

				startFromPath = currentPath.substring(idx1, idx2 + 1);

				Logger.debug(NavigationWebAPI.class, "path=" + startFromPath);
			}

			Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder hostId=" + host.getIdentifier());

			java.util.List itemsList = new ArrayList<Inode>();
			String folderPath = "";
			String fileName = "";
			boolean fileExists = true;

			java.io.File file = null;
			if ("/".equals(startFromPath)) {
				fileName = hostId + "_levels" + startFromPath.replace("/", "_") + "_" + numberOfLevels + "_static.vtl";
				file = new java.io.File(MENU_VTL_PATH + fileName);
				if (!file.exists() || file.length() == 0) {
					itemsList = APILocator.getFolderAPI().findSubFolders(host, true);
					folderPath = startFromPath;
					fileExists = false;
				}
			} else {
                Folder folder = APILocator.getFolderAPI().findFolderByPath(startFromPath, hostId, user, true);
                try{
                	Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder folder=" + APILocator.getIdentifierAPI().find(folder).getPath());
                }catch(Exception e){/*do Nothing*/}

				fileName = folder.getInode() + "_levels" + startFromPath.replace("/", "_") + "_" + numberOfLevels + "_static.vtl";
				file = new java.io.File(MENU_VTL_PATH + fileName);
				Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder file=" + MENU_VTL_PATH + fileName);

				if (!file.exists() || file.length() == 0) {
					file.createNewFile();
					itemsList = APILocator.getFolderAPI().findMenuItems(folder, APILocator.getUserAPI().getSystemUser(),false);
					folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
					fileExists = false;
				}
			}

			Comparator comparator = new AssetsComparator(1);
			Collections.sort(itemsList, comparator);

			String filePath = "dynamic" + java.io.File.separator + "menus" + java.io.File.separator + fileName;
			if (fileExists) {
				return filePath;
			} else {

				if (itemsList.size() > 0) {

					stringbuf.append("#set ($navigationItems = $contents.getEmptyList())");

					// gets menu items for this folder
					Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: StaticMenuBuilder number of items=" + itemsList.size());

					String submenu;
					String submenuName;
					// /FIRST LEVEL MENU ITEMS!!!!
					boolean isLastItem = false;
					boolean isFirstItem = true;
					int index = 0;
					for (Object itemChild : itemsList) {
						index++;
						//Check if the item is the last one
						if(index == itemsList.size()){
							isLastItem = true;
						}
						//Check if the item is the first one
						if(index > 1){
							isFirstItem = false;
						}

						if (itemChild instanceof Folder) {
							Folder folderChild = (Folder) itemChild;

							submenuName = "_" + folderChild.getName().replace(" ", "").trim();
							// recursive method here
							submenu = getSubFolderMenuItems(folderChild, submenuName, numberOfLevels, 1, isFirstItem, isLastItem);

							stringbuf.append("#set ($menuItem = $contents.getEmptyMap())");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"FOLDER\"))");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"").append(UtilHTML.escapeHTMLSpecialChars(folderChild.getTitle())).append("\"))");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"").append(folderChild.getName()).append("\"))");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", \"").append(APILocator.getIdentifierAPI().find(folderChild).getPath()).append("\"))");
							stringbuf.append(submenu);
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"submenu\", $_").append(submenuName).append("))");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", ").append(isFirstItem).append("))");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", ").append(isLastItem).append("))");
							stringbuf.append("#set ($_dummy = $navigationItems.add($menuItem))");

							stringbuf.append("#set ($_").append(submenuName).append(" = $contents.getEmptyList()) ");

						} else if (itemChild instanceof Link) {
							Link link = (Link) itemChild;
							if(link.getLinkType().equals(LinkType.CODE.toString())) {
								stringbuf.append("#set ($menuItem = $contents.getEmptyMap())");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"LINK\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", $UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('").append(UtilMethods.espaceVariableForVelocity(link.getLinkCode())).append("'), $velocityContext)))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"linkType\", \"CODE\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", ").append(isFirstItem).append("))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", ").append(isLastItem).append("))");
								stringbuf.append("#set ($_dummy = $navigationItems.add($menuItem))");
							} else {
								stringbuf.append("#set ($menuItem = $contents.getEmptyMap())");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"LINK\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"").append(link.getUrl()).append("\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"protocal\", \"").append(link.getProtocal()).append("\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"target\", \"").append(link.getTarget()).append("\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"").append(UtilHTML.escapeHTMLSpecialChars(link.getTitle())).append("\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", ").append(isFirstItem).append("))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", ").append(isLastItem).append("))");
								stringbuf.append("#set ($_dummy = $navigationItems.add($menuItem))");
							}
						} else if (itemChild instanceof HTMLPage) {
							HTMLPage htmlpage = (HTMLPage) itemChild;

							stringbuf.append("#set ($menuItem = $contents.getEmptyMap())");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"HTMLPAGE\"))");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"").append(htmlpage.getPageUrl()).append("\"))");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", \"").append(folderPath).append("\"))");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"").append(UtilHTML.escapeHTMLSpecialChars(htmlpage.getTitle())).append("\"))");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", ").append(isFirstItem).append("))");
							stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", ").append(isLastItem).append("))");
							stringbuf.append("#set ($_dummy = $navigationItems.add($menuItem))");
						} else if (itemChild instanceof IFileAsset) {
							IFileAsset fileItem = (IFileAsset) itemChild;
							if (fileItem.isWorking() && !fileItem.isDeleted()) {
								stringbuf.append("#set ($menuItem = $contents.getEmptyMap())");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"FILE\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"").append(fileItem.getFileName()).append("\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", \"").append(folderPath).append("\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"").append(UtilHTML.escapeHTMLSpecialChars(fileItem.getTitle())).append("\"))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", ").append(isFirstItem).append("))");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", ").append(isLastItem).append("))");
								stringbuf.append("#set ($_dummy = $navigationItems.add($menuItem))");
							}
						}
					}
					stringbuf.append("#set ($menuItem = $contents.getEmptyMap())");
				}else{
					stringbuf.append("#set ($navigationItems = $contents.getEmptyList())");
				}

				if (stringbuf.toString().getBytes().length > 0) {
					// Specifying explicitly a proper character set encoding
					FileOutputStream fo = new FileOutputStream(file);
					OutputStreamWriter out = new OutputStreamWriter(fo, UtilMethods.getCharsetConfiguration());
					out.write(stringbuf.toString());
					out.flush();
					out.close();
					fo.close();
				} else {
					Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: Error creating static menu!!!!!");
				}

				Logger.debug(NavigationWebAPI.class, "NavigationWebAPI :: End of StaticMenuBuilder" + filePath);

				return filePath;
			}
		} catch (Exception e) {
			// Clear the string buffer, and insert only the main hyperlink text
			// to it.
			// Ignore the embedded links.
			stringbuf.delete(0, stringbuf.length());
			Logger.error(NavigationWebAPI.class,e.getMessage(),e);

		}
		return "";
	}

	/**
	  * Return a string with velocity code of the submenu items ordered. The file will contain a velocity array of hashmap. Each item of the array will be a item of the submenu.
	  * Each item (hashmap) will have the following keys, depending the type of item:
	  *
	  * Type FOLDER
	  * key="type"			value="FOLDER"
	  * key="title"			String
	  * key="name"			String
	  * key="path"			String
	  * key="submenu"		ArrayList of the next level
	  * key="isFirstItem"	Boolean
	  * key="isLastItem"	Boolean
	  *
	  * Type CODED LINK
	  * key="type"			value="LINK"
	  * key="path"			String
	  * key="linkType"		value="CODE"
	  * key="isFirstItem"	Boolean
	  * key="isLastItem"	Boolean
	  *
	  * Type LINK
	  * key="type"			value="LINK"
	  * key="name"			String
	  * key="protocal"		String
	  * key="target"		String
	  * key="title"			String
	  * key="isFirstItem"	Boolean
	  * key="isLastItem"	Boolean
	  *
	  * Type HTMLPAGE
	  * key="type"			value="HTMLPAGE"
	  * key="name"			String
	  * key="path"			String
	  * key="title"			String
	  * key="isFirstItem"	Boolean
	  * key="isLastItem"	Boolean
	  *
	  * Type FILE
	  * key="type"			value="FILE"
	  * key="name"			String
	  * key="path"			String
	  * key="title"			String
	  * key="isFirstItem"	Boolean
	  * key="isLastItem"	Boolean
	  *
	  * @param		thisFolder Folder of the submenu.
	  * @param		submenuName String with the name of the velocity submenu name.
	  * @param		numberOfLevels integer with the number of level of the menu items counting from startFromPath of the parent menu.
	  * @param		currentLevel integer with the current level of this folder.
	  * @param		isFirstItem boolean.
	  * @param		isLastItem boolean.
	  * @return		String with velocity code of the array of menu items
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotStateException 
	  */
	@SuppressWarnings("unchecked")
	private String getSubFolderMenuItems(Folder thisFolder, String submenuName, int numberOfLevels, int currentLevel, boolean isFirstItem, boolean isLastItem) throws DotStateException, DotDataException, DotSecurityException {
		StringBuffer stringbuf = new StringBuffer();
		stringbuf.append("#set ($" + "_" + submenuName + " = $contents.getEmptyList()) ");

		// gets menu items for this folder
		java.util.List itemsChildrenList2 = new ArrayList();
		try {
			itemsChildrenList2 = APILocator.getFolderAPI().findMenuItems(thisFolder, user,true);
		} catch (Exception e1) {
			Logger.error(NavigationWebAPI.class, e1.getMessage(), e1);
		} 
		
		String folderPath = "";
		try {
			folderPath = APILocator.getIdentifierAPI().find(thisFolder).getPath();
		} catch (Exception e1) {
			Logger.error(NavigationWebAPI.class, e1.getMessage(), e1);
		} 
		String folderChildPath = folderPath.substring(0, folderPath.length() - 1);
		folderChildPath = folderChildPath.substring(0, folderChildPath.lastIndexOf("/"));

		if (currentLevel < numberOfLevels) {

			String submenu;
			String subSubmenuName;
			isLastItem = false;
			isFirstItem = true;
			int index = 0;
			for (Object childChild2 : itemsChildrenList2) {
				index++;
				//Check if is last item
				if(index == itemsChildrenList2.size()){
					isLastItem = true;
				}
				//Check if is first item
				if(index > 1){
					isFirstItem = false;
				}

				if (childChild2 instanceof Folder) {
					Folder folderChildChild2 = (Folder) childChild2;
					String folderChildPath2 = "";
					try {
						folderChildPath2 = APILocator.getIdentifierAPI().find(folderChildChild2).getPath();
					} catch (Exception e) {
						Logger.error(NavigationWebAPI.class, e.getMessage(), e);
					} 
					
					Logger.debug(this, "folderChildChild2= " + folderChildChild2.getTitle() + " currentLevel=" + currentLevel + " numberOfLevels=" + numberOfLevels);

					if (currentLevel <= numberOfLevels) {
						subSubmenuName = folderChildChild2.getName().replace(" ", "").trim();

						submenu = getSubFolderMenuItems(folderChildChild2, subSubmenuName, numberOfLevels, currentLevel + 1, isFirstItem, isLastItem);

						stringbuf.append("#set ($menuItem").append(submenuName).append(" = $contents.getEmptyMap())");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"type\", \"FOLDER\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"title\", \"").append(UtilHTML.escapeHTMLSpecialChars(folderChildChild2.getTitle())).append("\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"name\", \"").append(folderChildChild2.getName()).append("\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"path\", \"").append(folderChildPath2).append("\"))");
						stringbuf.append(submenu);
						stringbuf.append(" #set ($_dummy  = $menuItem").append(submenuName).append(".put(\"submenu\", $_").append(subSubmenuName).append("))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isFirstItem\", ").append(isFirstItem).append("))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isLastItem\", ").append(isLastItem).append("))");
						stringbuf.append("#set ($_dummy = $_").append(submenuName).append(".add($menuItem").append(submenuName).append("))");
						stringbuf.append("#set ($_").append(subSubmenuName).append(" = $contents.getEmptyList())");
					} else {
						stringbuf.append("#set ($menuItem").append(submenuName).append(" = $contents.getEmptyMap())");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"type\", \"HTMLPAGE\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"path\", \"").append(folderChildPath2).append("\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"title\", \"").append(UtilHTML.escapeHTMLSpecialChars(folderChildChild2.getTitle())).append("\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isFirstItem\", ").append(isFirstItem).append("))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isLastItem\", ").append(isLastItem).append("))");
						stringbuf.append("#set ($_dummy = $_").append(submenuName).append(".add($menuItem").append(submenuName).append("))");
					}
				} else if (childChild2 instanceof Link) {
					if (((Link) childChild2).isWorking() && !((Link) childChild2).isDeleted()) {
						Link link = (Link) childChild2;
	                	if(link.getLinkType().equals(LinkType.CODE.toString())) {
		                    stringbuf.append("#set ($menuItem").append(submenuName).append(" = $contents.getEmptyMap())");
		                    stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"type\", \"LINK\"))");
							stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"path\", $UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('").append(UtilMethods.espaceVariableForVelocity(link.getLinkCode()) ).append("'), $velocityContext)))");
							stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"linkType\", \"CODE\"))");
							stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isFirstItem\", ").append(isFirstItem).append("))");
							stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isLastItem\", ").append(isLastItem).append("))");
							stringbuf.append("#set ($_dummy = $_").append(submenuName).append(".add($menuItem").append(submenuName).append("))");
	                	} else {
	        				stringbuf.append("#set ($menuItem").append(submenuName).append(" = $contents.getEmptyMap())");
	        				stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"type\", \"LINK\"))");
	        				stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"name\", \"").append(link.getUrl()).append("\"))");
							stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"protocal\", \"").append(link.getProtocal()).append("\"))");
							stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"target\", \"").append(link.getTarget()).append("\"))");
							stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"title\", \"").append(UtilHTML.escapeHTMLSpecialChars(link.getTitle())).append("\"))");
							stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isFirstItem\", ").append(isFirstItem).append("))");
							stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isLastItem\", ").append(isLastItem).append("))");
							stringbuf.append("#set ($_dummy = $_").append(submenuName).append(".add($menuItem").append(submenuName).append("))");
	                	}
					}
				} else if (childChild2 instanceof HTMLPage) {
					HTMLPage htmlpage = (HTMLPage) childChild2;
					if (htmlpage.isWorking() && !htmlpage.isDeleted()) {
        				stringbuf.append("#set ($menuItem").append(submenuName).append(" = $contents.getEmptyMap())");
        				stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"type\", \"HTMLPAGE\"))");
        				stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"name\", \"").append(htmlpage.getPageUrl()).append("\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"path\", \"").append(folderPath).append("\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"title\", \"").append(UtilHTML.escapeHTMLSpecialChars(htmlpage.getTitle())).append("\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isFirstItem\", ").append(isFirstItem).append("))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isLastItem\", ").append(isLastItem).append("))");
						stringbuf.append("#set ($_dummy = $_").append(submenuName).append(".add($menuItem").append(submenuName).append("))");
					}
				} else if (childChild2 instanceof IFileAsset) {
					IFileAsset fileItem = (IFileAsset) childChild2;
					if (fileItem.isWorking() && !fileItem.isDeleted()) {
        				stringbuf.append("#set ($menuItem").append(submenuName).append(" = $contents.getEmptyMap())");
        				stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"type\", \"FILE\"))");
        				stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"name\", \"").append(fileItem.getFileName()).append("\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"path\", \"").append(folderPath).append("\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"title\", \"").append(UtilHTML.escapeHTMLSpecialChars(fileItem.getTitle())).append("\"))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isFirstItem\", ").append(isFirstItem).append("))");
						stringbuf.append("#set ($_dummy  = $menuItem").append(submenuName).append(".put(\"isLastItem\", ").append(isLastItem).append("))");
						stringbuf.append("#set ($_dummy = $_").append(submenuName).append(".add($menuItem").append(submenuName).append("))");
					}
				}
			}
			stringbuf.append("#set ($menuItem").append(submenuName).append(" = $contents.getEmptyMap())");
		}
		return stringbuf.toString();
	}

	public int getFormCount(){
		return formCount;
	}

	public void increaseFormCount(){
		formCount++;
	}

	//=========== End Navigation macro methods ==============
}