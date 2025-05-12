import { cache } from "react";
import { dotCMSClient } from "./dotCMSClient";
import { blogQuery, destinationQuery } from "./queries";

export const getDotCMSPage = cache(async (path, searchParams) => {
    try {
        const pageData = await dotCMSClient.page.get(path, {
            ...searchParams,
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

