package com.dotcms.publishing.bundlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class StaticHTMLPageBundler implements IBundler {

	private PublisherConfig config;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	FolderAPI fAPI = null;
	IdentifierAPI iAPI = null;
	HTMLPageAPI pAPI = null;
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
		pAPI = APILocator.getHTMLPageAPI();
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
				for (Identifier i : pageIdents) {
					pAPI.getHTML(i.getURI(), h, liveMode, null, uAPI.getSystemUser());
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
	
private void writeFileToDisk(File bundleRoot, String html, String uri, Host h, boolean live) throws IOException, DotBundleException{
		
		
		Host h = null;
		try{
			h = APILocator.getHostAPI().find(fileAsset.getHost(), APILocator.getUserAPI().getSystemUser(), true);

			
			FileAssetWrapper wrap = new FileAssetWrapper();
			wrap.setAsset(fileAsset);
			wrap.setInfo(APILocator.getVersionableAPI().getContentletVersionInfo(fileAsset.getIdentifier(), fileAsset.getLanguageId()));
			wrap.setId(APILocator.getIdentifierAPI().find(fileAsset.getIdentifier()));
			
			
			
			
			
			String liveworking = (fileAsset.getInode().equals(wrap.getInfo().getLiveInode() )) ? "live" : "working";
			String myFile = bundleRoot.getPath() + File.separator 
					+liveworking + File.separator 
					+ h.getHostname() 
					+ fileAsset.getURI().replace("/", File.separator) + FILE_ASSET_EXTENSION;
			File f = new File(myFile);
			
			// Should we write or is the file already there:
			Calendar cal = Calendar.getInstance();
			cal.setTime(fileAsset.getModDate());
			cal.set(Calendar.MILLISECOND, 0);
			if(f.exists() && f.lastModified() == cal.getTimeInMillis()){
				return;
			}
			String dir = myFile.substring(0, myFile.lastIndexOf(File.separator));
			new File(dir).mkdirs();
			String x  = (String) fileAsset.get("metaData");
			fileAsset.setMetaData(x);
			BundlerUtil.objectToXML(wrap, f);
			
			// set the time of the file
			f.setLastModified(cal.getTimeInMillis());
		}
		catch(Exception e){
			throw new DotBundleException("cant get host for " + fileAsset + " reason " + e.getMessage());
		}
		
		
	}

}
