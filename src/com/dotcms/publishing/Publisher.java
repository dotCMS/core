package com.dotcms.publishing;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;

import edu.emory.mathcs.backport.java.util.Arrays;

public abstract class Publisher implements IPublisher {

	protected PublisherConfig config;

	
	/**
	 * This method gets called before any publisher processes
	 * the config
	 */
	public PublisherConfig init(PublisherConfig config)throws DotPublishingException {
		
		this.config = config;
		
		return config;
	}
	
	abstract public PublisherConfig process(PublishStatus status) throws DotPublishingException ;



	protected void processDirectory(Folder folder) {

	}
	
	
	protected void processStructure(Structure struct) {

	}
	// returns true if an asset/path should be included (DENY, ALLOW)
	protected boolean includeAsset(String path) {
		return true;
		
		
		
	}
	
	// gets a fake request object with language and user set
	protected HttpServletRequest constructRequest() {

		return null;

	}

	// gets a fake response object
	protected HttpServletResponse constructResponse() {
		return null;
	}
	
	
	protected boolean bundleContains(String file){
		return false;
	}
	
	
	

	public Host getHostFromFilePath(File file) throws DotPublishingException{
		
		try{
			if(!file.getAbsolutePath().contains(config.getId())){
				throw new DotPublishingException("no bundle file found");
			}
		
			
			List<String> path = Arrays.asList(file.getAbsolutePath().split(File.separator));
			String host = path.get(path.indexOf(config.getId())+2); 
			
			return APILocator.getHostAPI().resolveHostName(host, APILocator.getUserAPI().getSystemUser(), true);
		}
		catch(Exception e){
			throw new DotPublishingException("error getting host:" + e.getMessage());
		}
		
	}
	
	public String getUriFromFilePath(File file) throws DotPublishingException{
		
		try{
			if(!file.getAbsolutePath().contains(config.getId())){
				throw new DotPublishingException("no bundle file found");
			}
		
			
			List<String> path = Arrays.asList(file.getAbsolutePath().split(File.separator));
			path = path.subList(path.indexOf(config.getId())+3, path.size());
			StringBuilder bob = new StringBuilder();
			for(String x:path){
				bob.append("/" + x);
			}
			return bob.toString();

		}
		catch(Exception e){
			throw new DotPublishingException("error getting host:" + e.getMessage());
		}
		
	}
	public String getUrlFromFilePath(File file) throws DotPublishingException{
		
		return getHostFromFilePath(file).getHostname()  + getUriFromFilePath(file);
		
	}
	
	
	
	protected boolean shouldProcess(File f) throws IOException, DotPublishingException{
		if(f.isDirectory()){
			return false;
		}else if(config.getStartDate() != null && f.lastModified() <  config.getStartDate().getTime()){	
			return false;
		
		}else if(config.getEndDate() != null && f.lastModified() >  config.getEndDate().getTime()){	
			return false;
		}
		
		
		
		String filePath = getUriFromFilePath(f);
		
		
		
		
		
		filePath = filePath.replace(File.pathSeparatorChar, '/');
		if(config.getIncludePatterns() != null){
			for(String x : config.getIncludePatterns()){
				boolean startsWith = x.startsWith("*");
				boolean endsWith = x.endsWith("*");
				x = x.replace("*", "");


				if(endsWith && filePath.indexOf(x)!=0){
					return false;
				}
				else if(startsWith && !filePath.contains(x + ".")){
					return false;
				}
			}
		}
		if(config.getExcludePatterns() != null){
			for(String x : config.getExcludePatterns()){
				boolean startsWith = x.startsWith("*");
				boolean endsWith = x.endsWith("*");
				x = x.replace("*", "");

				if(endsWith && filePath.indexOf(x)==0){
					return false;
				}
				else if(startsWith && filePath.contains(x + ".")){
					return false;
				}
				
			}
		}
		
		return true;
	}
	
	
}
