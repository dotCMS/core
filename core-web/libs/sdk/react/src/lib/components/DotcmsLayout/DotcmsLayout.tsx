import { DotCMSPageEditorConfig } from '@dotcms/client';
import { useExperimentVariant } from '@dotcms/experiments';

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

    readonly config?: DotCMSPageEditorConfig;
};

/**
 * `DotcmsLayout` is a functional component that renders a layout for a DotCMS page.
 * It takes a `DotcmsPageProps` object as a parameter and returns a JSX element.
 *
 * @category Components
 * @param {DotcmsPageProps} props - The properties for the DotCMS page.
 * @returns {JSX.Element} - A JSX element that represents the layout for a DotCMS page.
 */
export function DotcmsLayout({ entity, config }: DotcmsPageProps): JSX.Element {
    const isInsideEditor = useDotcmsEditor(config);

    const { shouldWaitForVariant } = useExperimentVariant(entity);

    if (shouldWaitForVariant()) {
        return <div></div>;
    }

    return (
        <PageProvider entity={{ ...entity, isInsideEditor }}>
            {entity.layout.body.rows.map((row, index) => (
                <Row key={index} row={row} />
            ))}
        </PageProvider>
    );
}
