import { dotcmsClient } from "@dotcms/client";

// Client for content fetching
export const client = dotcmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST, // 'http://localhost:8080'
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: "59bb8831-6706-4589-9ca0-ff74016e02b2",
    requestOptions: {
        // In production you might want to deal with this differently
        cache: "no-cache",
    }
});


/**
 * Get request params
 *
 * @param {*} { params, searchParams }
 * @return {*}
 */
export const getRequestParams = ({ params, searchParams, defaultPath }) => {
    const path = params?.slug?.join("/") || defaultPath;

    return {
        path,
        language_id: searchParams.language_id,
        "com.dotmarketing.persona.id":
            searchParams["com.dotmarketing.persona.id"] || "",
        mode: searchParams.mode,
        variantName: searchParams["variantName"],
    };
};
