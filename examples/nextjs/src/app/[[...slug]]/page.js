import { dotcmsClient } from "@dotcms/client";
import { MyPage } from "@/components/my-page";

const client = dotcmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.DOTCMS_AUTH_TOKEN,
    siteId: "59bb8831-6706-4589-9ca0-ff74016e02b2",
    requestOptions: {
        // In production you might want to deal with this differently
        cache: "no-cache",
    },
});

export async function generateMetadata({ params, searchParams }) {
    const requestData = {
        path: params?.slug ? params.slug.join("/") : "index",
        language_id: searchParams.language_id,
        "com.dotmarketing.persona.id":
            searchParams["com.dotmarketing.persona.id"] || "",
        mode: searchParams.mode,
        variantName: searchParams["variantName"],
    };

    const data = await client.page.get(requestData);

    return {
        title: data.entity.page.friendlyName || data.entity.page.title,
    };
}

export default async function Home({ searchParams, params }) {
    const requestData = {
        path: params?.slug ? params.slug.join("/") : "index",
        language_id: searchParams.language_id,
        "com.dotmarketing.persona.id":
            searchParams["com.dotmarketing.persona.id"] || "",
        mode: searchParams.mode,
        variantName: searchParams["variantName"],
    };

    const data = await client.page.get(requestData);

    const data2 = await client.content
        .getCollection("Blog")
        .query((qb) => qb.field("title").equals("Costa"))
        .depth(2)
        .language(2)
        .sortBy({
            modDate: "desc",
        })
        .fetch();

    let data2Resolved = await data2.json();

    console.log("######################################");

    console.log(data2Resolved.entity.jsonObjectView.contentlets);

    const nav = await client.nav.get({
        path: "/",
        depth: 2,
        languageId: searchParams.language_id,
    });

    return <MyPage nav={nav.entity.children} data={data.entity}></MyPage>;
}
