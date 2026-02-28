import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotWorkflowActionsFireService,
    DotTagsService,
    DotContentletService,
    DotSiteService
} from '@dotcms/data-access';
import {
    DotCMSContentType,
    DotCMSContentlet,
    DotCMSContentletVersion,
    DotContentletDepth,
    DotCMSResponse,
    PaginationParams,
    CustomTreeNode,
    DotFolder,
    TreeNodeItem,
    ContentByFolderParams,
    DotCMSAPIResponse
} from '@dotcms/dotcms-models';
import { DotBrowsingService } from '@dotcms/ui';

import { Activity, DotPushPublishHistoryItem } from '../models/dot-edit-content.model';

@Injectable()
export class DotEditContentService {
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotContentletService = inject(DotContentletService);
    readonly #dotBrowsingService = inject(DotBrowsingService);
    readonly #dotTagsService = inject(DotTagsService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #siteService = inject(DotSiteService);
    readonly #http = inject(HttpClient);

    /**
     * Retrieves the content by its ID.
     *
     * @param {string} id - The ID of the content to retrieve.
     * @param {number} [languageId] - Optional language ID to filter the content.
     * @param {DotContentletDepth} [depth] - Optional depth to filter the content.
     * @returns {Observable<DotCMSContentlet>} An observable of the DotCMSContentlet object.
     */
    getContentById(params: {
        id: string;
        languageId?: number;
        depth?: DotContentletDepth;
    }): Observable<DotCMSContentlet> {
        const { id, languageId, depth } = params;
        let httpParams = new HttpParams();

        if (languageId) {
            httpParams = httpParams.set('language', languageId.toString());
        }

        if (depth) {
            httpParams = httpParams.set('depth', depth);
        }

        return this.#dotContentletService
            .getContentletByInode(id, httpParams)
            .pipe(map((contentlet) => contentlet));
    }

    /**
     * Retrieves the content type by its ID or variable name.
     *
     * @param {string} idOrVar - The identifier or variable name of the content type to retrieve form data for.
     * @return {*}  {Observable<DotCMSContentType>}
     * @memberof DotEditContentService
     */
    getContentType(idOrVar: string): Observable<DotCMSContentType> {
        return this.#dotContentTypeService.getContentType(idOrVar);
    }

    /**
     * Retrieves tags based on the provided name.
     * @param name - The name of the tags to retrieve.
     * @returns An Observable that emits an array of tag labels.
     */
    getTags(name: string): Observable<string[]> {
        return this.#dotTagsService.getTags(name).pipe(map((tags) => tags.map((tag) => tag.label)));
    }
    /**
     * Saves a contentlet with the provided data.
     * @param data An object containing key-value pairs of data to be saved.
     * @returns An observable that emits the saved contentlet.
     * The type of the emitted contentlet is determined by the generic type parameter.
     */
    saveContentlet<T>(data: { [key: string]: string }): Observable<T> {
        return this.#dotWorkflowActionsFireService.saveContentlet(data);
    }

    /**
     * Retrieves and transforms site data into TreeNode format for the site/folder field.
     * Optionally filters out the System Host based on the isRequired parameter.
     *
     * @param {Object} data - The parameters for fetching sites
     * @param {string} data.filter - Filter string to search sites
     * @param {number} [data.perPage] - Number of items per page
     * @param {number} [data.page] - Page number to fetch
     * @param {boolean} data.isRequired - If true, excludes System Host from results
     * @returns {Observable<TreeNodeItem[]>} Observable that emits an array of TreeNodeItems
     */
    getSitesTreePath(data: {
        filter: string;
        perPage?: number;
        page?: number;
    }): Observable<TreeNodeItem[]> {
        const { filter, perPage, page } = data;

        return this.#siteService.getSites({ filter, per_page: perPage, page }).pipe(
            map(({ sites }) =>
                sites.map((site) => ({
                    key: site.identifier,
                    label: site.hostname,
                    data: {
                        id: site.identifier,
                        hostname: site.hostname,
                        path: '',
                        type: 'site'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                }))
            )
        );
    }

    /**
     *
     *
     * @param {string} path
     * @return {*}  {Observable<DotFolder[]>}
     * @memberof DotEditContentService
     */
    getFolders(path: string): Observable<DotFolder[]> {
        return this.#dotBrowsingService.getFolders(path);
    }

    /**
     * Retrieves folders and transforms them into a tree node structure.
     * The first folder in the response is considered the parent folder.
     *
     * @param {string} path - The path to fetch folders from
     * @returns {Observable<{ parent: DotFolder; folders: TreeNodeItem[] }>} Observable that emits an object containing the parent folder and child folders as TreeNodeItems
     */
    getFoldersTreeNode(path: string): Observable<{ parent: DotFolder; folders: TreeNodeItem[] }> {
        return this.#dotBrowsingService.getFoldersTreeNode(path);
    }

    /**
     * Builds a hierarchical tree structure based on the provided path.
     * Splits the path into segments and creates a nested tree structure
     * by making multiple API calls for each path segment.
     *
     * @param {string} path - The full path to build the tree from (e.g., 'hostname/folder1/folder2')
     * @returns {Observable<CustomTreeNode>} Observable that emits a CustomTreeNode containing the complete tree structure and the target node
     */
    buildTreeByPaths(path: string): Observable<CustomTreeNode> {
        return this.#dotBrowsingService.buildTreeByPaths(path);
    }

    /**
     * Retrieves the current site and transforms it into a TreeNodeItem format.
     * Useful for initializing the site/folder field with the current context.
     *
     * @returns {Observable<TreeNodeItem>} Observable that emits the current site as a TreeNodeItem
     */
    getCurrentSiteAsTreeNodeItem(): Observable<TreeNodeItem> {
        return this.#dotBrowsingService.getCurrentSiteAsTreeNodeItem();
    }

    /**
     * Get the number of reference pages for a contentlet
     * @param identifier - The identifier of the contentlet
     * @returns An observable that emits the number of reference pages
     */
    getReferencePages(identifier: string): Observable<number> {
        return this.#http
            .get<{ entity: { count: number } }>(`/api/v1/content/${identifier}/references/count`)
            .pipe(map((response) => response.entity.count));
    }

    /**
     * Get content by folder
     *
     * @param {{ folderId: string; mimeTypes?: string[] }} { folderId, mimeTypes }
     * @return {*}
     * @memberof DotEditContentService
     */
    getContentByFolder(params: ContentByFolderParams) {
        return this.#dotBrowsingService.getContentByFolder(params);
    }

    /**
     * Get activities (comments) for a content
     * @param identifier Content identifier
     * @returns Observable of activities
     */
    getActivities(identifier: string): Observable<Activity[]> {
        return this.#http
            .get<{ entity: Activity[] }>(`/api/v1/workflow/tasks/history/comments/${identifier}`)
            .pipe(pluck('entity'));
    }

    /**
     * Create a new activity (comment) for a content
     * @param identifier Content identifier
     * @param comment Comment text
     * @returns Observable of the created activity
     */
    createActivity(identifier: string, comment: string): Observable<Activity> {
        return this.#http
            .post<
                DotCMSAPIResponse<Activity>
            >(`/api/v1/workflow/${identifier}/comments`, { comment })
            .pipe(map((response) => response.entity));
    }

    /**
     * Creates HTTP parameters for pagination requests.
     * Handles the logic for setting offset and limit parameters with proper defaults.
     *
     * @private
     * @param {PaginationParams} [paginationParams] - Optional pagination parameters
     * @returns {HttpParams} Configured HTTP parameters
     */
    private buildPaginationParams(paginationParams?: PaginationParams): HttpParams {
        let httpParams = new HttpParams();

        if (paginationParams?.limit) {
            const offset = paginationParams.offset || 1;
            httpParams = httpParams
                .set('offset', offset.toString())
                .set('limit', paginationParams.limit.toString());
        }

        return httpParams;
    }

    /**
     * Retrieves the version history for a content item by its identifier.
     * Returns all versions of the content including live, working, and archived versions.
     *
     * @param {string} identifier - The unique identifier of the content item
     * @param {PaginationParams} [paginationParams] - Optional pagination parameters (offset-based)
     * @param {number} [languageId] - Optional language ID to filter versions for a specific language
     * @returns {Observable<DotCMSResponse<DotCMSContentletVersion[]>>} Observable that emits DotCMS response with contentlet version history
     */
    getVersions(
        identifier: string,
        paginationParams?: PaginationParams,
        languageId?: number
    ): Observable<DotCMSResponse<DotCMSContentletVersion[]>> {
        let httpParams = this.buildPaginationParams(paginationParams);

        if (languageId) {
            httpParams = httpParams.set('languageId', languageId.toString());
        }

        return this.#http.get<DotCMSResponse<DotCMSContentletVersion[]>>(
            `/api/v1/content/versions/id/${identifier}/history`,
            { params: httpParams }
        );
    }

    /**
     * Retrieves the push publish history for a content item by its identifier.
     * Returns all push publish operations for the content.
     *
     * @param {string} identifier - The unique identifier of the content item
     * @param {PaginationParams} [paginationParams] - Optional pagination parameters (offset-based)
     * @returns {Observable<DotCMSResponse<DotPushPublishHistoryItem[]>>} Observable that emits DotCMS response with push publish history
     */
    getPushPublishHistory(
        identifier: string,
        paginationParams?: PaginationParams
    ): Observable<DotCMSResponse<DotPushPublishHistoryItem[]>> {
        const httpParams = this.buildPaginationParams(paginationParams);

        return this.#http.get<DotCMSResponse<DotPushPublishHistoryItem[]>>(
            `/api/v1/content/${identifier}/push/history`,
            { params: httpParams }
        );
    }

    /**
     * Deletes all push publish history for a content item by its identifier.
     * Calls the /api/bundle/deletepushhistory/assetid/{identifier} endpoint.
     *
     * @param {string} identifier - The unique identifier of the content item
     * @returns {Observable<any>} Observable that emits the deletion response
     */
    deletePushPublishHistory(identifier: string): Observable<unknown> {
        return this.#http.get(`/api/bundle/deletepushhistory/assetid/${identifier}`);
    }
}
