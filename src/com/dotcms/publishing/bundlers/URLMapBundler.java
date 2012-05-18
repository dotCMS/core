package com.dotcms.publishing.bundlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.output.FileWriterWithEncoding;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;

public class URLMapBundler implements IBundler {

	

	private PublisherConfig config;
	ContentletAPI capi = APILocator.getContentletAPI();


	public final static String  FILE_ASSET_EXTENSION = ".dotUrlMap.xml" ;
	
	
	@Override
	public String getName() {
		return "Static URL Map Bundler";
	}
	
	@Override
	public void setConfig(PublisherConfig pc) {
		this.config = pc;

	}

	@Override
	public void generate(File bundleRoot,BundlerStatus status) throws DotBundleException{
		try{
		//Get APIs

		
		User systemUser = APILocator.getUserAPI().getSystemUser();
		List<ContentletSearch> cs = new ArrayList<ContentletSearch>();
		

		List<String> structsToAdd = new ArrayList<String>();
		if(config.getStructures() ==null ){
			List<SimpleStructureURLMap> urlMaps = StructureFactory.findStructureURLMapPatterns();
			for(SimpleStructureURLMap map : urlMaps){
				structsToAdd.add(map.getInode());
			}
		}else{
			for(Structure s : config.getStructures()){
				structsToAdd.add(s.getInode());
			}
		}
		StringBuilder bob = new StringBuilder("+languageid:" + config.getLanguage() + " " );
		
		// if we have no urlmap structures...
		if(structsToAdd.size() ==0){
			return;
		}


		bob.append("+(" );
		for(String s : structsToAdd){
			Structure struc = StructureFactory.getStructureByInode(s);
			bob.append("structureName:" + struc.getVelocityVarName() + " ");
		}
		bob.append(") " );

		if(config.getExcludePatterns() != null && config.getExcludePatterns().size()>0){
			bob.append("-(" );
			for (String p : config.getExcludePatterns()) {
				if(!UtilMethods.isSet(p)){
					continue;
				}
				//p = p.replace(" ", "+");
				bob.append("urlMap:" + p + " ");
			}
			bob.append(") " );
		}else if(config.getIncludePatterns() != null && config.getIncludePatterns().size()>0){
			bob.append("+(" );
			for (String p : config.getIncludePatterns()) {
				if(!UtilMethods.isSet(p)){
					continue;
				}
				//p = p.replace(" ", "+");
				bob.append("urlMap:" + p + " ");
			}
			bob.append(") " );
		}
		
			
		
		
		


		
		
		if(config.getStartDate() != null){
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 2500);
			Date d = (Date) config.getStartDate();
			String start = ESMappingAPIImpl.datetimeFormat.format(config.getStartDate());
			String forever = ESMappingAPIImpl.datetimeFormat.format(cal.getTime());
			bob.append(" +versionts:[" + start + " TO " + forever +"] ");
		}
		
		if(config.getEndDate() != null){
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 1900);
			Date d = (Date) config.getEndDate();
			String end = ESMappingAPIImpl.datetimeFormat.format(config.getEndDate());
			String longAgo = ESMappingAPIImpl.datetimeFormat.format(cal.getTime());
			bob.append(" +versionts:[" + longAgo + " TO " + end +"] ");
		}
		
		
		
		if(config.getHosts() != null && config.getHosts().size() > 0){
			bob.append(" +(" );
			for(Host h : config.getHosts()){
				bob.append("conhost:" + h.getIdentifier() + " ");
			}
			bob.append(" ) " );
		}
		
		
		
		
		Logger.info(this.getClass(),bob.toString());
		try {
			cs = capi.searchIndex(bob.toString() + " +live:true ", 0, 0, "moddate", systemUser, true);
			//if(!config.liveOnly()){
				cs.addAll(capi.searchIndex(bob.toString() + "+working:true", 0, 0, "moddate", systemUser, true));
			//}
		} catch (Exception e) {
			
			Logger.error(this.getClass(),e.getMessage(),e);
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()" + e.getMessage() + ": Unable to pull content with query " + bob.toString(), e);
		}
		
		List<List<ContentletSearch>> listsOfCS = Lists.partition(cs, 500);
		for (List<ContentletSearch> l : listsOfCS) {
			List<String> inodes = new ArrayList<String>();
			for (ContentletSearch c : l) {
				inodes.add(c.getInode());
			}
			List<Contentlet> cons = new ArrayList<Contentlet>();
			try {
				cons = capi.findContentlets(inodes);
			} catch (Exception e) {
				Logger.error(this.getClass(),e.getMessage(),e);
				throw new DotBundleException(this.getClass().getName() + " : " + "generate()" + e.getMessage() + ": Unable to retrieve content", e);
			}
			status.setTotal(cons.size());
			for (Contentlet con : cons) {
				try {
					writeFileToDisk(bundleRoot, con);
					status.addCount();
				}
				catch(Exception e){
					Logger.warn(this.getClass(), "Bundle Failed: " + e.getMessage());
					status.addFailure();
				}
			}
		}
		
		}
		catch(Exception e){
			status.addFailure();
		}
		
	}
	private void writeFileToDisk(File bundleRoot, Contentlet contentlet) throws IOException, DotBundleException{
		
		if(contentlet ==null){
			throw new DotBundleException("null contentlet passed in ");
		}
		Calendar cal = Calendar.getInstance();

		BufferedWriter w = null;
		URLMapWrapper wrap = new URLMapWrapper();
		try{
			Host h  = APILocator.getHostAPI().find(contentlet.getHost(), APILocator.getUserAPI().getSystemUser(), true);
			File f = null;
			

			String url = capi.getUrlMapForContentlet(contentlet,APILocator.getUserAPI().getSystemUser(), true );

			if(url ==null){
				throw new DotBundleException("contentlet:" + contentlet.getInode() + " does not have a urlmap");
			}
			//url = FileUtil.sanitizeFileName(url);

			ContentletVersionInfo info = APILocator.getVersionableAPI().getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());
			
			wrap.setInfo(info);
			wrap.setContent(contentlet);
			wrap.setId(APILocator.getIdentifierAPI().find(contentlet.getIdentifier()));
			
			
			String liveworking = (contentlet.getInode().equals(info.getLiveInode() )) ? "live" : "working";

			String myFileUrl = bundleRoot.getPath() + File.separator 
					+liveworking + File.separator 
					+ h.getHostname() 
					+ url.replace("/", File.separator) ;
			 f = new File(myFileUrl);
			
			// Should we write or is the file already there:
			
			cal.setTime(info.getVersionTs());
			cal.set(Calendar.MILLISECOND, 0);
			if(!f.exists() || f.lastModified() != cal.getTimeInMillis()){
				Structure struc = contentlet.getStructure();
				String detailPageId = struc.getDetailPage();
				HTMLPage htmlPage = APILocator.getHTMLPageAPI().loadLivePageById(detailPageId, APILocator.getUserAPI().getSystemUser(), false);
				String pageString = APILocator.getHTMLPageAPI().getHTML(htmlPage, true, contentlet.getInode(), APILocator.getUserAPI().getSystemUser());
				
				String dir = myFileUrl.substring(0, myFileUrl.lastIndexOf(File.separator));
				new File(dir).mkdirs();
	
				if ( f.exists() )
					f.delete();
				
				w = new BufferedWriter(new FileWriterWithEncoding(f, "UTF-8"));
				w.write(pageString);
				w.close();
				f.setLastModified(cal.getTimeInMillis());
				w=null;
			}
			f = new File(f.getAbsolutePath() + FILE_ASSET_EXTENSION);
			if(!f.exists() || f.lastModified() != cal.getTimeInMillis()){
				BundlerUtil.objectToXML(wrap, f, true);
				f.setLastModified(cal.getTimeInMillis());
			}
			
			// set the time of the file
			
		}
		catch(Exception e){
			throw new DotBundleException("cant get host for " + contentlet + " reason " + e.getMessage());
		}finally{
			if(w != null){
				try{
					w.close();
				}
				catch(Exception ex){
					
				}
			}
		}
		
		
	}
	
	@Override
	public FileFilter getFileFilter(){
		return new URLMapBundlerFilter();
		
	}
	
	
	
	
	
	public class URLMapBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(FILE_ASSET_EXTENSION));
		}

	}
	
}
