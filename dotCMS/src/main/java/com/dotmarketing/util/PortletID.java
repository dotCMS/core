package com.dotmarketing.util;

/**
 * Set of Portlet constants
 */
public enum PortletID {

    CALENDAR,
    CATEGORIES,
    CATEGORIES_LEGACY("categories-legacy"),
    CONFIGURATION, 
    CONTAINERS,
    CONTENT,
    CONTENT_TYPES,
    DASHBOARD,
    DIRECTOR,
    DYNAMIC_PLUGINS,
    PLUGINS,
    PLUGINS_LEGACY("plugins-legacy"),
    ES_SEARCH,
    ES_SEARCH_LEGACY("es-search-legacy"),
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
    QUERY_TOOL,
    QUERY_TOOL_LEGACY("query-tool-legacy"),
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
    DOT_AUTH("dotAuth");

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
