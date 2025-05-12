import { DotCMSPageRequestParams } from '@dotcms/types';

export const BASE_EXTRA_QUERIES: DotCMSPageRequestParams['graphql'] = {
    content: {
        logoImage: `FileAssetCollection(query: "+title:logo.png") {
  fileAsset {
    versionPath
  }
}`,
        blogs: `BlogCollection(limit: 3, sortBy: "modDate desc") {
_map
}`,
        destinations: `DestinationCollection(limit: 3, sortBy: "modDate desc") {
_map
}`
    }
};
