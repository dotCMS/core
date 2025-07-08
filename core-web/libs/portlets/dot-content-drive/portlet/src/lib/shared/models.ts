import { Site } from '@dotcms/dotcms-js';
import { DotContentDriveItem } from '@dotcms/dotcms-models';

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

export interface DotContentDriveState {
    currentSite: Site;
    path: string;
    filters: Record<string, string>;
    items: DotContentDriveItem[];
    status: DotContentDriveStatus;
    totalItems: number;
    pagination: DotContentDrivePagination;
    sort: DotContentDriveSort;
}

export interface DotContentDriveInit {
    currentSite: Site;
    path: string;
    filters: Record<string, string>;
}
