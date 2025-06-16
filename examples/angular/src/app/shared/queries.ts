import { DotCMSGraphQLParams, DotCMSPageRequestParams } from '@dotcms/types';

export const blogQuery = `
    search(query: "+contenttype:Blog +live:true", limit: 3) {
        title
        identifier
        ... on Blog {
            inode
            urlMap
            modDate
            urlTitle
            teaser
            author {
                firstName
                lastName
                inode
            }
        }
    }
`;

export const destinationQuery = `
    search(query: "+contenttype:Destination +live:true", limit: 3) {
        title
        identifier
        ... on Destination {
            inode
            urlMap
            modDate
            url
        }
    }
`;

export const navigationQuery = `
    DotNavigation(uri: "/", depth: 2) {
        ...NavProps
        children {
            ...NavProps
        }
    }
`;

export const fragmentNav = `
fragment NavProps on DotNavigation {
    code
    folder
    hash
    host
    href
    languageId
    order
    target
    title
    type
}
`;

export const BASE_EXTRA_QUERIES: DotCMSGraphQLParams = {
    content: {
        logoImage: `FileAssetCollection(query: "+title:logo.png") {
  fileAsset {
    versionPath
  }
}`,
        blogs: blogQuery,
        destinations: destinationQuery,
        navigation: navigationQuery
    },
    fragments: [fragmentNav]
};
