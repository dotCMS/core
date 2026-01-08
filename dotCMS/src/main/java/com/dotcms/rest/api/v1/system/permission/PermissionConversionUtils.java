package com.dotcms.rest.api.v1.system.permission;

import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared utility class for permission conversion operations.
 * Provides static methods for converting between permission representations
 * (bits, names, types) used by REST API endpoints.
 *
 * <p>This utility centralizes permission conversion logic to avoid duplication
 * across helper classes like {@link AssetPermissionHelper} and
 * {@link com.dotcms.rest.api.v1.user.UserPermissionHelper}.
 *
 * <p>Uses {@link PermissionAPI.Scope} and {@link PermissionAPI.Type} enums
 * as the single source of truth for valid scopes and permission levels.
 *
 * @author dotCMS
 * @since 24.01
 */
public final class PermissionConversionUtils {

    private PermissionConversionUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Valid permission level names for validation.
     * Derived from PermissionAPI.Type canonical types for single source of truth.
     */
    public static final Set<String> VALID_PERMISSION_LEVELS =
        PermissionAPI.Type.getCanonicalLevelNames();

    /**
     * Gets the modern API type name for a permission type.
     * Uses {@link PermissionAPI.Scope} enum as the source of truth.
     *
     * @param permissionType Internal permission type (class name or "individual")
     * @return Modern API type constant (e.g., "FOLDER", "HOST", "CONTENT")
     */
    public static String getModernPermissionType(final String permissionType) {
        if (!UtilMethods.isSet(permissionType)) {
            return StringPool.BLANK;
        }

        final PermissionAPI.Scope scope = PermissionAPI.Scope.fromPermissionType(permissionType);
        if (scope != null) {
            return scope.name();
        }

        Logger.debug(PermissionConversionUtils.class,
            () -> String.format("Unknown permission type: %s", permissionType));
        return permissionType.toUpperCase();
    }

    /**
     * Converts permission bits to permission level names.
     * Uses {@link PermissionAPI.Type#fromBitsAsNames(int)} which excludes aliases (USE, EDIT).
     *
     * @param permissionBits Bit-packed permission value
     * @return List of permission level strings (e.g., ["READ", "WRITE"])
     */
    public static List<String> convertBitsToPermissionNames(final int permissionBits) {
        return PermissionAPI.Type.fromBitsAsNames(permissionBits);
    }

    /**
     * Converts permission level names to a bitwise permission value.
     *
     * @param permissionNames List of permission level names (READ, WRITE, etc.)
     * @return Combined bit value
     */
    public static int convertPermissionNamesToBits(final List<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return 0;
        }

        int bits = 0;
        for (final String name : permissionNames) {
            final PermissionAPI.Type type = PermissionAPI.Type.fromString(name);
            if (type != null) {
                bits |= type.getType();
            } else {
                Logger.warn(PermissionConversionUtils.class,
                    String.format("Unknown permission name: %s", name));
            }
        }
        return bits;
    }

    /**
     * Converts a collection of permission types to a bitwise permission value.
     * Type-safe version using enum directly.
     *
     * @param types Collection of PermissionAPI.Type enum values
     * @return Combined bit value
     */
    public static int convertTypesToBits(final Collection<PermissionAPI.Type> types) {
        if (types == null || types.isEmpty()) {
            return 0;
        }

        int bits = 0;
        for (final PermissionAPI.Type type : types) {
            bits |= type.getType();
        }
        return bits;
    }

    /**
     * Converts permission bits to a set of Type enums.
     *
     * @param permissionBits Bit-packed permission value
     * @return Set of PermissionAPI.Type enum values
     */
    public static Set<PermissionAPI.Type> convertBitsToTypes(final int permissionBits) {
        return PermissionAPI.Type.getCanonicalTypes().stream()
            .filter(type -> (permissionBits & type.getType()) > 0)
            .collect(Collectors.toSet());
    }

    /**
     * Converts an API scope name to internal permission type.
     * Uses {@link PermissionAPI.Scope} enum as the source of truth.
     *
     * @param scopeName API scope name (FOLDER, CONTENT, etc.)
     * @return Internal permission type (class canonical name)
     * @throws IllegalArgumentException If scope is unknown
     */
    public static String convertScopeToPermissionType(final String scopeName) {
        final PermissionAPI.Scope scope = PermissionAPI.Scope.fromName(scopeName);
        if (scope == null) {
            throw new IllegalArgumentException(String.format(
                "Invalid permission scope: %s", scopeName));
        }
        return scope.getPermissionType();
    }

    /**
     * Converts an API scope name to Scope enum.
     *
     * @param scopeName API scope name (FOLDER, CONTENT, etc.)
     * @return PermissionAPI.Scope enum value, or null if invalid
     */
    public static PermissionAPI.Scope toScope(final String scopeName) {
        return PermissionAPI.Scope.fromName(scopeName);
    }

    /**
     * Validates that a permission level name is valid.
     * Uses {@link PermissionAPI.Type} enum as the source of truth.
     *
     * @param permissionName Permission level name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPermissionLevel(final String permissionName) {
        if (permissionName == null) {
            return false;
        }
        return PermissionAPI.Type.fromString(permissionName) != null;
    }

    /**
     * Validates that a scope name is valid.
     * Uses {@link PermissionAPI.Scope} enum as the source of truth.
     *
     * @param scopeName Scope name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidScope(final String scopeName) {
        return PermissionAPI.Scope.fromName(scopeName) != null;
    }
}
