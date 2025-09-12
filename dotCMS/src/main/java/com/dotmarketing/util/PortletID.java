package com.dotmarketing.util;

/**
 * Set of Portlet constants
 */
public enum PortletID {

    CALENDAR,
    CATEGORIES, 
    CONFIGURATION, 
    CONTAINERS,
    CONTENT,
    CONTENT_TYPES,
    DASHBOARD,
    DIRECTOR,
    DYNAMIC_PLUGINS,
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
    QUERY_TOOL, 
    TAGS,
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
    ANALYTICS_DASHBOARD;

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
