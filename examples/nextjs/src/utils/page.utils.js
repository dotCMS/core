import { client } from "./dotcmsClient";

export const fetchPageData = async (params) => {
    try {
        const pageAsset = await client.page.get({
            ...params,
            depth: 3,
        });

        return { pageAsset };
    } catch (error) {
        return { pageAsset: null, error };
    }
};

export const fetchNavData = async (languageId = 1) => {
    try {
        const nav = await client.nav.get({
            path: "/",
            depth: 2,
            languageId,
        });

        return { nav };
    } catch (error) {
        return { nav: null, error };
    }
};
