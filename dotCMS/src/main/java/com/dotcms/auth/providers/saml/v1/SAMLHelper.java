package com.dotcms.auth.providers.saml.v1;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.company.CompanyAPI;
import com.dotcms.filters.interceptor.saml.SamlWebInterceptor;
import com.dotcms.rest.api.v1.user.UserHelper;
import com.dotcms.saml.Attributes;
import com.dotcms.saml.DotSamlConstants;
import com.dotcms.saml.DotSamlException;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.Encryptor;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.dotmarketing.util.UtilMethods.isSet;

/**
 * SAML Helper for the Endpoints
 * @author jsanca
 */
public class SAMLHelper {

    protected static final String DO_HASH_KEY = "hash.userid";
    // when on, look for an user by id, if the email is repeated on the system just fakes coming email to avoid the email constraint
    protected static final String ALLOW_USERS_DIFF_ID_REPEATED_EMAIL_KEY = "allowusers.diffid.repeatedemail";

    private final HostWebAPI hostWebAPI;
    private final UserAPI    userAPI;
    private final RoleAPI    roleAPI;
    private final CompanyAPI companyAPI;
    private final SamlAuthenticationService  samlAuthenticationService;
    private static SamlConfigurationService  thirdPartySamlConfigurationService;

    private static EmailGenStrategy emailGenStrategy = SAMLHelper::prefixRandomEmail;

    protected static final RoleGroupMappingStrategy DEFAULT_ROLE_GROUP_MAPPING_STRATEGY = (roleGroupKey, identityProviderConfiguration) -> List.of(roleGroupKey);
    public static final String SAML_ROLES_GROUP_MAPPING_STRATEGY_BYCONTENTTYPE = "saml.roles.group.mapping.strategy.bycontenttype";
    private static Map<String, RoleGroupMappingStrategy> roleGroupMappingStrategiesMap =
            new ConcurrentHashMap<>(Map.of(SAML_ROLES_GROUP_MAPPING_STRATEGY_BYCONTENTTYPE, new ContentTypeRoleGroupMappingStrategyImpl()));

    private static String prefixRandomEmail(final String email) {

        final int randomNumber = RandomUtils.nextInt();
        return randomNumber + email;
    }

    public SAMLHelper(final SamlAuthenticationService samlAuthenticationService, final CompanyAPI companyAPI) {

        this.userAPI      = APILocator.getUserAPI();
        this.roleAPI      = APILocator.getRoleAPI();
        this.hostWebAPI   = WebAPILocator.getHostWebAPI();
        this.companyAPI   = companyAPI;
        this.samlAuthenticationService = samlAuthenticationService;
    }

    @VisibleForTesting
    protected static void setEmailGenStrategy(final EmailGenStrategy emailGenStrategy) {
        if (null != emailGenStrategy) {
            SAMLHelper.emailGenStrategy = emailGenStrategy;
        }
    }

    @VisibleForTesting
    protected static void setThirdPartySamlConfigurationService(final SamlConfigurationService thirdPartySamlConfigurationService) {
        SAMLHelper.thirdPartySamlConfigurationService = thirdPartySamlConfigurationService;
    }

    private SamlConfigurationService getSamlConfigurationService() {

        return null != SAMLHelper.thirdPartySamlConfigurationService?
                SAMLHelper.thirdPartySamlConfigurationService: DotSamlProxyFactory.getInstance().samlConfigurationService();
    }

    protected void doLogin(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final IdentityProviderConfiguration identityProviderConfiguration,
                           final User user,
                           final LoginServiceAPI loginServiceAPI) {

        // we are going to do the autologin, so if the session is null,
        // create it!
        try {

            Logger.debug(this, "User with ID '" + user.getUserId()
                    + "' has been returned by SAML Service. User " + "Map: " + user.toMap());
        } catch (Exception e) {

            Logger.error(this,
                    "An error occurred when retrieving data from user '" + user.getUserId() + "': " + e.getMessage(), e);
        }

        final boolean doCookieLogin = loginServiceAPI
                .doCookieLogin(EncryptorFactory.getInstance().getEncryptor().encryptString(user.getUserId()), request, response);

        Logger.debug(this, ()->"Cookie Login by LoginService = " + doCookieLogin);

        if (doCookieLogin) {

            final HttpSession session = request.getSession(false);
            if (null != session && null != user.getUserId()) {
                // this is what the PortalRequestProcessor needs to check the login.
                Logger.debug(this, ()->"Adding user ID '" + user.getUserId() + "' to the session");

                final String uri = session.getAttribute(SamlWebInterceptor.ORIGINAL_REQUEST) != null?
                        (String) session.getAttribute(SamlWebInterceptor.ORIGINAL_REQUEST):
                        request.getRequestURI();

                Logger.debug(this,"SAML, uri: " + uri);

                session.removeAttribute(SamlWebInterceptor.ORIGINAL_REQUEST);

                Logger.debug(this, ()->  "URI '" + uri + "' belongs to the back-end. Setting the user session data");
                session.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
                session.setAttribute(com.liferay.portal.util.WebKeys.USER,    user);
                PrincipalThreadLocal.setName(user.getUserId());

                try {

                    final Host   host  = this.hostWebAPI.getCurrentHost(request);
                    final String env   = request.getRequestURI().startsWith("/dotCMS/login") || request.getRequestURI().startsWith("/application/login")? "frontend" : "backend";
                    final String log   = new Date() + ": Successfull SAML login for Site '" + host.getHostname() + "' with IdP " +
                            "ID: " + identityProviderConfiguration.getId() + " (" + env + ") from " + request.getRemoteAddr() + " for user: " +
                            user.getEmailAddress();

                    // “$TIMEDATE: SAML login success for $host (frontend|backend) from $REQUEST_ADDR for user $username”
                    SecurityLogger.logInfo(SecurityLogger.class, this.getClass() + " - " + log);
                    Logger.info(this, log);
                } catch (Exception e) {

                    Logger.error(this, e.getMessage(), e);
                }
            }
        }
    } // doLogin.

    /*
    * Tries to load the user by id, if not found, tries to hash the user id in case the id was previously hashed.
     */
    private User loadUserById (final String userId, final User currentUser, final boolean doHash) throws DotSecurityException, DotDataException {

        User user = null;

        // first try unhashed
        user = Try.of(()->this.userAPI.loadUserById(userId, currentUser, false)).getOrNull();

        if(null == user) {

            // not found, try hashed
            final String hashedUserId= Try.of(()->this.hashIt(userId, doHash)).getOrNull();

            if (null != hashedUserId) {

                user = this.userAPI.loadUserById(hashedUserId, currentUser, false);
            }
        }

        return user;
    }

    protected User resolveUser(final User systemUser, final Attributes attributes,  final String nameId,
                               final IdentityProviderConfiguration identityProviderConfiguration) {

        User user = null;

        try {

            Logger.debug(this, ()-> "Validating user - " + attributes);

            final Company company  = companyAPI.getDefaultCompany();
            final String  authType = company.getAuthType();
            final boolean doHash   = identityProviderConfiguration.containsOptionalProperty(DO_HASH_KEY)?
                    BooleanUtils.toBoolean(identityProviderConfiguration.getOptionalProperty(DO_HASH_KEY).toString()):true;
            user                   = Company.AUTH_TYPE_ID.equals(authType)?
                    this.loadUserById(nameId, systemUser, doHash):
                    this.userAPI.loadByUserByEmail(nameId, systemUser, false);
        } catch (NoSuchUserException e) {

            final String email = this.samlAuthenticationService.getValue(attributes.getEmail());
            Logger.warn(this, String.format("No user matches ID '%s'. Checking for email match with '%s' instead...",
                    nameId, email));
            try {
                user = this.userAPI.loadByUserByEmail(email, systemUser, false);
            } catch (final DotDataException | DotSecurityException | NoSuchUserException ex) {
                Logger.error(this, "An error occurred when resolving user with email '" + (UtilMethods.isSet(email) ?
                        email : "-null-") + "'", e);
                user = null;
            }
        } catch (Exception e) {

            Logger.error(this, String.format("An error occurred when resolving user with ID '%s': %s", nameId, e
                    .getMessage()), e);
            user = null;
        }

        return user;
    }

    protected User resolveUserById(final User systemUser, final Attributes attributes, final String nameId,
                                   final IdentityProviderConfiguration identityProviderConfiguration) {

        try {

            Logger.debug(this, ()-> "Looking for an user per id - " + nameId);

            final boolean doHash   = identityProviderConfiguration.containsOptionalProperty(DO_HASH_KEY)?
                    BooleanUtils.toBoolean(identityProviderConfiguration.getOptionalProperty(DO_HASH_KEY).toString()):true;
            return  this.loadUserById(nameId, systemUser, doHash);
        } catch (Exception e) {

            Logger.error(this, String.format("An error occurred when resolving user with ID '%s': %s", nameId, e
                    .getMessage()), e);
            return null;
        }
    }

    private boolean allowUsersDiffIdWithRepeatedEmailOn (final IdentityProviderConfiguration identityProviderConfiguration) {

        return identityProviderConfiguration.containsOptionalProperty(ALLOW_USERS_DIFF_ID_REPEATED_EMAIL_KEY)?
                BooleanUtils.toBoolean(identityProviderConfiguration.getOptionalProperty(ALLOW_USERS_DIFF_ID_REPEATED_EMAIL_KEY).toString()):false;
    }

    // Gets the attributes from the Assertion, based on the attributes
    // see if the user exists return it from the dotCMS records, if does not
    // exist then, tries to create it.
    // the existing or created user, will be updated the roles if they present
    // on the assertion.
    protected User resolveUser(final Attributes attributes,
                             final IdentityProviderConfiguration identityProviderConfiguration) {

        if (null == attributes || !UtilMethods.isSet(attributes.getNameID())) {

            Logger.error(this, "Failed to resolve user because Attributes or NameID are null");
            throw new DotSamlException("Failed to resolve user because Attributes or NameID are null");
        }

        final boolean allowUsersDiffIdWithRepeatedEmailOn = allowUsersDiffIdWithRepeatedEmailOn(identityProviderConfiguration);
        final User systemUser = APILocator.systemUser();
        final String nameId   = Try.of(()->this.samlAuthenticationService.getValue(attributes.getNameID())).getOrNull();
        User user             =  allowUsersDiffIdWithRepeatedEmailOn?
                                resolveUserById(systemUser, attributes, nameId, identityProviderConfiguration):
                                resolveUser(systemUser, attributes, nameId, identityProviderConfiguration);

        // check if the client wants synchronization
        final SamlConfigurationService samlConfigurationService = this.getSamlConfigurationService();
        final boolean createUserWhenDoesNotExists =
                null != samlConfigurationService?
                        samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOT_SAML_ALLOW_USER_SYNCHRONIZATION): true;

        if (createUserWhenDoesNotExists) {

            user = null == user?
                    this.createNewUser(systemUser,
                            allowUsersDiffIdWithRepeatedEmailOn? checkRepeatedEmail(attributes):attributes,
                            identityProviderConfiguration):  // if user does not exist, create a new one.
                    this.updateUser(user, systemUser,
                            allowUsersDiffIdWithRepeatedEmailOn? keepCurrentEmail(attributes, user):attributes,
                            identityProviderConfiguration); // update it, since exists

            if (user.isActive()) {

                this.addRoles(user, attributes, identityProviderConfiguration);
            } else {

                Logger.info(this, ()-> "User with ID '" + this.samlAuthenticationService.getValue(attributes.getNameID()) + "' is not active. No roles " +
                        "were added.");
            }
        }

        return user;
    }

    protected Attributes checkRepeatedEmail (final Attributes attributes) {

        final String email = this.samlAuthenticationService.getValue(attributes.getEmail());
        final boolean emailExist = Try.of(()->null != this.userAPI.loadByUserByEmail(email,
                APILocator.systemUser(), false)).getOrElse(false);
        if (emailExist) {

            return
                    new Attributes.Builder()
                            .nameID(attributes.getNameID())
                            .firstName(attributes.getFirstName())
                            .lastName(attributes.getLastName())
                            .addRoles(attributes.isAddRoles())
                            .roles(attributes.getRoles())
                            .sessionIndex(attributes.getSessionIndex())
                            .email(this.makeEmailUnique(email)).build();
        }

        return attributes;
    }

    protected Attributes keepCurrentEmail(Attributes attributes, User user) {

        return
                new Attributes.Builder()
                        .nameID(attributes.getNameID())
                        .firstName(attributes.getFirstName())
                        .lastName(attributes.getLastName())
                        .addRoles(attributes.isAddRoles())
                        .roles(attributes.getRoles())
                        .sessionIndex(attributes.getSessionIndex())
                        .email(user.getEmailAddress()).build();
    }

    private String makeEmailUnique(final String email) {
        return emailGenStrategy.apply(email);
    }

    protected User updateUser(final User user, final User systemUser,
                              final Attributes attributesBean, final IdentityProviderConfiguration identityProviderConfiguration) {
        try {

            final SamlConfigurationService samlConfigurationService = this.getSamlConfigurationService();
            if (samlConfigurationService
                    .getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL)){

                user.setEmailAddress(attributesBean.getEmail());
            }

            user.setFirstName(attributesBean.getFirstName());
            user.setLastName(attributesBean.getLastName());

            // setting the last login date
            user.setLastLoginDate(new Date());

            if (Objects.nonNull(attributesBean.getAdditionalAttributes()) &&
                    attributesBean.getAdditionalAttributes().size() > 0) {

                final Map<String, Object> additionalInfo = user.getAdditionalInfo();
                final Map<String, Object> additionalAttr = attributesBean.getAdditionalAttributes();
                final Map<String, Object> finalAdditionalInfo = new HashMap<>();
                finalAdditionalInfo.putAll(additionalAttr); // assumes override the idp info
                if (samlConfigurationService // by default it overrides
                        .getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_MERGE_ADDITIONAL_ATTRIBUTES, ()->false)) {
                    Logger.debug(this, ()-> "Merging additional attributes for user with email '" + attributesBean.getEmail() + "'");
                    // merge
                    finalAdditionalInfo.putAll(additionalInfo);
                    Logger.debug(this, ()-> "Merged additional attributes for user with email '"
                            + attributesBean.getEmail() + "', attributes: " + finalAdditionalInfo);
                }

                user.setAdditionalInfo(finalAdditionalInfo);
            }

            this.userAPI.save(user, systemUser, false);
            Logger.debug(this, ()-> "User with email '" + attributesBean.getEmail() + "' has been updated");
        } catch (Exception e) {

            Logger.error(this, "Error updating user with email '" + attributesBean.getEmail() + "': " + e.getMessage()
                    , e);
            throw new DotSamlException(e.getMessage(), e);
        }

        return user;
    }

    private String getBuildRoles(final IdentityProviderConfiguration identityProviderConfiguration) {

        final SamlConfigurationService samlConfigurationService = this.getSamlConfigurationService();
        final String buildRolesStrategy = samlConfigurationService == null?
                DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_ALL_VALUE: // if not config, use all as a default
                samlConfigurationService.getConfigAsString(identityProviderConfiguration, SamlName.DOTCMS_SAML_BUILD_ROLES);

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
                throw new DotSamlException(e.getMessage(), e);
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
        UserHelper.getInstance().addRole(user, DotSamlConstants.DOTCMS_SAML_USER_ROLE, true, true);
        Logger.debug(this, ()->"Default SAML User role has been assigned");

        // the only strategy that does not include the saml user role is the "idp"
        if (!DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_IDP_VALUE.equalsIgnoreCase(buildRolesStrategy)) {
            // Add DOTCMS_SAML_OPTIONAL_USER_ROLE
            if (this.getSamlConfigurationService().getConfigAsString(identityProviderConfiguration,
                    SamlName.DOTCMS_SAML_OPTIONAL_USER_ROLE) != null) {

                final String [] rolesExtra = this.getSamlConfigurationService().getConfigAsString(identityProviderConfiguration,
                        SamlName.DOTCMS_SAML_OPTIONAL_USER_ROLE).split(",");

                for (final String roleExtra : rolesExtra){

                    UserHelper.getInstance().addRole(user, roleExtra, false, false);
                    Logger.debug(this, () -> "Optional user role: " +
                            this.getSamlConfigurationService().getConfigAsString(identityProviderConfiguration,
                                    SamlName.DOTCMS_SAML_OPTIONAL_USER_ROLE) + " has been assigned");
                }
            }
        } else {

            Logger.info(this, "The build roles strategy is 'idp'. No saml_user_role has been added");
        }
    }

    private boolean isValidRole(final String role, final String... rolePatterns) {

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

                final String removeRolePrefix = this.getSamlConfigurationService().getConfigAsString(
                        identityProviderConfiguration, SamlName.DOT_SAML_REMOVE_ROLES_PREFIX);

                final String[] rolePatterns = this.getSamlConfigurationService().getConfigAsArrayString(
                        identityProviderConfiguration, SamlName.DOTCMS_SAML_INCLUDE_ROLES_PATTERN);

                final Optional<Tuple2<String, String>> roleKeySubstitutionOpt = this.getRoleKeySubstitution (identityProviderConfiguration);

                Logger.debug(this, () -> "Role Patterns: " + this.toString(rolePatterns) + ", remove role prefix: " + removeRolePrefix);

                // add roles
                addIDPRoles(user, roleList, rolePatterns, removeRolePrefix,
                        roleKeySubstitutionOpt, identityProviderConfiguration);
            }

            return;
        }

        Logger.info(this, "Roles have been ignore by the build roles strategy: " + buildRolesStrategy
                + ", or roles have been not set from the IdP");
    }

    /**
     * This method add the roles form the IDP to the user
     * In addition apply any filter on rolePatterns if they are present
     * Also, remove any prefix from the roles if it is present and use the roleKeySubstitutionOpt to do replacements over the role keys if it is present
     * Finally, if the roleGroupMappingStrategy is present, it will use it to get the roles for the roleGroup
     * @param user
     * @param roleList
     * @param rolePatterns
     * @param removeRolePrefix
     * @param roleKeySubstitutionOpt
     * @param identityProviderConfiguration
     * @throws DotDataException
     */
    private void addIDPRoles(final User user,
                             final List<String> roleList,
                             final String[] rolePatterns,
                             final String removeRolePrefix,
                             final Optional<Tuple2<String, String>> roleKeySubstitutionOpt,
                             final IdentityProviderConfiguration identityProviderConfiguration) throws DotDataException {

        final RoleGroupMappingStrategy roleGroupMappingStrategy = getRoleGroupMappingStrategy(identityProviderConfiguration);

        Logger.debug(this, ()-> "Using roleGroupMappingStrategy: " + roleGroupMappingStrategy);
        for (final String roleGroup : roleList) {

            final Collection<String> rolesMapped = Objects.nonNull(roleGroupMappingStrategy)?
                    roleGroupMappingStrategy.getRolesForGroup(roleGroup, identityProviderConfiguration): List.of(roleGroup);

            Logger.debug(this, ()-> "Roles Mapped: " + rolesMapped);

            for (final String role : rolesMapped) {
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

                this.addRole(user, removeRolePrefix, this.processReplacement(role, roleKeySubstitutionOpt));
            }
        }
    }

    /**
     * Allows to add a new strategy via osgi by strategyName
     * @param strategyNameId String unique name for the strategy
     * @param roleGroupMappingStrategy
     */
    public static void addRoleGroupMappingStrategy(final String strategyNameId, final RoleGroupMappingStrategy roleGroupMappingStrategy) {
        Logger.debug(SAMLHelper.class, ()-> "Adding the role group mapping strategy: " + strategyNameId);
        roleGroupMappingStrategiesMap.put(strategyNameId, roleGroupMappingStrategy);
    }

    public static void removeRoleGroupMappingStrategy(final String strategyNameId) {
        if (roleGroupMappingStrategiesMap.containsKey(strategyNameId)) {
            Logger.debug(SAMLHelper.class, ()-> "Removing the role group mapping strategy: " + strategyNameId);
            roleGroupMappingStrategiesMap.remove(strategyNameId);
        }
    }

    private RoleGroupMappingStrategy getRoleGroupMappingStrategy(final IdentityProviderConfiguration identityProviderConfiguration) {

        Logger.debug(this, ()-> "Getting the role group mapping strategy");
        return identityProviderConfiguration.containsOptionalProperty(SamlName.DOTCMS_SAML_ROLE_GROUP_MAPPING_STRATEGY.getPropertyName())?
                roleGroupMappingStrategiesMap.get(
                    identityProviderConfiguration.getOptionalProperty(SamlName.DOTCMS_SAML_ROLE_GROUP_MAPPING_STRATEGY.getPropertyName()).toString()):
                DEFAULT_ROLE_GROUP_MAPPING_STRATEGY;
    }

    protected String processReplacement(final String role, final Optional<Tuple2<String, String>> roleKeySubstitutionOpt) {

        if (roleKeySubstitutionOpt.isPresent()) {

            final String replace  = roleKeySubstitutionOpt.get()._1();
            final String replacement      = roleKeySubstitutionOpt.get()._2();
            return RegEX.replace(role, replacement, replace);
        }

        return role;
    }

    private Optional<Tuple2<String, String>> getRoleKeySubstitution(final IdentityProviderConfiguration identityProviderConfiguration) {

        final String roleKeySubstitution = this.getSamlConfigurationService().getConfigAsString(
                identityProviderConfiguration, SamlName.DOT_SAML_ROLE_KEY_SUBSTITUTION);

        return getRoleKeySubstitution(roleKeySubstitution);
    }

    protected Optional<Tuple2<String, String>> getRoleKeySubstitution(final String roleKeySubstitution) {

        if (UtilMethods.isSet(roleKeySubstitution) && roleKeySubstitution.startsWith(StringPool.FORWARD_SLASH)
                && roleKeySubstitution.endsWith(StringPool.FORWARD_SLASH)) {

            final String [] substitutionTokens = roleKeySubstitution.substring(1, roleKeySubstitution.length()-1).split(StringPool.FORWARD_SLASH);
            return substitutionTokens.length == 2? Optional.ofNullable(Tuple.of(substitutionTokens[0], substitutionTokens[1])): Optional.empty();
        }

        return Optional.empty();
    }

    private void addRole(final User user, final String removeRolePrefix, final String roleObject)
            throws DotDataException {

        // remove role prefix
        final String roleKey = isSet(removeRolePrefix)?
                roleObject.replaceFirst(removeRolePrefix, StringUtils.EMPTY):
                roleObject;

        UserHelper.getInstance().addRole(user, roleKey, false, false);
    }


    private String toString(final String... rolePatterns) {
        return null == rolePatterns ? DotSamlConstants.NULL : Arrays.asList(rolePatterns).toString();
    }

    @VisibleForTesting
    protected String hashIt (final String token) throws NoSuchAlgorithmException {

        final String hashed = Encryptor.Hashing.sha256().append(token.getBytes(StandardCharsets.UTF_8)).buildUnixHash();
        return org.apache.commons.lang3.StringUtils.abbreviate(hashed, Config.getIntProperty("dotcms.user.id.maxlength", 100));
    }

    @VisibleForTesting
    protected String hashIt (final String token, final boolean doHash) throws NoSuchAlgorithmException {

        return doHash? hashIt(token): token;
    }


    protected User createNewUser(final User systemUser, final Attributes attributesBean,
                                 final IdentityProviderConfiguration identityProviderConfiguration) {
        User user = null;

        try {

            final boolean doHash      = identityProviderConfiguration.containsOptionalProperty(DO_HASH_KEY)?
                    BooleanUtils.toBoolean(identityProviderConfiguration.getOptionalProperty(DO_HASH_KEY).toString()):true;
            final String nameID       = this.samlAuthenticationService.getValue(attributesBean.getNameID());
            final String hashedNameID = this.hashIt(nameID, doHash);
            try {

                user = this.userAPI.createUser(hashedNameID, attributesBean.getEmail());
            } catch (DuplicateUserException due) {

                user = this.onDuplicateUser(attributesBean, identityProviderConfiguration, hashedNameID);
            }

            user.setFirstName(attributesBean.getFirstName());
            user.setLastName(attributesBean.getLastName());
            user.setActive(true);

            user.setCreateDate(new Date());
            user.setPassword(PublicEncryptionFactory.digestString(UUIDGenerator.generateUuid() + "/" + UUIDGenerator.generateUuid()));
            user.setPasswordEncrypted(true);

            if (Objects.nonNull(attributesBean.getAdditionalAttributes()) &&
                    attributesBean.getAdditionalAttributes().size() > 0) {

                user.setAdditionalInfo(attributesBean.getAdditionalAttributes());
            }

            this.userAPI.save(user, systemUser, false);
            Logger.debug(this, ()-> "User with NameID '" + nameID + "' and email '" +
                    attributesBean.getEmail() + "' has been created.");

        } catch (Exception e) {

            final String errorMsg = "Error creating user with NameID '" + this.samlAuthenticationService.getValue(attributesBean.getNameID()) + "': " +
                    "" + e.getMessage();
            Logger.error(this, errorMsg, e);
            throw new DotSamlException(errorMsg, e);
        }

        return user;
    }

    private User onDuplicateUser(final Attributes attributesBean,
                                 final IdentityProviderConfiguration identityProviderConfiguration,
                                 final String nameID) throws DotDataException {

        User user;
        final String companyDomain =
                this.getSamlConfigurationService().getConfigAsString(
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

            // $TIMEDATE: SAML login request for $host (frontend|backend)from
            // $REQUEST_ADDR
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
