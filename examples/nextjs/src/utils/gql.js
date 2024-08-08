const GRAPHQL_ENPOINT = `/api/v1/graphql`;

/**
 * Get the GraphQL query for a page
 *
 * @param {*} query
 * @return {*}
 */
export function getGraphQLPageQuery({ path, language_id, mode}) {
    const params = [];

    if (language_id) {
        params.push(`languageId: "${language_id}"`);
    }

    if (mode) {
        params.push('pageMode: "EDIT_MODE"');
    }

    const paramsString = params.length ? `, ${params.join(", ")}` : "";

    return `
    {
        page(url: "${path}"${paramsString}) {
            _map
            canEdit
            canLock
            canRead
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
                        _map
                        ... on calendarEvent {
                            # Related Contentlet
                            location {
                                title
                                url
                                # Related Contentlet
                                activities {
                                    title
                                    urlMap
                                }
                            }
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
    }
    `;
};


/**
 * Fetch content from dotCMS using GraphQL
 *
 * @param {*} query
 * @return {*}
 */
export const getGraphQLPageData = async (query) => {
    const url = new URL(GRAPHQL_ENPOINT, process.env.NEXT_PUBLIC_DOTCMS_HOST);

    try {
        const res = await fetch(url, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN}`,
                "Content-Type": "application/json",
                "dotcachettl": "0" // Bypasses GraphQL cache
            },
            body: JSON.stringify({ query }),
            cache: "no-cache", // Invalidate cache for Next.js
        });
        const { data } = await res.json();
        return data;
    } catch(err) {
        console.group("Error fetching Page");
        console.warn("Check your URL or DOTCMS_HOST: ", url.toString());
        console.error(err);
        console.groupEnd();

        return { page: null };
    }
};
