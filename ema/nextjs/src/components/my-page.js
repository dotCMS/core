'use client';

// import GlobalProvider from '@/lib/providers/global';
// import { DotcmsPage } from '@/components/dotcms-page';

import { PageProvider, DotcmsPage } from '@dotcms/react';

export function MyPage({ data, nav }) {
    return (
        <PageProvider
            entity={{
                ...data,
                nav
            }}>
            <DotcmsPage />
        </PageProvider>
    );
}
