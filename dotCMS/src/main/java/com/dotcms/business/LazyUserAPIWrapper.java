package com.dotcms.business;

import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;

import java.util.Date;
import java.util.List;

/**
 * This is just a lazy user api for the {@link UserAPI}
 * @author jsanca
 */
public class LazyUserAPIWrapper implements UserAPI {

    private UserAPI userAPI = null;

    private UserAPI getUserAPI () {

        if (null == this.userAPI) {
            synchronized (this) {
                if (null == this.userAPI) {
                    this.userAPI = APILocator.getUserAPI();
                }
            }
        }

        return userAPI;
    }

    @Override
    public String encryptUserId(String userId) throws DotStateException {
        return this.getUserAPI().encryptUserId(userId);
    }

    @Override
    public User loadUserById(String userId, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException, NoSuchUserException {
        return this.getUserAPI().loadUserById(userId, user, respectFrontEndRoles);
    }

    @Override
    public User loadUserById(String userId) throws DotDataException, DotSecurityException, NoSuchUserException {
        return this.getUserAPI().loadUserById(userId);
    }

    @Override
    public User loadByUserByEmail(String email, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException, NoSuchUserException {
        return this.getUserAPI().loadByUserByEmail(email, user, respectFrontEndRoles);
    }

    @Override
    public List<User> findAllUsers(int begin, int end) throws DotDataException {
        return this.getUserAPI().findAllUsers(begin, end);
    }

    @Override
    public List<User> findAllUsers() throws DotDataException {
        return this.getUserAPI().findAllUsers();
    }

    @Override
    public List<User> getUsersByName(String filter, int start, int limit, User user, boolean respectFrontEndRoles) throws DotDataException {
        return this.getUserAPI().getUsersByName(filter, start, limit, user, respectFrontEndRoles);
    }

    @Override
    public User createUser(String userId, String email) throws DotDataException, DuplicateUserException {
        return this.getUserAPI().createUser(userId, email);
    }

    @Override
    public User getDefaultUser() throws DotDataException {
        return this.getUserAPI().getDefaultUser();
    }

    @Override
    public User getSystemUser() throws DotDataException {
        return this.getUserAPI().getSystemUser();
    }

    @Override
    public User getAnonymousUser() throws DotDataException {
        return this.getUserAPI().getAnonymousUser();
    }

    @Override
    public boolean userExistsWithEmail(String email) throws DotDataException, NoSuchUserException {
        return this.getUserAPI().userExistsWithEmail(email);
    }

    @Override
    public long getCountUsersByNameOrEmail(String filter) throws DotDataException {
        return this.getUserAPI().getCountUsersByNameOrEmail(filter);
    }

    @Override
    public long getCountUsersByNameOrEmailOrUserID(String filter) throws DotDataException {
        return this.getUserAPI().getCountUsersByNameOrEmailOrUserID(filter);
    }

    @Override
    public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous) throws DotDataException {
        return this.getUserAPI().getCountUsersByNameOrEmailOrUserID(filter, includeAnonymous);
    }

    @Override
    public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous, boolean includeDefault) throws DotDataException {
        return this.getUserAPI().getCountUsersByNameOrEmailOrUserID(filter, includeAnonymous, includeDefault);
    }

    @Override
    public List<User> getUsersByNameOrEmail(String filter, int page, int pageSize) throws DotDataException {
        return this.getUserAPI().getUsersByNameOrEmail(filter, page, pageSize);
    }

    @Override
    public List<String> getUsersIdsByCreationDate(Date filterDate, int page, int pageSize) throws DotDataException {
        return this.getUserAPI().getUsersIdsByCreationDate(filterDate, page, pageSize);
    }

    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize) throws DotDataException {
        return this.getUserAPI().getUsersByNameOrEmailOrUserID(filter, page, pageSize);
    }

    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize, boolean includeAnonymous) throws DotDataException {
        return this.getUserAPI().getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous);
    }

    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize, boolean includeAnonymous, boolean includeDefault) throws DotDataException {
        return this.getUserAPI().getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous, includeDefault);
    }

    @Override
    public void save(User userToSave, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException, DuplicateUserException {
        this.getUserAPI().save(userToSave, user, respectFrontEndRoles);
    }

    @Override
    public void save(User userToSave, User user, boolean validatePassword, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException, DuplicateUserException {
        this.getUserAPI().save(userToSave, user, validatePassword, respectFrontEndRoles);
    }

    @Override
    public void delete(User userToDelete, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        this.getUserAPI().delete(userToDelete, user, respectFrontEndRoles);
    }

    @Override
    public void delete(User userToDelete, User replacementUser, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        this.getUserAPI().delete(userToDelete, replacementUser, user, respectFrontEndRoles);
    }

    @Override
    public void saveAddress(User userToSaveNewAddress, Address ad, User user, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
        this.getUserAPI().saveAddress(userToSaveNewAddress, ad, user, respectFrontEndRoles);
    }

    @Override
    public Address loadAddressById(String addressId, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        return this.getUserAPI().loadAddressById(addressId, user, respectFrontEndRoles);
    }

    @Override
    public void deleteAddress(Address ad, User user, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
        this.getUserAPI().deleteAddress(ad, user, respectFrontEndRoles);
    }

    @Override
    public List<Address> loadUserAddresses(User userToGetAddresses, User user, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
        return this.getUserAPI().loadUserAddresses(userToGetAddresses, user, respectFrontEndRoles);
    }

    @Override
    public boolean isCMSAdmin(User user) throws DotDataException {
        return this.getUserAPI().isCMSAdmin(user);
    }

    @Override
    public void updatePassword(User user, String newpass, User currentUser, boolean respectFrontEndRoles) throws DotSecurityException, DotDataException, DotInvalidPasswordException {
        this.getUserAPI().updatePassword(user, newpass, currentUser, respectFrontEndRoles);
    }

    @Override
    public void markToDelete(User userToDelete) throws DotDataException {
        this.getUserAPI().markToDelete(userToDelete);
    }

    @Override
    public List<User> getUnDeletedUsers() throws DotDataException {
        return this.getUserAPI().getUnDeletedUsers();
    }
} // E:O:F:LazyUserAPIWrapper.
