package com.dotmarketing.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;

/**
 * UserAPI is an API intended to be a helper class for class to get User entities.  Classes within the dotCMS
 * should use this API for user management.  The UserAPI does not do cache management. It delegates this responsibilities
 * to underlying classes.
 * @author Jason Tesser
 * @version 1.9
 * @since 1.6
 */
public interface UserAPI {

	/**
	 * Used to encrypt a User's userid
	 * @param userId
	 * @return
	 * @throws DotStateException if userid doesn't exist
	 */
	public String encryptUserId(java.lang.String userId) throws DotStateException;

	/**
	 *
	 * @param userId - UserID being searched for
	 * @param user - The user who is requesting the user to be returned
	 * @param respectFrontEndRoles
	 * @return
	 * @throws DotDataException
	 */
	public User loadUserById(String userId, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException, NoSuchUserException;

	/**
	 *
	 * @param userId - UserID being searched for
	 * @return
	 * @throws DotDataException
	 */
	public User loadUserById(String userId) throws DotDataException, DotSecurityException, NoSuchUserException;

	/**
	 * This method finds a User by email, if the user was not found it returns a new user instance
	 * ready to be filled and stored. This methods pulls a new userid from liferay counters.
	 * If the user is not found it will return a new user.
	 * @param email
	 * @param user - The user who is requesting the user to be returned
	 * @param respectFrontEndRoles
	 * @return User
	 * @version 1.9
	 * @since 1.9
	 */
	public User loadByUserByEmail(String email, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException, NoSuchUserException;

	/**
	 * This method return a list of all the existing users in the cms.  It will ALWAYS hit the database
	 * @param begin
	 * @param end
	 * @return
	 * @throws DotDataException
	 */
	public  List<User> findAllUsers (int begin, int end) throws DotDataException;

	/**
	 * This method return a list of all the existing users in the cms.  It will ALWAYS hit the database
	 * @return
	 * @throws DotDataException
	 */
	public  List<User> findAllUsers () throws DotDataException;

	/**
	 * This method returns a list of users whose names are like the string passed in.
	 * This method WILL hit the DB EVERY time
	 * @param filter compare string
	 * @param start First element to display. For a negative value, zero is assumed.
	 * @param limit Max number of elements to show. For a negative value, zero is assumed.
	 * @param user
	 * @param respectFrontEndRoles
	 * @return
	 * @throws DotDataException
	 */
	public List<User> getUsersByName(String filter, int start,int limit, User user, boolean respectFrontEndRoles) throws  DotDataException;

	/**
	 * Creates an instance of a user
     * @param userId Can be null
     * @param email Can be null
	 * @return
	 * @throws DotDataException
	 */
	public User createUser(String userId, String email) throws DotDataException, DuplicateUserException;

	/**
	 * This method return the default user of the system
	 * @return User
	 * @version 1.9
	 * @throws DotDataException
	 * @since 1.9
	 */
	public User getDefaultUser() throws DotDataException;

	public User getSystemUser() throws DotDataException;

	 /**
	 * This method return an anonymous user, created to manage the submitContent macro with no user logged in
	 * @return User
	 * @throws DotDataException
	 */
	public User getAnonymousUser() throws DotDataException;

	/**
	 * Verify is exists a user with the specified email address
	 * @param email user email
	 * @return boolean
	 * @version 1.9
	 * @since 1.9
	 */
	public boolean userExistsWithEmail(String email) throws DotDataException, NoSuchUserException;

	/**
	 * This Method return the number of user that have a firstname, lastname or email like the filter string.
	 * For example all amount of user with lastName "Andrews"
	 * This method will ALWAYS hit DB
	 * @param filter Compare string
	 * @return long
	 * @version 1.9
	 * @throws DotDataException
	 */
    public long getCountUsersByNameOrEmail(String filter) throws DotDataException;
    
    /**
	 * This Method return the number of user that have a firstname, lastname or email like the filter string.
	 * For example all amount of user with lastName "Andrews"
	 * This method will ALWAYS hit DB
	 * @param filter Compare string
	 * @return long
	 * @version 1.9
	 * @throws DotDataException
	 */
    public long getCountUsersByNameOrEmailOrUserID(String filter) throws DotDataException;


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
    public List<User> getUsersByNameOrEmail(String filter,int page,int pageSize) throws DotDataException;
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
    public List<User> getUsersByNameOrEmailOrUserID(String filter,int page,int pageSize) throws DotDataException;

    /**
     * This method return a list of users and roles which name are like the compared string passed
	 * This method will ALWAYS hit DB
     * @param filter compare string
     * @param start first element to display
     * @param limit max number of elements to show
     * @return Map<String, Object>
     * @throws DotRuntimeException
     * @version 1.9
	 * @since 1.0
	 * @author David Torres
	 * @author Oswaldo Gallango
	 * @deprecated
     */
    public Map<String, Object> getUsersAnRolesByName(String filter, int start,int limit) throws DotDataException;

    /**
     * Save or update in db the user object
     * @param user - User to save
     * @param user - User to check permissions to save
     * @param respectFrontEndRoles
     */
    public void save(User userToSave, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException, DuplicateUserException;

    /**
     *
     * @param userToDelete
     * @param user
     * @param respectFrontEndRoles
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public void delete(User userToDelete, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

    /**
     * Method that saves a new address and tie it to the user
     * @param user
     * @param ad
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotRuntimeException
     */
	public void saveAddress(User userToSaveNewAddress, Address ad, User user, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException;

	/**
	 * Load address by id
	 * @throws DotSecurityException
	 */
	public Address loadAddressById(String addressId, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

	/**
	 * Removes from the system the given address
	 * @param ad
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotRuntimeException
	 */
	public void deleteAddress(Address ad, User user, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException;

	/**
	 * Retrieves all addresses associated to a user
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotRuntimeException
	 */
	public List<Address> loadUserAddresses(User userToGetAddresses, User user, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException;

	/**
	 * Returns true if the user is a cms admin
	 *
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotRuntimeException
	 * @throws DotSecurityException
	 */
	public boolean isCMSAdmin(User user) throws DotDataException;

}