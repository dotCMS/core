package com.dotcms.publishing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.bundlers.BundlerUtil;
import com.dotcms.publishing.bundlers.FileObjectBundler;

public class ESSiteSearchPublisher extends Publisher {

	
	public static final String SITE_SEARCH_INDEX = "SITE_SEARCH_INDEX"; 
	
	
	
	
	
	
	@Override
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException{

		if(!(config instanceof SiteSearchConfig)){
			
			//throw new DotPublishingException("Config if not a SiteSearchConfig");
		}
		this.config = super.init(config);
		
		
		if(config.get(SITE_SEARCH_INDEX) ==null){
			
			
		}
		
		
		
		
		
		
		
		return this.config;
		
	}
	
	
	
	@Override
	public PublisherConfig process() throws DotPublishingException {

		File bundleRoot = BundlerUtil.getBundleRoot(config);
		
		
		
		
		
		
		
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
