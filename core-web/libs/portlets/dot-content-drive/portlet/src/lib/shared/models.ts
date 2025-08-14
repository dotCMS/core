import { DotContentDriveItem, SiteEntity } from '@dotcms/dotcms-models';

export enum DotContentDriveStatus {
    LOADING = 'loading',
    LOADED = 'loaded',
    ERROR = 'error'
}

export enum DotContentDriveSortOrder {
    ASC = 'asc',
    DESC = 'desc'
}

export interface DotContentDrivePagination {
    limit: number;
    offset: number;
}

export interface DotContentDriveSort {
    field: string;
    order: DotContentDriveSortOrder;
}

export interface DotContentDriveInit {
    currentSite: SiteEntity;
    path: string;
    filters: DotContentDriveFilters;
    isTreeExpanded: boolean;
}
export interface DotContentDriveState extends DotContentDriveInit {
    items: DotContentDriveItem[];
    status: DotContentDriveStatus;
    totalItems: number;
    pagination: DotContentDrivePagination;
    sort: DotContentDriveSort;
}

export type DotContentDriveFilters = Record<string, string | string[]>;
