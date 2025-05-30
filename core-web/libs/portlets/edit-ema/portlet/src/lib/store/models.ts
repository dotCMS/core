import { CurrentUser } from '@dotcms/dotcms-js';
import {
    DotCMSWorkflowAction,
    DotExperiment,
    DotLanguage,
    DotPageToolUrlParams
} from '@dotcms/dotcms-models';
import { DotCMSPage, DotCMSPageAsset } from '@dotcms/types';
import { InfoPage } from '@dotcms/ui';

import { UVE_STATUS } from '../shared/enums';
import { DotPageAssetParams, NavigationBarItem } from '../shared/models';

export interface UVEState {
    languages: DotLanguage[];
    isEnterprise: boolean;
    pageAPIResponse?: DotCMSPageAsset;
    pageParams?: DotPageAssetParams;
    currentUser?: CurrentUser;
    experiment?: DotExperiment;
    errorCode?: number;
    viewParams?: DotUveViewParams;
    status: UVE_STATUS;
    isTraditionalPage: boolean;
    canEditPage: boolean;
    pageIsLocked: boolean;
    isClientReady: boolean;
    workflowActions?: DotCMSWorkflowAction[];
}

export interface ShellProps {
    canRead: boolean;
    error: {
        code: number;
        pageInfo: InfoPage;
    };
    items: NavigationBarItem[];
    seoParams: DotPageToolUrlParams;
}

export interface TranslateProps {
    page: DotCMSPage;
    currentLanguage: DotLanguage;
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
