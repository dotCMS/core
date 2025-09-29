import { CurrentUser } from '@dotcms/dotcms-js';
import { DotExperiment, DotLanguage } from '@dotcms/dotcms-models';
import { DotCMSPage, DotCMSPageAsset, UVE_MODE } from '@dotcms/types';

export interface UVEState {
    url: string;
    pageLanguages: DotLanguage[];
    isEnterprise: boolean;
    editorStatus: UVE_STATUS;
    pageAssetData: DotCMSPageAsset | null;
    currentUser: CurrentUser | null;
    experiment?: DotExperiment;
    configuration: UVEConfiguration;
}

export interface UVEConfiguration {
    mode: UVE_MODE;
    language_id: string;
    publishDate: string;
    device: string;
    [PERSONA_KEY]: string;
}

export enum UVE_STATUS {
    LOADING = 'loading',
    LOADED = 'loaded',
    ERROR = 'error'
}

export interface DotUveViewParams {
    orientation: Orientation;
    device: string;
    seo: string;
}

export enum Orientation {
    LANDSCAPE = 'landscape',
    PORTRAIT = 'portrait'
}

/**
 * Persona key
 *
 * @type {string}
 */
export const PERSONA_KEY = 'com.dotmarketing.persona.id';

/**
 * Check if the page is locked
 *
 * @export
 * @param {DotPage} page
 * @param {CurrentUser} currentUser
 * @return {*}
 */
export function isLockedByAnotherUser(page?: DotCMSPage, userId?: string): boolean {
    if (!page || !userId) {
        return false;
    }

    return !!page?.locked && page?.lockedBy !== userId;
}
