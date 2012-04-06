package com.dotcms.publishing;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.bundlers.FileObjectBundler;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;

public class ESSiteSearchPublisher extends Publisher {

	
	public static final String SITE_SEARCH_INDEX = "SITE_SEARCH_INDEX"; 
	
	
	
	
	
	
	@Override
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException{
	
		this.config = super.init(config);
		
		
		if(config.get(SITE_SEARCH_INDEX) ==null){
			
			
		}
		
		
		
		
		
		
		
		return this.config;
		
	}
	
	
	
	@Override
	public PublisherConfig process() throws DotPublishingException {

		List<Host> hosts = config.getHosts();
		List<Folder> folders = config.getFolders();
		List<Structure> strucs = config.getStructures();
		
		
		if(hosts == null || hosts.size() ==0){
			try {
				hosts = APILocator.getHostAPI().findAll(config.getUser(), true);
			} catch (Exception e) {
				Logger.error(ESSiteSearchPublisher.class,e.getMessage(),e);
				throw new DotPublishingException(e.getMessage());
			} 
		}
		
		if(strucs == null || strucs.size() ==0){
			strucs = StructureFactory.getStructures();
		}
		
		
		for(Host h : hosts){
			
			
			
			
		}
		
		
		for(Folder f : folders){
			
			
			
			
		}
		
		
		for(Structure s : strucs){
			
			
			
			
		}

		
		
		
		return config;
		
		
		
		
		
		
		
		
		
	}



	@Override
	public List<Class> getBundlers() {
		List<Class> list = new ArrayList<Class>();
		
		list.add(FileObjectBundler.class);
		//list.add(StaticHTMLPageBundler.class);
		//list.add(StaticURLMapBundler.class);
		return list;
	}
	
	
	

	
	
	
	
	
	
	

}
