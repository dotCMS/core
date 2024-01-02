import { useRouter, usePathname } from 'next/navigation';

import { DotcmsPageProps, PageProvider, Row, usePageEditor } from '@dotcms/react';

/**
 * Renders a dotCMS page body, does not include header and footer
 *
 * @export
 * @param {DotcmsPageProps} { entity }
 * @return {*}
 */
export function DotcmsLayout({ entity }: DotcmsPageProps) {
    const router = useRouter();
    const pathname = usePathname();

    const rowsRef = usePageEditor({
        reloadFunction: router.refresh,
        pathname
    });

    const addRowRef = (el: HTMLDivElement) => {
        if (el && !rowsRef.current.includes(el)) {
            rowsRef.current.push(el);
        }
    };

    return (
        <PageProvider entity={entity}>
            {entity.layout.body.rows.map((row, index) => (
                <Row ref={addRowRef} key={index} row={row} />
            ))}
        </PageProvider>
    );
}
