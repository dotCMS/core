import { client } from "./dotcmsClient";

export const fetchPageData = async (params) => {
    try {
        // Can the `get` method use the user cookie under the hood when it's inside the editor?
        // Maybe using the dotUVE object from the window object?
        const pageAsset = await client.page.get({
            ...params,
            depth: 3,
        });

        return { pageAsset };
    } catch (error) {
        if (error?.status === 404) {
            return { pageAsset: null, error: null };
        }

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
