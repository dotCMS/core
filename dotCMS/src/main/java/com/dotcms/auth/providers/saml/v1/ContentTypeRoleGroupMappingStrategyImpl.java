package com.dotcms.auth.providers.saml.v1;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.SamlName;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import org.apache.velocity.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This implementation is based on a content type to role group mapping.
 * If there is not configuration on SAML to the content type mapping, will use the following keys as a default:
 * - ContentType Varname: RoleMap
 * - ContentType Field Varname for Group Key: fromRoleName
 * - ContentType Field Varname for Group Key:toRoleNames
 * @author jsanca
 */
public class ContentTypeRoleGroupMappingStrategyImpl implements RoleGroupMappingStrategy {

    public static final String ROLE_MAP_CONTENT_TYPE_NAME = "RoleMap";

    public static final String ROLE_MAP_KEY_FIELD_NAME = "fromRoleName";

    public static final String ROLE_MAP_VALUES_FIELD_NAME = "toRoleNames";

    private final Map<String, Collection<String>> roleKeyMappingsMap = new ConcurrentHashMap<>();

    @Override
    public Collection<String> getRolesForGroup(final String roleGroupKey,
                                               final IdentityProviderConfiguration identityProviderConfiguration) {

        if (roleKeyMappingsMap.isEmpty()) {
            Logger.debug(this, () -> "Loading Role Mappings");
            loadRoleMappings(identityProviderConfiguration);
        }

        Logger.debug(this, () -> "Retrieving the getRolesForGroup, Role Group Key: " + roleGroupKey);
        return this.roleKeyMappingsMap.getOrDefault(roleGroupKey, Arrays.asList(roleGroupKey));
    }


    private synchronized void loadRoleMappings(final IdentityProviderConfiguration identityProviderConfiguration) {

        try {
            final String samlAppContentTypeRoleGroupMappingConfigValue = identityProviderConfiguration.containsOptionalProperty(
                        SamlName.DOT_SAML_ROLES_GROUP_MAPPING_BY_CONTENT_TYPE.getPropertyName()) ?
                    identityProviderConfiguration.getOptionalProperty(SamlName.DOT_SAML_ROLES_GROUP_MAPPING_BY_CONTENT_TYPE.getPropertyName()).toString() : null;

            Logger.debug(this, () -> "SAML App Content Type Role Group Mapping Config Value: " + samlAppContentTypeRoleGroupMappingConfigValue);
            final String[] samlAppContentTypeRoleGroupMappingConfigValueArray = Objects.nonNull(samlAppContentTypeRoleGroupMappingConfigValue) ?
                    StringUtils.split(samlAppContentTypeRoleGroupMappingConfigValue, StringPool.COMMA) : new String[]{null, null, null};

            Logger.debug(this, () -> "SAML App Content Type Role Group Mapping Config Value Array: " + Arrays.toString(samlAppContentTypeRoleGroupMappingConfigValueArray));
            final String roleMapKeyName = samlAppContentTypeRoleGroupMappingConfigValueArray.length > 0 &&
                    Objects.nonNull(samlAppContentTypeRoleGroupMappingConfigValueArray[0]) ?
                    samlAppContentTypeRoleGroupMappingConfigValueArray[0] : ROLE_MAP_CONTENT_TYPE_NAME;

            Logger.debug(this, () -> "Role Map Key Name: " + roleMapKeyName);
            final String roleMapFieldKeyName = samlAppContentTypeRoleGroupMappingConfigValueArray.length > 1 &&
                    Objects.nonNull(samlAppContentTypeRoleGroupMappingConfigValueArray[1]) ?
                    samlAppContentTypeRoleGroupMappingConfigValueArray[1] : ROLE_MAP_KEY_FIELD_NAME;

            Logger.debug(this, () -> "Role Map Field Key Name: " + roleMapFieldKeyName);
            final String roleMapKeyValuesName = samlAppContentTypeRoleGroupMappingConfigValueArray.length > 2 &&
                    Objects.nonNull(samlAppContentTypeRoleGroupMappingConfigValueArray[2]) ?
                    samlAppContentTypeRoleGroupMappingConfigValueArray[2] : ROLE_MAP_VALUES_FIELD_NAME;

            Logger.debug(this, () -> "Role Map Key Values Name: " + roleMapKeyValuesName);

            final ContentType roleMapContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(roleMapKeyName);
            if (Objects.nonNull(roleMapContentType)) {

                loadMappingRoles(roleMapContentType, roleMapFieldKeyName, roleMapKeyValuesName);
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    private void loadMappingRoles(final ContentType roleMapContentType,
                                  final String roleMapFieldKeyName,
                                  final String roleMapKeyValuesName) throws DotDataException, DotSecurityException {

        final List<Contentlet> roleMapContentlets = APILocator.getContentletAPI().findByStructure(roleMapContentType.id(),
                APILocator.systemUser(), false, 0, 0);
        if (UtilMethods.isSet(roleMapContentlets)) {

            Logger.debug(this, () -> "Role Map Contentlets: " + roleMapContentlets.size());

            for (final Contentlet roleMapContentlet : roleMapContentlets) {

                final String roleGroupKey = roleMapContentlet.getStringProperty(roleMapFieldKeyName);
                final String roleKeyCommaSeparatedList = roleMapContentlet.getStringProperty(roleMapKeyValuesName);

                Logger.debug(this, () -> "Role Group Key: " + roleGroupKey +
                        ", Role Key Comma Separated List: " + roleKeyCommaSeparatedList);

                if (UtilMethods.isSet(roleKeyCommaSeparatedList)) {

                    this.roleKeyMappingsMap.put(roleGroupKey, Arrays.asList(StringUtils.split(roleKeyCommaSeparatedList, StringPool.COMMA)));
                } else {
                    Logger.debug(this, () -> "Empty Role Map found for this key: " + roleGroupKey);
                }
            }
        }
    }
}
