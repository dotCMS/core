import { DotcmsLayout } from '@dotcms/react';

import { usePageAsset } from './hooks/usePageAsset';

// Mapping of components to DotCMS content types
const componentsMap = {};

export function MyPage({ pageAsset, nav }: any) {
    pageAsset = usePageAsset(pageAsset);

    return (
        <div className="flex flex-col min-h-screen gap-6 bg-lime-50">
            {/* {pageAsset.layout.header && (
                <Header>
                    <Navigation items={nav} />
                 </Header>
             )} */}

            <main className="container flex flex-col gap-8 m-auto">
                <DotcmsLayout
                    pageContext={{
                        components: componentsMap,
                        pageAsset: pageAsset
                    }}
                    config={{
                        pathname: '/'
                    }}
                />
            </main>

            {/* {pageAsset.layout.footer && <Footer />} */}
        </div>
    );
}
