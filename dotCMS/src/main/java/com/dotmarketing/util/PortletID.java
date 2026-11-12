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
    /** The Experiments tool; also gates the Variants REST resource that serves it. */
    EXPERIMENTS,
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
    /** The Angular Roles portlet, which took over this id from the old screen (now {@link #ROLES_LEGACY}). */
    ROLES,
    SITES,
    SITE_BROWSER,
    SITE_SEARCH,
    /** The Angular Users portlet, which took over this id from the old screen (now {@link #USERS_LEGACY}). */
    USERS,
    /**
     * The old JSP/Dojo Users screen, kept as an opt-in portlet after the Angular Users portlet
     * took over the {@link #USERS} id. It ships in no menu layout; an administrator adds it to a
     * section from the Tools portlet. Gates that mean "can administer users" accept it alongside
     * {@link #USERS}, so a role holding only this portlet can still use the screen.
     */
    USERS_LEGACY("users-legacy"),
    /**
     * The old JSP/Dojo Roles &amp; Tools screen, kept as an opt-in portlet after the Angular Roles
     * portlet took over the {@link #ROLES} id. It ships in no menu layout. The gates its Roles
     * and Tools tabs call accept it alongside {@link #ROLES}.
     */
    ROLES_LEGACY("roles-legacy"),
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
