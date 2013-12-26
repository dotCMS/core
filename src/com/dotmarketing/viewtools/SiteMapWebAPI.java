package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.velocity.VelocityServlet;

public class SiteMapWebAPI implements ViewTool {


    public void init(Object obj) {

    }

    //SiteMap Building Methods
    private int columns = 3;
    private double numberToBreak = 0;
    private int totalCount = 0;
    private int percent = 0;

    public String getSiteMap(HttpServletRequest request) {

        StringBuffer stringbuf = new StringBuffer();

        try {

            int level = 0;

            Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);

            java.util.List itemsChildrenList = APILocator.getFolderAPI().findMenuItems(host,APILocator.getUserAPI().getSystemUser(),false);
            double divTotal = ((double) itemsChildrenList.size() / (double) columns);
            numberToBreak = Math.ceil(divTotal);
            percent = Math.round(100 / columns);

            stringbuf.append("<br><table border=0 cellpadding=0 cellspacing=0 width=\"95%\" align=\"center\">\n");
            stringbuf.append("<tr><td colspan=\"" + columns + "\">");
            stringbuf.append("<strong><A href='/'>Home</a></strong></td><br></tr>\n");
            stringbuf.append("<tr><td valign=top width=\"" + percent + "%\">");

            stringbuf = getEntries(itemsChildrenList, host, stringbuf, level, request);

            stringbuf.append("</td></tr></table><BR><BR><BR>");

        } catch (Exception e) {
            // Clear the string buffer, and insert only the main hyperlink text
            // to it.
            // Ignore the embedded links.
            stringbuf.delete(0, stringbuf.length());
            Logger.error(this, e.toString(), e);
        }
        return stringbuf.toString();
    }

    private StringBuffer getEntries(List itemsChildrenList, Host parentHost, StringBuffer stringbuf, int level, HttpServletRequest request) throws DotStateException, DotDataException, DotSecurityException {

        //gets menu items for this folder
        java.util.Iterator itemsChildrenListIter = itemsChildrenList.iterator();

        while (itemsChildrenListIter.hasNext()) {

            Inode childItem = (Inode) itemsChildrenListIter.next();

            if (childItem instanceof Folder) {
                if ((level == 0) && (totalCount != 0) && (totalCount % numberToBreak) == 0) {
                    stringbuf.append("</td>\n<td valign=top width=\"" + percent + "%\">\n");
                    totalCount = 0;
                }
                Folder folderchildItem = (Folder) childItem;
                String folderchildItemPath = "";
				try {
					folderchildItemPath = APILocator.getIdentifierAPI().find(folderchildItem).getPath();
				} catch (Exception e) {
					Logger.error(SiteMapWebAPI.class, e.getMessage(), e);
				} 
                if (level == 0) {
                    if (totalCount != 0) {
                        stringbuf.append("</ul>\n");
                    }
                    totalCount++;
                    //stringbuf.append("<img align=\"absmiddle\"
                    // src=\"/portal/jsp/html/skin/image/common/trees/plus.gif\"
                    // id=\"img" + folderchildItem.getInode() + "\">\n");
                    stringbuf.append("<strong><a href=\"" + folderchildItemPath + "\">"
                            + UtilHTML.escapeHTMLSpecialChars(folderchildItem.getTitle()) + "</a></strong><ul>\n");
                } else {
                    stringbuf.append("<B><a href=\"" + folderchildItemPath + "\">" + folderchildItem.getTitle()
                            + "</a></B><ul>\n");
                }
                java.util.List itemsChildrenListFolder = new ArrayList();
				try {
					itemsChildrenListFolder = APILocator.getFolderAPI().findMenuItems(folderchildItem,APILocator.getUserAPI().getSystemUser(),false);
				} catch (Exception e) {
					Logger.error(SiteMapWebAPI.class, e.getMessage(), e);
				} 
                stringbuf = getEntries(itemsChildrenListFolder, folderchildItem, stringbuf, level + 1);
                stringbuf.append("</ul>\n");
            } else if (childItem instanceof Link) {
                if (((Link) childItem).isWorking() && !((Link) childItem).isDeleted()) {
                	Link link = (Link) childItem;
                	if(link.getLinkType().equals(LinkType.CODE.toString())) {
                		if ( request.getAttribute( VelocityServlet.VELOCITY_CONTEXT ) != null && request.getAttribute( VelocityServlet.VELOCITY_CONTEXT ) instanceof ChainedContext ) {
                			stringbuf.append(UtilMethods.evaluateVelocity(UtilMethods.restoreVariableForVelocity(UtilMethods.espaceVariableForVelocity(link.getLinkCode())), (Context) request.getAttribute( VelocityServlet.VELOCITY_CONTEXT )));
                        }else{
                        	stringbuf.append("$UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('" + UtilMethods.espaceVariableForVelocity(link.getLinkCode()) + "'), $velocityContext)\n");
                        }
                	} else {
	                    stringbuf.append("<li><a href=\"" + ((Link) childItem).getProtocal() + ((Link) childItem).getUrl()
	                            + "\" target=\"" + ((Link) childItem).getTarget() + "\">\n");
	                    stringbuf.append(UtilHTML.escapeHTMLSpecialChars(((Link) childItem).getTitle()) + "</a></li>\n");
                	}
                }
            } else if (childItem instanceof HTMLPage) {
                if (((HTMLPage) childItem).isWorking() && !((HTMLPage) childItem).isDeleted()) {
                    stringbuf.append("<li><a href=\"" + "/" + ((HTMLPage) childItem).getPageUrl() + "\">\n");
                    stringbuf.append(UtilHTML.escapeHTMLSpecialChars(((HTMLPage) childItem).getTitle()) + "</a></li>\n");
                }
            } else if (childItem instanceof IFileAsset) {
                if (((IFileAsset) childItem).isWorking() && !((IFileAsset) childItem).isDeleted()) {
                    stringbuf.append("<li><a href=\"" + "/" + ((IFileAsset) childItem).getFileName() + "\">\n");
                    stringbuf.append(UtilHTML.escapeHTMLSpecialChars(((IFileAsset) childItem).getTitle()) + "</a></li>\n");
                }
            }

        }

        return stringbuf;
    }

    private StringBuffer getEntries(List itemsChildrenList, Folder parentFolder, StringBuffer stringbuf, int level) throws DotStateException, DotDataException, DotSecurityException {

        //gets menu items for this folder
        java.util.Iterator itemsChildrenListIter = itemsChildrenList.iterator();
        String parentFolderPath = "";
		try {
			parentFolderPath = APILocator.getIdentifierAPI().find(parentFolder).getPath();
		} catch (Exception e) {
			Logger.error(SiteMapWebAPI.class, e.getMessage(), e);
		} 

        while (itemsChildrenListIter.hasNext()) {

            Inode childItem = (Inode) itemsChildrenListIter.next();

            if (childItem instanceof Folder) {
                if ((level == 0) && (totalCount != 0) && (totalCount % numberToBreak) == 0) {
                    stringbuf.append("</td>\n<td valign=top width=\"" + percent + "%\">\n");
                    totalCount = 0;
                }
                Folder folderchildItem = (Folder) childItem;
                String folderchildItemPath = "";
				try {
					folderchildItemPath = APILocator.getIdentifierAPI().find(folderchildItem).getPath();
				} catch (Exception e) {
					Logger.error(SiteMapWebAPI.class, e.getMessage(), e);
				} 
                if (level == 0) {
                    if (totalCount != 0) {
                        stringbuf.append("</ul>\n");
                    }
                    totalCount++;
                    //stringbuf.append("<img align=\"absmiddle\"
                    // src=\"/portal/jsp/html/skin/image/common/trees/plus.gif\"
                    // id=\"img" + folderchildItem.getInode() + "\">\n");
                    stringbuf.append("<strong><a href=\"" + folderchildItemPath + "\">"
                            + folderchildItem.getTitle() + "</a></strong><ul>\n");
                } else {
                    stringbuf.append("<B><a href=\"" + folderchildItemPath + "\">" + folderchildItem.getTitle()
                            + "</a></B><ul>\n");
                }
                java.util.List itemsChildrenListFolder = new ArrayList();
				try {
					itemsChildrenListFolder = APILocator.getFolderAPI().findMenuItems(folderchildItem,APILocator.getUserAPI().getSystemUser(),false);
				} catch (Exception e) {
					Logger.error(SiteMapWebAPI.class, e.getMessage(), e);
				} 
                stringbuf = getEntries(itemsChildrenListFolder, folderchildItem, stringbuf, level + 1);
                stringbuf.append("</ul>\n");
            } else if (childItem instanceof Link) {
                if (((Link) childItem).isWorking() && !((Link) childItem).isDeleted()) {
                	Link link = (Link) childItem;
                	if(link.getLinkType().equals(LinkType.CODE.toString())) {
	                    stringbuf.append("$UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('" + UtilMethods.espaceVariableForVelocity(link.getLinkCode()) + "'), $velocityContext)\n");
                	} else {
	                    stringbuf.append("<li><a href=\"" + ((Link) childItem).getProtocal() + ((Link) childItem).getUrl()
	                            + "\" target=\"" + ((Link) childItem).getTarget() + "\">\n");
	                    stringbuf.append(((Link) childItem).getTitle() + "</a></li>\n");
                	}
                }
            } else if (childItem instanceof HTMLPage) {
                if (((HTMLPage) childItem).isWorking() && !((HTMLPage) childItem).isDeleted()) {
                    stringbuf.append("<li><a href=\"" + parentFolderPath + ((HTMLPage) childItem).getPageUrl()
                            + "\">\n");
                    stringbuf.append(((HTMLPage) childItem).getTitle() + "</a></li>\n");
                }
            } else if (childItem instanceof IFileAsset) {
                if (((IFileAsset) childItem).isWorking() && !((IFileAsset) childItem).isDeleted()) {
                    stringbuf.append("<li><a href=\"" + parentFolderPath + ((IFileAsset) childItem).getFileName()
                            + "\">\n");
                    stringbuf.append(((IFileAsset) childItem).getTitle() + "</a></li>\n");
                }
            }

        }

        return stringbuf;

    }
}