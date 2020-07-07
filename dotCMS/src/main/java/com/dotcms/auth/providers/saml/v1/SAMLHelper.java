package com.dotcms.auth.providers.saml.v1;

import com.dotcms.saml.Attributes;
import com.dotcms.saml.DotSamlConstants;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlException;
import com.dotcms.saml.SamlName;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.dotmarketing.util.UtilMethods.isSet;

public class SAMLHelper {

    private final HostWebAPI hostWebAPI;
    private final UserAPI    userAPI;
    private final RoleAPI    roleAPI;
    private final SamlAuthenticationService  samlAuthenticationService;

    public SAMLHelper(final SamlAuthenticationService samlAuthenticationService) {

        this.userAPI      = APILocator.getUserAPI();
        this.roleAPI      = APILocator.getRoleAPI();
        this.hostWebAPI   = WebAPILocator.getHostWebAPI();
        this.samlAuthenticationService = samlAuthenticationService;
    }

    // Gets the attributes from the Assertion, based on the attributes
    // see if the user exists return it from the dotCMS records, if does not
    // exist then, tries to create it.
    // the existing or created user, will be updated the roles if they present
    // on the assertion.
    protected User resolveUser(final Attributes attributes,
                             final IdentityProviderConfiguration identityProviderConfiguration) {

        User user       = null;
        User systemUser = null;
        try {

            Logger.debug(this, ()-> "Validating user - " + attributes);

            systemUser             = this.userAPI.getSystemUser();
            final Company company  = APILocator.getCompanyAPI().getDefaultCompany();
            final String  authType = company.getAuthType();
            user                   = Company.AUTH_TYPE_ID.equals(authType)?
                    this.userAPI.loadUserById(this.samlAuthenticationService.getValue(attributes.getNameID()),      systemUser, false):
                    this.userAPI.loadByUserByEmail(this.samlAuthenticationService.getValue(attributes.getNameID()), systemUser, false);
        } catch (NoSuchUserException e) {

            Logger.error(this, "No user matches ID '" +
                    this.samlAuthenticationService.getValue(attributes.getNameID()) + "'. Creating one...", e);
            user = null;
        } catch (Exception e) {

            Logger.error(this, "An error occurred when loading user with ID '" +
                    this.samlAuthenticationService.getValue(attributes.getNameID()) + "'", e);
            user = null;
        }

        // check if the client wants synchronization
        final boolean createUserWhenDoesNotExists = DotSamlProxyFactory.getInstance()
                .samlConfigurationService().getConfigAsBoolean(identityProviderConfiguration, SamlName.DOT_SAML_ALLOW_USER_SYNCHRONIZATION);
        if (createUserWhenDoesNotExists) {

            user = null == user?
                    this.createNewUser(systemUser,    attributes, identityProviderConfiguration):  // if user does not exists, create a new one.
                    this.updateUser(user, systemUser, attributes, identityProviderConfiguration); // update it, since exists

            if (user.isActive()) {

                this.addRoles(user, attributes, identityProviderConfiguration);
            } else {

                Logger.info(this, ()-> "User with ID '" + this.samlAuthenticationService.getValue(attributes.getNameID()) + "' is not active. No roles " +
                        "were added.");
            }
        }

        return user;
    }

    protected User updateUser(final User user, final User systemUser,
                              final Attributes attributesBean, final IdentityProviderConfiguration identityProviderConfiguration) {
        try {

            if (DotSamlProxyFactory.getInstance().samlConfigurationService()
                    .getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL)){

                user.setEmailAddress(attributesBean.getEmail());
            }

            user.setFirstName(attributesBean.getFirstName());
            user.setLastName(attributesBean.getLastName());

            this.userAPI.save(user, systemUser, false);
            Logger.debug(this, ()-> "User with email '" + attributesBean.getEmail() + "' has been updated");
        } catch (Exception e) {

            Logger.error(this, "Error updating user with email '" + attributesBean.getEmail() + "': " + e.getMessage()
                    , e);
            throw new SamlException(e.getMessage());
        }

        return user;
    }

    private String getBuildRoles(final IdentityProviderConfiguration identityProviderConfiguration) {

        final String buildRolesStrategy = DotSamlProxyFactory.getInstance()
                .samlConfigurationService().getConfigAsString(identityProviderConfiguration, SamlName.DOTCMS_SAML_BUILD_ROLES);

        return this.checkBuildRoles(buildRolesStrategy)?
                buildRolesStrategy: this.getDefaultBuildRoles(buildRolesStrategy);
    }

    private String getDefaultBuildRoles(final String invalidBuildRolesStrategy) {
        Logger.info(this, ()-> "The build.roles: " + invalidBuildRolesStrategy + ", property is invalid. Using the default " +
                "strategy: " + DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_ALL_VALUE);

        return DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_ALL_VALUE;
    }

    public boolean checkBuildRoles(final String buildRolesProperty) {

        return DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_ALL_VALUE.equalsIgnoreCase( buildRolesProperty )  ||
                DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_IDP_VALUE.equalsIgnoreCase( buildRolesProperty ) ||
                DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_STATIC_ONLY_VALUE.equalsIgnoreCase( buildRolesProperty ) ||
                DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_STATIC_ADD_VALUE.equalsIgnoreCase( buildRolesProperty )  ||
                DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_NONE_VALUE.equalsIgnoreCase( buildRolesProperty );
    }

    private void addRoles(final User user, final Attributes attributesBean, final IdentityProviderConfiguration identityProviderConfiguration) {

        final String buildRolesStrategy = this.getBuildRoles(identityProviderConfiguration);

        Logger.debug(this, ()-> "Using the build roles Strategy: " + buildRolesStrategy);

        if (!DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_NONE_VALUE.equalsIgnoreCase(buildRolesStrategy)) {
            try {
                // remove previous roles
                if (!DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_STATIC_ADD_VALUE.equalsIgnoreCase(buildRolesStrategy)) {

                    Logger.debug(this, ()-> "Removing ALL existing roles from user '" + user.getUserId() + "'...");
                    this.roleAPI.removeAllRolesFromUser(user);
                } else {

                    Logger.debug(this, ()-> "The buildRoles strategy is: 'staticadd'. It won't remove any existing dotCMS role");
                }

                this.handleRoles(user, attributesBean, identityProviderConfiguration, buildRolesStrategy);
            } catch (DotDataException e) {

                Logger.error(this, "Error adding roles to user '" + user.getUserId() + "': " + e.getMessage(), e);
                throw new SamlException(e.getMessage());
            }
        } else {

            Logger.info(this, ()->"The build roles strategy is 'none'. No user roles were added/changed.");
        }
    }

    private void handleRoles(final User user, final Attributes attributesBean,
                             final IdentityProviderConfiguration identityProviderConfiguration,
                             final String buildRolesStrategy) throws DotDataException {

        this.addRolesFromIDP(user, attributesBean, identityProviderConfiguration, buildRolesStrategy);

        // Add SAML User role
        this.addRole(user, DotSamlConstants.DOTCMS_SAML_USER_ROLE, true, true);
        Logger.debug(this, ()->"Default SAML User role has been assigned");

        // the only strategy that does not include the saml user role is the "idp"
        if (!DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_IDP_VALUE.equalsIgnoreCase(buildRolesStrategy)) {
            // Add DOTCMS_SAML_OPTIONAL_USER_ROLE
            if (DotSamlProxyFactory.getInstance().samlConfigurationService().getConfigAsString(identityProviderConfiguration,
                    SamlName.DOTCMS_SAML_OPTIONAL_USER_ROLE) != null) {

                this.addRole(user, DotSamlProxyFactory.getInstance().samlConfigurationService().getConfigAsString(identityProviderConfiguration,
                        SamlName.DOTCMS_SAML_OPTIONAL_USER_ROLE), false, false);
                Logger.debug(this, ()-> "Optional user role: " +
                        DotSamlProxyFactory.getInstance().samlConfigurationService().getConfigAsString(identityProviderConfiguration,
                                SamlName.DOTCMS_SAML_OPTIONAL_USER_ROLE) + " has been assigned");
            }
        } else {

            Logger.info(this, "The build roles strategy is 'idp'. No saml_user_role has been added");
        }
    }

    private boolean isValidRole(final String role, final String[] rolePatterns) {

        boolean isValidRole = false;

        if (null != rolePatterns) {
            for (final String rolePattern : rolePatterns) {
                Logger.debug(this, ()-> "Valid Role: " + role + ", pattern: " + rolePattern);
                isValidRole |= this.match(role, rolePattern);
            }
        } else {
            // if not pattern, role is valid.
            isValidRole = true;
        }

        return isValidRole;
    }

    private boolean match(final String role, final String rolePattern) {
        String uftRole = null;

        try {

            uftRole = URLDecoder.decode(role, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            uftRole = role;
        }

        return RegEX.contains(uftRole, rolePattern);
    }

    private void addRolesFromIDP(final User user, final Attributes attributesBean, final IdentityProviderConfiguration identityProviderConfiguration,
                                 final String buildRolesStrategy) throws DotDataException {

        final boolean includeIDPRoles = DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_ALL_VALUE.equalsIgnoreCase(buildRolesStrategy)
                || DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_IDP_VALUE.equalsIgnoreCase(buildRolesStrategy);

        Logger.debug(this, ()-> "Including roles from IdP '" + includeIDPRoles + "' for the build roles Strategy: " + buildRolesStrategy);

        if (includeIDPRoles && attributesBean.isAddRoles() && null != attributesBean.getRoles()) {

            final List<String> roleList = this.samlAuthenticationService.getValues(attributesBean.getRoles());
            if (null != roleList && roleList.size() > 0) {

                final String removeRolePrefix = DotSamlProxyFactory.getInstance().samlConfigurationService().getConfigAsString(
                        identityProviderConfiguration, SamlName.DOT_SAML_REMOVE_ROLES_PREFIX);

                final String[] rolePatterns = DotSamlProxyFactory.getInstance().samlConfigurationService().getConfigAsArrayString(
                        identityProviderConfiguration, SamlName.DOTCMS_SAML_INCLUDE_ROLES_PATTERN);

                Logger.debug(this, () -> "Role Patterns: " + this.toString(rolePatterns) + ", remove role prefix: " + removeRolePrefix);

                // add roles
                for (final String role : roleList) {

                    if (null != rolePatterns && rolePatterns.length > 0) {
                        if (!this.isValidRole(role, rolePatterns)) {
                            // when there are role filters and the current roles is not a valid role, we have to filter it.

                            Logger.debug(this, () -> "Skipping role: " + role);
                            continue;
                        } else {

                            Logger.debug(this, () -> "Role Patterns: " + this.toString(rolePatterns) + ", remove role prefix: "
                                    + removeRolePrefix + ": true");
                        }
                    }

                    this.addRole(user, removeRolePrefix, role);
                }
            }

            return;
        }

        Logger.info(this, "Roles have been ignore by the build roles strategy: " + buildRolesStrategy
                + ", or roles have been not set from the IdP");
    }

    private void addRole(final User user, final String removeRolePrefix, final String roleObject)
            throws DotDataException {

        // remove role prefix
        final String roleKey = isSet(removeRolePrefix)?
                roleObject.replaceFirst(removeRolePrefix, StringUtils.EMPTY):
                roleObject;

        addRole(user, roleKey, false, false);
    }

    private void addRole(final User user, final String roleKey, final boolean createRole, final boolean isSystem)
            throws DotDataException {

        Role role = this.roleAPI.loadRoleByKey(roleKey);

        // create the role, in case it does not exist
        if (role == null && createRole) {
            Logger.info(this, "Role with key '" + roleKey + "' was not found. Creating it...");
            role = createNewRole(roleKey, isSystem);
        }

        if (null != role) {
            if (!this.roleAPI.doesUserHaveRole(user, role)) {
                this.roleAPI.addRoleToUser(role, user);
                Logger.debug(this, "Role named '" + role.getName() + "' has been added to user: " + user.getEmailAddress());
            } else {
                Logger.debug(this,
                        "User '" + user.getEmailAddress() + "' already has the role '" + role + "'. Skipping assignment...");
            }
        } else {
            Logger.debug(this, "Role named '" + roleKey + "' does NOT exists in dotCMS. Ignoring it...");
        }
    }

    private Role createNewRole(String roleKey, boolean isSystem) throws DotDataException {
        Role role = new Role();
        role.setName(roleKey);
        role.setRoleKey(roleKey);
        role.setEditUsers(true);
        role.setEditPermissions(false);
        role.setEditLayouts(false);
        role.setDescription("");
        role.setId(UUIDGenerator.generateUuid());

        // Setting SYSTEM role as a parent
        role.setSystem(isSystem);
        Role parentRole = roleAPI.loadRoleByKey(Role.SYSTEM);
        role.setParent(parentRole.getId());

        String date = DateUtil.getCurrentDate();

        ActivityLogger.logInfo(ActivityLogger.class, getClass() + " - Adding Role",
                "Date: " + date + "; " + "Role:" + roleKey);
        AdminLogger.log(AdminLogger.class, getClass() + " - Adding Role", "Date: " + date + "; " + "Role:" + roleKey);

        try {
            role = roleAPI.save(role, role.getId());
        } catch (DotDataException | DotStateException e) {
            ActivityLogger.logInfo(ActivityLogger.class, getClass() + " - Error adding Role",
                    "Date: " + date + ";  " + "Role:" + roleKey);
            AdminLogger.log(AdminLogger.class, getClass() + " - Error adding Role",
                    "Date: " + date + ";  " + "Role:" + roleKey);
            throw e;
        }

        return role;
    }

    private String toString(final String... rolePatterns) {
        return null == rolePatterns ? DotSamlConstants.NULL : Arrays.asList(rolePatterns).toString();
    }

    protected User createNewUser(final User systemUser, final Attributes attributesBean,
                                 final IdentityProviderConfiguration identityProviderConfiguration) {
        User user = null;

        try {

            final String nameID = this.samlAuthenticationService.getValue(attributesBean.getNameID());
            try {

                user = this.userAPI.createUser(nameID, attributesBean.getEmail());
            } catch (DuplicateUserException due) {

                user = this.onDuplicateUser(attributesBean, identityProviderConfiguration, nameID);
            }

            user.setFirstName(attributesBean.getFirstName());
            user.setLastName(attributesBean.getLastName());
            user.setActive(true);

            user.setCreateDate(new Date());
            user.setPassword(PublicEncryptionFactory.digestString(UUIDGenerator.generateUuid() + "/" + UUIDGenerator.generateUuid()));
            user.setPasswordEncrypted(true);

            this.userAPI.save(user, systemUser, false);
            Logger.debug(this, ()-> "User with NameID '" + nameID + "' and email '" +
                    attributesBean.getEmail() + "' has been created.");

        } catch (Exception e) {

            final String errorMsg = "Error creating user with NameID '" + this.samlAuthenticationService.getValue(attributesBean.getNameID()) + "': " +
                    "" + e.getMessage();
            Logger.error(this, errorMsg, e);
            throw new SamlException(errorMsg);
        }

        return user;
    }

    private User onDuplicateUser(final Attributes attributesBean,
                                 final IdentityProviderConfiguration identityProviderConfiguration,
                                 final String nameID) throws DotDataException {

        User user;
        final String companyDomain =
                DotSamlProxyFactory.getInstance().samlConfigurationService().getConfigAsString(
                        identityProviderConfiguration, SamlName.DOTCMS_SAML_COMPANY_EMAIL_DOMAIN, ()->"fakedomain.com");

        Logger.warn(this, ()->"NameId " + nameID + " or email: " + attributesBean.getEmail() +
                ", are duplicated. User could not be created, trying the new email strategy");

        final String newEmail = nameID + "@" + companyDomain;

        try {

            user = this.userAPI.createUser(nameID, newEmail);
            Logger.info(this, ()-> "UserID: "+ nameID + " has been created with email: " + newEmail);

        } catch (DuplicateUserException dueAgain) {

            Logger.warn(this, ()-> "NameId " + nameID
                    + " or email: " + attributesBean.getEmail() +
                    ", are duplicated. User could not be created, trying the UUID strategy");

            final String newUUIDEmail = UUIDGenerator.generateUuid() + "@" + companyDomain;
            user = this.userAPI.createUser(nameID, newUUIDEmail);

            Logger.info(this, ()-> "UserID: "+ nameID +
                    " has been created created with email: " + newUUIDEmail);
        }

        return user;
    }

    protected void doRequestLoginSecurityLog(final HttpServletRequest request,
                                          final IdentityProviderConfiguration identityProviderConfiguration) {

        try {

            final Host host  = this.hostWebAPI.getCurrentHost(request);
            final String env = this.isFrontEndLoginPage(request.getRequestURI()) ? "frontend" : "backend";
            final String log = new Date() + ": SAML login request for Site '" + host.getHostname() + "' with IdP ID: "
                    + identityProviderConfiguration.getId() + " (" + env + ") from " + request.getRemoteAddr();

            // “$TIMEDATE: SAML login request for $host (frontend|backend)from
            // $REQUEST_ADDR”
            SecurityLogger.logInfo(SecurityLogger.class, this.getClass() + " - " + log);
            Logger.debug(this, ()-> log);
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    protected boolean isFrontEndLoginPage(final String uri) {

        return uri.startsWith("/dotCMS/login") || uri.startsWith("/application/login");
    }

}
