package com.dotmarketing.portlets.contentlet.business;

/**
 * This class is used to define the options for the search of a host.
 * The options are:
 * - includeSystemHost: if true, the system host will be included in the search
 * - retrieveLiveVersion: if true, the live version of the host will be retrieved if available
 * - respectFrontendRoles: if true, the search will respect the user frontend roles
 */
public class HostSearchOptions {
    private boolean includeSystemHost = false;
    private boolean retrieveLiveVersion = false;
    private boolean respectFrontendRoles = false;

    /**
     * Returns if the system host is included in the search
     * @return true if the system host is included in the search, false otherwise
     */
    public boolean isIncludeSystemHost() {
        return includeSystemHost;
    }

    /**
     * Returns if the live version of the host is retrieved if available
     * @return true if the live version of the host is retrieved, false otherwise
     */
    public boolean isRetrieveLiveVersion() {
        return retrieveLiveVersion;
    }

    /**
     * Returns if the search respects the user frontend roles
     * @return true if the search respects the user frontend roles, false otherwise
     */
    public boolean isRespectFrontendRoles() {
        return respectFrontendRoles;
    }

    /**
     * Sets if the system host is included in the search
     * @param includeSystemHost true if the system host is included in the search, false otherwise
     * @return this object
     */
    public HostSearchOptions withIncludeSystemHost(boolean includeSystemHost) {
        this.includeSystemHost = includeSystemHost;
        return this;
    }

    /**
     * Sets if the live version of the host is retrieved if available
     * @param retrieveLiveVersion true if the live version of the host is retrieved, false otherwise
     * @return this object
     */
    public HostSearchOptions withRetrieveLiveVersion(boolean retrieveLiveVersion) {
        this.retrieveLiveVersion = retrieveLiveVersion;
        return this;
    }

    /**
     * Sets if the search respects the user frontend roles
     * @param respectFrontendRoles true if the search respects the user frontend roles, false otherwise
     * @return this object
     */
    public HostSearchOptions withRespectFrontendRoles(boolean respectFrontendRoles) {
        this.respectFrontendRoles = respectFrontendRoles;
        return this;
    }
}
