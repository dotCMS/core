'use client';

import GqlWebPageContent from './contenttypes/gqlWebPageContent';
import Banner from './contenttypes/banner';
import Activity from './contenttypes/activity';
import CallToAction from './contenttypes/callToAction';
import CalendarEvent from './contenttypes/calendarEvent';
import Product from './contenttypes/product';
import ImageComponent from './contenttypes/image';

import Header from './layout/header/header';
import Footer from './layout/footer/footer';
import Navigation from './layout/navigation';
import { usePathname, useRouter } from 'next/navigation';
import { DotcmsLayout } from '@dotcms/react';
import { withExperiments } from '@dotcms/experiments';
import { CustomNoComponent } from './contenttypes/empty';

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
    webPageContent: GqlWebPageContent,
    Banner: Banner,
    Activity: Activity,
    Product: Product,
    Image: ImageComponent,
    calendarEvent: CalendarEvent,
    CallToAction: CallToAction,
    CustomNoComponent: CustomNoComponent
};

export function MyGraphQLPage({ pageAsset, nav, query }) {
    const { replace } = useRouter();
    const pathname = usePathname();

    /**
     * If using experiments, `DotLayoutComponent` is `withExperiments(DotcmsLayout)`.
     * If not using experiments:
     * - Replace the below line with `const DotLayoutComponent = DotcmsLayout;`
     * - Remove DotExperimentsProvider from the return statement.
     */
    const DotLayoutComponent = experimentConfig.apiKey
        ? withExperiments(DotcmsLayout, {
              ...experimentConfig,
              redirectFn: replace
          })
        : DotcmsLayout;

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-lime-50">
            {pageAsset.layout.header && (
                <Header>
                    <Navigation items={nav} />
                </Header>
            )}

            <main className="flex flex-col gap-8 m-auto">
                <DotLayoutComponent
                    pageContext={{
                        components: componentsMap,
                        pageAsset: pageAsset
                    }}
                    config={{
                        pathname,
                        editor: { query }
                    }}
                />
            </main>

            {pageAsset.layout.footer && <Footer />}
        </div>
    );
}
