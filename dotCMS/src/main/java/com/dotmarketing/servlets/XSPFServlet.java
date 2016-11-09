package com.dotmarketing.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.files.model.MP3File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.XMLUtils;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.util.FileUtil;

public class XSPFServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	

	@Override
	public void init() throws ServletException {
		java.io.File dir = new java.io.File(FileUtil.getRealPath("/WEB-INF/velocity/static/xspf/"));
		dir.mkdirs();
	}

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {

		Host host;
		try {
			host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		} catch (PortalException e1) {
			Logger.error(XSPFServlet.class, e1.getMessage(), e1);
			throw new ServletException(e1.getMessage(), e1);
		} catch (SystemException e1) {
			Logger.error(XSPFServlet.class, e1.getMessage(), e1);
			throw new ServletException(e1.getMessage(), e1);
		} catch (DotDataException e1) {
			Logger.error(XSPFServlet.class, e1.getMessage(), e1);
			throw new ServletException(e1.getMessage(), e1);
		} catch (DotSecurityException e1) {
			Logger.error(XSPFServlet.class, e1.getMessage(), e1);
			throw new ServletException(e1.getMessage(), e1);
		}
		String path = request.getParameter("path");

		if (path == null) {
			return;
		}

		path = (path.endsWith("/")) ? path : path + "/";

		Folder folder = new Folder();
		try {
			folder = APILocator.getFolderAPI().findFolderByPath(path, host, APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e1) {
			Logger.error(this,e1.getMessage(), e1);
		} 
		List<File> mp3Files = InodeFactory.getChildrenClassByConditionAndOrderBy(folder, File.class,
				"lower(file_name) like '%.mp3'", "sort_order");
/*
		java.io.File cachedFile = new java.io.File(FileUtil.getRealPath("/WEB-INF/velocity/static/xspf/"
				+ URLEncoder.encode(path, "UTF-8") + ".xml"));
		boolean useCache = cachedFile.exists();

		if (useCache) {
			Date d = new Date(cachedFile.lastModified());
			for (File file : mp3Files) {
				Date modDate = file.getModDate();
				if (modDate.after(d)) {
					useCache = false;
				}
			}
		}
*/
		response.setContentType("text/xml");
		PrintWriter out = response.getWriter();
		/*
		useCache = false;
		if (useCache) {
			BufferedReader fin = new BufferedReader(new FileReader(cachedFile));
			while (fin.ready()) {
				out.println(fin.readLine());
			}
			fin.close();
			out.close();
			return;
		}
*/
		StringWriter sw = new StringWriter();
		//FileWriter fout = new FileWriter(cachedFile);

		try {
			sw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

			sw.write("<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\">\n");
			sw.write("<trackList>");

			for (File file : mp3Files) {
				// only show live files
				if (!file.isLive()) {
					continue;
				}
				MP3File mp3 = new MP3File(file);

				sw.write("<track>\n");
				sw.write("<location>http://" + host.getHostname() + APILocator.getIdentifierAPI().find(folder).getPath() + file.getFileName()
						+ "</location>\n");

				if (UtilMethods.isSet(mp3.getArtist())) {
					sw.write("<creator>" + XMLUtils.xmlEscape(mp3.getArtist()) + "</creator>\n");
				}
				if (UtilMethods.isSet(mp3.getAlbum())) {
					sw.write("<album>" + XMLUtils.xmlEscape(mp3.getAlbum()) + "</album>\n");
				}

				if (UtilMethods.isSet(mp3.getTitle())) {
					sw.write("<title>" + XMLUtils.xmlEscape(mp3.getTitle()) + "</title>\n");
				}
				if (UtilMethods.isSet(mp3.getGenre())) {
					sw.write("<genre>" + XMLUtils.xmlEscape(mp3.getGenre()) + "</genre>\n");
				}

				if (mp3.getDuration() > 0) {
					sw.write("<duration>" + mp3.getDuration() + "</duration>\n");
				}
				sw.write("<image>http://" + host.getHostname() + "/global/images/mp3logo.jpg</image>\n");

				sw.write("</track>\n");

			}

			sw.write("</trackList>\n");
			sw.write("</playlist>\n");
		} catch (Exception e) {

			Logger.error(this,e.getMessage(),e);
		} finally {
			out.print(sw.toString());
			out.close();

		//	fout.write(sw.toString());
		//	fout.close();
			sw.close();
		}
	}

}
