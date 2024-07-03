const API_URL = `${process.env.NEXT_PUBLIC_DOTCMS_HOST}/api/v1/graphql`;

/**
 * Get the GraphQL query for a page
 *
 * @param {*} query
 * @return {*}
 */
const getGraphQLPageQuery = ({ path, language_id, mode}) => {
    const params = [];

    if(language_id) {
        params.push(`languageId: "${language_id}"`);
    }

    if(mode) {
        params.push(`pageMode: "${mode}"`);
    }

    const paramsString = params.length ? `, ${params.join(", ")}` : "";

    return `
    {
        page(url: "${path}" ${paramsString}) {
            title
            url
            seodescription
            containers {
                path
                identifier
                maxContentlets
                containerStructures {
                    id
                    structureId
                    containerInode
                    containerId
                    code
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
            host {
                hostName
            }
            layout {
                header
                footer
                body {
                    rows {
                        columns {
                            widthPercent
                            leftOffset
                            styleClass
                            preview
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
            template {
                iDate
                inode
                identifier
                source
                title
                friendlyName
                modDate
                sortOrder
                showOnMenu
                image
                drawed
                drawedBody
            }
            viewAs {
                mode
                visitor {
                  persona {
                    name
                  }
                }
                language {
                  id
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
export const getGraphQLPageData = async (params) => {
    const query = getGraphQLPageQuery(params);

    const res = await fetch(API_URL, {
        method: "POST",
        headers: {
            "Cookie": `access_token=${process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN}`,
            "Content-Type": "application/json",
            "dotcachettl": "0" // Bypasses GraphQL cache
        },
        body: JSON.stringify({ query }),
        cache: "no-cache", // Invalidate cache for Next.js
    });
    const { data } = await res.json();
    return data;
};
