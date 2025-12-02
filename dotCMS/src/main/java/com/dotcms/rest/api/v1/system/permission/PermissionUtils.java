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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for permission conversion operations.
 * Provides static methods for converting between permission representations
 * used by REST API endpoints.
 *
 * @author dotCMS
 * @since 24.01
 */
public final class PermissionUtils {

    private PermissionUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Maps internal permission type class names to modern API type constants.
     * Keys are uppercase for case-insensitive lookup.
     *
     * Stays in REST layer (not PermissionAPI) because: it maps class names to modern API names
     * (presentation concern), and includes INDIVIDUAL/HOST which are scope modifiers not asset types.
     */
    private static final Map<String, String> PERMISSION_TYPE_MAPPINGS = Map.ofEntries(
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
     * Returns all available permission scopes that can be assigned.
     * These represent the different asset types that support permissions.
     *
     * @return Set of permission scope names (e.g., "INDIVIDUAL", "HOST", "FOLDER")
     */
    public static Set<String> getAvailablePermissionScopes() {
        return new HashSet<>(PERMISSION_TYPE_MAPPINGS.values());
    }

    /**
     * Returns all available permission levels that can be assigned.
     * These represent the different types of access (READ, WRITE, etc.).
     *
     * @return Set of permission level names (maintains insertion order)
     */
    public static Set<String> getAvailablePermissionLevels() {
        return PermissionAPI.Type.getCanonicalLevelNames();
    }
}
