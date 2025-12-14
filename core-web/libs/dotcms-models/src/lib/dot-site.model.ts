import { DotCMSContentType } from './dot-content-types.model';

/**
 * @deprecated Use SiteEntity instead
 */
export interface Site {
    archived?: string;
    categoryId?: string;
    contentType: DotCMSContentType;
    contentTypeId?: string;
    host?: string;
    hostname: string;
    identifier: string;
    inode?: string;
    keyValue?: boolean;
    locked?: boolean;
    modDate?: Date;
    name: string;
    owner?: string;
    permissionId: string;
    permissionType?: string;
    sortOrder?: number;
    tagStorage?: string;
    title?: string;
    type: string;
    vanityUrl?: boolean;
    versionId?: string;
    versionType?: string;
}


/**
 * The primary (and only) site model to be used in components and stores.
 *
 * This interface defines the minimal, normalized site entity used across all
 * UI components, data-access services, and global store state. It consolidates the
 * fields needed for displaying, selecting, and working with sites in the application.
 *
 * Do NOT use the old `Site` or `SiteEntity` types in component code—this is the one
 * to use everywhere in the app. All backend responses/DTOs should be mapped to this model.
 *
 * - `archived`: Whether the site is archived.
 * - `identifier`: The unique site identifier (primary key).
 * - `hostname`: The main hostname for the site (used for display/select).
 * - `aliases`: Any alias hostnames, or null if none.
 */
export interface DotSite {
    archived?: boolean;
    identifier: string;
    hostname: string;
    aliases: string | null;
}
