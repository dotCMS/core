package com.dotcms.rest.api.v1.grafana;

import com.dotcms.rest.api.v1.grafana.client.GrafanaClient;
import com.dotcms.rest.api.v1.grafana.client.dto.DashboardDetail;
import com.dotcms.rest.api.v1.grafana.client.dto.DashboardSearchResult;
import com.dotcms.rest.api.v1.grafana.client.dto.GrafanaFolder;
import com.dotmarketing.util.Logger;

import java.util.List;

/**
 * Implementation of GrafanaAPI using the GrafanaClient.
 *
 * This class provides a service layer on top of the raw Grafana client,
 * adding business logic, error handling, and following dotCMS patterns.
 */
public class GrafanaAPIImpl implements GrafanaAPI {

    private final GrafanaClient grafanaClient;

    public GrafanaAPIImpl() {
        this.grafanaClient = new GrafanaClient();
    }

    // Constructor for dependency injection/testing
    public GrafanaAPIImpl(GrafanaClient grafanaClient) {
        this.grafanaClient = grafanaClient;
    }

    @Override
    public List<DashboardSearchResult> searchDashboards(String query, String type, Boolean starred,
                                                        String folderIds, String tag, Integer limit) {
        Logger.debug(this, "Searching dashboards with query: " + query);

        try {
            return grafanaClient.searchDashboards(query, type, starred, folderIds, tag, limit);
        } catch (Exception e) {
            Logger.error(this, "Error searching dashboards: " + e.getMessage(), e);
            throw new RuntimeException("Failed to search dashboards", e);
        }
    }

    @Override
    public DashboardDetail getDashboardByUid(String uid) {
        Logger.debug(this, "Getting dashboard by UID: " + uid);

        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("Dashboard UID cannot be null or empty");
        }

        try {
            return grafanaClient.getDashboardByUid(uid.trim());
        } catch (Exception e) {
            Logger.error(this, "Error getting dashboard by UID [" + uid + "]: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get dashboard", e);
        }
    }

    @Override
    public List<GrafanaFolder> getFolders(Integer limit) {
        Logger.debug(this, "Getting folders with limit: " + limit);

        try {
            return grafanaClient.getFolders(limit);
        } catch (Exception e) {
            Logger.error(this, "Error getting folders: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get folders", e);
        }
    }

    @Override
    public GrafanaFolder getFolderByUid(String uid) {
        Logger.debug(this, "Getting folder by UID: " + uid);

        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder UID cannot be null or empty");
        }

        try {
            return grafanaClient.getFolderByUid(uid.trim());
        } catch (Exception e) {
            Logger.error(this, "Error getting folder by UID [" + uid + "]: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get folder", e);
        }
    }

    @Override
    public List<DashboardSearchResult> getDashboardsInFolder(String folderUid) {
        Logger.debug(this, "Getting dashboards in folder: " + folderUid);

        if (folderUid == null || folderUid.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder UID cannot be null or empty");
        }

        try {
            return grafanaClient.getDashboardsInFolder(folderUid.trim());
        } catch (Exception e) {
            Logger.error(this, "Error getting dashboards in folder [" + folderUid + "]: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get dashboards in folder", e);
        }
    }

    @Override
    public boolean testConnection() {
        Logger.debug(this, "Testing Grafana connection");

        try {
            // Test connection by trying to get folders with limit 1
            grafanaClient.getFolders(1);
            Logger.info(this, "Grafana connection test successful");
            return true;
        } catch (Exception e) {
            Logger.warn(this, "Grafana connection test failed: " + e.getMessage());
            return false;
        }
    }
}