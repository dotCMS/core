package com.dotcms.saml.service.internal;

import org.opensaml.security.credential.Credential;

import java.io.Serializable;

/**
 * This is the contract to create a new custom credential, if you have some SP
 * or Idp credential implement this contract and set's the class name on the dot
 * cms configuration.
 *
 * @author jsanca
 */
public interface CredentialProvider extends Serializable {

    /**
     * Creates the custom credential
     *
     * @return Credential
     */
    Credential createCredential();
}
