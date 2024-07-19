import { CurrentUser } from '@dotcms/dotcms-js';
import { DotExperiment, DotLanguage, DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { InfoPage } from '@dotcms/ui';

import {
    Container,
    ContentletArea,
    EmaDragItem
} from '../edit-ema-editor/components/ema-page-dropzone/types';
import { DotPageApiParams, DotPageApiResponse } from '../services/dot-page-api.service';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';
import { DotDeviceWithIcon, DotPage, NavigationBarItem } from '../shared/models';

export interface UVEState {
    isEnterprise: boolean;
    pageAPIResponse?: DotPageApiResponse;
    languages: DotLanguage[];
    currentUser?: CurrentUser;
    experiment?: DotExperiment;
    error?: number;
    params?: DotPageApiParams;
    status: UVE_STATUS;
    isLegacyPage: boolean;
    canEditPage: boolean;
    pageIsLocked: boolean;
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
    device?: DotDeviceWithIcon;
    socialMedia?: string;
    isEditState: boolean;
}

export interface EditorState {
    bounds: Container[];
    state: EDITOR_STATE;
    contentletArea?: ContentletArea;
    dragItem?: EmaDragItem;
}
