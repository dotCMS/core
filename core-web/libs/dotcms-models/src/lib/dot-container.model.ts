import { DotCMSContentlet } from './dot-contentlet.model';

export enum CONTAINER_SOURCE {
    FILE = 'FILE',
    DB = 'DB'
}

export interface DotContainerEntity {
    container: DotContainer;
    contentTypes: DotContainerStructure[];
}

export interface DotContainer {
    archived?: boolean;
    hostName?: string;
    hostId?: string;
    categoryId?: string;
    deleted?: boolean;
    live?: boolean;
    working?: boolean;
    locked?: boolean;
    friendlyName?: string;
    path?: string;
    identifier?: string;
    name?: string;
    type?: string;
    title?: string;
    source?: CONTAINER_SOURCE;
    maxContentlets?: number;
    preLoop?: string;
    postLoop?: string;
    code?: string;
    disableInteraction?: boolean;
    pathName?: string;
}

export interface DotContainerPayload {
    identifier: string;
    title: string;
    friendlyName: string;
    maxContentlets: number;
    notes: string;
    code: string;
    preLoop: string;
    postLoop: string;
    containerStructures: DotContainerStructure[];
}

export interface DotContainerStructure {
    structureId?: string;
    code?: string;
    containerId?: string;
    containerInode?: string;
}

export interface DotContainerStructure {
    contentTypeVar: string;
}

// The template endpoint returns DotContainer but the page endpoint returns {container}
export interface DotContainerMap {
    [key: string]: DotContainer;
}

export interface DotPageContainerStructure {
    [key: string]: {
        container: DotContainer;
        containerStructures?: DotContainerStructure[];
        contentlets?: { [key: string]: DotCMSContentlet[] };
    };
}

/**
 * A template that uses a container, as returned by `GET /api/v1/containers/dependencies`.
 * Values are returned as saved and must be rendered as text.
 */
export interface DotContainerTemplateDependency {
    inode: string;
    identifier: string;
    title: string;
    /** Host name of the template's site; null when the site is not readable for the user. */
    siteName: string | null;
}

/**
 * A container and the templates that use it, as returned by
 * `GET /api/v1/containers/dependencies`. `templates` is empty when nothing uses the container.
 */
export interface DotContainerDependencies {
    inode: string;
    identifier: string;
    title: string;
    templates: DotContainerTemplateDependency[];
}
