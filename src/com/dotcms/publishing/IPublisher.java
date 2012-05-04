package com.dotcms.publishing;

import java.util.List;


public interface IPublisher {


	public PublisherConfig process(PublishStatus status) throws DotPublishingException;
	
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException;
	
	public List<Class> getBundlers();
	
	
	
	
}
