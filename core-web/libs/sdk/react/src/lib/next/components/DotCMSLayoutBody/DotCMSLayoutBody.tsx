import { ReactNode } from 'react';

import { DotCMSBasicContentlet, DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/types';

import { ErrorMessage } from './components/ErrorMessage';

import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';
import { Row } from '../Row/Row';

export interface DotCMSLayoutBodyProps<
    TContentlet extends DotCMSBasicContentlet = DotCMSBasicContentlet
> {
    page: DotCMSPageAsset;
    components: {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        [key: string]: React.ComponentType<TContentlet> | React.ComponentType<any>;
    };
    mode?: DotCMSPageRendererMode;
    /**
     * Pre-rendered server component nodes keyed by contentlet identifier.
     * Use this to render Next.js server components (async components) within the layout.
     * When a contentlet's identifier matches a key in this map, the pre-rendered node
     * is used instead of looking up a component in `components`.
     *
     * @example
     * ```tsx
     * // Server component (page.tsx)
     * const slots = {
     *   [blogListContentlet.identifier]: <BlogListContainer {...blogListContentlet} />
     * };
     * <Page pageContent={pageContent} slots={slots} />
     *
     * // Client component (Page.tsx)
     * <DotCMSLayoutBody page={pageAsset} components={pageComponents} slots={slots} />
     * ```
     */
    slots?: Record<string, ReactNode>;
}

/**
 * DotCMSLayoutBody component renders the layout body for a DotCMS page.
 *
 * It utilizes the dotCMS page asset's layout body to render the page body.
 * If the layout body does not exist, it renders an error message in the mode is `development`.
 *
 * @public
 * @component
 * @param {Object} props - Component properties.
 * @param {DotCMSPageAsset} props.page - The DotCMS page asset containing the layout information.
 * @param {Record<string, React.ComponentType<DotCMSContentlet>>} [props.components] - mapping of custom components for content rendering.
 * @param {DotCMSPageRendererMode} [props.mode='production'] - The renderer mode; defaults to 'production'. Alternate modes might trigger different behaviors.
 * @param {Record<string, ReactNode>} [props.slots] - Pre-rendered server component nodes keyed by contentlet identifier.
 *
 * @returns {JSX.Element} The rendered DotCMS page body or an error message if the layout body is missing.
 *
 */
export const DotCMSLayoutBody = ({
    page,
    components = {},
    mode = 'production',
    slots = {}
}: DotCMSLayoutBodyProps) => {
    const dotCMSPageBody = page?.layout?.body;

    const contextValue = {
        pageAsset: page,
        userComponents: components,
        mode,
        slots
    };

    return (
        <DotCMSPageContext.Provider value={contextValue}>
            {dotCMSPageBody ? (
                dotCMSPageBody.rows.map((row, index) => <Row key={index} row={row} />)
            ) : (
                <ErrorMessage />
            )}
        </DotCMSPageContext.Provider>
    );
};
