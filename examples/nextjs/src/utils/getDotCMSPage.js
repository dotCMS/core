import { cache } from "react";
import { dotCMSClient } from "./dotCMSClient";
import {
    blogQuery,
    destinationQuery,
    fragmentNav,
    navigationQuery,
} from "./queries";

export const getDotCMSPage = cache(async (path, searchParams = {}) => {
    // Avoid passing mode if you have a read only auth token
    const { mode, ...params } = searchParams;
    try {
        const pageData = await dotCMSClient.page.get(path, {
            ...params,
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
        console.error("ERROR FETCHING PAGE: ", e.message);

        return null;
    }
});
