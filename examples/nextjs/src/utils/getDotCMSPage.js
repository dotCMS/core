import { cache } from "react";
import { dotCMSClient } from "./dotCMSClient";
import {
    blogQuery,
    destinationQuery,
    fragmentNav,
    navigationQuery,
} from "./queries";

export const getDotCMSPage = cache(async (path) => {
    try {
        return await dotCMSClient.page.get(path, {
            graphql: {
                content: {
                    blogs: blogQuery,
                    destinations: destinationQuery,
                    navigation: navigationQuery,
                },
                fragments: [fragmentNav],
            },
        });
    } catch (e) {
        return { error: e };
    }
});
