import { cache } from "react";
import { dotClient } from "./dotClient";

export const getPage = cache(async (path, searchParams) => {
    try {
        const pageData = await dotClient.page.get(path, {
            graphql: {
                content: {
                    blogs: blogQuery,
                    destinations: destinationQuery,
                },
            },
        });
        return pageData;
    } catch (e) {
        return {
            pageAsset: null,
        };
    }
});

const blogQuery = `
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

const destinationQuery = `
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
