import React from 'react';

import GlobalProvider from '@/providers/global';
import { DotcmsPage } from '@/components/dotcms-page';

async function getPage({ url, language_id }) {
    const requestUrl = `${process.env.DOTCMS_HOST}/api/v1/page/json/${url || 'index'}?language_id=${
        language_id || '1'
    }`;

    const res = await fetch(requestUrl, {
        headers: {
            Authorization: `Bearer ${process.env.DOTCMS_AUTH_TOKEN}`
        }
    });

    if (!res.ok) {
        const message = await res.text();
        throw new Error(`Failed to fetch data ${res.status} ${message}`);
    }

    return res.json();
}

export default async function Home({ searchParams, params }) {
    const data = await getPage({
        url: params.slug.join('/'),
        language_id: searchParams.language_id
    });

    return (
        // Provide the page data globally
        <GlobalProvider entity={data.entity}>
            <DotcmsPage />
        </GlobalProvider>
    );
}
