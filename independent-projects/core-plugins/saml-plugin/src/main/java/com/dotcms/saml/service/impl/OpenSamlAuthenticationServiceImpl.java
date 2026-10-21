package com.dotcms.saml.service.impl;

import com.dotcms.saml.Attributes;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.SamlNameID;
import com.dotcms.saml.service.external.AdditionalInfoValue;
import com.dotcms.saml.service.external.AdditionalInformationType;
import com.dotcms.saml.service.external.AttributesNotFoundException;
import com.dotcms.saml.service.external.NotNullEmailAllowedException;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.handler.AssertionResolverHandler;
import com.dotcms.saml.service.handler.AssertionResolverHandlerFactory;
import com.dotcms.saml.service.handler.AuthenticationHandler;
import com.dotcms.saml.service.handler.AuthenticationResolverHandlerFactory;
import com.dotcms.saml.service.handler.LogoutHandler;
import com.dotcms.saml.service.handler.LogoutResolverHandlerFactory;
import com.dotcms.saml.service.init.Initializer;
import com.dotcms.saml.service.internal.MetaDescriptorService;
import com.dotcms.saml.service.internal.SamlCoreService;
import com.dotcms.saml.utils.EndpointHostRedirect;
import com.dotcms.saml.utils.MetaDataXMLPrinter;
import com.dotcms.saml.utils.SamlUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.util.StringPool;
import org.apache.commons.lang.StringUtils;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Open Saml implementation
 * This is the main class to interact with the IDP Assertion
 * Does the init of the SAML services
 * Validates the request to see if it is minimum ok
 * Handles the authentication to create the auth request to ask the IDP for login
 * Similar way handles the logout request
 * Parse and retrieves the attributes from the Assertion SAML: this contains the user information such as name, email, etc; plus roles (claims, authorizations, groups, etc)
 * Also renders the metadata and have some util methods to retrieve values form the attributes
 * @author jsanca
 */
public class OpenSamlAuthenticationServiceImpl implements SamlAuthenticationService {

    public static final String ADDITIONAL_INFO = "additionalInfo";
    private final LogoutResolverHandlerFactory         logoutResolverHandlerFactory;
    private final AuthenticationResolverHandlerFactory authenticationResolverHandlerFactory;
    private final AssertionResolverHandlerFactory assertionResolverHandlerFactory;
    private final SamlCoreService                 samlCoreService;
    private final SamlConfigurationService        samlConfigurationService;
    private final MessageObserver                 messageObserver;
    private final MetaDescriptorService           metaDescriptorService;
    private final MetaDataXMLPrinter              metaDataXMLPrinter;
    private final Initializer                     initializer;

    public OpenSamlAuthenticationServiceImpl(final LogoutResolverHandlerFactory logoutResolverHandlerFactory,
                                             final AuthenticationResolverHandlerFactory authenticationResolverHandlerFactory,
                                             final AssertionResolverHandlerFactory assertionResolverHandlerFactory,
                                             final SamlCoreService samlCoreService,
                                             final SamlConfigurationService samlConfigurationService,
                                             final MessageObserver messageObserver,
                                             final MetaDescriptorService metaDescriptorService,
                                             final Initializer initializer) {

        this.logoutResolverHandlerFactory         = logoutResolverHandlerFactory;
        this.authenticationResolverHandlerFactory = authenticationResolverHandlerFactory;
        this.assertionResolverHandlerFactory = assertionResolverHandlerFactory;
        this.samlCoreService          = samlCoreService;
        this.samlConfigurationService = samlConfigurationService;
        this.messageObserver          = messageObserver;
        this.metaDescriptorService    = metaDescriptorService;
        this.metaDataXMLPrinter       = new MetaDataXMLPrinter();
        this.initializer              = initializer;
    }

    /**
     * Inits the SAML Services
     * @param context
     */
    @Override
    public void initService (final Map<String, Object> context) {

        if (!this.initializer.isInitializationDone()) {

            this.messageObserver.updateInfo(this.getClass().getName(), "InitService now...");
            this.initializer.init(context);
        } else {

            this.messageObserver.updateInfo(this.getClass().getName(), "Saml Services were already started.");
        }
    }

    /**
     * Figure out if the request has a valid Assertion, usually just checks if a parameter is coming on the request
     *
     * @param request
     * @param response
     * @param identityProviderConfiguration
     * @return
     */
    @Override
    public boolean isValidSamlRequest(final HttpServletRequest  request,
                                      final HttpServletResponse response,
                                      final IdentityProviderConfiguration identityProviderConfiguration) {

        final AssertionResolverHandler assertionResolverHandler = this.assertionResolverHandlerFactory
                .getAssertionResolverForSite(identityProviderConfiguration);

        return assertionResolverHandler.isValidSamlRequest(request, response, identityProviderConfiguration);
    }

    /**
     * This method creates the authentication request based on the site configuration and the IDP metadata, sends the request to the IDP
     * to ask for the login credentials or so
     * @param request
     * @param response
     * @param identityProviderConfiguration
     * @param relayState
     */
    @Override
    public void authentication(final HttpServletRequest  request,
                               final HttpServletResponse response,
                               final IdentityProviderConfiguration identityProviderConfiguration, final String relayState) {

        // start the login on the host the IdP posts back to, so the request-binding cookie comes back with it
        try {

            if (EndpointHostRedirect.redirectIfNeeded(request, response,
                    this.getAssertionConsumerUrl(request, identityProviderConfiguration))) {

                this.messageObserver.updateDebug(this.getClass().getName(), "Login for IdP '"
                        + identityProviderConfiguration.getIdpName() + "' started on another host; redirected to the endpoint host");
                return;
            }
        } catch (IOException e) {

            throw new SamlException("Could not redirect the login to the Service Provider Endpoint Hostname", e);
        }

        final AuthenticationHandler authenticationHandler =
                this.authenticationResolverHandlerFactory.getAuthenticationHandlerForSite(identityProviderConfiguration);

        authenticationHandler.handle(request, response, identityProviderConfiguration, relayState);
    }

    /** The configured assertion consumer URL, or null when it can't be built (the handler then reports it). */
    private String getAssertionConsumerUrl(final HttpServletRequest request,
                                           final IdentityProviderConfiguration identityProviderConfiguration) {

        try {
            return this.samlCoreService.getAssertionConsumerEndpoint(request, identityProviderConfiguration);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * This method creates the logout requet based on the site configuration and the IDP metadata, sends the request to the IDP to ask for the logout
     * @param request
     * @param response
     * @param nameID
     * @param sessionIndexValue
     * @param identityProviderConfiguration
     */
    @Override
    public void logout(final HttpServletRequest  request,
                       final HttpServletResponse response,
                       final Object nameID,
                       final String sessionIndexValue,
                       final IdentityProviderConfiguration identityProviderConfiguration) {

        final LogoutHandler logoutHandler =
            this.logoutResolverHandlerFactory.getLogoutHandlerForSite(identityProviderConfiguration);

        logoutHandler.handle(request, response, nameID, sessionIndexValue, identityProviderConfiguration);
    }


    /**
     * This method is in charge of retrieve the attributes (aka user info + roles) from the IDP XML Assertion
     * @param request
     * @param response
     * @param identityProviderConfiguration
     * @return
     */
    @Override
    public Attributes resolveAttributes(final HttpServletRequest  request,
                                        final HttpServletResponse response,
                                        final IdentityProviderConfiguration identityProviderConfiguration) {
        Attributes attributes = null;

        try {

            // extracting the assertion from the request
            final Assertion assertion = this.resolveAssertion(request, response, identityProviderConfiguration);
            // extrating the attributes form the asserrion
            attributes                = this.retrieveAttributes(assertion, identityProviderConfiguration);

            this.messageObserver.updateDebug(this.getClass().getName(), "Validating user - " + attributes);

        } catch (AttributesNotFoundException e) {

            this.messageObserver.updateError(this.getClass().getName(), e.getMessage(), e);
        } catch (Exception e) {

            final String nameID = null != attributes && attributes.getNameID() instanceof SamlNameID samlNameID
                    ? SamlUtils.toNameID(samlNameID).getValue() : StringUtils.EMPTY;
            this.messageObserver.updateError(this.getClass().getName(),
                    "An error occurred when loading user with ID '" + nameID+ "'", e);
        }

        return attributes;
    }


    /**
     * This method resolve all attributes returns just key value (instead of attributes) and it is mostly from scenerios where the client
     * wants to do something custom instead the normal behavior, so they need to see all the attributes to do the custom thing
     * @param request
     * @param response
     * @param identityProviderConfiguration
     * @return
     */
    @Override
    public Map<String, String> resolveAllAttributes(final HttpServletRequest request,
                                                    final HttpServletResponse response,
                                                    final IdentityProviderConfiguration identityProviderConfiguration) {

        final Map<String, String> attributes = new HashMap<>();

        try {

            final Assertion assertion = this.resolveAssertion(request, response, identityProviderConfiguration);
            this.messageObserver.updateDebug(this.getClass().getName(),
                    "Resolving attributes - Name ID : " + assertion.getSubject().getNameID().getValue());

            attributes.put("nameID", assertion.getSubject().getNameID().getValue());

            if (null != assertion.getAttributeStatements()) {
                assertion.getAttributeStatements().forEach(attributeStatement -> {

                    this.messageObserver.updateDebug(this.getClass().getName(),
                            "Attribute Statement - local name: " + AttributeStatement.DEFAULT_ELEMENT_LOCAL_NAME + ", type: "
                                    + AttributeStatement.TYPE_LOCAL_NAME + ", number of attributes: "
                                    + attributeStatement.getAttributes().size());

                    attributeStatement.getAttributes().forEach(attribute -> {

                        this.messageObserver.updateDebug(this.getClass().getName(),
                                "Attribute - friendly name: " + attribute.getFriendlyName() + ", name: " + attribute.getName()
                                        + ", type: " + Attribute.TYPE_LOCAL_NAME + ", number of values: "
                                        + attribute.getAttributeValues().size());

                        final String value = attribute.getAttributeValues().get(0).getDOM().getFirstChild().getNodeValue();
                        attributes.put(attribute.getName(), value);
                        attributes.put(attribute.getFriendlyName(), value);

                    });
                });
            }

            attributes.put("sessionIndex", this.getSessionIndex(assertion));
        } catch (AttributesNotFoundException e) {

            this.messageObserver.updateError(this.getClass().getName(), e.getMessage(), e);
        } catch (Exception e) {

            final String nameID = null != attributes? attributes.get("nameID"): StringUtils.EMPTY ;
            this.messageObserver.updateError(this.getClass().getName(),
                    "An error occurred when loading user with ID '" + nameID+ "'", e);
        }

        return attributes;
    }

    protected Assertion resolveAssertion(final HttpServletRequest request, final HttpServletResponse response,
                                      final IdentityProviderConfiguration identityProviderConfiguration) {

        final AssertionResolverHandler assertionResolverHandler = this.assertionResolverHandlerFactory
                .getAssertionResolverForSite(identityProviderConfiguration);

        return assertionResolverHandler.resolveAssertion(request, response, identityProviderConfiguration);
    }


    /**
     * Return the value of the /AuthnStatement@SessionIndex element in an
     * assertion
     * The session index is a ref number to the session created on the IDP
     *
     * @return The value. <code>null</code>, if the assertion does not contain
     *         the element.
     */
    protected String getSessionIndex(final Assertion assertion) {
        String sessionIndex = null;

        if (assertion != null && assertion.getAuthnStatements() != null) {
            if (assertion.getAuthnStatements().size() > 0) {
                // We only look into the first AuthnStatement
                final AuthnStatement authnStatement = assertion.getAuthnStatements().get(0);
                sessionIndex = authnStatement.getSessionIndex();
            }
        }

        return sessionIndex;
    }

    /**
     * Resolve the attributes from the assertion resolved from the OpenSaml
     * artifact resolver via
     * @param assertion
     * @param identityProviderConfiguration
     * @return
     * @throws AttributesNotFoundException
     */
    protected Attributes retrieveAttributes(final Assertion assertion, final IdentityProviderConfiguration identityProviderConfiguration)
            throws AttributesNotFoundException {

        // First we retrieves the attributes name from the config (if they exist, otherwise will use the default names)
        final String emailField     = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration, SamlName.DOT_SAML_EMAIL_ATTRIBUTE);
        final String firstNameField = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration, SamlName.DOT_SAML_FIRSTNAME_ATTRIBUTE);
        final String lastNameField  = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration, SamlName.DOT_SAML_LASTNAME_ATTRIBUTE);
        final String rolesField     = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration, SamlName.DOT_SAML_ROLES_ATTRIBUTE);

        // logic for the optional additional info
        final Map<String, AdditionalInfoValue> additionalInfoMap = new HashMap<>();
        final Map<String, Object> additionalAttributes = new HashMap<>();
        if (identityProviderConfiguration.containsOptionalProperty(ADDITIONAL_INFO)) {
            final String additionalInfoValue = (String) identityProviderConfiguration.getOptionalProperty(ADDITIONAL_INFO);
            parseAdditionalInfoMap(additionalInfoMap, additionalInfoValue);
        }
        // this is configuration when some of these props is null
        final String firstNameForNullValue = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration,  SamlName.DOT_SAML_FIRSTNAME_ATTRIBUTE_NULL_VALUE);
        final String lastNameForNullValue  = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration,  SamlName.DOT_SAML_LASTNAME_ATTRIBUTE_NULL_VALUE);
        final boolean allowNullEmail       = this.samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOT_SAML_EMAIL_ATTRIBUTE_ALLOW_NULL);

        final String customConfiguration = new StringBuilder(
                SamlName.DOT_SAML_EMAIL_ATTRIBUTE.getPropertyName()).append("=").append(emailField)
                .append(",").append(SamlName.DOT_SAML_FIRSTNAME_ATTRIBUTE.getPropertyName())
                .append("=").append(firstNameField).append(",")
                .append(SamlName.DOT_SAML_LASTNAME_ATTRIBUTE.getPropertyName()).append("=")
                .append(lastNameField).append(",")
                .append(SamlName.DOT_SAML_ROLES_ATTRIBUTE.getPropertyName()).append("=")
                .append(rolesField).toString();

        final Attributes.Builder attrBuilder = new Attributes.Builder();

        this.validateAttributes(assertion, identityProviderConfiguration);

        final String nameId = assertion.getSubject().getNameID().getValue();

        this.messageObserver.updateDebug(this.getClass().getName(),
                "Resolving attributes - Name ID : " + assertion.getSubject().getNameID().getValue());

        final NameID rawNameID = assertion.getSubject().getNameID();
        attrBuilder.nameID(new SamlNameID(SamlUtils.toXMLObjectString(rawNameID), rawNameID.getValue()));

        this.messageObserver.updateDebug(this.getClass().getName(),
                "Elements of type AttributeStatement in assertion : " + assertion.getAttributeStatements().size());
        if (null != assertion.getAttributeStatements()) {
            assertion.getAttributeStatements().forEach(attributeStatement -> {

                this.messageObserver.updateDebug(this.getClass().getName(),
                        "Attribute Statement - local name: " + AttributeStatement.DEFAULT_ELEMENT_LOCAL_NAME + ", type: "
                                + AttributeStatement.TYPE_LOCAL_NAME + ", number of attributes: "
                                + attributeStatement.getAttributes().size());

                attributeStatement.getAttributes().forEach(attribute -> {

                    this.messageObserver.updateDebug(this.getClass().getName(),
                            "Attribute - friendly name: " + attribute.getFriendlyName() + ", name: " + attribute.getName()
                                    + ", type: " + Attribute.TYPE_LOCAL_NAME + ", number of values: "
                                    + attribute.getAttributeValues().size());

                    if ((attribute.getName() != null && attribute.getName().equals(emailField))
                            || (attribute.getFriendlyName() != null && attribute.getFriendlyName().equals(emailField))) {

                        this.resolveEmail(emailField, attrBuilder, attribute, nameId, allowNullEmail);
                    } else if ((attribute.getName() != null && attribute.getName().equals(lastNameField))
                            || (attribute.getFriendlyName() != null && attribute.getFriendlyName().equals(lastNameField))) {

                        this.messageObserver.updateDebug(this.getClass().getName(),
                                "Resolving attribute - LastName : " + lastNameField);

                        final String lastName = StringUtils.isNotBlank(
                                attribute.getAttributeValues().get(0).getDOM().getFirstChild().getNodeValue())
                                ? attribute.getAttributeValues().get(0).getDOM().getFirstChild().getNodeValue()
                                : checkDefaultValue(lastNameForNullValue, lastNameField + " attribute is null",
                                lastNameField + " is null and the default is null too");

                        attrBuilder.lastName(lastName);

                        this.messageObserver.updateDebug(this.getClass().getName(),
                                "Resolved attribute - lastName : " + attrBuilder.getLastName());
                    } else if ((attribute.getName() != null && attribute.getName().equals(firstNameField))
                            || (attribute.getFriendlyName() != null
                            && attribute.getFriendlyName().equals(firstNameField))) {

                        this.messageObserver.updateDebug(this.getClass().getName(),
                                "Resolving attribute - firstName : " + firstNameField);

                        final String firstName = StringUtils.isNotBlank(
                                attribute.getAttributeValues().get(0).getDOM().getFirstChild().getNodeValue())
                                ? attribute.getAttributeValues().get(0).getDOM().getFirstChild().getNodeValue()
                                : checkDefaultValue(firstNameForNullValue, firstNameField + " attribute is null",
                                firstNameField + " is null and the default is null too");

                        attrBuilder.firstName(firstName);

                        this.messageObserver.updateDebug(this.getClass().getName(),
                                "Resolved attribute - firstName : " + attrBuilder.getFirstName());
                    } else if ((attribute.getName() != null && attribute.getName().equals(rolesField))
                            || (attribute.getFriendlyName() != null && attribute.getFriendlyName().equals(rolesField))) {

                        this.messageObserver.updateDebug(this.getClass().getName(), "Resolving attribute - roles : " + rolesField);
                        attrBuilder.addRoles(true).roles(attribute);
                        this.messageObserver.updateDebug(this.getClass().getName(), "Resolving attributes - roles : " + attribute);
                    } else {

                        // in case the attribute do not match with any of the legacy props, lets try the additional information
                        if (additionalInfoMap.containsKey(attribute.getName()) || additionalInfoMap.containsKey(attribute.getFriendlyName())) {

                            final String key = Objects.isNull(attribute.getName())? attribute.getFriendlyName() : attribute.getName();
                            final AdditionalInfoValue additionalInfoValue = additionalInfoMap.get(key);

                            additionalAttributes.put(additionalInfoValue.getAliasKey(), parseValue (attribute, additionalInfoValue.getType()));
                        } else {
                            final String attributeName = attribute.getName();
                            this.messageObserver.updateDebug(this.getClass().getName(),
                                    attributeName + " attribute did not match any user property in the idpConfig: " + customConfiguration);
                        }
                    }
                });
            });
        }

        attrBuilder.sessionIndex(this.getSessionIndex(assertion));

        this.messageObserver.updateInfo(this.getClass().getName(), "additionalAttributes: " + additionalAttributes);

        if (additionalAttributes.size() > 0) {
            attrBuilder.additionalAttributes(additionalAttributes);
        }

        Attributes attributes = attrBuilder.build();
        this.messageObserver.updateDebug(this.getClass().getName(), "-> Value of attributesBean = " + attributes.toString());
        attributes = this.doubleCheckAttributes(attributes, firstNameField, firstNameForNullValue, lastNameField,
                lastNameForNullValue, allowNullEmail);
        this.messageObserver.updateDebug(this.getClass().getName(), "-> Double Checked attributes = " + attributes);

        return attributes;
    }

    /**
     * It parse something such as
     * additionalInfo=prop1|single,prop2|collection,prop3|json|alias-for-json
     *
     * where prop1 is the attribute name and single means the type (single value in this case)
     * in the same sense collection means an attributes collection and results as a List and Json is a single value but parsed as a JSON.
     * Finally the third one on the prop3 (alias-for-json) is the key you want to use to replace whatever is on the assertion, you can replace for instance
     * big azure namespa
     *
     * keep in mind that this sentence is equivalent to the previous one
     * additionalInfo=prop1,prop2|collection,prop3|json|alias-for-json
     *
     * here prop1 is not being qualified under any type, so the default will be single.
     *
     * @param additionalInfoMap
     * @param additionalInfoValue
     */
    protected void parseAdditionalInfoMap(final Map<String, AdditionalInfoValue> additionalInfoMap,
                                          final String additionalInfoValue) {

        final String [] additionalInfoConfigTokens = additionalInfoValue.split(StringPool.COMMA);
        for (String additionalInfoConfigToken : additionalInfoConfigTokens) {

            final String [] parsedAdditionalInfoConfigToken = additionalInfoConfigToken.split("\\"+StringPool.PIPE);
            if (parsedAdditionalInfoConfigToken.length > 0) {

                final AdditionalInformationType additionInformationType = (parsedAdditionalInfoConfigToken.length >= 2 &&
                        Objects.nonNull(parsedAdditionalInfoConfigToken[1])) ?
                        AdditionalInformationType.valueOf(parsedAdditionalInfoConfigToken[1].toUpperCase()) :
                        AdditionalInformationType.SINGLE; // default if not qualified

                final String key = (parsedAdditionalInfoConfigToken.length >= 3 &&
                        Objects.nonNull(parsedAdditionalInfoConfigToken[2])) ?
                        parsedAdditionalInfoConfigToken[2]:
                        parsedAdditionalInfoConfigToken[0];

                additionalInfoMap.put(parsedAdditionalInfoConfigToken[0], new AdditionalInfoValue(key, additionInformationType));
             }
        }
    } //parseAdditionalInfoMap

    /**
     * Parse the attribute based on the previous given type
     * @param attribute
     * @param additionInformationType
     * @return
     */
    private Object parseValue (final Attribute attribute, final AdditionalInformationType additionInformationType) {

        switch (additionInformationType) {

            case JSON:
                return new JSONObject(attribute.getAttributeValues().get(0).getDOM().getFirstChild().getNodeValue());
            case COLLECTION:
                return getValues(attribute);
            default:
                return attribute.getAttributeValues().get(0).getDOM().getFirstChild().getNodeValue();
        }
    }

    private Attributes doubleCheckAttributes(final Attributes originalAttributes, final String firstNameField,
                                                 final String firstNameForNullValue, final String lastNameField, final String lastNameForNullValue,
                                                 final boolean allowNullEmail) {
        return (this.anyAttributeNullOrBlank(originalAttributes)) ? this.checkDefaultValues(originalAttributes,
                firstNameField, firstNameForNullValue, lastNameField, lastNameForNullValue, allowNullEmail)
                : originalAttributes;
    }

    private Attributes checkDefaultValues(final Attributes originalAttributes, final String firstNameField,
                                              final String firstNameForNullValue, final String lastNameField, final String lastNameForNullValue,
                                              final boolean allowNullEmail) {
        final Attributes.Builder attrBuilder = new Attributes.Builder();

        attrBuilder.nameID(originalAttributes.getNameID());
        attrBuilder.roles(originalAttributes.getRoles());
        attrBuilder.addRoles(originalAttributes.isAddRoles());
        attrBuilder.sessionIndex(originalAttributes.getSessionIndex());

        if (StringUtils.isBlank((originalAttributes.getEmail()))) {

            final NameID nameID = SamlUtils.toNameID((SamlNameID) originalAttributes.getNameID());
            attrBuilder.email(this.createNoReplyEmail(nameID.getValue(), allowNullEmail));
        } else {

            attrBuilder.email(originalAttributes.getEmail());
        }

        if (StringUtils.isBlank(originalAttributes.getFirstName())) {

            attrBuilder.firstName(checkDefaultValue(firstNameForNullValue, firstNameField + " attribute is null",
                    firstNameField + " is null and the default is null too"));
        } else {

            attrBuilder.firstName(originalAttributes.getFirstName());
        }

        if (StringUtils.isBlank(originalAttributes.getLastName())) {

            attrBuilder.lastName(checkDefaultValue(lastNameForNullValue, lastNameField + " attribute is null",
                    lastNameField + " is null and the default is null too"));
        } else {

            attrBuilder.lastName(originalAttributes.getLastName());
        }

        return attrBuilder.build();
    }

    private boolean anyAttributeNullOrBlank(final Attributes originalAttributes) {
        return StringUtils.isBlank(originalAttributes.getEmail())
                || StringUtils.isBlank(originalAttributes.getFirstName())
                || StringUtils.isBlank(originalAttributes.getLastName());
    }

    protected String checkDefaultValue(final String lastNameForNullValue,
                                       final String logMessage,
                                       final String exceptionMessage) {

        if (StringUtils.isBlank(lastNameForNullValue)) {

            throw new DotRuntimeException(exceptionMessage);
        }

        this.messageObserver.updateInfo(this.getClass().getName(), logMessage);

        return lastNameForNullValue;
    }

    protected void resolveEmail(final String emailField, final Attributes.Builder attributesBuilder,
                              final Attribute attribute, final String nameId, final boolean allowNullEmail) {

        this.messageObserver.updateDebug(this.getClass().getName(), "Resolving attribute - Email : " + emailField);

        String emailValue = attribute.getAttributeValues().get(0).getDOM().getFirstChild().getNodeValue();

        emailValue = StringUtils.isBlank(emailValue)? createNoReplyEmail(nameId, allowNullEmail) : emailValue;

        attributesBuilder.email(emailValue);

        this.messageObserver.updateDebug(this.getClass().getName(),  "Resolved attribute - Email : " + attributesBuilder.getEmail());
    }

    /**
     * This method allows to create such as a default generic email
     * The email is based in the nameId @no-reply.dotcms.com, todo: this may be good to have a configuration point to use a custom email mask
     * @param nameId
     * @param allowNullEmail
     * @return
     */
    protected String createNoReplyEmail(final String nameId, final boolean allowNullEmail) {

        if (!allowNullEmail) {

            throw new NotNullEmailAllowedException("Email attribute is null, which is not allowed");
        }

        this.messageObserver.updateInfo(this.getClass().getName(),
                "UserID '" + nameId + "' has a null email attribute. Generating one...");

        final String emailValue = new StringBuilder(NO_REPLY).append(sanitizeNameId(nameId)).append(NO_REPLY_DOTCMS_COM)
                .toString();

        this.messageObserver.updateDebug(this.getClass().getName(),
                "UserID '" + nameId + "' has been assigned email '" + emailValue + "'");

        return emailValue;
    }

    private String sanitizeNameId(final String nameId) {

        return StringUtils.replace(nameId, AT_SYMBOL, AT_);
    }

    /**
     * We make a validation based on, if the SAML will allows empty attributes, check if the subject (name id) is present
     * @param assertion
     * @param identityProviderConfiguration
     * @throws AttributesNotFoundException
     */
    protected void validateAttributes(final Assertion assertion, final IdentityProviderConfiguration identityProviderConfiguration) throws AttributesNotFoundException {

        if (null == assertion) {

            throw new DotRuntimeException("SAML Assertion is null");
        }

        final boolean allowEmptyAttrs = identityProviderConfiguration.containsOptionalProperty("saml.allow.empty.attrs")?
                Boolean.parseBoolean(identityProviderConfiguration.getOptionalProperty("saml.allow.empty.attrs").toString()): true;
        this.samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOT_SAML_EMAIL_ATTRIBUTE);
        if (null == assertion.getAttributeStatements() || assertion.getAttributeStatements().isEmpty()) {

            if (allowEmptyAttrs) {

                Logger.info(this.getClass().getName(), "SAML: Attributes are empty");
            } else {
                throw new DotRuntimeException("Attribute list in SAML Assertion is null or empty");
            }
        }

        if (null == assertion.getSubject()) {

            throw new DotRuntimeException("Subject in SAML Assertion is null");
        }

        if (null == assertion.getSubject().getNameID() || assertion.getSubject().getNameID().getValue().isEmpty()) {

            throw new DotRuntimeException("NameID in SAML Assertion is null or empty");
        }
    }

    /**
     * This is the method to allows dotCMS to create SP (aka dotCMS) metadata
     * @param writer
     * @param identityProviderConfiguration
     */
    @Override
    public void renderMetadataXML(final Writer writer,
                           final IdentityProviderConfiguration identityProviderConfiguration) {

        try {

            // First, get the Entity descriptor.
            final EntityDescriptor descriptor = this.metaDescriptorService.
                    getServiceProviderEntityDescriptor(identityProviderConfiguration);

            this.messageObserver.updateDebug(this.getClass().getName(), "Printing Metadata Descriptor:");
            this.messageObserver.updateDebug(this.getClass().getName(), "\n\n" + descriptor);
            // get ready to convert it to XML.
            this.metaDataXMLPrinter.print(descriptor, writer);

            this.messageObserver.updateDebug(this.getClass().getName(),"Metadata Descriptor printed.");
        } catch (Exception e) {

            this.messageObserver.updateError(this.getClass().getName(), e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * This is a helper method to retrieve a value from an object
     * Could retrieve NameID as a String, XMLObject as a String (first child) or just an string
     * @param samlObject
     * @return
     */
    @Override
    public String getValue(final Object samlObject) {

        if (samlObject instanceof SamlNameID samlNameID) {
            return samlNameID.getValue();
        } else if (samlObject instanceof NameID nameID) {
            return nameID.getValue();
        } else if (samlObject instanceof XMLObject xmlObject) {
            return xmlObject.getDOM().getFirstChild().getNodeValue();
        }

        return null != samlObject? samlObject.toString(): null;
    }

    /**
     * Retrieves values from nodes that are collections
     * @param samlObject
     * @return
     */
    @Override
    public List<String> getValues(final Object samlObject) {

        List<String> values = null;

        if (samlObject instanceof Attribute) {

            values = new ArrayList<>();
            for (final XMLObject childSamlObject : Attribute.class.cast(samlObject).getAttributeValues()) {

                values.add(this.getValue(childSamlObject));
            }
        }

        return values;
    }
}
