import React from 'react';

import GlobalProvider from '@/providers/global';
import { DotcmsPage } from '@/components/dotcms-page';

async function getPage(url) {
    const res = await fetch(
        `${process.env.DOTCMS_HOST}/api/v1/page/render/${url || 'index'}?language_id=1`,
        {
            headers: {
                Authorization: `Bearer ${process.env.DOTCMS_AUTH_TOKEN}`
            }
        }
    );

    if (!res.ok) {
        const message = await res.text();
        throw new Error(`Failed to fetch data ${res.status} ${message}`);
    }

    return res.json();
}

export default async function Home({ params }) {
    const data = await getPage(params.slug);

    return (
        // Provide the page data globally
        <GlobalProvider entity={data.entity}>
            <DotcmsPage />
        </GlobalProvider>
    );
}
