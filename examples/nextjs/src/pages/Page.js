'use client';

import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/react/next';
import Footer from '@/components/footer/footer';
import Header from '@/components/header/header';
import Navigation from '@/components/navigation';

import NotFound from '@/app/not-found';
import { pageComponents } from '@/components/contenttypes';

export function Page({ pageContent }) {
    const { pageAsset, content = {} } = useEditableDotCMSPage(pageContent);
    console.log("HERE - PAGE COMPONENT");
    return (
        <div className="flex flex-col gap-6 min-h-screen bg-slate-50">
            {pageAsset?.layout.header && <Header />}

            <main className="container m-auto">
                <DotCMSLayoutBody
                    page={pageAsset}
                    components={pageComponents}
                    mode='development'
                />
            </main>

            {pageAsset?.layout.footer && <Footer {...content} />}
        </div>
    );
}
