import { MyPage } from '@/components/my-page';
import { ErrorPage } from '@/components/error';

import { handleVanityUrlRedirect } from '@/utils/vanityUrlHandler';
import { client } from '@/utils/dotcmsClient';
import { getPageRequestParams, graphqlToPageEntity } from '@dotcms/client';
import { fetchNavData, fetchPageData } from '@/utils/page.utils';

/**
 * Generate metadata
 *
 * @export
 * @param {*} { params, searchParams }
 * @return {*}
 */
export async function generateMetadata({ params, searchParams }) {
    const path = params?.slug?.join('/') || '/';
    const pageRequestParams = getPageRequestParams({
        path,
        params: searchParams
    });

    try {
        const data = await client.page.get(pageRequestParams);
        const page = data.page;
        const title = page?.friendlyName || page?.title;

        return {
            title
        };
    } catch (e) {
        return {
            title: 'not found'
        };
    }
}

export default async function Home({ searchParams, params }) {
    const getNavigation = async () => {
        const path = params?.slug?.join('/') || '/';
        const pageParams = getPageRequestParams({
            path,
            params: searchParams
        });

        const { nav, error: navError } = await fetchNavData(pageParams.language_id);

        return {
            nav,
            error: navError
        };
    };
    const { nav, error } = await getNavigation();

    const data = await client.gql({
        page: {
            url: params?.slug?.join('/'),
            language: searchParams?.language_id,
            mode: 'EDIT_MODE',
            pageFragment: `
            containers {
                containerContentlets {
                    contentlets {
                        ... on Blog {
                            author {
                                firstName
                                lastName
                            }
                        }
                    }
                }
            }
        `
        },
        content: {
            blogs: `search(query: "+contentType: blog", limit: 3) {
            _map
            title
            ...on Blog {
                author {
                    title
                }
            }
        }`,
            destinations: `search(query: "+contentType: destination", limit: 3) {
            _map
        }`
        }
    });

    const { data: res, queryMetadata } = data;

    const pageAsset = graphqlToPageEntity({ page: res.page });

    // Move this to MyPage
    if (error) {
        return <ErrorPage error={error} />;
    }

    if (pageAsset?.vanityUrl) {
        handleVanityUrlRedirect(pageAsset?.vanityUrl);
    }

    const content = {
        blogs: res.blogs.map((blog) => ({
            ...blog._map,
            author: blog.author
        })),
        destinations: res.destinations.map((destination) => destination._map)
    };

    return (
        <MyPage
            nav={nav?.entity.children}
            pageAsset={pageAsset}
            queryMetadata={queryMetadata}
            content={content}
        />
    );
}
