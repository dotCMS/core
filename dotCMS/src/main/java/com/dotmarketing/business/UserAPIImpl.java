package com.dotmarketing.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.PasswordTrackerLocalManager;
import com.liferay.portal.ejb.PasswordTrackerLocalManagerFactory;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;
import com.liferay.portal.pwd.PwdToolkitUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import io.vavr.control.Try;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * UserAPIImpl is an API intended to be a helper class for class to get User
 * entities from liferay's repository. Classes within the dotCMS should use this
 * API for user management. The UserAPIImpl does not do cache management. It
 * delegates this responsabilities to underlying classes.
 *
 * @author David Torres
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.9
 * @since 1.6
 */
public class UserAPIImpl implements UserAPI {

    private final UserFactory userFactory;
    private final PermissionAPI permissionAPI;
    private final UserProxyAPI userProxyAPI;
    private final NotificationAPI notfAPI;
    private final BundleAPI bundleAPI;
    static User systemUser, anonUser=null;
    /**
     * Creates an instance of the class.
     */
    public UserAPIImpl() {
        userFactory = FactoryLocator.getUserFactory();
        permissionAPI = APILocator.getPermissionAPI();
        userProxyAPI = APILocator.getUserProxyAPI();
        notfAPI = APILocator.getNotificationAPI();
        bundleAPI = APILocator.getBundleAPI();
    }

    @CloseDBIfOpened
    @Override
    public User loadUserById(final String userId, final User user, final boolean respectFrontEndRoles)
            throws DotDataException, DotSecurityException,com.dotmarketing.business.NoSuchUserException {

        if(!UtilMethods.isSet(userId)){
            throw new DotDataException("You must specifiy an userId to search for");
        }
        final User u = userFactory.loadUserById(userId);
        if(!UtilMethods.isSet(u)){
            throw new com.dotmarketing.business.NoSuchUserException("No user found with passed in email");
        }
        if(user!=null && user.getUserId().equals(u.getUserId())) {
            return user;
        }
        if(permissionAPI.doesUserHavePermission(userProxyAPI.getUserProxy(u,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)){
            return u;
        }else{
            throw new DotSecurityException("The User being passed in doesn't have permission to requested User");
        }
    }

    @CloseDBIfOpened
    @Override
    public User loadUserById(final String userId) throws DotDataException, DotSecurityException,com.dotmarketing.business.NoSuchUserException {
        if(!UtilMethods.isSet(userId)){
            throw new DotDataException("You must specifiy an userId to search for");
        }

        final User user = userFactory.loadUserById(userId);
        if(!UtilMethods.isSet(user)){
            throw new com.dotmarketing.business.NoSuchUserException("No user found with passed in email");
        }
        return user;
    }

    @CloseDBIfOpened
    @Override
    public User loadByUserByEmail(final String email, final User user,
            final boolean respectFrontEndRoles) throws DotDataException, DotSecurityException, com.dotmarketing.business.NoSuchUserException {

        if(!UtilMethods.isSet(email)){
            throw new DotDataException("You must specifiy an email to search for");
        }

        final User u = userFactory.loadByUserByEmail(email);
        if(!UtilMethods.isSet(u)){
            throw new com.dotmarketing.business.NoSuchUserException("No user found with passed in email");
        }
        if(permissionAPI.doesUserHavePermission(userProxyAPI.getUserProxy(u,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)){
            return u;
        }else{
            throw new DotSecurityException("The User being passed in doesn't have permission to requested User");
        }
    }

    @CloseDBIfOpened
    @Override
    public String encryptUserId(final String userId) throws DotStateException{
        try{
            return UserManagerUtil.encryptUserId(userId);
        }catch (Exception e) {
            throw new DotStateException("Unable to encrypt userID : ", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public long getCountUsersByName(String filter) throws DotDataException {
        return userFactory.getCountUsersByName(filter);
    }

    @CloseDBIfOpened
    @Override
    public long getCountUsersByName(String filter, List<Role> roles) throws DotDataException {
        return userFactory.getCountUsersByName(filter, roles);
    }

    @CloseDBIfOpened
    @Override
    public List<User> getUsersByName(String filter, int start,int limit, User user, boolean respectFrontEndRoles) throws DotDataException {
        return userFactory.getUsersByName(filter, start, limit);
    }

    @CloseDBIfOpened
    @Override
    public List<User> getUsersByName(String filter, List<Role> roles, int start,int limit) throws DotDataException {
        return userFactory.getUsersByName(filter, roles ,start, limit);
    }

    @WrapInTransaction
    @Override
    public User createUser(String userId, String email) throws DotDataException, DuplicateUserException {
        return userFactory.createUser(userId, email);
    }

    @CloseDBIfOpened
    @Override
    public User getDefaultUser() throws DotDataException {
        try {
            return userFactory.loadDefaultUser();
        } catch (Exception e) {
            throw new DotDataException("getting default user user failed", e);
        }
    }

    @Override
    public User getSystemUser() throws DotDataException {
        if(this.systemUser==null){
            this.systemUser=_getSystemUser();
        }
        return this.systemUser;
    }

    @WrapInTransaction
    private User _getSystemUser() throws DotDataException {
        User user = null;
        RoleAPI roleAPI = com.dotmarketing.business.APILocator.getRoleAPI();
        Role cmsAdminRole = roleAPI.loadCMSAdminRole();
        try {
            user = userFactory.loadUserById(SYSTEM_USER_ID);
        } catch (NoSuchUserException e) {
            user = createUser("system", "system@dotcmsfakeemail.org");
            user.setUserId(SYSTEM_USER_ID);
            user.setFirstName("system user");
            user.setLastName("system user");
            user.setCreateDate(new java.util.Date());
            user.setCompanyId(PublicCompanyFactory.getDefaultCompanyId());
            userFactory.saveUser(user);
        }
        if(!roleAPI.doesUserHaveRole(user, cmsAdminRole))
            roleAPI.addRoleToUser(cmsAdminRole.getId(), user);

        return user;
    }


    @Override
    public User getAnonymousUser() throws DotDataException {
        if(this.anonUser==null){
            this.anonUser=_getAnonymousUser();
        }
        return this.anonUser;
    }


    @Override
    public User getAnonymousUserNoThrow()  {
        return Try.of(() -> getAnonymousUser()).getOrElseThrow(e->new DotRuntimeException(e));
    }

    @WrapInTransaction
    private synchronized User _getAnonymousUser() throws DotDataException {
        if(this.anonUser!=null) {
            return this.anonUser;
        }
        User user = null;
        try {
            user = userFactory.loadUserById(CMS_ANON_USER_ID);
        } catch (DotDataException e) {
            user = createUser(CMS_ANON_USER_ID, "anonymous@dotcmsfakeemail.org");
            user.setUserId(CMS_ANON_USER_ID);
            user.setFirstName("Anonymous");
            user.setLastName("User");
            user.setCreateDate(new java.util.Date());
            user.setCompanyId(PublicCompanyFactory.getDefaultCompanyId());
            userFactory.saveUser(user);
        } catch (NoSuchUserException e) {
            user = createUser(CMS_ANON_USER_ID, "anonymous@dotcmsfakeemail.org");
            user.setUserId(CMS_ANON_USER_ID);
            user.setFirstName("Anonymous");
            user.setLastName("User");
            user.setCreateDate(new java.util.Date());
            user.setCompanyId(PublicCompanyFactory.getDefaultCompanyId());
            userFactory.saveUser(user);
        }

        // Assure CMS ANON has the anon role and the Front End User Role
        Role cmsAnon = APILocator.getRoleAPI().loadCMSAnonymousRole();
        if(cmsAnon!=null) {
            APILocator.getRoleAPI().addRoleToUser(cmsAnon, user);
        }
        APILocator.getRoleAPI().addRoleToUser(Role.DOTCMS_FRONT_END_USER, user);
        this.anonUser=user;
        return this.anonUser;
    }

    @CloseDBIfOpened
    @Override
    public boolean userExistsWithEmail(String email) throws DotDataException {
        return userFactory.userExistsWithEmail(email);
    }

    @CloseDBIfOpened
    @Override
    public List<User> findAllUsers(int begin, int end) throws DotDataException {
        return userFactory.findAllUsers(begin, end);
    }

    @CloseDBIfOpened
    @Override
    public List<User> findAllUsers() throws DotDataException {
        return userFactory.findAllUsers();
    }

    @CloseDBIfOpened
    @Override
    public long getCountUsersByNameOrEmail(String filter) throws DotDataException {
        return userFactory.getCountUsersByNameOrEmail(filter);
    }

    @CloseDBIfOpened
    @Override
    public List<User> getUsersByNameOrEmail(String filter, int page, int pageSize) throws DotDataException {
        return userFactory.getUsersByNameOrEmail(filter, page, pageSize);
    }

    @CloseDBIfOpened
    @Override
    public List<String> getUsersIdsByCreationDate ( Date filterDate, int page, int pageSize ) throws DotDataException {
        return userFactory.getUsersIdsByCreationDate( filterDate, page, pageSize );
    }

    @CloseDBIfOpened
    @Override
    public long getCountUsersByNameOrEmailOrUserID(String filter) throws DotDataException {
        return userFactory.getCountUsersByNameOrEmailOrUserID(filter);
    }

    @CloseDBIfOpened
    @Override
    public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous) throws DotDataException {
        return userFactory.getCountUsersByNameOrEmailOrUserID(filter, includeAnonymous);
    }

    @Override
    public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous, boolean includeDefault) throws DotDataException {
        // TODO Auto-generated method stub
        return getCountUsersByNameOrEmailOrUserID(filter, includeAnonymous, includeDefault, null);
    }

    @CloseDBIfOpened
    @Override
    public long getCountUsersByNameOrEmailOrUserID(String filter, boolean includeAnonymous, boolean includeDefault, String roleId)
            throws DotDataException {
        return userFactory.getCountUsersByNameOrEmailOrUserID(filter, includeAnonymous, includeDefault, roleId);
    }

    @CloseDBIfOpened
    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize) throws DotDataException {
        return userFactory.getUsersByNameOrEmailOrUserID(filter, page, pageSize);
    }

    @CloseDBIfOpened
    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize, boolean includeAnonymous) throws DotDataException {
        return userFactory.getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous);
    }

    @CloseDBIfOpened
    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page, int pageSize, boolean includeAnonymous, String roleId) throws DotDataException {
        return userFactory.getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous,false, roleId);
    }

    @CloseDBIfOpened
    @Override
    public List<User> getUsersByNameOrEmailOrUserID(String filter, int page,
            int pageSize, boolean includeAnonymous, boolean includeDefault, String roleId) throws DotDataException {
        return userFactory.getUsersByNameOrEmailOrUserID(filter, page, pageSize, includeAnonymous, includeDefault,roleId);
    }

    @WrapInTransaction
    @Override
    public void save(final User userToSave, final User user, final boolean respectFrontEndRoles) throws DotDataException, DotSecurityException,DuplicateUserException {
        String userId = userToSave.getUserId();
        if (userId == null) {
            throw new DotDataException("Can't save a user without a userId");
        }
        if (!permissionAPI.doesUserHavePermission(userProxyAPI.getUserProxy(userToSave,
                APILocator.getUserAPI().getSystemUser(), false),
                PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)) {
            throw new DotSecurityException(
                    "User doesn't have permission to save the user which is trying to be saved");
        }
        userFactory.saveUser(userToSave);
        PasswordTrackerLocalManager passwordTracker = PasswordTrackerLocalManagerFactory
                .getManager();
        try {
            if (passwordTracker.isPasswordRecyclingActive()
                    && StringUtils.isNotBlank(userToSave.getPassword())) {
                passwordTracker.trackPassword(userId, userToSave.getPassword());
            }
        } catch (PortalException | SystemException e) {
            SecurityLogger.logInfo(UserAPIImpl.class, "Password for user ["
                    + userId + "] could not be added for tracking.");
        }
        APILocator.getRoleAPI().getUserRole(userToSave);
    }

    @Override
    public void save(User userToSave, User user, boolean validatePassword,
            boolean respectFrontEndRoles) throws DotDataException,
            DotSecurityException, DuplicateUserException {
        String pwd = userToSave.getPassword();
        if (validatePassword) {
            PasswordTrackerLocalManager passwordTracker = PasswordTrackerLocalManagerFactory
                    .getManager();
            try {
                if (!passwordTracker.isValidPassword(userToSave.getUserId(),
                        pwd)) {
                    // Get the first validation error and display it
                    throw new DotDataException(passwordTracker
                            .getValidationErrors().get(0).toString(),"User-Info-Save-Password-Failed");
                }
            } catch (PortalException | SystemException e) {
                throw new DotDataException(
                        "An error occurred during the save process.");
            }

            // Use new password hash method
            try {
                userToSave.setPassword(PasswordFactoryProxy.generateHash(pwd));
            } catch (PasswordException e) {
                Logger.error(UserAPIImpl.class, "An error occurred generating the hashed password for userId: " + userToSave.getUserId(), e);
                throw new DotDataException("An error occurred generating the hashed password.");
            }
        }
        save(userToSave, user, respectFrontEndRoles);
    }

    @Override
    public void delete(User userToDelete, User user, boolean respectFrontEndRoles) throws DotDataException,	DotSecurityException {
        if (userToDelete.getUserId() == null) {
            throw new DotDataException("Can't delete a user without a userId");
        }
        if(!permissionAPI.doesUserHavePermission(userProxyAPI.getUserProxy(userToDelete,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
            throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
        }
        delete(userToDelete,user, user, respectFrontEndRoles);
    }

    @WrapInTransaction
    @Override
    public void delete(User userToDelete, User replacementUser, User user, boolean respectFrontEndRoles) throws DotDataException,	DotSecurityException {
        if (!UtilMethods.isSet(userToDelete) || userToDelete.getUserId() == null) {
            throw new DotDataException("Can't delete a user without a userId");
        }
        if (!UtilMethods.isSet(replacementUser) || replacementUser.getUserId() == null) {
            throw new DotDataException("Can't delete a user without a replacement userId");
        }
        if (userToDelete.getUserId() == replacementUser.getUserId()) {
            throw new DotDataException("Can't delete a user without a replacement userId");
        }
        if (getAnonymousUser().getUserId() == userToDelete.getUserId()){
            throw new DotDataException("Anonymous user can not be deleted.");
        }
        if(!permissionAPI.doesUserHavePermission(userProxyAPI.getUserProxy(userToDelete,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
            throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
        }

        //replace the user reference in Inodes
        logDelete(DeletionStage.BEGINNING, userToDelete, user, "Inodes");

        InodeFactory.updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());

        logDelete(DeletionStage.END, userToDelete, user, "Inodes");

        //replace the user references in contentlets
        logDelete(DeletionStage.BEGINNING, userToDelete, user, "Contentlets");

        ContentletAPI conAPI = APILocator.getContentletAPI();
        conAPI.updateUserReferences(userToDelete,replacementUser.getUserId(), user);

        logDelete(DeletionStage.END, userToDelete, user, "Contentlets");

        //replace the user references in menulink
        logDelete(DeletionStage.BEGINNING, userToDelete, user, "Menulinks");

        MenuLinkAPI menuAPI = APILocator.getMenuLinkAPI();
        menuAPI.updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());

        logDelete(DeletionStage.END, userToDelete, user, "Menulinks");

        //replace the user references in HostVariables
        logDelete(DeletionStage.BEGINNING, userToDelete, user, "HostVariables");
        APILocator.getHostVariableAPI().updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());
        logDelete(DeletionStage.END, userToDelete, user, "HostVariables");




        //replace user references in containers
        logDelete(DeletionStage.BEGINNING, userToDelete, user, "Containers");

        ContainerAPI contAPI = APILocator.getContainerAPI();
        contAPI.updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());

        logDelete(DeletionStage.END, userToDelete, user, "Containers");

        //replace user references in templates
        logDelete(DeletionStage.BEGINNING, userToDelete, user, "Templates");

        TemplateAPI temAPI = APILocator.getTemplateAPI();
        temAPI.updateUserReferences(userToDelete.getUserId(), replacementUser.getUserId());

        logDelete(DeletionStage.END, userToDelete, user, "Templates");

        RoleAPI roleAPI = APILocator.getRoleAPI();
        Role userRole = roleAPI.loadRoleByKey(userToDelete.getUserId());
        Role replacementUserRole = roleAPI.loadRoleByKey(replacementUser.getUserId());

        //replace the user reference in workflows
        logDelete(DeletionStage.BEGINNING, userToDelete, user, "Workflows");

        WorkflowAPI wofAPI = APILocator.getWorkflowAPI();
        wofAPI.updateUserReferences(userToDelete.getUserId(), userRole.getId(), replacementUser.getUserId(),replacementUserRole.getId());

        logDelete(DeletionStage.END, userToDelete, user, "Workflows");

        //replace the user reference in publishing bundles
        logDelete(DeletionStage.BEGINNING, userToDelete, user, "Publishing Bundles");

        bundleAPI.updateOwnerReferences(userToDelete.getUserId(), replacementUser.getUserId());

        logDelete(DeletionStage.END, userToDelete, user, "Publishing Bundles");

        //removing user roles
        permissionAPI.removePermissionsByRole(userRole.getId());
        roleAPI.removeAllRolesFromUser(userToDelete);

        /**
         * Need to edit user name role system value
         * to allow delete the role
         * */
        userRole.setSystem(false);
        userRole=roleAPI.save(userRole);
        roleAPI.delete(userRole);

        /*Delete role*/
        userFactory.delete(userToDelete);

    }

    /**
     *
     * @author Daniel Silva
     * @version 3.7
     * @since Jul 25, 2016
     *
     */
    private enum DeletionStage {
        BEGINNING,
        END
    }

    /**
     *
     * @param stage
     * @param userToDelete
     * @param user
     * @param referenceType
     */
    private void logDelete(DeletionStage stage, User userToDelete, User user, String referenceType) {

        String userToDeleteStr = userToDelete.getUserId() + "/" + userToDelete.getFullName();

        try {
            String msg = stage==DeletionStage.BEGINNING
                    ?
                    MessageFormat.format(LanguageUtil.get(user,
                            "com.dotmarketing.business.UserAPI.delete.beginning"), userToDeleteStr, referenceType)
                    :
                            MessageFormat.format(LanguageUtil.get(user,
                                    "com.dotmarketing.business.UserAPI.delete.end"), userToDeleteStr, referenceType);

            Logger.info(this, msg);

        } catch (LanguageException e) {
            Logger.error(this, "Error logging info of Delete user operation. User: " + userToDeleteStr);
        }
    }

    @WrapInTransaction
    @Override
    public void saveAddress(User user, Address ad, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
        if(!permissionAPI.doesUserHavePermission(userProxyAPI.getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, currentUser, respectFrontEndRoles)){
            throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
        }
        userFactory.saveAddress(user, ad);
    }

    @CloseDBIfOpened
    @Override
    public Address loadAddressById(String addressId, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        Address ad = userFactory.loadAddressById(addressId);
        if(!permissionAPI.doesUserHavePermission(userProxyAPI.getUserProxy(ad.getUserId(),APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_READ, currentUser, respectFrontEndRoles)){
            throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
        }
        return ad;
    }

    @WrapInTransaction
    @Override
    public void deleteAddress(Address ad, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
        if(!permissionAPI.doesUserHavePermission(userProxyAPI.getUserProxy(ad.getUserId(),APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_EDIT, currentUser, respectFrontEndRoles)){
            throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
        }
        userFactory.deleteAddress(ad);
    }

    @CloseDBIfOpened
    @Override
    public List<Address> loadUserAddresses(User user, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotRuntimeException, DotSecurityException {
        if(!permissionAPI.doesUserHavePermission(userProxyAPI.getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false), PermissionAPI.PERMISSION_READ, currentUser, respectFrontEndRoles)){
            throw new DotSecurityException("User doesn't have permission to userToDelete the user which is trying to be saved");
        }
        return userFactory.loadUserAddresses(user);
    }

    @Override
    public boolean isCMSAdmin(User user) throws DotDataException {
        RoleAPI roleAPI = APILocator.getRoleAPI();
        return roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole());
    }

    @Override
    public void updatePassword(User user, String newpass, User currentUser, boolean respectFrontEndRoles) throws DotDataException, DotInvalidPasswordException, DotSecurityException {
        if(!PwdToolkitUtil.validate(newpass)) {
            throw new DotInvalidPasswordException("Invalid password");
        }

        // Use new password hash method
        try {
            user.setPassword(PasswordFactoryProxy.generateHash(newpass));
        } catch (PasswordException e) {
            Logger.error(UserAPIImpl.class, "An error occurred generating the hashed password for userId: " + user.getUserId(), e);
            throw new DotDataException("An error occurred generating the hashed password.");
        }

        user.setIcqId("");
        user.setPasswordReset(GetterUtil.getBoolean(
                PropsUtil.get(PropsUtil.PASSWORDS_CHANGE_ON_FIRST_USE)));
        save(user, currentUser, respectFrontEndRoles);

    }

    @WrapInTransaction
    @Override
    public void markToDelete(User userToDelete) throws DotDataException {
        userToDelete.setDeleteInProgress(true);
        userToDelete.setDeleteDate(Calendar.getInstance().getTime());
        userFactory.saveUser(userToDelete);
    }

    @CloseDBIfOpened
    @Override
    public List<User> getUnDeletedUsers() throws DotDataException {
        return userFactory.getUnDeletedUsers();
    }

}
