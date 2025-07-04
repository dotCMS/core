import { Site } from '@dotcms/dotcms-js';
import { DotContentDriveItem } from '@dotcms/dotcms-models';

export enum DotContentDriveStatus {
    LOADING = 'loading',
    LOADED = 'loaded',
    ERROR = 'error'
}

export interface DotContentDrivePagination {
    limit: number;
    offset: number;
}

export interface DotContentDriveState {
    currentSite: Site;
    path: string;
    filters: Record<string, string>;
    items: DotContentDriveItem[];
    status: DotContentDriveStatus;
    totalItems: number;
    pagination: DotContentDrivePagination;
}

export interface DotContentDriveInit {
    currentSite: Site;
    path: string;
    filters: Record<string, string>;
}
