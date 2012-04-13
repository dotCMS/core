package com.dotcms.publishing;

public interface PublisherAPI {

	
	public PublishStatus publish(PublisherConfig config) throws DotPublishingException;

	PublishStatus publish(PublisherConfig config, PublishStatus status) throws DotPublishingException;
	
	
	
	
	
	
}
