package com.dotcms.saml.utils;

import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.service.internal.SamlCoreService;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import org.apache.xml.security.c14n.Canonicalizer;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xml.util.Base64;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.ConfigurableContentReference;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidationProvider;
import org.opensaml.xmlsec.signature.support.provider.ApacheSantuarioSignerProviderImpl;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Signature utils
 * @author jsanca
 */
public class SignatureUtils {

    private static SignatureValidationProvider validatorInstance;

    public static void init () {

        Try.run(()->getSignatureValidationProvider()).onFailure(throwable ->
                Logger.error(SignatureUtils.class.getName(), throwable.getMessage()));
    }

    public static void validate(final Signature signature, final Credential validationCredential) throws SignatureException {

        final SignatureValidationProvider validator = getSignatureValidationProvider();
        Logger.debug(SignatureUtils.class.getName(),
                "Using a validation provider of implementation: " + validator.getClass().getName());
        validator.validate(signature, validationCredential);
    }

    private static SignatureValidationProvider getSignatureValidationProvider() throws SignatureException {

        if (validatorInstance == null) {

            final ServiceLoader<SignatureValidationProvider> loader = ServiceLoader.load(SignatureValidationProvider.class);
            final Iterator<SignatureValidationProvider> iterator = loader.iterator();
            if (!iterator.hasNext()) {

                throw new SignatureException("Could not load a signature validation provider implementation via service API");
            }

            validatorInstance = iterator.next();
        }

        return validatorInstance;
    }

    /**
     *
     * @param samlCoreService
     * @param messageObserver
     * @param credential
     * @param signatureAlgorithm
     * @return
     */
    public static Signature createSignature(final SamlCoreService samlCoreService, final MessageObserver messageObserver,
                                             final X509Credential credential, final String signatureAlgorithm) {

        final Signature signature       = samlCoreService.buildSAMLObject(Signature.class);
        signature.setSigningCredential(credential);
        signature.setSignatureAlgorithm(signatureAlgorithm);
        signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        try {

            final KeyInfo keyInfo       = samlCoreService.buildSAMLObject(KeyInfo.class);
            final X509Data data         = samlCoreService.buildSAMLObject(X509Data.class);
            final X509Certificate cert  = samlCoreService.buildSAMLObject(X509Certificate.class);
            final String value          = Base64.encodeBytes(credential.getEntityCertificate().getEncoded());
            cert.setValue(value);
            data.getX509Certificates().add(cert);
            keyInfo.getX509Datas().add(data);
            signature.setKeyInfo(keyInfo);
        } catch (Exception e) {

            messageObserver.updateError(SignatureUtils.class.getName(), e.getMessage(), e);
            throw new RuntimeException("Error getting certificate", e);
        }

        return signature;
    } // createSignature.

    public static void setSignatureSigningParams(final Credential credential,
                                           final MessageContext context) {

        final SignatureSigningParameters signatureSigningParameters = new SignatureSigningParameters();

        signatureSigningParameters.setSigningCredential(credential);
        signatureSigningParameters.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);

        context.getSubcontext(SecurityParametersContext.class, true)
                .setSignatureSigningParameters(signatureSigningParameters);
    } // setSignatureSigningParams.

    /**
     * Do the XML Marshal and sing the signature
     * @param xmlObject
     * @param messageObserver
     * @param signature
     * @param digestAlgorithm
     * @throws SignatureException
     * @throws MarshallingException
     */
    public static void marshalAndSing (final XMLObject xmlObject, final MessageObserver messageObserver,
                                       final Signature signature, final String digestAlgorithm) throws SignatureException, MarshallingException {

        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(xmlObject).marshall(xmlObject);
        if (null != signature.getContentReferences() && !signature.getContentReferences().isEmpty()) {

            messageObserver.updateInfo(SignatureUtils.class.getName(), "digestAlgorithm = " + digestAlgorithm);
            ConfigurableContentReference.class.cast(signature.getContentReferences().get(0)).setDigestAlgorithm(digestAlgorithm);
        }

        new ApacheSantuarioSignerProviderImpl().signObject(signature);
    }
}
