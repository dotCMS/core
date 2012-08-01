package com.dotmarketing.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;

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
    protected abstract Map<String, Object> getUsersAnRolesByName(String filter, int start,int limit) throws DotDataException;
    
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
	 * For example all amount of user with lastName "Andrews"
	 * @param filter Compare string
	 * @return long
	 * @version 1.9
	 * @throws DotDataException 
	 */
	protected abstract long getCountUsersByNameOrEmailOrUserID(String filter)
			throws DotDataException;
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
	protected abstract List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
			int pageSize) throws DotDataException;
	
	
	
    
}
