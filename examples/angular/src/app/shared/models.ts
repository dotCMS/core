import { DotCMSComposedPageResponse, DotcmsNavigationItem, DotCMSPageAsset } from '@dotcms/types';

export type PageError = {
    message: string;
    status: number | string;
};

export type ComposedPageResponse<
    TPage extends DotCMSPageAsset = DotCMSPageAsset,
    TContent = unknown
> = DotCMSComposedPageResponse<{
    pageAsset: TPage;
    content: TContent;
}>;

export type PageRender<TPage extends DotCMSPageAsset = DotCMSPageAsset, TContent = unknown> = {
    pageResponse?: ComposedPageResponse<TPage, TContent> | null;
    nav?: DotcmsNavigationItem;
    error?: PageError;
    status: 'idle' | 'success' | 'error' | 'loading';
};
