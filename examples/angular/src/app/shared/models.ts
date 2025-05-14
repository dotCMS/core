import {
    DotCMSBasicContentlet,
    DotCMSComposedPageResponse,
    DotcmsNavigationItem,
    DotCMSPageAsset
} from '@dotcms/types';

export interface LogoImage {
    fileAsset: {
        versionPath: string;
    };
}

export interface ContentletImage {
    identifier: string;
}

export interface Contentlet extends DotCMSBasicContentlet {
    image: ContentletImage;
    urlMap?: string;
    urlTitle?: string;
    widgetTitle?: string;
}

export interface Blog {
    _map: Contentlet;
}

export interface Destination {
    _map: Contentlet;
}
export interface FooterContent {
    logoImage: LogoImage[];
    blogs: Blog[];
    destinations: Destination[];
}

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
