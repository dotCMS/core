package com.dotcms.publishing;

public class StaticPublisher extends Publisher {
	@Override
	public PublisherConfig process() throws DotPublishingException {
		return config;
	}
}
