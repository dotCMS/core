package com.dotcms.publishing.bundlers;

import java.io.File;

import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;

public class StaticHTMLPageBundler implements IBundler {

	@Override
	public String getName() {
		return "Static HTML Page Bundler";
	}
	
	@Override
	public void setConfig(PublisherConfig pc) {
		// TODO Auto-generated method stub

	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException{
		
		/** CODE FROM JSP
		<%@page import="com.dotmarketing.beans.Identifier"%>
<%@page import="com.dotmarketing.portlets.folders.model.Folder"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.dotmarketing.portlets.folders.business.FolderAPI"%>
<%@page import="com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI"%>
<%@page import="java.io.FileWriter"%>
<%@page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="java.io.File"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>

<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%
//Get APIs

HTMLPageAPI hapi = APILocator.getHTMLPageAPI();
FolderAPI fapi = APILocator.getFolderAPI();
HostAPI hostApi  = APILocator.getHostAPI();



ContentletAPI capi = APILocator.getContentletAPI();
User systemUser = APILocator.getUserAPI().getSystemUser();



Folder folder = fapi.findFolderByPath("/about-us", hostApi.findDefaultHost(systemUser, true), systemUser, true);
List <HTMLPage> pages = fapi.getLiveHTMLPages(folder, systemUser, true);
String staticPath = "/_static";
new File(Config.CONTEXT.getRealPath(staticPath)).mkdirs();
for(HTMLPage htmlPage : pages){

	Identifier id = APILocator.getIdentifierAPI().find(htmlPage.getIdentifier());
	String pageString = APILocator.getHTMLPageAPI().getHTML(htmlPage, true);
	String url = id.getPath();
	String subfolder =Config.CONTEXT.getRealPath(staticPath +  url.substring(0, url.lastIndexOf("/")));
	String filePath =Config.CONTEXT.getRealPath(staticPath +  url);
	new File(subfolder).mkdirs();
	FileWriter fout = new FileWriter(filePath);
	fout.write(pageString);
	fout.close();
	
}

%>
		 */
		
	}

}
