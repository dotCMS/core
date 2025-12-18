import type {
  BlockEditorNode,
  DotCMSBasicContentlet,
  DotCMSComposedPageResponse,
  DotCMSNavigationItem,
  DotCMSPageAsset,
  DotCMSURLContentMap,
} from "@dotcms/types";

export type DotCMSCustomPageResponse = DotCMSComposedPageResponse<{
  pageAsset: DotCMSPageAsset;
  content: DotCMSContent;
}>;

export type DotCMSCustomDetailPageResponse = DotCMSComposedPageResponse<{
  pageAsset: DetailPageAsset;
  content: DotCMSContent;
}>;

export interface DetailPageAsset extends DotCMSPageAsset {
  urlContentMap?: URLContentMap;
}

interface URLContentMap extends DotCMSURLContentMap {
  urlMap: string;
  blogContent: BlockEditorNode;
}

interface DotCMSContent {
  navigation: DotCMSNavigationItem;
  blogs: DotCMSBlog[];
  destinations: DotCMSDestination[];
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
