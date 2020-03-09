package com.dotcms.publishing;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;

public interface PublisherAPI {

    /**
     * This method call will create and send to an specified Environments a Bundle, in order to do that it will follow this main steps:<br/>
     * <ul>
     * <li>Calls all the registered IBundlers associated to a specified Publisher ({@link com.dotcms.publishing.Publisher#getBundlers()})<br/>
     * in order generate for each of them the different parts and objects of the bundle we are trying to create.</li>
     * <li>The Publisher will call it process method ({@link com.dotcms.publishing.Publisher#process(PublishStatus)}) in order to compress<br/>
     * all the generated content by the Bundlers and create the final Bundle file that will be send to a specific Environment.</li>
     * </ul>
     *
     * @param config Class that have the main configuration values for the Bundle we are trying to create
     * @return The status of the Bundle creation process
     * @throws DotPublishingException If there is an exception on the publish process
     * @see IBundler
     * @see Publisher
     * @see com.dotcms.publisher.environment.bean.Environment
     */
    public PublishStatus publish ( PublisherConfig config ) throws DotPublishingException;

    /**
     * This method call will create and send to an specified Environments a Bundle, in order to do that it will follow this main steps:<br/>
     * <ul>
     * <li>Calls all the registered IBundlers associated to a specified Publisher ({@link com.dotcms.publishing.Publisher#getBundlers()})<br/>
     * in order generate for each of them the different parts and objects of the bundle we are trying to create.</li>
     * <li>The Publisher will call it process method ({@link com.dotcms.publishing.Publisher#process(PublishStatus)}) in order to compress<br/>
     * all the generated content by the Bundlers and create the final Bundle file that will be send to a specific Environment.</li>
     * </ul>
     *
     * @param config Class that have the main configuration values for the Bundle we are trying to create
     * @param status Object that store the status of the Bundle on all of its stages
     * @return The status of the Bundle creation process
     * @throws DotPublishingException If there is an exception on the publish process
     * @see IBundler
     * @see Publisher
     * @see com.dotcms.publisher.environment.bean.Environment
     */
    public PublishStatus publish ( PublisherConfig config, PublishStatus status ) throws DotPublishingException;

    /**
     * Adds a filter to the map of filters, using the filterDescriptor.Key as the key
     * @param filterDescriptor
     */
    void addFilter(final FilterDescriptor filterDescriptor);

    List<FilterDescriptor> getFiltersByRole(final User user) throws DotDataException;

    Map<String, FilterDescriptor> getFilterMap();

    FilterDescriptor getFilterDescriptorByKey(final String filterKey);

    PublisherFilter createPublisherFilter(final String bundleId) throws DotDataException, DotSecurityException;
}