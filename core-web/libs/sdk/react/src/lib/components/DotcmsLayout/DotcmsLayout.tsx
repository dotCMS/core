import { DotCMSPageEditorConfig } from '@dotcms/client';

import { useDotcmsEditor } from '../../hooks/useDotcmsEditor';
import { PageProvider, PageProviderContext } from '../PageProvider/PageProvider';
import { Row } from '../Row/Row';
/**
 * `DotcmsPageProps` is a type that defines the properties for the `DotcmsLayout` component.
 * It includes a readonly `entity` property that represents the context for a DotCMS page.
 *
 * @typedef {Object} DotcmsPageProps
 *
 * @property {PageProviderContext} entity - The context for a DotCMS page.
 * @readonly
 */
export type DotcmsPageProps = {
    /**
     * `entity` is a readonly property of the `DotcmsPageProps` type.
     * It represents the context for a DotCMS page and is of type `PageProviderContext`.
     *
     * @property {PageProviderContext} entity
     * @memberof DotcmsPageProps
     * @type {PageProviderContext}
     * @readonly
     */
    readonly entity: PageProviderContext;

    readonly options?: DotCMSPageEditorConfig;
};

/**
 * `DotcmsLayout` is a functional component that renders a layout for a DotCMS page.
 * It takes a `DotcmsPageProps` object as a parameter and returns a JSX element.
 *
 * @category Components
 * @param {DotcmsPageProps} props - The properties for the DotCMS page.
 * @returns {JSX.Element} - A JSX element that represents the layout for a DotCMS page.
 */
export function DotcmsLayout({ entity, options }: DotcmsPageProps): JSX.Element {
    useDotcmsEditor(options);

    return (
        <PageProvider entity={entity}>
            {entity.layout.body.rows.map((row, index) => (
                <Row key={index} row={row} />
            ))}
        </PageProvider>
    );
}
