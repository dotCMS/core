'use client';

import Activity from './content-types/activity';
import Banner from './content-types/banner';
import Blog from './content-types/blog';
import CalendarEvent from './content-types/calendarEvent';
import CallToAction from './content-types/callToAction';
import ImageComponent from './content-types/image';
import Product from './content-types/product';
import WebPageContent from './content-types/webPageContent';

import { DotcmsLayout } from '@dotcms/react';
import { usePathname, useRouter } from 'next/navigation';
import { CustomNoComponent } from './content-types/empty';
import Footer from './layout/footer/footer';
import Header from './layout/header/header';
import Navigation from './layout/navigation';

import NotFound from '@/app/not-found';
import { withExperiments } from '@dotcms/experiments';
import { usePageAsset } from '../hooks/usePageAsset';
/**
 * Configure experiment settings below. If you are not using experiments,
 * you can ignore or remove the experiment-related code and imports.
 */
const experimentConfig = {
    apiKey: process.env.NEXT_PUBLIC_EXPERIMENTS_API_KEY, // API key for experiments, should be securely stored
    server: process.env.NEXT_PUBLIC_DOTCMS_HOST, // DotCMS server endpoint
    debug: process.env.NEXT_PUBLIC_EXPERIMENTS_DEBUG // Debug mode for additional logging
};

// Mapping of components to DotCMS content types
const componentsMap = {
    Blog: Blog,
    webPageContent: WebPageContent,
    Banner: Banner,
    Activity: Activity,
    Product: Product,
    Image: ImageComponent,
    calendarEvent: CalendarEvent,
    CallToAction: CallToAction,
    CustomNoComponent: CustomNoComponent
};

export function MyPage({ pageAsset, nav }) {
    const { replace } = useRouter();
    const pathname = usePathname();

    /**
     * If using experiments, `DotLayoutComponent` is `withExperiments(DotcmsLayout)`.
     * If not using experiments:
     * - Replace the below line with `const DotLayoutComponent = DotcmsLayout;`
     * - Remove DotExperimentsProvider from the return statement.
     */

    const DotLayoutComponent = experimentConfig?.apiKey
        ? withExperiments(DotcmsLayout, {
              ...experimentConfig,
              redirectFn: replace
          })
        : DotcmsLayout;

    pageAsset = usePageAsset(pageAsset);

    if (!pageAsset) {
        return <NotFound />;
    }

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-lime-50">
            {pageAsset?.layout.header && <Header>{!!nav && <Navigation items={nav} />}</Header>}

            <main className="container m-auto">
                <DotLayoutComponent
                    pageContext={{
                        pageAsset,
                        components: componentsMap
                    }}
                    config={{
                        pathname,
                        editor: {
                            params: {
                                depth: 3
                            }
                        }
                    }}
                />
            </main>

            {pageAsset?.layout.footer && <Footer />}
        </div>
    );
}
