import { useEffect } from 'react';

import { DotCMSPageContext, RendererMode } from '../../contexts/DotCMSPageContext';
import { useIsDevMode } from '../../hooks/useIsDevMode';
import { DotCMSContentlet, DotCMSPageAsset } from '../../types';
import { Row } from '../Row/Row';

interface DotCMSLayoutBodyRendererProps {
    page: DotCMSPageAsset;
    components?: Record<string, React.ComponentType<DotCMSContentlet>>;
    mode?: RendererMode;
}

export const DotCMSLayoutBodyRenderer = ({
    page,
    components,
    mode = 'production'
}: DotCMSLayoutBodyRendererProps) => {
    const dotCMSPageBody = page?.layout?.body;

    if (!dotCMSPageBody) {
        return <ErrorMessage mode={mode} />;
    }

    const contextValue = {
        pageAsset: page,
        userComponents: components,
        mode
    };

    return (
        <DotCMSPageContext.Provider value={contextValue}>
            {dotCMSPageBody.rows.map((row, index) => (
                <Row key={index} row={row} />
            ))}
        </DotCMSPageContext.Provider>
    );
};

/**
 * Error message component for when the page body is missing
 *
 * @return {JSX.Element} Error message component
 */
const ErrorMessage = ({ mode }: { mode: RendererMode }) => {
    useEffect(() => {
        console.warn('Missing required layout.body property in page');
    }, []);

    const isDevMode = useIsDevMode(mode);

    if (!isDevMode) {
        return null;
    }

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
