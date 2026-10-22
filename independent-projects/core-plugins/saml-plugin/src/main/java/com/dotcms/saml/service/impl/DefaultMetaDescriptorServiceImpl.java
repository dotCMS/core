package com.dotcms.saml.service.impl;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.service.internal.CredentialService;
import com.dotcms.saml.service.internal.EndpointService;
import com.dotcms.saml.service.internal.MetaDescriptorService;
import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.internal.SamlCoreService;
import com.dotcms.saml.service.external.SamlConstants;
import org.apache.commons.io.IOUtils;
import com.dotcms.saml.service.external.MetaData;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.EncryptionMethod;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.impl.EncryptionMethodBuilder;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xml.util.Base64;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

/**
 * Idp Meta Descriptor service default implementation.
 * - Parse the Identity provider metadata
 * - Creates the SP (aka dotCMS) metadata
 * @author jsanca
 */

public class DefaultMetaDescriptorServiceImpl implements MetaDescriptorService {
	private static final long serialVersionUID = 7259833793217618045L;

	private final ParserPool               parserPool;
	private final UnmarshallerFactory      unmarshallerFactory;
	private final XMLObjectBuilderFactory  xmlObjectBuilderFactory;
	private final SamlConfigurationService samlConfigurationService;
	private final MessageObserver messageObserver;
	private final SamlCoreService samlService;
	private final CredentialService credentialService;
	private final EndpointService endpointService;

	public DefaultMetaDescriptorServiceImpl(final SamlConfigurationService samlConfigurationService,
											final MessageObserver messageObserver,
											final SamlCoreService samlService,
											final CredentialService credentialService,
											final EndpointService endpointService) {

		this.parserPool               = XMLObjectProviderRegistrySupport.getParserPool();
		this.unmarshallerFactory      = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
		this.xmlObjectBuilderFactory  = XMLObjectProviderRegistrySupport.getBuilderFactory();

		this.samlConfigurationService = samlConfigurationService;
		this.messageObserver          = messageObserver;
		this.samlService              = samlService;
		this.credentialService        = credentialService;
		this.endpointService          = endpointService;
	}

	/**
	 * By default the pivot to get the {@link IDPSSODescriptor} by using
	 * {@link SamlConstants#DOT_SAML_IDP_METADATA_PROTOCOL_DEFAULT_VALUE}
	 * However if there is any different protocol you want to use, please change
	 * the {@link SamlConstants#DOT_SAML_IDP_METADATA_PROTOCOL} to override
	 * the value on the configuration
	 * 
	 * @param inputStream {@link InputStream} this is the stream of the Idp-metadata.xml
	 *            (get it from the IdP, for instance the idp-metadata.xml from
	 *            the shibboleth installation.
	 * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
	 * @return MetadataBean
	 * @throws Exception
	 */
	@Override
	public MetaData parse(final InputStream inputStream,
                          final IdentityProviderConfiguration identityProviderConfiguration) {

		final EntityDescriptor descriptor    = this.unmarshall(inputStream);
		final String protocol = this.samlConfigurationService.getConfigAsString(identityProviderConfiguration,
				SamlName.DOT_SAML_IDP_METADATA_PROTOCOL);
		final IDPSSODescriptor idpDescriptor = descriptor.getIDPSSODescriptor(protocol);

		this.messageObserver.updateInfo(this.getClass().getName(), "Parsing metadata from IdP with entityID: " + descriptor.getEntityID());

		return new MetaData(descriptor.getEntityID(), idpDescriptor.getErrorURL(),
				this.getSingleSignOnMap(idpDescriptor), this.getSingleLogoutMap(idpDescriptor),
				this.getCredentialSigningList(descriptor.getEntityID(), idpDescriptor));
	}

	/**
	 * Retrieves the logout endpoints from the IDP metadata descriptor
	 * @param idpDescriptor IDPSSODescriptor
	 * @return Map - binding - location
	 */
	protected Map<String, String> getSingleLogoutMap(final IDPSSODescriptor idpDescriptor) {

		final Map<String, String> singleLogoutBindingLocationMap = new LinkedHashMap<>();

		idpDescriptor.getSingleLogoutServices().stream().forEach(sso -> {

			this.messageObserver.updateInfo(this.getClass().getName(), "Add SLO binding " + sso.getBinding() + " (" + sso.getLocation() + ")");
			singleLogoutBindingLocationMap.put(sso.getBinding(), sso.getLocation());
		});

		return singleLogoutBindingLocationMap;
	}

	/**
	 * This creates from the runtime configuration, the {@link EntityDescriptor}
	 * for dotCMS Service Provider. Things to keep in mind:
	 * 1) By default WantAssertionsSigned is true, if you want to overrides it please use
	 * {@link SamlConstants#DOTCMS_SAML_WANT_ASSERTIONS_SIGNED} on the
	 * configuration
	 * 2) By default AuthnRequestsSigned is true, if you want to overrides it please use
	 * {@link SamlConstants#DOTCMS_SAML_AUTHN_REQUESTS_SIGNED} on the configuration.
	 * 3) All Assertion Consumer Services use the url returned by:
	 * {@link SamlConstants#DOT_SAML_ASSERTION_CUSTOMER_ENDPOINT_URL}, this
	 * value usually will be the dotCMS landing page.
	 * 4) By default as a formats we return: {@link NameIDType#TRANSIENT}, {@link NameIDType#PERSISTENT}
	 * however if you want to override it change the {@link SamlConstants#DOTCMS_SAML_NAME_ID_FORMATS} on the
	 * configuration (comma separated)
	 * 
	 * @param identityProviderConfiguration
	 *            {@link IdentityProviderConfiguration}
	 * @return EntityDescriptor
	 */
	@SuppressWarnings("unchecked")
	@Override
	public EntityDescriptor getServiceProviderEntityDescriptor(final IdentityProviderConfiguration identityProviderConfiguration) {

		final SAMLObjectBuilder<EntityDescriptor> entityDescriptorBuilder = (SAMLObjectBuilder<EntityDescriptor>) this.xmlObjectBuilderFactory
				.getBuilder(EntityDescriptor.DEFAULT_ELEMENT_NAME);
		final SAMLObjectBuilder<SPSSODescriptor> spssoDescriptorBuilder = (SAMLObjectBuilder<SPSSODescriptor>) this.xmlObjectBuilderFactory
				.getBuilder(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
		final SAMLObjectBuilder<AssertionConsumerService> assertionConsumerServiceBuilder = (SAMLObjectBuilder<AssertionConsumerService>) this.xmlObjectBuilderFactory
				.getBuilder(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
		final SAMLObjectBuilder<SingleLogoutService> singleLogoutServiceBuilder = (SAMLObjectBuilder<SingleLogoutService>) this.xmlObjectBuilderFactory
				.getBuilder(SingleLogoutService.DEFAULT_ELEMENT_NAME);

		final EntityDescriptor descriptor     = entityDescriptorBuilder.buildObject();
		final SPSSODescriptor spssoDescriptor = spssoDescriptorBuilder.buildObject();

		descriptor.setEntityID(this.samlService.getSPIssuerValue(identityProviderConfiguration));

		this.messageObserver.updateInfo(this.getClass().getName(),
				"Creating the MetaData for the site: " + identityProviderConfiguration.getSpEndpointHostname());
		this.messageObserver.updateDebug(this.getClass().getName(),
				"Generating the Entity Provider Descriptor for: " + descriptor.getEntityID());

		spssoDescriptor.setWantAssertionsSigned(
				this.credentialService.isVerifyAssertionSignatureNeeded(identityProviderConfiguration));
		spssoDescriptor.setAuthnRequestsSigned(
				this.credentialService.isVerifyResponseSignatureNeeded(identityProviderConfiguration));
		spssoDescriptor.addSupportedProtocol(SAMLConstants.SAML20_NS);

		this.messageObserver.updateDebug(this.getClass().getName(),
				"Setting the key descriptors for: " + descriptor.getEntityID());

		// set's the SIGNING and ENCRYPTION keyinfo.
		this.setKeyDescriptors(spssoDescriptor, identityProviderConfiguration);

		// set's the messages format.
		this.setFormat(identityProviderConfiguration, spssoDescriptor);

		// set's the assertion customer services, this will be a fixed url on
		// dotCMS.
		/*
		 * spssoDescriptor.getAssertionConsumerServices().add
		 * (this.createAssertionConsumerService(0,
		 * SAMLConstants.SAML2_ARTIFACT_BINDING_URI,
		 * configuration.getAssertionConsumerEndpoint(),
		 * assertionConsumerServiceBuilder));
		 */

		spssoDescriptor.getAssertionConsumerServices()
				.add(this.createAssertionConsumerService(0, SAMLConstants.SAML2_POST_BINDING_URI,
						this.endpointService.getAssertionConsumerEndpoint(identityProviderConfiguration), assertionConsumerServiceBuilder));

		spssoDescriptor.getSingleLogoutServices()
				.add(this.createSingleLogoutService(SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI,
						this.endpointService.getSingleLogoutEndpoint(identityProviderConfiguration), singleLogoutServiceBuilder));

		/*
		 * spssoDescriptor.getAssertionConsumerServices().add
		 * (this.createAssertionConsumerService(2,
		 * SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI,
		 * configuration.getAssertionConsumerEndpoint(),
		 * assertionConsumerServiceBuilder));
		 */

		spssoDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
		descriptor.getRoleDescriptors().add(spssoDescriptor);

		this.messageObserver.updateDebug(this.getClass().getName(), "Getting SP Entity Descriptor: DONE!");

		return descriptor;
	}

	/**
	 * Template method to create an assertion consumer service
	 * 
	 * @param index
	 *            {@link Integer}
	 * @param binding
	 *            {@link String}
	 * @param location
	 *            {@link String}
	 * @param assertionConsumerServiceBuilder
	 *            {@link SAMLObjectBuilder}
	 * @return AssertionConsumerService
	 */
	protected AssertionConsumerService createAssertionConsumerService(final int index, final String binding,
                                                                      final String location, final SAMLObjectBuilder<AssertionConsumerService> assertionConsumerServiceBuilder) {

		this.messageObserver.updateDebug(this.getClass().getName(), "Assertion consumer service Location: " + location);

		final AssertionConsumerService assertionConsumerServiceArtifact = assertionConsumerServiceBuilder.buildObject();
		assertionConsumerServiceArtifact.setIndex(index);
		assertionConsumerServiceArtifact.setBinding(binding);
		assertionConsumerServiceArtifact.setLocation(location);

		return assertionConsumerServiceArtifact;
	}

	/**
	 * Template method to create single logout service
	 * 
	 * @param binding
	 *            {@link String}
	 * @param location
	 *            {@link String}
	 * @param singleLogoutServiceBuilder
	 *            {@link SAMLObjectBuilder}
	 * @return SingleLogoutService
	 */
	protected SingleLogoutService createSingleLogoutService(final String binding, final String location,
                                                            final SAMLObjectBuilder<SingleLogoutService> singleLogoutServiceBuilder) {

		this.messageObserver.updateDebug(this.getClass().getName(), "Assertion consumer service. Location: " + location);

		final SingleLogoutService assertionConsumerService = singleLogoutServiceBuilder.buildObject();

		assertionConsumerService.setBinding(binding);
		assertionConsumerService.setLocation(location);

		return assertionConsumerService;
	}

	/**
	 * Sets the format's supported for the messages.
	 *
	 * @param identityProviderConfiguration
	 *            {@link IdentityProviderConfiguration}
	 * @param spssoDescriptor
	 *            {@link SPSSODescriptor}
	 */
	@SuppressWarnings("unchecked")
	protected void setFormat(final IdentityProviderConfiguration identityProviderConfiguration,
							 final SPSSODescriptor spssoDescriptor) {

		final SAMLObjectBuilder<NameIDFormat> nameIDFormatBuilder = (SAMLObjectBuilder<NameIDFormat>) this.xmlObjectBuilderFactory
				.getBuilder(NameIDFormat.DEFAULT_ELEMENT_NAME);

		final String[] formats = this.samlConfigurationService.getConfigAsArrayString(identityProviderConfiguration,
				SamlName.DOTCMS_SAML_NAME_ID_POLICY_FORMAT);

		for (final String format : formats) {

			spssoDescriptor.getNameIDFormats().add(this.createFormat(format, nameIDFormatBuilder));
		}
	}

	protected NameIDFormat createFormat(final String format,
                                        final SAMLObjectBuilder<NameIDFormat> nameIDFormatBuilder) {

		final NameIDFormat nameIDFormat = nameIDFormatBuilder.buildObject();
		nameIDFormat.setFormat(format);
		return nameIDFormat;
	}

	/**
	 * This method is created in this way, just in case some subclass would like
	 * to override it.
	 *
	 * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
	 * @return Credential
	 */
	protected Credential getCredential(final IdentityProviderConfiguration identityProviderConfiguration) {

		return this.samlService.getCredential(identityProviderConfiguration);
	}

	/**
	 * Set the Key Descriptors, SIGNING and ENCRYPTION keyInfo (if any).
	 * 
	 * @param spssoDescriptor {@link SPSSODescriptor}
	 * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
	 */
	@SuppressWarnings("unchecked")
	protected void setKeyDescriptors(final SPSSODescriptor spssoDescriptor, final IdentityProviderConfiguration identityProviderConfiguration) {

		final boolean isEncryptedDescriptor = this.samlConfigurationService.getConfigAsBoolean(
				identityProviderConfiguration, SamlName.DOTCMS_SAML_USE_ENCRYPTED_DESCRIPTOR);
		final SAMLObjectBuilder<KeyDescriptor> keyDescriptorBuilder = (SAMLObjectBuilder<KeyDescriptor>) this.xmlObjectBuilderFactory
				.getBuilder(KeyDescriptor.DEFAULT_ELEMENT_NAME);
		final Credential credential = this.getCredential(identityProviderConfiguration);
		final EncryptionMethodBuilder encryptionMethodBuilder = new EncryptionMethodBuilder();
		final EncryptionMethod encryptionMethod;
		final KeyDescriptor    signKeyDescriptor;
		KeyDescriptor encryptedKeyDescriptor = null;

		try {

			signKeyDescriptor = keyDescriptorBuilder.buildObject();
			signKeyDescriptor.setUse(UsageType.SIGNING);

			if (isEncryptedDescriptor) {

				encryptedKeyDescriptor = keyDescriptorBuilder.buildObject();
				encryptedKeyDescriptor.setUse(UsageType.ENCRYPTION);
			}

			try {

				signKeyDescriptor.setKeyInfo(getKeyInfo(credential));
				encryptionMethod = encryptionMethodBuilder.buildObject();
				encryptionMethod.setAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
				signKeyDescriptor.getEncryptionMethods().add(encryptionMethod);
				spssoDescriptor.getKeyDescriptors().add(signKeyDescriptor);

				if (isEncryptedDescriptor) {

					encryptedKeyDescriptor.setKeyInfo(getKeyInfo(credential));
					spssoDescriptor.getKeyDescriptors().add(encryptedKeyDescriptor);
				}
			} catch (final Exception e) {
				final String errorMsg = String.format("An error occurred when generating credentials for IdP Config " +
						"'%s' [%s]: %s", identityProviderConfiguration.getIdpName(), identityProviderConfiguration.getId(), e.getMessage());
				this.messageObserver.updateError(this.getClass().getName(), errorMsg, e);
				throw new SamlException(errorMsg, e);
			}
		} catch (final SamlException e) {

			this.messageObserver.updateWarning(this.getClass().getName(), "An error occurred when setting Key Descriptors. Continue...");
			throw e;
		} catch (final Exception e) {

			final String errorMsg = String.format("An error occurred when retrieving credentials for IdP Config " +
					"'%s' [%s]: %s", identityProviderConfiguration.getIdpName(), identityProviderConfiguration.getId(), e.getMessage());
			this.messageObserver.updateError(this.getClass().getName(), errorMsg, e);
			throw new SamlException(errorMsg, e);
		}
	}

	/**
	 * Using the {@link Credential} gets the KeyInfo.
	 * 
	 * @param credential
	 *            {@link Credential}
	 * @return KeyInfo
	 * @throws Exception
	 */
	protected KeyInfo getKeyInfo(final Credential credential) throws Exception {
		final X509KeyInfoGeneratorFactory keyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();

		keyInfoGeneratorFactory.setEmitEntityCertificate(true);
		final KeyInfoGenerator keyInfoGenerator = keyInfoGeneratorFactory.newInstance();

		this.messageObserver.updateDebug(this.getClass().getName(), "Getting Key Info from Metadata Credential: " + credential);

		return keyInfoGenerator.generate(credential);
	}

	/**
	 * Gets from the idp metada the list of signing credential's
	 * 
	 * @param entityId
	 *            {@link String}
	 * @param idpDescriptor
	 *            {@link IDPSSODescriptor}
	 * @return
	 */
	protected List<Credential> getCredentialSigningList(final String entityId, final IDPSSODescriptor idpDescriptor) {

		return idpDescriptor.getKeyDescriptors().stream()
				.filter(key -> null != key.getKeyInfo()
						&& key.getKeyInfo().getX509Datas().get(0).getX509Certificates().size() > 0
						&& UsageType.SIGNING == key.getUse()) // not signing are relevant by now.
				.map(key -> convertToCredential(entityId,
						key.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0)))
				.collect(Collectors.toList());
	}

	/**
	 * Convert the x509Certificate
	 * {@link org.opensaml.xmlsec.signature.X509Certificate} to
	 * {@link Credential}
	 * 
	 * @param entityId
	 *            {@link String}
	 * @param x509Certificate
	 *            {@link org.opensaml.xmlsec.signature.X509Certificate}
	 * @return Credential
	 */
	protected Credential convertToCredential(final String entityId,
                                             final org.opensaml.xmlsec.signature.X509Certificate x509Certificate) {
		final byte[] decoded;
		final CertificateFactory certificateFactory;
		final java.security.cert.X509Certificate javaX509Certificate;
		ByteArrayInputStream byteArrayInputStream = null;
		Credential           credential           = null;

		try {

			decoded              = Base64.decode(x509Certificate.getValue());
			certificateFactory   = CertificateFactory.getInstance(X_509);
			byteArrayInputStream = new ByteArrayInputStream(decoded);
			javaX509Certificate  = java.security.cert.X509Certificate.class.cast(
					certificateFactory.generateCertificate(byteArrayInputStream));

			javaX509Certificate.checkValidity();

			final BasicX509Credential signing = new BasicX509Credential(javaX509Certificate);
			signing.setEntityId(entityId);
			credential = signing;
		} catch (CertificateException e) {

			this.messageObserver.updateError(this.getClass().getName(), e.getMessage(), e);
			credential = null;
		} finally {

			IOUtils.closeQuietly(byteArrayInputStream);
		}

		return credential;
	}

	/**
	 * Generates a map of binding type -> location
	 * 
	 * @param idpDescriptor
	 *            IDPSSODescriptor
	 * @return Map
	 */
	protected Map<String, String> getSingleSignOnMap(final IDPSSODescriptor idpDescriptor) {

		final Map<String, String> singleSignOnBindingLocationMap = new LinkedHashMap<>();

		idpDescriptor.getSingleSignOnServices().stream().forEach(sso -> {

			this.messageObserver.updateDebug(this.getClass().getName(),
					"Add SSO binding " + sso.getBinding() + " (" + sso.getLocation() + ")");
			singleSignOnBindingLocationMap.put(sso.getBinding(), sso.getLocation());
		});

		return singleSignOnBindingLocationMap;
	}

	protected EntityDescriptor unmarshall(final InputStream inputStream)  {

		EntityDescriptor descriptor;
		
		// load the unmarshallers - this fixed an NPE, perhaps because of the QNAME import
		final Map<QName,Unmarshaller> unmarshallers = this.unmarshallerFactory.getUnmarshallers();
		
		try {
			// Parse metadata file
			final Element metadata = this.parserPool.parse(inputStream).getDocumentElement();
			this.messageObserver.updateInfo(DefaultMetaDescriptorServiceImpl.class.getName(),
					"The metadata element from the IdP Metadata " + (
							(metadata == null) ? "is empty!" : "has a value!"));
			
			
			
			// Get appropriate unmarshaller
			final Unmarshaller unmarshaller = this.unmarshallerFactory.getUnmarshaller(metadata);
			this.messageObserver.updateInfo(DefaultMetaDescriptorServiceImpl.class.getName(),
					"The unmarshaller from the metadata element " + (
							(unmarshaller == null) ? "is null!" : "has a value!"));
			// Unmarshall using the document root element, an EntitiesDescriptor in this case
			descriptor = EntityDescriptor.class.cast(unmarshaller.unmarshall(metadata));
		} catch (final Exception e) {

			final String errorMsg = "An error occurred when parsing the IdP Metadata file: " + e.getMessage();
			this.messageObserver.updateError(this.getClass().getName(), errorMsg, e);
			throw new SamlException(errorMsg, e);
		}

		return descriptor;
	}

}
