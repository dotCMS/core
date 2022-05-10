package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Provides access to information related to Sites in dotCMS.
 * <p>
 * A single dotCMS instance may manage multiple different web sites. Each “site” is actually a separate website, but
 * all sites are managed and the content for all sites is served from a single dotCMS instance. A single dotCMS server
 * can manage literally hundreds of sites.
 * </p>
 *
 * @author Jose Castro
 * @version 22.04
 * @since Mar 15, 2022
 */
public interface HostFactory {

    /**
     * Finds a Site in the repository, based on its name. If it cannot be found or if an error ocurred, the "default"
     * Site will be returned instead.
     *
     * @param siteName The name of the Site
     *
     * @return The Site with the specified name.
     */
    Host bySiteName(final String siteName);

    /**
     * Returns the Site that matches the specified alias. Depending on the existing data, the result may vary:
     * <ol>
     *  <li>If one single Site matches the alias, then such a Site will be returned.</li>
     *  <li>If no Site matches the alias, then a {@code null} value is returned.</li>
     *  <li>If two or more Sites matches the alias, then:
     *  <ol>
     *      <li>If one of those Sites is the "deault" Site, then it will be returned.</li>
     *      <li>Otherwise, the first Site in the result set is returned.</li>
     *      </ol>
     *  </li>
     * </ol>
     *
     * @param alias The alias of the Site.
     *
     * @return The {@link Host} object matching the alias, the "default" Site, or the first Site from the result set.
     */
    Host byAlias(final String alias);

    /**
     * Returns the list of Sites in your dotCMS repository retrieved <b>directly from the data source</b> matching the
     * specified search criteria.
     *
     * @return The list of {@link Host} objects.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    List<Host> findAll() throws DotDataException, DotSecurityException;

    /**
     * Returns the list of Sites in your dotCMS repository retrieved <b>directly from the data source</b> matching the
     * specified search criteria.
     *
     * @param limit   Limit of results returned in the response, for pagination purposes. If set equal or lower than
     *                zero, this parameter will be ignored.
     * @param offset  Expected offset of results in the response, for pagination purposes. If set equal or lower than
     *                zero, this parameter will be ignored.
     * @param orderBy Optional sorting criterion, as specified by the available columns in: {@link
     *                com.dotmarketing.common.util.SQLUtil#ORDERBY_WHITELIST}
     *
     * @return The list of {@link Host} objects.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    List<Host> findAll(final int limit, final int offset, final String orderBy) throws DotDataException, DotSecurityException;

    /**
     * Retrieves the System Host object.
     *
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The System Host instance.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    Host findSystemHost(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * Creates the System Host object, in case it doesn't exist already.
     *
     * @return The System Host instance.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    Host createSystemHost() throws DotDataException, DotSecurityException;

    /**
     * Searches for a Site that matches the specified ID.
     *
     * @param id                   The Identifier of the Site
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The {@link Host} object representing the specified ID.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    Host DBSearch(final String id, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * <b>NOTE: Use with caution.</b> This method deletes the specified Site plus all assets under it. It has the
     * ability to do all the job in a separated thread, and returns immediately. It returns an optional Future that
     * returns {@code true} when the {@code runAsSeparatedThread} parameter is {@code true}).
     *
     * @param site                 The Site being deleted.
     * @param deletingUser         The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     * @param runAsSeparatedThread If a separtae thread will be created for deleting the Site, set to {@code true}.
     *                             Otherwise, set to {@code false}.
     *
     * @return The {@link Future} object contain the result of the deletion process.
     */
    Optional<Future<Boolean>> delete(final Host site, final User deletingUser, final boolean respectFrontendRoles,
                                     final boolean runAsSeparatedThread);

    /**
     * Returns the "default" Site in the current data repository.
     *
     * @param contentTypeId Content Type Inode of the current "Host" type.
     * @param columnName    For non-JSON databases, the name of the database column that determines whether a Site is
     *                      the default one or not.
     *
     * @return The Site with the specified name, or the "default" Site.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    Optional<Host> findDefaultHost(final String contentTypeId, final String columnName) throws DotDataException, DotSecurityException;

    /**
     * Returns all live Sites in dotCMS.
     *
     * @param siteNameFilter       Optional parameter used to filter by Site name.
     * @param limit                Limit of results being returned as part of the response, for pagination purposes. If
     *                             set equal or lower than zero, this parameter will be ignored.
     * @param offset               Expected offset of results in the response, for pagination purposes. If set equal or
     *                             lower than zero, this parameter will be ignored.
     * @param showSystemHost       If the System Host must be included in the results, set to {@code true}. Otherwise,
     *                             set to {@code false}.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The resulting list of {@link Host} objects.
     */
    Optional<List<Host>> findLiveSites(final String siteNameFilter, final int limit, final int offset,
                                       final boolean showSystemHost, final User user,
                                       final boolean respectFrontendRoles);

    /**
     * Returns all stopped/un-published Sites in dotCMS.
     *
     * @param siteNameFilter       Optional parameter used to filter by Site name.
     * @param includeArchivedSites If archived Sites must be returned, set to {@code true}. Otherwise, set to {@code
     *                             false}.
     * @param limit                Limit of results being returned as part of the response, for pagination purposes. If
     *                             set equal or lower than zero, this parameter will be ignored.
     * @param offset               Expected offset of results in the response, for pagination purposes. If set equal or
     *                             lower than zero, this parameter will be ignored.
     * @param showSystemHost       If the System Host must be included in the results, set to {@code true}. Otherwise,
     *                             set to {@code false}.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The resulting list of {@link Host} objects.
     */
    Optional<List<Host>> findStoppedSites(final String siteNameFilter, boolean includeArchivedSites, final int limit,
                                          final int offset, boolean showSystemHost, final User user,
                                          final boolean respectFrontendRoles);

    /**
     * Returns all stopped/un-published Sites in dotCMS.
     *
     * @param siteNameFilter       Optional parameter used to filter by Site name.
     * @param limit                Limit of results being returned as part of the response, for pagination purposes. If
     *                             set equal or lower than zero, this parameter will be ignored.
     * @param offset               Expected offset of results in the response, for pagination purposes. If set equal or
     *                             lower than zero, this parameter will be ignored.
     * @param showSystemHost       If the System Host must be included in the results, set to {@code true}. Otherwise,
     *                             set to {@code false}.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The resulting list of {@link Host} objects.
     */
    Optional<List<Host>> findArchivedSites(final String siteNameFilter, final int limit, final int offset,
                                           boolean showSystemHost, final User user, final boolean respectFrontendRoles);

    /**
     * Returns the total number of Sites that exist in your content repository.
     *
     * @return The total number of Sites.
     *
     * @throws DotDataException An error occurred when accessing the data source.
     */
    long count() throws DotDataException;

}
