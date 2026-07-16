package com.dotcms.auth.providers.saml.v1;

import com.dotcms.saml.IdentityProviderConfiguration;

import java.util.Collection;

/**
 * This class is in charge of mapping the group roles from the SAML response to the collection role keys in dotCMS.
 * @author jsanca
 */
@FunctionalInterface
public interface RoleGroupMappingStrategy {

    /**
     * Returns the roles group key for the given role group name.
     * If role group key does not exist, it is returned as the only element in the collection.
     * @param roleGroupKey String
     * @return Collection<String>
     */
    Collection<String> getRolesForGroup(String roleGroupKey, IdentityProviderConfiguration identityProviderConfiguration);
}
