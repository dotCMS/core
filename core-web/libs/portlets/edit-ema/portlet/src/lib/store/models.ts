import { CurrentUser } from '@dotcms/dotcms-js';
import { DotExperiment, DotLanguage, DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { InfoPage } from '@dotcms/ui';

import { DotPageApiParams, DotPageApiResponse } from '../services/dot-page-api.service';
import { UVE_STATUS } from '../shared/enums';
import { DotPage, NavigationBarItem } from '../shared/models';

export interface UVEState {
    isEnterprise: boolean;
    pageAPIResponse?: DotPageApiResponse;
    languages: DotLanguage[];
    currentUser?: CurrentUser;
    experiment?: DotExperiment;
    errorCode?: number;
    params?: DotPageApiParams;
    status: UVE_STATUS;
    isTraditionalPage: boolean;
    canEditPage: boolean;
    pageIsLocked: boolean;
    graphQL?: string;
    isClientReady?: boolean;
}

export interface ShellProps {
    canRead: boolean;
    error: {
        code: number;
        pageInfo: InfoPage;
    };
    items: NavigationBarItem[];
    translateProps: TranslateProps;
    seoParams: DotPageToolUrlParams;
}

export interface TranslateProps {
    page: DotPage;
    languageId: number;
    languages: DotLanguage[];
}
