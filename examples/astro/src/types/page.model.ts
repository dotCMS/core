import type { DotCMSBasicContentlet, DotCMSComposedPageResponse, DotCMSNavigationItem, DotCMSPageAsset, DotCMSURLContentMap } from "@dotcms/types";

export type DotCMSCustomPageResponse = DotCMSComposedPageResponse< {
    pageAsset: CustomDotCMSPageAsset;
    content: {
        navigation: DotCMSNavigationItem;
        blogs: DotCMSBlog[];
        destinations: DotCMSDestination[];
    };
}
>

export interface CustomDotCMSPageAsset extends DotCMSPageAsset {
    urlContentMap?: URLContentMap;
}

interface URLContentMap extends DotCMSURLContentMap {
    urlMap: string;
    blogContent: string;
}

interface DotCMSBlog extends DotCMSBasicContentlet {
    image: {
        fileName: string;
    };
    urlMap: string;
    urlTitle: string;
    teaser: string;
    author: {
        firstName: string;
        lastName: string;
        inode: string;
    };
}

interface DotCMSDestination extends DotCMSBasicContentlet {
    image: {
        fileName: string;
    };
    urlMap: string;
    modDate: string;
    url: string;
}
