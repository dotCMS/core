package com.dotcms.publishing;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;

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

	abstract public PublisherConfig process() throws DotPublishingException ;



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
	
	
	
	

}
