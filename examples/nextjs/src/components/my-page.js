'use client';

import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/react/next';
import Footer from './layout/footer/footer';
import Header from './layout/header/header';
import Navigation from './layout/navigation';

import NotFound from '@/app/not-found';
import { pageComponents } from './contenttypes';

export function MyPage({ pageContent }) {
    const { pageAsset } = useEditableDotCMSPage(pageContent);

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-slate-50">
            {/* {pageAsset?.layout.header && <Header>{!!nav && <Navigation items={nav} />}</Header>} */}

            <main className="container m-auto">
                <DotCMSLayoutBody
                    page={pageAsset}
                    components={pageComponents}
                />
            </main>

            {/* {pageAsset?.layout.footer && <Footer />} */}
        </div>
    );
}
