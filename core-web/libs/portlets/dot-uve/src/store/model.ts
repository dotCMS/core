import { CurrentUser } from '@dotcms/dotcms-js';
import { DotCMSWorkflowAction, DotExperiment, DotLanguage } from '@dotcms/dotcms-models';
import { DotCMSPageAsset, UVE_MODE } from '@dotcms/types';

export interface UVEState {
    languages: DotLanguage[];
    isEnterprise: boolean;
    editorStatus: UVE_STATUS;
    pageAssetData?: DotCMSPageAsset;
    currentUser?: CurrentUser;
    experiment?: DotExperiment;
    workflowActions?: DotCMSWorkflowAction[];
    configuration: UVEConfiguration;
}

export interface UVEConfiguration {
    url: string;
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
