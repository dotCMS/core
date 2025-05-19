import {
    DotCMSComposedPageResponse,
    DotcmsNavigationItem,
    DotCMSExtendedPageResponse
} from '@dotcms/types';

export type PageError = {
    message: string;
    status: number | string;
};

export type ComposedPageResponse<T extends DotCMSExtendedPageResponse> =
    DotCMSComposedPageResponse<T>;

export type PageRender<T extends DotCMSExtendedPageResponse> = {
    pageResponse?: ComposedPageResponse<T> | null;
    nav?: DotcmsNavigationItem;
    error?: PageError;
    status: 'idle' | 'success' | 'error' | 'loading';
};
