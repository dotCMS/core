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
 *
 * @returns {JSX.Element} The rendered DotCMS page body or an error message if the layout body is missing.
 *
 */
export const DotCMSLayoutBody = ({
    page,
    components = {},
    mode = 'production'
}: DotCMSLayoutBodyProps) => {
    const dotCMSPageBody = page?.layout?.body;

    const contextValue = {
        pageAsset: page,
        userComponents: components,
        mode
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
