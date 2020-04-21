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
    void addFilterDescriptor(final FilterDescriptor filterDescriptor);

    /**
     * Gets all the filters that user has access to.
     * @param user User that is trying to get the Filters.
     * @return list of filterDescriptors that the user has access.
     * @throws DotDataException
     */
    List<FilterDescriptor> getFiltersDescriptorsByRole(final User user) throws DotDataException;

    /**
     * Gets the FilterMap that contains all the Filters loaded to the system.
     * @return map of FilterDescriptors, the FilterDescriptor.Key is used as the key of the map.
     */
    Map<String, FilterDescriptor> getFilterDescriptorMap();

    /**
     * Get a FilterDescriptor using the FilterDescriptor.Key as key
     * @param filterKey key of the filterDescriptor
     * @return filterDescriptor referenced to that key
     */
    FilterDescriptor getFilterDescriptorByKey(final String filterKey);

    /**
     * Creates and returns a PublisherFilter (that is an object that contains the filters of a filterDescriptor)
     * @param bundleId BundleId of the bundle that is gonna be created.
     * @return PublisherFilter with the filters of the FilterDescriptor that was selected for that bundle.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    PublisherFilter createPublisherFilter(final String bundleId) throws DotDataException, DotSecurityException;
}