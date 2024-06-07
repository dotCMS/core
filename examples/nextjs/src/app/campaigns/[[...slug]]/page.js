import { dotcmsClient, graphqlToPageEntity } from "@dotcms/client";
import { MyPage } from "@/components/my-page";

import { getGraphQLPageData } from "../../utils/gql";

const client = dotcmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.DOTCMS_AUTH_TOKEN,
    siteId: "59bb8831-6706-4589-9ca0-ff74016e02b2",
    requestOptions: {
        // In production you might want to deal with this differently
        cache: "no-cache",
    },
});

export default async function Home({ searchParams, params }) {
    const defaultPath = "/campaigns/colorado-preseason-special";
    const path = params?.slug
        ? "/campaigns/" + params.slug.join("/")
        : defaultPath;
    const requestData = {
        path,
        language_id: searchParams.language_id,
        "com.dotmarketing.persona.id":
            searchParams["com.dotmarketing.persona.id"] || "",
        mode: searchParams.mode,
        variantName: searchParams["variantName"],
    };
    const nav = await client.nav.get({
        path: "/",
        depth: 2,
        languageId: searchParams.language_id,
    });

    const data = await getGraphQLPageData(requestData);
    const pageAsset = graphqlToPageEntity(data);

    return <MyPage nav={nav.entity.children} pageAsset={pageAsset}></MyPage>;
}
