import { MyPage } from "@/components/my-page";
import { ErrorPage } from "@/components/error";

import { handleVanityUrlRedirect } from "@/utils/vanityUrlHandler";
import { client, getRequestParams } from "@/utils/dotcmsClient";
import { getPageRequestParams } from "@dotcms/client";

/**
 * Generate metadata
 *
 * @export
 * @param {*} { params, searchParams }
 * @return {*}
 */
export async function generateMetadata({ params, searchParams }) {
    const path = params?.slug?.join("/") || '/';
    const pageRequestParams = getPageRequestParams({ path, params: searchParams })

    try {
        const data = await client.page.get(pageRequestParams);
        const page = data.entity?.page;
        const title = page?.friendlyName || page?.title;

        return {
            title,
        };
    } catch (e) {
        return {
            title: "not found",
        };
    }
};

export default async function Home({ searchParams, params }) {
    try {
        const path = params?.slug?.join("/") || '/';
        const pageRequestParams = getPageRequestParams({ path, params: searchParams })
        const data = await client.page.get(pageRequestParams);
        const nav = await client.nav.get({
            path: "/",
            depth: 2,
            languageId: searchParams.language_id,
        });
        const { vanityUrl } = data?.entity;

        if (vanityUrl) {
            handleVanityUrlRedirect(vanityUrl);
        }

        return (
            <MyPage nav={nav.entity.children} pageAsset={data.entity}></MyPage>
        );
    } catch (error) {
        return <ErrorPage error={error} />
    }
}
