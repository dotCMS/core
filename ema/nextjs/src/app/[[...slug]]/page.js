import React from 'react';

import GlobalProvider from '@/lib/providers/global';
import { DotcmsPage } from '@/components/dotcms-page';

async function getPage(params) {
    const { url, language_id } = params;

    // TODO: Check why /json is not getting the page correctly
    const requestUrl = `${
        process.env.NEXT_PUBLIC_DOTCMS_HOST
    }/api/v1/page/render/${url}?language_id=${language_id || '1'}&com.dotmarketing.persona.id=${
        params['com.dotmarketing.persona.id'] || 'modes.persona.no.persona'
    }`;

    const res = await fetch(requestUrl, {
        headers: {
            Authorization: `Bearer ${process.env.DOTCMS_AUTH_TOKEN}`
        },
        cache: 'no-store'
    });

    if (!res.ok) {
        const message = await res.text();
        throw new Error(`Failed to fetch data ${res.status} ${message}`);
    }

    return res.json();
}

async function getNav() {
    // TODO: Check why /json is not getting the page correctly
    const requestUrl = `${process.env.NEXT_PUBLIC_DOTCMS_HOST}/api/v1/nav/?depth=2`;

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

export async function generateMetadata({ params, searchParams }) {
    const data = await getPage({
        url: params?.slug ? params.slug.join('/') : 'index',
        language_id: searchParams.language_id
    });

    return {
        title: data.entity.page.friendlyName || data.entity.page.title
    };
}

export default async function Home({ searchParams, params }) {
    const data = await getPage({
        url: params?.slug ? params.slug.join('/') : 'index',
        language_id: searchParams.language_id,
        'com.dotmarketing.persona.id': searchParams['com.dotmarketing.persona.id']
    });

    const nav = await getNav();

    return (
        // Provide the page data globally
        <GlobalProvider
            entity={{
                ...data.entity,
                nav: nav.entity.children
            }}>
            <DotcmsPage />
        </GlobalProvider>
    );
}
