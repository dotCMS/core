import { DotCMSBasicContentlet, DotCMSNavigationItem } from '@dotcms/types';

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

export interface BannerWidgetCodeJSON {
  banners: Banner[];
}

export interface BannerCarousel extends Contentlet {
  widgetCodeJSON: BannerWidgetCodeJSON;
}

export interface Destination extends Contentlet {
  title: string;
  identifier: string;
  inode: string;
  urlMap: string;
  modDate: string;
  url: string;
  shortDescription?: string;
  activities?: Activity[];
  selectValue?: string;
}

export interface Product extends Contentlet {
  salePrice: number;
  retailPrice: number;
  urlTitle: string;
  description?: string;
}

export interface Activity extends Contentlet {
  title: string;
  description: string;
  urlTitle: string;
}

export interface VTLInclude extends Contentlet {
  componentType: string | 'destinationListing';
  widgetCodeJSON: unknown;
}

export interface DestinationListingWidgetJSON {
  destinations: Destination[];
}

export interface VTLIncludeDestinationListing extends VTLInclude {
  componentType: 'destinationListing';
  widgetCodeJSON: DestinationListingWidgetJSON;
}

export type VTLIncludeWithVariations =
  | VTLIncludeDestinationListing
  | VTLInclude;

export interface Category {
  title: string;
  url: string;
}

export interface CategoryFilter extends DotCMSBasicContentlet {
  widgetCodeJSON: CategoryFilterWidgetJSON;
}

export interface CategoryFilterWidgetJSON {
  categories: Category[];
}

export interface StoreProductList extends DotCMSBasicContentlet {
  widgetTitle: string;
  widgetCodeJSON: StoreProductListWidgetJSON;
}

export interface StoreProductListWidgetJSON {
  products: Product[];
}

export interface SimpleWidget extends DotCMSBasicContentlet {
  widgetTitle: string;
  identifier: string;
  code?: string;
}

export interface PageForm extends DotCMSBasicContentlet {
  formType: string;
  description?: string;
}

export interface ImageContentlet extends DotCMSBasicContentlet {
    fileAsset: string;
    title: string;
    description: string;
  }

export interface FooterContent {
  logoImage: FileAsset[];
  blogs: Blog[];
  destinations: Destination[];
}

export interface ExtraContent extends FooterContent {
  navigation: DotCMSNavigationItem;
}

export interface Contentlet extends DotCMSBasicContentlet {
  image: ContentletImage;
  urlMap?: string;
  urlTitle?: string;
  widgetTitle?: string;
}
