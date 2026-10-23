package com.dotcms.saml.utils;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.MessageObserver;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang.StringUtils;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.credential.impl.AbstractCriteriaFilteringCredentialResolver;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xml.util.Base64;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;

/**
 * <strong>NOTE:</strong> this class is a non-standard, custom implementation of
 * the proposed FilesystemCredentialResolver A
 * {@link org.opensaml.security.credential.CredentialResolver} that pulls
 * credential information from the file system.
 * 
 * This credential resolver attempts to retrieve credential information from the
 * file system or dotSAML cache. Specifically it will attempt to find the
 * IdpConfig data file and use it to populate BasicX509Credentials including the
 * entityID and public and private key data.
 * 
 */
public class IdpConfigCredentialResolver extends AbstractCriteriaFilteringCredentialResolver {

	private final IdentityProviderConfigurationFactory identityProviderConfigurationFactory;
	private final MessageObserver messageObserver;

	public IdpConfigCredentialResolver(final IdentityProviderConfigurationFactory identityProviderConfigurationFactory,
									   final MessageObserver messageObserver) {
		this.identityProviderConfigurationFactory = identityProviderConfigurationFactory;
		this.messageObserver = messageObserver;
	}

	/** {@inheritDoc} */
	protected Iterable<Credential> resolveFromSource(final CriteriaSet criteriaSet) throws ResolverException {

		IdentityProviderConfiguration identityProviderConfiguration = null;
		try {

			this.checkCriteriaRequirements(criteriaSet);
			final String entityID = criteriaSet.get(EntityIdCriterion.class).getEntityId();
			identityProviderConfiguration = getIdpConfig(entityID);

			final X509Certificate cert = getPublicCert(identityProviderConfiguration.getPublicCert());
			final PrivateKey privateKey = getPrivateKey(identityProviderConfiguration.getPrivateKey());

			final BasicX509Credential credential = new BasicX509Credential(cert, privateKey);
			credential.setEntityId(identityProviderConfiguration.getId());
			credential.setUsageType(UsageType.UNSPECIFIED);

			final ArrayList<X509Certificate> certChain = new ArrayList<>();
			certChain.add(cert);
			credential.setEntityCertificateChain(certChain);

			return Collections.singleton(credential);
		} finally {
			if (null != identityProviderConfiguration) {
				identityProviderConfiguration.destroy();
			}
		}
	}

	/**
	 * Check that required credential criteria are available.
	 * 
	 * @param criteriaSet
	 *            the credential criteria set to evaluate
	 */
	protected void checkCriteriaRequirements(final CriteriaSet criteriaSet) {

		if (criteriaSet == null || criteriaSet.get(EntityIdCriterion.class) == null) {

			this.messageObserver.updateError(this.getClass().getName(),
					"EntityIDCriterion was not specified in the criteria set, resolution cannot be attempted");
			throw new IllegalArgumentException("No EntityIDCriterion was available in criteria set");
		}
	}

	protected IdentityProviderConfiguration getIdpConfig(final String id) throws ResolverException {

		IdentityProviderConfiguration identityProviderConfiguration = null;

		try {

			identityProviderConfiguration = identityProviderConfigurationFactory.findIdentityProviderConfigurationById(id);
		} catch (final Exception e) {

			this.messageObserver.updateError(this.getClass().getName(), "Exception while reading IdpConfig data for ID: {0}", id);
			throw new ResolverException("Exception while reading IdpConfig data", e);

		}

		if (identityProviderConfiguration == null) {

			this.messageObserver.updateError(this.getClass().getName(), "Unable to located IdpConfig file with ID: {0}", id);
			throw new ResolverException("Unable to located IdpConfig file with ID:");
		}

		return identityProviderConfiguration;
	}

	public X509Certificate getPublicCert(final char[] certFile) throws ResolverException {

		X509Certificate cert = null;

		if (certFile == null || certFile.length == 0) {
			this.messageObserver.updateError(this.getClass().getName(), "Public Key cannot be null!");
			throw new ResolverException("Public Key file cannot be null!");
		}

		try (InputStream inputStream = new ReaderInputStream(new CharArrayReader(certFile), StandardCharsets.UTF_8)) {

			final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) certificateFactory.generateCertificate(inputStream);
		} catch (IOException e) {

			this.messageObserver.updateError(this.getClass().getName(), "Unable to read Public Key File");
			throw new ResolverException("Unable to read Public Key File", e);
		} catch (CertificateException e) {

			this.messageObserver.updateError(this.getClass().getName(), "Certificate Error reading Public Key File");
			throw new ResolverException("Certificate Error reading Public Key File", e);

		}

		if (cert == null) {

			this.messageObserver.updateError(this.getClass().getName(), "Public certificate cannot be null!");
			throw new ResolverException("Public certificate cannot be null!");
		}

		return cert;
	}

	public PrivateKey getPrivateKey(final char[] keyFile) throws ResolverException {

		PrivateKey privateKey = null;

		if (keyFile == null || keyFile.length == 0) {

			this.messageObserver.updateError(this.getClass().getName(),"Private Key cannot be null!");
			throw new ResolverException("Private Key file cannot be null!");
		}

		try {
			String stringPrivateKey = new String(keyFile);
			stringPrivateKey = stringPrivateKey.replace("-----BEGIN PRIVATE KEY-----\n", StringUtils.EMPTY);
			stringPrivateKey = stringPrivateKey.replace("-----END PRIVATE KEY-----", StringUtils.EMPTY);

			final KeyFactory keyFactory            = KeyFactory.getInstance("RSA");
			//final PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(stringPrivateKey));
			final PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(org.apache.commons.codec.binary.Base64.decodeBase64(stringPrivateKey));
			privateKey = keyFactory.generatePrivate(keySpecPKCS8);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {

			this.messageObserver.updateError(this.getClass().getName(),"Unable to translate Private Key");
			throw new ResolverException("Unable to translate Private Key", e);
		}  catch (Exception e) {

			this.messageObserver.updateError(this.getClass().getName(),"Unable to read Private Key File");
			throw new ResolverException("Unable to read Private Key File", e);

		}

		if (privateKey == null) {

			this.messageObserver.updateError(this.getClass().getName(),"Private certificate cannot be null!");
			throw new ResolverException("Private certificate cannot be null!");
		}

		return privateKey;
	}

}
