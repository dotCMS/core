package com.dotcms.rest.api.v1.grafana;

import com.dotcms.rest.api.v1.grafana.client.dto.DashboardDetail;
import com.dotcms.rest.api.v1.grafana.client.dto.DashboardSearchResult;
import com.dotcms.rest.api.v1.grafana.client.dto.GrafanaFolder;

import java.util.List;

/**
 * API interface for Grafana integration.
 *
 * This interface provides high-level methods for interacting with Grafana
 * following dotCMS API patterns.
 */
public interface GrafanaAPI {

    /**
     * Search for dashboards with various filters
     *
     * @param query      Search query string
     * @param type       Dashboard type filter
     * @param starred    Filter by starred status
     * @param folderIds  Comma-separated folder IDs
     * @param tag        Tag filter
     * @param limit      Maximum number of results
     * @return List of matching dashboard search results
     */
    List<DashboardSearchResult> searchDashboards(String query, String type, Boolean starred,
                                                 String folderIds, String tag, Integer limit);

    /**
     * Get detailed dashboard information by UID
     *
     * @param uid Dashboard unique identifier
     * @return Dashboard detail information
     * @throws IllegalArgumentException if uid is null or empty
     * @throws RuntimeException if dashboard cannot be retrieved
     */
    DashboardDetail getDashboardByUid(String uid);

    /**
     * Get all folders with optional limit
     *
     * @param limit Maximum number of folders to return
     * @return List of Grafana folders
     */
    List<GrafanaFolder> getFolders(Integer limit);

    /**
     * Get folder by UID
     *
     * @param uid Folder unique identifier
     * @return Grafana folder information
     * @throws IllegalArgumentException if uid is null or empty
     * @throws RuntimeException if folder cannot be retrieved
     */
    GrafanaFolder getFolderByUid(String uid);

    /**
     * Get dashboards contained in a specific folder
     *
     * @param folderUid Folder unique identifier
     * @return List of dashboard search results in the folder
     * @throws IllegalArgumentException if folderUid is null or empty
     */
    List<DashboardSearchResult> getDashboardsInFolder(String folderUid);

    /**
     * Test connectivity to Grafana API
     *
     * @return true if connection is successful, false otherwise
     */
    boolean testConnection();
}