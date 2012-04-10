package com.dotcms.publishing.bundlers;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.util.List;

import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

public class StaticURLMapBundler implements IBundler {

	@Override
	public String getName() {
		return "Static URL Map Bundler";
	}
	
	@Override
	public void setConfig(PublisherConfig pc) {
		// TODO Auto-generated method stub

	}

	@Override
	public void generate(File bundleRoot,BundlerStatus status) throws DotBundleException{
		try{
		//Get APIs

		ContentletAPI capi = APILocator.getContentletAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();
		// Pulling Content
		String query ="+structureName:Products +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +live:true";
		List<Contentlet> cons = capi.search(query, 100, 0,"modDate", APILocator.getUserAPI().getSystemUser(), false);
		String staticPath = "/_static";
		new File(Config.CONTEXT.getRealPath(staticPath)).mkdirs();

		for(Contentlet c : cons){
			
			String url = capi.getUrlMapForContentlet(c,systemUser, false );
			String hostId = url.substring(url.indexOf("?host_id=") + 9, url.length());
			Structure struc = c.getStructure();
			String detailPageId = struc.getDetailPage();
			HTMLPage htmlPage = APILocator.getHTMLPageAPI().loadLivePageById(detailPageId, systemUser, false);
			String pageString = APILocator.getHTMLPageAPI().getHTML(htmlPage, true, c.getInode(), systemUser);
			url = url.substring(0, url.indexOf("?") );
			String subfolder =Config.CONTEXT.getRealPath(staticPath +  url.substring(0, url.lastIndexOf("/")));
			String filePath =Config.CONTEXT.getRealPath(staticPath +  url);
			new File(subfolder).mkdirs();
			
			FileWriter fout = new FileWriter(filePath);
			fout.write(pageString);
			fout.close();

	
		}
		
		}
		catch(Exception e){
			
		}
		
	}
	@Override
	public FileFilter getFileFilter(){
		return null;
		
	}
	
}
