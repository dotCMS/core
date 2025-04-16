import { CurrentUser } from '@dotcms/dotcms-js';
import {
    DotCMSWorkflowAction,
    DotExperiment,
    DotLanguage,
    DotPageToolUrlParams
} from '@dotcms/dotcms-models';
import { InfoPage } from '@dotcms/ui';

import { DotPageApiResponse, UVEPageParams } from '../services/dot-page-api.service';
import { UVE_STATUS } from '../shared/enums';
import { DotPage, NavigationBarItem } from '../shared/models';

export interface UVEState {
    languages: DotLanguage[];
    isEnterprise: boolean;
    pageAPIResponse?: DotPageApiResponse;
    pageParams?: UVEPageParams;
    currentUser?: CurrentUser;
    experiment?: DotExperiment;
    errorCode?: number;
    viewParams?: DotUveViewParams;
    status: UVE_STATUS;
    isTraditionalPage: boolean;
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
    page: DotPage;
    currentLanguage: DotLanguage;
}

export interface DotUveViewParams {
    orientation: Orientation;
    device: string;
    seo: string;
    experimentId?: string;
}

export enum Orientation {
    LANDSCAPE = 'landscape',
    PORTRAIT = 'portrait'
}
