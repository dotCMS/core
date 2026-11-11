package com.dotcms.featureflag;

/**
 * This class is in charge of encapsulating all the feature flag names
 * @author jsanca
 */
public interface FeatureFlagName {

    // Experiments and Analytics
    String TELEMETRY_API_METRICS_ENABLED = "TELEMETRY_API_METRICS_ENABLED";

    String FEATURE_FLAG_EXPERIMENTS = "FEATURE_FLAG_EXPERIMENTS";

    String FEATURE_FLAG_TELEMETRY_CORE_ENABLED = "FEATURE_FLAG_TELEMETRY_CORE_ENABLED";

    String FEATURE_FLAG_CONTENT_ANALYTICS = "FEATURE_FLAG_CONTENT_ANALYTICS";

    String FEATURE_FLAG_CONTENT_ANALYTICS_AUTO_INJECT = "FEATURE_FLAG_CONTENT_ANALYTICS_AUTO_INJECT";

    // Unique Fields
    String FEATURE_FLAG_DB_UNIQUE_FIELD_VALIDATION = "FEATURE_FLAG_DB_UNIQUE_FIELD_VALIDATION";

    /**
     * Master switch for the Velocity tool hardening on this LTS: the read-only {@code $cmsuser}
     * profiles, the removal of the API accessors from the bound tools and the Scripting Developer
     * rule for Velocity inside widget field values over REST. Off by default on the LTS, so
     * templates and widgets keep behaving as they did.
     * Turn it on to close the template privilege escalation; the two flags below then fine-tune
     * the widget and {@code $cmsuser} parts.
     */
    String FEATURE_FLAG_VELOCITY_TOOL_HARDENING = "FEATURE_FLAG_VELOCITY_TOOL_HARDENING";

    /**
     * Kill switch for widget field scripting over REST ({@code /api/widget},
     * {@code /api/content?render=true} and {@code _search} with {@code render}), read only when
     * {@link #FEATURE_FLAG_VELOCITY_TOOL_HARDENING} is on. On by default:
     * Velocity found in a widget's field values is evaluated when the widget was last modified by
     * a user holding the Scripting Developer role, and rendered as text otherwise. Off: field
     * values render as text for every widget. Pages are not affected either way.
     */
    String FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER = "FEATURE_FLAG_WIDGET_FIELD_VELOCITY_RENDER";

    // others
    String FULLSCREEN_FIELD_FEATURE_FLAG = "content.edit.ui.fullscreen";

    String FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET = "FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET";

    String DOTFAVORITEPAGE_FEATURE_ENABLE = "DOTFAVORITEPAGE_FEATURE_ENABLE";

    String FEATURE_FLAG_TEMPLATE_BUILDER_2 = "FEATURE_FLAG_TEMPLATE_BUILDER_2";

    String FEATURE_FLAG_SEO_IMPROVEMENTS = "FEATURE_FLAG_SEO_IMPROVEMENTS";

    String FEATURE_FLAG_SEO_PAGE_TOOLS = "FEATURE_FLAG_SEO_PAGE_TOOLS";

    String FEATURE_FLAG_EDIT_URL_CONTENT_MAP = "FEATURE_FLAG_EDIT_URL_CONTENT_MAP";

    String FEATURE_FLAG_NEW_BINARY_FIELD = "FEATURE_FLAG_NEW_BINARY_FIELD";

    String FEATURE_FLAG_ANNOUNCEMENTS = "FEATURE_FLAG_ANNOUNCEMENTS";

    String FEATURE_FLAG_NEW_EDIT_PAGE = "FEATURE_FLAG_NEW_EDIT_PAGE";

    String FEATURE_FLAG_UVE_PREVIEW_MODE = "FEATURE_FLAG_UVE_PREVIEW_MODE";

    String FEATURE_FLAG_UVE_TOGGLE_LOCK = "FEATURE_FLAG_UVE_TOGGLE_LOCK";

    String FEATURE_FLAG_UVE_STYLE_EDITOR = "FEATURE_FLAG_UVE_STYLE_EDITOR";

    String FEATURE_FLAG_PAGE_SCANNER = "FEATURE_FLAG_PAGE_SCANNER";

    String FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION = "FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION";

    /**
     * Controls the active ES → OpenSearch migration phase (integer ordinal 0–3).
     *
     * <ul>
     *   <li>0 — ES only (default)</li>
     *   <li>1 — dual-write, ES reads</li>
     *   <li>2 — dual-write, OS reads</li>
     *   <li>3 — OS only</li>
     * </ul>
     *
     * @see com.dotcms.content.index.IndexConfigHelper.MigrationPhase
     */
    String FEATURE_FLAG_OPEN_SEARCH_PHASE = "FEATURE_FLAG_OPEN_SEARCH_PHASE";

    String FEATURE_FLAG_NEW_BLOCK_EDITOR = "FEATURE_FLAG_NEW_BLOCK_EDITOR";

    String FEATURE_FLAG_REPORT_ISSUE_ENABLED = "FEATURE_FLAG_REPORT_ISSUE_ENABLED";
    /**
     * Enables the new content editor (Edit Content v2).
     * Also checked in content-type metadata to opt individual types out.
     * Frontend equivalent: {@code FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED}.
     */
    String FEATURE_FLAG_CONTENT_EDITOR2_ENABLED = "CONTENT_EDITOR2_ENABLED";

    String FEATURE_FLAG_LOCALE_SELECTOR_V2 = "FEATURE_FLAG_LOCALE_SELECTOR_V2";

    /**
     * Opens the new content editor (Edit Content v2) in a right slide-in side panel instead of
     * navigating full-screen (Content Drive) or a centered dialog (UVE). On by default
     * ({@code dotmarketing-config.properties} sets {@code FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL=true}).
     * Frontend equivalent: {@code FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL}.
     */
    String FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL = "FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL";

    /**
     * Minifies rendered page HTML (collapses insignificant whitespace and strips comments) before
     * it is written to the response. Off by default.
     *
     * @see com.dotcms.rendering.util.HtmlMinifier
     */
    String FEATURE_FLAG_MINIFY_HTML = "FEATURE_FLAG_MINIFY_HTML";

    /**
     * libvips image-engine toggle (off by default; the legacy Java2D engine is used
     * otherwise). The new image editor reads this through the configuration endpoint
     * to gate the libvips-only AVIF output format. Not a UI feature flag — kept here so
     * {@code ConfigurationResource} can reference it as a constant like the others.
     */
    String IMAGE_API_USE_LIBVIPS = "IMAGE_API_USE_LIBVIPS";

    /**
     * Lets users with the Apps portlet change and read Apps configuration, upload app descriptors
     * and remove apps, as they could before Apps configuration became an administrator setting.
     * Off by default; kept for one release for installations that delegated Apps configuration to
     * non-administrator roles.
     *
     * @see com.dotcms.security.apps.AppsAPIImpl
     */
    String FEATURE_FLAG_APPS_ALLOW_PORTLET_CONFIGURATION =
            "FEATURE_FLAG_APPS_ALLOW_PORTLET_CONFIGURATION";

    /**
     * With {@link #FEATURE_FLAG_VELOCITY_TOOL_HARDENING} on, makes the {@code $cmsuser} Velocity
     * tool's lookups ({@code getUserByEmail},
     * {@code getUserByUserId}, {@code getAllUsersInGroup}) return read-only
     * {@code UserProfile} objects. On by default: when the flag is not set, it is treated as
     * {@code true}. Turn it off only for installations whose templates pass looked-up accounts to
     * methods that require a {@code User}; lookups then return detached {@code User} copies with
     * the credential fields cleared.
     */
    String FEATURE_FLAG_CMSUSER_READ_ONLY_PROFILES = "FEATURE_FLAG_CMSUSER_READ_ONLY_PROFILES";
}
