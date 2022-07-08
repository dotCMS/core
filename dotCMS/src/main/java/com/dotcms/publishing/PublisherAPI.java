package com.dotcms.publishing;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * This API allows the Push Publishing mechanism in dotCMS to generate a bundle with the list of objects specified by
 * the User and potential dependent data. Such a bundle will be either pushed to another dotCMS instance, or will be
 * available for download via the back-end.
 * <p>This API also allows users to choose Push Publishing Filters when sending data. These filters control which
 * content is pushed from your sending server to your receiving server. The filters allow you to have fine-grained
 * control over what content does and does not get pushed, whether intentionally (when specifically selected) or by
 * dependency.</p>
 * <p>Bundles allow you to add and group multiple types of content (the same content items you can individually Push)
 * for later use. For instance, it is possible to create a Bundle with Folders, Pages, and content assets, and after
 * changing each of the bundled items you can remote Push the whole Bundle instead of having to push each of the
 * individual content items separately.</p>
 *
 * @author Jason Tesser
 * @since Mar 23rd, 2012
 */
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
    public PublishStatus publish ( PublisherConfig config) throws DotPublishingException;

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
    public PublishStatus publish ( PublisherConfig config, PublishStatus status) throws DotPublishingException;

    /**
     * Initializes the data structures containing the Push Publishing Filter Descriptors. This method will access the
     * location that Filter Descriptors live in, loads them and validates them so that they can be accessed by dotCMS
     * or any User with the appropriate permissions.
     */
    void initializeFilterDescriptors();

    /**
     * Loads the Filter Descriptor specified in the given path and saves it in the system.
     *
     * @param path The {@link Path} representing the location of the Filter Descriptor.
     */
    void loadFilter(final Path path);

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
     * Returns true if the filter descriptor exists
     * @return boolean
     */
    boolean existsFilterDescriptor(final String filterKey);

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

    /**
     * Clears the current list of Push Publishing Filters. This is a common operation before attempting to re-load all
     * the filters in the system. Because of that, this method must be used carefully. You need to make sure that you
     * call the {@link #initializeFilterDescriptors()} after doing this so that dotCMS re-loads the filter list.
     */
    void clearFilterDescriptorList();

    /**
     * Deletes the Filter Descriptor that matches the specified key. If the operation is successful, all Filter
     * Descriptors in the system will be re-initialized.
     *
     * @param filterKey The Filter Descriptor key.
     *
     * @return If the delete operation worked as expected, return {@code true}. Otherwise, return {@code false}.
     */
    boolean deleteFilterDescriptor(final String filterKey);

    /**
     * Saves or updates a Filter Descriptor. If the operation is successful, all Filter Descriptors in the system will
     * be re-initialized.
     *
     * @param filterDescriptor The {@link FilterDescriptor} object that will be saved or updated.
     */
    void upsertFilterDescriptor(final FilterDescriptor filterDescriptor);

    /**
     * Saves or updates the specified list of new Filter Descriptors in the form of files.
     *
     * @param filterFiles The list of {@link File} objects representing a Filter Descriptor.
     */
    void saveFilterDescriptors(final List<File> filterFiles);

}
