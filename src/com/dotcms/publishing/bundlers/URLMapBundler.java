package com.dotcms.publishing.bundlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.output.FileWriterWithEncoding;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.publishing.BundlerStatus;
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


	public final static String  FILE_ASSET_EXTENSION = ".dotUrlMap" ;
	
	
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
		
		if(structsToAdd.size() ==0){

			if(config.getExcludePatterns() != null && config.getExcludePatterns().size()>0){
				for (String p : config.getExcludePatterns()) {
					if(!UtilMethods.isSet(p)){
						continue;
					}
					p = p.replace(" ", "+");
					bob.append("-uri:" + p + " ");
				}
			}else if(config.getIncludePatterns() != null && config.getIncludePatterns().size()>0){
				for (String p : config.getIncludePatterns()) {
					if(!UtilMethods.isSet(p)){
						continue;
					}
					p = p.replace(" ", "+");
					bob.append("+uri:" + p + " ");
				}
			}

		}
		else{
			if(structsToAdd.size() > 0){
				bob.append("+(" );
				for(String s : structsToAdd){
					
					
					Structure struc = StructureFactory.getStructureByInode(s);
					

					
					bob.append("structureName:" + struc.getVelocityVarName() + " ");
				}
				bob.append(") " );
			}
			
		}
		
		


		
		
		if(config.getStartDate() != null){
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 2500);
			String start = ESMappingAPIImpl.datetimeFormat.format(config.getStartDate());
			String forever = ESMappingAPIImpl.datetimeFormat.format(cal.getTime());
			bob.append(" +moddate:[" + start + " TO " + forever +"] ");
		}
		
		if(config.getEndDate() != null){
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 1900);
			
			String end = ESMappingAPIImpl.datetimeFormat.format(config.getEndDate());
			String longAgo = ESMappingAPIImpl.datetimeFormat.format(cal.getTime());
			bob.append(" +moddate:[" + longAgo + " TO " + end +"] ");
		}
		
		
		
		if(config.getHosts() != null && config.getHosts().size() > 0){
			
			for(Host h : config.getHosts()){
				bob.append(" +conhost:" + h.getIdentifier() + " ");
			}
		}
		
		
		
		

		try {
			cs = capi.searchIndex(bob.toString() + " +live:true ", 0, 0, "moddate", systemUser, true);
			if(!config.liveOnly()){
				cs.addAll(capi.searchIndex(bob.toString() + "+working:true", 0, 0, "moddate", systemUser, true));
			}
		} catch (Exception e) {
			
			Logger.error(FileObjectBundler.class,e.getMessage(),e);
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
				Logger.error(FileObjectBundler.class,e.getMessage(),e);
				throw new DotBundleException(this.getClass().getName() + " : " + "generate()" + e.getMessage() + ": Unable to retrieve content", e);
			}
			
			for (Contentlet con : cons) {
				try {
					writeFileToDisk(bundleRoot, con);
					status.addCount();
				}
				catch(Exception e){
					Logger.warn(this.getClass(), "Bundle Failed: " + e.getMessage());
					
				}
			}
			


	
		}
		
		}
		catch(Exception e){
			
		}
		
	}
	private void writeFileToDisk(File bundleRoot, Contentlet contentlet) throws IOException, DotBundleException{
		
		if(contentlet ==null){
			throw new DotBundleException("null contentlet passed in ");
		}
		Calendar cal = Calendar.getInstance();
		File f = null;
		BufferedWriter w = null;
		try{
			Host h  = APILocator.getHostAPI().find(contentlet.getHost(), APILocator.getUserAPI().getSystemUser(), true);
			
			

			String url = capi.getUrlMapForContentlet(contentlet,APILocator.getUserAPI().getSystemUser(), true );

			if(url ==null){
				throw new DotBundleException("contentlet:" + contentlet.getInode() + " does not have a urlmap");
			}
			//url = FileUtil.sanitizeFileName(url);

			ContentletVersionInfo info = APILocator.getVersionableAPI().getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());
			String liveworking = (contentlet.getInode().equals(info.getLiveInode() )) ? "live" : "working";

			String myFile = bundleRoot.getPath() + File.separator 
					+liveworking + File.separator 
					+ h.getHostname() 
					+ url.replace("/", File.separator) + FILE_ASSET_EXTENSION;
			 f = new File(myFile);
			
			// Should we write or is the file already there:
			
			cal.setTime(contentlet.getModDate());
			cal.set(Calendar.MILLISECOND, 0);
			if(f.exists() && f.lastModified() == cal.getTimeInMillis()){
				return;
			}
			
			Structure struc = contentlet.getStructure();
			String detailPageId = struc.getDetailPage();
			HTMLPage htmlPage = APILocator.getHTMLPageAPI().loadLivePageById(detailPageId, APILocator.getUserAPI().getSystemUser(), false);
			String pageString = APILocator.getHTMLPageAPI().getHTML(htmlPage, true, contentlet.getInode(), APILocator.getUserAPI().getSystemUser());
			
			String dir = myFile.substring(0, myFile.lastIndexOf(File.separator));
			new File(dir).mkdirs();

			w = new BufferedWriter(new FileWriterWithEncoding(f, "UTF-8"));
			w.write(pageString);
			
			// set the time of the file
			
		}
		catch(Exception e){
			throw new DotBundleException("cant get host for " + contentlet + " reason " + e.getMessage());
		}finally{
			if(w != null){
				try{
					w.close();
					f.setLastModified(cal.getTimeInMillis());
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
