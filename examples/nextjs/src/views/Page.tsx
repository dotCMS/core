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
        <div className="flex min-h-dvh flex-col bg-bg">
            {pageAsset?.layout.header && <Header navItems={navigation?.children} />}

            <main className="flex-1">
                <div className="container mx-auto flex flex-col gap-16 px-4 py-10 sm:px-6 sm:py-14 md:gap-24">
                    <DotCMSLayoutBody
                        page={pageAsset}
                        components={pageComponents}
                        mode={dotCMSMode}
                    />
                </div>
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
