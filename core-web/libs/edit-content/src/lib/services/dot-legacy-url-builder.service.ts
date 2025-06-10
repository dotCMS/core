import { Location } from '@angular/common';
import { Injectable, inject } from '@angular/core';

/**
 * Configuration interface for the legacy editor URL parameters
 */
export interface LegacyEditorUrlParams {
    /** Content type variable/identifier */
    contentTypeVariable: string;
    /** Language ID for the content */
    languageId?: string | number;
    /** Custom referer URL (optional, defaults to current location) */
    referer?: string;
    /** Content inode (optional, defaults to empty for new content) */
    inode?: string;
    /** Command for the portlet (defaults to 'new' for creating content) */
    cmd?: string;
}

/**
 * Legacy editor configuration constants
 *
 * These values are extracted from the JSP logic where:
 * - WindowState.MAXIMIZED.toString() = "maximized"
 * - Struts action for content editing = "/ext/contentlet/edit_contentlet"
 * - Command for new content = "new"
 */
const LEGACY_EDITOR_CONFIG = {
    windowState: 'maximized',
    strutsAction: '/ext/contentlet/edit_contentlet',
    cmd: 'new'
} as const;

/**
 * Service to build URLs for the legacy content editor
 *
 * This service extracts the URL building logic that was previously embedded in JSP files
 * and makes it available to Angular components. It constructs portlet action URLs
 * with the correct parameters to open the legacy content editor.
 *
 * @example
 * ```typescript
 * const url = this.legacyUrlBuilder.buildCreateContentUrl({
 *   contentTypeVariable: 'blog',
 *   languageId: '1'
 * });
 * ```
 */
@Injectable({
    providedIn: 'root'
})
export class DotLegacyUrlBuilderService {
    private readonly location = inject(Location);

    /**
     * Builds a URL to create new content using the legacy content editor
     *
     * This method replicates the JSP logic:
     * ```jsp
     * var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
     * href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
     * href += "<portlet:param name='cmd' value='new' />";
     * href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";
     * href += "<portlet:param name='inode' value='' />";
     * href += "</portlet:actionURL>";
     * href += "&selectedStructure=" + structureInode;
     * href += "&lang=" + getSelectedLanguageId();
     * ```
     *
     * @param params Configuration parameters for the URL
     * @returns Complete URL string for the legacy content editor
     */
    buildCreateContentUrl(params: LegacyEditorUrlParams): string {
        const currentUrl = this.getCurrentUrl();

        // Build the base portlet action URL with parameters
        const baseUrl = new URL(window.location.origin);
        const searchParams = new URLSearchParams();

        // Portlet window state (equivalent to WindowState.MAXIMIZED.toString())
        searchParams.set('p_p_state', LEGACY_EDITOR_CONFIG.windowState);

        // Portlet action parameters
        searchParams.set('struts_action', LEGACY_EDITOR_CONFIG.strutsAction);
        searchParams.set('cmd', params.cmd || LEGACY_EDITOR_CONFIG.cmd);
        searchParams.set('referer', params.referer || currentUrl);
        searchParams.set('inode', params.inode || '');

        // Additional content-specific parameters
        searchParams.set('selectedStructure', params.contentTypeVariable);

        if (params.languageId !== undefined) {
            searchParams.set('lang', params.languageId.toString());
        }

        // Construct the final URL
        baseUrl.search = searchParams.toString();

        return baseUrl.toString();
    }

    /**
     * Builds a URL to edit existing content using the legacy content editor
     *
     * @param params Configuration parameters for editing existing content
     * @returns Complete URL string for editing content in the legacy editor
     */
    buildEditContentUrl(params: LegacyEditorUrlParams & { inode: string }): string {
        return this.buildCreateContentUrl({
            ...params,
            cmd: 'edit' // Override cmd to 'edit' for existing content
        });
    }

    /**
     * Gets the current browser URL for use as a referer
     *
     * @private
     * @returns Current page URL as a string
     */
    private getCurrentUrl(): string {
        return `${window.location.origin}${this.location.path()}`;
    }
}
