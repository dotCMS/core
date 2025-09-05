import { DotCMSGraphQLParams } from '@dotcms/types';

export const blogQuery = (limit: number) => {
  return `
    search(query: "+contenttype:Blog +live:true", limit: ${limit}) {
        title
        identifier
        ... on Blog {
            inode
            image {
                fileName
                fileAsset {
                    versionPath
                }
            }
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
};

export const destinationQuery = (limit: number) => {
  return `
    search(query: "+contenttype:Destination +live:true", limit: ${limit}) {
          title
          identifier
          ... on Destination {
                  inode
                  image {
                  fileName
                  fileAsset {
                      versionPath
                  }
                  }
                  urlMap
                  modDate
                  url
          }
      }
  `;
};

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

export const logoQuery = `
    FileAssetCollection(query: "+title:logo.png") {
        fileAsset {
            versionPath
        }
    }
`;

interface BaseExtraQueriesParams {
  limitBlogs?: number;
  limitDestinations?: number;
}

export const buildExtraQuery = (
  params?: BaseExtraQueriesParams,
): DotCMSGraphQLParams => {
  const { limitBlogs = 3, limitDestinations = 3 } = params || {};

  return {
    content: {
      logoImage: logoQuery,
      blogs: blogQuery(limitBlogs),
      destinations: destinationQuery(limitDestinations),
      navigation: navigationQuery,
    },
    fragments: [fragmentNav],
  };
};
