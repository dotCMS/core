package com.dotmarketing.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.List;

public interface UserFactory {
    /**
     * @param userId - UserID being searched for
     * @throws DotDataException if the user cannot be found
     */
    User loadUserById(String userId)
            throws DotDataException, com.dotmarketing.business.NoSuchUserException;

    User loadByUserEmailAndCompany(String email, String companyId)
            throws DotDataException;

    /**
     * Saves the user
     */
    User save(User user) throws DotDataException, DuplicateUserException;

    List<User> findAllUsers(String companyId, int begin, int end) throws DotDataException;

    long getCountUsersByName(String filter, List<Role> roles, String companyId);

    /**
     * This method returns a list of users whose names are like the filter passed in. It also allows
     * filtering through a list of roles
     *
     * @param filter filter compare string.
     * @param roles a list of roles to filter by. This is optional
     * @param start is the first element to display.
     * @param limit is the maximum number of elements to get.
     * @return List<User> of user entities
     */
    List<User> getUsersByName(final String filter, final List<Role> roles, final String companyId, final int start,
            final int limit) throws DotDataException;

    /**
     * Will create user if it doesn't exist
     */
    User loadDefaultUser(final Company company) throws DotDataException, NoSuchUserException;

    /**
     *
     */
    boolean userExistsWithEmail(final String email, final String companyId) throws DotDataException;

    /**
     * This Method return the number of user that have a firstname, lastname or email like the
     * filter string. For example all amount of user with lastName "Andrews"
     *
     * @param filter Compare string
     * @return long
     * @version 1.9
     */
    long getCountUsersByNameOrEmail(String filter) throws DotDataException;

    /**
     * This method return a a paginated list of user that have a firstname, lastname or email like
     * the compare string passed This method will ALWAYS hit DB
     *
     * @param filter compare string
     * @param page page to display
     * @param pageSize number of element to show in the page
     * @return List<User>
     * @version 1.9
     */
    List<User> getUsersByNameOrEmail(String filter, int page, int pageSize) throws DotDataException;

    /**
     * Returns a list of users ids filtering by a given creation date
     *
     * @param limit -1 for no limit
     */
    List<String> getUsersIdsByCreationDate(final String companyId, final Date filterDate, final int start, final int limit)
            throws DotDataException;

    /**
     *
     */
    void delete(User userToDelete) throws DotDataException;

    /**
     * This Method return the number of user that have a firstname, lastname or email like the
     * filter string. For example all amount of user with lastName "Andrews", includes anonymous
     * user
     *
     * @param filter Compare string
     * @return long
     * @version 1.9
     */
    long getCountUsersByNameOrEmailOrUserID(String filter)
            throws DotDataException;

    /**
     * This Method return the number of user that have a firstname, lastname or email like the
     * filter string. For example all amount of user with lastName "Andrews", this might exclude
     * anonymous
     *
     * @param filter Compare string
     * @return long
     */
    long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous)
            throws DotDataException;

    /**
     * Return the number of users whose first name, last name, or email are similar to the value
     * specified in the {@code filter} parameter. For example, retrieving all users that match
     * {@code filter = "and"} will return records like these:
     * <ul>
     * <li>Where {@code firstname = "Andrew"}</li>
     * <li>Or {@code firstname = "Alexander"}</li>
     * <li>Or {@code lastname = "Andrews"}</li>
     * <li>Or {@code lastname = "Allmand"}</li>
     * <li>Or {@code email = "john.anderson@domain.com"}</li>
     * <li>Etc.</li>
     * </ul>
     * <p>
     * If the filter is not specified, all users will be returned. This method
     * <b>ALWAYS</b> hits the database.
     *
     * @param filter - A set of characters that can match the value of the user's first name, last
     * name, or e-mail. If not specified, all users will be returned.
     * @param includeAnonymous - If set to {@code true}, the "Anonymous" user will be included in
     * the result. Otherwise, set to {@code false}.
     * @param includeDefault - If set to {@code true}, the "Default" user will be included in the
     * result. Otherwise, set to {@code false}.
     * @return The total count of users that match the specified criteria.
     * @throws DotDataException An error occurred when retrieving the information from the
     * database.
     */
    long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous,
            boolean includeDefault)
            throws DotDataException;

    /**
     * This method return a a paginated list of user that have a firstname, lastname or email like
     * the compare string passed, includes anonymous user This method will ALWAYS hit DB
     *
     * @param filter compare string
     * @param page page to display
     * @param pageSize number of element to show in the page
     * @return List<User>
     * @version 1.9
     */
    List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
            int pageSize) throws DotDataException;

    /**
     * This method return a a paginated list of user that have a firstname, lastname or email like
     * the compare string passed, this might exclude anonymous from the list This method will ALWAYS
     * hit DB
     *
     * @param filter compare string
     * @param page page to display
     * @param pageSize number of element to show in the page
     * @return List<User>
     */
    List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
            int pageSize, boolean includeAnonymous) throws DotDataException;

    /**
     * Return the list of {@link User} objects whose first name, last name, or email are similar to
     * the value specified in the {@code filter} parameter. For example, retrieving all users that
     * match {@code filter = "and"} will return records like these:
     * <ul>
     * <li>Where {@code firstname = "Andrew"}</li>
     * <li>Or {@code firstname = "Alexander"}</li>
     * <li>Or {@code lastname = "Andrews"}</li>
     * <li>Or {@code lastname = "Allmand"}</li>
     * <li>Or {@code email = "john.anderson@domain.com"}</li>
     * <li>Etc.</li>
     * </ul>
     * <p>
     * If the filter is not specified, all users will be returned. This method
     * <b>ALWAYS</b> hits the database.
     *
     * @param filter - A set of characters that can match the value of the user's first name, last
     * name, or e-mail. If not specified, all users will be returned.
     * @param includeAnonymous - If set to {@code true}, the "Anonymous" user will be included in
     * the result. Otherwise, set to {@code false}.
     * @param includeDefault - If set to {@code true}, the "Default" user will be included in the
     * result. Otherwise, set to {@code false}.
     * @return The list of users that match the specified criteria.
     * @throws DotDataException An error occurred when retrieving the information from the
     * database.
     */
    List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
            int pageSize, boolean includeAnonymous, boolean includeDefault) throws DotDataException;

    /**
     * Search for users that could not be deleted successfully
     *
     * @return List of users with uncompleted status
     */
    List<User> getUnDeletedUsers() throws DotDataException;

    List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize,
            boolean includeAnonymous,
            boolean includeDefault, String showUserType) throws DotDataException;

    long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous,
            boolean includeDefault, String showUserType)
            throws DotDataException;


	/**
	 * Get the userId by the icqId
	 *
	 * @param icqId icqId to search for
	 * @return userId that the icqId is associated
	 */
  protected abstract String getUserIdByIcqId(final String icqId);


}
