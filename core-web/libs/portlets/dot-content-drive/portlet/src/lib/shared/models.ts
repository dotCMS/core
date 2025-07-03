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
