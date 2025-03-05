import { ErrorMessages } from '../models';

const DEFAULT_PAGE_CONTENTLETS_CONTENT = `
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
`;

/**
 * Builds a GraphQL query for retrieving page content from DotCMS.
 *
 * @param {string} pageQuery - Custom fragment fields to include in the ClientPage fragment
 * @param {string} additionalQueries - Additional GraphQL queries to include in the main query
 * @returns {string} Complete GraphQL query string for page content
 */
export const buildPageQuery = ({
    page,
    fragments,
    additionalQueries
}: {
    page?: string;
    fragments?: string[];
    additionalQueries?: string;
}) => {
    if (!page) {
        console.warn(
            'No page query provided. The query will be used by fetching all content with _map. This may mean poor performance in the query. We suggest you provide a detailed query on page attribute.'
        );
    }

    return `
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
        id
        code
        structureId
        containerId
        contentTypeVar
        containerInode
      }
      containerContentlets {
        uuid
        contentlets {
          ${page ? DEFAULT_PAGE_CONTENTLETS_CONTENT : '_map'}
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

  ${page ? ` fragment ClientPage on DotPage { ${page} } ` : ''}

  ${fragments ? fragments.join(' ') : ''}

  query PageContent($url: String!, $languageId: String, $mode: String) {
    page: page(url: $url, languageId: $languageId, pageMode: $mode) {
      ...DotCMSPage
      ${page ? '...ClientPage' : ''}
    }

    ${additionalQueries}
  }
  `;
};

/**
 * Converts a record of query strings into a single GraphQL query string.
 *
 * @param {Record<string, string>} queryData - Object containing named query strings
 * @returns {string} Combined query string or empty string if no queryData provided
 */
export function buildQuery(queryData: Record<string, string>): string {
    if (!queryData) return '';

    return Object.entries(queryData)
        .map(([key, query]) => `${key}: ${query}`)
        .join(' ');
}

/**
 * Filters response data to include only specified keys.
 *
 * @param {Record<string, string>} responseData - Original response data object
 * @param {string[]} keys - Array of keys to extract from the response data
 * @returns {Record<string, string>} New object containing only the specified keys
 */
export function mapResponseData(
    responseData: Record<string, string>,
    keys: string[]
): Record<string, string> {
    return keys.reduce(
        (accumulator, key) => {
            if (responseData[key] !== undefined) {
                accumulator[key] = responseData[key];
            }

            return accumulator;
        },
        {} as Record<string, string>
    );
}

/**
 * Executes a GraphQL query against the DotCMS API.
 *
 * @param {Object} options - Options for the fetch request
 * @param {string} options.body - GraphQL query string
 * @param {Record<string, string>} options.headers - HTTP headers for the request
 * @returns {Promise<any>} Parsed JSON response from the GraphQL API
 * @throws {Error} If the HTTP response is not successful
 */
export async function fetchGraphQL({
    baseURL,
    body,
    headers
}: {
    baseURL: string;
    body: string;
    headers: Record<string, string>;
}) {
    const response = await fetch(`${baseURL}/api/v1/graphql`, {
        method: 'POST',
        body,
        headers
    });

    if (!response.ok) {
        const error = {
            status: response.status,
            message: ErrorMessages[response.status] || response.statusText
        };

        throw error;
    }

    return await response.json();
}
