package com.dotcms.xmlsitemap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.HostWebAPIImpl;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.XMLUtils;

public class XMLSitemapServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private FolderAPI folderAPI = APILocator.getFolderAPI();
	private UserAPI userAPI = APILocator.getUserAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	
	public void init(ServletConfig config) throws ServletException {

	}


	protected void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

		ServletOutputStream out = response.getOutputStream();
		try {
			if (Config.CONTEXT == null) {
				Logger.debug(this, "Link not Found");
				response.sendError(404, "Link not Found");
				return;
			}

			HostWebAPI hostWebAPI = new HostWebAPIImpl();
			Host host = hostWebAPI.getCurrentHost(request);
			String hostId = host.getIdentifier();
			List<Contentlet> itemsList = new ArrayList<Contentlet>();

			
			Folder folder = folderAPI.findFolderByPath(Config.getStringProperty("org.dotcms.XMLSitemap.XML_SITEMAPS_FOLDER","/XMLSitemaps/"), hostId, userAPI.getSystemUser(), true);
			itemsList = conAPI.findContentletsByFolder(folder, userAPI.getSystemUser(), false);

			if(itemsList.size() > 0){
				StringBuilder sitemapIndex =  new StringBuilder();
				sitemapIndex.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				sitemapIndex.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/siteindex.xsd\">");

				for (Contentlet itemChild : itemsList) {
						if (itemChild.isWorking() && !itemChild.isArchived()) {
							Identifier identifier = APILocator.getIdentifierAPI().find(itemChild);
							sitemapIndex.append("<sitemap>");
							sitemapIndex.append("<loc>"+ XMLUtils.xmlEscape("http://www."
									+ host.getHostname()
									+ UtilMethods.encodeURIComponent(identifier.getParentPath()+itemChild.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)))
							+ "</loc>");
							sitemapIndex.append("<lastmod>"+UtilMethods.dateToHTMLDate(itemChild.getModDate(), "yyyy-MM-dd")+"</lastmod>");
							sitemapIndex.append("</sitemap>");
						}
				}

				sitemapIndex.append("</sitemapindex>");
				out.print(sitemapIndex.toString());

			}else {
				Logger.debug(this, "No Index found");
				response.sendError(404, "Link not Found");
				return;
			}

		}catch(Exception e){
			Logger.error(this, "Error getting XML SiteMap Index file. "+e.getMessage());
		}finally{
			out.close();
		}
	}
}