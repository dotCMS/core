package com.dotcms.publishing;


public interface IPublisher {


	public PublisherConfig process() throws DotPublishingException;
	
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException;
	
}
