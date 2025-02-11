import { isInsideEditor } from '@dotcms/client';

import { DotCMSRenderContext } from '../../contexts/DotCMSRenderContext';
import { DotCMSContentlet, DotCMSPageAsset } from '../../types';
import { Row } from '../Row/Row';

interface DotCMSBodyRendererProps {
    dotCMSPageAsset: DotCMSPageAsset;
    customComponents?: Record<string, React.ComponentType<DotCMSContentlet>>;
    devMode?: boolean;
}

export const DotCMSBodyRenderer = ({
    dotCMSPageAsset,
    customComponents,
    devMode
}: DotCMSBodyRendererProps) => {
    const dotCMSPageBody = dotCMSPageAsset?.layout?.body;
    const isDevMode = !!devMode || isInsideEditor();

    if (!dotCMSPageBody) {
        console.warn('Missing required layout.body property in page');

        if (isDevMode) {
            return <ErrorMessage />;
        }

        return null;
    }

    return (
        <DotCMSRenderContext.Provider value={{ dotCMSPageAsset, customComponents, isDevMode }}>
            {dotCMSPageBody.rows.map((row, index) => (
                <Row key={index} row={row} />
            ))}
        </DotCMSRenderContext.Provider>
    );
};

/**
 * Error message component for when the page body is missing
 *
 * @return {JSX.Element} Error message component
 */
const ErrorMessage = () => {
    return (
        <div style={{ padding: '1rem', border: '1px solid #e0e0e0', borderRadius: '4px' }}>
            <p style={{ margin: '0 0 0.5rem', color: '#666' }}>
                The <code>page</code> is missing the required <code>layout.body</code> property.
            </p>
            <p style={{ margin: 0, color: '#666' }}>
                Make sure the page asset is properly loaded and includes a layout configuration.
            </p>
        </div>
    );
};
