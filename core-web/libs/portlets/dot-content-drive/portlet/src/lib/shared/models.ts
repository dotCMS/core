import { DotContentDriveItem, SiteEntity } from '@dotcms/dotcms-models';

/**
 * The status of the content drive.
 *
 * @export
 * @enum {string}
 */
export enum DotContentDriveStatus {
    LOADING = 'loading',
    LOADED = 'loaded',
    ERROR = 'error'
}

/**
 * The sort order of the content drive.
 *
 * @export
 * @enum {string}
 */
export enum DotContentDriveSortOrder {
    ASC = 'asc',
    DESC = 'desc'
}

/**
 * The pagination of the content drive.
 *
 * @export
 * @interface DotContentDrivePagination
 */
export interface DotContentDrivePagination {
    limit: number;
    offset: number;
}

/**
 * The sort of the content drive.
 *
 * @export
 * @interface DotContentDriveSort
 */
export interface DotContentDriveSort {
    field: string;
    order: DotContentDriveSortOrder;
}

/**
 * The init of the content drive.
 *
 * @export
 * @interface DotContentDriveInit
 */
export interface DotContentDriveInit {
    currentSite: SiteEntity;
    path: string;
    filters: DotContentDriveFilters;
    isTreeExpanded: boolean;
}

/**
 * The state of the content drive.
 *
 * @export
 * @interface DotContentDriveState
 */
export interface DotContentDriveState extends DotContentDriveInit {
    items: DotContentDriveItem[];
    status: DotContentDriveStatus;
    totalItems: number;
    pagination: DotContentDrivePagination;
    sort: DotContentDriveSort;
}

/**
 * The known filters of the content drive.
 *
 * @export
 * @interface DotKnownContentDriveFilters
 */
export type DotKnownContentDriveFilters = {
    baseType: string[];
    contentType: string[];
    title: string;
};

/**
 * The filters of the content drive.
 *
 * @export
 * @interface DotContentDriveFilters
 */
export type DotContentDriveFilters = Partial<DotKnownContentDriveFilters> & {
    [key: string]: string | string[];
};

/**
 * The decode function of the content drive.
 *
 * @export
 * @interface DotContentDriveDecodeFunction
 */
export type DotContentDriveDecodeFunction = (value: string) => string | string[];

/**
 * The base types of the content drive.
 *
 * @export
 * @constant {Record<string, string>} BASE_TYPES
 */
export const BASE_TYPES = {
    content: 'CONTENT',
    widget: 'WIDGET',
    fileAsset: 'FILEASSET',
    htmlPage: 'HTMLPAGE',
    keyValue: 'KEY_VALUE',
    vanityURL: 'VANITY_URL',
    dotAsset: 'DOTASSET',
    form: 'FORM',
    persona: 'PERSONA'
} as const;
