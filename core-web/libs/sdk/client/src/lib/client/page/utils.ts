import { consola } from 'consola';

import { DotGraphQLApiResponse, DotHttpClient } from '@dotcms/types';

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
        consola.warn(
            "[DotCMS Client]: No page query was found, so we're loading all content using _map. This might slow things down. For better performance, we recommend adding a specific query in the page attribute."
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
    pageURI
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
    runningExperimentId
    urlContentMap {
      _map
    }
    host {
      identifier
      hostName
     	googleMap
      archived
      contentType
    }
    vanityUrl {
      action
      forwardTo
      uri
    }
    conLanguage {
      id
      language
      languageCode
    }
    template {
      drawed
      anonymous
      theme
      identifier
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
          styleClass
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
      variantId
      visitor {
        persona {
          modDate
          inode
          name
          identifier
          keyTag
          photo {
            versionPath
          }
        }
      }
      persona {
        modDate
        inode
        name
        identifier
        keyTag
        photo {
         versionPath
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

  ${fragments ? fragments.join('\n\n') : ''}

  query PageContent($url: String!, $languageId: String, $mode: String, $personaId: String, $fireRules: Boolean, $publishDate: String, $siteId: String, $variantName: String) {
    page: page(url: $url, languageId: $languageId, pageMode: $mode, persona: $personaId, fireRules: $fireRules, publishDate: $publishDate, site: $siteId, variantName: $variantName) {
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
 * @param {Record<string, unknown> | undefined} responseData - Original response data object
 * @param {string[]} keys - Array of keys to extract from the response data
 * @returns {Record<string, unknown> | undefined} New object containing only the specified keys
 */
export function mapContentResponse(
    responseData: Record<string, unknown> | undefined,
    keys: string[]
): Record<string, unknown> | undefined {
    if (!responseData) {
        return undefined;
    }

    return keys.reduce(
        (accumulator, key) => {
            if (responseData[key] !== undefined) {
                accumulator[key] = responseData[key];
            }
            return accumulator;
        },
        {} as Record<string, unknown>
    );
}

/**
 * Executes a GraphQL query against the DotCMS API.
 *
 * @param {Object} options - Options for the fetch request
 * @param {string} options.body - GraphQL query string
 * @param {Record<string, string>} options.headers - HTTP headers for the request
 * @param {DotHttpClient} options.httpClient - HTTP client for making requests
 * @returns {Promise<DotGraphQLApiResponse>} Parsed JSON response from the GraphQL API
 * @throws {DotHttpError} If the HTTP request fails (non-2xx status or network error)
 */
export async function fetchGraphQL({
    baseURL,
    body,
    headers,
    httpClient
}: {
    baseURL: string;
    body: string;
    headers?: HeadersInit;
    httpClient: DotHttpClient;
}): Promise<DotGraphQLApiResponse> {
    const url = new URL(baseURL);
    url.pathname = '/api/v1/graphql';

    // httpClient.request throws DotHttpError on failure, so we just return the response directly
    return await httpClient.request<DotGraphQLApiResponse>(url.toString(), {
        method: 'POST',
        body,
        headers
    } as RequestInit);
}
