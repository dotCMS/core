package com.dotcms.publishing;

import java.util.List;


public interface IPublisher {

    /**
     * Method to configure any required value before to process this Publisher
     *
     * @param config Object that have the main configuration values for the processing of this Publisher
     * @return Object with this Publisher configuration info
     * @throws DotPublishingException If there is an exception while this publisher is initialized.
     */
    public PublisherConfig init ( PublisherConfig config ) throws DotPublishingException;

	public PublisherConfig process(PublishStatus status) throws DotPublishingException;
	
    /**
     * List fo Bundlers used by this Publisher
     *
     * @return List of Bundlers classes
     */
	public List<Class> getBundlers();
	
}
