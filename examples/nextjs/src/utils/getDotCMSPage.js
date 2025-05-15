import { cache } from "react";
import { dotCMSClient } from "./dotCMSClient";
import { blogQuery, destinationQuery, fragmentNav, navigationQuery } from "./queries";

export const getDotCMSPage = cache(async (path, searchParams = {}) => {
    const { languageId= '1', mode = "LIVE"} = searchParams;
    try {
        const pageData = await dotCMSClient.page.get(path, {
            languageId,
            mode,
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
