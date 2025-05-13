export const blogQuery = `
    search(query: "+contenttype:Blog +live:true", limit: 3) {
        title
        identifier
        ... on Blog {
            inode
            image {
            fileName
            }
            urlMap
            modDate
            urlTitle
        }
    }
`;

export const destinationQuery = `
    search(query: "+contenttype:Destination +live:true", limit: 3) {
        title
        identifier
            ... on Destination {
                inode
                image {
                fileName
                }
                urlMap
                modDate
                url
        }
    }
`;

export const navigationQuery = `
DotNavigation(uri: "/", depth: 1000) {
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