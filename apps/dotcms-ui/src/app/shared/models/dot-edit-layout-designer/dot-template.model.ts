import { DotContainerMap } from '@shared/models/container/dot-container.model';
import { DotLayout } from './dot-layout.model';

export interface DotTemplate {
    anonymous: boolean;
    archived?: boolean;
    body?: string;
    categoryId?: string;
    countAddContainer?: number;
    countContainers?: number;
    deleted?: boolean;
    drawed?: boolean;
    drawedBody?: string;
    friendlyName: string;
    hasLiveVersion: boolean;
    iDate?: Date;
    identifier: string;
    image?: string;
    inode: string;
    live?: boolean;
    locked?: boolean;
    modDate?: Date;
    name: string;
    permissionId?: string;
    permissionType?: string;
    showOnMenu?: boolean;
    sortOrder?: number;
    theme?: string;
    title?: string;
    type: string;
    versionType: string;
    working: boolean;
}

// Extra properties from renderHTML
export interface DotTemplate {
    canEdit: boolean;
    canWrite?: boolean;
    canPublish?: boolean;
    containers?: DotContainerMap;
    layout: DotLayout;
    selectedimage?: string;
}
