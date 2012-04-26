package com.dotcms.publishing;

import java.util.List;


public class StaticPublisher extends Publisher {
	@Override
	public PublisherConfig process() throws DotPublishingException {
		return config;
	}

	@Override
	public List<Class> getBundlers() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
}
