package com.dotmarketing.util;

/**
 * The product's portlet id registry: every portlet id the product declares as its own. A tool
 * declared here is a product tool even when it is stored in the database the way an admin-made
 * custom content tool is (see {@link #LANGUAGE_VARIABLES}). The id is derived from the constant
 * name in lower case with underscores turned into dashes, unless given explicitly.
 */
public enum PortletID {

    CALENDAR,
    CATEGORIES,
    CATEGORIES_LEGACY("categories-legacy"),
    CONFIGURATION, 
    CONFIGURATION_BETA("configuration-beta"),
    CONTAINERS,
    CONTENT,
    CONTENT_TYPES,
    DASHBOARD,
    DIRECTOR,
    DYNAMIC_PLUGINS,
    PLUGINS,
    PLUGINS_LEGACY("plugins-legacy"),
    ES_SEARCH,
    EVENTS,
    EVENTS_APPROVAL,
    FOLDERS,
    FORMS, 
    HTML_PAGES,
    JOBS,
    LANGUAGES, 
    LEGACY_PAGE_VIEWS,
    LINKS,
    LINK_CHECKER,
    MAINTENANCE,
    MY_ACCOUNT,
    PERSONAS,
    PUBLISHING_QUEUE,
    PUBLISHING_QUEUE_LEGACY("publishing-queue-legacy"),
    QUERY_TOOL,
    TAGS,
    TAGS_LEGACY("tags-legacy"),
    TEMPLATES,
    TIME_MACHINE,
    REPORTS,
    RULES,
    ROLES, 
    SITES,
    SITE_BROWSER,
    SITE_SEARCH,
    USERS,
    /**
     * Beta-period alias of {@link #USERS}: the Angular Users portlet ships opt-in as
     * {@code users-beta} (see {@code portlet.xml}). A gate meaning "can administer users" should accept
     * either id while both portlets coexist, as {@code UserPaginator#canReadFullProfile} does; not every
     * Users-portlet check has been widened to the beta id yet. Remove when the Angular portlet is promoted.
     */
    USERS_BETA("users-beta"),
    VANITY_URLS,
    WEB_EVENT_REGISTRATIONS,
    WEB_FORMS,
    WORKFLOW,
    WORKFLOW_SCHEMES,
    LOCALES,
    ANALYTICS_DASHBOARD,
    USAGE,
    VELOCITY_PLAYGROUND("velocity_playground"),
    VELOCITY_PLAYGROUND_LEGACY("velocity_playground-legacy"),
    DOT_AUTH("dotAuth"),
    /** The Tools portlet, which owns the navigation sections and the tools inside them. */
    TOOLS,
    /**
     * Beta-period alias of {@link #TOOLS}. Every Tools gate accepts both ids while the Angular
     * portlet ships opt-in as {@code tools-beta}. Remove when the Tools portlet is promoted
     * (#37356).
     */
    TOOLS_BETA("tools-beta"),
    /**
     * Beta-period alias for the Network portlet. The cluster nodes endpoint accepts it alongside
     * {@link #CONFIGURATION} while the Angular Network portlet ships opt-in as {@code network-beta}.
     * Remove when the Network portlet is promoted (#37875).
     */
    NETWORK_BETA("network-beta"),
    /**
     * Language Variables ships with the product but is created as a database row by
     * {@code Task241016AddCustomLanguageVariablesPortletToLayout}, in the same shape as an
     * admin-made custom content tool. Declaring it here keeps it a product tool: it is never
     * flagged custom and cannot be removed through the custom-tool delete.
     */
    LANGUAGE_VARIABLES("c_Language-Variables");

    private final String url;

    private PortletID(){
        url = this.name().toLowerCase().replace("_", "-");
    }

    private PortletID(String url){
        this.url = url;
    }

    @Override
    public String toString() {
        return url;
    }

}
