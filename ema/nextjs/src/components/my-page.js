'use client';

import { PageProvider, DotcmsPage } from '@dotcms/react';

import WebPageContent from './content-types/webPageContent';
import Banner from './content-types/banner';
import Activity from './content-types/activity';
import Product from './content-types/product';
import ImageComponent from './content-types/image';

import Header from './layout/header';
import Footer from './layout/footer';

// Provide a component for each content type
export const contentComponents = {
    webPageContent: WebPageContent,
    Banner: Banner,
    Activity: Activity,
    Product: Product,
    Image: ImageComponent
};

export function MyPage({ data, nav }) {
    return (
        <PageProvider
            entity={{
                components: contentComponents,
                ...data,
                nav
            }}>
            <div className="flex flex-col min-h-screen gap-6">
                {data.layout.header && <Header />}
                <main className="container flex flex-col gap-8 m-auto">
                    <DotcmsPage />
                </main>
                {data.layout.footer && <Footer />}
            </div>
        </PageProvider>
    );
}
