package com.dotcms.rest.api.v1.system.permission;

import com.dotmarketing.business.PermissionAPI;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Utility class for permission conversion operations.
 * Provides static methods for retrieving available permission types
 * used by REST API endpoints.
 *
 * @author hassandotcms
 */
public final class PermissionUtils {

    /**
     * Available permission scopes for the REST API.
     * Excludes STRUCTURE which is a backward-compatibility alias for CONTENT_TYPE.
     */
    private static final Set<PermissionAPI.Scope> AVAILABLE_SCOPES = Collections.unmodifiableSet(
        EnumSet.complementOf(EnumSet.of(PermissionAPI.Scope.STRUCTURE))
    );

    private PermissionUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Returns all available permission scopes that can be assigned.
     * These represent the different asset types that support permissions.
     * Excludes STRUCTURE (use CONTENT_TYPE instead).
     *
     * @return Set of permission scopes (e.g., INDIVIDUAL, HOST, FOLDER)
     */
    public static Set<PermissionAPI.Scope> getAvailablePermissionScopes() {
        return AVAILABLE_SCOPES;
    }

    /**
     * Returns all available permission levels that can be assigned.
     * Returns only canonical types (excludes aliases USE and EDIT).
     *
     * @return Set of canonical permission types (READ, WRITE, PUBLISH, EDIT_PERMISSIONS, CAN_ADD_CHILDREN)
     */
    public static Set<PermissionAPI.Type> getAvailablePermissionLevels() {
        return PermissionAPI.Type.getCanonicalTypes();
    }
}
