package com.dotcms.rest.api.v1.system.permission;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared utility class for permission conversion operations.
 * Provides static methods for converting between permission representations
 * (bits, names, types) used by REST API endpoints.
 *
 * <p>This utility centralizes permission conversion logic to avoid duplication
 * across helper classes like {@link AssetPermissionHelper} and
 * {@link com.dotcms.rest.api.v1.user.UserPermissionHelper}.
 *
 * @author dotCMS
 * @since 24.01
 */
public final class PermissionConversionUtils {

    private PermissionConversionUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Maps internal permission type class names to modern API type constants.
     * Keys are uppercase for case-insensitive lookup.
     */
    public static final Map<String, String> PERMISSION_TYPE_MAPPINGS = Map.ofEntries(
        Map.entry(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE.toUpperCase(), "INDIVIDUAL"),
        Map.entry(IHTMLPage.class.getCanonicalName().toUpperCase(), "PAGE"),
        Map.entry(Container.class.getCanonicalName().toUpperCase(), "CONTAINER"),
        Map.entry(Folder.class.getCanonicalName().toUpperCase(), "FOLDER"),
        Map.entry(Link.class.getCanonicalName().toUpperCase(), "LINK"),
        Map.entry(Template.class.getCanonicalName().toUpperCase(), "TEMPLATE"),
        Map.entry(TemplateLayout.class.getCanonicalName().toUpperCase(), "TEMPLATE_LAYOUT"),
        Map.entry(Structure.class.getCanonicalName().toUpperCase(), "CONTENT_TYPE"),
        Map.entry(Contentlet.class.getCanonicalName().toUpperCase(), "CONTENT"),
        Map.entry(Category.class.getCanonicalName().toUpperCase(), "CATEGORY"),
        Map.entry(Rule.class.getCanonicalName().toUpperCase(), "RULE"),
        Map.entry(Host.class.getCanonicalName().toUpperCase(), "HOST")
    );

    /**
     * Maps API scope names to internal permission type class names.
     * Reverse of PERMISSION_TYPE_MAPPINGS for use in update operations.
     */
    public static final Map<String, String> SCOPE_TO_TYPE_MAPPINGS = Map.ofEntries(
        Map.entry("INDIVIDUAL", PermissionAPI.INDIVIDUAL_PERMISSION_TYPE),
        Map.entry("FOLDER", Folder.class.getCanonicalName()),
        Map.entry("HOST", Host.class.getCanonicalName()),
        Map.entry("CONTENT", Contentlet.class.getCanonicalName()),
        Map.entry("PAGE", IHTMLPage.class.getCanonicalName()),
        Map.entry("CONTAINER", Container.class.getCanonicalName()),
        Map.entry("TEMPLATE", Template.class.getCanonicalName()),
        Map.entry("TEMPLATE_LAYOUT", TemplateLayout.class.getCanonicalName()),
        Map.entry("LINK", Link.class.getCanonicalName()),
        Map.entry("CONTENT_TYPE", Structure.class.getCanonicalName()),
        Map.entry("CATEGORY", Category.class.getCanonicalName()),
        Map.entry("RULE", Rule.class.getCanonicalName())
    );

    /**
     * Maps permission level names to their bit values.
     * Used for both validation and conversion.
     */
    public static final Map<String, Integer> PERMISSION_NAME_TO_BITS = Map.of(
        "READ", PermissionAPI.PERMISSION_READ,
        "WRITE", PermissionAPI.PERMISSION_WRITE,
        "PUBLISH", PermissionAPI.PERMISSION_PUBLISH,
        "EDIT_PERMISSIONS", PermissionAPI.PERMISSION_EDIT_PERMISSIONS,
        "CAN_ADD_CHILDREN", PermissionAPI.PERMISSION_CAN_ADD_CHILDREN
    );

    /**
     * Valid permission level names for validation.
     * Derived from PERMISSION_NAME_TO_BITS keys for single source of truth.
     */
    public static final Set<String> VALID_PERMISSION_LEVELS = PERMISSION_NAME_TO_BITS.keySet();

    /**
     * Gets the modern API type name for a permission type.
     *
     * @param permissionType Internal permission type (class name or scope)
     * @return Modern API type constant (e.g., "FOLDER", "HOST", "CONTENT")
     */
    public static String getModernPermissionType(final String permissionType) {
        if (!UtilMethods.isSet(permissionType)) {
            return StringPool.BLANK;
        }

        final String mappedType = PERMISSION_TYPE_MAPPINGS.get(permissionType.toUpperCase());
        if (mappedType != null) {
            return mappedType;
        }

        Logger.debug(PermissionConversionUtils.class,
            () -> String.format("Unknown permission type: %s", permissionType));
        return permissionType.toUpperCase();
    }

    /**
     * Converts permission bits to permission level names.
     * Avoids duplicate aliases (e.g., USE=READ, EDIT=WRITE).
     *
     * @param permissionBits Bit-packed permission value
     * @return List of permission level strings (e.g., ["READ", "WRITE"])
     */
    public static List<String> convertBitsToPermissionNames(final int permissionBits) {
        final List<String> permissions = new ArrayList<>();

        if ((permissionBits & PermissionAPI.PERMISSION_READ) > 0) {
            permissions.add("READ");
        }
        if ((permissionBits & PermissionAPI.PERMISSION_WRITE) > 0) {
            permissions.add("WRITE");
        }
        if ((permissionBits & PermissionAPI.PERMISSION_PUBLISH) > 0) {
            permissions.add("PUBLISH");
        }
        if ((permissionBits & PermissionAPI.PERMISSION_EDIT_PERMISSIONS) > 0) {
            permissions.add("EDIT_PERMISSIONS");
        }
        if ((permissionBits & PermissionAPI.PERMISSION_CAN_ADD_CHILDREN) > 0) {
            permissions.add("CAN_ADD_CHILDREN");
        }

        return permissions;
    }

    /**
     * Converts permission level names to a bitwise permission value.
     *
     * @param permissionNames Collection of permission level names (READ, WRITE, etc.)
     * @return Combined bit value
     */
    public static int convertPermissionNamesToBits(final Collection<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return 0;
        }

        int bits = 0;
        for (final String name : permissionNames) {
            final Integer bitValue = PERMISSION_NAME_TO_BITS.get(name.toUpperCase());
            if (bitValue != null) {
                bits |= bitValue;
            } else {
                Logger.warn(PermissionConversionUtils.class,
                    String.format("Unknown permission name: %s", name));
            }
        }
        return bits;
    }

    /**
     * Converts an API scope name to internal permission type.
     *
     * @param scopeName API scope name (FOLDER, CONTENT, etc.)
     * @return Internal permission type (class canonical name)
     * @throws IllegalArgumentException If scope is unknown
     */
    public static String convertScopeToPermissionType(final String scopeName) {
        final String type = SCOPE_TO_TYPE_MAPPINGS.get(scopeName.toUpperCase());
        if (type == null) {
            throw new IllegalArgumentException(String.format(
                "Invalid permission scope: %s", scopeName));
        }
        return type;
    }

    /**
     * Validates that a permission level name is valid.
     *
     * @param permissionName Permission level name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPermissionLevel(final String permissionName) {
        return permissionName != null &&
               VALID_PERMISSION_LEVELS.contains(permissionName.toUpperCase());
    }

    /**
     * Validates that a scope name is valid.
     *
     * @param scopeName Scope name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidScope(final String scopeName) {
        return scopeName != null &&
               SCOPE_TO_TYPE_MAPPINGS.containsKey(scopeName.toUpperCase());
    }
}
