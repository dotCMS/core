package com.dotcms.publishing.bundlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class StaticHTMLPageBundler implements IBundler {

	private PublisherConfig config;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	FolderAPI fAPI = null;
	IdentifierAPI iAPI = null;
	User systemUser = null;
	
	@Override
	public String getName() {
		return "Static HTML Page Bundler";
	}
	
	@Override
	public void setConfig(PublisherConfig pc) {
		config = pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		fAPI = APILocator.getFolderAPI();
		iAPI = APILocator.getIdentifierAPI();
		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(this,e.getMessage(),e);
		}
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException{
		boolean include = true;
		boolean hasPatterns = false;
		List<String> patterns = null;
		List<Identifier> pageIdents = new ArrayList<Identifier>();
		if(config.getExcludePatterns()!=null && config.getExcludePatterns().size()>0){
			hasPatterns = true;
			include = false;
			patterns = config.getExcludePatterns();
		}else if(config.getIncludePatterns()!=null && config.getIncludePatterns().size()>0){
			hasPatterns = true;
			include = true;
			patterns = config.getIncludePatterns();
		}
				
		try{
			for(Host h : config.getHosts()){
				if(!hasPatterns){
					try{
						pageIdents.addAll(iAPI.findByURIPattern(new HTMLPage().getType(), "/*", include, h, config.getStartDate(), config.getEndDate()));
					}catch (NullPointerException e) {}
				}else{
					for(String pattern : patterns){
						try{
							pageIdents.addAll(iAPI.findByURIPattern(new HTMLPage().getType(),pattern , include, h, config.getStartDate(), config.getEndDate()));
						}catch (NullPointerException e) {}
					}
				}
			}
		}catch (DotDataException e) {
			Logger.error(this, e.getMessage() + " : Unable to get Pages for Start HTML Bundler",e);
		}
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
