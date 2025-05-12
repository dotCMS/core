import { cache } from "react";

import { dotClient } from "@/utils/dotClient";
import { BlogListingPage } from "@/pages/BlogListingPage";

export async function generateMetadata() {
    const { pageAsset } = await getPage(`/blog`);
    const title = pageAsset?.page?.friendlyName || "Page not found";
    return {
        title: `${title} - Blog`
    };
}

export default async function Home() {
    const pageResponse = await getPage(`/blog`);
    return <BlogListingPage {...pageResponse} />;
}

export const getPage = cache(async (path) => {
    try {
        const pageData = await dotClient.page.get(path, {
            graphql: {
                content: {
                    blogs: blogQuery,
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
    search(query: "+contenttype:Blog +live:true") {
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
            teaser
            author {
                firstName
                lastName
                inode
            }
        }
    }
`;
