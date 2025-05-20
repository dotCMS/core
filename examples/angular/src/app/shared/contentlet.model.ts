import { DotCMSBasicContentlet, DotcmsNavigationItem } from '@dotcms/types';

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

export interface Author {
    firstName: string;
    lastName: string;
    inode: string;
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

export interface Banner extends Contentlet {
    title: string;
    caption: string;
    link: string;
    buttonText: string;
}

export interface BannerCarousel extends Contentlet {
    widgetCodeJSON: {
        banners: Banner[];
    };
}

export interface Destination extends Contentlet {
    title: string;
    identifier: string;
    inode: string;
    urlMap: string;
    modDate: string;
    url: string;
}

export interface Product extends Contentlet {
    salePrice: number;
    retailPrice: number;
    urlTitle: string;
}

export interface Activity extends Contentlet {
    title: string;
    description: string;
    urlTitle: string;
}

export interface FooterContent {
    logoImage: FileAsset[];
    blogs: Blog[];
    destinations: Destination[];
}

export interface ExtraContent extends FooterContent {
    navigation: DotcmsNavigationItem;
}
