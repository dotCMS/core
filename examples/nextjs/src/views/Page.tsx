'use client';

import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/react';

import { pageComponents } from '@/components/content-types';
import { dotCMSMode } from '@/config/dotcms.config';
import Footer from '@/components/footer/Footer';
import Header from '@/components/header/Header';
import type { PageExtraContent } from '@/types/content';

interface PageProps {
    pageContent: Parameters<typeof useEditableDotCMSPage>[0];
}

export function Page({ pageContent }: PageProps) {
    const { pageAsset, content = {} } = useEditableDotCMSPage(pageContent);
    const pageContentData = content as PageExtraContent;
    const navigation = pageContentData.navigation;

    return (
        <div className="flex flex-col gap-6 bg-slate-50">
            {pageAsset?.layout.header && <Header navItems={navigation?.children} />}

            <main className="container mx-auto">
                <DotCMSLayoutBody
                    page={pageAsset}
                    components={pageComponents}
                    mode={dotCMSMode}
                />
            </main>

            {pageAsset?.layout.footer && (
                <Footer
                    blogs={pageContentData.blogs}
                    destinations={pageContentData.destinations}
                />
            )}
        </div>
    );
}
