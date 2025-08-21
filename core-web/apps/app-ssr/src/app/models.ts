import { DotCMSBasicContentlet } from '@dotcms/types';

export interface FileAsset {
    fileAsset: {
        versionPath: string;
    };
}

export interface ContentletImage extends FileAsset {
    identifier: string;
    fileName: string;
    versionPath?: string;
}

export interface Contentlet extends DotCMSBasicContentlet {
    image: ContentletImage;
    urlMap?: string;
    urlTitle?: string;
    widgetTitle?: string;
}

export interface Contentlet extends DotCMSBasicContentlet {
    image: ContentletImage;
    urlMap?: string;
    urlTitle?: string;
    widgetTitle?: string;
}

export interface Blog extends Contentlet {
    title: string;
    identifier: string;
    inode: string;
    modDate: string;
    urlTitle: string;
    teaser: string;
    author: Author;
}

export interface Author {
    firstName: string;
    lastName: string;
    inode: string;
}
