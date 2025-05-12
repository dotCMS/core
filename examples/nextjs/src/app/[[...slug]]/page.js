import { cache } from "react";

import { Page } from "@/pages/Page";
import { dotClient } from "@/utils/dotClient";

export async function generateMetadata({ params, searchParams }) {
    try {
        const { pageAsset } = await getPageData(params, searchParams);
        const page = pageAsset.page;
        const title = page?.friendlyName || page?.title;

        return {
            title,
        };
    } catch (e) {
        return {
            title: "not found",
        };
    }
}

export default async function Home({ params, searchParams }) {
    const pageContent = await getPageData(params, searchParams);
    return <Page pageContent={pageContent} />;
}

export const getPageData = cache(async (params, searchParams) => {
    try {
        const path = params?.slug?.join("/") || "/";
        const pageData = await dotClient.page.get(path, {
            graphql: {
                content: {
                    blogs: blogQuery,
                    destinations: destinationQuery
                }
            }
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