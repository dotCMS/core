import { DotCMSPageEditorConfig } from '@dotcms/client';

import { useDotcmsEditor } from '../../hooks/useDotcmsEditor';
import { DotCMSPageContext } from '../../models';
import { PageProvider } from '../PageProvider/PageProvider';
import { Row } from '../Row/Row';

/**
 * `DotcmsPageProps` is a type that defines the properties for the `DotcmsLayout` component.
 * It includes a readonly `entity` property that represents the context for a DotCMS page.
 *
 * @typedef {Object} DotcmsPageProps
 *
 * @property {DotCMSPageContext} entity - The context for a DotCMS page.
 * @readonly
 */
export type DotcmsPageProps = {
    /**
     * `pageContext` is a readonly property of the `DotcmsPageProps` type.
     * It represents the context for a DotCMS page and is of type `PageProviderContext`.
     *
     * @property {PageProviderContext} pageContext
     * @memberof DotcmsPageProps
     * @type {DotCMSPageContext}
     * @readonly
     */
    readonly pageContext: DotCMSPageContext;

    readonly config: DotCMSPageEditorConfig;
};

/**
 * `DotcmsLayout` is a functional component that renders a layout for a DotCMS page.
 * It takes a `DotcmsPageProps` object as a parameter and returns a JSX element.
 *
 * @category Components
 * @param {DotcmsPageProps} props - The properties for the DotCMS page.
 * @returns {JSX.Element} - A JSX element that represents the layout for a DotCMS page.
 */
export function DotcmsLayout(dotPageProps: DotcmsPageProps): JSX.Element {
    const pageContext = useDotcmsEditor(dotPageProps);

    return (
        <PageProvider pageContext={pageContext}>
            {pageContext.pageAsset?.layout?.body.rows.map((row, index) => (
                <Row key={index} row={row} />
            ))}
        </PageProvider>
    );
}
