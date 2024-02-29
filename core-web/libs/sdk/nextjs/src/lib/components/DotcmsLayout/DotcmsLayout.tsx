import { useRouter, usePathname } from 'next/navigation';

import { sdkDotPageEditor } from '@dotcms/editor';
import { DotcmsPageProps, PageProvider, Row } from '@dotcms/react';
import { useEffect } from 'react';

/**
 * `DotcmsLayout` is a functional component that renders a layout for a DotCMS page.
 * It takes a `DotcmsPageProps` object as a parameter and returns a JSX element.
 *
 * This component should use as a client component becuase it uses the some of the hooks from the `@dotcms/react` package.
 *
 * @example
 * ```tsx
 * 'use client';
 *
 * export function MyPage({ data, nav }) {
    return (
        <div className="flex flex-col min-h-screen gap-6">
            {data.layout.header && (
                <Header>
                    <Navigation items={nav} />
                </Header>
            )}
            <main className="container flex flex-col gap-8 m-auto">
                <DotcmsLayout
                    entity={{
                        // These are the components that will be used to render the contentlets in the page.
                        components: {
                            webPageContent: WebPageContent,
                            Banner: Banner,
                            Activity: Activity,
                            Product: Product,
                            Image: ImageComponent
                        },
                        ...data,
                    }}
                />
            </main>
            {data.layout.footer && <Footer />}
        </div>
    );
}
 * ```
 *
 * @category Components
 * @param {DotcmsPageProps} props - The properties for the DotCMS page.
 * @returns {JSX.Element} - A JSX element that represents the layout for a DotCMS page.
 */
export function DotcmsLayout(props: DotcmsPageProps) {
    const { entity } = props;
    const router = useRouter();
    const pathname = usePathname();

    const client = sdkDotPageEditor.createClient({
        onReload: router.refresh
    });

    useEffect(() => {
        client.init();
        client.updateNavigation(pathname);

        return () => {
            client.destroy();
        };
    }, [client, pathname]);

    entity.isInsideEditor = client.isInsideEditor;

    return (
        <PageProvider entity={entity}>
            {entity.layout.body.rows.map((row, index) => (
                <Row key={index} row={row} />
            ))}
        </PageProvider>
    );
}
