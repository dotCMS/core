import { CurrentUser } from '@dotcms/dotcms-js';
import { DotExperiment, DotLanguage, DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { InfoPage } from '@dotcms/ui';

import { DotPageApiParams, DotPageApiResponse } from '../services/dot-page-api.service';
import { UVE_STATUS } from '../shared/enums';
import { DotPage, NavigationBarItem } from '../shared/models';

export interface UVEState {
    languages: DotLanguage[];
    isEnterprise: boolean;
    pageAPIResponse?: DotPageApiResponse;
    pageParams?: DotPageApiParams;
    currentUser?: CurrentUser;
    experiment?: DotExperiment;
    errorCode?: number;
    viewParams: {
        preview: false;
        orientation: 'landscape';
        device: 'desktop';
    };
    status: UVE_STATUS;
    isTraditionalPage: boolean;
    canEditPage: boolean;
    pageIsLocked: boolean;
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
