package com.dotmarketing.business;

import java.util.Date;
import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;

/**
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public abstract class UserFactory {

	/**
	 * @param userId - UserID being searched for
	 * @param user - The user who is requesting the user to be returned
	 * @param respectFrontEndRoles
	 * @return
	 * @throws DotDataException if the user cannot be found
	 */
	protected abstract User loadUserById(String userId) throws DotDataException, com.dotmarketing.business.NoSuchUserException;
	
	/**
	 * This method finds a User by email, if the user was not found it returns a new user instance 
	 * ready to be filled and stored. This methods pulls a new userid from liferay counters. 
	 * @param email
	 * @return User
	 * @version 1.9
	 * @since 1.9
	 */
	protected abstract User loadByUserByEmail(String email) throws DotDataException, DotSecurityException, com.dotmarketing.business.NoSuchUserException;
	
	/**
	 * This method return a list of all the existing users in the cms.  It will ALWAYS hit the database
	 * @return List<User>
	 * @version 1.9
	 */
	protected abstract List<User> findAllUsers (int begin, int end) throws DotDataException;
	
	/**
	 * This method return a list of all the existing users in the cms.  It will ALWAYS hit the database
	 * @return List<User>
	 * @version 1.9
	 */
	protected abstract List<User> findAllUsers () throws DotDataException;
	
	/**
	 * Saves the user
	 * @param user
	 * @return
	 * @throws DotDataException
	 */
	protected abstract User saveUser(User user) throws DotDataException,DuplicateUserException; 
	
	 /**
     * 
     * This method returns a list of users whose names are like the filter passed in.
     * This method WILL hit the DB EVERY time
     * @param filter compare string.
     * @param start is the first element to display.
     * @param limit is the maximum number of elements to get.
     * @return List<User> of user entities
     * @throws DotRuntimeException
     * @version 1.6
	 * @since 1.0
	 * @author David Torres
	 * @author Oswaldo Gallango
	 * @author Jason Tesser
     */
    protected abstract List<User> getUsersByName(String filter, int start,int limit) throws DotDataException;

	/**
	 * This method returns a list of users whose names are like the filter passed in.
	 * It also allows filtering through a list of roles
	 * @param filter filter compare string.
	 * @param roles a list of roles to filter by. This is optional
	 * @param start is the first element to display.
	 * @param limit  is the maximum number of elements to get.
	 * @return List<User> of user entities
	 * @throws DotDataException
	 */
	protected abstract List<User> getUsersByName(final String filter, final List<Role> roles ,final int start, final int limit) throws DotDataException;

	/**
	 * This Method return the number of user that have a name like the filter string.
	 *
	 * @param filter compare string.
	 * @return
	 * @throws DotDataException
	 */
	protected abstract long getCountUsersByName(String filter) throws DotDataException;


	/**
	 *
	 * @param filter
	 * @param roles
	 * @return
	 * @throws DotDataException
	 */
	protected abstract long getCountUsersByName(String filter, final List<Role> roles) throws DotDataException;

    /**
     * 
     * @param userId Can be null
     * @param email Can be null 
     * @return
     * @throws DotDataException
     */
    protected abstract User createUser(String userId, String email) throws DotDataException,DuplicateUserException;
    
    /**
     * Will create user if it doesn't exist
     * @return
     * @throws DotDataException
     * @throws NoSuchUserException
     */
    protected abstract User loadDefaultUser() throws DotDataException, NoSuchUserException;
    
    /**
     * 
     * @param email
     * @return
     * @throws DotDataException
     */
    protected abstract boolean userExistsWithEmail(String email) throws DotDataException;
    
    /**
	 * This Method return the number of user that have a firstname, lastname or email like the filter string.
	 * For example all amount of user with lastName "Andrews"
	 * @param filter Compare string
	 * @return long
	 * @version 1.9
     * @throws DotDataException 
	 */
    protected abstract long getCountUsersByNameOrEmail(String filter) throws DotDataException;
    
    /**
     * This method return a a paginated list of user that have a firstname, lastname or email like
     * the compare string passed
	 * This method will ALWAYS hit DB
     * @param filter compare string
     * @param page page to display
     * @param pageSize number of element to show in the page
     * @return List<User>
     * @version 1.9
     */
    protected abstract List<User> getUsersByNameOrEmail(String filter,int page,int pageSize) throws DotDataException;

    /**
     * Returns a list of users ids filtering by a given creation date
     *
     * @param filterDate
     * @param start
     * @param limit      -1 for no limit
     * @return
     * @throws DotDataException
     */
    protected abstract List<String> getUsersIdsByCreationDate ( Date filterDate, int start, int limit ) throws DotDataException;
    
    /**
     * 
     * @param userToDelete
     * @param user
     * @param respectFrontEndRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    protected abstract void delete(User userToDelete) throws DotDataException;
    
    /**
     * This method saves the given address and attaches it to the user
     * @param user
     * @param ad
     * @throws DotDataException
     */
    protected abstract void saveAddress(User user, Address ad) throws DotDataException;

    /**
     * Loads an address by address id
     * @param addressId
     * @return
     * @throws DotDataException
     */
    protected abstract Address loadAddressById(String addressId) throws DotDataException;

    /**
     * Delete address
     * @param ad
     * @return
     */
	protected abstract void deleteAddress(Address ad);

	/**
	 * Retrieves user associated addresses
	 * @param user
	 * @return
	 * @throws DotDataException
	 */
	protected abstract  List<Address> loadUserAddresses(User user) throws DotDataException;
	
	/**
	 * This Method return the number of user that have a firstname, lastname or email like the filter string.
	 * For example all amount of user with lastName "Andrews", includes anonymous user
	 * @param filter Compare string
	 * @return long
	 * @version 1.9
	 * @throws DotDataException 
	 */
	protected abstract long getCountUsersByNameOrEmailOrUserID(String filter)
			throws DotDataException;

	/**
	 * This Method return the number of user that have a firstname, lastname or email like the filter string.
	 * For example all amount of user with lastName "Andrews", this might exclude anonymous
	 * @param filter Compare string
	 * @return long
	 * @throws DotDataException
	 */
	protected abstract long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous)
			throws DotDataException;

	/**
	 * Return the number of users whose first name, last name, or email are
	 * similar to the value specified in the {@code filter} parameter. For
	 * example, retrieving all users that match {@code filter = "and"} will
	 * return records like these:
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
	 * @param filter
	 *            - A set of characters that can match the value of the user's
	 *            first name, last name, or e-mail. If not specified, all users
	 *            will be returned.
	 * @param includeAnonymous
	 *            - If set to {@code true}, the "Anonymous" user will be
	 *            included in the result. Otherwise, set to {@code false}.
	 * @param includeDefault
	 *            - If set to {@code true}, the "Default" user will be included
	 *            in the result. Otherwise, set to {@code false}.
	 * @return The total count of users that match the specified criteria.
	 * @throws DotDataException
	 *             An error occurred when retrieving the information from the
	 *             database.
	 */
	protected abstract long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous, boolean includeDefault)
			throws DotDataException;

	/**
     * This method return a a paginated list of user that have a firstname, lastname or email like
     * the compare string passed, includes anonymous user
	 * This method will ALWAYS hit DB
     * @param filter compare string
     * @param page page to display
     * @param pageSize number of element to show in the page
     * @return List<User>
     * @version 1.9
     */
	protected abstract List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
			int pageSize) throws DotDataException;

	/**
     * This method return a a paginated list of user that have a firstname, lastname or email like
     * the compare string passed, this might exclude anonymous from the list
	 * This method will ALWAYS hit DB
     * @param filter compare string
     * @param page page to display
     * @param pageSize number of element to show in the page
     * @return List<User>
     */
	protected abstract List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
			int pageSize, boolean includeAnonymous) throws DotDataException;

	/**
	 * Return the list of {@link User} objects whose first name, last name, or
	 * email are similar to the value specified in the {@code filter} parameter.
	 * For example, retrieving all users that match {@code filter = "and"} will
	 * return records like these:
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
	 * @param filter
	 *            - A set of characters that can match the value of the user's
	 *            first name, last name, or e-mail. If not specified, all users
	 *            will be returned.
	 * @param includeAnonymous
	 *            - If set to {@code true}, the "Anonymous" user will be
	 *            included in the result. Otherwise, set to {@code false}.
	 * @param includeDefault
	 *            - If set to {@code true}, the "Default" user will be included
	 *            in the result. Otherwise, set to {@code false}.
	 * @return The list of users that match the specified criteria.
	 * @throws DotDataException
	 *             An error occurred when retrieving the information from the
	 *             database.
	 */
	protected abstract List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
			int pageSize, boolean includeAnonymous, boolean includeDefault) throws DotDataException;

    /**
     * Search for users that could not be deleted successfully
     *
     * @return List of users with uncompleted status
     */
    protected abstract List<User> getUnDeletedUsers() throws DotDataException;

  protected abstract List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize, boolean includeAnonymous,
      boolean includeDefault, String showUserType) throws DotDataException;

  public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous, boolean includeDefault, String showUserType)
      throws DotDataException {
    // TODO Auto-generated method stub
    return 0;
  }


}
