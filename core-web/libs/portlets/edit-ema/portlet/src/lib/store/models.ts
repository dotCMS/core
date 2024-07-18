import { CurrentUser } from '@dotcms/dotcms-js';
import { DotDevice, DotExperiment, DotLanguage, DotPageToolUrlParams } from '@dotcms/dotcms-models';
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
    error?: number;
    params?: DotPageApiParams;
    status: UVE_STATUS;
}

export interface ShellState {
    canRead: boolean;
    error: number;
    items: NavigationBarItem[];
    translateProps: TranslateProps;
    seoParams: DotPageToolUrlParams;
    uvePageInfo: Record<'NOT_FOUND' | 'ACCESS_DENIED', InfoPage>;
}

export interface TranslateProps {
    page: DotPage;
    languageId: number;
    languages: DotLanguage[];
}

export interface EditorToolbarState {
    device?: DotDevice;
    socialMedia?: string;
}
