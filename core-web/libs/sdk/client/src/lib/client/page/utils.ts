import { RequestOptions } from '../client';

export const buildPageQuery = ({
    pageFragment,
    contentQueries,
    navQueries
}: {
    pageFragment: string;
    contentQueries: string;
    navQueries: string;
}) => `
  fragment DotCMSPage on DotPage {
    publishDate
    type
    httpsRequired
    inode
    path
    identifier
    hasTitleImage
    sortOrder
    extension
    canRead
    pageURI
    canEdit
    archived
    friendlyName
    workingInode
    url
    hasLiveVersion
    deleted
    pageUrl
    shortyWorking
    mimeType
    locked
    stInode
    contentType
    creationDate
    liveInode
    name
    shortyLive
    modDate
    title
    baseType
    working
    canLock
    live
    isContentlet
    statusIcons
    canEdit
    canLock
    canRead
    canEdit
    canLock
    canRead
    conLanguage {
      id
      language
      languageCode
    }
    template {
      drawed
    }
    containers {
      path
      identifier
      maxContentlets
      containerStructures {
        contentTypeVar
      }
      containerContentlets {
        uuid
        contentlets {
          publishDate
          inode
          identifier
          archived
          urlMap
          urlMap
          locked
          contentType
          creationDate
          modDate
          title
          baseType
          working
          live
          publishUser {
            firstName
            lastName
          }
          owner {
            lastName
          }
          conLanguage {
            language
            languageCode
          }
          modUser {
            firstName
            lastName
          }
        }
      }
    }
    layout {
      header
      footer
      body {
        rows {
          columns {
            leftOffset
            styleClass
            width
            left
            containers {
              identifier
              uuid
            }
          }
        }
      }
    }
    viewAs {
      visitor {
        persona {
          name
        }
      }
      language {
        id
        languageCode
        countryCode
        language
        country
      }
    }
  }

  ${pageFragment ? ` fragment ClientPage on DotPage { ${pageFragment} } ` : ''}

  query PageContent($url: String!, $languageId: String, $mode: String) {
    page: page(url: $url, languageId: $languageId, pageMode: $mode) {
      ...DotCMSPage
      ${pageFragment ? '...ClientPage' : ''}
    }

    ${contentQueries}

    ${navQueries}
  }
`;

export function buildQueries(data: Record<string, string>) {
    return Object.entries(data || {})
        .map(([key, query]) => `${key}: ${query}`)
        .join('');
}

export function mapResponseData(data: Record<string, string>, keys: string[]) {
    return keys.reduce(
        (acc, key) => {
            acc[key] = data[key];

            return acc;
        },
        {} as Record<string, string>
    );
}

export async function fetchGraphQL({
    body,
    requestOptions
}: {
    body: string;
    requestOptions: RequestOptions;
}) {
    const response = await fetch('http://localhost:8080/api/v1/graphql', {
        method: 'POST',
        body,
        headers: requestOptions.headers
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();

    if (result.errors) {
        throw new Error(result.errors);
    }

    return result.data;
}
