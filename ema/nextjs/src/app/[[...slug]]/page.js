import { dotcmsClient } from '@dotcms/client';

const client = dotcmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.DOTCMS_AUTH_TOKEN,
    siteId: '59bb8831-6706-4589-9ca0-ff74016e02b2'
});

import { MyPage } from '@/components/my-page';

export async function generateMetadata({ params, searchParams }) {
    const data = await client.getPage({
        path: params?.slug ? params.slug.join('/') : 'index',
        language_id: searchParams.language_id,
        'com.dotmarketing.persona.id': searchParams['com.dotmarketing.persona.id'] || ''
    });

    return {
        title: data.entity.page.friendlyName || data.entity.page.title
    };
}

export default async function Home({ searchParams, params }) {
    const data = await client.getPage({
        path: params?.slug ? params.slug.join('/') : 'index',
        language_id: searchParams.language_id,
        personaId: searchParams['com.dotmarketing.persona.id'] || ''
    });

    const nav = await client.getNav({
        path: '/',
        depth: 2,
        languageId: searchParams.language_id
    });

    return <MyPage nav={nav.entity.children} data={data.entity}></MyPage>;
}
