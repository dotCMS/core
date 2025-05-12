import { DotCMSBasicContentlet } from '@dotcms/types';

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
