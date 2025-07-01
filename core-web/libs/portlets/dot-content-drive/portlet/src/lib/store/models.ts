import { Site } from '@dotcms/dotcms-js';
import { DotContentDriveItem } from '@dotcms/dotcms-models';

export enum DotContentDriveStatus {
    LOADING = 'loading',
    LOADED = 'loaded',
    ERROR = 'error'
}

export interface DotContentDriveState {
    currentSite: Site;
    path: string;
    filters: Record<string, string>;
    items: DotContentDriveItem[];
    status: DotContentDriveStatus;
}

export interface DotContentDriveInit {
    currentSite: Site;
    path: string;
    filters: Record<string, string>;
}

export const SYSTEM_HOST: Site = {
    identifier: 'SYSTEM_HOST',
    hostname: 'SYSTEM_HOST',
    type: 'HOST',
    archived: false,
    googleMap: ''
};

// We want to exclude forms and Hosts, and only show contentlets that are not deleted
export const BASE_QUERY = '+systemType:false -contentType:forms -contentType:Host +deleted:false';
