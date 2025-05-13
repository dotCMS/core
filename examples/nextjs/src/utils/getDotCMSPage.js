import { cache } from "react";
import { dotCMSClient } from "./dotCMSClient";
import { blogQuery, destinationQuery, fragmentNav, navigationQuery } from "./queries";

export const getDotCMSPage = cache(async (path, searchParams) => {
    try {
        const pageData = await dotCMSClient.page.get(path, {
            ...searchParams,
            graphql: {
                content: {
                    blogs: blogQuery,
                    destinations: destinationQuery,
                    navigation: navigationQuery,
                },
                fragments: [fragmentNav],
            },
        });
        return pageData;
    } catch (e) {
        console.log("ERROR FETCHING PAGE: ", e);

        return null;
    }
});

